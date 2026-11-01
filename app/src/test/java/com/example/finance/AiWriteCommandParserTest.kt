package com.example.finance

import com.example.finance.ai.AiWriteCommand
import com.example.finance.ai.AiWriteCommandParser
import com.example.finance.ai.TransactionWriteType
import com.example.finance.model.AccountEntity
import com.example.finance.model.AccountType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AiWriteCommandParserTest {
    private val wallet = AccountEntity(
        id = 7L,
        name = "المحفظة الرئيسية",
        type = AccountType.CASH,
        currency = "LYD",
        balance = 500.0
    )

    @Test
    fun parsesArabicExpenseAndUsesSingleMatchingAccount() {
        val command = AiWriteCommandParser.parse(
            "سجل مصروف 50 دينار مواصلات",
            listOf(wallet),
            "LYD"
        )

        val transaction = assertIs<AiWriteCommand.RecordTransaction>(command)
        assertEquals(TransactionWriteType.EXPENSE, transaction.type)
        assertEquals(50.0, transaction.amount)
        assertEquals("LYD", transaction.currency)
        assertEquals(wallet.id, transaction.account?.id)
        assertEquals("مواصلات", transaction.category)
    }

    @Test
    fun parsesArabicDigitsAndIncome() {
        val command = AiWriteCommandParser.parse(
            "سجّل دخل ١٬٢٠٠ دينار راتب".replace('٬', ','),
            listOf(wallet),
            "LYD"
        )

        val transaction = assertIs<AiWriteCommand.RecordTransaction>(command)
        assertEquals(TransactionWriteType.INCOME, transaction.type)
        assertEquals(1200.0, transaction.amount)
        assertEquals("راتب", transaction.category)
    }

    @Test
    fun requiresAccountWhenMultipleAccountsMatchCurrency() {
        val second = wallet.copy(id = 8L, name = "حساب آخر")
        val command = AiWriteCommandParser.parse(
            "سجل مصروف 25 دينار طعام",
            listOf(wallet, second),
            "LYD"
        )

        val transaction = assertIs<AiWriteCommand.RecordTransaction>(command)
        assertNull(transaction.account)
        assertEquals(25.0, transaction.amount)
    }

    @Test
    fun resolvesExplicitAccount() {
        val second = wallet.copy(id = 8L, name = "حساب آخر")
        val command = AiWriteCommandParser.parse(
            "سجل مصروف 25 دينار طعام من حساب حساب آخر",
            listOf(wallet, second),
            "LYD"
        )

        val transaction = assertIs<AiWriteCommand.RecordTransaction>(command)
        assertNotNull(transaction.account)
        assertEquals(8L, transaction.account.id)
    }

    @Test
    fun ignoresNormalQuestions() {
        assertNull(AiWriteCommandParser.parse("كم مصروفي هذا الشهر؟", listOf(wallet), "LYD"))
    }
}
