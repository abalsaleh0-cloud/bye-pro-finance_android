package com.example.finance.ai

import com.example.finance.model.AccountEntity
import com.example.finance.model.AppLanguage
import com.example.finance.model.AssetEntity
import com.example.finance.model.BillEntity
import com.example.finance.model.BudgetEntity
import com.example.finance.model.BudgetPeriod
import com.example.finance.model.DebtEntity
import com.example.finance.model.SavingsGoalEntity
import com.example.finance.model.TransactionEntity
import com.example.finance.model.TransactionStatus
import com.example.finance.model.TransactionType
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

/**
 * Deterministic, records-only assistant matching the Web assistant's safe baseline.
 * It never invents financial values, never converts currencies and never mutates data.
 */
object FinancialAssistantEngine {
    private const val MAX_CATEGORIES = 10
    private const val MAX_BUDGETS = 10

    fun answer(
        query: String,
        accounts: List<AccountEntity>,
        transactions: List<TransactionEntity>,
        budgets: List<BudgetEntity>,
        debts: List<DebtEntity>,
        savingsGoals: List<SavingsGoalEntity>,
        assets: List<AssetEntity>,
        bills: List<BillEntity>,
        language: AppLanguage,
        now: Long = System.currentTimeMillis()
    ): String {
        val arabic = language == AppLanguage.AR
        val q = query.trim().lowercase(Locale.ROOT)
        val wantsSpending = has(q, "مصروف", "انفاق", "إنفاق", "صرف", "spend", "expense", "expenses", "spending", "cost")
        val wantsIncome = has(q, "دخل", "راتب", "income", "salary", "revenue")
        val wantsBudget = has(q, "ميزاني", "budget", "budgets", "تجاوز")
        val wantsSave = has(q, "ادخار", "توفير", "وفر", "أوفر", "وفّر", "save", "saving", "savings", "reduce")
        val wantsCategory = has(q, "فئة", "تصنيف", "أكثر شيء", "أعلى مصروف", "category", "categories", "top", "largest", "highest")
        val wantsDebt = has(q, "دين", "ديون", "دائن", "مدين", "debt", "debts", "owe", "owed")
        val wantsBill = has(q, "فاتورة", "فواتير", "bill", "bills")
        val wantsSavingsGoal = has(q, "هدف ادخار", "أهداف الادخار", "savings goal", "savings goals")
        val wantsAssets = has(q, "أصل", "أصول", "asset", "assets", "صافي الثروة", "net worth")
        val wantsOverview = has(q, "حلل", "تحليل", "وضعي", "مالي", "مالية", "finance", "financial", "analyze", "analysis", "overview", "summary", "cash flow", "تدفق")
        val wantsHelp = has(q, "ماذا تستطيع", "كيف تستخدم", "مساعدة", "help", "what can you do", "capabilities")
        val recognized = wantsSpending || wantsIncome || wantsBudget || wantsSave || wantsCategory || wantsDebt || wantsBill || wantsSavingsGoal || wantsAssets || wantsOverview || wantsHelp

        val completed = transactions.filter { it.status != TransactionStatus.CANCELLED && it.amount.isFinite() && it.amount >= 0.0 }
        val lines = mutableListOf<String>()
        if (arabic) {
            lines += "مساعد BYE PRO FINANCE — تحليل مالي حتمي يعتمد على سجلاتك فقط، ولا ينفّذ أي عملية مالية."
            if (!recognized) {
                lines += "أستطيع تحليل المصروفات والدخل والفئات والميزانيات والديون والفواتير وأهداف الادخار والأصول. اسألني سؤالًا محددًا عن بياناتك."
                return lines.joinToString("\n")
            }
            if (wantsHelp) lines += "أستطيع تلخيص الدخل والمصروفات لكل عملة، ترتيب أعلى فئات الإنفاق، فحص الميزانيات، ومراجعة الديون والفواتير وأهداف الادخار والأصول."
            if (wantsSpending || wantsCategory) {
                lines += "تحليل المصروفات المكتملة حسب الفئة والعملة:"
                val groups = completed.filter { it.type == TransactionType.EXPENSE && inCurrentMonth(it.date, now) }
                    .groupBy { "${it.currency.trim().uppercase(Locale.ROOT)}::${it.category.ifBlank { "غير مصنف" }}" }
                    .map { (_, rows) -> Triple(rows.first().category.ifBlank { "غير مصنف" }, rows.first().currency.uppercase(Locale.ROOT), rows.sumOf { it.amount }) }
                    .sortedByDescending { it.third }
                if (groups.isEmpty()) lines += "لا توجد مصروفات مكتملة في الشهر الحالي ضمن البيانات المتاحة."
                groups.take(MAX_CATEGORIES).forEach { lines += "• ${it.first} — ${money(it.third, it.second)}." }
            }
            if (wantsIncome || wantsOverview) {
                lines += "ملخص التدفق المالي المكتمل لكل عملة:"
                completed.groupBy { it.currency.trim().uppercase(Locale.ROOT).ifBlank { "UNKNOWN" } }.forEach { (currency, rows) ->
                    val income = rows.filter { it.type == TransactionType.INCOME || it.type == TransactionType.REFUND }.sumOf { it.amount }
                    val expense = rows.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                    val net = income - expense
                    lines += "• $currency: الدخل ${money(income, currency)}، المصروفات ${money(expense, currency)}، الصافي ${money(net, currency)}."
                    if (net < 0) lines += "  تنبيه: المصروفات المسجلة أعلى من الدخل المسجل بعملة $currency."
                    else if (income > 0 && expense / income >= 0.8) lines += "  تنبيه: المصروفات تستهلك 80% أو أكثر من الدخل المسجل بعملة $currency."
                }
                if (completed.isEmpty()) lines += "لا توجد معاملات مكتملة كافية لإظهار التدفق المالي."
            }
            if (wantsBudget) appendBudgets(lines, budgets, completed, arabic)
            if (wantsDebt) {
                val active = debts.filter { it.remaining > 0 }
                lines += "الديون القائمة: ${active.size}."
                active.groupBy { it.currency.uppercase(Locale.ROOT) }.forEach { (currency, rows) ->
                    lines += "• $currency — المتبقي ${money(rows.sumOf { it.remaining }, currency)}."
                }
            }
            if (wantsBill) {
                val unpaid = bills.filter { !it.isPaid }
                lines += "الفواتير غير المدفوعة: ${unpaid.size}."
                unpaid.sortedBy { it.dueDate }.take(MAX_BUDGETS).forEach { lines += "• ${it.name}: ${money(it.amount, it.currency.uppercase(Locale.ROOT))}." }
            }
            if (wantsSavingsGoal) {
                lines += "أهداف الادخار: ${savingsGoals.size}."
                savingsGoals.take(MAX_BUDGETS).forEach { goal ->
                    val progress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount * 100).coerceIn(0.0, 100.0) else 0.0
                    lines += "• ${goal.name}: ${money(goal.currentAmount, goal.currency.uppercase(Locale.ROOT))} من ${money(goal.targetAmount, goal.currency.uppercase(Locale.ROOT))} — ${"%.1f".format(Locale.US, progress)}%."
                }
            }
            if (wantsAssets) {
                lines += "الأصول المسجلة: ${assets.size}."
                assets.groupBy { it.currency.uppercase(Locale.ROOT) }.forEach { (currency, rows) -> lines += "• $currency — ${money(rows.sumOf { it.value }, currency)}." }
            }
            if (wantsSave) {
                val top = completed.filter { it.type == TransactionType.EXPENSE && inCurrentMonth(it.date, now) }
                    .groupBy { it.category.ifBlank { "غير مصنف" } }.mapValues { (_, rows) -> rows.sumOf { it.amount } }.maxByOrNull { it.value }
                lines += "خطوة عملية للادخار: راجع الفئة الأعلى إنفاقًا، خفّض المصروفات المتكررة غير الضرورية، وحدد مبلغ ادخار ثابت بعد الدخل."
                if (top != null) lines += "ابدأ بمراجعة فئة ${top.key} لأنها الأعلى إنفاقًا في الشهر الحالي ضمن البيانات المتاحة."
            }
        } else {
            lines += "BYE PRO FINANCE Assistant — deterministic financial analysis using your records only; it cannot execute financial operations."
            if (!recognized) {
                lines += "I can analyze spending, income, categories, budgets, debts, bills, savings goals and assets. Ask a specific question about your records."
                return lines.joinToString("\n")
            }
            if (wantsHelp) lines += "I can summarize income and expenses by currency, rank spending categories, review budgets, and inspect debts, bills, savings goals and assets."
            if (wantsSpending || wantsCategory) {
                lines += "Completed current-month expenses by category and currency:"
                val groups = completed.filter { it.type == TransactionType.EXPENSE && inCurrentMonth(it.date, now) }
                    .groupBy { "${it.currency.trim().uppercase(Locale.ROOT)}::${it.category.ifBlank { "Uncategorized" }}" }
                    .map { (_, rows) -> Triple(rows.first().category.ifBlank { "Uncategorized" }, rows.first().currency.uppercase(Locale.ROOT), rows.sumOf { it.amount }) }
                    .sortedByDescending { it.third }
                if (groups.isEmpty()) lines += "There are no completed current-month expenses in the available records."
                groups.take(MAX_CATEGORIES).forEach { lines += "• ${it.first} — ${money(it.third, it.second)}." }
            }
            if (wantsIncome || wantsOverview) {
                lines += "Completed cash flow by currency:"
                completed.groupBy { it.currency.trim().uppercase(Locale.ROOT).ifBlank { "UNKNOWN" } }.forEach { (currency, rows) ->
                    val income = rows.filter { it.type == TransactionType.INCOME || it.type == TransactionType.REFUND }.sumOf { it.amount }
                    val expense = rows.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                    val net = income - expense
                    lines += "• $currency: income ${money(income, currency)}, expenses ${money(expense, currency)}, net ${money(net, currency)}."
                    if (net < 0) lines += "  Alert: recorded expenses exceed recorded income in $currency."
                    else if (income > 0 && expense / income >= 0.8) lines += "  Alert: recorded expenses use at least 80% of recorded income in $currency."
                }
                if (completed.isEmpty()) lines += "There are not enough completed transactions to show cash flow."
            }
            if (wantsBudget) appendBudgets(lines, budgets, completed, arabic)
            if (wantsDebt) {
                val active = debts.filter { it.remaining > 0 }
                lines += "Outstanding debts: ${active.size}."
                active.groupBy { it.currency.uppercase(Locale.ROOT) }.forEach { (currency, rows) -> lines += "• $currency — remaining ${money(rows.sumOf { it.remaining }, currency)}." }
            }
            if (wantsBill) {
                val unpaid = bills.filter { !it.isPaid }
                lines += "Unpaid bills: ${unpaid.size}."
                unpaid.sortedBy { it.dueDate }.take(MAX_BUDGETS).forEach { lines += "• ${it.name}: ${money(it.amount, it.currency.uppercase(Locale.ROOT))}." }
            }
            if (wantsSavingsGoal) {
                lines += "Savings goals: ${savingsGoals.size}."
                savingsGoals.take(MAX_BUDGETS).forEach { goal ->
                    val progress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount * 100).coerceIn(0.0, 100.0) else 0.0
                    lines += "• ${goal.name}: ${money(goal.currentAmount, goal.currency.uppercase(Locale.ROOT))} of ${money(goal.targetAmount, goal.currency.uppercase(Locale.ROOT))} — ${"%.1f".format(Locale.US, progress)}%."
                }
            }
            if (wantsAssets) {
                lines += "Recorded assets: ${assets.size}."
                assets.groupBy { it.currency.uppercase(Locale.ROOT) }.forEach { (currency, rows) -> lines += "• $currency — ${money(rows.sumOf { it.value }, currency)}." }
            }
            if (wantsSave) {
                val top = completed.filter { it.type == TransactionType.EXPENSE && inCurrentMonth(it.date, now) }
                    .groupBy { it.category.ifBlank { "Uncategorized" } }.mapValues { (_, rows) -> rows.sumOf { it.amount } }.maxByOrNull { it.value }
                lines += "Practical saving step: review the highest-spending category, reduce unnecessary recurring expenses, and set a fixed saving amount after income arrives."
                if (top != null) lines += "Start with ${top.key}, the highest-spending category in the available current-month records."
            }
        }
        lines += if (arabic) "هذا تحليل استرشادي مبني على السجلات المتاحة فقط، وليس استشارة مالية مرخصة." else "This is analytical guidance based only on available records, not licensed financial advice."
        return lines.joinToString("\n")
    }

    private fun appendBudgets(lines: MutableList<String>, budgets: List<BudgetEntity>, transactions: List<TransactionEntity>, arabic: Boolean) {
        if (budgets.isEmpty()) {
            lines += if (arabic) "لا توجد ميزانيات مسجلة." else "No budgets are recorded."
            return
        }
        lines += if (arabic) "حالة الميزانيات:" else "Budget status:"
        budgets.take(MAX_BUDGETS).forEach { budget ->
            val spent = transactions.asSequence()
                .filter { it.type == TransactionType.EXPENSE && it.currency.equals(budget.currency, true) && it.date >= budget.startDate && it.date <= budget.endDate && (budget.category.isBlank() || it.category.equals(budget.category, true)) }
                .sumOf { it.amount }
            val percent = if (budget.limitAmount > 0) spent / budget.limitAmount * 100 else 0.0
            val suffix = if (arabic) "${money(spent, budget.currency.uppercase(Locale.ROOT))} من ${money(budget.limitAmount, budget.currency.uppercase(Locale.ROOT))} — ${"%.1f".format(Locale.US, percent)}%" else "${money(spent, budget.currency.uppercase(Locale.ROOT))} of ${money(budget.limitAmount, budget.currency.uppercase(Locale.ROOT))} — ${"%.1f".format(Locale.US, percent)}%"
            lines += "• ${budget.name}: $suffix${if (percent > 100) if (arabic) " — متجاوزة" else " — exceeded" else ""}."
        }
    }

    private fun has(q: String, vararg terms: String): Boolean = terms.any { q.contains(it.lowercase(Locale.ROOT)) }

    private fun inCurrentMonth(timestamp: Long, now: Long): Boolean {
        val a = Calendar.getInstance().apply { timeInMillis = timestamp }
        val b = Calendar.getInstance().apply { timeInMillis = now }
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.MONTH) == b.get(Calendar.MONTH)
    }

    private fun money(value: Double, currency: String): String {
        val format = NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 2; minimumFractionDigits = 0 }
        return "${format.format(value)} $currency"
    }
}
