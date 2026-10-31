package com.example.finance.supabase

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

object SupabaseConnection {
    private val client = OkHttpClient()

    val url: String = BuildConfig.SUPABASE_URL
    val publishableKey: String = BuildConfig.SUPABASE_PUBLISHABLE_KEY

    suspend fun checkConnection(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(url.isNotBlank()) { "Supabase URL is missing" }
            require(publishableKey.isNotBlank()) { "Supabase publishable key is missing" }

            val request = Request.Builder()
                .url("$url/auth/v1/settings")
                .header("apikey", publishableKey)
                .header("Accept", "application/json")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Supabase connection failed: HTTP ${response.code}")
                }
            }
        }
    }
}
