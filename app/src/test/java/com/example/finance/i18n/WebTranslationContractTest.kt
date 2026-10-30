package com.example.finance.i18n

import com.example.finance.model.AppLanguage
import com.example.ui.theme.ByeProDesignTokens
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WebTranslationContractTest {
    @Test
    fun webLocaleContractSupportsOnlyArabicAndEnglish() {
        assertEquals("ar", WebTranslationContract.locale(AppLanguage.AR))
        assertEquals("en", WebTranslationContract.locale(AppLanguage.EN))
        assertEquals(AppLanguage.AR, WebTranslationContract.language("unknown"))
    }

    @Test
    fun stableWebTranslationsMatchExactArabicAndEnglishMessages() {
        val expected = mapOf(
            "dashboard" to ("لوحة التحكم" to "Dashboard"),
            "accounts" to ("الحسابات" to "Accounts"),
            "transactions" to ("المعاملات" to "Transactions"),
            "settings" to ("الإعدادات" to "Settings")
        )
        expected.forEach { (key, translations) ->
            assertEquals(translations.first, WebTranslationContract.text(key, AppLanguage.AR))
            assertEquals(translations.second, WebTranslationContract.text(key, AppLanguage.EN))
        }
    }

    @Test
    fun androidDesignTokensMatchWebTokens() {
        assertEquals(Color(0xFF101114), ByeProDesignTokens.Colors.background)
        assertEquals(Color(0xFF181A1F), ByeProDesignTokens.Colors.surface)
        assertEquals(Color(0xFF1E1F24), ByeProDesignTokens.Colors.surfaceRaised)
        assertEquals(Color(0xFF2B2D34), ByeProDesignTokens.Colors.border)
        assertEquals(Color(0xFFF1F0EB), ByeProDesignTokens.Colors.text)
        assertEquals(Color(0xFFA5A7B0), ByeProDesignTokens.Colors.muted)
        assertEquals(Color(0xFFD6B56D), ByeProDesignTokens.Colors.accent)
        assertEquals(Color(0xFF67D6A1), ByeProDesignTokens.Colors.success)
        assertEquals(Color(0xFFFF8585), ByeProDesignTokens.Colors.danger)
        assertEquals(ByeProDesignTokens.Colors.background, com.example.ui.theme.DarkBgMain)
        assertEquals(ByeProDesignTokens.Colors.surface, com.example.ui.theme.DarkSurfaceMain)
        assertEquals(ByeProDesignTokens.Colors.surfaceRaised, com.example.ui.theme.DarkCardMain)
        assertEquals(ByeProDesignTokens.Colors.border, com.example.ui.theme.DarkBorderBase)
        assertEquals(ByeProDesignTokens.Colors.text, com.example.ui.theme.DarkTextPrimary)
        assertEquals(ByeProDesignTokens.Colors.muted, com.example.ui.theme.DarkTextMuted)
        assertEquals(ByeProDesignTokens.Colors.accent, com.example.ui.theme.GoldPrimary)
        assertEquals(ByeProDesignTokens.Colors.success, com.example.ui.theme.FinancialGreen)
        assertEquals(ByeProDesignTokens.Colors.danger, com.example.ui.theme.FinancialRed)
        assertEquals(1440.dp, ByeProDesignTokens.Control.contentMaxWidth)
        assertEquals(1536.dp, ByeProDesignTokens.Breakpoints.xxxl)
    }

    @Test
    fun arabicAndEnglishDictionariesHaveCompleteMatchingKeys() {
        assertEquals(Strings.arStrings.keys, Strings.enStrings.keys)
        Strings.arStrings.forEach { (key, value) ->
            assertTrue("Arabic translation is blank for $key", value.isNotBlank())
            assertTrue("English translation is blank for $key", Strings.enStrings[key]?.isNotBlank() == true)
        }
    }

    @Test
    fun visualDirectionIsAlwaysRtl() {
        assertEquals(androidx.compose.ui.unit.LayoutDirection.Rtl, WebTranslationContract.layoutDirection())
        assertTrue(WebTranslationContract.stableWebKeys.containsAll(setOf("dashboard", "accounts", "transactions", "settings")))
    }
}
