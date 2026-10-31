package com.example.finance.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val email: String,
    val role: UserRole = UserRole.ADMIN,
    val currencyPreference: String = "LYD",
    val languagePreference: AppLanguage = AppLanguage.AR,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: AccountType,
    val currency: String = "LYD",
    val balance: Double = 0.0,
    val openingBalance: Double = balance,
    val accountNumberSafe: String = "",
    val institution: String = "",
    val isArchived: Boolean = false,
    val notes: String = "",
    val colorHex: String = "#00D09C",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val type: TransactionType,
    val amount: Double,
    val currency: String = "LYD",
    val category: String,
    val date: Long = System.currentTimeMillis(),
    val description: String,
    val merchant: String = "",
    val tags: String = "",
    val notes: String = "",
    val attachmentUri: String? = null,
    val isRecurring: Boolean = false,
    val status: TransactionStatus = TransactionStatus.COMPLETED,
    val targetAccountId: Long? = null
)

@Entity(tableName = "transfers")
data class TransferEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fromAccountId: Long,
    val toAccountId: Long,
    val amount: Double,
    val fee: Double = 0.0,
    val currency: String = "LYD",
    val date: Long = System.currentTimeMillis(),
    val description: String = "",
    val reference: String = ""
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val period: BudgetPeriod = BudgetPeriod.MONTHLY,
    val limitAmount: Double,
    val currency: String = "LYD",
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)
)

@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val creditor: String,
    val principal: Double,
    val remaining: Double,
    val interestRate: Double = 0.0,
    val dueDate: Long = System.currentTimeMillis() + (90L * 24 * 60 * 60 * 1000),
    val minimumPayment: Double = 0.0,
    val currency: String = "LYD",
    val status: DebtStatus = DebtStatus.ACTIVE,
    val notes: String = "",
    val direction: String = "i_owe"
)

@Entity(tableName = "debt_payments")
data class DebtPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val debtId: Long,
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(tableName = "bills")
data class BillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val amount: Double,
    val currency: String = "LYD",
    val dueDate: Long,
    val isRecurring: Boolean = true,
    val recurrenceIntervalDays: Int = 30,
    val isPaid: Boolean = false,
    val reminderDaysBefore: Int = 3
)

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val currency: String = "LYD",
    val deadline: Long,
    val targetMonthlyDeposit: Double = 0.0,
    val colorHex: String = "#F1B942"
)

@Entity(tableName = "assets")
data class AssetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: AssetType,
    val value: Double,
    val currency: String = "LYD",
    val purchaseDate: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(tableName = "household_members")
data class HouseholdMemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val role: String = "Member",
    val email: String = "",
    val contributionPercent: Double = 50.0
)

@Entity(tableName = "household_expenses")
data class HouseholdExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val payerMemberId: Long,
    val category: String,
    @ColumnInfo(defaultValue = "'LYD'") val currency: String = "LYD",
    val date: Long = System.currentTimeMillis(),
    val isSettled: Boolean = false
)

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val type: DocumentType,
    val fileUri: String = "",
    val merchant: String = "",
    val totalAmount: Double = 0.0,
    val taxAmount: Double = 0.0,
    @ColumnInfo(defaultValue = "'LYD'") val currency: String = "LYD",
    val date: Long = System.currentTimeMillis(),
    val notes: String = "",
    val linkedTransactionId: Long? = null
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val message: String,
    val type: NotificationType,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val action: String,
    val resource: String,
    val details: String,
    val userRole: String = "ADMIN",
    val timestamp: Long = System.currentTimeMillis()
)
