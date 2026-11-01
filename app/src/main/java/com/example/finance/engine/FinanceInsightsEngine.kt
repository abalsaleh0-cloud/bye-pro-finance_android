package com.example.finance.engine

import com.example.finance.model.AppLanguage
import com.example.finance.model.BudgetEntity
import com.example.finance.model.TransactionEntity
import com.example.finance.model.TransactionStatus
import com.example.finance.model.TransactionType

data class FinanceInsight(
    val title: String,
    val description: String,
    val impactType: String,
    val estimatedSavings: Double = 0.0
)

/** Deterministic, auditable insights calculated only from recorded finance rows. */
object FinanceInsightsEngine {
    fun generateInsights(
        transactions: List<TransactionEntity>,
        budgets: List<BudgetEntity>,
        language: AppLanguage
    ): List<FinanceInsight> {
        val completedExpenses = transactions.filter {
            it.status == TransactionStatus.COMPLETED &&
                it.type == TransactionType.EXPENSE &&
                it.amount.isFinite() && it.amount > 0.0
        }
        if (completedExpenses.isEmpty() && budgets.isEmpty()) {
            return listOf(
                FinanceInsight(
                    if (language == AppLanguage.AR) "لا توجد مؤشرات كافية بعد" else "Not enough data yet",
                    if (language == AppLanguage.AR) "أضف معاملات وميزانيات فعلية لعرض تحليلات مالية موثوقة." else "Add recorded transactions and budgets to see reliable financial insights.",
                    "TIP"
                )
            )
        }

        val insights = mutableListOf<FinanceInsight>()
        budgets.forEach { budget ->
            val spent = completedExpenses.asSequence()
                .filter {
                    it.category.equals(budget.category, ignoreCase = true) &&
                        it.currency.equals(budget.currency, ignoreCase = true) &&
                        it.date in budget.startDate..budget.endDate
                }
                .sumOf { it.amount }
            if (budget.limitAmount > 0.0 && budget.limitAmount.isFinite()) {
                val percent = (spent / budget.limitAmount * 100.0).toInt()
                if (percent >= 90) {
                    insights += FinanceInsight(
                        if (language == AppLanguage.AR) "اقتراب تجاوز الميزانية" else "Budget nearly exceeded",
                        if (language == AppLanguage.AR)
                            "استهلكت ${percent}% من ميزانية ${budget.name} (${spent} ${budget.currency} من ${budget.limitAmount})."
                        else
                            "${percent}% of ${budget.name} has been used (${spent} ${budget.currency} of ${budget.limitAmount}).",
                        "WARNING"
                    )
                } else if (percent >= 70) {
                    insights += FinanceInsight(
                        if (language == AppLanguage.AR) "راقب إنفاقك" else "Keep an eye on spending",
                        if (language == AppLanguage.AR)
                            "وصل إنفاق ${budget.name} إلى ${percent}% من الحد المحدد."
                        else
                            "${budget.name} has reached ${percent}% of its limit.",
                        "TIP"
                    )
                }
            }
        }

        val topCategory = completedExpenses.groupBy { it.category to it.currency }
            .mapValues { (_, rows) -> rows.sumOf { it.amount } }
            .maxByOrNull { it.value }
        if (topCategory != null) {
            insights += FinanceInsight(
                if (language == AppLanguage.AR) "أعلى تصنيف إنفاق" else "Top spending category",
                if (language == AppLanguage.AR)
                    "أعلى إنفاق مسجل هو ${topCategory.key.first}: ${topCategory.value} ${topCategory.key.second}."
                else
                    "Your highest recorded expense category is ${topCategory.key.first}: ${topCategory.value} ${topCategory.key.second}.",
                "INFO"
            )
        }
        return insights.ifEmpty {
            listOf(
                FinanceInsight(
                    if (language == AppLanguage.AR) "وضعك ضمن الحدود المسجلة" else "Within recorded limits",
                    if (language == AppLanguage.AR) "لا توجد ميزانيات قريبة من التجاوز وفق البيانات المسجلة." else "No budgets are close to their limits based on recorded data.",
                    "POSITIVE"
                )
            )
        }
    }
}
