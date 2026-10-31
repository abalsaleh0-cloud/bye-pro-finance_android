package com.example

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.setContent
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.finance.model.AccountEntity
import com.example.finance.model.AppLanguage
import com.example.ui.screens.AddAccountForm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountsFormInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun formRequiresNameAndPersistsOpeningBalanceAndOptionalFields() {
        var saved: AccountEntity? = null
        composeRule.setContent {
            MaterialTheme {
                AddAccountForm(
                    lang = AppLanguage.AR,
                    onCancel = {},
                    onSave = { saved = it }
                )
            }
        }

        composeRule.onNodeWithTag("submit_account_btn").performClick()
        composeRule.onNodeWithText("اسم الحساب مطلوب.").assertExists()

        composeRule.onNodeWithTag("account_name_input").performTextInput("Daily wallet")
        composeRule.onNodeWithTag("account_institution_input").performTextInput("Local bank")
        composeRule.onNodeWithTag("account_last_four_input").performTextInput("1234")
        composeRule.onNodeWithTag("submit_account_btn").performClick()

        composeRule.runOnIdle {
            assertNotNull(saved)
            assertEquals("Daily wallet", saved!!.name)
            assertEquals("Local bank", saved!!.institution)
            assertEquals("1234", saved!!.accountNumberSafe)
            assertEquals(0.0, saved!!.openingBalance, 0.001)
            assertEquals(0.0, saved!!.balance, 0.001)
            assertEquals("LYD", saved!!.currency)
        }
    }

    @Test
    fun accountNameIsLimitedToEightyCharacters() {
        composeRule.setContent {
            MaterialTheme {
                AddAccountForm(lang = AppLanguage.AR, onCancel = {}, onSave = {})
            }
        }
        composeRule.onNodeWithTag("account_name_input")
            .performTextInput("x".repeat(85))
        composeRule.onNodeWithTag("account_name_input")
            .assertTextContains("x".repeat(80))
    }
}
