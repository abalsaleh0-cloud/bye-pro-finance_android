package com.example

import com.example.finance.engine.FinanceEngine
import com.example.finance.model.AccountEntity
import com.example.finance.model.AccountType
import com.example.finance.model.AssetEntity
import com.example.finance.model.AssetType
import com.example.finance.model.BudgetEntity
import com.example.finance.model.BudgetPeriod
import com.example.finance.model.DebtEntity
import com.example.finance.model.DebtStatus
import com.example.finance.model.TransactionEntity
import com.example.finance.model.TransactionType
import com.example.finance.model.TransactionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class FinanceEngineTest {

    /**
     * Requirement Test Scenario:
     * Starting Balance = 1000
     * Income = 500
     * Expense = 200
     * Transfer = 100
     * Expected Result: 1000 + 500 - 200 - 100 = 1200.00
     */
    @Test
    fun testCoreFinancialScenario() {
        val accountId = 1L
        val startingBalance = BigDecimal("1000.00")

        val transactions = listOf(
            TransactionEntity(
                id = 1,
                accountId = accountId,
                type = TransactionType.INCOME,
                amount = 500.00,
                category = "Salary",
                description = "Monthly Salary"
            ),
            TransactionEntity(
                id = 2,
                accountId = accountId,
                type = TransactionType.EXPENSE,
                amount = 200.00,
                category = "Groceries",
                description = "Grocery Store"
            ),
            TransactionEntity(
                id = 3,
                accountId = accountId,
                type = TransactionType.TRANSFER,
                amount = 100.00,
                category = "Transfer",
                description = "Transfer to Savings",
                targetAccountId = 2L
            )
        )

        val finalBalance = FinanceEngine.calculateAccountBalance(
            startingBalance = startingBalance,
            transactions = transactions,
            accountId = accountId
        )

        assertEquals(BigDecimal("1200.00"), finalBalance)

        // Net Cash Flow for the period: Income (500) - Expense (200) = 300.00
        val netCashFlow = FinanceEngine.calculateNetCashFlow(transactions)
        assertEquals(BigDecimal("300.00"), netCashFlow)
    }

    @Test
    fun cancelledTransactionsDoNotChangeBalancesAndAdjustmentsAreAdditive() {
        val accountId = 7L
        val transactions = listOf(
            TransactionEntity(id = 1, accountId = accountId, type = TransactionType.EXPENSE, amount = 80.0, category = "Food", description = "Cancelled", status = TransactionStatus.CANCELLED),
            TransactionEntity(id = 2, accountId = accountId, type = TransactionType.ADJUSTMENT, amount = 25.0, category = "Adjustment", description = "Correction")
        )
        assertEquals(
            BigDecimal("1025.00"),
            FinanceEngine.calculateAccountBalance(BigDecimal("1000.00"), transactions, accountId)
        )
        assertEquals(BigDecimal("0.00"), FinanceEngine.calculateTotalExpenses(transactions))
    }

    @Test
    fun testRefundScenario() {
        val accountId = 1L
        val startingBalance = BigDecimal("1000.00")

        val transactions = listOf(
            TransactionEntity(
                id = 1,
                accountId = accountId,
                type = TransactionType.EXPENSE,
                amount = 150.00,
                category = "Shopping",
                description = "Online Order"
            ),
            TransactionEntity(
                id = 2,
                accountId = accountId,
                type = TransactionType.REFUND,
                amount = 150.00,
                category = "Shopping",
                description = "Online Order Refund"
            )
        )

        val balanceAfterRefund = FinanceEngine.calculateAccountBalance(
            startingBalance = startingBalance,
            transactions = transactions,
            accountId = accountId
        )

        // 1000 - 150 + 150 = 1000
        assertEquals(BigDecimal("1000.00"), balanceAfterRefund)
        assertEquals(BigDecimal("150.00"), FinanceEngine.calculateTotalIncome(transactions))
    }

    @Test
    fun testCurrencyConversion() {
        // 100 USD to SAR = 375.00
        val inSar = FinanceEngine.convertCurrency(BigDecimal("100.00"), "USD", "SAR")
        assertEquals(BigDecimal("375.00"), inSar)

        // 375 SAR to USD = 100.00
        val inUsd = FinanceEngine.convertCurrency(BigDecimal("375.00"), "SAR", "USD")
        assertEquals(BigDecimal("100.00"), inUsd)

        // 10 KWD to SAR = 122.50
        val kwdInSar = FinanceEngine.convertCurrency(BigDecimal("10.00"), "KWD", "SAR")
        assertEquals(BigDecimal("122.50"), kwdInSar)
    }

    @Test
    fun testNetWorthCalculationWithMultiCurrency() {
        val accounts = listOf(
            AccountEntity(id = 1, name = "Bank SAR", type = AccountType.BANK, balance = 25000.00, currency = "SAR"),
            AccountEntity(id = 2, name = "Wallet USD", type = AccountType.SAVINGS, balance = 10000.00, currency = "USD") // 37,500 SAR
        )
        val assets = listOf(
            AssetEntity(id = 1, name = "Real Estate", type = AssetType.PROPERTY, value = 500000.00, currency = "SAR"),
            AssetEntity(id = 2, name = "Gold", type = AssetType.GOLD, value = 35000.00, currency = "SAR")
        )
        val debts = listOf(
            DebtEntity(
                id = 1,
                name = "Car Loan",
                creditor = "Bank",
                principal = 50000.00,
                remaining = 20000.00,
                currency = "SAR",
                interestRate = 3.0,
                status = DebtStatus.ACTIVE
            )
        )

        // Liquid = 25,000 + (10,000 * 3.75) = 25,000 + 37,500 = 62,500 SAR
        // Assets = 500,000 + 35,000 = 535,000 SAR
        // Total Assets = 62,500 + 535,000 = 597,500 SAR
        // Debts = 20,000 SAR
        // Net Worth in SAR = 597,500 - 20,000 = 577,500.00 SAR
        val netWorth = FinanceEngine.calculateNetWorth(accounts, assets, debts, baseCurrency = "SAR")
        assertEquals(BigDecimal("577500.00"), netWorth)
    }

    @Test
    fun testBudgetThresholds() {
        val budget = BudgetEntity(
            id = 1,
            name = "Food",
            category = "Dining",
            period = BudgetPeriod.MONTHLY,
            limitAmount = 1000.00,
            startDate = 0L,
            endDate = Long.MAX_VALUE
        )

        val txs75 = listOf(
            TransactionEntity(id = 1, accountId = 1, type = TransactionType.EXPENSE, amount = 780.00, category = "Dining", description = "Dining out", date = 1000L)
        )
        val status75 = FinanceEngine.calculateBudgetStatus(budget, txs75)
        assertEquals(75, status75.alertThreshold)
        assertFalse(status75.isOverBudget)

        val txsOver = listOf(
            TransactionEntity(id = 1, accountId = 1, type = TransactionType.EXPENSE, amount = 1100.00, category = "Dining", description = "Party dinner", date = 1000L)
        )
        val statusOver = FinanceEngine.calculateBudgetStatus(budget, txsOver)
        assertEquals(100, statusOver.alertThreshold)
        assertTrue(statusOver.isOverBudget)
    }

    @Test
    fun testSavingsProgressCalculation() {
        val target = 24000.00
        val current = 12000.00
        val sixMonthsFuture = System.currentTimeMillis() + (180L * 24 * 60 * 60 * 1000)

        val progress = FinanceEngine.calculateSavingsProgress(target, current, sixMonthsFuture)
        assertEquals(50.0f, progress.percentage, 0.5f)
        assertEquals(BigDecimal("12000.00"), progress.remaining)
        assertTrue(progress.requiredMonthlySaving > BigDecimal.ZERO)
    }

    @Test
    fun testDebtPayoffCalculation() {
        val months = FinanceEngine.calculateDebtPayoffMonths(
            remainingPrincipal = 12000.0,
            annualInterestRatePercent = 0.0,
            monthlyPayment = 1000.0
        )
        assertEquals(12, months)
    }
}
