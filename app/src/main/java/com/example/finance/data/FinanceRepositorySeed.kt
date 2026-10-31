package com.example.finance.data

import com.example.finance.model.AccountEntity
import com.example.finance.model.AccountType
import com.example.finance.model.TransactionEntity
import com.example.finance.model.TransactionType
import kotlinx.coroutines.flow.first

/** Local compatibility seed used by the existing integration test suite. */
suspend fun FinanceRepository.seedInitialDataIfEmpty() {
    if (accounts.first().isNotEmpty()) return
    val accountId = addAccount(
        AccountEntity(
            name = "الحساب الرئيسي",
            type = AccountType.CASH,
            currency = "LYD",
            balance = 1000.0,
            openingBalance = 1000.0
        )
    )
    addTransaction(
        TransactionEntity(
            accountId = accountId,
            type = TransactionType.INCOME,
            amount = 1000.0,
            currency = "LYD",
            category = "Other",
            description = "Initial balance"
        )
    )
}
