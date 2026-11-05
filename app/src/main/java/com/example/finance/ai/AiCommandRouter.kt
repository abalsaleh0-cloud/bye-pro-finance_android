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
    fun parseSectionNavigation(query: String): String? {
        val text = query.trim().lowercase(Locale.ROOT)
        val navigationIntent = listOf("افتح", "اذهب إلى", "انتقل إلى", "روح على", "وريني", "اعرض قسم", "open ", "go to ").any(text::contains)
        if (!navigationIntent) return null
        val targets = listOf(
            "dashboard" to listOf("الرئيسية", "لوحة التحكم", "dashboard", "home"),
            "accounts" to listOf("الحسابات", "حساباتي", "accounts"),
            "transactions" to listOf("المعاملات", "العمليات", "transactions"),
            "budgets" to listOf("الميزانيات", "ميزانية", "budgets"),
            "debts" to listOf("الديون", "الدين", "debts"),
            "bills" to listOf("الفواتير", "فاتورة", "bills"),
            "savings" to listOf("الادخار", "التوفير", "savings"),
            "assets" to listOf("الأصول", "اصول", "assets"),
            "transfers" to listOf("التحويلات", "تحويل", "transfers"),
            "reports" to listOf("التقارير", "reports"),
            "household" to listOf("الأسرة", "العائلة", "household"),
            "documents" to listOf("المستندات", "الوثائق", "documents"),
            "notifications" to listOf("الإشعارات", "التنبيهات", "notifications"),
            "settings" to listOf("الإعدادات", "settings"),
            "ai_assistant" to listOf("المساعد الذكي", "ai assistant"),
            "work" to listOf("العمل", "الرواتب", "العمل الحر", "work"),
            "smart_scan" to listOf("المسح الذكي", "المسح", "smart scan"),
            "import_export" to listOf("الاستيراد والتصدير", "الاستيراد", "التصدير", "import export"),
            "admin" to listOf("الإدارة", "لوحة الإدارة", "admin")
        )
        return targets.firstOrNull { (_, aliases) -> aliases.any(text::contains) }?.first
    }

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

        val amountMatch = Regex("""([0-9]+(?:[.,][0-9]{1,2})?)""").find(text) ?: return null
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
            listOf("خضار", "فواكه", "مواد غذائية", "غذائية", "بقالة", "سوبرماركت", "مقاضي", "groceries", "grocery", "vegetables", "produce").any(text::contains) -> "groceries"
            listOf("رصيد", "شحن هاتف", "تعبئة هاتف", "mobile credit", "top up", "phone credit").any(text::contains) -> "mobile_credit"
            listOf("إيجار", "ايجار", "rent").any(text::contains) -> "rent"
            listOf("مواصلات", "تاكسي", "نقل", "transport", "taxi").any(text::contains) -> "transport"
            listOf("كهرباء", "ماء", "إنترنت", "انترنت", "utilities", "internet").any(text::contains) -> "utilities"
            listOf("دواء", "طبيب", "مستشفى", "صحة", "medicine", "doctor", "health").any(text::contains) -> "health"
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
