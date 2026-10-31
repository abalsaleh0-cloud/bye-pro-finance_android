package com.example.finance.engine

import com.example.finance.model.AppLanguage
import com.example.finance.model.BudgetEntity
import com.example.finance.model.TransactionEntity
import com.example.finance.model.TransactionStatus
import com.example.finance.model.TransactionType
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceInsightsEngineTest {
    @Test
    fun budgetInsightsUseRecordedAmountsAndIgnoreCancelledTransactions() {
        val transactions = listOf(
            TransactionEntity(
                accountId = 1, type = TransactionType.EXPENSE, amount = 950.0,
                currency = "LYD", category = "Groceries", description = "Recorded expense",
                date = 1_000L
            ),
            TransactionEntity(
                accountId = 1, type = TransactionType.EXPENSE, amount = 500.0,
                currency = "LYD", category = "Groceries", description = "Cancelled expense",
                date = 1_000L, status = TransactionStatus.CANCELLED
            )
        )
        val budgets = listOf(
            BudgetEntity(
                name = "Groceries", category = "Groceries", limitAmount = 1_000.0,
                currency = "LYD", startDate = 0L, endDate = 2_000L
            )
        )

        val insights = FinanceInsightsEngine.generateInsights(transactions, budgets, AppLanguage.EN)

        assertTrue(insights.any { it.impactType == "WARNING" && it.description.contains("95%") })
        assertTrue(insights.none { it.description.contains("1450") })
    }

    @Test
    fun emptyDataProducesNoInventedAmounts() {
        val insights = FinanceInsightsEngine.generateInsights(emptyList(), emptyList(), AppLanguage.AR)
        assertTrue(insights.single().description.contains("أضف معاملات"))
        assertTrue(insights.single().estimatedSavings == 0.0)
    }
}
