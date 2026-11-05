package com.example

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
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

    private fun saveWithDirection(direction: String): DebtEntity? {
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

        composeRule.onNodeWithTag("debt_name_input").performTextInput("اختبار الدين")
        composeRule.onNodeWithTag("debt_principal_input").performTextInput("125")
        composeRule.onNodeWithTag("debt_direction_$direction").performClick()
        composeRule.onNodeWithTag("submit_debt_btn").performClick()
        composeRule.waitForIdle()
        return saved
    }

    @Test
    fun selectingOwedToMePersistsCreditorDirection() {
        val saved = saveWithDirection("owed_to_me")
        assertNotNull(saved)
        assertEquals("owed_to_me", saved!!.direction)
    }

    @Test
    fun selectingIOwePersistsDebtorDirection() {
        val saved = saveWithDirection("i_owe")
        assertNotNull(saved)
        assertEquals("i_owe", saved!!.direction)
    }
}
