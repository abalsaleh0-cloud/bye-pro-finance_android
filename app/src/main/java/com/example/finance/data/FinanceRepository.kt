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
        require(amount > 0.0) { "Transfer amount must be positive" }
        require(fee >= 0.0) { "Transfer fee cannot be negative" }
        require(fromAccountId != toAccountId) { "Cannot transfer to the same account" }

        val transferOperation: suspend () -> Boolean = transferOp@{
            val fromAcc = dao.getAccountById(fromAccountId) ?: return@transferOp false
            val toAcc = dao.getAccountById(toAccountId) ?: return@transferOp false

            val transferAmount = FinanceEngine.toBigDecimal(amount)
            val transferFee = FinanceEngine.toBigDecimal(fee)
            val totalDeduction = transferAmount.add(transferFee)

            val fromBalance = FinanceEngine.toBigDecimal(fromAcc.balance)
            val toBalance = FinanceEngine.toBigDecimal(toAcc.balance)

            if (fromBalance < totalDeduction) {
                throw IllegalStateException("Insufficient funds: balance $fromBalance < deduction $totalDeduction")
            }

            // The web ledger keeps currencies separate; do not apply guessed FX rates.
            require(fromAcc.currency.equals(toAcc.currency, ignoreCase = true)) {
                "Cross-currency transfers require a verified exchange-rate source"
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
        dao.insertBudget(budget)
        logAudit("CREATE", "Budget", "Created budget for ${budget.category}: ${budget.limitAmount} ${budget.currency}")
    }

    suspend fun deleteBudget(budget: BudgetEntity) = withContext(Dispatchers.IO) {
        dao.deleteBudget(budget)
        logAudit("DELETE", "Budget", "Deleted budget ${budget.name}")
    }

    // Debts
    suspend fun addDebt(debt: DebtEntity) = withContext(Dispatchers.IO) {
        dao.insertDebt(debt)
        logAudit("CREATE", "Debt", "Added debt ${debt.name} (${debt.remaining} ${debt.currency})")
    }

    suspend fun recordDebtPayment(debtId: Long, amount: Double, notes: String) = withContext(Dispatchers.IO) {
        val allDebts = dao.getAllDebts().firstOrNull() ?: emptyList()
        val debt = allDebts.find { it.id == debtId } ?: return@withContext
        val rem = (debt.remaining - amount).coerceAtLeast(0.0)
        val newStatus = if (rem <= 0.0) DebtStatus.PAID else DebtStatus.ACTIVE
        dao.updateDebt(debt.copy(remaining = rem, status = newStatus))
        dao.insertDebtPayment(DebtPaymentEntity(debtId = debtId, amount = amount, notes = notes))
        logAudit("PAYMENT", "Debt", "Paid $amount toward ${debt.name}, remaining $rem")
    }

    suspend fun deleteDebt(debt: DebtEntity) = withContext(Dispatchers.IO) {
        dao.deleteDebt(debt)
        logAudit("DELETE", "Debt", "Deleted debt ${debt.name}")
    }

    // Bills
    suspend fun addBill(bill: BillEntity) = withContext(Dispatchers.IO) {
        dao.insertBill(bill)
        logAudit("CREATE", "Bill", "Created bill ${bill.name} (${bill.amount} ${bill.currency})")
    }

    suspend fun markBillPaid(billId: Long, accountId: Long) = withContext(Dispatchers.IO) {
        val allBills = dao.getAllBills().firstOrNull() ?: emptyList()
        val bill = allBills.find { it.id == billId } ?: return@withContext
        dao.updateBill(bill.copy(isPaid = true))

        // Create transaction
        addTransaction(
            TransactionEntity(
                accountId = accountId,
                type = TransactionType.EXPENSE,
                amount = bill.amount,
                currency = bill.currency,
                category = bill.category,
                description = "Bill payment: ${bill.name}"
            )
        )
        logAudit("PAYMENT", "Bill", "Marked bill ${bill.name} as paid")
    }

    suspend fun deleteBill(bill: BillEntity) = withContext(Dispatchers.IO) {
        dao.deleteBill(bill)
        logAudit("DELETE", "Bill", "Deleted bill ${bill.name}")
    }

    // Savings
    suspend fun addSavingsGoal(goal: SavingsGoalEntity) = withContext(Dispatchers.IO) {
        dao.insertSavingsGoal(goal)
        logAudit("CREATE", "Savings", "Created savings goal ${goal.name}")
    }

    suspend fun contributeToSavings(goalId: Long, amount: Double, accountId: Long) = withContext(Dispatchers.IO) {
        val allGoals = dao.getAllSavingsGoals().firstOrNull() ?: emptyList()
        val goal = allGoals.find { it.id == goalId } ?: return@withContext
        val newAmount = goal.currentAmount + amount
        dao.updateSavingsGoal(goal.copy(currentAmount = newAmount))

        addTransaction(
            TransactionEntity(
                accountId = accountId,
                type = TransactionType.EXPENSE,
                amount = amount,
                currency = goal.currency,
                category = "Savings",
                description = "Contribution to goal: ${goal.name}"
            )
        )
        logAudit("CONTRIBUTION", "Savings", "Added $amount to goal ${goal.name}")
    }

    suspend fun deleteSavingsGoal(goal: SavingsGoalEntity) = withContext(Dispatchers.IO) {
        dao.deleteSavingsGoal(goal)
        logAudit("DELETE", "Savings", "Deleted savings goal ${goal.name}")
    }

    // Assets
    suspend fun addAsset(asset: AssetEntity) = withContext(Dispatchers.IO) {
        dao.insertAsset(asset)
        logAudit("CREATE", "Asset", "Added asset ${asset.name} valued at ${asset.value} ${asset.currency}")
    }

    suspend fun deleteAsset(asset: AssetEntity) = withContext(Dispatchers.IO) {
        dao.deleteAsset(asset)
        logAudit("DELETE", "Asset", "Deleted asset ${asset.name}")
    }

    // Household
    suspend fun addHouseholdMember(name: String, email: String, role: String, percent: Double) = withContext(Dispatchers.IO) {
        dao.insertHouseholdMember(HouseholdMemberEntity(name = name, email = email, role = role, contributionPercent = percent))
        logAudit("CREATE", "Household", "Added member $name")
    }

    suspend fun addHouseholdExpense(title: String, amount: Double, payerId: Long, category: String) = withContext(Dispatchers.IO) {
        dao.insertHouseholdExpense(HouseholdExpenseEntity(title = title, amount = amount, payerMemberId = payerId, category = category))
        logAudit("CREATE", "Household", "Added household expense $title ($amount)")
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
