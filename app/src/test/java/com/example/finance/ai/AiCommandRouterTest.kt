package com.example.finance.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AiCommandRouterTest {
    @Test
    fun parsesArabicFuelExpenseWithArabicIndicDigits() {
        val command = AiCommandRouter.parseTransaction("دفعت ٥٠ دينار بنزين", "LYD")
        assertEquals("EXPENSE", command?.type)
        assertEquals(50.0, command?.amount ?: 0.0, 0.0)
        assertEquals("LYD", command?.currency)
        assertEquals("fuel", command?.category)
    }

    @Test
    fun parsesEnglishIncomeAndCurrency() {
        val command = AiCommandRouter.parseTransaction("Received 125 USD salary", "LYD")
        assertEquals("INCOME", command?.type)
        assertEquals(125.0, command?.amount ?: 0.0, 0.0)
        assertEquals("USD", command?.currency)
    }

    @Test
    fun doesNotTreatHowToQuestionAsAWriteCommand() {
        assertNull(AiCommandRouter.parseTransaction("كيف أسجل مصروف بنزين 50 دينار؟", "LYD"))
    }

    @Test
    fun doesNotGuessWhenBothIncomeAndExpenseIntentsArePresent() {
        assertNull(AiCommandRouter.parseTransaction("دفعت 50 ثم استلمت 20", "LYD"))
    }

    @Test
    fun mapsArabicSectionNavigation() {
        assertEquals("debts", AiCommandRouter.parseSectionNavigation("افتح قسم الديون"))
        assertEquals("accounts", AiCommandRouter.parseSectionNavigation("اذهب إلى حساباتي"))
        assertEquals("work", AiCommandRouter.parseSectionNavigation("افتح قسم العمل"))
        assertEquals("admin", AiCommandRouter.parseSectionNavigation("افتح لوحة الإدارة"))
        assertEquals("smart_scan", AiCommandRouter.parseSectionNavigation("افتح المسح الذكي"))
        assertEquals("import_export", AiCommandRouter.parseSectionNavigation("افتح الاستيراد والتصدير"))
    }
}
