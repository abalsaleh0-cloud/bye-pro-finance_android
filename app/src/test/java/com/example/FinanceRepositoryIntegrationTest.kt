package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.finance.data.AppDatabase
import com.example.finance.data.FinanceRepository
import com.example.finance.model.AccountEntity
import com.example.finance.model.AccountType
import com.example.finance.model.TransactionEntity
import com.example.finance.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
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
            AccountEntity(name = "Checking", type = AccountType.BANK, balance = 1000.00, currency = "SAR")
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
    fun testAccountArchiving() = runBlocking {
        val accId = repository.addAccount(AccountEntity(name = "Old Card", type = AccountType.CREDIT_CARD, balance = 0.00))

        repository.archiveAccount(accId, true)
        val activeAccounts = repository.accounts.first()
        assertFalse(activeAccounts.any { it.id == accId })

        val archivedAccounts = repository.archivedAccounts.first()
        assertTrue(archivedAccounts.any { it.id == accId })
    }
}
