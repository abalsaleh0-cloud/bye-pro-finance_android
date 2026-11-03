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
    private val workspace: AiProjectWorkspace,
    private val navigate: (String) -> Unit
) : ToolSet {
    private val _pendingAction = MutableStateFlow<AiActionPreview?>(null)
    val pendingAction: StateFlow<AiActionPreview?> = _pendingAction.asStateFlow()
    private val actionMutex = Mutex()
    private sealed interface PendingPayload {
        data class Budget(val value: BudgetEntity) : PendingPayload
        data class Project(val value: AiProjectPlan) : PendingPayload
        data class ProjectFile(val projectId: String, val path: String, val content: String) : PendingPayload
    }
    private data class Pending(val preview: AiActionPreview, val payload: PendingPayload)
    @Volatile private var pending: Pending? = null

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

    @Tool(description = "List locally saved project plans and their milestones and tasks. Do not invent project details.")
    fun listProjectPlans(): List<Map<String, Any>> = workspace.listPlans().take(30).map { plan ->
        mapOf(
            "id" to plan.id,
            "title" to plan.title,
            "objective" to plan.objective,
            "technologyStack" to plan.technologyStack,
            "milestones" to plan.milestones,
            "tasks" to plan.tasks,
            "createdAt" to plan.createdAt
        )
    }

    @Tool(description = "Prepare a complete project plan for local saving. Provide milestones and tasks as newline-separated lists. This only creates a review preview; it does not save until the user confirms.")
    fun proposeCreateProjectPlan(
        @ToolParam(description = "Short project title.") title: String,
        @ToolParam(description = "Clear project objective and scope.") objective: String,
        @ToolParam(description = "Suggested technology stack.") technologyStack: String,
        @ToolParam(description = "Milestones, one per line, in execution order.") milestones: String,
        @ToolParam(description = "Concrete tasks, one per line.") tasks: String
    ): Map<String, Any> {
        val cleanTitle = title.trim()
        val cleanObjective = objective.trim()
        val cleanStack = technologyStack.trim()
        val milestoneItems = milestones.lines().map(String::trim).filter(String::isNotBlank).take(15)
        val taskItems = tasks.lines().map(String::trim).filter(String::isNotBlank).take(40)
        if (cleanTitle.length !in 3..100 || cleanObjective.length !in 10..1500 ||
            cleanStack.length !in 3..1200 || milestoneItems.isEmpty() || taskItems.isEmpty()
        ) return mapOf("success" to false, "error" to "Project plan is incomplete or exceeds the supported limits.")
        val plan = AiProjectPlan(
            id = UUID.randomUUID().toString(),
            title = cleanTitle,
            objective = cleanObjective,
            technologyStack = cleanStack,
            milestones = milestoneItems,
            tasks = taskItems,
            createdAt = System.currentTimeMillis()
        )
        val preview = AiActionPreview(
            id = UUID.randomUUID().toString(),
            title = "مراجعة خطة المشروع",
            description = buildString {
                append("المشروع: $cleanTitle\n")
                append("الهدف: $cleanObjective\n")
                append("التقنيات: $cleanStack\n")
                append("المراحل:\n")
                milestoneItems.forEachIndexed { index, item -> append("${index + 1}. $item\n") }
                append("المهام: ${taskItems.size}")
            }
        )
        pending = Pending(preview, PendingPayload.Project(plan))
        _pendingAction.value = preview
        return mapOf(
            "success" to true,
            "requires_user_confirmation" to true,
            "preview_id" to preview.id,
            "message" to "A project plan preview is visible. Wait for user confirmation before saving."
        )
    }

    @Tool(description = "List files saved inside an existing AI project workspace.")
    fun listProjectFiles(@ToolParam(description = "Existing project identifier.") projectId: String): List<Map<String, Any>> {
        if (workspace.getPlan(projectId) == null) return emptyList()
        return workspace.fileWorkspace().listFiles(projectId).map {
            mapOf("path" to it.path, "sizeBytes" to it.sizeBytes, "modifiedAt" to it.modifiedAt)
        }
    }

    @Tool(description = "Read a UTF-8 text file from an existing AI project. Maximum file size is 1 MiB.")
    fun readProjectFile(
        @ToolParam(description = "Existing project identifier.") projectId: String,
        @ToolParam(description = "Relative path inside the project.") path: String
    ): Map<String, Any> {
        if (workspace.getPlan(projectId) == null) return mapOf("success" to false, "error" to "Project not found.")
        val content = workspace.fileWorkspace().readFile(projectId, path)
            ?: return mapOf("success" to false, "error" to "Project file not found.")
        return mapOf("success" to true, "path" to path, "content" to content.take(MAX_TOOL_FILE_READ_CHARS),
            "truncated" to (content.length > MAX_TOOL_FILE_READ_CHARS))
    }

    @Tool(description = "Prepare a UTF-8 file write inside an existing AI project. This only creates a preview; wait for user confirmation before writing.")
    fun proposeWriteProjectFile(
        @ToolParam(description = "Existing project identifier.") projectId: String,
        @ToolParam(description = "Relative path inside the project.") path: String,
        @ToolParam(description = "Complete UTF-8 file content, maximum 1 MiB.") content: String
    ): Map<String, Any> {
        if (workspace.getPlan(projectId) == null) return mapOf("success" to false, "error" to "Project not found.")
        if (content.toByteArray(Charsets.UTF_8).size > MAX_PROJECT_FILE_BYTES) {
            return mapOf("success" to false, "error" to "Project file exceeds the 1 MiB limit.")
        }
        val preview = AiActionPreview(
            id = UUID.randomUUID().toString(),
            title = "مراجعة كتابة ملف مشروع",
            description = "المشروع: " + projectId + "\\nالمسار: " + path + "\\nالحجم: " + content.toByteArray(Charsets.UTF_8).size + " bytes\\nلن يُكتب الملف قبل تأكيدك."
        )
        pending = Pending(preview, PendingPayload.ProjectFile(projectId, path, content))
        _pendingAction.value = preview
        return mapOf("success" to true, "requires_user_confirmation" to true, "preview_id" to preview.id,
            "message" to "A project file write preview is visible. Wait for user confirmation.")
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
        pending = Pending(preview, PendingPayload.Budget(BudgetEntity(
            name = cleanName,
            category = cleanCategory,
            period = BudgetPeriod.MONTHLY,
            limitAmount = limitAmount,
            currency = cleanCurrency
        )))
        _pendingAction.value = preview
        return mapOf(
            "success" to true,
            "requires_user_confirmation" to true,
            "preview_id" to preview.id,
            "message" to "A preview is shown to the user. Wait for confirmation; no data has been changed."
        )
    }

    @Tool(description = "Read active and archived accounts with balances and currencies.")
    fun listAccounts(includeArchived: Boolean = false): List<Map<String, Any>> = runBlocking(Dispatchers.IO) {
        val rows = repository.accounts.first() + if (includeArchived) repository.archivedAccounts.first() else emptyList()
        rows.distinctBy { it.id }.take(50).map { mapOf("id" to it.id, "name" to it.name, "type" to it.type.name, "balance" to it.balance, "currency" to it.currency, "archived" to it.isArchived) }
    }

    @Tool(description = "Read saved transfers, newest first.")
    fun listTransfers(): List<Map<String, Any>> = runBlocking(Dispatchers.IO) {
        repository.transfers.first().take(30).map { mapOf("id" to it.id, "fromAccountId" to it.fromAccountId, "toAccountId" to it.toAccountId, "amount" to it.amount, "fee" to it.fee, "currency" to it.currency, "date" to it.date, "description" to it.description) }
    }

    @Tool(description = "Read all budgets and their limits.")
    fun listBudgets(): List<Map<String, Any>> = runBlocking(Dispatchers.IO) {
        repository.budgets.first().take(50).map { mapOf("id" to it.id, "name" to it.name, "category" to it.category, "period" to it.period.name, "limit" to it.limitAmount, "currency" to it.currency) }
    }

    @Tool(description = "Read debts, direction, remaining amount and due date.")
    fun listDebts(): List<Map<String, Any>> = runBlocking(Dispatchers.IO) {
        repository.debts.first().take(50).map { mapOf("id" to it.id, "name" to it.name, "principal" to it.principal, "remaining" to it.remaining, "currency" to it.currency, "direction" to it.direction, "status" to it.status.name, "dueDate" to it.dueDate) }
    }

    @Tool(description = "Read bills and payment status.")
    fun listBills(): List<Map<String, Any>> = runBlocking(Dispatchers.IO) {
        repository.bills.first().take(50).map { mapOf("id" to it.id, "name" to it.name, "category" to it.category, "amount" to it.amount, "currency" to it.currency, "dueDate" to it.dueDate, "paid" to it.isPaid, "recurring" to it.isRecurring) }
    }

    @Tool(description = "Read savings goals and progress.")
    fun listSavingsGoals(): List<Map<String, Any>> = runBlocking(Dispatchers.IO) {
        repository.savingsGoals.first().take(50).map { mapOf("id" to it.id, "name" to it.name, "target" to it.targetAmount, "current" to it.currentAmount, "currency" to it.currency, "deadline" to it.deadline) }
    }

    @Tool(description = "Read assets and recorded values.")
    fun listAssets(): List<Map<String, Any>> = runBlocking(Dispatchers.IO) {
        repository.assets.first().take(50).map { mapOf("id" to it.id, "name" to it.name, "type" to it.type.name, "value" to it.value, "currency" to it.currency, "purchaseDate" to it.purchaseDate) }
    }

    @Tool(description = "Read household members without exposing email addresses.")
    fun listHouseholdMembers(): List<Map<String, Any>> = runBlocking(Dispatchers.IO) {
        repository.householdMembers.first().take(50).map { mapOf("id" to it.id, "name" to it.name, "role" to it.role, "contributionPercent" to it.contributionPercent) }
    }

    @Tool(description = "Read household expenses.")
    fun listHouseholdExpenses(): List<Map<String, Any>> = runBlocking(Dispatchers.IO) {
        repository.householdExpenses.first().take(50).map { mapOf("id" to it.id, "title" to it.title, "amount" to it.amount, "payerMemberId" to it.payerMemberId, "category" to it.category, "currency" to it.currency, "date" to it.date, "settled" to it.isSettled) }
    }

    @Tool(description = "Read document register metadata only; never expose local file URIs or document contents.")
    fun listDocuments(): List<Map<String, Any>> = runBlocking(Dispatchers.IO) {
        repository.documents.first().take(50).map { mapOf("id" to it.id, "title" to it.title, "type" to it.type.name, "merchant" to it.merchant, "totalAmount" to it.totalAmount, "taxAmount" to it.taxAmount, "currency" to it.currency, "date" to it.date) }
    }

    @Tool(description = "Read recent in-app notifications.")
    fun listNotifications(): List<Map<String, Any>> = runBlocking(Dispatchers.IO) {
        repository.notifications.first().take(30).map { mapOf("id" to it.id, "title" to it.title, "message" to it.message, "type" to it.type.name, "timestamp" to it.timestamp, "read" to it.isRead) }
    }

    @Tool(description = "Read recent audit activity for transparency.")
    fun listAuditActivity(): List<Map<String, Any>> = runBlocking(Dispatchers.IO) {
        repository.auditLogs.first().take(30).map { mapOf("id" to it.id, "action" to it.action, "resource" to it.resource, "details" to it.details, "timestamp" to it.timestamp) }
    }
    suspend fun confirmPendingAction(actionId: String): String = actionMutex.withLock {
        val staged = pending ?: error("لا توجد عملية تنتظر التأكيد.")
        require(staged.preview.id == actionId) { "انتهت صلاحية معاينة العملية." }
        val result = when (val payload = staged.payload) {
            is PendingPayload.Budget -> {
                repository.addBudget(payload.value)
                "تم إنشاء الميزانية «${payload.value.name}» بنجاح بعد تأكيدك."
            }
            is PendingPayload.Project -> {
                workspace.savePlan(payload.value)
                "تم حفظ خطة المشروع «${payload.value.title}» محليًا بعد تأكيدك."
            }
            is PendingPayload.ProjectFile -> {
                val saved = workspace.fileWorkspace().writeFile(payload.projectId, payload.path, payload.content)
                "تم حفظ الملف «" + saved.path + "» داخل مساحة المشروع بعد تأكيدك."
            }
        }
        pending = null
        _pendingAction.value = null
        result
    }

    private companion object {
        const val MAX_PROJECT_FILE_BYTES = 1024 * 1024
        const val MAX_TOOL_FILE_READ_CHARS = 20_000
    }

    fun cancelPendingAction(actionId: String) {
        if (pending?.preview?.id != actionId) return
        pending = null
        _pendingAction.value = null
    }
}
