package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.finance.data.AppDatabase
import com.example.finance.data.FinanceRepository
import com.example.finance.engine.FinanceEngine
import com.example.finance.model.AccountEntity
import com.example.finance.model.AccountType
import com.example.finance.model.TransactionEntity
import com.example.finance.model.TransactionType
import com.example.finance.model.TransactionStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FinanceRepositoryIntegrationTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: FinanceRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = FinanceRepository(db.financeDao(), db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testAddAccountAndBalanceTracking() = runBlocking {
        val accId = repository.addAccount(
            AccountEntity(name = "Checking", type = AccountType.BANK, balance = 1000.00, currency = "LYD")
        )
        assertTrue(accId > 0)

        // Add Income of 500
        repository.addTransaction(
            TransactionEntity(
                accountId = accId,
                type = TransactionType.INCOME,
                amount = 500.00,
                category = "Salary",
                description = "Bonus"
            )
        )

        val updatedAcc = db.financeDao().getAccountById(accId)
        assertEquals(1500.00, updatedAcc?.balance ?: 0.0, 0.01)

        // Add Expense of 200
        repository.addTransaction(
            TransactionEntity(
                accountId = accId,
                type = TransactionType.EXPENSE,
                amount = 200.00,
                category = "Food",
                description = "Dinner"
            )
        )

        val afterExpenseAcc = db.financeDao().getAccountById(accId)
        assertEquals(1300.00, afterExpenseAcc?.balance ?: 0.0, 0.01)
    }

    @Test
    fun testAtomicTransferSuccess() = runBlocking {
        val srcId = repository.addAccount(AccountEntity(name = "Src", type = AccountType.BANK, balance = 1000.00, currency = "SAR"))
        val dstId = repository.addAccount(AccountEntity(name = "Dst", type = AccountType.SAVINGS, balance = 200.00, currency = "SAR"))

        val success = repository.executeTransfer(
            fromAccountId = srcId,
            toAccountId = dstId,
            amount = 300.00,
            fee = 10.00,
            currency = "SAR",
            description = "Savings deposit",
            reference = "REF-101"
        )

        assertTrue(success)
        val srcAcc = db.financeDao().getAccountById(srcId)
        val dstAcc = db.financeDao().getAccountById(dstId)

        // Src: 1000 - 300 - 10 = 690.00
        assertEquals(690.00, srcAcc?.balance ?: 0.0, 0.01)
        // Dst: 200 + 300 = 500.00
        assertEquals(500.00, dstAcc?.balance ?: 0.0, 0.01)

        val ledgerRows = repository.transactions.first()
        assertEquals(
            FinanceEngine.toBigDecimal(srcAcc!!.balance),
            FinanceEngine.calculateAccountBalance(
                BigDecimal("1000.00"), ledgerRows, srcId
            )
        )
        assertEquals(
            FinanceEngine.toBigDecimal(dstAcc!!.balance),
            FinanceEngine.calculateAccountBalance(
                BigDecimal("200.00"), ledgerRows, dstId
            )
        )
        assertEquals(BigDecimal("10.00"), FinanceEngine.calculateTotalExpenses(ledgerRows))
        assertEquals(BigDecimal("0.00"), FinanceEngine.calculateTotalAdjustments(ledgerRows))
        assertEquals(BigDecimal("-10.00"), FinanceEngine.calculateNetCashFlow(ledgerRows))
    }

    @Test
    fun crossCurrencyTransferIsRejectedWithoutChangingBalances() = runBlocking {
        val sourceId = repository.addAccount(
            AccountEntity(name = "LYD wallet", type = AccountType.CASH, balance = 500.0, currency = "LYD")
        )
        val destinationId = repository.addAccount(
            AccountEntity(name = "USD wallet", type = AccountType.SAVINGS, balance = 100.0, currency = "USD")
        )

        val transferred = repository.executeTransfer(
            fromAccountId = sourceId,
            toAccountId = destinationId,
            amount = 50.0,
            fee = 0.0,
            currency = "LYD",
            description = "Cross currency test",
            reference = "FX-001"
        )
        assertFalse("Cross-currency transfer must be blocked without verified FX", transferred)

        assertEquals(500.0, db.financeDao().getAccountById(sourceId)?.balance ?: 0.0, 0.001)
        assertEquals(100.0, db.financeDao().getAccountById(destinationId)?.balance ?: 0.0, 0.001)
        assertTrue(repository.transfers.first().isEmpty())
    }

    @Test
    fun importingTransactionsIsAtomicWhenAnyRowIsInvalid() = runBlocking {
        val accountId = repository.addAccount(
            AccountEntity(name = "Import account", type = AccountType.CASH, balance = 100.0, currency = "LYD")
        )
        val valid = TransactionEntity(
            accountId = accountId,
            type = TransactionType.INCOME,
            amount = 50.0,
            currency = "LYD",
            category = "Income",
            description = "Valid row"
        )
        val invalid = TransactionEntity(
            accountId = 99999L,
            type = TransactionType.EXPENSE,
            amount = 10.0,
            currency = "LYD",
            category = "Expense",
            description = "Missing account"
        )
        try {
            repository.importTransactions(listOf(valid, invalid))
            fail("Import must roll back when any row is invalid")
        } catch (_: IllegalArgumentException) {
            assertEquals(100.0, db.financeDao().getAccountById(accountId)?.balance ?: 0.0, 0.001)
            assertTrue(repository.transactions.first().isEmpty())
        }
    }

    @Test
    fun testAtomicTransferRollbackOnInsufficientFunds() = runBlocking {
        val srcId = repository.addAccount(AccountEntity(name = "Src", type = AccountType.BANK, balance = 100.00, currency = "SAR"))
        val dstId = repository.addAccount(AccountEntity(name = "Dst", type = AccountType.SAVINGS, balance = 50.00, currency = "SAR"))

        try {
            repository.executeTransfer(
                fromAccountId = srcId,
                toAccountId = dstId,
                amount = 500.00, // Insufficient!
                fee = 0.00,
                currency = "SAR",
                description = "Will fail",
                reference = "FAIL-1"
            )
            fail("Expected IllegalStateException due to insufficient funds")
        } catch (_: IllegalStateException) {
            // Expected
        }

        // Verify total rollback: neither account was modified!
        val srcAcc = db.financeDao().getAccountById(srcId)
        val dstAcc = db.financeDao().getAccountById(dstId)
        assertEquals(100.00, srcAcc?.balance ?: 0.0, 0.01)
        assertEquals(50.00, dstAcc?.balance ?: 0.0, 0.01)
    }

    @Test
    fun cancelledRowsDoNotPostAndAdjustmentsApplyAsDeltas() = runBlocking {
        val accountId = repository.addAccount(
            AccountEntity(name = "Adjustment account", type = AccountType.BANK, balance = 1000.0, currency = "LYD")
        )
        val cancelledId = repository.addTransaction(
            TransactionEntity(
                accountId = accountId,
                type = TransactionType.EXPENSE,
                amount = 80.0,
                category = "Food",
                description = "Cancelled expense",
                status = TransactionStatus.CANCELLED
            )
        )
        assertTrue(cancelledId > 0)
        assertEquals(1000.0, db.financeDao().getAccountById(accountId)?.balance ?: 0.0, 0.01)

        val adjustmentId = repository.addTransaction(
            TransactionEntity(
                accountId = accountId,
                type = TransactionType.ADJUSTMENT,
                amount = 25.0,
                category = "Adjustment",
                description = "Balance correction"
            )
        )
        assertTrue(adjustmentId > 0)
        assertEquals(1025.0, db.financeDao().getAccountById(accountId)?.balance ?: 0.0, 0.01)
    }

    @Test
    fun testAccountArchiving() = runBlocking {
        val accId = repository.addAccount(AccountEntity(name = "Old Card", type = AccountType.CREDIT_CARD, balance = 0.00))

        repository.archiveAccount(accId, true)
        val activeAccounts = repository.accounts.first()
        assertFalse(activeAccounts.any { it.id == accId })

        val archivedAccounts = repository.archivedAccounts.first()
        assertTrue(archivedAccounts.any { it.id == accId })
    }
    @Test
    fun seedInitialDataCreatesDashboardRecordsAndIsIdempotent() = runBlocking {
        repository.seedInitialDataIfEmpty()
        val seededAccounts = repository.accounts.first()
        val seededTransactions = repository.transactions.first()
        assertTrue("Initial accounts should be available", seededAccounts.isNotEmpty())
        assertTrue("Initial transactions should be available", seededTransactions.isNotEmpty())

        repository.seedInitialDataIfEmpty()
        assertEquals("Seeding twice must not duplicate accounts", seededAccounts.size, repository.accounts.first().size)
    }

}
