package com.example.finance.sync

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.example.finance.auth.SupabaseAuthRepository
import com.example.finance.data.AppDatabase
import com.example.finance.model.AccountEntity
import com.example.finance.model.AccountType
import com.example.finance.model.SyncEntityMapEntity
import com.example.finance.supabase.SupabaseConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.UUID
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * First complete sync vertical-slice: accounts.
 * Uses the signed-in user's JWT for every Data API request; RLS remains the
 * authority for row ownership. The Room outbox is durable and survives restarts.
 */
class AccountSyncCoordinator(context: Context) {
    private val appContext = context.applicationContext
    private val db = AppDatabase.getDatabase(appContext)
    private val dao = db.financeDao()
    private val auth = SupabaseAuthRepository(appContext)
    private val http = OkHttpClient.Builder()
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun syncOnce() = withContext(Dispatchers.IO) {
        check(auth.restoreSession()) { "No active Supabase session; sync is paused without signing out" }
        val token = auth.currentAccessToken() ?: error("Supabase access token unavailable")
        val userId = fetchUserId(token)
        queueLegacyLocalAccounts()
        pushPending(token, userId)
        pullAccounts(token)
        check(dao.countPendingSyncMutations() == 0) { "Some account changes remain queued for retry" }
        dao.pruneCompletedSyncMutations(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(14))
    }

    suspend fun handleRealtimeEvent(
        table: String,
        event: String,
        record: JSONObject,
        oldRecord: JSONObject
    ) = withContext(Dispatchers.IO) {
        if (table != "accounts") return@withContext
        if (!auth.restoreSession()) return@withContext
        val token = auth.currentAccessToken() ?: return@withContext
        when (event.uppercase()) {
            "DELETE" -> {
                val remoteId = oldRecord.optString("id").takeIf(String::isNotBlank) ?: return@withContext
                val mapping = dao.getSyncEntityMapByRemoteId("accounts", remoteId) ?: return@withContext
                if (dao.hasPendingSyncMutation("accounts", mapping.localId)) return@withContext
                db.withTransaction {
                    dao.deleteAccountById(mapping.localId)
                    dao.deleteSyncEntityMap("accounts", mapping.localId)
                }
            }
            "INSERT", "UPDATE" -> {
                val remoteId = record.optString("id").takeIf(String::isNotBlank) ?: return@withContext
                val row = fetchAccount(token, remoteId) ?: return@withContext
                applyRemoteAccount(row)
            }
        }
    }

    private suspend fun queueLegacyLocalAccounts() {
        dao.getAllAccountsSnapshot().forEach { account ->
            if (dao.getSyncEntityMap("accounts", account.id) == null) {
                db.withTransaction {
                    if (dao.getSyncEntityMap("accounts", account.id) != null) return@withTransaction
                    val remoteId = UUID.randomUUID().toString()
                    dao.upsertSyncEntityMap(SyncEntityMapEntity("accounts", account.id, remoteId))
                    val lastFour = account.accountNumberSafe.filter(Char::isDigit).takeLast(4).takeIf(String::isNotEmpty)
                    val payload = JSONObject()
                        .put("id", remoteId)
                        .put("name", account.name)
                        .put("type", when (account.type) {
                            AccountType.CASH -> "cash"
                            AccountType.BANK -> "bank"
                            AccountType.WALLET -> "wallet"
                            AccountType.CREDIT_CARD -> "credit_card"
                            AccountType.INVESTMENT -> "investment"
                            AccountType.SAVINGS, AccountType.OTHER -> "other"
                        })
                        .put("currency_code", account.currency.uppercase())
                        .put("opening_balance", account.openingBalance)
                        .put("current_balance", account.balance)
                        .put("institution_name", if (account.institution.isBlank()) JSONObject.NULL else account.institution)
                        .put("last_four", lastFour ?: JSONObject.NULL)
                        .put("is_active", !account.isArchived)
                        .put("updated_at", utcTimestamp())
                    dao.enqueueSyncMutation(
                        com.example.finance.model.SyncOutboxEntity(
                            entityType = "accounts",
                            localId = account.id,
                            operation = "UPSERT",
                            payload = payload.toString()
                        )
                    )
                }
            }
        }
    }

    private suspend fun pushPending(token: String, userId: String) {
        dao.getPendingSyncMutations(200).forEach { item ->
            try {
                if (item.entityType != "accounts") {
                    dao.recordSyncMutationFailure(item.id, "Unsupported sync entity: ${item.entityType}")
                    return@forEach
                }
                val payload = JSONObject(item.payload)
                if (item.operation == "DELETE") {
                    val remoteId = payload.getString("id")
                    val url = "${SupabaseConnection.url.trimEnd('/')}/rest/v1/accounts?id=eq.$remoteId&user_id=eq.$userId"
                    execute(token, url, "DELETE", null, "return=minimal")
                    dao.markSyncMutationComplete(item.id)
                    dao.deleteSyncEntityMap("accounts", item.localId)
                } else {
                    payload.put("user_id", userId)
                    val url = "${SupabaseConnection.url.trimEnd('/')}/rest/v1/accounts?on_conflict=id"
                    execute(token, url, "POST", payload.toString(), "resolution=merge-duplicates,return=minimal")
                    dao.markSyncMutationComplete(item.id)
                }
            } catch (error: Exception) {
                Log.e(TAG, "Account outbox item ${item.id} failed", error)
                dao.recordSyncMutationFailure(item.id, error.message ?: error.javaClass.simpleName)
            }
        }
    }

    private suspend fun pullAccounts(token: String) {
        val url = "${SupabaseConnection.url.trimEnd('/')}/rest/v1/accounts?select=*&order=updated_at.asc"
        val body = execute(token, url, "GET", null, null)
        val rows = JSONArray(body)
        for (index in 0 until rows.length()) {
            applyRemoteAccount(rows.getJSONObject(index))
        }
    }

    private suspend fun fetchAccount(token: String, remoteId: String): JSONObject? {
        val url = "${SupabaseConnection.url.trimEnd('/')}/rest/v1/accounts?id=eq.$remoteId&select=*"
        val rows = JSONArray(execute(token, url, "GET", null, null))
        return rows.optJSONObject(0)
    }

    private suspend fun applyRemoteAccount(row: JSONObject) {
        val remoteId = row.getString("id")
        val mapping = dao.getSyncEntityMapByRemoteId("accounts", remoteId)
        if (mapping != null && dao.hasPendingSyncMutation("accounts", mapping.localId)) return
        val existing = mapping?.let { dao.getAccountById(it.localId) }
        val remoteType = row.optString("type")
        val account = AccountEntity(
            id = mapping?.localId ?: 0L,
            name = row.optString("name"),
            type = if (remoteType == "other" && existing?.type == AccountType.SAVINGS) AccountType.SAVINGS else when (remoteType) {
                "cash" -> AccountType.CASH
                "bank" -> AccountType.BANK
                "wallet" -> AccountType.WALLET
                "credit_card" -> AccountType.CREDIT_CARD
                "investment" -> AccountType.INVESTMENT
                else -> AccountType.OTHER
            },
            currency = row.optString("currency_code", "LYD"),
            balance = row.optDouble("current_balance", 0.0),
            openingBalance = row.optDouble("opening_balance", 0.0),
            accountNumberSafe = row.optString("last_four").takeIf(String::isNotBlank).orEmpty(),
            institution = row.optString("institution_name").takeIf(String::isNotBlank).orEmpty(),
            isArchived = !row.optBoolean("is_active", true)
        )
        db.withTransaction {
            val localId = dao.insertAccount(account)
            dao.upsertSyncEntityMap(SyncEntityMapEntity("accounts", localId, remoteId))
        }
    }

    private fun utcTimestamp(): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date())

    private fun fetchUserId(token: String): String {
        val url = "${SupabaseConnection.url.trimEnd('/')}/auth/v1/user"
        val response = execute(token, url, "GET", null, null)
        return JSONObject(response).getString("id")
    }

    private fun execute(
        token: String,
        url: String,
        method: String,
        body: String?,
        prefer: String?
    ): String {
        val builder = Request.Builder()
            .url(url)
            .header("apikey", SupabaseConnection.publishableKey)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
        if (prefer != null) builder.header("Prefer", prefer)
        when (method) {
            "POST" -> builder.post((body ?: "{}").toRequestBody("application/json; charset=utf-8".toMediaType()))
            "DELETE" -> builder.delete()
            else -> builder.get()
        }
        http.newCall(builder.build()).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("Supabase sync failed (HTTP ${response.code}): ${text.take(500)}")
            }
            return text
        }
    }

    companion object { private const val TAG = "AccountSyncCoordinator" }
}
