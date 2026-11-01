package com.example.finance.ai

import java.util.Locale

data class ExpenseVoiceCommand(
    val amount: Double,
    val description: String,
    val category: String
)

/** Fast, deterministic parser for explicit expense-recording commands. */
object FinanceVoiceCommandParser {
    fun parseExpense(text: String): ExpenseVoiceCommand? {
        val normalized = normalizeDigits(text).trim()
        val lower = normalized.lowercase(Locale.ROOT)
        val explicitExpense = listOf(
            "دفعت", "دفعت مبلغ", "سددت", "صرف", "مصروف", "اشتريت",
            "paid", "spent", "payed"
        ).any(lower::contains)
        if (!explicitExpense) return null

        val amountMatch = Regex("""(?<!\\d)(\\d+(?:[.,]\\d{1,2})?)(?!\\d)""").find(normalized) ?: return null
        val amountText = amountMatch.value
        val amount = amountText.replace(',', '.').toDoubleOrNull()
            ?.takeIf { it.isFinite() && it > 0.0 } ?: return null

        val category = when {
            listOf("إيجار", "ايجار", "أجرة المنزل", "اجرة المنزل", "السكن", "rent", "housing").any(lower::contains) -> "Housing"
            else -> return null
        }
        val description = normalized
            .removeRange(amountMatch.range)
            .replace(Regex("""(?i)\\b(paid|spent|payed)\\b"""), " ")
            .replace(Regex("""دفعت(\\s+مبلغ)?|سددت|صرف(ت)?|مصروف|اشتريت"""), " ")
            .replace(Regex("""[،,:؛]"""), " ")
            .replace(Regex("""\\s+"""), " ")
            .trim()
            .ifBlank { if (category == "Housing") "إيجار المنزل" else category }

        return ExpenseVoiceCommand(amount, description, category)
    }

    private fun normalizeDigits(value: String): String = buildString(value.length) {
        value.forEach { ch ->
            append(
                when (ch) {
                    in '٠'..'٩' -> ('0'.code + (ch.code - '٠'.code)).toChar()
                    in '۰'..'۹' -> ('0'.code + (ch.code - '۰'.code)).toChar()
                    else -> ch
                }
            )
        }
    }
}
