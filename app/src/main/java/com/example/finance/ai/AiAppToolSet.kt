package com.example.finance.ai

import com.example.finance.data.FinanceRepository
import com.example.finance.model.BudgetEntity
import com.example.finance.model.BudgetPeriod
import com.google.ai.edge.litertlm.Tool
import com.google.ai.edge.litertlm.ToolParam
import com.google.ai.edge.litertlm.ToolSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/** A write request is only a preview until the user explicitly confirms it. */
data class AiActionPreview(
    val id: String,
    val title: String,
    val description: String
)

/**
 * Local, typed app tools exposed to LiteRT-LM. The model never receives a
 * database handle and cannot issue SQL. Read tools return bounded summaries;
 * write tools only stage a preview for a separate user-confirmation action.
 */
class AiAppToolSet(
    private val repository: FinanceRepository,
    private val navigate: (String) -> Unit
) : ToolSet {
    private val _pendingAction = MutableStateFlow<AiActionPreview?>(null)
    val pendingAction: StateFlow<AiActionPreview?> = _pendingAction.asStateFlow()
    private val actionMutex = Mutex()

    @Tool(description = "Navigate to an existing section of the BYE PRO FINANCE app. Allowed sections: dashboard, accounts, transactions, budgets, debts, bills, savings, assets, transfers, reports, household, documents, notifications, settings, ai_assistant.")
    fun navigateToAppSection(
        @ToolParam(description = "One exact allowed section identifier.") section: String
    ): Map<String, Any> {
        val allowed = setOf(
            "dashboard", "accounts", "transactions", "budgets", "debts", "bills",
            "savings", "assets", "transfers", "reports", "household", "documents",
            "notifications", "settings", "ai_assistant"
        )
        val target = section.trim().lowercase()
        if (target !in allowed) return mapOf("success" to false, "error" to "Section is not available.")
        navigate(target)
        return mapOf("success" to true, "section" to target)
    }

    @Tool(description = "Read a compact overview of the user's local accounts, budgets, debts and savings goals. Use only to answer a question about the user's own finance data.")
    fun getFinancialOverview(): Map<String, Any> = runBlocking(Dispatchers.IO) {
        val accountRows = repository.accounts.first()
        val budgetRows = repository.budgets.first()
        val debtRows = repository.debts.first()
        val savingsRows = repository.savingsGoals.first()
        mapOf(
            "accounts" to accountRows.take(30).map {
                mapOf("id" to it.id, "name" to it.name, "balance" to it.balance, "currency" to it.currency)
            },
            "budgets" to budgetRows.take(30).map {
                mapOf("name" to it.name, "category" to it.category, "limit" to it.limitAmount, "currency" to it.currency)
            },
            "debts" to debtRows.take(30).map {
                mapOf("name" to it.name, "remaining" to it.remaining, "currency" to it.currency, "direction" to it.direction)
            },
            "savingsGoals" to savingsRows.take(30).map {
                mapOf("name" to it.name, "target" to it.targetAmount, "current" to it.currentAmount, "currency" to it.currency)
            }
        )
    }

    @Tool(description = "Read the most recent transactions from the local database. Never infer or invent missing transaction fields.")
    fun getRecentTransactions(
        @ToolParam(description = "Maximum rows to return, from 1 to 20.") limit: Int = 10
    ): List<Map<String, Any>> = runBlocking(Dispatchers.IO) {
        repository.transactions.first().take(limit.coerceIn(1, 20)).map {
            mapOf(
                "id" to it.id,
                "type" to it.type.name,
                "amount" to it.amount,
                "currency" to it.currency,
                "category" to it.category,
                "description" to it.description,
                "date" to it.date,
                "status" to it.status.name
            )
        }
    }

    @Tool(description = "Prepare a new monthly budget for user review. This tool NEVER saves the budget; it only creates a confirmation preview. Do not claim it was saved.")
    fun proposeCreateBudget(
        @ToolParam(description = "Budget name.") name: String,
        @ToolParam(description = "Budget category.") category: String,
        @ToolParam(description = "Positive budget limit.") limitAmount: Double,
        @ToolParam(description = "Three-letter currency code, e.g. LYD.") currency: String = "LYD"
    ): Map<String, Any> {
        val cleanName = name.trim()
        val cleanCategory = category.trim()
        val cleanCurrency = currency.trim().uppercase()
        if (cleanName.length !in 2..80 || cleanCategory.length !in 2..60 ||
            !limitAmount.isFinite() || limitAmount <= 0.0 ||
            !cleanCurrency.matches(Regex("[A-Z]{3}"))
        ) {
            return mapOf("success" to false, "error" to "Budget details are invalid.")
        }
        val preview = AiActionPreview(
            id = UUID.randomUUID().toString(),
            title = "مراجعة إنشاء ميزانية",
            description = "الاسم: $cleanName\nالفئة: $cleanCategory\nالحد: $limitAmount $cleanCurrency\nالمدة: شهرية"
        )
        _pendingAction.value = preview
        stagedBudget = BudgetEntity(
            name = cleanName,
            category = cleanCategory,
            period = BudgetPeriod.MONTHLY,
            limitAmount = limitAmount,
            currency = cleanCurrency
        )
        return mapOf(
            "success" to true,
            "requires_user_confirmation" to true,
            "preview_id" to preview.id,
            "message" to "A preview is shown to the user. Wait for confirmation; no data has been changed."
        )
    }

    @Volatile
    private var stagedBudget: BudgetEntity? = null

    suspend fun confirmPendingAction(actionId: String): String = actionMutex.withLock {
        val preview = _pendingAction.value ?: error("لا توجد عملية تنتظر التأكيد.")
        require(preview.id == actionId) { "انتهت صلاحية معاينة العملية." }
        val budget = stagedBudget ?: error("بيانات الميزانية غير متاحة.")
        repository.addBudget(budget)
        _pendingAction.value = null
        stagedBudget = null
        "تم إنشاء الميزانية «${budget.name}» بنجاح بعد تأكيدك."
    }

    fun cancelPendingAction(actionId: String) {
        if (_pendingAction.value?.id != actionId) return
        _pendingAction.value = null
        stagedBudget = null
    }
}
