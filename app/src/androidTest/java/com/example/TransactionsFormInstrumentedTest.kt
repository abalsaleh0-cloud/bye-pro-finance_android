package com.example

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.finance.model.AccountEntity
import com.example.finance.model.AccountType
import com.example.finance.model.AppLanguage
import com.example.finance.model.TransactionType
import com.example.ui.screens.AddTransactionForm
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionsFormInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val account = AccountEntity(
        id = 10L,
        name = "Main account",
        type = AccountType.BANK,
        currency = "LYD",
        balance = 1000.0
    )

    @Test
    fun formValidatesAmountAndPersistsWebFields() {
        var saved = false
        composeRule.setContent {
            MaterialTheme {
                LazyColumn {
                    item {
                        AddTransactionForm(
                            accounts = listOf(account),
                            lang = AppLanguage.AR,
                            onCancel = {},
                            onSave = { tx ->
                                saved = true
                                assertEquals(TransactionType.EXPENSE, tx.type)
                                assertEquals(25.5, tx.amount, 0.001)
                                assertEquals("LYD", tx.currency)
                                assertEquals("Other", tx.category)
                            }
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithTag("submit_transaction_btn").performScrollTo().performClick()
        composeRule.onNodeWithText("أدخل مبلغاً أكبر من الصفر.").fetchSemanticsNode()

        composeRule.onNodeWithTag("transaction_amount_input").performScrollTo().assertHeightIsAtLeast(56.dp).performTextInput("25.5")
        composeRule.onNodeWithTag("transaction_description_input").performScrollTo().assertHeightIsAtLeast(56.dp).performTextInput("Groceries")
        composeRule.onNodeWithTag("transaction_description_input").assertTextContains("Groceries")
        composeRule.onNodeWithTag("submit_transaction_btn").performScrollTo().performClick()

        composeRule.runOnIdle { assertEquals(true, saved) }
    }

    @Test
    fun descriptionIsLimitedToTwoHundredCharacters() {
        composeRule.setContent {
            MaterialTheme {
                LazyColumn {
                    item {
                        AddTransactionForm(accounts = listOf(account), lang = AppLanguage.AR, onCancel = {}, onSave = {})
                    }
                }
            }
        }
        composeRule.onNodeWithTag("transaction_description_input").performScrollTo().performTextInput("x".repeat(240))
        composeRule.onNodeWithTag("transaction_description_input").performScrollTo().assertTextContains("x".repeat(200))
    }
}
