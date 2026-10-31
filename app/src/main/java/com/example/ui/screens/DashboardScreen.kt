package com.example.ui.screens

import com.example.ui.components.FinanceIcon

import androidx.compose.material.icons.filled.*

import androidx.compose.material.icons.Icons

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finance.i18n.TextContent
import com.example.finance.i18n.WebI18n
import com.example.finance.model.AppLanguage
import com.example.finance.model.BillEntity
import com.example.finance.model.BudgetEntity
import com.example.finance.model.DebtEntity
import com.example.finance.model.SavingsGoalEntity
import com.example.finance.model.TransactionEntity
import com.example.finance.model.TransactionStatus
import com.example.finance.model.TransactionType
import com.example.finance.ui.FinanceViewModel
import com.example.ui.theme.ByeProTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: FinanceViewModel,
    onNavigate: (String) -> Unit,
    onOpenAddTransaction: () -> Unit
) {
    val lang by viewModel.appLanguage.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val debts by viewModel.debts.collectAsState()
    val assets by viewModel.assets.collectAsState()
    val bills by viewModel.bills.collectAsState()
    val goals by viewModel.savingsGoals.collectAsState()
    val activeAccounts = accounts.filter { !it.isArchived }
    val dashboardTransactions = transactions.sortedByDescending { it.date }.take(50)
    val dashboardBills = bills.filter { !it.isPaid }.sortedBy { it.dueDate }.take(20)
    val dashboardDebts = debts.filter { it.status.name != "PAID" }.sortedBy { it.dueDate }.take(20)
    val dashboardGoals = goals.sortedBy { it.deadline }.take(20)
    val dashboardAssets = assets.sortedByDescending { it.purchaseDate }.take(20)
    val currencyOrder = buildList {
        activeAccounts.forEach { add(it.currency) }
        dashboardTransactions.forEach { add(it.currency) }
        dashboardAssets.forEach { add(it.currency) }
    }.distinct()
    val dashboardCurrency = currencyOrder.firstOrNull { it.equals("LYD", true) }
        ?: currencyOrder.firstOrNull()
        ?: "LYD"
    val currencyAccounts = activeAccounts.filter { it.currency.equals(dashboardCurrency, true) }
    val currencyTransactions = dashboardTransactions.filter { it.currency.equals(dashboardCurrency, true) && it.status != TransactionStatus.CANCELLED }
    val currencyAssets = dashboardAssets.filter { it.currency.equals(dashboardCurrency, true) }
    val currencyBudgets = budgets.filter { it.currency.equals(dashboardCurrency, true) }
    val currencyDebts = dashboardDebts.filter { it.currency.equals(dashboardCurrency, true) }
    val currencyBills = dashboardBills.filter { it.currency.equals(dashboardCurrency, true) }
        .sortedBy { it.dueDate }
    val currencyGoals = dashboardGoals.filter { it.currency.equals(dashboardCurrency, true) }

    val availableBalance = currencyAccounts.sumOf { it.balance }
    val recordedIncome = currencyTransactions.filter {
        it.type == TransactionType.INCOME
    }.sumOf { it.amount }
    val recordedExpenses = currencyTransactions.filter {
        it.type == TransactionType.EXPENSE
    }.sumOf { it.amount }
    val assetValue = currencyAssets.sumOf { it.value }
    val budgetTotal = currencyBudgets.sumOf { it.limitAmount }
    val debtRemaining = currencyDebts.sumOf { it.remaining }
    val billsDue = currencyBills.sumOf { it.amount }
    val savingsProgress = currencyGoals.sumOf { it.currentAmount }
    val savingsTarget = currencyGoals.sumOf { it.targetAmount }
    val savingsPercent = if (savingsTarget > 0.0) (savingsProgress / savingsTarget * 100).toInt().coerceIn(0, 100) else 0
    val recentTransactions = dashboardTransactions.take(6)
    val locale = if (lang == AppLanguage.AR) Locale("ar") else Locale.ENGLISH

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 2.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    WebI18n.tr("الرؤية المالية", "Financial overview", lang),
                    color = ByeProTheme.colors.goldPrimary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    WebI18n.tr("لوحة التحكم", "Dashboard", lang),
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    WebI18n.tr(
                        "ملخص حي من بيانات BYE PRO FINANCE الفعلية. الأرقام المجمعة تعرض عملة أساسية واحدة ولا تخلط العملات.",
                        "Live summary from your BYE PRO FINANCE data. Aggregates use one primary currency and never mix currencies.",
                        lang
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.width(7.dp).height(7.dp)
                            .background(ByeProTheme.colors.goldPrimary, RoundedCornerShape(50))
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        WebI18n.tr("بيانات قاعدة الجهاز", "On-device database", lang),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        item {
            DashboardMetric(
                title = WebI18n.tr("الرصيد المتاح", "Available balance", lang),
                amount = TextContent.money(availableBalance, dashboardCurrency, lang),
                note = "${activeAccounts.size} " + WebI18n.tr("حساب نشط", "active accounts", lang),
                currency = dashboardCurrency,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            DashboardMetric(
                title = WebI18n.tr("الدخل المسجل", "Recorded income", lang),
                amount = TextContent.money(recordedIncome, dashboardCurrency, lang),
                note = WebI18n.tr("من المعاملات المكتملة", "From completed transactions", lang),
                modifier = Modifier.fillMaxWidth(),
                accent = ByeProTheme.colors.financialGreen
            )
        }
        item {
            DashboardMetric(
                title = WebI18n.tr("المصروفات المسجلة", "Recorded expenses", lang),
                amount = TextContent.money(recordedExpenses, dashboardCurrency, lang),
                note = WebI18n.tr("من المعاملات المكتملة", "From completed transactions", lang),
                modifier = Modifier.fillMaxWidth(),
                accent = ByeProTheme.colors.financialRed
            )
        }
        item {
            DashboardMetric(
                title = WebI18n.tr("قيمة الأصول", "Asset value", lang),
                amount = TextContent.money(assetValue, dashboardCurrency, lang),
                note = "${dashboardAssets.size} " + WebI18n.tr("أصل", "assets", lang),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            DashboardPanel(
                title = WebI18n.tr("المعاملات الأخيرة", "Recent transactions", lang),
                eyebrow = WebI18n.tr("آخر النشاط", "Recent activity", lang),
                count = recentTransactions.size,
                onClick = { onNavigate("transactions") }
            ) {
                if (recentTransactions.isEmpty()) {
                    DashboardEmpty(WebI18n.tr("لا توجد معاملات بعد.", "No transactions yet.", lang))
                } else {
                    recentTransactions.forEach { tx ->
                        DashboardTransactionLine(tx, lang, onClick = { onNavigate("transactions") })
                    }
                }
            }
        }

        item {
            DashboardPanel(
                title = WebI18n.tr("الفواتير", "Bills", lang),
                eyebrow = WebI18n.tr("التزامات قريبة", "Upcoming obligations", lang),
                count = dashboardBills.size,
                onClick = { onNavigate("bills") }
            ) {
                if (currencyBills.isEmpty()) {
                    DashboardEmpty(WebI18n.tr("لا توجد فواتير مستحقة.", "No bills due.", lang))
                } else {
                    dashboardBills.take(5).forEach { bill ->
                        DashboardLine(
                            title = bill.name,
                            subtitle = "${dashboardDate(bill.dueDate, locale)} · " +
                                WebI18n.tr("قادمة", "Upcoming", lang),
                            amount = TextContent.money(bill.amount, bill.currency, lang)
                        )
                    }
                }
            }
        }

        item {
            DashboardPanel(
                title = WebI18n.tr("الميزانيات", "Budgets", lang),
                eyebrow = WebI18n.tr("التخطيط", "Planning", lang),
                count = budgets.size,
                onClick = { onNavigate("budgets") }
            ) {
                Text(
                    TextContent.money(budgetTotal, dashboardCurrency, lang),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    WebI18n.tr("إجمالي المخصص النشط", "Total active allocation", lang) + " · $dashboardCurrency",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
                budgets.take(4).forEach { budget ->
                    DashboardLine(
                        title = budget.name,
                        subtitle = budgetPeriodLabel(budget, lang),
                        amount = TextContent.money(budget.limitAmount, budget.currency, lang)
                    )
                }
                if (currencyBudgets.isEmpty()) DashboardEmpty(WebI18n.tr("لا توجد ميزانيات نشطة.", "No active budgets.", lang))
            }
        }

        item {
            DashboardPanel(
                title = WebI18n.tr("الادخار", "Savings", lang),
                eyebrow = WebI18n.tr("الأهداف", "Goals", lang),
                count = dashboardGoals.size,
                onClick = { onNavigate("savings") }
            ) {
                LinearProgressIndicator(
                    progress = { savingsPercent / 100f },
                    modifier = Modifier.fillMaxWidth().height(7.dp),
                    color = ByeProTheme.colors.goldPrimary,
                    trackColor = ByeProTheme.colors.borderBase
                )
                Text(
                    "$savingsPercent%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "${TextContent.money(savingsProgress, dashboardCurrency, lang)} " +
                        WebI18n.tr("من", "of", lang) + " " +
                        TextContent.money(savingsTarget, dashboardCurrency, lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            DashboardPanel(
                title = WebI18n.tr("الديون", "Debts", lang),
                eyebrow = WebI18n.tr("الالتزامات", "Liabilities", lang),
                count = dashboardDebts.size,
                onClick = { onNavigate("debts") }
            ) {
                Text(
                    TextContent.money(debtRemaining, dashboardCurrency, lang),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    WebI18n.tr("إجمالي المتبقي", "Total remaining", lang) + " · $dashboardCurrency",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                dashboardDebts.take(3).forEach { debt ->
                    DashboardLine(
                        title = debt.name,
                        subtitle = if (debt.direction == "owed_to_me")
                            WebI18n.tr("لي عنده", "Owed to me", lang)
                        else WebI18n.tr("عليّ له", "I owe", lang),
                        amount = TextContent.money(debt.remaining, debt.currency, lang)
                    )
                }
                if (currencyDebts.isEmpty()) DashboardEmpty(WebI18n.tr("لا توجد ديون نشطة.", "No active debts.", lang))
            }
        }

        item {
            DashboardPanel(
                title = WebI18n.tr("ملخص اليوم", "Today at a glance", lang),
                eyebrow = WebI18n.tr("الاستحقاقات", "Due items", lang),
                count = null
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    DashboardQuickStat(activeAccounts.size, WebI18n.tr("حسابات", "Accounts", lang), Modifier.weight(1f))
                    DashboardQuickStat(dashboardTransactions.size, WebI18n.tr("معاملات", "Transactions", lang), Modifier.weight(1f))
                    DashboardQuickStat(dashboardBills.size, WebI18n.tr("الفواتير", "Bills", lang), Modifier.weight(1f))
                    DashboardQuickStat(dashboardDebts.size, WebI18n.tr("الديون", "Debts", lang), Modifier.weight(1f))
                }
                Text(
                    WebI18n.tr("الفواتير القادمة:", "Upcoming bills:", lang) +
                        " " + TextContent.money(billsDue, dashboardCurrency, lang) + " · $dashboardCurrency",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun DashboardMetric(
    title: String,
    amount: String,
    note: String,
    modifier: Modifier = Modifier,
    currency: String? = null,
    accent: Color = ByeProTheme.colors.goldPrimary
) {
    Card(
        modifier = modifier.border(1.dp, ByeProTheme.colors.borderBase, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(title + (currency?.let { " · $it" } ?: ""), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(amount, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(note, style = MaterialTheme.typography.labelSmall, color = accent)
        }
    }
}

@Composable
private fun DashboardPanel(
    title: String,
    eyebrow: String,
    count: Int?,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
            .border(1.dp, ByeProTheme.colors.borderBase, RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(eyebrow, style = MaterialTheme.typography.labelSmall, color = ByeProTheme.colors.goldPrimary)
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                if (count != null) Text(count.toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            content()
        }
    }
}

@Composable
private fun DashboardLine(title: String, subtitle: String, amount: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        Text(amount, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun DashboardTransactionLine(transaction: TransactionEntity, lang: AppLanguage, onClick: () -> Unit) {
    val isExpense = transaction.type == TransactionType.EXPENSE
    val sign = if (isExpense) "−" else "+"
    val color = if (isExpense) ByeProTheme.colors.financialRed else ByeProTheme.colors.financialGreen
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                transaction.description.ifBlank { WebI18n.tr("معاملة مالية", "Financial transaction", lang) },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(dashboardDate(transaction.date, if (lang == AppLanguage.AR) Locale("ar") else Locale.ENGLISH), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            "$sign${TextContent.money(transaction.amount, transaction.currency, lang)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun DashboardQuickStat(value: Int, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.background(ByeProTheme.colors.cardSecondary, RoundedCornerShape(9.dp)).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(value.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DashboardEmpty(message: String) {
    Text(message, modifier = Modifier.padding(vertical = 6.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private fun dashboardDate(timestamp: Long, locale: Locale): String =
    SimpleDateFormat("dd MMM yyyy", locale).format(Date(timestamp))

private fun budgetPeriodLabel(budget: BudgetEntity, lang: AppLanguage): String = when (budget.period.name) {
    "WEEKLY" -> WebI18n.tr("أسبوعية", "Weekly", lang)
    "MONTHLY" -> WebI18n.tr("شهرية", "Monthly", lang)
    "QUARTERLY" -> WebI18n.tr("ربع سنوية", "Quarterly", lang)
    "YEARLY" -> WebI18n.tr("سنوية", "Yearly", lang)
    else -> WebI18n.tr("مخصصة", "Custom", lang)
}
