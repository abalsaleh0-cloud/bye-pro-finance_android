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
        assertEquals(Color(0xFF2A0F2E), ByeProDesignTokens.Colors.background)
        assertEquals(Color(0xFF170918), ByeProDesignTokens.Colors.surface)
        assertEquals(Color(0xFF4A1A45), ByeProDesignTokens.Colors.surfaceRaised)
        assertEquals(Color(0x66F6ECF5), ByeProDesignTokens.Colors.border)
        assertEquals(Color(0xFFF6ECF5), ByeProDesignTokens.Colors.text)
        assertEquals(Color(0xFFCDB8CC), ByeProDesignTokens.Colors.muted)
        assertEquals(Color(0xFFF58BD6), ByeProDesignTokens.Colors.accent)
        assertEquals(Color(0xFFA8EFC0), ByeProDesignTokens.Colors.success)
        assertEquals(Color(0xFFFFB3BD), ByeProDesignTokens.Colors.coral)
        assertEquals(ByeProDesignTokens.Colors.background, com.example.ui.theme.DarkBgMain)
        assertEquals(ByeProDesignTokens.Colors.surface, com.example.ui.theme.DarkSurfaceMain)
        assertEquals(ByeProDesignTokens.Colors.surfaceRaised, com.example.ui.theme.DarkCardMain)
        assertEquals(ByeProDesignTokens.Colors.border, com.example.ui.theme.DarkBorderBase)
        assertEquals(ByeProDesignTokens.Colors.text, com.example.ui.theme.DarkTextPrimary)
        assertEquals(ByeProDesignTokens.Colors.muted, com.example.ui.theme.DarkTextMuted)
        assertEquals(ByeProDesignTokens.Colors.accent, com.example.ui.theme.GoldPrimary)
        assertEquals(ByeProDesignTokens.Colors.success, com.example.ui.theme.FinancialGreen)
        assertEquals(ByeProDesignTokens.Colors.coral, com.example.ui.theme.FinancialRed)
        assertEquals(1440.dp, ByeProDesignTokens.Control.contentMaxWidth)
        assertEquals(1536.dp, ByeProDesignTokens.Breakpoints.xxxl)
        assertEquals(Color(0xFFFBF5FA), ByeProDesignTokens.LightColors.background)
        assertEquals(Color(0xFFF6E3F3), ByeProDesignTokens.LightColors.surface)
        assertEquals(Color(0xFFFFFFFF), ByeProDesignTokens.LightColors.surfaceRaised)
        assertEquals(Color(0x336B4A69), ByeProDesignTokens.LightColors.border)
        assertEquals(Color(0xFF1F0B22), ByeProDesignTokens.LightColors.text)
        assertEquals(Color(0xFF6B4A69), ByeProDesignTokens.LightColors.muted)
        assertEquals(Color(0xFF9C1F86), ByeProDesignTokens.LightColors.accent)
        assertEquals(Color(0xFF9C1F86), ByeProDesignTokens.LightColors.accent)
        assertEquals(Color(0xFF15803D), ByeProDesignTokens.LightColors.success)
        assertEquals(Color(0xFFBE123C), ByeProDesignTokens.LightColors.expense)
        assertEquals(Color(0xFF170918), ByeProDesignTokens.Fields.darkBackground)
        assertEquals(Color(0x66F6ECF5), ByeProDesignTokens.Fields.darkBorder)
        assertEquals(Color(0x336B4A69), ByeProDesignTokens.Fields.lightBorder)
        assertEquals(Color(0xFFF58BD6), ByeProDesignTokens.Fields.focus)
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
