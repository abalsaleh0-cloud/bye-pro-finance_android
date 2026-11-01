package com.example.finance.ai

import com.example.finance.model.AppLanguage
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialAssistantEngineTest {
    @Test
    fun unknownQuestionDoesNotInventFinancialData() {
        val answer = FinancialAssistantEngine.answer(
            query = "ما سعر الذهب اليوم؟",
            accounts = emptyList(),
            transactions = emptyList(),
            budgets = emptyList(),
            debts = emptyList(),
            savingsGoals = emptyList(),
            assets = emptyList(),
            bills = emptyList(),
            language = AppLanguage.AR
        )
        assertTrue(answer.contains("أستطيع تحليل"))
        assertTrue(!answer.contains("سعر الذهب"))
    }

    @Test
    fun supportedBudgetQuestionWorksWithoutProviderKey() {
        val answer = FinancialAssistantEngine.answer(
            query = "ما حالة الميزانيات؟",
            accounts = emptyList(),
            transactions = emptyList(),
            budgets = emptyList(),
            debts = emptyList(),
            savingsGoals = emptyList(),
            assets = emptyList(),
            bills = emptyList(),
            language = AppLanguage.AR
        )
        assertTrue(answer.contains("لا توجد ميزانيات"))
    }

    @Test
    fun englishModeChangesTextOnly() {
        val answer = FinancialAssistantEngine.answer(
            query = "What can you do?",
            accounts = emptyList(),
            transactions = emptyList(),
            budgets = emptyList(),
            debts = emptyList(),
            savingsGoals = emptyList(),
            assets = emptyList(),
            bills = emptyList(),
            language = AppLanguage.EN
        )
        assertTrue(answer.contains("I can summarize"))
        assertTrue(!answer.contains("أستطيع"))
    }
}
