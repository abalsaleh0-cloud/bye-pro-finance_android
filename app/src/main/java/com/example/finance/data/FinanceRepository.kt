package com.example.finance.data

import com.example.finance.engine.FinanceEngine
import com.example.finance.model.AccountEntity
import com.example.finance.model.AccountType
import com.example.finance.model.AppLanguage
import com.example.finance.model.AssetEntity
import com.example.finance.model.AssetType
import com.example.finance.model.AuditLogEntity
import com.example.finance.model.BillEntity
import com.example.finance.model.BudgetEntity
import com.example.finance.model.BudgetPeriod
import com.example.finance.model.DebtEntity
import com.example.finance.model.DebtPaymentEntity
import com.example.finance.model.DebtStatus
import com.example.finance.model.DocumentEntity
import com.example.finance.model.DocumentType
import com.example.finance.model.HouseholdExpenseEntity
import com.example.finance.model.HouseholdMemberEntity
import com.example.finance.model.NotificationEntity
import com.example.finance.model.NotificationType
import com.example.finance.model.SavingsGoalEntity
import com.example.finance.model.TransactionEntity
import com.example.finance.model.TransactionStatus
import com.example.finance.model.TransactionType
import com.example.finance.model.TransferEntity
import com.example.finance.model.UserEntity
import com.example.finance.model.UserRole
import androidx.room.withTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class FinanceRepository(
    private val dao: FinanceDao,
    private val db: AppDatabase? = null
) {

    val accounts: Flow<List<AccountEntity>> = dao.getAllAccounts()
    val archivedAccounts: Flow<List<AccountEntity>> = dao.getArchivedAccounts()
    val transactions: Flow<List<TransactionEntity>> = dao.getAllTransactions()
    val transfers: Flow<List<TransferEntity>> = dao.getAllTransfers()
    val budgets: Flow<List<BudgetEntity>> = dao.getAllBudgets()
    val debts: Flow<List<DebtEntity>> = dao.getAllDebts()
    val bills: Flow<List<BillEntity>> = dao.getAllBills()
    val savingsGoals: Flow<List<SavingsGoalEntity>> = dao.getAllSavingsGoals()
    val assets: Flow<List<AssetEntity>> = dao.getAllAssets()
    val householdMembers: Flow<List<HouseholdMemberEntity>> = dao.getAllHouseholdMembers()
    val householdExpenses: Flow<List<HouseholdExpenseEntity>> = dao.getAllHouseholdExpenses()
    val documents: Flow<List<DocumentEntity>> = dao.getAllDocuments()
    val notifications: Flow<List<NotificationEntity>> = dao.getAllNotifications()
    val auditLogs: Flow<List<AuditLogEntity>> = dao.getAllAuditLogs()
    val currentUser: Flow<UserEntity?> = dao.getCurrentUser()

    suspend fun logAudit(action: String, resource: String, details: String) {
        dao.insertAuditLog(AuditLogEntity(action = action, resource = resource, details = details, timestamp = System.currentTimeMillis()))
    }

    suspend fun addAccount(account: AccountEntity): Long = withContext(Dispatchers.IO) {
        val id = dao.insertAccount(account)
        logAudit("CREATE", "Account", "Created account: ${account.name} with balance ${account.balance} ${account.currency}")
        id
    }

    suspend fun updateAccount(account: AccountEntity) = withContext(Dispatchers.IO) { dao.updateAccount(account); logAudit("UPDATE", "Account", "Updated account ${account.name}") }
    suspend fun deleteAccount(account: AccountEntity) = withContext(Dispatchers.IO) { dao.deleteAccount(account); logAudit("DELETE", "Account", "Deleted account ${account.name}") }

    suspend fun addTransaction(tx: TransactionEntity): Long = withContext(Dispatchers.IO) {
        val operation: suspend () -> Long = {
            val account = dao.getAccountById(tx.accountId) ?: throw IllegalArgumentException("Account not found: ${tx.accountId}")
            require(!account.isArchived) { "Cannot add a transaction to an inactive account" }
            require(tx.amount.isFinite() && tx.amount > 0.0) { "Transaction amount must be a finite positive number" }
            require(tx.currency.equals(account.currency, ignoreCase = true)) { "Transaction currency must match the selected account currency" }
            val txId = dao.insertTransaction(tx)
            if (tx.status == TransactionStatus.COMPLETED) postTransactionBalance(account, tx, apply = true)
            logAudit("TRANSACTION", "Transaction", "${tx.type}: ${tx.amount} ${tx.currency} - ${tx.description}")
            txId
        }
        if (db != null) db.withTransaction { operation() } else operation()
    }

    private suspend fun postTransactionBalance(account: AccountEntity, tx: TransactionEntity, apply: Boolean) {
        val current = FinanceEngine.toBigDecimal(account.balance)
        val amount = FinanceEngine.toBigDecimal(tx.amount)
        val delta = when (tx.type) {
            TransactionType.INCOME, TransactionType.REFUND, TransactionType.ADJUSTMENT -> amount
            TransactionType.EXPENSE, TransactionType.TRANSFER -> amount.negate()
        }
        val signed = if (apply) delta else delta.negate()
        dao.updateAccountBalance(account.id, current.add(signed).toDouble())
    }

    suspend fun updateTransactionStatus(tx: TransactionEntity, newStatus: TransactionStatus) = withContext(Dispatchers.IO) {
        if (tx.status == newStatus) return@withContext
        val operation: suspend () -> Unit = {
            val account = dao.getAccountById(tx.accountId)
            if (account != null) {
                if (tx.status == TransactionStatus.COMPLETED && newStatus != TransactionStatus.COMPLETED) postTransactionBalance(account, tx, apply = false)
                if (tx.status != TransactionStatus.COMPLETED && newStatus == TransactionStatus.COMPLETED) postTransactionBalance(account, tx, apply = true)
            }
            dao.updateTransaction(tx.copy(status = newStatus))
            logAudit("STATUS", "Transaction", "Transaction #${tx.id} status: ${tx.status} -> $newStatus")
        }
        if (db != null) db.withTransaction { operation() } else operation()
    }

    suspend fun importTransactions(rows: List<TransactionEntity>): Int = withContext(Dispatchers.IO) {
        require(rows.isNotEmpty()) { "The import file contains no transactions" }
        val operation: suspend () -> Int = {
            val existing = dao.getAllTransactionsSnapshot().mapTo(mutableSetOf()) { it.copy(id = 0L) }
            var importedCount = 0
            rows.forEach { source ->
                val tx = source.copy(id = 0L)
                if (!existing.add(tx)) return@forEach
                require(tx.amount.isFinite() && tx.amount > 0.0) { "Imported transaction amount must be finite and positive" }
                val account = dao.getAccountById(tx.accountId) ?: throw IllegalArgumentException("Imported transaction references a missing account: ${tx.accountId}")
                require(tx.currency.equals(account.currency, ignoreCase = true)) { "Imported transaction currency does not match account ${account.id}" }
                dao.insertTransaction(tx)
                if (tx.status == TransactionStatus.COMPLETED) postTransactionBalance(account, tx, apply = true)
                importedCount++
            }
            if (importedCount > 0) logAudit("IMPORT", "Transactions", "Imported $importedCount transactions")
            importedCount
        }
        if (db != null) db.withTransaction { operation() } else operation()
    }

    suspend fun deleteTransaction(tx: TransactionEntity) = withContext(Dispatchers.IO) {
        val operation: suspend () -> Unit = {
            val account = dao.getAccountById(tx.accountId)
            if (account != null && tx.status == TransactionStatus.COMPLETED) postTransactionBalance(account, tx, apply = false)
            dao.deleteTransaction(tx)
            logAudit("DELETE", "Transaction", "Deleted transaction #${tx.id}: ${tx.description}")
        }
        if (db != null) db.withTransaction { operation() } else operation()
    }

    suspend fun archiveAccount(accountId: Long, isArchived: Boolean) = withContext(Dispatchers.IO) { dao.setAccountArchived(accountId, isArchived); logAudit(if (isArchived) "ARCHIVE" else "UNARCHIVE", "Account", "Account #$accountId archive status: $isArchived") }

    suspend fun executeTransfer(fromAccountId: Long, toAccountId: Long, amount: Double, fee: Double, currency: String, description: String, reference: String): Boolean = withContext(Dispatchers.IO) {
        require(amount.isFinite() && amount > 0.0) { "Transfer amount must be finite and positive" }
        require(fee.isFinite() && fee >= 0.0) { "Transfer fee must be finite and non-negative" }
        require(fromAccountId != toAccountId) { "Cannot transfer to the same account" }
        val transferOperation: suspend () -> Boolean = transferOp@{
            val fromAcc = dao.getAccountById(fromAccountId) ?: return@transferOp false
            val toAcc = dao.getAccountById(toAccountId) ?: return@transferOp false
            if (fromAcc.isArchived || toAcc.isArchived || !fromAcc.currency.equals(toAcc.currency, ignoreCase = true)) return@transferOp false
            require(currency.trim().equals(fromAcc.currency.trim(), ignoreCase = true)) { "Transfer currency must match the source account currency" }
            if (!fromAcc.balance.isFinite() || !toAcc.balance.isFinite()) return@transferOp false
            val transferAmount = FinanceEngine.toBigDecimal(amount); val transferFee = FinanceEngine.toBigDecimal(fee); val totalDeduction = transferAmount.add(transferFee)
            val fromBalance = FinanceEngine.toBigDecimal(fromAcc.balance); val toBalance = FinanceEngine.toBigDecimal(toAcc.balance)
            if (fromBalance < totalDeduction) throw IllegalStateException("Insufficient funds: balance $fromBalance < deduction $totalDeduction")
            dao.updateAccountBalance(fromAccountId, fromBalance.subtract(totalDeduction).toDouble())
            dao.updateAccountBalance(toAccountId, toBalance.add(transferAmount).toDouble())
            dao.insertTransfer(TransferEntity(fromAccountId = fromAccountId, toAccountId = toAccountId, amount = amount, fee = fee, currency = currency, description = description, reference = reference))
            dao.insertTransaction(TransactionEntity(accountId = fromAccountId, type = TransactionType.TRANSFER, amount = amount, currency = fromAcc.currency, category = "Transfer", description = "Transfer to ${toAcc.name}: $description"))
            if (transferFee > BigDecimal.ZERO) dao.insertTransaction(TransactionEntity(accountId = fromAccountId, type = TransactionType.EXPENSE, amount = fee, currency = fromAcc.currency, category = "Transfer Fee", description = "Transfer fee: $description"))
            dao.insertTransaction(TransactionEntity(accountId = toAccountId, type = TransactionType.ADJUSTMENT, amount = transferAmount.toDouble(), currency = toAcc.currency, category = "Transfer", description = "Transfer received from ${fromAcc.name}: $description"))
            logAudit("TRANSFER", "Transfer", "Transferred $amount ${fromAcc.currency} to ${toAcc.name} (credited $transferAmount ${toAcc.currency})")
            true
        }
        if (db != null) db.withTransaction { transferOperation() } else transferOperation()
    }

    suspend fun restoreFromJson(backupJson: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = org.json.JSONObject(backupJson)
            if (root.optString("entity") == "transactions") { importTransactions(FinanceExportService.transactionsFromJson(backupJson)); return@withContext true }
            val accountsArray = root.optJSONArray("accounts")
            if (accountsArray != null) for (i in 0 until accountsArray.length()) {
                val obj = accountsArray.getJSONObject(i)
                dao.insertAccount(AccountEntity(id = obj.optLong("id", 0L), name = obj.getString("name"), type = AccountType.valueOf(obj.getString("type")), currency = obj.optString("currency", "LYD"), balance = obj.getDouble("balance"), accountNumberSafe = obj.optString("accountNumberSafe", ""), institution = obj.optString("institution", "")))
            }
            logAudit("RESTORE", "Database", "Restored accounts and configuration from backup JSON"); true
        } catch (_: Exception) { false }
    }

    suspend fun addBudget(budget: BudgetEntity) = withContext(Dispatchers.IO) { dao.insertBudget(budget); logAudit("CREATE", "Budget", "Created budget for ${budget.category}: ${budget.limitAmount} ${budget.currency}") }
    suspend fun deleteBudget(budget: BudgetEntity) = withContext(Dispatchers.IO) { dao.deleteBudget(budget); logAudit("DELETE", "Budget", "Deleted budget ${budget.name}") }
    suspend fun addDebt(debt: DebtEntity) = withContext(Dispatchers.IO) { dao.insertDebt(debt); logAudit("CREATE", "Debt", "Added debt ${debt.name} (${debt.remaining} ${debt.currency})") }
    suspend fun recordDebtPayment(debtId: Long, amount: Double, notes: String): Boolean = withContext(Dispatchers.IO) {
        val operation: suspend () -> Boolean = operation@{ val debt = dao.getDebtById(debtId) ?: return@operation false; if (!amount.isFinite() || amount <= 0.0 || !debt.remaining.isFinite() || amount > debt.remaining) return@operation false; val remaining = FinanceEngine.toBigDecimal(debt.remaining).subtract(FinanceEngine.toBigDecimal(amount)); dao.updateDebt(debt.copy(remaining = remaining.toDouble(), status = if (remaining <= BigDecimal.ZERO) DebtStatus.PAID else DebtStatus.ACTIVE)); dao.insertDebtPayment(DebtPaymentEntity(debtId = debtId, amount = amount, notes = notes)); logAudit("PAYMENT", "Debt", "Paid $amount toward ${debt.name}, remaining $remaining"); true }
        if (db != null) db.withTransaction { operation() } else operation()
    }
    suspend fun deleteDebt(debt: DebtEntity) = withContext(Dispatchers.IO) { dao.deleteDebt(debt); logAudit("DELETE", "Debt", "Deleted debt ${debt.name}") }
    suspend fun addBill(bill: BillEntity) = withContext(Dispatchers.IO) { dao.insertBill(bill); logAudit("CREATE", "Bill", "Created bill ${bill.name} (${bill.amount} ${bill.currency})") }
    suspend fun markBillPaid(billId: Long, accountId: Long): Boolean = withContext(Dispatchers.IO) {
        val operation: suspend () -> Boolean = operation@{ val bill = dao.getBillById(billId) ?: return@operation false; if (bill.isPaid) return@operation true; val account = dao.getAccountById(accountId) ?: return@operation false; if (!bill.amount.isFinite() || bill.amount <= 0.0 || !bill.currency.equals(account.currency, ignoreCase = true) || !account.balance.isFinite() || account.balance < bill.amount) return@operation false; val tx = TransactionEntity(accountId = accountId, type = TransactionType.EXPENSE, amount = bill.amount, currency = bill.currency, category = bill.category, description = "Bill payment: ${bill.name}"); dao.insertTransaction(tx); postTransactionBalance(account, tx, apply = true); dao.updateBill(bill.copy(isPaid = true)); logAudit("PAYMENT", "Bill", "Marked bill ${bill.name} as paid"); true }
        if (db != null) db.withTransaction { operation() } else operation()
    }
    suspend fun deleteBill(bill: BillEntity) = withContext(Dispatchers.IO) { dao.deleteBill(bill); logAudit("DELETE", "Bill", "Deleted bill ${bill.name}") }
    suspend fun addSavingsGoal(goal: SavingsGoalEntity) = withContext(Dispatchers.IO) { dao.insertSavingsGoal(goal); logAudit("CREATE", "SavingsGoal", "Created savings goal ${goal.name}") }
    suspend fun contributeToSavings(goalId: Long, amount: Double, accountId: Long): Boolean = withContext(Dispatchers.IO) { val goal = dao.getSavingsGoalById(goalId) ?: return@withContext false; val account = dao.getAccountById(accountId) ?: return@withContext false; if (!amount.isFinite() || amount <= 0 || account.currency != goal.currency || account.balance < amount) return@withContext false; val tx = TransactionEntity(accountId = accountId, type = TransactionType.EXPENSE, amount = amount, currency = account.currency, category = "Savings", description = "Savings contribution: ${goal.name}"); dao.insertTransaction(tx); postTransactionBalance(account, tx, apply = true); dao.updateSavingsGoal(goal.copy(currentAmount = (goal.currentAmount + amount).coerceAtMost(goal.targetAmount))); true }
    suspend fun deleteSavingsGoal(goal: SavingsGoalEntity) = withContext(Dispatchers.IO) { dao.deleteSavingsGoal(goal); logAudit("DELETE", "SavingsGoal", "Deleted savings goal ${goal.name}") }
    suspend fun addAsset(asset: AssetEntity) = withContext(Dispatchers.IO) { dao.insertAsset(asset); logAudit("CREATE", "Asset", "Added asset ${asset.name}") }
    suspend fun deleteAsset(asset: AssetEntity) = withContext(Dispatchers.IO) { dao.deleteAsset(asset); logAudit("DELETE", "Asset", "Deleted asset ${asset.name}") }
    suspend fun addHouseholdMember(name: String, email: String, role: String, percent: Double) = withContext(Dispatchers.IO) { dao.insertHouseholdMember(HouseholdMemberEntity(name = name, email = email, role = role, contributionPercent = percent)) }
    suspend fun addHouseholdExpense(title: String, amount: Double, payerId: Long, category: String, currency: String) = withContext(Dispatchers.IO) { dao.insertHouseholdExpense(HouseholdExpenseEntity(title = title, amount = amount, payerMemberId = payerId, category = category, currency = currency)) }
    suspend fun addDocument(doc: DocumentEntity) = withContext(Dispatchers.IO) { dao.insertDocument(doc); logAudit("CREATE", "Document", "Added document ${doc.title}") }
    suspend fun deleteDocument(doc: DocumentEntity) = withContext(Dispatchers.IO) { dao.deleteDocument(doc); logAudit("DELETE", "Document", "Deleted document ${doc.title}") }
    suspend fun markNotificationRead(id: Long) = withContext(Dispatchers.IO) { dao.markNotificationAsRead(id) }
    suspend fun clearNotifications() = withContext(Dispatchers.IO) { dao.clearAllNotifications() }
    suspend fun updateCurrentUser(user: UserEntity) = withContext(Dispatchers.IO) { dao.insertOrUpdateUser(user) }
}
