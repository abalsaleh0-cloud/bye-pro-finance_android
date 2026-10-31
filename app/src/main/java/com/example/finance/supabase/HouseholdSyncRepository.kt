package com.example.finance.supabase

import com.example.finance.auth.SupabaseAuthRepository
import com.example.finance.data.FinanceDao
import com.example.finance.model.HouseholdBudgetEntity
import com.example.finance.model.HouseholdExpenseEntity
import com.example.finance.model.HouseholdMemberEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HouseholdSyncRepository(
    private val dao: FinanceDao,
    private val auth: SupabaseAuthRepository
) {
    private val client = OkHttpClient()
    private val json = "application/json; charset=utf-8".toMediaType()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    suspend fun sync(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val token = auth.accessToken()?.takeIf { it.isNotBlank() } ?: throw IOException("No active Supabase session")
            auth.currentUserId().getOrThrow()
            syncMembers(token)
            syncBudgets(token)
            syncExpenses(token)
        }
    }

    private fun requestBuilder(path: String, token: String): Request.Builder = Request.Builder()
        .url(SupabaseConnection.url.trimEnd('/') + "/rest/v1/" + path)
        .header("apikey", SupabaseConnection.publishableKey)
        .header("Authorization", "Bearer $token")
        .header("Accept", "application/json")

    private fun execute(request: Request): String {
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IOException("Supabase HTTP ${response.code}: $body")
            return body
        }
    }

    private fun syncMembers(token: String) {
        val body = execute(requestBuilder("household_members?select=id,name,role,monthly_contribution,is_active&order=name.asc", token).get().build())
        val remote = JSONArray(body)
        for (i in 0 until remote.length()) {
            val r = remote.getJSONObject(i)
            val remoteId = r.getString("id")
            val local = dao.getHouseholdMemberByRemoteId(remoteId)
            val value = HouseholdMemberEntity(
                id = local?.id ?: 0,
                name = r.getString("name"), role = r.optString("role", "family"),
                monthlyContribution = r.optDouble("monthly_contribution", 0.0),
                isActive = r.optBoolean("is_active", true), remoteId = remoteId
            )
            if (local == null) dao.insertHouseholdMember(value) else dao.updateHouseholdMember(value)
        }
        for (local in dao.getAllHouseholdMembersSnapshot()) {
            val payload = JSONObject().put("name", local.name).put("role", local.role)
                .put("monthly_contribution", local.monthlyContribution).put("is_active", local.isActive)
            if (local.remoteId.isBlank()) {
                val created = execute(requestBuilder("household_members", token).header("Prefer", "return=representation")
                    .post(payload.toString().toRequestBody(json)).build())
                val id = JSONArray(created).getJSONObject(0).getString("id")
                dao.setHouseholdMemberRemoteId(local.id, id)
            } else {
                execute(requestBuilder("household_members?id=eq.${local.remoteId}", token).header("Prefer", "return=minimal")
                    .patch(payload.toString().toRequestBody(json)).build())
            }
        }
    }

    private fun syncBudgets(token: String) {
        val body = execute(requestBuilder("household_budgets?select=id,name,amount,currency_code,period,start_date,end_date,is_active&order=start_date.desc", token).get().build())
        val remote = JSONArray(body)
        for (i in 0 until remote.length()) {
            val r = remote.getJSONObject(i); val remoteId = r.getString("id")
            val local = dao.getHouseholdBudgetByRemoteId(remoteId)
            val value = HouseholdBudgetEntity(
                id = local?.id ?: 0, name = r.getString("name"), amount = r.getDouble("amount"),
                currencyCode = r.optString("currency_code", "LYD"), period = r.optString("period", "monthly"),
                startDate = parseDate(r.optString("start_date")), endDate = r.optString("end_date").takeIf { it.isNotBlank() }?.let(::parseDate),
                isActive = r.optBoolean("is_active", true), remoteId = remoteId
            )
            if (local == null) dao.insertHouseholdBudget(value) else dao.updateHouseholdBudget(value)
        }
        for (local in dao.getAllHouseholdBudgetsSnapshot()) {
            val payload = JSONObject().put("name", local.name).put("amount", local.amount).put("currency_code", local.currencyCode)
                .put("period", local.period).put("start_date", formatDate(local.startDate)).put("is_active", local.isActive)
            local.endDate?.let { payload.put("end_date", formatDate(it)) } ?: payload.put("end_date", JSONObject.NULL)
            if (local.remoteId.isBlank()) {
                val created = execute(requestBuilder("household_budgets", token).header("Prefer", "return=representation")
                    .post(payload.toString().toRequestBody(json)).build())
                dao.setHouseholdBudgetRemoteId(local.id, JSONArray(created).getJSONObject(0).getString("id"))
            } else execute(requestBuilder("household_budgets?id=eq.${local.remoteId}", token).header("Prefer", "return=minimal")
                .patch(payload.toString().toRequestBody(json)).build())
        }
    }

    private fun syncExpenses(token: String) {
        val body = execute(requestBuilder("household_expenses?select=id,member_id,category,description,amount,currency_code,expense_date,is_recurring,notes&order=expense_date.desc", token).get().build())
        val remote = JSONArray(body)
        for (i in 0 until remote.length()) {
            val r = remote.getJSONObject(i); val remoteId = r.getString("id")
            val local = dao.getHouseholdExpenseByRemoteId(remoteId)
            val payerId = r.optString("member_id").takeIf { it.isNotBlank() }?.let { memberRemote -> dao.getHouseholdMemberByRemoteId(memberRemote)?.id }
            val value = HouseholdExpenseEntity(
                id = local?.id ?: 0, title = r.optString("description").ifBlank { r.optString("category", "عام") }, amount = r.getDouble("amount"),
                payerMemberId = payerId, category = r.optString("category", "عام"), currency = r.optString("currency_code", "LYD"),
                date = parseDate(r.optString("expense_date")), isRecurring = r.optBoolean("is_recurring", false), notes = r.optString("notes", ""), remoteId = remoteId
            )
            if (local == null) dao.insertHouseholdExpense(value) else dao.updateHouseholdExpense(value)
        }
        for (local in dao.getAllHouseholdExpensesSnapshot()) {
            val memberRemote = local.payerMemberId?.let { dao.getHouseholdMemberById(it)?.remoteId?.takeIf(String::isNotBlank) }
            val payload = JSONObject().put("member_id", memberRemote ?: JSONObject.NULL).put("category", local.category)
                .put("description", local.title).put("amount", local.amount).put("currency_code", local.currency)
                .put("expense_date", formatDate(local.date)).put("is_recurring", local.isRecurring).put("notes", local.notes)
            if (local.remoteId.isBlank()) {
                val created = execute(requestBuilder("household_expenses", token).header("Prefer", "return=representation")
                    .post(payload.toString().toRequestBody(json)).build())
                dao.setHouseholdExpenseRemoteId(local.id, JSONArray(created).getJSONObject(0).getString("id"))
            } else execute(requestBuilder("household_expenses?id=eq.${local.remoteId}", token).header("Prefer", "return=minimal")
                .patch(payload.toString().toRequestBody(json)).build())
        }
    }

    suspend fun delete(table: String, remoteId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (remoteId.isBlank()) return@runCatching Unit
            val token = auth.accessToken()?.takeIf { it.isNotBlank() } ?: throw IOException("No active Supabase session")
            execute(requestBuilder("$table?id=eq.$remoteId", token).delete().build())
        }
    }

    private fun formatDate(value: Long): String = synchronized(dateFormat) { dateFormat.format(Date(value)) }
    private fun parseDate(value: String): Long = synchronized(dateFormat) { dateFormat.parse(value)?.time ?: System.currentTimeMillis() }
}
