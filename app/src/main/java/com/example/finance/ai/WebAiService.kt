package com.example.finance.ai

import android.content.Context
import com.example.finance.auth.SupabaseAuthRepository
import com.example.finance.model.AppLanguage
import com.example.finance.network.AiRequest
import com.example.finance.network.FinanceApiProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ExtractedReceiptData(
    val merchant: String = "",
    val amount: Double = 0.0,
    val tax: Double = 0.0,
    val category: String = "",
    val itemsSummary: String = "",
    val currency: String = "LYD"
)

/** Exact BYE PRO FINANCE Web AI engine; Android does not use Gemini or another AI provider. */
class WebAiService(context: Context) {
    private val auth = SupabaseAuthRepository(context.applicationContext)

    suspend fun askAssistant(query: String, language: AppLanguage): String = withContext(Dispatchers.IO) {
        val token = auth.accessToken()?.takeIf { it.isNotBlank() } ?: return@withContext fallback(language)
        runCatching {
            val result = FinanceApiProvider.api.ai("Bearer $token", AiRequest(query.trim()))
            result.data?.get("answer")?.toString()?.takeIf { it.isNotBlank() } ?: fallback(language)
        }.getOrElse { fallback(language) }
    }

    suspend fun parseReceiptImage(sampleText: String?, imageBase64: String?, language: AppLanguage): Result<ExtractedReceiptData> =
        Result.failure(UnsupportedOperationException("Receipt extraction is not part of the Web AI engine"))

    private fun fallback(language: AppLanguage) = if (language == AppLanguage.AR) {
        "تعذر الوصول إلى مساعد BYE PRO FINANCE على الويب حالياً."
    } else {
        "The BYE PRO FINANCE Web AI assistant is currently unavailable."
    }
}
