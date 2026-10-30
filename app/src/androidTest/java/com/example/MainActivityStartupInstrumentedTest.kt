package com.example

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs on a real Android emulator. Unlike the Robolectric unit test, this verifies
 * that MainActivity survives Android startup and that the first Compose screen renders.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityStartupInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun dashboardNavigationIsRenderedOnColdStart() {
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodes(
                androidx.compose.ui.test.hasTestTag("nav_dashboard_tab")
            ).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("nav_dashboard_tab").assertExists()
    }
}
