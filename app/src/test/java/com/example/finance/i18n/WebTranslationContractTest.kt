package com.example.finance.i18n

import com.example.finance.model.AppLanguage
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
    fun visualDirectionIsAlwaysRtl() {
        assertEquals(androidx.compose.ui.unit.LayoutDirection.Rtl, WebTranslationContract.layoutDirection())
        assertTrue(WebTranslationContract.stableWebKeys.containsAll(setOf("dashboard", "accounts", "transactions", "settings")))
    }
}
