package com.example
import com.example.finance.engine.WorkLedgerCalculator
import com.example.finance.model.WorkRecordEntity
import com.example.finance.model.WorkRecordType
import com.example.finance.model.WorkSourceEntity
import com.example.finance.model.WorkSourceType
import org.junit.Assert.assertEquals
import org.junit.Test
class WorkLedgerCalculatorTest {
    @Test fun salaryDueSubtractsAdvancesDeductionsAndPartialPayments() {
        val source = WorkSourceEntity(id = 1, name = "Job", type = WorkSourceType.EMPLOYMENT, monthlySalary = 2000.0)
        val rows = listOf(WorkRecordEntity(sourceId = 1, type = WorkRecordType.SALARY_ADVANCE, amount = 300.0, period = "2026-11"), WorkRecordEntity(sourceId = 1, type = WorkRecordType.SALARY_DEDUCTION, amount = 100.0, period = "2026-11"), WorkRecordEntity(sourceId = 1, type = WorkRecordType.SALARY_BONUS, amount = 150.0, period = "2026-11"), WorkRecordEntity(sourceId = 1, type = WorkRecordType.SALARY_OVERTIME, amount = 50.0, period = "2026-11"), WorkRecordEntity(sourceId = 1, type = WorkRecordType.SALARY_PAYMENT, amount = 400.0, period = "2026-11"))
        assertEquals(1400.0, WorkLedgerCalculator.salaryDue(source, rows, "2026-11").toDouble(), 0.001)
    }
    @Test fun freelanceReceivableAllowsPartialPayments() {
        val income = WorkRecordEntity(id = 10, sourceId = 2, type = WorkRecordType.FREELANCE_INCOME, amount = 900.0)
        val rows = listOf(income, WorkRecordEntity(sourceId = 2, relatedRecordId = 10, type = WorkRecordType.FREELANCE_PAYMENT, amount = 250.0), WorkRecordEntity(sourceId = 2, relatedRecordId = 10, type = WorkRecordType.FREELANCE_PAYMENT, amount = 150.0))
        assertEquals(500.0, WorkLedgerCalculator.freelanceOutstanding(income, rows).toDouble(), 0.001)
    }
    @Test fun internalTransfersAffectWorkCashButRemainSeparateFromIncomeAndExpenses() {
        val rows = listOf(WorkRecordEntity(type = WorkRecordType.WORK_TRANSFER_IN, amount = 500.0), WorkRecordEntity(type = WorkRecordType.FREELANCE_PAYMENT, amount = 300.0), WorkRecordEntity(type = WorkRecordType.FREELANCE_EXPENSE, amount = 100.0), WorkRecordEntity(type = WorkRecordType.WORK_TRANSFER_OUT, amount = 50.0))
        assertEquals(650.0, WorkLedgerCalculator.cashBalance(rows, "LYD").toDouble(), 0.001)
    }
}
