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
        dao.insertAuditLog(
            AuditLogEntity(
                action = action,
                resource = resource,
                details = details,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun addAccount(account: AccountEntity): Long = withContext(Dispatchers.IO) {
        val id = dao.insertAccount(account)
        logAudit("CREATE", "Account", "Created account: ${account.name} with balance ${account.balance} ${account.currency}")
        id
    }

    suspend fun updateAccount(account: AccountEntity) = withContext(Dispatchers.IO) {
        dao.updateAccount(account)
        logAudit("UPDATE", "Account", "Updated account ${account.name}")
    }

    suspend fun deleteAccount(account: AccountEntity) = withContext(Dispatchers.IO) {
        dao.deleteAccount(account)
        logAudit("DELETE", "Account", "Deleted account ${account.name}")
    }

    suspend fun addTransaction(tx: TransactionEntity): Long = withContext(Dispatchers.IO) {
        val operation: suspend () -> Long = {
            val account = dao.getAccountById(tx.accountId)
                ?: throw IllegalArgumentException("Account not found: ${tx.accountId}")
            require(tx.amount.isFinite() && tx.amount > 0.0) { "Transaction amount must be a finite positive number" }
            require(tx.currency.equals(account.currency, ignoreCase = true)) {
                "Transaction currency must match the selected account currency"
            }
            val txId = dao.insertTransaction(tx)
            if (tx.status == TransactionStatus.COMPLETED) {
                val currentBal = FinanceEngine.toBigDecimal(account.balance)
                val txAmount = FinanceEngine.toBigDecimal(tx.amount)
                val newBal = when (tx.type) {
                    TransactionType.INCOME, TransactionType.REFUND -> currentBal.add(txAmount)
                    TransactionType.EXPENSE, TransactionType.TRANSFER -> currentBal.subtract(txAmount)
                    TransactionType.ADJUSTMENT -> currentBal.add(txAmount)
                }
                dao.updateAccountBalance(account.id, newBal.toDouble())
            }
            logAudit("TRANSACTION", "Transaction", "${tx.type}: ${tx.amount} ${tx.currency} - ${tx.description}")
            txId
        }
        if (db != null) db.withTransaction { operation() } else operation()
    }

    suspend fun updateTransactionStatus(tx: TransactionEntity, newStatus: TransactionStatus) = withContext(Dispatchers.IO) {
        if (tx.status == newStatus) return@withContext
        val operation: suspend () -> Unit = {
            val account = dao.getAccountById(tx.accountId)
            if (account != null) {
                if (tx.status == TransactionStatus.COMPLETED && newStatus != TransactionStatus.COMPLETED) {
                    val current = FinanceEngine.toBigDecimal(account.balance)
                    val amount = FinanceEngine.toBigDecimal(tx.amount)
                    val reversed = when (tx.type) {
                        TransactionType.INCOME, TransactionType.REFUND, TransactionType.ADJUSTMENT -> current.subtract(amount)
                        TransactionType.EXPENSE, TransactionType.TRANSFER -> current.add(amount)
                    }
                    dao.updateAccountBalance(account.id, reversed.toDouble())
                } else if (tx.status != TransactionStatus.COMPLETED && newStatus == TransactionStatus.COMPLETED) {
                    val current = FinanceEngine.toBigDecimal(account.balance)
                    val amount = FinanceEngine.toBigDecimal(tx.amount)
                    val posted = when (tx.type) {
                        TransactionType.INCOME, TransactionType.REFUND, TransactionType.ADJUSTMENT -> current.add(amount)
                        TransactionType.EXPENSE, TransactionType.TRANSFER -> current.subtract(amount)
                    }
                    dao.updateAccountBalance(account.id, posted.toDouble())
                }
            }
            dao.updateTransaction(tx.copy(status = newStatus))
            logAudit("STATUS", "Transaction", "Transaction #${tx.id} status: ${tx.status} -> $newStatus")
        }
        if (db != null) db.withTransaction { operation() } else operation()
    }

    suspend fun importTransactions(rows: List<TransactionEntity>): Int = withContext(Dispatchers.IO) {
        require(rows.isNotEmpty()) { "The import file contains no transactions" }
        val operation: suspend () -> Int = {
            val existing = dao.getAllTransactionsSnapshot()
                .mapTo(mutableSetOf()) { it.copy(id = 0L) }
            var importedCount = 0
            rows.forEach { source ->
                val tx = source.copy(id = 0L)
                // Stable entity equality prevents importing the same export twice and double-posting balances.
                if (!existing.add(tx)) return@forEach
                require(tx.amount.isFinite() && tx.amount > 0.0) { "Imported transaction amount must be finite and positive" }
                val account = dao.getAccountById(tx.accountId)
                    ?: throw IllegalArgumentException("Imported transaction references a missing account: ${tx.accountId}")
                require(tx.currency.equals(account.currency, ignoreCase = true)) {
                    "Imported transaction currency does not match account ${account.id}"
                }
                dao.insertTransaction(tx)
                if (tx.status == TransactionStatus.COMPLETED) {
                    val current = FinanceEngine.toBigDecimal(account.balance)
                    val amount = FinanceEngine.toBigDecimal(tx.amount)
                    val updated = when (tx.type) {
                        TransactionType.INCOME, TransactionType.REFUND, TransactionType.ADJUSTMENT -> current.add(amount)
                        TransactionType.EXPENSE, TransactionType.TRANSFER -> current.subtract(amount)
                    }
                    dao.updateAccountBalance(account.id, updated.toDouble())
                }
                importedCount++
            }
            if (importedCount > 0) {
                logAudit("IMPORT", "Transactions", "Imported $importedCount transactions")
            }
            importedCount
        }
        if (db != null) db.withTransaction { operation() } else operation()
    }

    suspend fun deleteTransaction(tx: TransactionEntity) = withContext(Dispatchers.IO) {
        val operation: suspend () -> Unit = {
            val account = dao.getAccountById(tx.accountId)
            if (account != null && tx.status == TransactionStatus.COMPLETED) {
                val currentBal = FinanceEngine.toBigDecimal(account.balance)
                val txAmount = FinanceEngine.toBigDecimal(tx.amount)
                // Reverse only posted rows; adjustments are additive deltas.
                val newBal = when (tx.type) {
                    TransactionType.INCOME, TransactionType.REFUND -> currentBal.subtract(txAmount)
                    TransactionType.EXPENSE, TransactionType.TRANSFER -> currentBal.add(txAmount)
                    TransactionType.ADJUSTMENT -> currentBal.subtract(txAmount)
                }
                dao.updateAccountBalance(account.id, newBal.toDouble())
            }
            dao.deleteTransaction(tx)
            logAudit("DELETE", "Transaction", "Deleted transaction #${tx.id}: ${tx.description}")
        }
        if (db != null) db.withTransaction { operation() } else operation()
    }

    suspend fun archiveAccount(accountId: Long, isArchived: Boolean) = withContext(Dispatchers.IO) {
        dao.setAccountArchived(accountId, isArchived)
        val action = if (isArchived) "ARCHIVE" else "UNARCHIVE"
        logAudit(action, "Account", "Account #$accountId archive status: $isArchived")
    }

    suspend fun executeTransfer(
        fromAccountId: Long,
        toAccountId: Long,
        amount: Double,
        fee: Double,
        currency: String,
        description: String,
        reference: String
    ): Boolean = withContext(Dispatchers.IO) {
        require(amount.isFinite() && amount > 0.0) { "Transfer amount must be finite and positive" }
        require(fee.isFinite() && fee >= 0.0) { "Transfer fee must be finite and non-negative" }
        require(fromAccountId != toAccountId) { "Cannot transfer to the same account" }

        val transferOperation: suspend () -> Boolean = transferOp@{
            val fromAcc = dao.getAccountById(fromAccountId) ?: return@transferOp false
            val toAcc = dao.getAccountById(toAccountId) ?: return@transferOp false

            if (fromAcc.isArchived || toAcc.isArchived) return@transferOp false
            // The web app requires both accounts and the transfer to use one identical currency.
            if (!fromAcc.currency.equals(toAcc.currency, ignoreCase = true)) return@transferOp false
            require(currency.trim().equals(fromAcc.currency.trim(), ignoreCase = true)) {
                "Transfer currency must match the source account currency"
            }
            if (!fromAcc.balance.isFinite() || !toAcc.balance.isFinite()) return@transferOp false

            val transferAmount = FinanceEngine.toBigDecimal(amount)
            val transferFee = FinanceEngine.toBigDecimal(fee)
            val totalDeduction = transferAmount.add(transferFee)
            val fromBalance = FinanceEngine.toBigDecimal(fromAcc.balance)
            val toBalance = FinanceEngine.toBigDecimal(toAcc.balance)
            if (fromBalance < totalDeduction) {
                throw IllegalStateException("Insufficient funds: balance $fromBalance < deduction $totalDeduction")
            }
            val creditAmount = transferAmount

            val newFromBal = fromBalance.subtract(totalDeduction)
            val newToBal = toBalance.add(creditAmount)

            dao.updateAccountBalance(fromAccountId, newFromBal.toDouble())
            dao.updateAccountBalance(toAccountId, newToBal.toDouble())

            dao.insertTransfer(
                TransferEntity(
                    fromAccountId = fromAccountId,
                    toAccountId = toAccountId,
                    amount = amount,
                    fee = fee,
                    currency = currency,
                    description = description,
                    reference = reference
                )
            )

            // Keep each ledger row in the currency of the account it changes.
            // The transfer principal is excluded from cash-flow totals; the fee is an expense.
            dao.insertTransaction(
                TransactionEntity(
                    accountId = fromAccountId,
                    type = TransactionType.TRANSFER,
                    amount = amount,
                    currency = fromAcc.currency,
                    category = "Transfer",
                    description = "Transfer to ${toAcc.name}: $description"
                )
            )
            if (transferFee > BigDecimal.ZERO) {
                dao.insertTransaction(
                    TransactionEntity(
                        accountId = fromAccountId,
                        type = TransactionType.EXPENSE,
                        amount = fee,
                        currency = fromAcc.currency,
                        category = "Transfer Fee",
                        description = "Transfer fee: $description"
                    )
                )
            }
            dao.insertTransaction(
                TransactionEntity(
                    accountId = toAccountId,
                    type = TransactionType.ADJUSTMENT,
                    amount = creditAmount.toDouble(),
                    currency = toAcc.currency,
                    category = "Transfer",
                    description = "Transfer received from ${fromAcc.name}: $description"
                )
            )

            logAudit("TRANSFER", "Transfer", "Transferred $amount ${fromAcc.currency} to ${toAcc.name} (credited $creditAmount ${toAcc.currency})")
            true
        }

        if (db != null) {
            db.withTransaction { transferOperation() }
        } else {
            transferOperation()
        }
    }

    suspend fun restoreFromJson(backupJson: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = org.json.JSONObject(backupJson)
            if (root.optString("entity") == "transactions") {
                importTransactions(FinanceExportService.transactionsFromJson(backupJson))
                return@withContext true
            }
            val accountsArray = root.optJSONArray("accounts")
            if (accountsArray != null) {
                for (i in 0 until accountsArray.length()) {
                    val obj = accountsArray.getJSONObject(i)
                    dao.insertAccount(
                        AccountEntity(
                            id = obj.optLong("id", 0L),
                            name = obj.getString("name"),
                            type = AccountType.valueOf(obj.getString("type")),
                            currency = obj.optString("currency", "LYD"),
                            balance = obj.getDouble("balance"),
                            accountNumberSafe = obj.optString("accountNumberSafe", ""),
                            institution = obj.optString("institution", "")
                        )
                    )
                }
            }
            logAudit("RESTORE", "Database", "Restored accounts and configuration from backup JSON")
            true
        } catch (_: Exception) {
            false
        }
    }

    // Budgets
    suspend fun addBudget(budget: BudgetEntity) = withContext(Dispatchers.IO) {
        require(budget.name.isNotBlank()) { "Budget name is required" }
        require(budget.limitAmount.isFinite() && budget.limitAmount > 0.0) { "Budget amount must be finite and positive" }
        require(budget.endDate >= budget.startDate) { "Budget end date must not be before start date" }
        dao.insertBudget(budget)
        logAudit("CREATE", "Budget", "Created budget for ${budget.category}: ${budget.limitAmount} ${budget.currency}")
    }

    suspend fun updateBudget(budget: BudgetEntity) = withContext(Dispatchers.IO) {
        require(budget.id > 0L) { "Budget id is required for update" }
        require(budget.name.isNotBlank()) { "Budget name is required" }
        require(budget.limitAmount.isFinite() && budget.limitAmount > 0.0) { "Budget amount must be finite and positive" }
        require(budget.endDate >= budget.startDate) { "Budget end date must not be before start date" }
        dao.updateBudget(budget)
        logAudit("UPDATE", "Budget", "Updated budget ${budget.name}: ${budget.limitAmount} ${budget.currency}")
    }

    suspend fun deleteBudget(budget: BudgetEntity) = withContext(Dispatchers.IO) {
        dao.deleteBudget(budget)
        logAudit("DELETE", "Budget", "Deleted budget ${budget.name}")
    }

    // Debts
    suspend fun addDebt(debt: DebtEntity) = withContext(Dispatchers.IO) {
        require(debt.name.isNotBlank()) { "Debt party name is required" }
        require(debt.principal.isFinite() && debt.principal > 0.0) { "Debt principal must be positive" }
        require(debt.remaining.isFinite() && debt.remaining in 0.0..debt.principal) { "Invalid remaining debt amount" }
        require(debt.direction == "owed_to_me" || debt.direction == "i_owe") { "Invalid debt direction" }
        require(debt.currency.matches(Regex("[A-Z]{3}"))) { "Invalid debt currency" }
        dao.insertDebt(debt)
        logAudit("CREATE", "Debt", "Added debt ${debt.name} (${debt.remaining} ${debt.currency})")
    }

    suspend fun updateDebt(debt: DebtEntity) = withContext(Dispatchers.IO) {
        require(debt.name.isNotBlank()) { "Debt party name is required" }
        require(debt.principal.isFinite() && debt.principal > 0.0) { "Debt principal must be positive" }
        require(debt.remaining.isFinite() && debt.remaining in 0.0..debt.principal) { "Invalid remaining debt amount" }
        require(debt.direction == "owed_to_me" || debt.direction == "i_owe") { "Invalid debt direction" }
        require(debt.currency.matches(Regex("[A-Z]{3}"))) { "Invalid debt currency" }
        dao.updateDebt(debt)
        logAudit("UPDATE", "Debt", "Updated debt ${debt.name}")
    }

    val debtPayments: (Long) -> Flow<List<DebtPaymentEntity>> = { debtId -> dao.getDebtPayments(debtId) }

    suspend fun recordDebtPayment(
        debtId: Long,
        amount: Double,
        notes: String,
        paymentDate: Long = System.currentTimeMillis()
    ): Boolean = withContext(Dispatchers.IO) {
        val operation: suspend () -> Boolean = operation@{
            val debt = dao.getDebtById(debtId) ?: return@operation false
            if (debt.status == DebtStatus.PAID || !amount.isFinite() || amount <= 0.0 ||
                !debt.remaining.isFinite() || amount > debt.remaining || paymentDate <= 0L
            ) return@operation false
            val remaining = FinanceEngine.toBigDecimal(debt.remaining)
                .subtract(FinanceEngine.toBigDecimal(amount))
            val newStatus = if (remaining <= BigDecimal.ZERO) DebtStatus.PAID else DebtStatus.ACTIVE
            dao.updateDebt(debt.copy(remaining = remaining.toDouble(), status = newStatus))
            dao.insertDebtPayment(DebtPaymentEntity(debtId = debtId, amount = amount, date = paymentDate, notes = notes.trim(), currency = debt.currency))
            logAudit("PAYMENT", "Debt", "Paid $amount toward ${debt.name}, remaining $remaining")
            true
        }
        if (db != null) db.withTransaction { operation() } else operation()
    }

    suspend fun deleteDebt(debt: DebtEntity) = withContext(Dispatchers.IO) {
        dao.deleteDebt(debt)
        logAudit("DELETE", "Debt", "Deleted debt ${debt.name}")
    }

    // Bills
    suspend fun addBill(bill: BillEntity) = withContext(Dispatchers.IO) {
        require(bill.name.isNotBlank() && bill.category.isNotBlank()) { "Bill name and category are required" }
        require(bill.amount.isFinite() && bill.amount > 0.0) { "Bill amount must be finite and positive" }
        require(bill.currency.matches(Regex("[A-Z]{3}"))) { "Bill currency must be a three-letter code" }
        require(bill.recurrenceIntervalDays > 0 && bill.reminderDaysBefore >= 0) { "Bill recurrence and reminder values are invalid" }
        dao.insertBill(bill)
        logAudit("CREATE", "Bill", "Created bill ${bill.name} (${bill.amount} ${bill.currency})")
    }

    suspend fun updateBill(bill: BillEntity) = withContext(Dispatchers.IO) {
        require(bill.id > 0 && bill.name.isNotBlank() && bill.category.isNotBlank()) { "Bill identity, name and category are required" }
        require(bill.amount.isFinite() && bill.amount > 0.0) { "Bill amount must be finite and positive" }
        require(bill.currency.matches(Regex("[A-Z]{3}"))) { "Bill currency must be a three-letter code" }
        require(bill.recurrenceIntervalDays > 0 && bill.reminderDaysBefore >= 0) { "Bill recurrence and reminder values are invalid" }
        val existing = dao.getBillById(bill.id) ?: return@withContext
        require(!existing.isPaid || (bill.amount == existing.amount && bill.currency.equals(existing.currency, ignoreCase = true))) {
            "Paid bill amount and currency cannot change because the expense is already posted"
        }
        dao.updateBill(bill.copy(isPaid = existing.isPaid))
        logAudit("UPDATE", "Bill", "Updated bill ${bill.name}")
    }

    suspend fun markBillPaid(billId: Long, accountId: Long): Boolean = withContext(Dispatchers.IO) {
        val operation: suspend () -> Boolean = operation@{
            val bill = dao.getBillById(billId)
                ?: return@operation false
            if (bill.isPaid) return@operation true
            val account = dao.getAccountById(accountId) ?: return@operation false
            if (!bill.amount.isFinite() || bill.amount <= 0.0 ||
                !bill.currency.equals(account.currency, ignoreCase = true) ||
                !account.balance.isFinite() || account.balance < bill.amount
            ) return@operation false

            dao.insertTransaction(
                TransactionEntity(
                    accountId = accountId,
                    type = TransactionType.EXPENSE,
                    amount = bill.amount,
                    currency = bill.currency,
                    category = bill.category,
                    description = "Bill payment: ${bill.name}"
                )
            )
            dao.updateAccountBalance(
                accountId,
                FinanceEngine.toBigDecimal(account.balance)
                    .subtract(FinanceEngine.toBigDecimal(bill.amount)).toDouble()
            )
            dao.updateBill(bill.copy(isPaid = true))
            logAudit("PAYMENT", "Bill", "Marked bill ${bill.name} as paid")
            true
        }
        if (db != null) db.withTransaction { operation() } else operation()
    }

    suspend fun deleteBill(bill: BillEntity) = withContext(Dispatchers.IO) {
        dao.deleteBill(bill)
        logAudit("DELETE", "Bill", "Deleted bill ${bill.name}")
    }

    // Savings
    suspend fun addSavingsGoal(goal: SavingsGoalEntity) = withContext(Dispatchers.IO) {
        require(goal.name.isNotBlank()) { "Savings goal name is required" }
        require(goal.targetAmount.isFinite() && goal.targetAmount > 0.0) { "Savings target must be finite and positive" }
        require(goal.currentAmount.isFinite() && goal.currentAmount >= 0.0 && goal.currentAmount <= goal.targetAmount) { "Current savings must be between zero and target" }
        require(goal.currency.matches(Regex("[A-Z]{3}"))) { "Savings currency must be a three-letter code" }
        require(goal.targetMonthlyDeposit.isFinite() && goal.targetMonthlyDeposit >= 0.0) { "Monthly deposit must be non-negative" }
        dao.insertSavingsGoal(goal)
        logAudit("CREATE", "Savings", "Created savings goal ${goal.name}")
    }

    suspend fun updateSavingsGoal(goal: SavingsGoalEntity) = withContext(Dispatchers.IO) {
        require(goal.id > 0 && goal.name.isNotBlank()) { "Savings goal identity and name are required" }
        require(goal.targetAmount.isFinite() && goal.targetAmount > 0.0) { "Savings target must be finite and positive" }
        require(goal.currentAmount.isFinite() && goal.currentAmount >= 0.0 && goal.currentAmount <= goal.targetAmount) { "Current savings must be between zero and target" }
        require(goal.currency.matches(Regex("[A-Z]{3}"))) { "Savings currency must be a three-letter code" }
        require(goal.targetMonthlyDeposit.isFinite() && goal.targetMonthlyDeposit >= 0.0) { "Monthly deposit must be non-negative" }
        dao.updateSavingsGoal(goal)
        logAudit("UPDATE", "Savings", "Updated savings goal ${goal.name}")
    }

    suspend fun contributeToSavings(goalId: Long, amount: Double, accountId: Long): Boolean = withContext(Dispatchers.IO) {
        val operation: suspend () -> Boolean = operation@{
            val goal = dao.getSavingsGoalById(goalId)
                ?: return@operation false
            val account = dao.getAccountById(accountId) ?: return@operation false
            if (!amount.isFinite() || amount <= 0.0 ||
                !goal.currency.equals(account.currency, ignoreCase = true) ||
                !account.balance.isFinite() || account.balance < amount
            ) return@operation false
            val updatedGoalAmount = goal.currentAmount + amount
            if (!updatedGoalAmount.isFinite()) return@operation false

            dao.insertTransaction(
                TransactionEntity(
                    accountId = accountId,
                    type = TransactionType.EXPENSE,
                    amount = amount,
                    currency = goal.currency,
                    category = "Savings",
                    description = "Contribution to goal: ${goal.name}"
                )
            )
            dao.updateAccountBalance(
                accountId,
                FinanceEngine.toBigDecimal(account.balance)
                    .subtract(FinanceEngine.toBigDecimal(amount)).toDouble()
            )
            dao.updateSavingsGoal(goal.copy(currentAmount = updatedGoalAmount))
            logAudit("CONTRIBUTION", "Savings", "Added $amount to goal ${goal.name}")
            true
        }
        if (db != null) db.withTransaction { operation() } else operation()
    }

    suspend fun deleteSavingsGoal(goal: SavingsGoalEntity) = withContext(Dispatchers.IO) {
        dao.deleteSavingsGoal(goal)
        logAudit("DELETE", "Savings", "Deleted savings goal ${goal.name}")
    }

    // Assets
    suspend fun addAsset(asset: AssetEntity) = withContext(Dispatchers.IO) {
        require(asset.name.isNotBlank()) { "Asset name is required" }
        require(asset.value.isFinite() && asset.value >= 0.0) { "Asset value must be finite and non-negative" }
        require(asset.currency.matches(Regex("[A-Z]{3}"))) { "Asset currency must be a three-letter code" }
        dao.insertAsset(asset)
        logAudit("CREATE", "Asset", "Added asset ${asset.name} valued at ${asset.value} ${asset.currency}")
    }

    suspend fun updateAsset(asset: AssetEntity) = withContext(Dispatchers.IO) {
        require(asset.id > 0 && asset.name.isNotBlank()) { "Asset identity and name are required" }
        require(asset.value.isFinite() && asset.value >= 0.0) { "Asset value must be finite and non-negative" }
        require(asset.currency.matches(Regex("[A-Z]{3}"))) { "Asset currency must be a three-letter code" }
        dao.updateAsset(asset)
        logAudit("UPDATE", "Asset", "Updated asset ${asset.name}")
    }

    suspend fun deleteAsset(asset: AssetEntity) = withContext(Dispatchers.IO) {
        dao.deleteAsset(asset)
        logAudit("DELETE", "Asset", "Deleted asset ${asset.name}")
    }

    // Household
    suspend fun addHouseholdMember(name: String, email: String, role: String, percent: Double) = withContext(Dispatchers.IO) {
        require(name.isNotBlank() && role.isNotBlank()) { "Household member name and role are required" }
        require(percent.isFinite() && percent in 0.0..100.0) { "Contribution percentage must be between 0 and 100" }
        dao.insertHouseholdMember(HouseholdMemberEntity(name = name.trim(), email = email.trim(), role = role.trim(), contributionPercent = percent))
        logAudit("CREATE", "Household", "Added member $name")
    }

    suspend fun updateHouseholdMember(member: HouseholdMemberEntity) = withContext(Dispatchers.IO) {
        require(member.id > 0 && member.name.isNotBlank() && member.role.isNotBlank()) { "Household member identity, name and role are required" }
        require(member.contributionPercent.isFinite() && member.contributionPercent in 0.0..100.0) { "Contribution percentage must be between 0 and 100" }
        dao.updateHouseholdMember(member.copy(name = member.name.trim(), role = member.role.trim(), email = member.email.trim()))
        logAudit("UPDATE", "Household", "Updated member " + member.name)
    }

    suspend fun deleteHouseholdMember(member: HouseholdMemberEntity) = withContext(Dispatchers.IO) {
        require(dao.getAllHouseholdExpenses().firstOrNull().orEmpty().none { it.payerMemberId == member.id }) {
            "Cannot delete a household member who has recorded expenses"
        }
        dao.deleteHouseholdMember(member)
        logAudit("DELETE", "Household", "Deleted member " + member.name)
    }

    suspend fun addHouseholdExpense(title: String, amount: Double, payerId: Long, category: String, currency: String = "LYD") = withContext(Dispatchers.IO) {
        require(title.isNotBlank() && category.isNotBlank()) { "Household expense title and category are required" }
        require(amount.isFinite() && amount > 0.0) { "Household expense must be finite and positive" }
        require(currency.matches(Regex("[A-Z]{3}"))) { "Household expense currency must be a three-letter code" }
        require(dao.getHouseholdMemberById(payerId) != null) { "Household payer does not exist" }
        dao.insertHouseholdExpense(HouseholdExpenseEntity(title = title.trim(), amount = amount, payerMemberId = payerId, category = category.trim(), currency = currency))
        logAudit("CREATE", "Household", "Added household expense " + title + " (" + amount + ")")
    }

    suspend fun updateHouseholdExpense(expense: HouseholdExpenseEntity) = withContext(Dispatchers.IO) {
        require(expense.id > 0 && expense.title.isNotBlank() && expense.category.isNotBlank()) { "Household expense identity, title and category are required" }
        require(expense.amount.isFinite() && expense.amount > 0.0) { "Household expense must be finite and positive" }
        require(expense.currency.matches(Regex("[A-Z]{3}"))) { "Household expense currency must be a three-letter code" }
        require(dao.getHouseholdMemberById(expense.payerMemberId) != null) { "Household payer does not exist" }
        dao.updateHouseholdExpense(expense.copy(title = expense.title.trim(), category = expense.category.trim()))
        logAudit("UPDATE", "Household", "Updated household expense " + expense.title)
    }

    suspend fun deleteHouseholdExpense(expense: HouseholdExpenseEntity) = withContext(Dispatchers.IO) {
        dao.deleteHouseholdExpense(expense)
        logAudit("DELETE", "Household", "Deleted household expense " + expense.title)
    }

    // Documents
    suspend fun addDocument(doc: DocumentEntity) = withContext(Dispatchers.IO) {
        dao.insertDocument(doc)
        logAudit("UPLOAD", "Document", "Saved document ${doc.title} - ${doc.merchant}")
    }

    suspend fun deleteDocument(doc: DocumentEntity) = withContext(Dispatchers.IO) {
        dao.deleteDocument(doc)
        logAudit("DELETE", "Document", "Deleted document ${doc.title}")
    }

    // Notifications
    suspend fun addNotification(title: String, message: String, type: NotificationType) = withContext(Dispatchers.IO) {
        dao.insertNotification(NotificationEntity(title = title, message = message, type = type))
    }

    suspend fun markNotificationRead(id: Long) = withContext(Dispatchers.IO) {
        dao.markNotificationAsRead(id)
    }

    suspend fun clearNotifications() = withContext(Dispatchers.IO) {
        dao.clearAllNotifications()
    }

    // User settings
    suspend fun updateCurrentUser(user: UserEntity) = withContext(Dispatchers.IO) {
        dao.insertOrUpdateUser(user)
        logAudit("UPDATE", "User", "Updated profile settings")
    }

    /**
     * Seeds initial production sample data if database is brand new,
     * ensuring immediate rich experience with realistic financial records.
     */
    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        val existingAccounts = dao.getAllAccounts().firstOrNull() ?: emptyList()
        if (existingAccounts.isNotEmpty()) return@withContext

        // 1. Initial User
        dao.insertOrUpdateUser(
            UserEntity(
                name = "عبدالله الصالح (Abdullah)",
                email = "ab.alsaleh0@gmail.com",
                role = UserRole.ADMIN,
                currencyPreference = "LYD",
                languagePreference = AppLanguage.AR
            )
        )

        // 2. Initial Accounts
        val accAlRajhiId = dao.insertAccount(
            AccountEntity(
                name = "مصرف الراجحي - الحساب الجاري",
                type = AccountType.BANK,
                currency = "LYD",
                balance = 28450.00,
                accountNumberSafe = "•••• 4821",
                institution = "Al Rajhi Bank",
                colorHex = "#00D09C"
            )
        )

        val accSnbId = dao.insertAccount(
            AccountEntity(
                name = "البنك الأهلي - حساب الادخار",
                type = AccountType.SAVINGS,
                currency = "LYD",
                balance = 45000.00,
                accountNumberSafe = "•••• 9120",
                institution = "SNB",
                colorHex = "#2196F3"
            )
        )

        val accCashId = dao.insertAccount(
            AccountEntity(
                name = "محفظة الكاش اليومية",
                type = AccountType.CASH,
                currency = "LYD",
                balance = 1250.00,
                accountNumberSafe = "Cash In Hand",
                institution = "Cash",
                colorHex = "#FFB300"
            )
        )

        val accInvestId = dao.insertAccount(
            AccountEntity(
                name = "محفظة الاستثمار والتداول",
                type = AccountType.INVESTMENT,
                currency = "LYD",
                balance = 82300.00,
                accountNumberSafe = "•••• 3301",
                institution = "Derayah / Tadawul",
                colorHex = "#9C27B0"
            )
        )

        val now = System.currentTimeMillis()
        val oneDay = 24L * 60 * 60 * 1000

        // 3. Transactions
        dao.insertTransaction(
            TransactionEntity(
                accountId = accAlRajhiId,
                type = TransactionType.INCOME,
                amount = 18500.00,
                currency = "LYD",
                category = "Salary",
                description = "راتب الشهر - شركة التقنية المتقدمة",
                merchant = "Advanced Tech Corp",
                date = now - (2 * oneDay)
            )
        )

        dao.insertTransaction(
            TransactionEntity(
                accountId = accAlRajhiId,
                type = TransactionType.EXPENSE,
                amount = 1240.50,
                currency = "LYD",
                category = "Groceries",
                description = "مشتريات تموينات المنزل الأسبوعية",
                merchant = "Tamimi Markets",
                date = now - (1 * oneDay)
            )
        )

        dao.insertTransaction(
            TransactionEntity(
                accountId = accCashId,
                type = TransactionType.EXPENSE,
                amount = 85.00,
                currency = "LYD",
                category = "Dining",
                description = "قهوة مختصة وإفطار عمل",
                merchant = "Half Million Cafe",
                date = now - (4 * 60 * 60 * 1000)
            )
        )

        dao.insertTransaction(
            TransactionEntity(
                accountId = accAlRajhiId,
                type = TransactionType.EXPENSE,
                amount = 450.00,
                currency = "LYD",
                category = "Utilities",
                description = "فاتورة الكهرباء الشهرية",
                merchant = "Saudi Electricity Co.",
                date = now - (4 * oneDay)
            )
        )

        dao.insertTransaction(
            TransactionEntity(
                accountId = accInvestId,
                type = TransactionType.INCOME,
                amount = 1350.00,
                currency = "LYD",
                category = "Investments",
                description = "أرباح توزيعات سنوية أسهم أرامكو",
                merchant = "Saudi Aramco Dividends",
                date = now - (7 * oneDay)
            )
        )

        // 4. Budgets
        dao.insertBudget(
            BudgetEntity(
                name = "ميزانية المطاعم والمقاهي",
                category = "Dining",
                period = BudgetPeriod.MONTHLY,
                limitAmount = 1500.00,
                startDate = now - (10 * oneDay),
                endDate = now + (20 * oneDay)
            )
        )

        dao.insertBudget(
            BudgetEntity(
                name = "ميزانية المقاضي والتموينات",
                category = "Groceries",
                period = BudgetPeriod.MONTHLY,
                limitAmount = 3500.00,
                startDate = now - (10 * oneDay),
                endDate = now + (20 * oneDay)
            )
        )

        dao.insertBudget(
            BudgetEntity(
                name = "ميزانية التسوق والمستلزمات",
                category = "Shopping",
                period = BudgetPeriod.MONTHLY,
                limitAmount = 2000.00,
                startDate = now - (10 * oneDay),
                endDate = now + (20 * oneDay)
            )
        )

        // 5. Debts
        dao.insertDebt(
            DebtEntity(
                name = "تمويل شخصي مرابحة",
                creditor = "Al Rajhi Bank",
                principal = 100000.00,
                remaining = 34500.00,
                interestRate = 3.2,
                minimumPayment = 2850.00,
                dueDate = now + (15 * oneDay),
                notes = "خصم شهري تلقائي يوم 27"
            )
        )

        // 6. Bills
        dao.insertBill(
            BillEntity(
                name = "فاتورة ألياف بصرية STC إنترنت",
                category = "Internet",
                amount = 330.00,
                dueDate = now + (5 * oneDay),
                isPaid = false
            )
        )

        dao.insertBill(
            BillEntity(
                name = "إيجار الشقة السكني (دفعة ربعية)",
                category = "Rent",
                amount = 12000.00,
                dueDate = now + (22 * oneDay),
                isPaid = false
            )
        )

        // 7. Savings Goals
        dao.insertSavingsGoal(
            SavingsGoalEntity(
                name = "صندوق الطوارئ العائلي (6 أشهر)",
                targetAmount = 60000.00,
                currentAmount = 45000.00,
                deadline = now + (180 * oneDay),
                targetMonthlyDeposit = 2500.00,
                colorHex = "#00D09C"
            )
        )

        dao.insertSavingsGoal(
            SavingsGoalEntity(
                name = "شراء سيارة دفع رباعي جديدة",
                targetAmount = 140000.00,
                currentAmount = 52000.00,
                deadline = now + (365 * oneDay),
                targetMonthlyDeposit = 5000.00,
                colorHex = "#F1B942"
            )
        )

        // 8. Assets
        dao.insertAsset(
            AssetEntity(
                name = "شقة سكنية استثمارية - الرياض",
                type = AssetType.PROPERTY,
                value = 680000.00,
                purchaseDate = now - (730 * oneDay),
                notes = "مؤجرة بعائد سنوي 8%"
            )
        )

        dao.insertAsset(
            AssetEntity(
                name = "سبائك ذهب سويسري عيار 24 (100 جرام)",
                type = AssetType.GOLD,
                value = 34200.00,
                purchaseDate = now - (300 * oneDay),
                notes = "محفوظة في الخزنة الخاصة"
            )
        )

        dao.insertAsset(
            AssetEntity(
                name = "سيارة تويوتا لاندكروزر 2023",
                type = AssetType.VEHICLE,
                value = 240000.00,
                purchaseDate = now - (400 * oneDay)
            )
        )

        // 9. Household
        val m1 = dao.insertHouseholdMember(HouseholdMemberEntity(name = "عبدالله", role = "رب الأسرة", contributionPercent = 60.0))
        val m2 = dao.insertHouseholdMember(HouseholdMemberEntity(name = "سارة", role = "شريك", contributionPercent = 40.0))

        dao.insertHouseholdExpense(
            HouseholdExpenseEntity(
                title = "أجهزة كهربائية للمطبخ",
                amount = 890.00,
                payerMemberId = m1,
                category = "Home Appliances"
            )
        )

        // 10. Notifications
        dao.insertNotification(
            NotificationEntity(
                title = "تنبيه اقتراب ميزانية التموينات",
                message = "وصل إنفاقك على التموينات إلى 78% من الحد المحدد لهذا الشهر.",
                type = NotificationType.BUDGET_WARNING
            )
        )

        dao.insertNotification(
            NotificationEntity(
                title = "استحقاق فاتورة إنترنت قريباً",
                message = "فاتورة STC للإنترنت بمبلغ 330 ر.س تستحق الدفع خلال 5 أيام.",
                type = NotificationType.BILL_DUE
            )
        )

        // 11. Initial Audit Log
        dao.insertAuditLog(
            AuditLogEntity(
                action = "SYSTEM_INIT",
                resource = "Database",
                details = "BYE PRO FINANCE initialized with verified encryption and production ledger"
            )
        )
    }
}
