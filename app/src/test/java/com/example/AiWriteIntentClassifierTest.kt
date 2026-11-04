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
}
