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

    private suspend fun enqueueTransactionForSync(tx: TransactionEntity) {
        val accountMap = dao.getSyncEntityMap("accounts", tx.accountId)
            ?: error("Account " + tx.accountId + " must be mapped before its transactions")
        val targetRemoteId = tx.targetAccountId?.let { targetId ->
            dao.getSyncEntityMap("accounts", targetId)?.remoteId
        }
        val mapping = dao.getSyncEntityMap("transactions", tx.id)
            ?: SyncEntityMapEntity("transactions", tx.id, UUID.randomUUID().toString())
        dao.upsertSyncEntityMap(mapping)
        val metadata = JSONObject()
            .put("local_type", tx.type.name.lowercase())
            .put("category", tx.category)
            .put("merchant", tx.merchant)
            .put("tags", tx.tags)
            .put("notes", tx.notes)
            .put("is_recurring", tx.isRecurring)
            .put("target_account_id", targetRemoteId ?: JSONObject.NULL)
        val payload = JSONObject()
            .put("id", mapping.remoteId)
            .put("account_id", accountMap.remoteId)
            .put("category_id", JSONObject.NULL)
            .put("type", when (tx.type) {
                TransactionType.INCOME, TransactionType.REFUND -> "income"
                TransactionType.EXPENSE -> "expense"
                TransactionType.TRANSFER -> "transfer"
                TransactionType.ADJUSTMENT -> "adjustment"
            })
            .put("status", tx.status.name.lowercase())
            .put("amount", tx.amount)
            .put("currency_code", tx.currency.uppercase())
            .put("description", tx.description)
            .put("transaction_date", utcTimestamp(tx.date))
            .put("reference", JSONObject.NULL)
            .put("metadata", metadata)
            .put("updated_at", utcTimestamp())
        dao.enqueueSyncMutation(SyncOutboxEntity("transactions", tx.id, "UPSERT", payload.toString()))
    }

    private suspend fun enqueueTransferForSync(transfer: TransferEntity) {
        val from = dao.getSyncEntityMap("accounts", transfer.fromAccountId)
            ?: error("Transfer source account must be mapped first")
        val to = dao.getSyncEntityMap("accounts", transfer.toAccountId)
            ?: error("Transfer destination account must be mapped first")
        val mapping = dao.getSyncEntityMap("transfers", transfer.id)
            ?: SyncEntityMapEntity("transfers", transfer.id, UUID.randomUUID().toString())
        dao.upsertSyncEntityMap(mapping)
        val payload = JSONObject()
            .put("id", mapping.remoteId)
            .put("from_account_id", from.remoteId)
            .put("to_account_id", to.remoteId)
            .put("amount", transfer.amount)
            .put("fee", transfer.fee)
            .put("currency_code", transfer.currency.uppercase())
            .put("description", transfer.description)
            .put("transfer_date", utcTimestamp(transfer.date))
            .put("status", "completed")
        dao.enqueueSyncMutation(SyncOutboxEntity("transfers", transfer.id, "UPSERT", payload.toString()))
    }

    private suspend fun queueLegacyLocalTransactions() {
        dao.getAllTransactionsSnapshot().forEach { tx ->
            if (dao.getSyncEntityMap("transactions", tx.id) == null) {
                db.withTransaction {
                    if (dao.getSyncEntityMap("transactions", tx.id) == null) enqueueTransactionForSync(tx)
                }
            }
        }
    }

    private suspend fun queueLegacyLocalTransfers() {
        dao.getAllTransfersSnapshot().forEach { transfer ->
            if (dao.getSyncEntityMap("transfers", transfer.id) == null) {
                db.withTransaction {
                    if (dao.getSyncEntityMap("transfers", transfer.id) == null) enqueueTransferForSync(transfer)
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

    private suspend fun applyRemoteTransaction(row: JSONObject) {
        val remoteId = row.getString("id")
        val mapping = dao.getSyncEntityMapByRemoteId("transactions", remoteId)
        if (mapping != null && dao.hasPendingSyncMutation("transactions", mapping.localId)) return
        val accountRemoteId = row.optString("account_id")
        val accountMapping = dao.getSyncEntityMapByRemoteId("accounts", accountRemoteId) ?: return
        val existing = mapping?.let { dao.getTransactionById(it.localId) }
        val metadata = row.optJSONObject("metadata") ?: JSONObject()
        val targetRemoteId = metadata.optString("target_account_id").takeIf { it.isNotBlank() && it != "null" }
        val targetLocalId = targetRemoteId?.let { dao.getSyncEntityMapByRemoteId("accounts", it)?.localId }
        val localType = metadata.optString("local_type")
        val type = when (localType) {
            "refund" -> TransactionType.REFUND
            "income" -> TransactionType.INCOME
            "expense" -> TransactionType.EXPENSE
            "transfer" -> TransactionType.TRANSFER
            "adjustment" -> TransactionType.ADJUSTMENT
            else -> when (row.optString("type")) {
                "income" -> TransactionType.INCOME
                "expense" -> TransactionType.EXPENSE
                "transfer" -> TransactionType.TRANSFER
                else -> TransactionType.ADJUSTMENT
            }
        }
        val status = when (row.optString("status")) {
            "pending" -> TransactionStatus.PENDING
            "cancelled" -> TransactionStatus.CANCELLED
            else -> TransactionStatus.COMPLETED
        }
        val transaction = TransactionEntity(
            id = mapping?.localId ?: 0L,
            accountId = accountMapping.localId,
            type = type,
            amount = row.optDouble("amount", 0.0),
            currency = row.optString("currency_code", "LYD"),
            category = metadata.optString("category", existing?.category.orEmpty()),
            date = parseTimestamp(row.optString("transaction_date")),
            description = row.optString("description").takeUnless { it == "null" }.orEmpty(),
            merchant = metadata.optString("merchant", existing?.merchant.orEmpty()),
            tags = metadata.optString("tags", existing?.tags.orEmpty()),
            notes = metadata.optString("notes", existing?.notes.orEmpty()),
            attachmentUri = existing?.attachmentUri,
            isRecurring = metadata.optBoolean("is_recurring", existing?.isRecurring ?: false),
            status = status,
            targetAccountId = targetLocalId
        )
        db.withTransaction {
            val localId = dao.insertTransaction(transaction)
            dao.upsertSyncEntityMap(SyncEntityMapEntity("transactions", localId, remoteId))
        }
    }

    private suspend fun applyRemoteTransfer(row: JSONObject) {
        val remoteId = row.getString("id")
        val mapping = dao.getSyncEntityMapByRemoteId("transfers", remoteId)
        if (mapping != null && dao.hasPendingSyncMutation("transfers", mapping.localId)) return
        val from = dao.getSyncEntityMapByRemoteId("accounts", row.optString("from_account_id")) ?: return
        val to = dao.getSyncEntityMapByRemoteId("accounts", row.optString("to_account_id")) ?: return
        val existing = mapping?.let { dao.getTransferById(it.localId) }
        val transfer = TransferEntity(
            id = mapping?.localId ?: 0L,
            fromAccountId = from.localId,
            toAccountId = to.localId,
            amount = row.optDouble("amount", 0.0),
            fee = row.optDouble("fee", 0.0),
            currency = row.optString("currency_code", "LYD"),
            date = parseTimestamp(row.optString("transfer_date")),
            description = row.optString("description").takeUnless { it == "null" }.orEmpty(),
            reference = existing?.reference.orEmpty()
        )
        db.withTransaction {
            val localId = dao.insertTransfer(transfer)
            dao.upsertSyncEntityMap(SyncEntityMapEntity("transfers", localId, remoteId))
        }
    }

    private fun parseTimestamp(value: String): Long {
        val patterns = listOf("yyyy-MM-dd'T'HH:mm:ss.SSSX", "yyyy-MM-dd'T'HH:mm:ssX", "yyyy-MM-dd")
        for (pattern in patterns) {
            val parsed = runCatching {
                SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.parse(value)?.time
            }.getOrNull()
            if (parsed != null) return parsed
        }
        return System.currentTimeMillis()
    }

    private fun utcTimestamp(value: Long = System.currentTimeMillis()): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date(value))

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
