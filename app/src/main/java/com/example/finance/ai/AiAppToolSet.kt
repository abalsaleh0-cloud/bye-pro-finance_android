package com.example.finance.ai

import com.example.finance.data.FinanceRepository
import com.example.finance.model.BudgetEntity
import com.example.finance.model.BudgetPeriod
import com.example.finance.model.AccountEntity
import com.example.finance.model.AccountType
import com.example.finance.model.TransactionEntity
import com.example.finance.model.TransactionType
import com.example.finance.model.TransactionStatus
import com.example.finance.model.DebtEntity
import com.example.finance.model.BillEntity
import com.example.finance.model.SavingsGoalEntity
import com.example.finance.model.AssetEntity
import com.example.finance.model.AssetType
import com.example.finance.model.WorkSourceEntity
import com.example.finance.model.WorkSourceType
import com.example.finance.model.WorkRecordEntity
import com.example.finance.model.WorkRecordType
import com.example.finance.engine.WorkLedgerCalculator
import com.google.ai.edge.litertlm.Tool
import com.google.ai.edge.litertlm.ToolParam
import com.google.ai.edge.litertlm.ToolSet
import org.json.JSONArray
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
        data class ProjectFiles(val projectId: String, val writes: List<AiProjectFileWrite>) : PendingPayload
        data class Account(val value: AccountEntity) : PendingPayload
        data class Transaction(val value: TransactionEntity) : PendingPayload
        data class Debt(val value: DebtEntity) : PendingPayload
        data class Bill(val value: BillEntity) : PendingPayload
        data class SavingsGoal(val value: SavingsGoalEntity) : PendingPayload
        data class Asset(val value: AssetEntity) : PendingPayload
        data class WorkSource(val value: WorkSourceEntity) : PendingPayload
        data class WorkRecord(val value: WorkRecordEntity, val transaction: TransactionEntity?) : PendingPayload
        data class Transfer(val fromId: Long, val toId: Long, val amount: Double, val fee: Double, val currency: String, val description: String, val reference: String) : PendingPayload
        data class HouseholdMember(val name: String, val email: String, val role: String, val percent: Double) : PendingPayload
        data class HouseholdExpense(val title: String, val amount: Double, val payerId: Long, val category: String, val currency: String) : PendingPayload
        data class Update(val execute: suspend () -> String) : PendingPayload
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
            "notifications", "settings", "ai_assistant", "work", "smart_scan", "import_export", "admin"
        )
        val target = section.trim().lowercase()
        if (target !in allowed) return mapOf("success" to false, "error" to "Section is not available.")
        navigate(target)
        return mapOf("success" to true, "section" to target)
    }


    @Tool(description = "Read the user's work ledger: employment and freelance sources, recent work records, salary amounts, receivables and work cash balances.")
    fun getWorkLedgerOverview(): Map<String, Any> = runBlocking(Dispatchers.IO) {
        val sources = repository.workSources.first()
        val records = repository.workRecords.first()
        mapOf(
            "sources" to sources.take(50).map { mapOf("id" to it.id, "name" to it.name, "type" to it.type.name, "monthlySalary" to it.monthlySalary, "currency" to it.currency, "active" to it.isActive) },
            "records" to records.take(100).map { mapOf("id" to it.id, "sourceId" to it.sourceId, "relatedRecordId" to it.relatedRecordId, "type" to it.type.name, "amount" to it.amount, "currency" to it.currency, "period" to it.period, "date" to it.date, "notes" to it.notes) },
            "cashBalances" to records.map { it.currency.uppercase() }.distinct().associateWith { currency -> WorkLedgerCalculator.cashBalance(records, currency).toPlainString() }
        )
    }

    @Tool(description = "Prepare a new employment or freelance income source in Work. type must be EMPLOYMENT or FREELANCE; freelance monthlySalary must be zero. Saved only after confirmation.")
    fun proposeCreateWorkSource(name: String, type: String, monthlySalary: Double, currency: String): Map<String, Any> {
        val cleanName = name.trim()
        val parsedType = runCatching { WorkSourceType.valueOf(type.trim().uppercase()) }.getOrNull()
            ?: return mapOf("success" to false, "error" to "type must be EMPLOYMENT or FREELANCE.")
        val cleanCurrency = currency.trim().uppercase()
        if (cleanName.length !in 2..100 || !monthlySalary.isFinite() || monthlySalary < 0.0 ||
            !cleanCurrency.matches(Regex("[A-Z]{3}")) ||
            (parsedType == WorkSourceType.FREELANCE && monthlySalary != 0.0)
        ) return mapOf("success" to false, "error" to "Work source details are invalid.")
        val value = WorkSourceEntity(name = cleanName, type = parsedType, monthlySalary = monthlySalary, currency = cleanCurrency)
        return stageAction("مراجعة إضافة مصدر عمل", "الاسم: $cleanName\nالنوع: $parsedType\nالراتب الشهري: $monthlySalary $cleanCurrency", PendingPayload.WorkSource(value))
    }

    @Tool(description = "Prepare a work ledger record. type must exactly match WorkRecordType. For cash movements accountId is required to link the personal account. FREELANCE_PAYMENT requires relatedRecordId. Saved only after confirmation.")
    fun proposeCreateWorkRecord(sourceId: Long, type: String, amount: Double, currency: String, period: String, notes: String, accountId: Long, relatedRecordId: Long): Map<String, Any> = runBlocking(Dispatchers.IO) {
        val parsedType = runCatching { WorkRecordType.valueOf(type.trim().uppercase()) }.getOrNull()
            ?: return@runBlocking mapOf("success" to false, "error" to "Unknown work record type.")
        val cleanCurrency = currency.trim().uppercase()
        val cleanPeriod = period.trim().ifBlank { java.time.YearMonth.now().toString() }
        if (!amount.isFinite() || amount <= 0.0 || !cleanCurrency.matches(Regex("[A-Z]{3}")) ||
            !cleanPeriod.matches(Regex("\\d{4}-\\d{2}"))
        ) return@runBlocking mapOf("success" to false, "error" to "Work record amount, currency or period is invalid.")
        val cash = WorkLedgerCalculator.isCashMovement(parsedType)
        val account = if (cash) {
            if (accountId <= 0L) return@runBlocking mapOf("success" to false, "error" to "A personal account is required for this cash movement.")
            repository.accounts.first().firstOrNull { it.id == accountId }
                ?: return@runBlocking mapOf("success" to false, "error" to "Personal account not found.")
        } else null
        if (account != null && !account.currency.equals(cleanCurrency, true)) return@runBlocking mapOf("success" to false, "error" to "Personal account currency does not match.")
        if (!cash && accountId > 0L) return@runBlocking mapOf("success" to false, "error" to "Do not link an account to an accrued work record.")
        if (parsedType == WorkRecordType.FREELANCE_PAYMENT && relatedRecordId <= 0L) return@runBlocking mapOf("success" to false, "error" to "Select the freelance receivable.")
        if (parsedType != WorkRecordType.FREELANCE_PAYMENT && relatedRecordId > 0L) return@runBlocking mapOf("success" to false, "error" to "relatedRecordId is only valid for FREELANCE_PAYMENT.")
        val txType = when (parsedType) {
            WorkRecordType.SALARY_ADVANCE, WorkRecordType.SALARY_PAYMENT, WorkRecordType.FREELANCE_PAYMENT -> TransactionType.INCOME
            WorkRecordType.FREELANCE_EXPENSE, WorkRecordType.WORK_EXPENSE -> TransactionType.EXPENSE
            WorkRecordType.WORK_TRANSFER_IN -> TransactionType.TRANSFER
            WorkRecordType.WORK_TRANSFER_OUT -> TransactionType.ADJUSTMENT
            else -> null
        }
        val tx = if (account != null && txType != null) TransactionEntity(
            accountId = account.id, type = txType, amount = amount, currency = account.currency,
            category = when (txType) {
                TransactionType.INCOME -> "WORK_INCOME"
                TransactionType.EXPENSE -> "WORK_EXPENSE"
                else -> "WORK_TRANSFER"
            },
            description = notes.trim().ifBlank { parsedType.name }, status = TransactionStatus.COMPLETED
        ) else null
        val record = WorkRecordEntity(
            sourceId = sourceId.takeIf { it > 0L }, relatedRecordId = relatedRecordId.takeIf { it > 0L },
            type = parsedType, amount = amount, currency = cleanCurrency, period = cleanPeriod, notes = notes.trim()
        )
        val isTransfer = parsedType == WorkRecordType.WORK_TRANSFER_IN || parsedType == WorkRecordType.WORK_TRANSFER_OUT
        if (isTransfer && record.sourceId != null) return@runBlocking mapOf("success" to false, "error" to "Internal work transfers must not reference an employment source.")
        if (!isTransfer && record.sourceId == null) return@runBlocking mapOf("success" to false, "error" to "Choose an existing work source for this record.")
        val sources = repository.workSources.first()
        val source = record.sourceId?.let { id -> sources.firstOrNull { it.id == id } }
        if (record.sourceId != null && (source == null || !source.isActive)) return@runBlocking mapOf("success" to false, "error" to "Active work source not found.")
        val employmentRecord = parsedType in setOf(WorkRecordType.SALARY_ADVANCE, WorkRecordType.SALARY_DEDUCTION, WorkRecordType.SALARY_BONUS, WorkRecordType.SALARY_OVERTIME, WorkRecordType.SALARY_PAYMENT)
        if (source != null && parsedType != WorkRecordType.WORK_EXPENSE &&
            ((source.type == WorkSourceType.EMPLOYMENT) != employmentRecord)
        ) return@runBlocking mapOf("success" to false, "error" to "This work record type does not match the selected source type.")
        if (source != null && !source.currency.equals(cleanCurrency, true)) return@runBlocking mapOf("success" to false, "error" to "Work source currency does not match the record.")
        if (parsedType == WorkRecordType.FREELANCE_PAYMENT) {
            val receivable = repository.workRecords.first().firstOrNull { it.id == relatedRecordId }
                ?: return@runBlocking mapOf("success" to false, "error" to "Freelance receivable not found.")
            if (receivable.type != WorkRecordType.FREELANCE_INCOME || receivable.sourceId != record.sourceId) {
                return@runBlocking mapOf("success" to false, "error" to "The selected record is not a receivable for this source.")
            }
        }
        val sourceName = source?.name
        stageAction(
            "مراجعة تسجيل حركة عمل",
            "المصدر: " + (sourceName ?: "تحويل رصيد العمل") + "\nالنوع: " + parsedType + "\nالمبلغ: " + amount + " " + cleanCurrency + "\nالفترة: " + cleanPeriod + "\n" + notes.trim(),
            PendingPayload.WorkRecord(record, tx)
        )
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
    @Tool(description = "Prepare multiple UTF-8 source or project files in one reviewable change. Pass a JSON array of objects with path and content. Limits: 20 files, 1 MiB per file, 5 MiB total. No file is written until the user confirms the preview.")
    fun proposeWriteProjectFiles(
        @ToolParam(description = "Existing project identifier.") projectId: String,
        @ToolParam(description = "JSON array of objects with path and content fields.") filesJson: String
    ): Map<String, Any> {
        if (workspace.getPlan(projectId) == null) return mapOf("success" to false, "error" to "Project not found.")
        if (filesJson.toByteArray(Charsets.UTF_8).size > 5 * 1024 * 1024) {
            return mapOf("success" to false, "error" to "Project batch exceeds the 5 MiB limit.")
        }
        val writes = runCatching {
            val array = JSONArray(filesJson)
            require(array.length() in 1..20) { "A batch must contain 1 to 20 files." }
            (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                require(item.has("path") && item.has("content")) { "Each file needs path and content." }
                AiProjectFileWrite(item.getString("path"), item.getString("content"))
            }
        }.getOrElse { error ->
            return mapOf("success" to false, "error" to (error.message ?: "Invalid file manifest."))
        }
        val fileWorkspace = workspace.fileWorkspace()
        runCatching { fileWorkspace.validateWrites(projectId, writes) }.onFailure { error ->
            return mapOf("success" to false, "error" to (error.message ?: "Invalid project file batch."))
        }
        val totalBytes = writes.sumOf { it.content.toByteArray(Charsets.UTF_8).size }
        val preview = AiActionPreview(
            id = UUID.randomUUID().toString(),
            title = "مراجعة تعديل ملفات المشروع",
            description = buildString {
                append("المشروع: ").append(projectId).append("\n")
                append("عدد الملفات: ").append(writes.size).append("\n")
                append("الحجم الإجمالي: ").append(totalBytes).append(" bytes\n")
                writes.forEach { append("• ").append(it.path).append(" (").append(it.content.toByteArray(Charsets.UTF_8).size).append(" bytes)\n") }
                append("لن تتغير الملفات قبل تأكيدك.")
            }
        )
        pending = Pending(preview, PendingPayload.ProjectFiles(projectId, writes))
        _pendingAction.value = preview
        return mapOf("success" to true, "requires_user_confirmation" to true, "preview_id" to preview.id,
            "message" to "A multi-file project change preview is visible. Wait for explicit confirmation.")
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
    // Every write tool creates a review card only. The repository is touched solely by confirmPendingAction.
    @Tool(description = "Prepare a new account. It is not saved until the user confirms the preview in the app.")
    fun proposeCreateAccount(name: String, type: String, currency: String = "LYD", openingBalance: Double = 0.0): Map<String, Any> {
        val n=name.trim(); val cur=currency.trim().uppercase(); val parsed=runCatching { AccountType.valueOf(type.trim().uppercase()) }.getOrNull()
        if (n.length !in 2..80 || parsed == null || !cur.matches(Regex("[A-Z]{3}")) || !openingBalance.isFinite()) return mapOf("success" to false, "error" to "Account details are invalid.")
        return stageAction("مراجعة إنشاء حساب", "الحساب: " + n + "\nالنوع: " + parsed.name + "\nالرصيد: " + openingBalance + " " + cur, PendingPayload.Account(AccountEntity(name=n,type=parsed,currency=cur,balance=openingBalance,openingBalance=openingBalance)))
    }

    @Tool(description = "Prepare a transaction. Account existence and matching currency are checked again when confirmed.")
    fun proposeCreateTransaction(accountId: Long, type: String, amount: Double, category: String, description: String, currency: String = "LYD", status: String = "COMPLETED"): Map<String, Any> {
        val t=runCatching { TransactionType.valueOf(type.trim().uppercase()) }.getOrNull(); val s=runCatching { TransactionStatus.valueOf(status.trim().uppercase()) }.getOrNull(); val cur=currency.trim().uppercase()
        if (accountId<=0 || t==null || s==null || !amount.isFinite() || amount<=0.0 || category.isBlank() || description.isBlank() || !cur.matches(Regex("[A-Z]{3}"))) return mapOf("success" to false, "error" to "Transaction details are invalid.")
        val value=TransactionEntity(accountId=accountId,type=t,amount=amount,currency=cur,category=category.trim(),description=description.trim(),status=s)
        return stageAction("مراجعة تسجيل معاملة", "النوع: " + t.name + "\nالمبلغ: " + amount + " " + cur + "\nالحساب: " + accountId, PendingPayload.Transaction(value))
    }

    @Tool(description = "Prepare a debt record. direction must be i_owe or owed_to_me. Requires user confirmation.")
    fun proposeCreateDebt(name: String, principal: Double, remaining: Double, direction: String, currency: String = "LYD"): Map<String, Any> {
        val n=name.trim(); val d=direction.trim().lowercase(); val cur=currency.trim().uppercase()
        if (n.length !in 2..100 || !principal.isFinite() || principal<=0.0 || !remaining.isFinite() || remaining !in 0.0..principal || d !in setOf("i_owe","owed_to_me") || !cur.matches(Regex("[A-Z]{3}"))) return mapOf("success" to false, "error" to "Debt details are invalid.")
        return stageAction("مراجعة إضافة دين", "الاسم: " + n + "\nالاتجاه: " + d + "\nالمتبقي: " + remaining + " " + cur, PendingPayload.Debt(DebtEntity(name=n,creditor=n,principal=principal,remaining=remaining,direction=d,currency=cur)))
    }

    @Tool(description = "Prepare a bill for review. Requires user confirmation.")
    fun proposeCreateBill(name: String, category: String, amount: Double, dueDate: Long, currency: String = "LYD", recurring: Boolean = true): Map<String, Any> {
        val n=name.trim(); val cat=category.trim(); val cur=currency.trim().uppercase()
        if (n.length !in 2..100 || cat.isBlank() || !amount.isFinite() || amount<=0.0 || !cur.matches(Regex("[A-Z]{3}"))) return mapOf("success" to false, "error" to "Bill details are invalid.")
        return stageAction("مراجعة إضافة فاتورة", "الفاتورة: " + n + "\nالمبلغ: " + amount + " " + cur + "\nالاستحقاق: " + dueDate, PendingPayload.Bill(BillEntity(name=n,category=cat,amount=amount,currency=cur,dueDate=dueDate,isRecurring=recurring)))
    }

    @Tool(description = "Prepare a savings goal for review. Requires user confirmation.")
    fun proposeCreateSavingsGoal(name: String, targetAmount: Double, currentAmount: Double = 0.0, currency: String = "LYD", deadline: Long = System.currentTimeMillis() + 15552000000L): Map<String, Any> {
        val n=name.trim(); val cur=currency.trim().uppercase()
        if (n.length !in 2..100 || !targetAmount.isFinite() || targetAmount<=0.0 || !currentAmount.isFinite() || currentAmount !in 0.0..targetAmount || !cur.matches(Regex("[A-Z]{3}"))) return mapOf("success" to false, "error" to "Savings goal details are invalid.")
        return stageAction("مراجعة هدف ادخار", "الهدف: " + n + "\nالمستهدف: " + targetAmount + " " + cur, PendingPayload.SavingsGoal(SavingsGoalEntity(name=n,targetAmount=targetAmount,currentAmount=currentAmount,currency=cur,deadline=deadline)))
    }

    @Tool(description = "Prepare an asset record for review. Requires user confirmation.")
    fun proposeCreateAsset(name: String, type: String, value: Double, currency: String = "LYD", notes: String = ""): Map<String, Any> {
        val n=name.trim(); val cur=currency.trim().uppercase(); val parsed=runCatching { AssetType.valueOf(type.trim().uppercase()) }.getOrNull()
        if (n.length !in 2..100 || parsed==null || !value.isFinite() || value<0.0 || !cur.matches(Regex("[A-Z]{3}"))) return mapOf("success" to false, "error" to "Asset details are invalid.")
        return stageAction("مراجعة إضافة أصل", "الأصل: " + n + "\nالنوع: " + parsed.name + "\nالقيمة: " + value + " " + cur, PendingPayload.Asset(AssetEntity(name=n,type=parsed,value=value,currency=cur,notes=notes.trim())))
    }

    @Tool(description = "Prepare an internal transfer between two existing accounts. It is executed only after explicit confirmation; currency and funds are revalidated by the repository.")
    fun proposeTransfer(fromAccountId: Long, toAccountId: Long, amount: Double, fee: Double = 0.0, currency: String = "LYD", description: String = ""): Map<String, Any> {
        val cur=currency.trim().uppercase()
        if (fromAccountId<=0 || toAccountId<=0 || fromAccountId==toAccountId || !amount.isFinite() || amount<=0.0 || !fee.isFinite() || fee<0.0 || !cur.matches(Regex("[A-Z]{3}"))) return mapOf("success" to false,"error" to "Transfer details are invalid.")
        val preview=AiActionPreview(UUID.randomUUID().toString(),"مراجعة تحويل بين الحسابات","من الحساب: " + fromAccountId + "\nإلى الحساب: " + toAccountId + "\nالمبلغ: " + amount + " " + cur + "\nالرسوم: " + fee)
        pending=Pending(preview,PendingPayload.Transfer(fromAccountId,toAccountId,amount,fee,cur,description.trim(),"AI-" + System.currentTimeMillis()))
        _pendingAction.value=preview
        return mapOf("success" to true,"requires_user_confirmation" to true,"preview_id" to preview.id,"message" to "Transfer preview only. Wait for user confirmation.")
    }
    @Tool(description = "Prepare a household member. No member is saved until the user confirms the preview.")
    fun proposeCreateHouseholdMember(name: String, role: String, contributionPercent: Double, email: String = ""): Map<String, Any> {
        val cleanName=name.trim(); val cleanRole=role.trim()
        if (cleanName.length !in 2..100 || cleanRole.isBlank() || !contributionPercent.isFinite() || contributionPercent !in 0.0..100.0) return mapOf("success" to false,"error" to "Household member details are invalid.")
        val preview=AiActionPreview(UUID.randomUUID().toString(),"مراجعة إضافة فرد للأسرة","الاسم: " + cleanName + "\nالدور: " + cleanRole + "\nنسبة المساهمة: " + contributionPercent + "%")
        pending=Pending(preview,PendingPayload.HouseholdMember(cleanName,email.trim(),cleanRole,contributionPercent)); _pendingAction.value=preview
        return mapOf("success" to true,"requires_user_confirmation" to true,"preview_id" to preview.id,"message" to "Preview only. Wait for user confirmation.")
    }

    @Tool(description = "Prepare a household expense. The payer member is checked again when confirmed.")
    fun proposeCreateHouseholdExpense(title: String, amount: Double, payerMemberId: Long, category: String, currency: String = "LYD"): Map<String, Any> {
        val cleanTitle=title.trim(); val cleanCategory=category.trim(); val cur=currency.trim().uppercase()
        if (cleanTitle.isBlank() || cleanCategory.isBlank() || payerMemberId<=0 || !amount.isFinite() || amount<=0.0 || !cur.matches(Regex("[A-Z]{3}"))) return mapOf("success" to false,"error" to "Household expense details are invalid.")
        val preview=AiActionPreview(UUID.randomUUID().toString(),"مراجعة مصروف أسري","العنوان: " + cleanTitle + "\nالمبلغ: " + amount + " " + cur + "\nالمسؤول: " + payerMemberId)
        pending=Pending(preview,PendingPayload.HouseholdExpense(cleanTitle,amount,payerMemberId,cleanCategory,cur)); _pendingAction.value=preview
        return mapOf("success" to true,"requires_user_confirmation" to true,"preview_id" to preview.id,"message" to "Preview only. Wait for user confirmation.")
    }


    @Tool(description = "Prepare a real debt payment or debt collection. Reads the debt and account first, validates currency, remaining amount and available funds, then executes atomically only after confirmation.")
    fun proposeRecordDebtPayment(debtId: Long, accountId: Long, amount: Double, notes: String, paymentDate: Long): Map<String, Any> = runBlocking(Dispatchers.IO) {
        val debt = repository.debts.first().firstOrNull { it.id == debtId }
            ?: return@runBlocking mapOf("success" to false, "error" to "Debt not found.")
        val account = repository.accounts.first().firstOrNull { it.id == accountId }
            ?: return@runBlocking mapOf("success" to false, "error" to "Active account not found.")
        if (debt.status.name == "PAID" || !amount.isFinite() || amount <= 0.0 ||
            amount > debt.remaining || paymentDate <= 0L ||
            !debt.currency.equals(account.currency, true) ||
            (debt.direction == "i_owe" && account.balance < amount)
        ) return@runBlocking mapOf("success" to false, "error" to "Debt payment details, currency, remaining amount or account balance are invalid.")
        val action = if (debt.direction == "i_owe") "سداد دين" else "تحصيل دين"
        stageUpdate(
            "مراجعة " + action,
            "الدين: " + debt.name + "\nالمبلغ: " + amount + " " + debt.currency +
                "\nالحساب: " + account.name + "\nالمتبقي بعد العملية: " + (debt.remaining - amount) + " " + debt.currency,
            suspend {
                val ok = repository.recordDebtPayment(debtId, accountId, amount, notes.trim(), paymentDate)
                check(ok) { "تعذر تسجيل دفعة الدين؛ ربما تغير الرصيد أو حالة الدين." }
                "تم تسجيل " + (if (debt.direction == "i_owe") "سداد الدين" else "تحصيل الدين") + " وتحديث الحساب والدين بعد تأكيدك."
            }
        )
    }

    @Tool(description = "Prepare paying an unpaid bill from an existing personal account. Checks account currency and available balance. Payment, transaction, account balance and bill status are updated atomically only after confirmation.")
    fun proposeMarkBillPaid(billId: Long, accountId: Long): Map<String, Any> = runBlocking(Dispatchers.IO) {
        val bill = repository.bills.first().firstOrNull { it.id == billId }
            ?: return@runBlocking mapOf("success" to false, "error" to "Bill not found.")
        if (bill.isPaid) return@runBlocking mapOf("success" to true, "message" to "الفاتورة مدفوعة مسبقاً؛ لم يتم إنشاء دفعة مكررة.")
        val account = repository.accounts.first().firstOrNull { it.id == accountId }
            ?: return@runBlocking mapOf("success" to false, "error" to "Active account not found.")
        if (!bill.amount.isFinite() || bill.amount <= 0.0 ||
            !bill.currency.equals(account.currency, true) || account.balance < bill.amount
        ) return@runBlocking mapOf("success" to false, "error" to "Bill amount, account currency or available balance is invalid.")
        stageUpdate(
            "مراجعة دفع فاتورة",
            "الفاتورة: " + bill.name + "\nالمبلغ: " + bill.amount + " " + bill.currency +
                "\nالحساب: " + account.name,
            suspend {
                val ok = repository.markBillPaid(billId, accountId)
                check(ok) { "تعذر دفع الفاتورة؛ تحقق من الرصيد والحساب وحالة الفاتورة." }
                "تم دفع الفاتورة وتسجيل المصروف وتحديث رصيد الحساب بعد تأكيدك."
            }
        )
    }

    private fun stageUpdate(title: String, description: String, execute: suspend () -> String): Map<String, Any> {
        val preview = AiActionPreview(UUID.randomUUID().toString(), title, description + "\\nلن يُحفظ التعديل قبل تأكيدك.")
        pending = Pending(preview, PendingPayload.Update(execute))
        _pendingAction.value = preview
        return mapOf("success" to true, "requires_user_confirmation" to true, "preview_id" to preview.id,
            "message" to "An update preview is visible. Wait for explicit confirmation in the app.")
    }

    @Tool(description = "Prepare an account details update. Balance, currency and account history are not editable here.")
    fun proposeUpdateAccount(id: Long, name: String, institution: String, notes: String): Map<String, Any> = runBlocking(Dispatchers.IO) {
        val current = repository.accounts.first().firstOrNull { it.id == id } ?: return@runBlocking mapOf("success" to false, "error" to "Active account not found.")
        val n = name.trim()
        if (n.length !in 2..80) return@runBlocking mapOf("success" to false, "error" to "Account name is invalid.")
        stageUpdate("مراجعة تعديل حساب", "الحساب #" + id + "\\nالاسم: " + n + "\\nالمؤسسة: " + institution.trim(), suspend {
            repository.updateAccount(current.copy(name = n, institution = institution.trim(), notes = notes.trim()))
            "تم تحديث بيانات الحساب بعد تأكيدك."
        })
    }

    @Tool(description = "Prepare a transaction status update through repository accounting logic.")
    fun proposeUpdateTransactionStatus(id: Long, newStatus: String): Map<String, Any> = runBlocking(Dispatchers.IO) {
        val current = repository.transactions.first().firstOrNull { it.id == id } ?: return@runBlocking mapOf("success" to false, "error" to "Transaction not found.")
        val status = runCatching { TransactionStatus.valueOf(newStatus.trim().uppercase()) }.getOrNull() ?: return@runBlocking mapOf("success" to false, "error" to "Unsupported transaction status.")
        stageUpdate("مراجعة حالة معاملة", "المعاملة #" + id + "\\n" + current.status + " ← " + status + "\\n" + current.amount + " " + current.currency, suspend {
            repository.updateTransactionStatus(current, status)
            "تم تحديث حالة المعاملة بعد تأكيدك."
        })
    }

    @Tool(description = "Prepare a budget update while preserving period, currency and dates.")
    fun proposeUpdateBudget(id: Long, name: String, category: String, limitAmount: Double): Map<String, Any> = runBlocking(Dispatchers.IO) {
        val current = repository.budgets.first().firstOrNull { it.id == id } ?: return@runBlocking mapOf("success" to false, "error" to "Budget not found.")
        val n = name.trim(); val cat = category.trim()
        if (n.length !in 2..80 || cat.length !in 2..60 || !limitAmount.isFinite() || limitAmount <= 0.0) return@runBlocking mapOf("success" to false, "error" to "Budget values are invalid.")
        stageUpdate("مراجعة تعديل ميزانية", "الميزانية #" + id + "\\n" + n + " — " + cat + "\\nالحد الجديد: " + limitAmount + " " + current.currency, suspend {
            repository.updateBudget(current.copy(name = n, category = cat, limitAmount = limitAmount))
            "تم تحديث الميزانية بعد تأكيدك."
        })
    }

    @Tool(description = "Prepare a debt update preserving debt direction and currency.")
    fun proposeUpdateDebt(id: Long, name: String, principal: Double, remaining: Double): Map<String, Any> = runBlocking(Dispatchers.IO) {
        val current = repository.debts.first().firstOrNull { it.id == id } ?: return@runBlocking mapOf("success" to false, "error" to "Debt not found.")
        val n = name.trim()
        if (n.length !in 2..100 || !principal.isFinite() || principal <= 0.0 || !remaining.isFinite() || remaining !in 0.0..principal) return@runBlocking mapOf("success" to false, "error" to "Debt values are invalid.")
        stageUpdate("مراجعة تعديل دين", "الدين #" + id + "\\n" + n + "\\nالأصل: " + principal + " — المتبقي: " + remaining + " " + current.currency, suspend {
            repository.updateDebt(current.copy(name = n, principal = principal, remaining = remaining))
            "تم تحديث الدين بعد تأكيدك."
        })
    }

    @Tool(description = "Prepare a bill update without changing its paid state or recurrence settings.")
    fun proposeUpdateBill(id: Long, name: String, category: String, amount: Double, dueDate: Long): Map<String, Any> = runBlocking(Dispatchers.IO) {
        val current = repository.bills.first().firstOrNull { it.id == id } ?: return@runBlocking mapOf("success" to false, "error" to "Bill not found.")
        val n = name.trim(); val cat = category.trim()
        if (n.isBlank() || cat.isBlank() || !amount.isFinite() || amount <= 0.0 || dueDate <= 0L) return@runBlocking mapOf("success" to false, "error" to "Bill values are invalid.")
        stageUpdate("مراجعة تعديل فاتورة", "الفاتورة #" + id + "\\n" + n + " — " + cat + "\\n" + amount + " " + current.currency, suspend {
            repository.updateBill(current.copy(name = n, category = cat, amount = amount, dueDate = dueDate))
            "تم تحديث الفاتورة بعد تأكيدك."
        })
    }

    @Tool(description = "Prepare a savings goal update preserving currency.")
    fun proposeUpdateSavingsGoal(id: Long, name: String, targetAmount: Double, currentAmount: Double, deadline: Long): Map<String, Any> = runBlocking(Dispatchers.IO) {
        val current = repository.savingsGoals.first().firstOrNull { it.id == id } ?: return@runBlocking mapOf("success" to false, "error" to "Savings goal not found.")
        val n = name.trim()
        if (n.isBlank() || !targetAmount.isFinite() || targetAmount <= 0.0 || !currentAmount.isFinite() || currentAmount !in 0.0..targetAmount || deadline <= 0L) return@runBlocking mapOf("success" to false, "error" to "Savings goal values are invalid.")
        stageUpdate("مراجعة تعديل هدف ادخار", "الهدف #" + id + "\\n" + n + "\\nالمستهدف: " + targetAmount + " — المحقق: " + currentAmount + " " + current.currency, suspend {
            repository.updateSavingsGoal(current.copy(name = n, targetAmount = targetAmount, currentAmount = currentAmount, deadline = deadline))
            "تم تحديث هدف الادخار بعد تأكيدك."
        })
    }

    @Tool(description = "Prepare an asset update preserving type, currency and purchase date.")
    fun proposeUpdateAsset(id: Long, name: String, value: Double, notes: String): Map<String, Any> = runBlocking(Dispatchers.IO) {
        val current = repository.assets.first().firstOrNull { it.id == id } ?: return@runBlocking mapOf("success" to false, "error" to "Asset not found.")
        val n = name.trim()
        if (n.isBlank() || !value.isFinite() || value < 0.0) return@runBlocking mapOf("success" to false, "error" to "Asset values are invalid.")
        stageUpdate("مراجعة تعديل أصل", "الأصل #" + id + "\\n" + n + "\\nالقيمة: " + value + " " + current.currency, suspend {
            repository.updateAsset(current.copy(name = n, value = value, notes = notes.trim()))
            "تم تحديث الأصل بعد تأكيدك."
        })
    }

    @Tool(description = "Prepare a household member update without exposing email data.")
    fun proposeUpdateHouseholdMember(id: Long, name: String, role: String, contributionPercent: Double): Map<String, Any> = runBlocking(Dispatchers.IO) {
        val current = repository.householdMembers.first().firstOrNull { it.id == id } ?: return@runBlocking mapOf("success" to false, "error" to "Household member not found.")
        val n = name.trim(); val r = role.trim()
        if (n.isBlank() || r.isBlank() || !contributionPercent.isFinite() || contributionPercent !in 0.0..100.0) return@runBlocking mapOf("success" to false, "error" to "Household member values are invalid.")
        stageUpdate("مراجعة تعديل فرد الأسرة", "العضو #" + id + "\\n" + n + " — " + r + "\\nالمساهمة: " + contributionPercent + "%", suspend {
            repository.updateHouseholdMember(current.copy(name = n, role = r, contributionPercent = contributionPercent))
            "تم تحديث بيانات فرد الأسرة بعد تأكيدك."
        })
    }

    @Tool(description = "Prepare a household expense update. Currency remains unchanged.")
    fun proposeUpdateHouseholdExpense(id: Long, title: String, amount: Double, payerMemberId: Long, category: String): Map<String, Any> = runBlocking(Dispatchers.IO) {
        val current = repository.householdExpenses.first().firstOrNull { it.id == id } ?: return@runBlocking mapOf("success" to false, "error" to "Household expense not found.")
        val t = title.trim(); val cat = category.trim()
        if (t.isBlank() || cat.isBlank() || !amount.isFinite() || amount <= 0.0 || payerMemberId <= 0L) return@runBlocking mapOf("success" to false, "error" to "Household expense values are invalid.")
        stageUpdate("مراجعة تعديل مصروف أسري", "المصروف #" + id + "\\n" + t + "\\n" + amount + " " + current.currency, suspend {
            repository.updateHouseholdExpense(current.copy(title = t, amount = amount, payerMemberId = payerMemberId, category = cat))
            "تم تحديث المصروف الأسري بعد تأكيدك."
        })
    }

    @Tool(description = "Prepare marking one notification as read. It changes only after confirmation.")
    fun proposeMarkNotificationRead(id: Long): Map<String, Any> = runBlocking(Dispatchers.IO) {
        val current = repository.notifications.first().firstOrNull { it.id == id } ?: return@runBlocking mapOf("success" to false, "error" to "Notification not found.")
        if (current.isRead) return@runBlocking mapOf("success" to true, "message" to "Notification is already read.")
        stageUpdate("مراجعة قراءة إشعار", "الإشعار #" + id + "\\n" + current.title, suspend {
            repository.markNotificationRead(id)
            "تم وضع الإشعار كمقروء بعد تأكيدك."
        })
    }

    private fun stageAction(title: String, description: String, payload: PendingPayload): Map<String, Any> {
        val preview=AiActionPreview(UUID.randomUUID().toString(),title,description); pending=Pending(preview,payload); _pendingAction.value=preview
        return mapOf("success" to true,"requires_user_confirmation" to true,"preview_id" to preview.id,"message" to "Preview only. Wait for explicit user confirmation; no data has changed.")
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
            is PendingPayload.ProjectFiles -> {
                val saved = workspace.fileWorkspace().writeFiles(payload.projectId, payload.writes)
                "تم حفظ ${saved.size} ملفات داخل مساحة المشروع بعد تأكيدك."
            }
            is PendingPayload.Account -> { repository.addAccount(payload.value); "تم إنشاء الحساب بعد تأكيدك." }
            is PendingPayload.Transaction -> { repository.addTransaction(payload.value); "تم تسجيل المعاملة بعد تأكيدك." }
            is PendingPayload.Debt -> { repository.addDebt(payload.value); "تمت إضافة الدين بعد تأكيدك." }
            is PendingPayload.Bill -> { repository.addBill(payload.value); "تمت إضافة الفاتورة بعد تأكيدك." }
            is PendingPayload.SavingsGoal -> { repository.addSavingsGoal(payload.value); "تمت إضافة هدف الادخار بعد تأكيدك." }
            is PendingPayload.Asset -> { repository.addAsset(payload.value); "تمت إضافة الأصل بعد تأكيدك." }
            is PendingPayload.WorkSource -> { repository.addWorkSource(payload.value); "تمت إضافة مصدر العمل «${payload.value.name}» بعد تأكيدك." }
            is PendingPayload.WorkRecord -> { repository.addWorkRecord(payload.value, payload.transaction); "تم تسجيل حركة العمل «${payload.value.type.name}» فعلياً بعد تأكيدك." }
            is PendingPayload.Transfer -> {
                val completed = repository.executeTransfer(payload.fromId,payload.toId,payload.amount,payload.fee,payload.currency,payload.description,payload.reference)
                check(completed) { "تعذر تنفيذ التحويل؛ تحقق من الحسابات والعملة وحالة الحساب." }
                "تم تنفيذ التحويل بعد تأكيدك."
            }
            is PendingPayload.HouseholdMember -> { repository.addHouseholdMember(payload.name,payload.email,payload.role,payload.percent); "تمت إضافة فرد الأسرة بعد تأكيدك." }
            is PendingPayload.HouseholdExpense -> { repository.addHouseholdExpense(payload.title,payload.amount,payload.payerId,payload.category,payload.currency); "تم تسجيل المصروف الأسري بعد تأكيدك." }
            is PendingPayload.Update -> payload.execute()
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
