package com.example.finance.auth

import android.content.Context
import com.example.finance.supabase.SupabaseConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

sealed interface AuthOutcome {
    data object Authenticated : AuthOutcome
    data object NeedsEmailConfirmation : AuthOutcome
}

class SupabaseAuthRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val client = OkHttpClient()

    fun hasValidSession(): Boolean {
        val token = preferences.getString(KEY_ACCESS_TOKEN, null)
        val expiry = preferences.getLong(KEY_EXPIRES_AT, 0L)
        return !token.isNullOrBlank() && expiry > System.currentTimeMillis() / 1000L + 60L
    }

    suspend fun signIn(email: String, password: String): Result<AuthOutcome> =
        authenticate("/auth/v1/token?grant_type=password",
            JSONObject().put("email", email.trim()).put("password", password), false)

    suspend fun signUp(name: String, username: String, email: String, password: String): Result<AuthOutcome> =
        authenticate("/auth/v1/signup", JSONObject()
            .put("email", email.trim())
            .put("password", password)
            .put("data", JSONObject().put("full_name", name.trim()).put("username", username.trim())), true)

    private suspend fun authenticate(path: String, payload: JSONObject, signup: Boolean): Result<AuthOutcome> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(SupabaseConnection.url.isNotBlank()) { "Supabase URL is missing" }
                require(SupabaseConnection.publishableKey.isNotBlank()) { "Supabase publishable key is missing" }
                val request = Request.Builder()
                    .url(SupabaseConnection.url.trimEnd('/') + path)
                    .header("apikey", SupabaseConnection.publishableKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()
                client.newCall(request).execute().use { response ->
                    val raw = response.body?.string().orEmpty()
                    val body = runCatching { JSONObject(raw) }.getOrNull()
                    if (!response.isSuccessful) {
                        val error = body?.optString("msg")?.takeIf(String::isNotBlank)
                            ?: body?.optString("error_description")?.takeIf(String::isNotBlank)
                            ?: body?.optString("message")?.takeIf(String::isNotBlank)
                            ?: "Authentication failed (HTTP ${response.code})"
                        throw IOException(error)
                    }
                    val accessToken = body?.optString("access_token").orEmpty()
                    if (accessToken.isNotBlank()) {
                        val now = System.currentTimeMillis() / 1000L
                        val expiry = body?.optLong("expires_at", 0L)?.takeIf { it > 0L }
                            ?: now + (body?.optLong("expires_in", 3600L) ?: 3600L)
                        preferences.edit()
                            .putString(KEY_ACCESS_TOKEN, accessToken)
                            .putString(KEY_REFRESH_TOKEN, body?.optString("refresh_token").orEmpty())
                            .putLong(KEY_EXPIRES_AT, expiry)
                            .apply()
                        AuthOutcome.Authenticated
                    } else if (signup) AuthOutcome.NeedsEmailConfirmation
                    else throw IOException("The server did not return a valid session")
                }
            }
        }

    companion object {
        private const val PREFS = "bye_pro_supabase_auth"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRES_AT = "expires_at"
    }
}
