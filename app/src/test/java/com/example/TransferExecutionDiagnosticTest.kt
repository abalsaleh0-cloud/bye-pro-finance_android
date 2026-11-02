package com.example

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.example.finance.data.AppDatabase
import com.example.finance.data.FinanceRepository
import com.example.finance.model.AccountEntity
import com.example.finance.model.AccountType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TransferExecutionDiagnosticTest {
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
    fun tearDown() = db.close()

    @Test
    fun transferPreconditionsRemainValidInsideRoomTransaction() = runBlocking {
        val sourceId = repository.addAccount(AccountEntity(name = "Diagnostic source", type = AccountType.BANK, balance = 1000.0, currency = "SAR"))
        val targetId = repository.addAccount(AccountEntity(name = "Diagnostic target", type = AccountType.SAVINGS, balance = 200.0, currency = "SAR"))

        db.withTransaction {
            val source = db.financeDao().getAccountById(sourceId)
            val target = db.financeDao().getAccountById(targetId)
            assertTrue("source missing inside transaction", source != null)
            assertTrue("target missing inside transaction", target != null)
            assertTrue("source archived inside transaction", source?.isArchived == false)
            assertTrue("target archived inside transaction", target?.isArchived == false)
            assertTrue("currency mismatch inside transaction", source?.currency.equals(target?.currency, ignoreCase = true))
            assertTrue("source balance is not finite: ${source?.balance}", source?.balance?.isFinite() == true)
            assertTrue("target balance is not finite: ${target?.balance}", target?.balance?.isFinite() == true)
        }

        val success = repository.executeTransfer(sourceId, targetId, 300.0, 10.0, "SAR", "diagnostic", "DIAG")
        assertTrue("executeTransfer returned false after all preconditions passed; sourceId=$sourceId targetId=$targetId source=${db.financeDao().getAccountById(sourceId)} target=${db.financeDao().getAccountById(targetId)}", success)
    }
}
