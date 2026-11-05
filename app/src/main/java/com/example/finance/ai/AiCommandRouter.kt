package com.example.finance.ai

import java.util.Locale

/**
 * Deterministic first-pass command extraction shared by every AI provider.
 * Handles explicit expense/income, debt, work-payroll, rent and navigation
 * requests before a conversational model is invoked. Every write is staged
 * for explicit user confirmation; ambiguous requests fall through safely.
 */
internal data class ParsedTransactionCommand(
    val type: String,
    val amount: Double,
    val currency: String,
    val category: String,
    val description: String
)

internal data class ParsedWorkCommand(
    val type: String,
    val amount: Double,
    val currency: String,
    val sourceName: String?,
    val description: String
)

internal data class ParsedDebtPaymentCommand(
    val amount: Double,
    val currency: String?,
    val personName: String,
    val description: String
)

internal data class ParsedDebtCreateCommand(
    val amount: Double,
    val currency: String,
    val direction: String,
    val personName: String,
    val description: String
)

internal data class ParsedRentCommand(
    val amount: Double,
    val currency: String,
    val isPayment: Boolean,
    val month: String,
    val description: String
)

internal object AiCommandRouter {
    fun parseDebtCreate(query: String, defaultCurrency: String): ParsedDebtCreateCommand? {
        val normalized = normalizeDigits(query).trim()
        val text = normalized.lowercase(Locale.ROOT)
        if (!listOf("سجل دين", "سجّل دين", "اضف دين", "أضف دين", "ضيف دين", "add debt", "record debt").any(text::contains)) return null
        val amountMatch = Regex("""([0-9]+(?:[.,][0-9]{1,2})?)""").find(text) ?: return null
        val amount = amountMatch.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
        if (!amount.isFinite() || amount <= 0.0) return null
        val direction = if (listOf("لي عند", "إلي عند", "الي عند", "مستحق لي", "owed to me", "they owe me").any(text::contains)) "owed_to_me" else "i_owe"
        val before = normalized.substring(0, amountMatch.range.first)
        val after = normalized.substring(amountMatch.range.last + 1)
        val cleanedAfter = after.replace(Regex("""(?i)(?<![\p{L}])(?:دينار|دنانير|د\.ل|lyd|usd|eur|دولار|يورو|ليرة)(?![\p{L}])"""), " ")
            .replace(Regex("""[،,:؛.]+"""), " ").trim()
        val person = Regex("""([\p{L}][\p{L}0-9_-]*(?:\s+[\p{L}][\p{L}0-9_-]*){0,2})""").find(cleanedAfter)?.groupValues?.get(1)
            ?: Regex("""(?:لي عند|مستحق لي من|لدى)\s+([\p{L}][\p{L}0-9_-]*)""").find(before)?.groupValues?.get(1)
            ?: Regex("""(?:ل|إلى|الى)\s*([\p{L}][\p{L}0-9_-]*)""").find(before)?.groupValues?.get(1)
            ?: return null
        val name = person.trim()
        if (name.length < 2) return null
        val currency = when {
            text.contains("usd") || text.contains("دولار") || text.contains("$") -> "USD"
            text.contains("eur") || text.contains("يورو") || text.contains("€") -> "EUR"
            text.contains("syp") || text.contains("ليرة") -> "SYP"
            text.contains("lyd") || text.contains("دينار") || text.contains("د.ل") -> "LYD"
            else -> defaultCurrency.trim().uppercase(Locale.ROOT).takeIf { it.matches(Regex("[A-Z]{3}")) } ?: return null
        }
        return ParsedDebtCreateCommand(amount, currency, direction, name, normalized)
    }

    fun parseRentCommand(query: String, defaultCurrency: String, currentMonth: String): ParsedRentCommand? {
        val normalized = normalizeDigits(query).trim()
        val text = normalized.lowercase(Locale.ROOT)
        if (!listOf("إيجار", "ايجار", "rent").any(text::contains)) return null
        val amountMatch = Regex("""([0-9]+(?:[.,][0-9]{1,2})?)""").find(text) ?: return null
        val amount = amountMatch.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
        if (!amount.isFinite() || amount <= 0.0) return null
        val explicitMonth = Regex("""(20[0-9]{2})[-/]([0-9]{1,2})""").find(text)?.let {
            val month = it.groupValues[2].toIntOrNull() ?: return@let null
            if (month in 1..12) "${it.groupValues[1]}-${month.toString().padStart(2, '0')}" else null
        }
        val currency = when {
            text.contains("usd") || text.contains("دولار") || text.contains("$") -> "USD"
            text.contains("eur") || text.contains("يورو") || text.contains("€") -> "EUR"
            text.contains("syp") || text.contains("ليرة") -> "SYP"
            text.contains("lyd") || text.contains("دينار") || text.contains("د.ل") -> "LYD"
            else -> defaultCurrency.trim().uppercase(Locale.ROOT).takeIf { it.matches(Regex("[A-Z]{3}")) } ?: return null
        }
        val isPayment = listOf("دفعت", "سددت", "سدّدت", "paid", "pay rent").any(text::contains)
        return ParsedRentCommand(amount, currency, isPayment, explicitMonth ?: currentMonth, normalized)
    }
    fun parseDebtPayment(query: String): ParsedDebtPaymentCommand? {
        val normalized = normalizeDigits(query).trim()
        val text = normalized.lowercase(Locale.ROOT)
        val verb = listOf("دفعت", "سددت", "سدّدت", "حولت", "حوّلت", "اعطيت", "أعطيت", "paid", "repaid").any(text::contains)
        if (!verb) return null
        val amountMatch = Regex("""([0-9]+(?:[.,][0-9]{1,2})?)""").find(text) ?: return null
        val amount = amountMatch.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
        if (!amount.isFinite() || amount <= 0.0) return null
        val afterAmount = normalized.substring(amountMatch.range.last + 1)
        val nameMatch = Regex("""(?:ل|إلى|الى)\s*([\p{L}][\p{L}0-9_-]*(?:\s+[\p{L}][\p{L}0-9_-]*){0,2})""").find(afterAmount)
            ?: Regex("""(?:دين|الدين|قرض)\s+(?:ل|إلى|الى)\s*([\p{L}][\p{L}0-9_-]*)""").find(afterAmount)
            ?: return null
        val name = nameMatch.groupValues[1].trim().trimEnd('،', '.', '!')
        if (name.length < 2) return null
        val currency = when {
            text.contains("usd") || text.contains("دولار") || text.contains("$") -> "USD"
            text.contains("eur") || text.contains("يورو") || text.contains("€") -> "EUR"
            text.contains("lyd") || text.contains("دينار") || text.contains("د.ل") -> "LYD"
            text.contains("syp") || text.contains("ليرة") -> "SYP"
            else -> null
        }
        return ParsedDebtPaymentCommand(amount, currency, name, normalized)
    }

    fun parseWorkRecord(query: String, defaultCurrency: String): ParsedWorkCommand? {
        val normalized = normalizeDigits(query).trim()
        val text = normalized.lowercase(Locale.ROOT)
        if (text.isBlank()) return null
        val amountMatch = Regex("""([0-9]+(?:[.,][0-9]{1,2})?)""").find(text) ?: return null
        val amount = amountMatch.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
        if (!amount.isFinite() || amount <= 0.0) return null
        val type = when {
            listOf("سلفة", "سلفه", "سلفة راتب", "advance", "salary advance").any(text::contains) -> "SALARY_ADVANCE"
            listOf("دفعة راتب", "دفعة من الراتب", "استلمت الراتب", "استلمت راتبي", "قبضت الراتب", "اخذت الراتب", "أخذت الراتب", "salary payment", "received salary", "got paid").any(text::contains) -> "SALARY_PAYMENT"
            listOf("بدل وظيفي", "بدل", "allowance").any(text::contains) -> "SALARY_ALLOWANCE"
            listOf("مكافأة", "مكافاه", "bonus").any(text::contains) -> "SALARY_BONUS"
            listOf("عمل إضافي", "عمل اضافي", "أوفر تايم", "overtime").any(text::contains) -> "SALARY_OVERTIME"
            listOf("خصم راتب", "خصم من الراتب", "salary deduction").any(text::contains) -> "SALARY_DEDUCTION"
            else -> return null
        }
        val sourceMatch = Regex("""(?:من|عند|لدى|في)\s+([\p{L}][\p{L}0-9_-]*(?:\s+[\p{L}][\p{L}0-9_-]*){0,3})""").find(normalized)
        val sourceName = sourceMatch?.groupValues?.get(1)?.trim()
        val currency = when {
            text.contains("usd") || text.contains("دولار") || text.contains("$") -> "USD"
            text.contains("eur") || text.contains("يورو") || text.contains("€") -> "EUR"
            text.contains("syp") || text.contains("ليرة") -> "SYP"
            text.contains("lyd") || text.contains("دينار") || text.contains("د.ل") -> "LYD"
            else -> defaultCurrency.trim().uppercase(Locale.ROOT).takeIf { it.matches(Regex("[A-Z]{3}")) } ?: return null
        }
        return ParsedWorkCommand(type, amount, currency, sourceName, normalized)
    }

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
