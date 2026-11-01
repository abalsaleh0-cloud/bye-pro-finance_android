package com.example.finance.ai

import com.example.finance.model.AppLanguage
import com.example.finance.model.BudgetEntity
import com.example.finance.model.TransactionEntity
import com.example.finance.model.TransactionStatus
import com.example.finance.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.runBlocking

class OfflineFinanceServiceTest {
    private val service = OfflineFinanceService()

    @Test
    fun insightsUseRecordedAmountsAndIgnoreCancelledTransactions() {
        val transactions = listOf(
            TransactionEntity(
                accountId = 1,
                type = TransactionType.INCOME,
                amount = 1000.0,
                currency = "LYD",
                category = "Salary",
                description = "Recorded income",
                date = 1000L
            ),
            TransactionEntity(
                accountId = 1,
                type = TransactionType.EXPENSE,
                amount = 950.0,
                currency = "LYD",
                category = "Groceries",
                description = "Recorded grocery expense",
                date = 1000L
            ),
            TransactionEntity(
                accountId = 1,
                type = TransactionType.EXPENSE,
                amount = 500.0,
                currency = "LYD",
                category = "Groceries",
                description = "Cancelled expense",
                date = 1000L,
                status = TransactionStatus.CANCELLED
            )
        )
        val budgets = listOf(
            BudgetEntity(
                name = "Groceries",
                category = "Groceries",
                limitAmount = 1000.0,
                currency = "LYD",
                startDate = 0L,
                endDate = 2000L
            )
        )

        val insights = service.generateInsights(transactions, budgets, AppLanguage.EN)

        assertTrue(insights.any { it.impactType == "WARNING" && it.description.contains("95%") })
        assertFalse(insights.any { it.description.contains("340") || it.description.contains("4x") || it.description.contains("78%") })
    }

    @Test
    fun receiptTextExtractionDoesNotInventCurrencyTaxOrDate() = runBlocking {
        val extracted = service.parseReceiptImage(sampleText = "Store: Local Shop\nTotal: 25.50\nLYD\n2026-10-30").getOrThrow()
        assertEquals("Local Shop", extracted.merchant)
        assertEquals(25.50, extracted.amount, 0.001)
        assertEquals(0.0, extracted.tax, 0.001)
        assertEquals("LYD", extracted.currency)
        assertEquals("2026-10-30", extracted.dateStr)

        val incomplete = service.parseReceiptImage(sampleText = "Total: 12.00").getOrThrow()
        assertEquals("", incomplete.currency)
        assertEquals("", incomplete.dateStr)
        assertEquals(0.0, incomplete.tax, 0.001)
    }

    @Test
    fun emptyRecordsDoNotProduceInventedFinancialInsights() {
        val insights = service.generateInsights(emptyList(), emptyList(), AppLanguage.AR)

        assertEquals(1, insights.size)
        assertEquals("لا توجد مؤشرات كافية بعد", insights.single().title)
        assertEquals("TIP", insights.single().impactType)
        assertEquals(0.0, insights.single().estimatedSavings, 0.0)
    }
}
