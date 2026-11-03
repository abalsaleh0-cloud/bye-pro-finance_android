package com.example.finance.settings

import com.example.finance.auth.SupabaseAuthRepository
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

/**
 * Mirrors the web app's user_settings table. Requests are scoped to the
 * currently authenticated Supabase user; the publishable key is not a secret.
 */
class SupabaseUserSettingsRepository(
    private val authRepository: SupabaseAuthRepository,
) {
    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun load(): Result<JSONObject?> = withContext(Dispatchers.IO) {
        runCatching {
            val token = authRepository.getValidAccessToken()
                ?: throw IOException("AUTH_REQUIRED")
            val userId = fetchUserId(token)
            val request = Request.Builder()
                .url(
                    "${SupabaseConnection.url.trimEnd('/')}/rest/v1/user_settings" +
                        "?select=theme,locale,timezone,currency_code,date_format,number_format," +
                        "email_notifications,transaction_alerts,bill_reminders,budget_alerts" +
                        "&user_id=eq.$userId&limit=1"
                )
                .header("apikey", SupabaseConnection.publishableKey)
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/json")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IOException("USER_SETTINGS_LOAD_HTTP_${response.code}")
                }
                val rows = JSONArray(raw)
                if (rows.length() == 0) null else rows.getJSONObject(0)
            }
        }
    }

    suspend fun save(settings: JSONObject): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val token = authRepository.getValidAccessToken()
                ?: throw IOException("AUTH_REQUIRED")
            val userId = fetchUserId(token)
            val payload = JSONObject(settings.toString()).put("user_id", userId)
            val request = Request.Builder()
                .url("${SupabaseConnection.url.trimEnd('/')}/rest/v1/user_settings?on_conflict=user_id")
                .header("apikey", SupabaseConnection.publishableKey)
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .header("Prefer", "resolution=merge-duplicates,return=minimal")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val details = response.body?.string().orEmpty().take(300)
                    throw IOException("USER_SETTINGS_SAVE_HTTP_${response.code}: $details")
                }
            }
        }
    }

    private fun fetchUserId(token: String): String {
        val request = Request.Builder()
            .url("${SupabaseConnection.url.trimEnd('/')}/auth/v1/user")
            .header("apikey", SupabaseConnection.publishableKey)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .get()
            .build()
        return client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IOException("AUTH_USER_HTTP_${response.code}")
            JSONObject(raw).optString("id").takeIf(String::isNotBlank)
                ?: throw IOException("AUTH_USER_ID_MISSING")
        }
    }
}
