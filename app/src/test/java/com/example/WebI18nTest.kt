package com.example

import com.example.finance.i18n.WebI18n
import com.example.finance.model.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Test

class WebI18nTest {
    @Test
    fun localeContractMatchesWeb() {
        assertEquals("ar", WebI18n.locale(AppLanguage.AR))
        assertEquals("en", WebI18n.locale(AppLanguage.EN))
        assertEquals(AppLanguage.AR, WebI18n.language(null))
        assertEquals(AppLanguage.AR, WebI18n.language("ar"))
        assertEquals(AppLanguage.EN, WebI18n.language("en"))
        assertEquals("bye-pro-finance.locale", WebI18n.LOCALE_KEY)
    }

    @Test
    fun inlineTranslationChangesTextOnly() {
        val arabic = "الإعدادات"
        val english = "Settings"
        assertEquals(arabic, WebI18n.tr(arabic, english, AppLanguage.AR))
        assertEquals(english, WebI18n.tr(arabic, english, AppLanguage.EN))
        assertEquals(androidx.compose.ui.unit.LayoutDirection.Rtl, WebI18n.layoutDirection())
        assertEquals("ar", WebI18n.locale(WebI18n.language(null)))
        assertEquals("en", WebI18n.locale(WebI18n.language("en")))
    }
}
