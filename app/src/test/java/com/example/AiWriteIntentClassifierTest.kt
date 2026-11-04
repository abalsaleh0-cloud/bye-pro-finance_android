package com.example

import com.example.finance.ui.AiWriteIntentClassifier
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiWriteIntentClassifierTest {
    @Test
    fun arabicExpenseCommandIsWriteIntent() {
        assertTrue(AiWriteIntentClassifier.isWriteIntent("سجل مصروف 50 دينار بنزين اليوم"))
    }

    @Test
    fun arabicExpenseLedgerRequestIsReadIntent() {
        assertFalse(AiWriteIntentClassifier.isWriteIntent("اعرض سجل المصروفات"))
    }

    @Test
    fun arabicDebtLedgerRequestIsReadIntent() {
        assertFalse(AiWriteIntentClassifier.isWriteIntent("أريد سجل الديون"))
    }

    @Test
    fun bareArabicRecordWordIsAmbiguousNotWriteIntent() {
        assertFalse(AiWriteIntentClassifier.isWriteIntent("سجل"))
    }

    @Test
    fun englishRecordExpenseCommandIsWriteIntent() {
        assertTrue(AiWriteIntentClassifier.isWriteIntent("record expense 25 for fuel"))
    }

    @Test
    fun englishHistoryRequestIsReadIntent() {
        assertFalse(AiWriteIntentClassifier.isWriteIntent("show expense history"))
    }

    @Test
    fun arabicCommandsForEveryFinanceSectionAreWriteIntents() {
        listOf(
            "سجل أصل بقيمة 2000 دينار",
            "سجل حساب نقدي",
            "سجل ميزانية شهرية",
            "سجل هدف ادخار جديد",
            "سجل راتب هذا الشهر",
            "سجل مستند فاتورة",
            "سجل حركة عمل",
            "سجل عضو أسرة",
            "سجل مصروف أسرة"
        ).forEach { command ->
            assertTrue("Expected write intent: $command", AiWriteIntentClassifier.isWriteIntent(command))
        }
    }

    @Test
    fun englishCommandsForOtherFinanceSectionsAreWriteIntents() {
        listOf(
            "record an asset worth 2000",
            "record a new account",
            "record a monthly budget",
            "record a savings goal",
            "record salary",
            "record a work record",
            "record a household member"
        ).forEach { command ->
            assertTrue("Expected write intent: $command", AiWriteIntentClassifier.isWriteIntent(command))
        }
    }

    @Test
    fun arabicInflectionsAndEnglishModifiersRemainWriteIntents() {
        listOf(
            "سجل ميزانيتي الشهرية",
            "سجل المصروفات العائلية",
            "سجل عضو عائلتي"
        ).forEach { command ->
            assertTrue("Expected write intent: $command", AiWriteIntentClassifier.isWriteIntent(command))
        }
        listOf(
            "record a new account",
            "record a monthly budget",
            "record a household member"
        ).forEach { command ->
            assertTrue("Expected write intent: $command", AiWriteIntentClassifier.isWriteIntent(command))
        }
    }

    @Test
    fun historyRequestsStayReadOnlyAfterUnicodeMatchingChange() {
        listOf(
            "اعرض سجل المصروفات",
            "أريد سجل الديون",
            "show expense history",
            "list history"
        ).forEach { request ->
            assertFalse("Expected read intent: $request", AiWriteIntentClassifier.isWriteIntent(request))
        }
    }
}
