package com.example.finance.engine

import com.example.finance.model.TransactionEntity
import com.example.finance.model.TransactionStatus
import com.example.finance.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class LedgerSummaryTest {
    private fun tx(
        type: TransactionType,
        amount: Double,
        currency: String = "SAR",
        status: TransactionStatus = TransactionStatus.COMPLETED
    ) = TransactionEntity(
        accountId = 1L,
        type = type,
        amount = amount,
        currency = currency,
        category = "test",
        description = "test",
        status = status
    )

    @Test
    fun webContractExcludesTransfersAndCancelledRows() {
        val result = LedgerSummary.summarizeByCurrency(
            listOf(
                tx(TransactionType.INCOME, 100.0),
                tx(TransactionType.EXPENSE, 25.0),
                tx(TransactionType.ADJUSTMENT, 5.0),
                tx(TransactionType.TRANSFER, 1000.0),
                tx(TransactionType.EXPENSE, 500.0, status = TransactionStatus.CANCELLED)
            )
        )

        val sar = result.getValue("SAR")
        assertEquals("100.00", sar.income.toPlainString())
        assertEquals("25.00", sar.expense.toPlainString())
        assertEquals("5.00", sar.adjustment.toPlainString())
        assertEquals("80.00", sar.net.toPlainString())
    }

    @Test
    fun summaryIsSeparatedByCurrency() {
        val result = LedgerSummary.summarizeByCurrency(
            listOf(tx(TransactionType.INCOME, 10.0, "USD"), tx(TransactionType.EXPENSE, 2.0, "SAR"))
        )
        assertEquals("10.00", result.getValue("USD").income.toPlainString())
        assertEquals("2.00", result.getValue("SAR").expense.toPlainString())
    }
}
