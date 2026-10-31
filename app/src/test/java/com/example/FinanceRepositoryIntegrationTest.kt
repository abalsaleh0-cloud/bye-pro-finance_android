package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.finance.data.AppDatabase
import com.example.finance.data.FinanceRepository
import com.example.finance.engine.FinanceEngine
import com.example.finance.model.AccountEntity
import com.example.finance.model.AccountType
import com.example.finance.model.BudgetEntity
import com.example.finance.model.BudgetPeriod
import com.example.finance.model.BillEntity
import com.example.finance.model.DebtEntity
import com.example.finance.model.DebtStatus
import com.example.finance.model.SavingsGoalEntity
import com.example.finance.model.AssetEntity
import com.example.finance.model.AssetType
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
    fun billPaymentCurrencyMismatchDoesNotMarkBillPaidOrChangeBalance() = runBlocking {
        val accountId = repository.addAccount(
            AccountEntity(name = "LYD account", type = AccountType.CASH, balance = 100.0, currency = "LYD")
        )
        repository.addBill(
            BillEntity(name = "USD bill", category = "Utilities", amount = 20.0, currency = "USD", dueDate = System.currentTimeMillis())
        )
        val billId = db.financeDao().getAllBills().first().single().id

        assertFalse(repository.markBillPaid(billId, accountId))
        assertFalse(db.financeDao().getAllBills().first().single().isPaid)
        assertEquals(100.0, db.financeDao().getAccountById(accountId)?.balance ?: 0.0, 0.001)
        assertTrue(repository.transactions.first().isEmpty())
    }

    @Test
    fun savingsContributionCurrencyMismatchDoesNotChangeGoalOrBalance() = runBlocking {
        val accountId = repository.addAccount(
            AccountEntity(name = "LYD account", type = AccountType.CASH, balance = 100.0, currency = "LYD")
        )
        repository.addSavingsGoal(
            SavingsGoalEntity(name = "USD goal", targetAmount = 500.0, currency = "USD", deadline = System.currentTimeMillis() + 86400000)
        )
        val goalId = db.financeDao().getAllSavingsGoals().first().single().id

        assertFalse(repository.contributeToSavings(goalId, 25.0, accountId))
        assertEquals(0.0, db.financeDao().getAllSavingsGoals().first().single().currentAmount, 0.001)
        assertEquals(100.0, db.financeDao().getAccountById(accountId)?.balance ?: 0.0, 0.001)
        assertTrue(repository.transactions.first().isEmpty())
    }

    @Test
    fun importingSameTransactionTwiceDoesNotDoublePostBalance() = runBlocking {
        val accountId = repository.addAccount(
            AccountEntity(name = "Import account", type = AccountType.CASH, balance = 100.0, currency = "LYD")
        )
        val row = TransactionEntity(
            accountId = accountId,
            type = TransactionType.INCOME,
            amount = 25.0,
            currency = "LYD",
            category = "Income",
            description = "Import idempotency fixture",
            date = 123456789L
        )

        assertEquals(1, repository.importTransactions(listOf(row)))
        assertEquals(0, repository.importTransactions(listOf(row)))
        assertEquals(125.0, db.financeDao().getAccountById(accountId)?.balance ?: 0.0, 0.001)
        assertEquals(1, repository.transactions.first().size)
    }

    @Test
    fun debtPaymentCannotExceedRemainingBalance() = runBlocking {
        repository.addDebt(
            DebtEntity(
                name = "Personal debt",
                creditor = "Creditor",
                principal = 100.0,
                remaining = 40.0,
                currency = "LYD"
            )
        )
        val debtId = db.financeDao().getAllDebts().first().single().id
        assertFalse(repository.recordDebtPayment(debtId, 45.0, "Overpayment"))
        assertEquals(40.0, db.financeDao().getDebtById(debtId)?.remaining ?: 0.0, 0.001)
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

    @Test
    fun accountCanBeDeactivatedAndReactivatedWithoutBeingDeleted() = runBlocking {
        val accountId = repository.addAccount(
            AccountEntity(name = "Status account", type = AccountType.CASH, balance = 75.0, currency = "LYD")
        )

        repository.archiveAccount(accountId, true)
        assertTrue(repository.accounts.first().isEmpty())
        assertEquals(accountId, repository.archivedAccounts.first().single().id)

        repository.archiveAccount(accountId, false)
        assertEquals(accountId, repository.accounts.first().single().id)
        assertTrue(repository.archivedAccounts.first().isEmpty())
        assertEquals(75.0, db.financeDao().getAccountById(accountId)?.balance ?: 0.0, 0.001)
    }

    @Test
    fun openingBalanceRemainsUnchangedWhenCurrentBalanceChanges() = runBlocking {
        val accountId = repository.addAccount(
            AccountEntity(name = "Opening balance", type = AccountType.CASH, balance = 125.0, currency = "LYD")
        )
        repository.addTransaction(
            TransactionEntity(
                accountId = accountId,
                type = TransactionType.INCOME,
                amount = 25.0,
                currency = "LYD",
                category = "Income",
                description = "Opening balance regression"
            )
        )
        val account = db.financeDao().getAccountById(accountId)
        assertEquals(125.0, account?.openingBalance ?: 0.0, 0.001)
        assertEquals(150.0, account?.balance ?: 0.0, 0.001)
    }

    @Test
    fun transactionStatusChangesPostAndReverseAccountBalanceExactlyOnce() = runBlocking {
        val accountId = repository.addAccount(
            AccountEntity(name = "Status lifecycle", type = AccountType.CASH, balance = 100.0, currency = "LYD")
        )
        val transaction = TransactionEntity(
            accountId = accountId,
            type = TransactionType.EXPENSE,
            amount = 25.0,
            currency = "LYD",
            category = "Other",
            description = "Status lifecycle test",
            status = TransactionStatus.PENDING
        )
        val transactionId = repository.addTransaction(transaction)
        assertEquals(100.0, repository.accounts.first().first { it.id == accountId }.balance, 0.001)

        val stored = repository.transactions.first().first { it.id == transactionId }
        repository.updateTransactionStatus(stored, TransactionStatus.COMPLETED)
        assertEquals(75.0, repository.accounts.first().first { it.id == accountId }.balance, 0.001)

        val completed = repository.transactions.first().first { it.id == transactionId }
        repository.updateTransactionStatus(completed, TransactionStatus.PENDING)
        assertEquals(100.0, repository.accounts.first().first { it.id == accountId }.balance, 0.001)

        val pending = repository.transactions.first().first { it.id == transactionId }
        repository.updateTransactionStatus(pending, TransactionStatus.PENDING)
        assertEquals(100.0, repository.accounts.first().first { it.id == accountId }.balance, 0.001)
    }


    @Test
    fun successfulTransferUpdatesBothBalancesAndWritesLedgerAtomically() = runBlocking {
        val sourceId = repository.addAccount(
            AccountEntity(name = "Transfer source", type = AccountType.BANK, balance = 500.0, currency = "LYD")
        )
        val targetId = repository.addAccount(
            AccountEntity(name = "Transfer target", type = AccountType.SAVINGS, balance = 50.0, currency = "LYD")
        )

        assertTrue(repository.executeTransfer(sourceId, targetId, 100.0, 2.0, "LYD", "Monthly move", ""))
        assertEquals(398.0, db.financeDao().getAccountById(sourceId)?.balance ?: 0.0, 0.001)
        assertEquals(150.0, db.financeDao().getAccountById(targetId)?.balance ?: 0.0, 0.001)
        assertEquals(1, repository.transfers.first().size)
        assertEquals(3, repository.transactions.first().size)
    }

    @Test
    fun transferRejectsDifferentCurrenciesWithoutChangingBalances() = runBlocking {
        val sourceId = repository.addAccount(
            AccountEntity(name = "LYD source", type = AccountType.BANK, balance = 500.0, currency = "LYD")
        )
        val targetId = repository.addAccount(
            AccountEntity(name = "USD target", type = AccountType.SAVINGS, balance = 50.0, currency = "USD")
        )

        assertFalse(repository.executeTransfer(sourceId, targetId, 100.0, 0.0, "LYD", "Currency mismatch", ""))
        assertEquals(500.0, db.financeDao().getAccountById(sourceId)?.balance ?: 0.0, 0.001)
        assertEquals(50.0, db.financeDao().getAccountById(targetId)?.balance ?: 0.0, 0.001)
        assertTrue(repository.transfers.first().isEmpty())
    }



    @Test
    fun debtDirectionsEditingAndTimestampedPaymentsPersistCorrectly() = runBlocking {
        val dueDate = 1_800_000_000_000L
        repository.addDebt(
            DebtEntity(
                name = "Client A",
                creditor = "Contract",
                principal = 900.0,
                remaining = 900.0,
                dueDate = dueDate,
                currency = "USD",
                notes = "Invoice settlement",
                direction = "owed_to_me"
            )
        )
        val created = repository.debts.first().single()
        assertEquals("owed_to_me", created.direction)
        assertEquals("USD", created.currency)
        assertEquals(dueDate, created.dueDate)

        val paymentDate = 1_800_000_123_000L
        assertTrue(repository.recordDebtPayment(created.id, 250.0, "First installment", paymentDate))
        val afterPartial = repository.debts.first().single()
        assertEquals(650.0, afterPartial.remaining, 0.001)
        assertEquals(DebtStatus.ACTIVE, afterPartial.status)
        val history = repository.debtPayments(created.id).first()
        assertEquals(1, history.size)
        assertEquals(paymentDate, history.single().date)
        assertEquals("USD", history.single().currency)
        assertEquals("First installment", history.single().notes)

        assertFalse(repository.recordDebtPayment(created.id, 700.0, "Overpayment", paymentDate))
        assertEquals(650.0, repository.debts.first().single().remaining, 0.001)

        repository.updateDebt(afterPartial.copy(name = "Client A updated", direction = "i_owe"))
        val updated = repository.debts.first().single()
        assertEquals("Client A updated", updated.name)
        assertEquals("i_owe", updated.direction)

        assertTrue(repository.recordDebtPayment(created.id, 650.0, "Final payment", paymentDate + 1000))
        assertEquals(DebtStatus.PAID, repository.debts.first().single().status)
        assertEquals(2, repository.debtPayments(created.id).first().size)
    }

    @Test
    fun debtCreationRejectsInvalidPrincipalAndDirection() = runBlocking {
        try {
            repository.addDebt(DebtEntity(name = "Invalid", creditor = "", principal = 0.0, remaining = 0.0))
            fail("Expected invalid principal to be rejected")
        } catch (_: IllegalArgumentException) {
            // expected
        }
        try {
            repository.addDebt(DebtEntity(name = "Invalid direction", creditor = "", principal = 10.0, remaining = 10.0, direction = "unknown"))
            fail("Expected invalid direction to be rejected")
        } catch (_: IllegalArgumentException) {
            // expected
        }
        assertTrue(repository.debts.first().isEmpty())
    }


    @Test
    fun billSavingsAndAssetUpdatesPersistAndKeepPaymentState() = runBlocking {
        val accountId = repository.addAccount(AccountEntity(name = "Bill account", type = AccountType.CASH, balance = 200.0, currency = "LYD"))
        repository.addBill(BillEntity(name = "Internet", category = "Utilities", amount = 20.0, currency = "LYD", dueDate = 123456789L))
        val bill = db.financeDao().getAllBills().first().single()
        assertTrue(repository.markBillPaid(bill.id, accountId))
        repository.updateBill(bill.copy(name = "Updated internet", amount = 25.0))
        val updatedBill = db.financeDao().getBillById(bill.id)!!
        assertEquals("Updated internet", updatedBill.name)
        assertEquals(25.0, updatedBill.amount, 0.001)
        assertTrue("Editing a paid bill must preserve its paid state", updatedBill.isPaid)

        repository.addSavingsGoal(SavingsGoalEntity(name = "Emergency", targetAmount = 100.0, currentAmount = 20.0, deadline = 987654321L))
        val goal = db.financeDao().getAllSavingsGoals().first().single()
        repository.updateSavingsGoal(goal.copy(name = "Emergency reserve", targetAmount = 150.0, targetMonthlyDeposit = 15.0))
        val updatedGoal = db.financeDao().getSavingsGoalById(goal.id)!!
        assertEquals("Emergency reserve", updatedGoal.name)
        assertEquals(150.0, updatedGoal.targetAmount, 0.001)
        assertEquals(15.0, updatedGoal.targetMonthlyDeposit, 0.001)

        repository.addAsset(AssetEntity(name = "Gold", type = AssetType.GOLD, value = 50.0, currency = "LYD"))
        val asset = db.financeDao().getAllAssets().first().single()
        repository.updateAsset(asset.copy(name = "Gold bars", value = 75.0, notes = "Vault"))
        val updatedAsset = db.financeDao().getAllAssets().first().single()
        assertEquals("Gold bars", updatedAsset.name)
        assertEquals(75.0, updatedAsset.value, 0.001)
        assertEquals("Vault", updatedAsset.notes)
    }
}
