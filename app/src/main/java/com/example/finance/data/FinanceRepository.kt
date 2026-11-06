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
import com.example.finance.model.WorkSourceEntity
import com.example.finance.model.WorkSourceType
import com.example.finance.model.WorkRecordEntity
import com.example.finance.model.WorkRecordType
import com.example.finance.engine.WorkLedgerCalculator
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
    val workSources: Flow<List<WorkSourceEntity>> = dao.getWorkSources()
    val workRecords: Flow<List<WorkRecordEntity>> = dao.getWorkRecords()

    suspend fun addWorkSource(source: WorkSourceEntity): Long = withContext(Dispatchers.IO) {
        require(source.name.isNotBlank() && source.currency.isNotBlank())
        require(source.monthlySalary.isFinite() && source.monthlySalary >= 0.0)
        require(source.type != WorkSourceType.FREELANCE || source.monthlySalary == 0.0)
        val id = dao.insertWorkSource(source.copy(name = source.name.trim(), currency = source.currency.trim().uppercase()))
        logAudit("CREATE", "WorkSource", "Created source " + source.name)
        id
    }

    suspend fun updateWorkSource(source: WorkSourceEntity) = withContext(Dispatchers.IO) {
        require(source.id > 0L && source.name.isNotBlank() && source.currency.isNotBlank())
        require(source.monthlySalary.isFinite() && source.monthlySalary >= 0.0)
        require(source.type != WorkSourceType.FREELANCE || source.monthlySalary == 0.0)
        val existing = dao.getWorkSourceById(source.id) ?: throw IllegalArgumentException("Work source not found")
        require(existing.type == source.type) { "A work source cannot change between employment and freelance after records exist" }
        dao.insertWorkSource(source.copy(name = source.name.trim(), currency = source.currency.trim().uppercase()))
        logAudit("UPDATE", "WorkSource", "Updated source " + source.name)
    }

    suspend fun addWorkRecord(record: WorkRecordEntity, transaction: TransactionEntity? = null): Long = withContext(Dispatchers.IO) {
        require(record.amount.isFinite() && record.amount > 0.0)
        require(record.currency.isNotBlank() && record.period.matches(Regex("\\d{4}-\\d{2}")))
        val transfer = record.type == WorkRecordType.WORK_TRANSFER_IN || record.type == WorkRecordType.WORK_TRANSFER_OUT
        val salaryRecord = record.type in setOf(
            WorkRecordType.SALARY_ADVANCE, WorkRecordType.SALARY_DEDUCTION,
            WorkRecordType.SALARY_ALLOWANCE, WorkRecordType.SALARY_BONUS, WorkRecordType.SALARY_OVERTIME,
            WorkRecordType.SALARY_PAYMENT
        )
        val freelanceRecord = record.type in setOf(
            WorkRecordType.FREELANCE_INCOME, WorkRecordType.FREELANCE_PAYMENT
        )
        val source = record.sourceId?.let { dao.getWorkSourceById(it) }
        if (transfer) {
            require(record.sourceId == null && record.relatedRecordId == null)
        } else if (salaryRecord || freelanceRecord) {
            require(source != null && source.isActive) { "Select an active source for salary or freelance records" }
        } else if (source != null) {
            require(source.isActive) { "The selected work source is inactive" }
        }
        if (source != null) {
            require(source.type == record.scope) { "Work record scope must match its source type" }
            require(source.currency.equals(record.currency, true)) { "Record currency must match the selected source currency" }
            if (salaryRecord) require(source.type == WorkSourceType.EMPLOYMENT) { "Salary records require an employment source" }
            if (freelanceRecord || record.type == WorkRecordType.FREELANCE_EXPENSE) require(source.type == WorkSourceType.FREELANCE) { "Freelance records require a freelance source" }
        }
        if (salaryRecord) require(record.scope == WorkSourceType.EMPLOYMENT) { "Salary records belong to employment" }
        if (freelanceRecord || record.type == WorkRecordType.FREELANCE_EXPENSE) require(record.scope == WorkSourceType.FREELANCE) { "Freelance records belong to the freelance ledger" }
        if (record.type == WorkRecordType.FREELANCE_PAYMENT) {
            val parentId = record.relatedRecordId ?: throw IllegalArgumentException("Select the receivable being paid")
            val parent = dao.getWorkRecordById(parentId) ?: throw IllegalArgumentException("Receivable not found")
            require(parent.type == WorkRecordType.FREELANCE_INCOME && parent.sourceId == record.sourceId && parent.scope == record.scope && parent.currency.equals(record.currency, true))
            val alreadyPaid = dao.getWorkRecordsSnapshot()
                .filter { it.type == WorkRecordType.FREELANCE_PAYMENT && it.relatedRecordId == parentId }
                .sumOf { it.amount }
            require(record.amount <= parent.amount - alreadyPaid + 0.000001) { "Payment exceeds outstanding receivable" }
        } else require(record.relatedRecordId == null)

        val cash = WorkLedgerCalculator.isCashMovement(record.type)
        require(transaction == null || cash) { "Accrued records cannot post a personal cash transaction" }
        if (transaction != null) {
            val expectedType = when (record.type) {
                WorkRecordType.WORK_INCOME, WorkRecordType.SALARY_ADVANCE,
                WorkRecordType.SALARY_PAYMENT, WorkRecordType.FREELANCE_PAYMENT -> TransactionType.INCOME
                WorkRecordType.FREELANCE_EXPENSE, WorkRecordType.WORK_EXPENSE -> TransactionType.EXPENSE
                WorkRecordType.WORK_TRANSFER_IN -> TransactionType.TRANSFER
                WorkRecordType.WORK_TRANSFER_OUT -> TransactionType.ADJUSTMENT
                else -> throw IllegalArgumentException("This work record cannot create a cash transaction")
            }
            val expectedCategory = when (expectedType) {
                TransactionType.INCOME -> "WORK_INCOME"
                TransactionType.EXPENSE -> "WORK_EXPENSE"
                else -> "WORK_TRANSFER"
            }
            require(transaction.amount == record.amount && transaction.currency.equals(record.currency, true))
            require(transaction.type == expectedType && transaction.category == expectedCategory) {
                "Work record and personal ledger transaction types must match"
            }
            require(transaction.status == TransactionStatus.COMPLETED) {
                "Work cash movements must be posted as completed transactions"
            }
        }
        val operation: suspend () -> Long = {
            val txId = transaction?.let { addTransaction(it) }
            val linked = record.copy(
                currency = record.currency.trim().uppercase(),
                linkedAccountId = transaction?.accountId,
                linkedTransactionId = txId
            )
            val id = dao.insertWorkRecord(linked)
            logAudit("CREATE", "WorkRecord", "Recorded " + linked.type.name + ": " + linked.amount + " " + linked.currency)
            id
        }
        if (db != null) db.withTransaction { operation() } else operation()
    }

    suspend fun updateWorkRecord(record: WorkRecordEntity, transaction: TransactionEntity? = null) = withContext(Dispatchers.IO) {
        require(record.id > 0L && record.amount.isFinite() && record.amount > 0.0)
        require(record.currency.isNotBlank() && record.period.matches(Regex("\\d{4}-\\d{2}")))
        val existing = dao.getWorkRecordById(record.id) ?: throw IllegalArgumentException("Work record not found")
        val source = record.sourceId?.let { dao.getWorkSourceById(it) }
        require(record.sourceId == null || source != null && (source.isActive || source.id == existing.sourceId)) { "Selected work source is inactive or missing" }
        if (source != null) {
            require(source.type == record.scope) { "Work record scope must match its source type" }
            require(source.currency.equals(record.currency, true)) { "Record currency must match the selected source currency" }
        }
        val salaryRecord = record.type in setOf(WorkRecordType.SALARY_ADVANCE, WorkRecordType.SALARY_DEDUCTION, WorkRecordType.SALARY_ALLOWANCE, WorkRecordType.SALARY_BONUS, WorkRecordType.SALARY_OVERTIME, WorkRecordType.SALARY_PAYMENT)
        val freelanceRecord = record.type in setOf(WorkRecordType.FREELANCE_INCOME, WorkRecordType.FREELANCE_PAYMENT, WorkRecordType.FREELANCE_EXPENSE)
        if (salaryRecord) require(record.scope == WorkSourceType.EMPLOYMENT && source?.type == WorkSourceType.EMPLOYMENT)
        if (record.type in setOf(WorkRecordType.FREELANCE_INCOME, WorkRecordType.FREELANCE_PAYMENT)) require(record.scope == WorkSourceType.FREELANCE && source?.type == WorkSourceType.FREELANCE)
        if (record.type == WorkRecordType.FREELANCE_EXPENSE) require(record.scope == WorkSourceType.FREELANCE) { "Freelance expenses belong to the freelance ledger" }
        if (record.type == WorkRecordType.FREELANCE_PAYMENT) {
            val parent = record.relatedRecordId?.let { dao.getWorkRecordById(it) } ?: throw IllegalArgumentException("Select the receivable being paid")
            require(parent.type == WorkRecordType.FREELANCE_INCOME && parent.sourceId == record.sourceId && parent.scope == record.scope && parent.currency.equals(record.currency, true))
            require(parent.id != record.id)
            val alreadyPaid = dao.getWorkRecordsSnapshot()
                .filter { it.type == WorkRecordType.FREELANCE_PAYMENT && it.relatedRecordId == parent.id && it.id != record.id }
                .sumOf { it.amount }
            require(record.amount <= parent.amount - alreadyPaid + 0.000001) { "Payment exceeds outstanding receivable" }
        } else require(record.relatedRecordId == null)
        if (existing.type == WorkRecordType.FREELANCE_INCOME) {
            val linkedPayments = dao.getWorkRecordsSnapshot().filter {
                it.type == WorkRecordType.FREELANCE_PAYMENT && it.relatedRecordId == existing.id
            }
            if (linkedPayments.isNotEmpty()) {
                require(record.sourceId == existing.sourceId && record.scope == existing.scope && record.currency.equals(existing.currency, true)) {
                    "A receivable with linked payments cannot change source, scope or currency"
                }
                require(linkedPayments.sumOf { it.amount } <= record.amount + 0.000001) {
                    "Receivable amount cannot be lower than payments already received"
                }
            }
        }
        val cash = WorkLedgerCalculator.isCashMovement(record.type)
        require(transaction == null || cash) { "Accrued records cannot post a personal cash transaction" }
        if (transaction != null) {
            val expectedType = when (record.type) {
                WorkRecordType.WORK_INCOME, WorkRecordType.SALARY_ADVANCE, WorkRecordType.SALARY_PAYMENT, WorkRecordType.FREELANCE_PAYMENT -> TransactionType.INCOME
                WorkRecordType.FREELANCE_EXPENSE, WorkRecordType.WORK_EXPENSE -> TransactionType.EXPENSE
                WorkRecordType.WORK_TRANSFER_IN -> TransactionType.TRANSFER
                WorkRecordType.WORK_TRANSFER_OUT -> TransactionType.ADJUSTMENT
                else -> throw IllegalArgumentException("This work record cannot create a cash transaction")
            }
            require(transaction.amount == record.amount && transaction.currency.equals(record.currency, true))
            require(transaction.type == expectedType && transaction.status == TransactionStatus.COMPLETED)
            val account = dao.getAccountById(transaction.accountId) ?: throw IllegalArgumentException("Account not found")
            require(account.currency.equals(record.currency, true)) { "Account currency must match work record currency" }
        }
        val operation: suspend () -> Unit = {
            val oldTx = existing.linkedTransactionId?.let { dao.getTransactionById(it) }
            fun signed(tx: TransactionEntity): BigDecimal {
                val amount = FinanceEngine.toBigDecimal(tx.amount)
                return when (tx.type) {
                    TransactionType.INCOME, TransactionType.REFUND, TransactionType.ADJUSTMENT -> amount
                    TransactionType.EXPENSE, TransactionType.TRANSFER -> amount.negate()
                }
            }
            if (oldTx != null && oldTx.status == TransactionStatus.COMPLETED) {
                val oldAccount = dao.getAccountById(oldTx.accountId)
                if (oldAccount != null) {
                    val reversed = FinanceEngine.toBigDecimal(oldAccount.balance).subtract(signed(oldTx))
                    dao.updateAccountBalance(oldAccount.id, reversed.toDouble())
                }
            }
            var newTxId: Long? = null
            if (transaction != null) {
                val updatedTx = transaction.copy(id = oldTx?.id ?: 0L)
                if (oldTx == null) newTxId = dao.insertTransaction(updatedTx) else {
                    dao.updateTransaction(updatedTx)
                    newTxId = updatedTx.id
                }
                if (updatedTx.status == TransactionStatus.COMPLETED) {
                    val newAccount = dao.getAccountById(updatedTx.accountId) ?: throw IllegalArgumentException("Account not found")
                    val posted = FinanceEngine.toBigDecimal(newAccount.balance).add(signed(updatedTx))
                    dao.updateAccountBalance(newAccount.id, posted.toDouble())
                }
            } else if (oldTx != null) {
                dao.deleteTransaction(oldTx)
            }
            val saved = record.copy(
                currency = record.currency.trim().uppercase(),
                linkedAccountId = transaction?.accountId,
                linkedTransactionId = newTxId
            )
            dao.insertWorkRecord(saved)
            logAudit("UPDATE", "WorkRecord", "Updated work record #${saved.id}: ${saved.type} ${saved.amount} ${saved.currency}")
        }
        if (db != null) db.withTransaction { operation() } else operation()
    }

    suspend fun deleteWorkRecord(recordId: Long) = withContext(Dispatchers.IO) {
        val record = dao.getWorkRecordById(recordId) ?: return@withContext
        if (record.type == WorkRecordType.FREELANCE_INCOME) {
            val hasPayments = dao.getWorkRecordsSnapshot().any {
                it.type == WorkRecordType.FREELANCE_PAYMENT && it.relatedRecordId == record.id
            }
            require(!hasPayments) { "Remove or reassign linked partial payments before deleting this receivable" }
        }
        val operation: suspend () -> Unit = {
            val linkedTransactionId = record.linkedTransactionId
            if (linkedTransactionId != null) {
                val tx = dao.getTransactionById(linkedTransactionId)
                if (tx != null) {
                    val account = dao.getAccountById(tx.accountId)
                    if (account != null && tx.status == TransactionStatus.COMPLETED) {
                        val current = FinanceEngine.toBigDecimal(account.balance)
                        val amount = FinanceEngine.toBigDecimal(tx.amount)
                        val reversed = when (tx.type) {
                            TransactionType.INCOME, TransactionType.REFUND, TransactionType.ADJUSTMENT -> current.subtract(amount)
                            TransactionType.EXPENSE, TransactionType.TRANSFER -> current.add(amount)
                        }
                        dao.updateAccountBalance(account.id, reversed.toDouble())
                    }
                    dao.deleteTransaction(tx)
                }
            }
            dao.deleteWorkRecord(record)
            logAudit("DELETE", "WorkRecord", "Deleted work record #${record.id}: ${record.type} ${record.amount} ${record.currency}")
        }
        if (db != null) db.withTransaction { operation() } else operation()
    }

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
        val transactions = dao.getTransactionsByAccount(account.id).firstOrNull().orEmpty()
        val transfers = dao.getAllTransfers().firstOrNull().orEmpty()
        val workRecords = dao.getWorkRecords().firstOrNull().orEmpty()
        require(transactions.isEmpty()) { "Account has transaction history; archive it to preserve the ledger" }
        require(transfers.none { it.fromAccountId == account.id || it.toAccountId == account.id }) {
            "Account is referenced by a transfer; archive it to preserve the ledger"
        }
        require(workRecords.none { it.linkedAccountId == account.id }) {
            "Account is linked to work records; archive it to preserve the ledger"
        }
        dao.deleteAccount(account)
        logAudit("DELETE", "Account", "Deleted account ${account.name}")
    }

    suspend fun addTransaction(tx: TransactionEntity): Long = withContext(Dispatchers.IO) {
        val operation: suspend () -> Long = {
            val account = if (tx.accountId > 0L) dao.getAccountById(tx.accountId)
                ?: throw IllegalArgumentException("Account not found: ${tx.accountId}") else null
            require(tx.amount.isFinite() && tx.amount > 0.0) { "Transaction amount must be a finite positive number" }
            if (account != null) require(tx.currency.equals(account.currency, ignoreCase = true)) {
                "Transaction currency must match the selected account currency"
            }
            val txId = dao.insertTransaction(tx.copy(currency = account?.currency ?: tx.currency.trim().uppercase()))
            if (tx.status == TransactionStatus.COMPLETED && account != null) {
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

    private fun TransactionEntity.isTransferLedgerEntry(): Boolean =
        category.equals("Transfer", ignoreCase = true) ||
            category.equals("Transfer Fee", ignoreCase = true) ||
            category.equals("WORK_INCOME", ignoreCase = true) ||
            category.equals("WORK_EXPENSE", ignoreCase = true) ||
            category.equals("WORK_TRANSFER", ignoreCase = true)

    suspend fun updateTransaction(tx: TransactionEntity) = withContext(Dispatchers.IO) {
        require(tx.amount.isFinite() && tx.amount > 0.0) { "Transaction amount must be a finite positive number" }
        require(!tx.isTransferLedgerEntry()) {
            "Transfer and work ledger entries are managed by their source records"
        }
        val operation: suspend () -> Unit = {
            val existing = dao.getTransactionById(tx.id)
                ?: throw IllegalArgumentException("Transaction not found: ${tx.id}")
            require(!existing.isTransferLedgerEntry()) {
                "Transfer and work ledger entries are managed by their source records"
            }
            val destination = if (tx.accountId > 0L) dao.getAccountById(tx.accountId)
                ?: throw IllegalArgumentException("Account not found: ${tx.accountId}") else null
            if (destination != null) require(tx.currency.equals(destination.currency, ignoreCase = true)) {
                "Transaction currency must match the selected account currency"
            }
            // Reverse the old posting first, then post the edited row. Each balance
            // is read again so moving a transaction between accounts is also safe.
            fun delta(row: TransactionEntity): java.math.BigDecimal {
                val amount = FinanceEngine.toBigDecimal(row.amount)
                return when (row.type) {
                    TransactionType.INCOME, TransactionType.REFUND, TransactionType.ADJUSTMENT -> amount
                    TransactionType.EXPENSE, TransactionType.TRANSFER -> amount.negate()
                }
            }
            if (existing.status == TransactionStatus.COMPLETED && existing.accountId > 0L) {
                val oldAccount = dao.getAccountById(existing.accountId)
                    ?: throw IllegalStateException("Original transaction account is missing")
                val reversed = FinanceEngine.toBigDecimal(oldAccount.balance).subtract(delta(existing))
                dao.updateAccountBalance(oldAccount.id, reversed.toDouble())
            }
            val updated = tx.copy(id = existing.id, status = existing.status, currency = destination?.currency ?: tx.currency.trim().uppercase())
            if (updated.status == TransactionStatus.COMPLETED && destination != null) {
                val refreshed = dao.getAccountById(destination.id)
                    ?: throw IllegalStateException("Selected account is missing")
                val posted = FinanceEngine.toBigDecimal(refreshed.balance).add(delta(updated))
                dao.updateAccountBalance(destination.id, posted.toDouble())
            }
            dao.updateTransaction(updated)
            logAudit("UPDATE", "Transaction", "Updated transaction #${updated.id}: ${updated.description}")
        }
        if (db != null) db.withTransaction { operation() } else operation()
    }

    suspend fun updateTransactionStatus(tx: TransactionEntity, newStatus: TransactionStatus) = withContext(Dispatchers.IO) {
        val operation: suspend () -> Unit = {
            val existing = dao.getTransactionById(tx.id)
                ?: throw IllegalArgumentException("Transaction not found: ${tx.id}")
            require(!existing.isTransferLedgerEntry()) {
                "Transfer ledger entries are managed by the original transfer and cannot be changed independently"
            }
            if (existing.status != newStatus) {
                val crossesPostedBoundary = (existing.status == TransactionStatus.COMPLETED) != (newStatus == TransactionStatus.COMPLETED)
                if (crossesPostedBoundary) {
                    if (existing.accountId > 0L) {
                        val account = dao.getAccountById(existing.accountId)
                            ?: throw IllegalStateException("Transaction account is missing")
                        val current = FinanceEngine.toBigDecimal(account.balance)
                        val amount = FinanceEngine.toBigDecimal(existing.amount)
                        val signedDelta = when (existing.type) {
                            TransactionType.INCOME, TransactionType.REFUND, TransactionType.ADJUSTMENT -> amount
                            TransactionType.EXPENSE, TransactionType.TRANSFER -> amount.negate()
                        }
                        val updatedBalance = if (newStatus == TransactionStatus.COMPLETED) current.add(signedDelta) else current.subtract(signedDelta)
                        dao.updateAccountBalance(account.id, updatedBalance.toDouble())
                    }
                }
                dao.updateTransaction(existing.copy(status = newStatus))
                logAudit("STATUS", "Transaction", "Transaction #${existing.id} status: ${existing.status} -> $newStatus")
            }
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
                val account = if (tx.accountId > 0L) dao.getAccountById(tx.accountId)
                    ?: throw IllegalArgumentException("Imported transaction references a missing account: ${tx.accountId}") else null
                if (account != null) require(tx.currency.equals(account.currency, ignoreCase = true)) {
                    "Imported transaction currency does not match account ${account.id}"
                }
                dao.insertTransaction(tx.copy(currency = account?.currency ?: tx.currency.trim().uppercase()))
                if (tx.status == TransactionStatus.COMPLETED && account != null) {
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
            val existing = dao.getTransactionById(tx.id)
                ?: throw IllegalArgumentException("Transaction not found: ${tx.id}")
            require(!existing.isTransferLedgerEntry()) {
                "Transfer ledger entries are managed by their source records and cannot be deleted independently"
            }
            if (existing.status == TransactionStatus.COMPLETED && existing.accountId > 0L) {
                val account = dao.getAccountById(existing.accountId)
                    ?: throw IllegalStateException("Transaction account is missing; refusing to delete an unbalanced ledger row")
                val current = FinanceEngine.toBigDecimal(account.balance)
                val amount = FinanceEngine.toBigDecimal(existing.amount)
                val reversed = when (existing.type) {
                    TransactionType.INCOME, TransactionType.REFUND, TransactionType.ADJUSTMENT -> current.subtract(amount)
                    TransactionType.EXPENSE, TransactionType.TRANSFER -> current.add(amount)
                }
                dao.updateAccountBalance(account.id, reversed.toDouble())
            }
            dao.deleteTransaction(existing)
            logAudit("DELETE", "Transaction", "Deleted transaction #${existing.id}: ${existing.description}")
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
        accountId: Long?,
        amount: Double,
        notes: String,
        paymentDate: Long = System.currentTimeMillis()
    ): Boolean = withContext(Dispatchers.IO) {
        val operation: suspend () -> Boolean = operation@{
            val debt = dao.getDebtById(debtId) ?: return@operation false
            if (debt.status == DebtStatus.PAID || !amount.isFinite() || amount <= 0.0 ||
                !debt.remaining.isFinite() || amount > debt.remaining || paymentDate <= 0L
            ) return@operation false
            // A debt payment is a valid ledger event even when no personal account is linked.
            // Only an explicitly selected account receives a balance/transaction posting.
            val account = accountId?.let { dao.getAccountById(it) ?: return@operation false }
            if (account != null && (!account.currency.equals(debt.currency, ignoreCase = true) || !account.balance.isFinite())) return@operation false
            val outgoing = debt.direction == "i_owe"
            val amountDecimal = FinanceEngine.toBigDecimal(amount)
            val remaining = FinanceEngine.toBigDecimal(debt.remaining).subtract(amountDecimal)
            val newStatus = if (remaining <= BigDecimal.ZERO) DebtStatus.PAID else DebtStatus.ACTIVE
            if (account != null) {
                val balance = FinanceEngine.toBigDecimal(account.balance).let {
                    if (outgoing) it.subtract(amountDecimal) else it.add(amountDecimal)
                }
                dao.insertTransaction(
                    TransactionEntity(
                        accountId = account.id,
                        type = if (outgoing) TransactionType.EXPENSE else TransactionType.INCOME,
                        amount = amount,
                        currency = debt.currency,
                        category = if (outgoing) "Debt payment" else "Debt collection",
                        date = paymentDate,
                        description = (if (outgoing) "Debt payment: " else "Debt collection: ") + debt.name,
                        notes = notes.trim()
                    )
                )
                dao.updateAccountBalance(account.id, balance.toDouble())
            }
            dao.updateDebt(debt.copy(remaining = remaining.toDouble(), status = newStatus))
            dao.insertDebtPayment(DebtPaymentEntity(debtId = debtId, amount = amount, date = paymentDate, notes = notes.trim(), currency = debt.currency))
            logAudit("PAYMENT", "Debt", (if (outgoing) "Paid " else "Collected ") + amount + " for " + debt.name + (if (account == null) " (unlinked)" else " in account " + account.name) + "; remaining " + remaining)
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

    suspend fun markBillPaid(billId: Long, accountId: Long?): Boolean = withContext(Dispatchers.IO) {
        val operation: suspend () -> Boolean = operation@{
            val bill = dao.getBillById(billId) ?: return@operation false
            if (bill.isPaid) return@operation true
            if (!bill.amount.isFinite() || bill.amount <= 0.0) return@operation false
            val account = accountId?.let { dao.getAccountById(it) ?: return@operation false }
            if (account != null && (account.isArchived ||
                !bill.currency.equals(account.currency, ignoreCase = true) ||
                !account.balance.isFinite())
            ) return@operation false

            if (account != null) {
                dao.insertTransaction(
                    TransactionEntity(
                        accountId = account.id,
                        type = TransactionType.EXPENSE,
                        amount = bill.amount,
                        currency = bill.currency,
                        category = bill.category,
                        description = "Bill payment: ${bill.name}"
                    )
                )
                dao.updateAccountBalance(
                    account.id,
                    FinanceEngine.toBigDecimal(account.balance)
                        .subtract(FinanceEngine.toBigDecimal(bill.amount)).toDouble()
                )
            }
            dao.updateBill(bill.copy(isPaid = true))
            logAudit("PAYMENT", "Bill", "Marked bill ${bill.name} as paid" + if (account == null) " without account linkage" else " from ${account.name}")
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
            if (account.isArchived || !amount.isFinite() || amount <= 0.0 ||
                !goal.currency.equals(account.currency, ignoreCase = true) ||
                !account.balance.isFinite() || account.balance < amount
            ) return@operation false
            val updatedGoalAmount = FinanceEngine.toBigDecimal(goal.currentAmount)
                .add(FinanceEngine.toBigDecimal(amount))
            if (updatedGoalAmount > FinanceEngine.toBigDecimal(goal.targetAmount)) return@operation false

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
            dao.updateSavingsGoal(goal.copy(currentAmount = updatedGoalAmount.toDouble()))
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
        require(doc.title.isNotBlank()) { "Document title is required" }
        require(doc.totalAmount.isFinite() && doc.totalAmount >= 0.0) { "Document amount must be finite and non-negative" }
        require(doc.taxAmount.isFinite() && doc.taxAmount >= 0.0) { "Document tax must be finite and non-negative" }
        require(doc.currency.matches(Regex("[A-Z]{3}"))) { "Document currency must be a three-letter code" }
        dao.insertDocument(doc.copy(title = doc.title.trim(), merchant = doc.merchant.trim()))
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

    suspend fun markAllNotificationsRead() = withContext(Dispatchers.IO) {
        dao.markAllNotificationsRead()
    }

    suspend fun deleteNotification(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteNotification(id)
    }


    suspend fun resetAllAccountBalances() = withContext(Dispatchers.IO) {
        if (db != null) db.withTransaction { dao.resetAllAccountBalances() } else dao.resetAllAccountBalances()
        logAudit("RESET", "Accounts", "Reset all account balances to zero")
    }
    suspend fun clearAllFinancialData() = withContext(Dispatchers.IO) {
        val clear: suspend () -> Unit = {
            dao.clearDebtPayments(); dao.clearDebts()
            dao.clearWorkRecords(); dao.clearWorkSources()
            dao.clearHouseholdExpenses(); dao.clearHouseholdMembers()
            dao.clearTransactions(); dao.clearTransfers()
            dao.clearBudgets(); dao.clearBills()
            dao.clearSavingsGoals(); dao.clearAssets()
            dao.clearDocuments(); dao.clearAccounts()
            dao.clearAllNotifications(); dao.clearAuditLogs()
        }
        if (db != null) db.withTransaction { clear() } else clear()
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
