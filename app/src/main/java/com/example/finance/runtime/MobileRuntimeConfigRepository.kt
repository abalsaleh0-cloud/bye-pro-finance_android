package com.example.finance.runtime

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Fetches signed-off, non-executable runtime policy from the BYE PRO API.
 * The last valid response is cached so the app remains usable offline.
 *
 * This mechanism intentionally does not download or execute code, dex, scripts,
 * APKs, or native libraries.
 */
data class MobileRuntimeConfig(
    val schemaVersion: Int = 1,
    val configVersion: Int = 0,
    val minimumSupportedVersionCode: Int = 0,
    val maintenanceMode: Boolean = false,
    val maintenanceMessageAr: String = "",
    val maintenanceMessageEn: String = "",
    val remoteAnnouncements: Boolean = false,
    val remoteUiText: Boolean = false,
    val experimentalFeatures: Boolean = false
)

class MobileRuntimeConfigRepository(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(12, TimeUnit.SECONDS)
        .build()

    fun cached(): MobileRuntimeConfig =
        preferences.getString(CONFIG_JSON_KEY, null)?.let(::parseConfig) ?: MobileRuntimeConfig()

    suspend fun refresh(): MobileRuntimeConfig = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(CONFIG_URL)
            .get()
            .header("Accept", "application/json")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext cached()
                val body = response.body?.string() ?: return@withContext cached()
                val parsed = parseConfig(body) ?: return@withContext cached()
                if (parsed.schemaVersion != SUPPORTED_SCHEMA_VERSION) {
                    return@withContext cached()
                }

                val previous = cached()
                if (parsed.configVersion >= previous.configVersion) {
                    preferences.edit().putString(CONFIG_JSON_KEY, body).apply()
                    parsed
                } else {
                    previous
                }
            }
        } catch (_: Exception) {
            cached()
        }
    }

    private fun parseConfig(raw: String): MobileRuntimeConfig? = try {
        val root = JSONObject(raw)
        val features = root.optJSONObject("features") ?: JSONObject()
        MobileRuntimeConfig(
            schemaVersion = root.optInt("schema_version", 0),
            configVersion = root.optInt("config_version", 0),
            minimumSupportedVersionCode = root.optInt("minimum_supported_version_code", 0),
            maintenanceMode = root.optBoolean("maintenance_mode", false),
            maintenanceMessageAr = root.optString("maintenance_message_ar", ""),
            maintenanceMessageEn = root.optString("maintenance_message_en", ""),
            remoteAnnouncements = features.optBoolean("remote_announcements", false),
            remoteUiText = features.optBoolean("remote_ui_text", false),
            experimentalFeatures = features.optBoolean("experimental_features", false)
        )
    } catch (_: Exception) {
        null
    }

    companion object {
        private const val CONFIG_URL =
            "https://bye-pro-finance.vercel.app/api/v1/mobile/config"
        private const val PREFERENCES_NAME = "bye_pro_mobile_runtime_config"
        private const val CONFIG_JSON_KEY = "last_valid_config"
        private const val SUPPORTED_SCHEMA_VERSION = 1
    }
}
