package com.example.finance.sync

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.example.finance.auth.SupabaseAuthRepository
import com.example.finance.data.AppDatabase
import com.example.finance.model.AccountEntity
import com.example.finance.model.AccountType
import com.example.finance.model.SyncEntityMapEntity
import com.example.finance.model.SyncOutboxEntity
import com.example.finance.model.TransactionEntity
import com.example.finance.model.TransactionStatus
import com.example.finance.model.TransactionType
import com.example.finance.model.TransferEntity
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
        queueLegacyLocalTransactions()
        queueLegacyLocalTransfers()
        pushPending(token, userId)
        pullAccounts(token)
        pullTransactions(token)
        pullTransfers(token)
        check(dao.countPendingSyncMutations() == 0) { "Some finance changes remain queued for retry" }
        dao.pruneCompletedSyncMutations(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(14))
    }

    suspend fun handleRealtimeEvent(
        table: String,
        event: String,
        record: JSONObject,
        oldRecord: JSONObject
    ) = withContext(Dispatchers.IO) {
        if (table !in setOf("accounts", "transactions", "transfers")) return@withContext
        if (!auth.restoreSession()) return@withContext
        val token = auth.currentAccessToken() ?: return@withContext
        when (event.uppercase()) {
            "DELETE" -> {
                val remoteId = oldRecord.optString("id").takeIf(String::isNotBlank) ?: return@withContext
                val mapping = dao.getSyncEntityMapByRemoteId(table, remoteId) ?: return@withContext
                if (dao.hasPendingSyncMutation(table, mapping.localId)) return@withContext
                db.withTransaction {
                    when (table) {
                        "accounts" -> dao.deleteAccountById(mapping.localId)
                        "transactions" -> dao.deleteTransactionById(mapping.localId)
                        "transfers" -> dao.deleteTransferById(mapping.localId)
                    }
                    dao.deleteSyncEntityMap(table, mapping.localId)
                }
            }
            "INSERT", "UPDATE" -> {
                val remoteId = record.optString("id").takeIf(String::isNotBlank) ?: return@withContext
                val row = fetchRow(token, table, remoteId) ?: return@withContext
                when (table) {
                    "accounts" -> applyRemoteAccount(row)
                    "transactions" -> applyRemoteTransaction(row)
                    "transfers" -> applyRemoteTransfer(row)
                }
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
        dao.getPendingSyncMutations(500).forEach { item ->
            try {
                val table = item.entityType
                if (table !in setOf("accounts", "transactions", "transfers")) {
                    dao.recordSyncMutationFailure(item.id, "Unsupported sync entity: $table")
                    return@forEach
                }
                val payload = JSONObject(item.payload)
                val baseUrl = "${SupabaseConnection.url.trimEnd('/')}/rest/v1/$table"
                if (item.operation == "DELETE") {
                    val remoteId = payload.getString("id")
                    execute(
                        token,
                        "$baseUrl?id=eq.$remoteId&user_id=eq.$userId",
                        "DELETE",
                        null,
                        "return=minimal"
                    )
                    dao.markSyncMutationComplete(item.id)
                    dao.deleteSyncEntityMap(table, item.localId)
                } else {
                    payload.put("user_id", userId)
                    execute(
                        token,
                        "$baseUrl?on_conflict=id",
                        "POST",
                        payload.toString(),
                        "resolution=merge-duplicates,return=minimal"
                    )
                    dao.markSyncMutationComplete(item.id)
                }
            } catch (error: Exception) {
                Log.e(TAG, "Outbox item ${item.id} failed", error)
                dao.recordSyncMutationFailure(item.id, error.message ?: error.javaClass.simpleName)
            }
        }
    }

    private suspend fun pullAccounts(token: String) {
        val url = "${SupabaseConnection.url.trimEnd('/')}/rest/v1/accounts?select=*&order=updated_at.asc"
        val rows = JSONArray(execute(token, url, "GET", null, null))
        for (index in 0 until rows.length()) applyRemoteAccount(rows.getJSONObject(index))
    }

    private suspend fun pullTransactions(token: String) {
        val url = "${SupabaseConnection.url.trimEnd('/')}/rest/v1/transactions?select=*&order=updated_at.asc"
        val rows = JSONArray(execute(token, url, "GET", null, null))
        for (index in 0 until rows.length()) applyRemoteTransaction(rows.getJSONObject(index))
    }

    private suspend fun pullTransfers(token: String) {
        val url = "${SupabaseConnection.url.trimEnd('/')}/rest/v1/transfers?select=*&order=created_at.asc"
        val rows = JSONArray(execute(token, url, "GET", null, null))
        for (index in 0 until rows.length()) applyRemoteTransfer(rows.getJSONObject(index))
    }

    private suspend fun fetchRow(token: String, table: String, remoteId: String): JSONObject? {
        if (table !in setOf("accounts", "transactions", "transfers")) return null
        val url = "${SupabaseConnection.url.trimEnd('/')}/rest/v1/$table?id=eq.$remoteId&select=*"
        return JSONArray(execute(token, url, "GET", null, null)).optJSONObject(0)
    }

    private suspend fun fetchAccount(token: String, remoteId: String): JSONObject? =
        fetchRow(token, "accounts", remoteId)

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
            isArchived = !row.optBoolean("is_active", true),
            notes = existing?.notes.orEmpty(),
            colorHex = existing?.colorHex ?: "#00D09C",
            createdAt = existing?.createdAt ?: System.currentTimeMillis()
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
