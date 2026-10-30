package com.example.finance.i18n

import com.example.finance.model.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextContentTest {
    @Test fun arabicAndEnglishCataloguesHaveTheSameKeys() {
        assertEquals(Strings.arStrings.keys, Strings.enStrings.keys)
    }

    @Test fun placeholderInterpolationIsLocaleAwareAndKeepsUnknownTokens() {
        assertEquals("Welcome, ⁨Sam⁩", TextContent.text("welcome_user", AppLanguage.EN, mapOf("name" to "Sam")))
        assertEquals("مرحباً ⁨سارة⁩", TextContent.text("welcome_user", AppLanguage.AR, mapOf("name" to "سارة")))
        assertEquals("{missing}", TextContent.text("missing_key", AppLanguage.EN, mapOf("value" to 1)))
    }

    @Test fun arabicPluralCategoriesFollowArabicRules() {
        assertEquals("zero", TextContent.pluralCategory(0, AppLanguage.AR))
        assertEquals("one", TextContent.pluralCategory(1, AppLanguage.AR))
        assertEquals("two", TextContent.pluralCategory(2, AppLanguage.AR))
        assertEquals("few", TextContent.pluralCategory(7, AppLanguage.AR))
        assertEquals("many", TextContent.pluralCategory(15, AppLanguage.AR))
        assertEquals("other", TextContent.pluralCategory(101, AppLanguage.AR))
    }

    @Test fun englishPluralCategoriesFollowEnglishRules() {
        assertEquals("one", TextContent.pluralCategory(1, AppLanguage.EN))
        assertEquals("other", TextContent.pluralCategory(2, AppLanguage.EN))
    }

    @Test fun dynamicTextIsWrappedForMixedDirectionSafety() {
        assertTrue(TextContent.isolate("ABC-123").startsWith("\u2068"))
        assertTrue(TextContent.isolate("ABC-123").endsWith("\u2069"))
    }

    @Test fun financialFormattingReturnsNonEmptyValues() {
        assertTrue(TextContent.number(1234.5, AppLanguage.EN).isNotBlank())
        assertTrue(TextContent.money(12.5, "USD", AppLanguage.EN).isNotBlank())
        assertTrue(TextContent.percent(0.125, AppLanguage.EN).isNotBlank())
    }
}
