package com.example.finance.data

import androidx.room.*
import com.example.finance.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {
    @Query("SELECT * FROM accounts WHERE isArchived = 0 ORDER BY id ASC") fun getAllAccounts(): Flow<List<AccountEntity>>
    @Query("SELECT * FROM accounts WHERE id = :id") suspend fun getAccountById(id: Long): AccountEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAccount(account: AccountEntity): Long
    @Update suspend fun updateAccount(account: AccountEntity)
    @Query("UPDATE accounts SET balance = :newBalance WHERE id = :accountId") suspend fun updateAccountBalance(accountId: Long, newBalance: Double)
    @Query("SELECT * FROM accounts WHERE isArchived = 1 ORDER BY id ASC") fun getArchivedAccounts(): Flow<List<AccountEntity>>
    @Query("UPDATE accounts SET isArchived = :isArchived WHERE id = :accountId") suspend fun setAccountArchived(accountId: Long, isArchived: Boolean)
    @Delete suspend fun deleteAccount(account: AccountEntity)
    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC") fun getAllTransactions(): Flow<List<TransactionEntity>>
    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC") suspend fun getAllTransactionsSnapshot(): List<TransactionEntity>
    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY date DESC") fun getTransactionsByAccount(accountId: Long): Flow<List<TransactionEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertTransaction(transaction: TransactionEntity): Long
    @Update suspend fun updateTransaction(transaction: TransactionEntity)
    @Delete suspend fun deleteTransaction(transaction: TransactionEntity)
    @Query("SELECT * FROM transfers ORDER BY date DESC") fun getAllTransfers(): Flow<List<TransferEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertTransfer(transfer: TransferEntity): Long
    @Query("SELECT * FROM budgets ORDER BY id DESC") fun getAllBudgets(): Flow<List<BudgetEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertBudget(budget: BudgetEntity): Long
    @Update suspend fun updateBudget(budget: BudgetEntity)
    @Delete suspend fun deleteBudget(budget: BudgetEntity)
    @Query("SELECT * FROM debts ORDER BY dueDate ASC") fun getAllDebts(): Flow<List<DebtEntity>>
    @Query("SELECT * FROM debts WHERE id = :id LIMIT 1") suspend fun getDebtById(id: Long): DebtEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertDebt(debt: DebtEntity): Long
    @Update suspend fun updateDebt(debt: DebtEntity)
    @Delete suspend fun deleteDebt(debt: DebtEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertDebtPayment(payment: DebtPaymentEntity): Long
    @Query("SELECT * FROM debt_payments WHERE debtId = :debtId ORDER BY date DESC, id DESC") fun getDebtPayments(debtId: Long): Flow<List<DebtPaymentEntity>>
    @Query("SELECT * FROM bills ORDER BY dueDate ASC") fun getAllBills(): Flow<List<BillEntity>>
    @Query("SELECT * FROM bills WHERE id = :id LIMIT 1") suspend fun getBillById(id: Long): BillEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertBill(bill: BillEntity): Long
    @Update suspend fun updateBill(bill: BillEntity)
    @Delete suspend fun deleteBill(bill: BillEntity)
    @Query("SELECT * FROM savings_goals ORDER BY deadline ASC") fun getAllSavingsGoals(): Flow<List<SavingsGoalEntity>>
    @Query("SELECT * FROM savings_goals WHERE id = :id LIMIT 1") suspend fun getSavingsGoalById(id: Long): SavingsGoalEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertSavingsGoal(goal: SavingsGoalEntity): Long
    @Update suspend fun updateSavingsGoal(goal: SavingsGoalEntity)
    @Delete suspend fun deleteSavingsGoal(goal: SavingsGoalEntity)
    @Query("SELECT * FROM assets ORDER BY value DESC") fun getAllAssets(): Flow<List<AssetEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAsset(asset: AssetEntity): Long
    @Update suspend fun updateAsset(asset: AssetEntity)
    @Delete suspend fun deleteAsset(asset: AssetEntity)

    @Query("SELECT * FROM household_members ORDER BY id ASC") fun getAllHouseholdMembers(): Flow<List<HouseholdMemberEntity>>
    @Query("SELECT * FROM household_members") suspend fun getAllHouseholdMembersSnapshot(): List<HouseholdMemberEntity>
    @Query("SELECT * FROM household_members WHERE id = :id LIMIT 1") suspend fun getHouseholdMemberById(id: Long): HouseholdMemberEntity?
    @Query("SELECT * FROM household_members WHERE remoteId = :remoteId LIMIT 1") suspend fun getHouseholdMemberByRemoteId(remoteId: String): HouseholdMemberEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertHouseholdMember(member: HouseholdMemberEntity): Long
    @Update suspend fun updateHouseholdMember(member: HouseholdMemberEntity)
    @Delete suspend fun deleteHouseholdMember(member: HouseholdMemberEntity)
    @Query("UPDATE household_members SET remoteId = :remoteId WHERE id = :id") suspend fun setHouseholdMemberRemoteId(id: Long, remoteId: String)

    @Query("UPDATE household_expenses SET payerMemberId = NULL WHERE payerMemberId = :memberId") suspend fun clearHouseholdPayer(memberId: Long)
    @Query("SELECT * FROM household_expenses ORDER BY date DESC") fun getAllHouseholdExpenses(): Flow<List<HouseholdExpenseEntity>>
    @Query("SELECT * FROM household_expenses") suspend fun getAllHouseholdExpensesSnapshot(): List<HouseholdExpenseEntity>
    @Query("SELECT * FROM household_expenses WHERE remoteId = :remoteId LIMIT 1") suspend fun getHouseholdExpenseByRemoteId(remoteId: String): HouseholdExpenseEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertHouseholdExpense(expense: HouseholdExpenseEntity): Long
    @Update suspend fun updateHouseholdExpense(expense: HouseholdExpenseEntity)
    @Delete suspend fun deleteHouseholdExpense(expense: HouseholdExpenseEntity)
    @Query("UPDATE household_expenses SET remoteId = :remoteId WHERE id = :id") suspend fun setHouseholdExpenseRemoteId(id: Long, remoteId: String)

    @Query("SELECT * FROM household_budgets WHERE isActive = 1 ORDER BY startDate DESC") fun getAllHouseholdBudgets(): Flow<List<HouseholdBudgetEntity>>
    @Query("SELECT * FROM household_budgets") suspend fun getAllHouseholdBudgetsSnapshot(): List<HouseholdBudgetEntity>
    @Query("SELECT * FROM household_budgets WHERE remoteId = :remoteId LIMIT 1") suspend fun getHouseholdBudgetByRemoteId(remoteId: String): HouseholdBudgetEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertHouseholdBudget(budget: HouseholdBudgetEntity): Long
    @Update suspend fun updateHouseholdBudget(budget: HouseholdBudgetEntity)
    @Delete suspend fun deleteHouseholdBudget(budget: HouseholdBudgetEntity)
    @Query("UPDATE household_budgets SET remoteId = :remoteId WHERE id = :id") suspend fun setHouseholdBudgetRemoteId(id: Long, remoteId: String)

    @Query("SELECT * FROM documents ORDER BY date DESC") fun getAllDocuments(): Flow<List<DocumentEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertDocument(document: DocumentEntity): Long
    @Delete suspend fun deleteDocument(document: DocumentEntity)
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC") fun getAllNotifications(): Flow<List<NotificationEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertNotification(notification: NotificationEntity): Long
    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id") suspend fun markNotificationAsRead(id: Long)
    @Query("UPDATE notifications SET isRead = 1") suspend fun markAllNotificationsRead()
    @Query("DELETE FROM notifications WHERE id = :id") suspend fun deleteNotification(id: Long)
    @Query("DELETE FROM notifications") suspend fun clearAllNotifications()
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 200") fun getAllAuditLogs(): Flow<List<AuditLogEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAuditLog(log: AuditLogEntity): Long
    @Query("SELECT * FROM users LIMIT 1") fun getCurrentUser(): Flow<UserEntity?>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertOrUpdateUser(user: UserEntity): Long
}
