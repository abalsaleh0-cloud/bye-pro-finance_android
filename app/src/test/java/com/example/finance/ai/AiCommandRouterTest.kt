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
    fun classifiesColloquialGroceriesAndMobileCreditExpenses() {
        val groceries = AiCommandRouter.parseTransaction("سجل مصروف ٥٠ دينار مشتريات خضار", "LYD")
        assertEquals("groceries", groceries?.category)
        assertEquals("سجل مصروف 50 دينار مشتريات خضار", groceries?.description)

        val mobile = AiCommandRouter.parseTransaction("صرفت ١٠ دينار شحن رصيد", "LYD")
        assertEquals("mobile_credit", mobile?.category)
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
    fun parsesNamedDebtCreationAndDirection() {
        val owed = AiCommandRouter.parseDebtCreate("سجل دين علي له ٥٠ دينار أحمد", "LYD")
        assertEquals(50.0, owed?.amount ?: 0.0, 0.0)
        assertEquals("أحمد", owed?.personName)
        assertEquals("i_owe", owed?.direction)

        val receivable = AiCommandRouter.parseDebtCreate("سجل دين لي عند أحمد ١٠٠ دينار", "LYD")
        assertEquals("أحمد", receivable?.personName)
        assertEquals("owed_to_me", receivable?.direction)
    }

    @Test
    fun parsesColloquialDebtPaymentWithoutRequiringAccount() {
        val command = AiCommandRouter.parseDebtPayment("دفعت ٢٠ دينار لأحمد")
        assertEquals(20.0, command?.amount ?: 0.0, 0.0)
        assertEquals("أحمد", command?.personName)
        assertEquals("LYD", command?.currency)
    }

    @Test
    fun parsesSalaryAdvanceAndSalaryPaymentAsWorkRecords() {
        val advance = AiCommandRouter.parseWorkRecord("أخذت سلفة ٣٠٠ دينار من الشركة", "LYD")
        assertEquals("SALARY_ADVANCE", advance?.type)
        assertEquals(300.0, advance?.amount ?: 0.0, 0.0)
        assertEquals("الشركة", advance?.sourceName)

        val salary = AiCommandRouter.parseWorkRecord("استلمت الراتب ٢٠٠٠ دينار", "LYD")
        assertEquals("SALARY_PAYMENT", salary?.type)
        assertEquals(2000.0, salary?.amount ?: 0.0, 0.0)
    }

    @Test
    fun parsesMonthlyRentInstallmentAndPayment() {
        val unpaid = AiCommandRouter.parseRentCommand("سجل إيجار المنزل ٥٠٠ دينار لشهر 2026-11", "LYD", "2026-11")
        assertEquals(false, unpaid?.isPayment)
        assertEquals("2026-11", unpaid?.month)
        assertEquals(500.0, unpaid?.amount ?: 0.0, 0.0)

        val paid = AiCommandRouter.parseRentCommand("دفعت إيجار المنزل ٥٠٠ دينار", "LYD", "2026-11")
        assertEquals(true, paid?.isPayment)
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
