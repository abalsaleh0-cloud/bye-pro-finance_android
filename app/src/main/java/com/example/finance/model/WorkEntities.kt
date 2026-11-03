package com.example.finance.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class WorkSourceType { EMPLOYMENT, FREELANCE }
enum class WorkRecordType { ADVANCE, DEDUCTION, BONUS, OVERTIME, FREELANCE_INCOME, FREELANCE_EXPENSE, SALARY_PAYMENT }

@Entity(tableName = "work_sources")
data class WorkSourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: WorkSourceType,
    val monthlySalary: Double = 0.0,
    val currency: String = "LYD",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "work_records")
data class WorkRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceId: Long,
    val type: WorkRecordType,
    val amount: Double,
    val currency: String = "LYD",
    val date: Long = System.currentTimeMillis(),
    val period: String = "",
    val notes: String = "",
    val linkedAccountId: Long? = null,
    val linkedTransactionId: Long? = null
)
