package com.example.finance.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class FinanceVoiceCommandParserTest {
    @Test fun parsesArabicRentExpenseAndArabicDigits() {
        val command = FinanceVoiceCommandParser.parseExpense("دفعت ٥٨٠ إيجار المنزل")
        assertNotNull(command)
        assertEquals(580.0, command!!.amount, 0.0)
        assertEquals("Housing", command.category)
        assertEquals("إيجار المنزل", command.description)
    }

    @Test fun parsesWesternDigits() {
        val command = FinanceVoiceCommandParser.parseExpense("دفعت 580 إيجار المنزل")
        assertNotNull(command)
        assertEquals(580.0, command!!.amount, 0.0)
        assertEquals("Housing", command.category)
    }

    @Test fun doesNotTreatQuestionsAsCommands() {
        assertNull(FinanceVoiceCommandParser.parseExpense("كم دفعت للإيجار؟"))
        assertNull(FinanceVoiceCommandParser.parseExpense("إيجار المنزل 580"))
    }
}
