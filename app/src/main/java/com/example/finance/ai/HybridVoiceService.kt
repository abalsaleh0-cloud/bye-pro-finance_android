package com.example.finance.ai

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.util.concurrent.TimeUnit

/**
 * Stores provider credentials encrypted with an Android Keystore AES-GCM key.
 * The API key is never written to plaintext preferences or logs.
 */
class SecureVoiceSettings(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "bye_pro_voice_secure",
        Context.MODE_PRIVATE
    )

    fun saveApiKey(value: String) {
        val normalized = value.trim()
        if (normalized.isEmpty()) {
            preferences.edit().remove(KEY_CIPHERTEXT).remove(KEY_IV).apply()
            return
        }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(normalized.toByteArray(Charsets.UTF_8))
        preferences.edit()
            .putString(KEY_CIPHERTEXT, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .putString(KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .apply()
    }

    fun readApiKey(): String? {
        val encrypted = preferences.getString(KEY_CIPHERTEXT, null) ?: return null
        val iv = preferences.getString(KEY_IV, null) ?: return null
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP))
            )
            String(cipher.doFinal(Base64.decode(encrypted, Base64.NO_WRAP)), Charsets.UTF_8)
                .takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    fun saveVoiceId(value: String) {
        preferences.edit().putString(VOICE_ID, value.trim()).apply()
    }

    fun readVoiceId(): String = preferences.getString(VOICE_ID, DEFAULT_VOICE_ID) ?: DEFAULT_VOICE_ID

    fun hasApiKey(): Boolean = !readApiKey().isNullOrBlank()

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val KEY_ALIAS = "bye-pro-voice-api-key-v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_CIPHERTEXT = "elevenlabs_key_ciphertext"
        const val KEY_IV = "elevenlabs_key_iv"
        const val VOICE_ID = "elevenlabs_voice_id"
        const val DEFAULT_VOICE_ID = "EXAVITQu4vr4xnSDxMaL"
    }
}

/**
 * Optional cloud speech synthesis. Returns null when no cloud credential is
 * configured; callers can then use the on-device speech engine as an offline
 * fallback. Audio is fetched only for the text the user asked to hear.
 */
enum class ElevenLabsConnectionResult {
    CONNECTED,
    MISSING_KEY,
    INVALID_KEY,
    FORBIDDEN,
    RATE_LIMITED,
    SERVER_ERROR,
    BAD_REQUEST,
    NOT_FOUND,
    INVALID_REQUEST,
    OTHER_HTTP_RESPONSE,
    TIMEOUT,
    TLS_ERROR,
    DNS_ERROR,
    NETWORK_ERROR,
    REQUEST_FAILED
}

/** Safe diagnostic metadata only; never contains the API key or raw response body. */
data class ElevenLabsConnectionCheck(
    val result: ElevenLabsConnectionResult,
    val errorCode: String? = null,
    val requestId: String? = null
)

class ElevenLabsSpeechProvider(context: Context) {
    private val settings = SecureVoiceSettings(context)
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .callTimeout(120, TimeUnit.SECONDS)
        .build()

    /**
     * Checks the stored credential against ElevenLabs' authenticated user endpoint.
     * Provider response bodies are intentionally not exposed to the UI or logs.
     */
    suspend fun testConnection(): ElevenLabsConnectionCheck = withContext(Dispatchers.IO) {
        val apiKey = settings.readApiKey()
            ?: return@withContext ElevenLabsConnectionCheck(ElevenLabsConnectionResult.MISSING_KEY)
        val request = Request.Builder()
            .url("https://api.elevenlabs.io/v1/user")
            .header("xi-api-key", apiKey)
            .header("Accept", "application/json")
            .get()
            .build()
        try {
            client.newCall(request).execute().use { response ->
                // Read only safe diagnostic fields. Never expose or log the raw body.
                val responseBody = response.body?.string().orEmpty().take(8_192)
                val detail = runCatching {
                    JSONObject(responseBody).optJSONObject("detail")
                }.getOrNull()
                val errorCode = (detail?.optString("code")?.takeIf { it.isNotBlank() && it != "null" }
                    ?: detail?.optString("status")?.takeIf { it.isNotBlank() && it != "null" })
                    ?.takeIf { it.matches(Regex("[A-Za-z0-9_]{1,64}")) }
                val requestId = detail?.optString("request_id")
                    ?.takeIf { it.matches(Regex("[A-Fa-f0-9-]{8,64}")) }
                val result = when {
                    response.isSuccessful -> ElevenLabsConnectionResult.CONNECTED
                    response.code == 400 -> ElevenLabsConnectionResult.BAD_REQUEST
                    response.code == 401 -> ElevenLabsConnectionResult.INVALID_KEY
                    response.code == 403 -> ElevenLabsConnectionResult.FORBIDDEN
                    response.code == 404 -> ElevenLabsConnectionResult.NOT_FOUND
                    response.code == 422 -> ElevenLabsConnectionResult.INVALID_REQUEST
                    response.code == 429 -> ElevenLabsConnectionResult.RATE_LIMITED
                    response.code in 500..599 -> ElevenLabsConnectionResult.SERVER_ERROR
                    else -> ElevenLabsConnectionResult.OTHER_HTTP_RESPONSE
                }
                ElevenLabsConnectionCheck(result, errorCode, requestId)
            }
        } catch (_: SocketTimeoutException) {
            ElevenLabsConnectionCheck(ElevenLabsConnectionResult.TIMEOUT)
        } catch (_: SSLException) {
            ElevenLabsConnectionCheck(ElevenLabsConnectionResult.TLS_ERROR)
        } catch (_: UnknownHostException) {
            ElevenLabsConnectionCheck(ElevenLabsConnectionResult.DNS_ERROR)
        } catch (_: IOException) {
            ElevenLabsConnectionCheck(ElevenLabsConnectionResult.NETWORK_ERROR)
        } catch (_: Exception) {
            ElevenLabsConnectionCheck(ElevenLabsConnectionResult.REQUEST_FAILED)
        }
    }

    suspend fun synthesize(text: String): ByteArray? = withContext(Dispatchers.IO) {
        val apiKey = settings.readApiKey() ?: return@withContext null
        val voiceId = settings.readVoiceId().ifBlank { return@withContext null }
        val cleanText = text.trim().take(MAX_TEXT_LENGTH)
        if (cleanText.isBlank()) return@withContext null

        val payload = JSONObject()
            .put("text", cleanText)
            .put("model_id", "eleven_multilingual_v2")
            .put(
                "voice_settings",
                JSONObject()
                    .put("stability", 0.48)
                    .put("similarity_boost", 0.78)
                    .put("style", 0.22)
                    .put("use_speaker_boost", true)
            )
            .toString()

        val request = Request.Builder()
            .url("https://api.elevenlabs.io/v1/text-to-speech/$voiceId?output_format=mp3_44100_128")
            .header("xi-api-key", apiKey)
            .header("Accept", "audio/mpeg")
            .post(payload.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val detail = response.body?.string()?.take(240).orEmpty()
                throw IOException("تعذر توليد الصوت السحابي (HTTP ${response.code}). ${detail}")
            }
            val bytes = response.body?.bytes() ?: throw IOException("استجابة الصوت فارغة.")
            if (bytes.isEmpty()) throw IOException("استجابة الصوت فارغة.")
            bytes
        }
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        const val MAX_TEXT_LENGTH = 5_000
    }
}
