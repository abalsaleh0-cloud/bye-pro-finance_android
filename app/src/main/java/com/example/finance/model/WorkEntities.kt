package com.example.finance.model
import androidx.room.Entity
import androidx.room.PrimaryKey
enum class WorkSourceType { EMPLOYMENT, FREELANCE }
enum class WorkRecordType { SALARY_ADVANCE, SALARY_DEDUCTION, SALARY_BONUS, SALARY_OVERTIME, SALARY_PAYMENT, FREELANCE_INCOME, FREELANCE_PAYMENT, FREELANCE_EXPENSE, WORK_EXPENSE, WORK_TRANSFER_IN, WORK_TRANSFER_OUT }
@Entity(tableName = "work_sources")
data class WorkSourceEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val type: WorkSourceType, val monthlySalary: Double = 0.0, val currency: String = "LYD", val isActive: Boolean = true, val createdAt: Long = System.currentTimeMillis())
@Entity(tableName = "work_records")
data class WorkRecordEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val sourceId: Long? = null, val relatedRecordId: Long? = null, val type: WorkRecordType, val amount: Double, val currency: String = "LYD", val date: Long = System.currentTimeMillis(), val period: String = "", val notes: String = "", val linkedAccountId: Long? = null, val linkedTransactionId: Long? = null)
