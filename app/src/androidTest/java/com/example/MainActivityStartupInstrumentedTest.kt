package com.example

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodes
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs on a real Android emulator and verifies that MainActivity renders its
 * initial screen. A clean CI install is expected to show authentication;
 * an install with a valid persisted session is expected to show navigation.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityStartupInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun firstScreenRendersOnColdStart() {
        composeRule.waitUntil(timeoutMillis = 30_000) {
            composeRule.onAllNodes(hasTestTag("nav_dashboard_tab")).fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodes(hasTestTag("auth_email")).fetchSemanticsNodes().isNotEmpty()
        }

        val dashboardVisible = composeRule.onAllNodes(hasTestTag("nav_dashboard_tab"))
            .fetchSemanticsNodes().isNotEmpty()
        val authenticationVisible = composeRule.onAllNodes(hasTestTag("auth_email"))
            .fetchSemanticsNodes().isNotEmpty()

        assertTrue(
            "Expected either the dashboard navigation (valid session) or login form (fresh install) to render",
            dashboardVisible || authenticationVisible
        )
        if (dashboardVisible) {
            composeRule.onNodeWithTag("nav_dashboard_tab").assertExists()
        } else {
            composeRule.onNodeWithTag("auth_email").assertExists()
        }
    }
}
