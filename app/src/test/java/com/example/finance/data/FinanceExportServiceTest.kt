package com.example.finance.data

import com.example.finance.model.TransactionEntity
import com.example.finance.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FinanceExportServiceTest {
    @Test
    fun csvExportRoundTripsQuotedCommasQuotesAndMultilineText() {
        val original = TransactionEntity(
            accountId = 9,
            type = TransactionType.EXPENSE,
            amount = 25.75,
            currency = "LYD",
            category = "Food, groceries",
            description = "Receipt for \"family lunch\"\nsecond line",
            merchant = "Cafe, Tripoli",
            notes = "paid, confirmed"
        )
        val csv = FinanceExportService.transactionsToCsv(listOf(original))
        val restored = FinanceExportService.transactionsFromCsv(csv).single()
        assertEquals(original.accountId, restored.accountId)
        assertEquals(original.amount, restored.amount, 0.001)
        assertEquals(original.category, restored.category)
        assertEquals(original.description, restored.description)
        assertEquals(original.merchant, restored.merchant)
        assertEquals(original.notes, restored.notes)
    }

    @Test
    fun jsonExportRoundTripsTransactionPayload() {
        val original = TransactionEntity(
            accountId = 7,
            type = TransactionType.EXPENSE,
            amount = 12.5,
            currency = "LYD",
            category = "Food",
            description = "Lunch",
            merchant = "Cafe"
        )
        val json = FinanceExportService.transactionsToJson(listOf(original))
        val restored = FinanceExportService.transactionsFromJson(json).single()
        assertEquals(original.accountId, restored.accountId)
        assertEquals(original.type, restored.type)
        assertEquals(original.amount, restored.amount, 0.001)
        assertEquals(original.currency, restored.currency)
        assertEquals(original.category, restored.category)
        assertEquals(original.description, restored.description)
        assertTrue(json.contains("transactions"))
    }
}
