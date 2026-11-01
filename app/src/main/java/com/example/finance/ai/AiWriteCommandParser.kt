package com.example.finance.ai

import com.example.finance.model.AccountEntity
import java.util.Locale

sealed interface AiWriteCommand {
    data class RecordTransaction(
        val type: TransactionWriteType,
        val amount: Double,
        val currency: String,
        val category: String,
        val description: String,
        val account: AccountEntity?
    ) : AiWriteCommand
}

enum class TransactionWriteType { EXPENSE, INCOME }

object AiWriteCommandParser {
    private val amountRegex = Regex("(?<![\\d٠-٩])([0-9٠-٩]+(?:[.,][0-9٠-٩]+)?)(?![\\d٠-٩])")

    fun parse(query: String, accounts: List<AccountEntity>, defaultCurrency: String): AiWriteCommand? {
        val normalized = query.trim().lowercase(Locale.ROOT)
        val isRecord = listOf("سجل", "سجّل", "سجل لي", "دوّن", "دون", "أضف", "اضف", "record", "log", "add", "save").any { normalized.startsWith(it) || normalized.contains(" $it ") }
        if (!isRecord) return null

        val type = when {
            listOf("مصروف", "مصروفات", "صرف", "أنفق", "انفق", "expense", "spending").any(normalized::contains) -> TransactionWriteType.EXPENSE
            listOf("دخل", "راتب", "إيراد", "ايراد", "income", "salary", "revenue").any(normalized::contains) -> TransactionWriteType.INCOME
            else -> return null
        }

        val amountMatch = amountRegex.find(normalized) ?: return null
        val amount = amountMatch.groupValues[1]
            .replace('٠', '0').replace('١', '1').replace('٢', '2').replace('٣', '3').replace('٤', '4')
            .replace('٥', '5').replace('٦', '6').replace('٧', '7').replace('٨', '8').replace('٩', '9')
            .replace(',', '.')
            .toDoubleOrNull() ?: return null
        if (!amount.isFinite() || amount <= 0.0) return null

        val currency = detectCurrency(normalized) ?: defaultCurrency.uppercase(Locale.ROOT)
        val explicitAccount = extractAccount(normalized, accounts)
        val matchingAccounts = accounts.filter { !it.isArchived && it.currency.equals(currency, true) }
        val account = explicitAccount ?: matchingAccounts.singleOrNull()

        val remainder = normalized
            .removeRange(amountMatch.range)
            .replace(Regex("\\b(سجل|سجّل|سجل لي|دوّن|دون|أضف|اضف|record|log|add|save)\\b"), " ")
            .replace(Regex("\\b(مصروف|مصروفات|صرف|أنفق|انفق|expense|spending|دخل|راتب|إيراد|ايراد|income|salary|revenue)\\b"), " ")
            .replace(Regex("\\b(دينار|دينار ليبي|ليبي|dinar|lyd|دولار|usd|يورو|eur|جنيه|gbp|ريال|sar|درهم|aed)\\b"), " ")
            .replace(Regex("\\b(من حساب|على حساب|حساب)\\b"), " ")
            .replace(Regex("\\s+"), " ")
            .trim(' ', '-', ':', '،', ',')

        val category = remainder.ifBlank { if (type == TransactionWriteType.EXPENSE) "عام" else "دخل" }
        val description = when {
            type == TransactionWriteType.EXPENSE -> "مصروف: $category"
            else -> "دخل: $category"
        }

        return AiWriteCommand.RecordTransaction(type, amount, currency, category, description, account)
    }

    private fun detectCurrency(text: String): String? = when {
        listOf("lyd", "دينار", "دينار ليبي", "ليبي").any(text::contains) -> "LYD"
        listOf("usd", "دولار", "دولار أمريكي", "دولار امريكي").any(text::contains) -> "USD"
        listOf("eur", "يورو").any(text::contains) -> "EUR"
        listOf("gbp", "جنيه إسترليني", "جنيه استرليني").any(text::contains) -> "GBP"
        listOf("sar", "ريال سعودي").any(text::contains) -> "SAR"
        listOf("aed", "درهم إماراتي", "درهم اماراتي").any(text::contains) -> "AED"
        else -> null
    }

    private fun extractAccount(text: String, accounts: List<AccountEntity>): AccountEntity? {
        val marker = Regex("(?:من حساب|على حساب|حساب)\\s+(.+)$").find(text) ?: return null
        val name = marker.groupValues[1].trim().trimEnd('.', '،', ',')
        return accounts.firstOrNull { !it.isArchived && it.name.lowercase(Locale.ROOT) == name }
            ?: accounts.firstOrNull { !it.isArchived && name.contains(it.name.lowercase(Locale.ROOT)) }
    }
}
