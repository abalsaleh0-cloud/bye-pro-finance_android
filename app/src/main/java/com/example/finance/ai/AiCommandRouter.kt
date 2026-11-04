package com.example.finance.ai

import java.util.Locale

/**
 * Deterministic first-pass command extraction shared by every AI provider.
 * It only recognizes unambiguous transaction create requests; all other
 * messages continue through the selected conversational model.
 */
internal data class ParsedTransactionCommand(
    val type: String,
    val amount: Double,
    val currency: String,
    val category: String,
    val description: String
)

internal object AiCommandRouter {
    fun parseTransaction(query: String, defaultCurrency: String): ParsedTransactionCommand? {
        val normalized = normalizeDigits(query).trim()
        if (normalized.isBlank()) return null
        val text = normalized.lowercase(Locale.ROOT)
        if (text.contains("كيف") || text.contains("ما طريقة") || text.contains("كيف أ") || text.contains("how do i")) return null

        val expenseVerb = listOf(
            "دفعت", "دفعنا", "صرفت", "صرفنا", "اشتريت", "اشترينا",
            "سجل مصروف", "سجلت مصروف", "اضف مصروف", "أضف مصروف",
            "add expense", "record expense", "spent", "paid"
        ).any(text::contains)
        val incomeVerb = listOf(
            "استلمت", "استلمنا", "قبضت", "قبضنا", "وصلني", "دخل لي",
            "سجل إيراد", "سجلت إيراد", "اضف دخل", "أضف دخل",
            "add income", "record income", "received", "earned"
        ).any(text::contains)
        if (expenseVerb == incomeVerb) return null

        val amountMatch = Regex("""(?<![\\p{L}])([0-9]+(?:[.,][0-9]{1,2})?)""").find(text) ?: return null
        val amount = amountMatch.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
        if (!amount.isFinite() || amount <= 0.0) return null

        val currency = when {
            text.contains("usd") || text.contains("دولار") || text.contains("$") -> "USD"
            text.contains("eur") || text.contains("يورو") || text.contains("€") -> "EUR"
            text.contains("gbp") || text.contains("جنيه استرليني") || text.contains("£") -> "GBP"
            text.contains("sar") || text.contains("ريال سعودي") -> "SAR"
            text.contains("aed") || text.contains("درهم") -> "AED"
            text.contains("egp") || text.contains("جنيه مصري") -> "EGP"
            text.contains("lyd") || text.contains("دينار") || text.contains("د.ل") -> "LYD"
            else -> defaultCurrency.trim().uppercase(Locale.ROOT).takeIf { it.matches(Regex("[A-Z]{3}")) } ?: return null
        }

        val category = when {
            listOf("بنزين", "وقود", "محروقات", "fuel", "petrol").any(text::contains) -> "fuel"
            listOf("طعام", "أكل", "اكل", "مطعم", "food", "restaurant").any(text::contains) -> "food"
            listOf("بقالة", "سوبرماركت", "groceries", "grocery").any(text::contains) -> "groceries"
            listOf("مواصلات", "تاكسي", "نقل", "transport", "taxi").any(text::contains) -> "transport"
            listOf("كهرباء", "ماء", "إنترنت", "انترنت", "utilities", "internet").any(text::contains) -> "utilities"
            else -> "other"
        }
        return ParsedTransactionCommand(
            type = if (expenseVerb) "EXPENSE" else "INCOME",
            amount = amount,
            currency = currency,
            category = category,
            description = normalized
        )
    }

    private fun normalizeDigits(value: String): String = buildString(value.length) {
        value.forEach { char ->
            append(
                when (char) {
                    in '٠'..'٩' -> ('0'.code + (char.code - '٠'.code)).toChar()
                    in '۰'..'۹' -> ('0'.code + (char.code - '۰'.code)).toChar()
                    else -> char
                }
            )
        }
    }
}
