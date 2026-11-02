package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.finance.data.AppDatabase
import com.example.finance.data.FinanceRepository
import com.example.finance.model.AccountEntity
import com.example.finance.model.AccountType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AccountCurrencyPersistenceRegressionTest {
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
    fun savingsAccountPersistsExplicitCurrencyWithoutBeingRewritten() = runBlocking {
        val sourceId = repository.addAccount(
            AccountEntity(name = "Currency source", type = AccountType.BANK, balance = 1000.0, currency = "SAR")
        )
        val targetId = repository.addAccount(
            AccountEntity(name = "Currency target", type = AccountType.SAVINGS, balance = 200.0, currency = "SAR")
        )

        val source = db.financeDao().getAccountById(sourceId)
        val target = db.financeDao().getAccountById(targetId)
        println("ACCOUNT_CURRENCY_REGRESSION sourceId=$sourceId sourceCurrency=${source?.currency} targetId=$targetId targetCurrency=${target?.currency}")

        assertEquals("SAR", source?.currency)
        assertEquals("SAR", target?.currency)
    }
}
