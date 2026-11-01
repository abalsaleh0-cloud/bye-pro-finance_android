package com.example.finance.ai

import com.example.BuildConfig
import com.example.finance.engine.LedgerSummary
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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

data class ExtractedReceiptData(
    val merchant: String,
    val amount: Double,
    val tax: Double,
    val currency: String,
    val category: String,
    val dateStr: String,
    val itemsSummary: String
)

data class AiTransactionIntent(
    val intent: String,
    val amount: Double?,
    val currency: String?,
    val category: String?,
    val description: String?,
    val accountName: String?,
    val dateIso: String?,
    val confidence: Double,
    val clarification: String?
)

data class AiInsightItem(
    val title: String,
    val description: String,
    val impactType: String, // POSITIVE, WARNING, TIP
    val estimatedSavings: Double = 0.0
)

class GeminiFinanceService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val apiKey: String
        get() = try {
            val field = BuildConfig::class.java.getField("GEMINI_API_KEY")
            (field.get(null) as? String) ?: ""
        } catch (_: Exception) {
            ""
        }

    /**
     * Interprets natural conversation into a strictly validated transaction intent.
     * The model classifies meaning; it is not driven by a fixed command vocabulary.
     */
    suspend fun understandTransactionIntent(
        userQuery: String,
        recentConversation: String,
        accounts: List<AccountEntity>,
        defaultCurrency: String,
        language: AppLanguage
    ): AiTransactionIntent? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") return@withContext null
        val accountContext = accounts.filterNot { it.isArchived }
            .joinToString("; ") { "${it.name} [${it.currency}, id=${it.id}] balance=${it.balance}" }
        val prompt = """
            You are the natural-language understanding layer for BYE PRO FINANCE.
            Understand colloquial Arabic (including Levantine/Syrian phrasing), Modern Standard Arabic,
            and English as a human conversational assistant would. Do not require command words.
            Use the conversation history to resolve references such as "سجلها", "من نفس الحساب",
            "غيرها", "دفعت له", and pronouns. Current user message: $userQuery
            Recent conversation: $recentConversation
            Available active accounts: $accountContext
            Default currency: $defaultCurrency
            Return exactly one JSON object and no markdown:
            {"intent":"CREATE_EXPENSE|CREATE_INCOME|OTHER|CLARIFY","amount":number|null,
             "currency":"ISO-4217"|null,"category":"short natural category"|null,
             "description":"short description"|null,"accountName":"exact available account name"|null,
             "dateIso":"YYYY-MM-DD"|null,"confidence":number,
             "clarification":"one concise question in the user's language"|null}
            Rules:
            - Select CREATE_EXPENSE or CREATE_INCOME only when the user's meaning clearly requests recording a transaction.
            - Understand semantic equivalents such as paying for a taxi, buying fuel, receiving salary,
              getting paid, refund, and similar natural descriptions; do not rely on fixed keywords.
            - Never invent amount, currency, account, category, or date. A currency may use the default only
              when no currency is stated and the user's context clearly uses the default currency.
            - If amount or transaction direction is unclear, return CLARIFY and ask one short question.
            - If multiple accounts could fit, leave accountName null and ask which account in clarification.
            - If the message is a question, greeting, general discussion, report request, or another action,
              return OTHER; do not misclassify it as a transaction.
            - Preserve the user's language in clarification. Set confidence from 0 to 1.
        """.trimIndent()
        try {
            val raw = callGeminiApi(prompt).trim()
                .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val json = JSONObject(raw)
            val amount = if (json.isNull("amount")) null else json.optDouble("amount", Double.NaN)
            AiTransactionIntent(
                intent = json.optString("intent", "CLARIFY").uppercase(),
                amount = amount?.takeIf { it.isFinite() && it > 0.0 },
                currency = json.optString("currency").takeIf { it.isNotBlank() && it != "null" }?.uppercase(),
                category = json.optString("category").takeIf { it.isNotBlank() && it != "null" },
                description = json.optString("description").takeIf { it.isNotBlank() && it != "null" },
                accountName = json.optString("accountName").takeIf { it.isNotBlank() && it != "null" },
                dateIso = json.optString("dateIso").takeIf { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) },
                confidence = json.optDouble("confidence", 0.0).coerceIn(0.0, 1.0),
                clarification = json.optString("clarification").takeIf { it.isNotBlank() && it != "null" }
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * AI Financial Assistant conversation with user financial context
     */
    suspend fun askAssistant(
        userQuery: String,
        accounts: List<AccountEntity>,
        transactions: List<TransactionEntity>,
        budgets: List<BudgetEntity>,
        debts: List<DebtEntity>,
        language: AppLanguage,
        savingsGoals: List<SavingsGoalEntity> = emptyList(),
        assets: List<AssetEntity> = emptyList(),
        bills: List<BillEntity> = emptyList(),
        conversationHistory: String = ""
    ): String = withContext(Dispatchers.IO) {
        val accountsByCurrency = accounts.groupBy { it.currency.trim().ifBlank { "UNKNOWN" }.uppercase() }
        val accountSummary = if (accountsByCurrency.isEmpty()) {
            if (language == AppLanguage.AR) "لا توجد حسابات مسجلة" else "No accounts recorded"
        } else {
            accountsByCurrency.entries.joinToString("; ") { (currency, rows) ->
                "$currency: " + rows.joinToString(", ") { "${it.name} ${it.balance}" }
            }
        }

        val ledger = LedgerSummary.summarizeByCurrency(transactions)
        val ledgerSummary = if (ledger.isEmpty()) {
            if (language == AppLanguage.AR) "لا توجد معاملات مسجلة" else "No transactions recorded"
        } else {
            ledger.entries.joinToString("; ") { (currency, totals) ->
                "$currency: income ${totals.income}, expenses ${totals.expense}, adjustments ${totals.adjustment}, net ${totals.net}"
            }
        }

        val debtSummary = if (debts.isEmpty()) {
            if (language == AppLanguage.AR) "لا توجد ديون مسجلة" else "No debts recorded"
        } else {
            debts.groupBy { it.currency.trim().ifBlank { "UNKNOWN" }.uppercase() }
                .entries.joinToString("; ") { (currency, rows) ->
                    "$currency: " + rows.joinToString(", ") { "${it.name}: ${it.remaining}" }
                }
        }

        val budgetSummary = if (budgets.isEmpty()) {
            if (language == AppLanguage.AR) "لا توجد ميزانيات مسجلة" else "No budgets recorded"
        } else {
            budgets.joinToString("; ") { "${it.category}: limit ${it.limitAmount} ${it.currency}, period ${it.startDate}-${it.endDate}" }
        }

        val savingsSummary = if (savingsGoals.isEmpty()) "No savings goals recorded" else savingsGoals.groupBy { it.currency.uppercase() }.entries.joinToString("; ") { (currency, rows) -> "$currency: " + rows.joinToString(", ") { "${it.name}: ${it.currentAmount}/${it.targetAmount}" } }
        val assetsSummary = if (assets.isEmpty()) "No assets recorded" else assets.groupBy { it.currency.uppercase() }.entries.joinToString("; ") { (currency, rows) -> "$currency: " + rows.joinToString(", ") { "${it.name}: ${it.value}" } }
        val billsSummary = if (bills.isEmpty()) "No bills recorded" else bills.groupBy { it.currency.uppercase() }.entries.joinToString("; ") { (currency, rows) -> "$currency: " + rows.joinToString(", ") { "${it.name}: ${it.amount} ${if (it.isPaid) "paid" else "unpaid"}" } }

        val langPrompt = if (language == AppLanguage.AR) "Arabic" else "English"
        val contextPrompt = """
            You are BYE PRO AI, a personal financial assistant.
            Reply in $langPrompt.
            Use only the financial records supplied below. Never invent transactions, merchants, dates,
            savings amounts, budget percentages, or exchange rates. Keep different currencies separate;
            do not add or compare amounts across currencies. The transaction totals are recorded totals,
            not necessarily month-only totals. Clearly say when the data is insufficient.
            Accounts by currency: $accountSummary
            Ledger totals by currency: $ledgerSummary
            Debts by currency: $debtSummary
            Budgets: $budgetSummary
            Savings goals: $savingsSummary
            Assets by currency: $assetsSummary
            Bills by currency: $billsSummary
            User question: $userQuery
            End with a brief note that this is analytical guidance, not licensed financial advice.
        """.trimIndent()

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val response = callGeminiApi(contextPrompt)
                if (response.isNotBlank()) return@withContext response
            } catch (_: Exception) {
                // Use a transparent records-only response when the AI service is unavailable.
            }
        }

        if (language == AppLanguage.AR) {
            "تعذر الوصول إلى خدمة الذكاء الاصطناعي حالياً. هذه خلاصة السجلات المتاحة دون إجراء تحويل بين العملات:\n" +
                "الحسابات: $accountSummary\nالمعاملات: $ledgerSummary\nالديون: $debtSummary\nالميزانيات: $budgetSummary\nالمدخرات: $savingsSummary\nالأصول: $assetsSummary\nالفواتير: $billsSummary\n" +
                "لا تتوفر بيانات كافية لإضافة توصية موثوقة. (تحليل استرشادي وليس استشارة مالية مرخصة)"
        } else {
            "The AI service is currently unavailable. Here is the available record summary without cross-currency conversion:\n" +
                "Accounts: $accountSummary\nTransactions: $ledgerSummary\nDebts: $debtSummary\nBudgets: $budgetSummary\nSavings: $savingsSummary\nAssets: $assetsSummary\nBills: $billsSummary\n" +
                "There is not enough data to add a reliable recommendation. (Analytical guidance, not licensed financial advice)"
        }
    }

    /**
     * Smart Scan OCR extractor: Parses receipts or invoices into structured financial data
     * using Gemini Vision or text extraction. Returns Result with failure if extraction fails,
     * strictly preventing the invention of fake mock numbers.
     */
    suspend fun parseReceiptImage(
        sampleText: String? = null,
        imageBase64: String? = null,
        language: AppLanguage = AppLanguage.AR
    ): Result<ExtractedReceiptData> = withContext(Dispatchers.IO) {
        // 1. Try Gemini Vision multimodal extraction if image is provided
        if (!imageBase64.isNullOrBlank() && apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val visionResponse = callGeminiVisionApi(imageBase64)
                if (visionResponse.isNotBlank()) {
                    val cleanJson = visionResponse.replace("```json", "").replace("```", "").trim()
                    val obj = JSONObject(cleanJson)
                    return@withContext Result.success(
                        ExtractedReceiptData(
                            merchant = obj.optString("merchant", "").trim(),
                            amount = obj.optDouble("amount", Double.NaN).also {
                                require(it.isFinite() && it > 0.0) { "Invalid receipt amount" }
                            },
                            tax = obj.optDouble("tax", 0.0).takeIf { it.isFinite() && it >= 0.0 } ?: 0.0,
                            currency = obj.optString("currency", "").trim().uppercase(),
                            category = obj.optString("category", "Other").trim().ifBlank { "Other" },
                            dateStr = obj.optString("dateStr", "").trim(),
                            itemsSummary = obj.optString("itemsSummary", "").trim()
                        )
                    )
                }
            } catch (_: Exception) {
                // proceed to text fallback below
            }
        }

        // 2. Parse from text stream (e.g. from OCR engine or raw receipt text)
        if (!sampleText.isNullOrBlank()) {
            val merchantMatch = Regex("""(?:مؤسسة|شركة|متجر|اسواق|مطعم|سوبرماركت|Store|Market|Cafe|Shop)[:\s]+([^\n,]+)""").find(sampleText)?.groupValues?.get(1)
            val totalMatch = Regex("""(?:الإجمالي|المجموع|Total|Amount)[:\s]*([0-9]+(?:\.[0-9]+)?)""").find(sampleText)?.groupValues?.get(1)?.toDoubleOrNull()
            val taxMatch = Regex("""(?:الضريبة|VAT|Tax)[:\s]*([0-9]+(?:\.[0-9]+)?)""").find(sampleText)?.groupValues?.get(1)?.toDoubleOrNull()

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

        // 3. Never return fake mock data on failure. Return clear error.
        Result.failure(
            Exception(
                if (language == AppLanguage.AR)
                    "تعذر استخراج بيانات الفاتورة آلياً. يرجى إدخال البيانات يدوياً في النموذج."
                else
                    "Could not extract receipt details automatically. Please enter numbers manually."
            )
        )
    }

    /**
     * Generates real financial health insights and spending anomalies
     */
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

    private fun callGeminiVisionApi(imageBase64: String, mimeType: String = "image/jpeg"): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent?key=$apiKey"
        val prompt = "You are an expert financial receipt OCR engine. Extract data in JSON: {\"merchant\": string, \"amount\": number, \"tax\": number, \"currency\": string, \"category\": string, \"dateStr\": string, \"itemsSummary\": string}. Return ONLY raw valid JSON, no markdown."
        val jsonPayload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                        put(JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", mimeType)
                                put("data", imageBase64)
                            })
                        })
                    })
                })
            })
        }
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = jsonPayload.toString().toRequestBody(mediaType)
        val request = Request.Builder().url(url).post(body).build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return ""
            val bodyStr = response.body?.string() ?: return ""
            val json = JSONObject(bodyStr)
            val candidates = json.optJSONArray("candidates") ?: return ""
            if (candidates.length() > 0) {
                val first = candidates.getJSONObject(0)
                val content = first.optJSONObject("content") ?: return ""
                val parts = content.optJSONArray("parts") ?: return ""
                if (parts.length() > 0) {
                    return parts.getJSONObject(0).optString("text", "")
                }
            }
        }
        return ""
    }

    private fun callGeminiApi(prompt: String): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent?key=$apiKey"
        val jsonPayload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })
            })
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = jsonPayload.toString().toRequestBody(mediaType)
        val request = Request.Builder().url(url).post(body).build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return ""
            val bodyStr = response.body?.string() ?: return ""
            val json = JSONObject(bodyStr)
            val candidates = json.optJSONArray("candidates") ?: return ""
            if (candidates.length() > 0) {
                val first = candidates.getJSONObject(0)
                val content = first.optJSONObject("content") ?: return ""
                val parts = content.optJSONArray("parts") ?: return ""
                if (parts.length() > 0) {
                    return parts.getJSONObject(0).optString("text", "")
                }
            }
        }
        return ""
    }
}
