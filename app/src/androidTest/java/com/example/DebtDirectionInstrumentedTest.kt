package com.example

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.finance.model.AppLanguage
import com.example.finance.model.DebtEntity
import com.example.ui.screens.AddDebtDialog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DebtDirectionInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun addDebtDialogAllowsChoosingCreditorOrDebtor() {
        composeRule.setContent {
            MaterialTheme {
                AddDebtDialog(
                    lang = AppLanguage.AR,
                    onDismiss = {},
                    onConfirm = {}
                )
            }
        }

        composeRule.onNodeWithTag("debt_direction_owed_to_me").assertIsSelected()
        composeRule.onNodeWithTag("debt_direction_i_owe").assertIsNotSelected()

        composeRule.onNodeWithTag("debt_direction_i_owe").performClick()

        composeRule.onNodeWithTag("debt_direction_i_owe").assertIsSelected()
        composeRule.onNodeWithTag("debt_direction_owed_to_me").assertIsNotSelected()

        composeRule.onNodeWithTag("debt_direction_owed_to_me").performClick()

        composeRule.onNodeWithTag("debt_direction_owed_to_me").assertIsSelected()
        composeRule.onNodeWithTag("debt_direction_i_owe").assertIsNotSelected()
    }
    @Test
    fun selectedDirectionIsSavedWithNewDebt() {
        var saved: DebtEntity? = null
        composeRule.setContent {
            MaterialTheme {
                AddDebtDialog(
                    lang = AppLanguage.AR,
                    onDismiss = {},
                    onConfirm = { saved = it }
                )
            }
        }

        composeRule.onNodeWithTag("debt_name_input").performTextInput("Test debtor")
        composeRule.onNodeWithTag("debt_direction_i_owe").performClick()
        composeRule.onNodeWithTag("debt_principal_input").performTextInput("250")
        composeRule.onNodeWithTag("debt_confirm_button").performClick()

        composeRule.runOnIdle {
            assertNotNull(saved)
            assertEquals("i_owe", saved!!.direction)
        }
    }

}
