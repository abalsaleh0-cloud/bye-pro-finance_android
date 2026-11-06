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
    fun parsesExpenseWhenAmountPrecedesExpenseWord() {
        val command = AiCommandRouter.parseTransaction("سجل 10 دينار مصروف", "LYD")
        assertEquals("EXPENSE", command?.type)
        assertEquals(10.0, command?.amount ?: 0.0, 0.0)
        assertEquals("LYD", command?.currency)
        assertEquals("other", command?.category)
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
    fun parsesShortArabicDebtPaymentWithBarePersonName() {
        val command = AiCommandRouter.parseDebtPayment("سجل دفعة 10 دينار خالد")
        assertEquals(10.0, command?.amount ?: 0.0, 0.0)
        assertEquals("خالد", command?.personName)
        assertEquals("LYD", command?.currency)

        val secondName = AiCommandRouter.parseDebtPayment("سجل دفعة 25 دينار محمود")
        assertEquals("محمود", secondName?.personName)
    }

    @Test
    fun doesNotMisclassifyStoreOrGroceryPaymentsAsDebtPayments() {
        assertNull(AiCommandRouter.parseDebtPayment("دفعت 50 دينار للبقالة"))
        assertNull(AiCommandRouter.parseDebtPayment("paid 50 USD for groceries"))

        val arabicExpense = AiCommandRouter.parseTransaction("دفعت 50 دينار للبقالة", "LYD")
        assertEquals("EXPENSE", arabicExpense?.type)
        assertEquals("groceries", arabicExpense?.category)

        val englishExpense = AiCommandRouter.parseTransaction("paid 50 USD for groceries", "LYD")
        assertEquals("EXPENSE", englishExpense?.type)
        assertEquals("groceries", englishExpense?.category)
    }

    @Test
    fun parsesDebtPaymentWithPrepositionAndKeepsWorkPaymentSeparate() {
        val debtPayment = AiCommandRouter.parseDebtPayment("دفعة ١٠ دينار لخالد")
        assertEquals("خالد", debtPayment?.personName)
        assertNull(AiCommandRouter.parseDebtPayment("سجل 600 دينار دفعة من العمل"))
        val english = AiCommandRouter.parseDebtPayment("paid 10 USD to Khaled")
        assertEquals("Khaled", english?.personName)
    }

    @Test
    fun parsesWorkPaymentDescribedAsInstallmentFromWork() {
        val command = AiCommandRouter.parseWorkRecord("سجل 600 دينار دفعة من العمل", "LYD")
        assertEquals("SALARY_PAYMENT", command?.type)
        assertEquals(600.0, command?.amount ?: 0.0, 0.0)
        assertEquals(null, command?.sourceName)
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
    fun routesColloquialWorkPaymentsWithoutMisclassifyingThemAsDebtPayments() {
        listOf(
            "سجل العمل 600 دينار دفعة",
            "سجل 600 دينار دفعه من العمل"
        ).forEach { query ->
            val work = AiCommandRouter.parseWorkRecord(query, "LYD")
            assertEquals("SALARY_PAYMENT", work?.type)
            assertEquals(600.0, work?.amount ?: 0.0, 0.0)
            assertNull(AiCommandRouter.parseDebtPayment(query))
        }
    }

    @Test
    fun parsesDebtCreationWhenAmountPrecedesDebtDescription() {
        val command = AiCommandRouter.parseDebtCreate("سجل 20 دينار دين عليه له احمد", "LYD")
        assertEquals(20.0, command?.amount ?: 0.0, 0.0)
        assertEquals("احمد", command?.personName)
        assertEquals("i_owe", command?.direction)
        assertEquals("LYD", command?.currency)
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
    fun recognizesPartialRentPaymentAsPartialNotFullBillSettlement() {
        val partial = AiCommandRouter.parseRentCommand("ال 200 هذه كسور ايجار بيت سجل 200 دينار دفعه", "LYD", "2026-11")
        assertEquals(true, partial?.isPayment)
        assertEquals(true, partial?.isPartialPayment)
        assertEquals(200.0, partial?.amount ?: 0.0, 0.0)
    }

    @Test
    fun recognizesColloquialWorkPaymentWordOrder() {
        val command = AiCommandRouter.parseWorkRecord("سجل 700 دينار العمل دفعه", "LYD")
        assertEquals("SALARY_PAYMENT", command?.type)
        assertEquals(700.0, command?.amount ?: 0.0, 0.0)
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
