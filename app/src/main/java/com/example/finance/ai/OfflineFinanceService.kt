package com.example.finance.ai

import com.example.finance.i18n.TextContent
import com.example.finance.model.AccountEntity
import com.example.finance.model.AssetEntity
import com.example.finance.model.BillEntity
import com.example.finance.model.SavingsGoalEntity
import com.example.finance.model.AppLanguage
import com.example.finance.model.BudgetEntity
import com.example.finance.model.DebtEntity
import com.example.finance.model.TransactionEntity
import com.example.finance.model.TransactionStatus
import com.example.finance.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal

data class ExtractedReceiptData(
    val merchant: String,
    val amount: Double,
    val tax: Double,
    val currency: String,
    val category: String,
    val dateStr: String,
    val itemsSummary: String
)

data class AiInsightItem(
    val title: String,
    val description: String,
    val impactType: String,
    val estimatedSavings: Double = 0.0
)

/**
 * Offline-only finance helpers. No API keys, cloud inference, or network requests.
 * Natural-language answers are provided by FinancialAssistantEngine.
 */
class OfflineFinanceService {

    suspend fun parseReceiptImage(
        sampleText: String? = null,
        imageBase64: String? = null,
        language: AppLanguage = AppLanguage.AR
    ): Result<ExtractedReceiptData> = withContext(Dispatchers.IO) {
        if (!imageBase64.isNullOrBlank()) {
            return@withContext Result.failure(
                Exception(
                    if (language == AppLanguage.AR)
                        "تحليل الصور دون اتصال غير مفعّل بعد. يمكنك لصق النص المستخرج من الفاتورة مؤقتاً؛ لم يتم إرسال الصورة إلى أي خدمة خارجية."
                    else
                        "Offline image OCR is not enabled yet. You can paste the extracted invoice text for now; the image was not sent to any external service."
                )
            )
        }

        if (!sampleText.isNullOrBlank()) {
            val merchantMatch = Regex("""(?:مؤسسة|شركة|متجر|اسواق|مطعم|سوبرماركت|Store|Market|Cafe|Shop)[:\s]+([^\n,]+)""", RegexOption.IGNORE_CASE)
                .find(sampleText)?.groupValues?.get(1)
            val totalMatch = Regex("""(?:الإجمالي|المجموع|Total|Amount)[:\s]*([0-9]+(?:\.[0-9]+)?)""", RegexOption.IGNORE_CASE)
                .find(sampleText)?.groupValues?.get(1)?.toDoubleOrNull()
            val taxMatch = Regex("""(?:الضريبة|VAT|Tax)[:\s]*([0-9]+(?:\.[0-9]+)?)""", RegexOption.IGNORE_CASE)
                .find(sampleText)?.groupValues?.get(1)?.toDoubleOrNull()

            if (totalMatch != null && totalMatch > 0.0) {
                return@withContext Result.success(
                    ExtractedReceiptData(
                        merchant = merchantMatch?.trim().orEmpty(),
                        amount = totalMatch,
                        tax = taxMatch ?: 0.0,
                        currency = when {
                            sampleText.contains("LYD", ignoreCase = true) || sampleText.contains("د.ل") -> "LYD"
                            sampleText.contains("USD", ignoreCase = true) || sampleText.contains("$") -> "USD"
                            sampleText.contains("EUR", ignoreCase = true) || sampleText.contains("€") -> "EUR"
                            sampleText.contains("SAR", ignoreCase = true) || sampleText.contains("ر.س") -> "SAR"
                            else -> ""
                        },
                        category = "Other",
                        dateStr = Regex("""\b\d{4}-\d{2}-\d{2}\b""").find(sampleText)?.value.orEmpty(),
                        itemsSummary = ""
                    )
                )
            }
        }

        Result.failure(
            Exception(
                if (language == AppLanguage.AR)
                    "تعذر استخراج بيانات الفاتورة من النص. أدخل البيانات يدوياً أو الصق نصاً أوضح."
                else
                    "Could not extract invoice details from the text. Enter the details manually or paste clearer text."
            )
        )
    }

    fun generateInsights(
        transactions: List<TransactionEntity>,
        budgets: List<BudgetEntity>,
        language: AppLanguage
    ): List<AiInsightItem> {
        val posted = transactions.filter {
            it.status != TransactionStatus.CANCELLED && it.amount.isFinite() && it.amount > 0.0
        }
        val insights = mutableListOf<AiInsightItem>()

        posted.groupBy { it.currency.trim().ifBlank { "UNKNOWN" }.uppercase() }
            .forEach { (currency, rows) ->
                val income = rows.asSequence()
                    .filter { it.type == TransactionType.INCOME || it.type == TransactionType.REFUND }
                    .fold(BigDecimal.ZERO) { total, tx -> total.add(BigDecimal.valueOf(tx.amount)) }
                val expenses = rows.asSequence()
                    .filter { it.type == TransactionType.EXPENSE }
                    .fold(BigDecimal.ZERO) { total, tx -> total.add(BigDecimal.valueOf(tx.amount)) }
                val net = income.subtract(expenses)

                if (income > BigDecimal.ZERO && net > BigDecimal.ZERO) {
                    val rate = net.multiply(BigDecimal("100"))
                        .divide(income, 0, java.math.RoundingMode.HALF_UP).toInt()
                    insights.add(
                        AiInsightItem(
                            title = if (language == AppLanguage.AR) "فائض مسجل — $currency" else "Recorded surplus — $currency",
                            description = if (language == AppLanguage.AR)
                                "الفائض المسجل في هذه العملة هو ${TextContent.money(net, currency, language)}، أي نحو $rate% من الدخل المسجل."
                            else
                                "Your recorded surplus in this currency is ${TextContent.money(net, currency, language)}, about $rate% of recorded income.",
                            impactType = "POSITIVE"
                        )
                    )
                } else if (net < BigDecimal.ZERO) {
                    insights.add(
                        AiInsightItem(
                            title = if (language == AppLanguage.AR) "المصروفات أعلى من الدخل — $currency" else "Expenses exceed income — $currency",
                            description = if (language == AppLanguage.AR)
                                "تتجاوز المصروفات المسجلة الدخل بمقدار ${TextContent.money(net.abs(), currency, language)}. راجع المعاملات المسجلة لهذه العملة."
                            else
                                "Recorded expenses exceed income by ${TextContent.money(net.abs(), currency, language)}. Review the transactions recorded in this currency.",
                            impactType = "WARNING"
                        )
                    )
                }
            }

        budgets.forEach { budget ->
            val limit = BigDecimal.valueOf(budget.limitAmount)
            if (limit <= BigDecimal.ZERO) return@forEach
            val currency = budget.currency.trim().ifBlank { "UNKNOWN" }.uppercase()
            val spent = posted.asSequence()
                .filter { tx ->
                    tx.type == TransactionType.EXPENSE &&
                        tx.currency.equals(currency, ignoreCase = true) &&
                        (budget.category == "All" || tx.category.equals(budget.category, ignoreCase = true)) &&
                        tx.date >= budget.startDate && tx.date <= budget.endDate
                }
                .fold(BigDecimal.ZERO) { total, tx -> total.add(BigDecimal.valueOf(tx.amount)) }
            val percentage = spent.multiply(BigDecimal("100"))
                .divide(limit, 0, java.math.RoundingMode.HALF_UP).toInt()

            if (percentage >= 75) {
                val warning = percentage >= 90
                insights.add(
                    AiInsightItem(
                        title = if (language == AppLanguage.AR)
                            (if (warning) "تنبيه اقتراب تجاوز الميزانية" else "متابعة الميزانية")
                        else
                            (if (warning) "Budget limit warning" else "Budget progress"),
                        description = if (language == AppLanguage.AR)
                            "بلغ الإنفاق المسجل في ${budget.category} نسبة $percentage% من الحد ${TextContent.money(limit, currency, language)}."
                        else
                            "Recorded spending for ${budget.category} has reached $percentage% of the ${TextContent.money(limit, currency, language)} limit.",
                        impactType = if (warning) "WARNING" else "TIP"
                    )
                )
            }
        }

        if (insights.isEmpty()) {
            insights.add(
                AiInsightItem(
                    title = if (language == AppLanguage.AR) "لا توجد مؤشرات كافية بعد" else "Not enough data yet",
                    description = if (language == AppLanguage.AR)
                        "سجّل معاملاتك وميزانياتك للحصول على مؤشرات مبنية على بياناتك الفعلية."
                    else
                        "Record transactions and budgets to receive insights based on your actual financial data.",
                    impactType = "TIP"
                )
            )
        }
        return insights.take(6)
    }
}
