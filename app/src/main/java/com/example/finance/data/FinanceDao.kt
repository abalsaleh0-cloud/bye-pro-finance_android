package com.example.finance.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.finance.model.AccountEntity
import com.example.finance.model.AssetEntity
import com.example.finance.model.AuditLogEntity
import com.example.finance.model.BillEntity
import com.example.finance.model.BudgetEntity
import com.example.finance.model.DebtEntity
import com.example.finance.model.DebtPaymentEntity
import com.example.finance.model.DocumentEntity
import com.example.finance.model.HouseholdExpenseEntity
import com.example.finance.model.HouseholdMemberEntity
import com.example.finance.model.NotificationEntity
import com.example.finance.model.SavingsGoalEntity
import com.example.finance.model.TransactionEntity
import com.example.finance.model.TransferEntity
import com.example.finance.model.UserEntity
import com.example.finance.model.SyncEntityMapEntity
import com.example.finance.model.SyncOutboxEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {
    @Query("SELECT * FROM accounts WHERE isArchived = 0 ORDER BY id ASC") fun getAllAccounts(): Flow<List<AccountEntity>>
    @Query("SELECT * FROM accounts WHERE id = :id") suspend fun getAccountById(id: Long): AccountEntity?
    @Query("SELECT * FROM accounts ORDER BY id ASC") suspend fun getAllAccountsSnapshot(): List<AccountEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAccount(account: AccountEntity): Long
    @Update suspend fun updateAccount(account: AccountEntity)
    @Query("UPDATE accounts SET balance = :newBalance WHERE id = :accountId") suspend fun updateAccountBalance(accountId: Long, newBalance: Double)
    @Query("SELECT * FROM accounts WHERE isArchived = 1 ORDER BY id ASC") fun getArchivedAccounts(): Flow<List<AccountEntity>>
    @Query("UPDATE accounts SET isArchived = :isArchived WHERE id = :accountId") suspend fun setAccountArchived(accountId: Long, isArchived: Boolean)
    @Delete suspend fun deleteAccount(account: AccountEntity)
    @Query("DELETE FROM accounts WHERE id = :id") suspend fun deleteAccountById(id: Long)

    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC") fun getAllTransactions(): Flow<List<TransactionEntity>>
    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC") suspend fun getAllTransactionsSnapshot(): List<TransactionEntity>
    @Query("DELETE FROM transactions WHERE id = :id") suspend fun deleteTransactionById(id: Long)
    @Query("SELECT * FROM transactions WHERE id = :id") suspend fun getTransactionById(id: Long): TransactionEntity?
    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY date DESC") fun getTransactionsByAccount(accountId: Long): Flow<List<TransactionEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertTransaction(transaction: TransactionEntity): Long
    @Update suspend fun updateTransaction(transaction: TransactionEntity)
    @Delete suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM transfers ORDER BY date DESC") fun getAllTransfers(): Flow<List<TransferEntity>>
    @Query("SELECT * FROM transfers ORDER BY date DESC, id DESC") suspend fun getAllTransfersSnapshot(): List<TransferEntity>
    @Query("DELETE FROM transfers WHERE id = :id") suspend fun deleteTransferById(id: Long)
    @Query("SELECT * FROM transfers WHERE id = :id") suspend fun getTransferById(id: Long): TransferEntity?
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
    @Query("SELECT * FROM household_members WHERE id = :id LIMIT 1") suspend fun getHouseholdMemberById(id: Long): HouseholdMemberEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertHouseholdMember(member: HouseholdMemberEntity): Long
    @Update suspend fun updateHouseholdMember(member: HouseholdMemberEntity)
    @Delete suspend fun deleteHouseholdMember(member: HouseholdMemberEntity)
    @Query("SELECT * FROM household_expenses ORDER BY date DESC") fun getAllHouseholdExpenses(): Flow<List<HouseholdExpenseEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertHouseholdExpense(expense: HouseholdExpenseEntity): Long
    @Update suspend fun updateHouseholdExpense(expense: HouseholdExpenseEntity)
    @Delete suspend fun deleteHouseholdExpense(expense: HouseholdExpenseEntity)

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

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertSyncEntityMap(mapping: SyncEntityMapEntity)
    @Query("SELECT * FROM sync_entity_map WHERE entityType = :entityType AND localId = :localId LIMIT 1") suspend fun getSyncEntityMap(entityType: String, localId: Long): SyncEntityMapEntity?
    @Query("SELECT * FROM sync_entity_map WHERE entityType = :entityType AND remoteId = :remoteId LIMIT 1") suspend fun getSyncEntityMapByRemoteId(entityType: String, remoteId: String): SyncEntityMapEntity?
    @Query("DELETE FROM sync_entity_map WHERE entityType = :entityType AND localId = :localId") suspend fun deleteSyncEntityMap(entityType: String, localId: Long)

    // Outbox rows are retried after process death. REPLACE keeps enqueue operations
    // idempotent if a caller reuses an explicit row id, while the auto-increment
    // primary key still creates a durable row for normal inserts.
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun enqueueSyncMutation(item: SyncOutboxEntity): Long
    @Query("SELECT * FROM sync_outbox WHERE state = 'PENDING' ORDER BY createdAt ASC, id ASC LIMIT :limit") suspend fun getPendingSyncMutations(limit: Int = 100): List<SyncOutboxEntity>
    @Query("SELECT * FROM sync_outbox WHERE state = 'PENDING' ORDER BY createdAt ASC, id ASC") fun observePendingSyncMutations(): Flow<List<SyncOutboxEntity>>
    @Query("UPDATE sync_outbox SET state = 'SYNCED', lastAttemptAt = :attemptedAt, lastError = NULL WHERE id = :id") suspend fun markSyncMutationComplete(id: Long, attemptedAt: Long = System.currentTimeMillis())
    @Query("UPDATE sync_outbox SET attemptCount = attemptCount + 1, lastAttemptAt = :attemptedAt, lastError = :error WHERE id = :id") suspend fun recordSyncMutationFailure(id: Long, error: String, attemptedAt: Long = System.currentTimeMillis())
    @Query("DELETE FROM sync_outbox WHERE state = 'SYNCED' AND createdAt < :before") suspend fun pruneCompletedSyncMutations(before: Long): Int
    @Query("SELECT COUNT(*) FROM sync_outbox WHERE state = 'PENDING'") suspend fun countPendingSyncMutations(): Int
    @Query("SELECT EXISTS(SELECT 1 FROM sync_outbox WHERE entityType = :entityType AND localId = :localId AND state = 'PENDING')") suspend fun hasPendingSyncMutation(entityType: String, localId: Long): Boolean
}
