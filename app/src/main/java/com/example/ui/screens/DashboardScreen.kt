package com.example.ui.screens

import com.example.finance.i18n.TextContent

import com.example.ui.components.FinanceAlertDialog
import com.example.ui.components.FinanceButton
import com.example.ui.components.FinanceOutlinedButton
import com.example.ui.components.FinanceTextField

import com.example.finance.i18n.WebI18n
import com.example.ui.theme.ByeProTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finance.engine.FinanceEngine
import com.example.finance.i18n.Strings
import com.example.finance.model.AccountEntity
import com.example.finance.model.BudgetPeriod
import com.example.finance.model.TransactionStatus
import com.example.finance.model.TransactionEntity
import com.example.finance.model.TransactionType
import com.example.finance.ui.FinanceViewModel
import com.example.ui.components.FinancialMetricCard
import com.example.ui.components.HealthScoreCard
import com.example.ui.components.InteractiveCashFlowChart
import com.example.ui.components.QuickActionButton
import com.example.ui.components.SectionHeader
import java.math.BigDecimal
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
    val savingsGoals by viewModel.savingsGoals.collectAsState()

    // Mirror the web dashboard currency rule: prefer LYD, otherwise the first
    // currency present in account, transaction, or asset records.
    val availableCurrencies = (accounts.map { it.currency } +
        transactions.map { it.currency } + assets.map { it.currency })
        .map { it.trim().uppercase(Locale.ROOT) }
        .filter { it.isNotBlank() }
        .distinct()
    val currency = if ("LYD" in availableCurrencies) "LYD" else availableCurrencies.firstOrNull() ?: "LYD"

    val currencyAccounts = accounts.filter { it.currency.equals(currency, ignoreCase = true) }
    val completedTransactions = transactions.filter {
        it.currency.equals(currency, ignoreCase = true) && it.status == TransactionStatus.COMPLETED
    }
    val currencyAssets = assets.filter { it.currency.equals(currency, ignoreCase = true) }
    val currencyBudgets = budgets.filter { it.currency.equals(currency, ignoreCase = true) }
    val currencyDebts = debts.filter { it.currency.equals(currency, ignoreCase = true) && it.remaining > 0.0 }
    val currencyBills = bills.filter { it.currency.equals(currency, ignoreCase = true) }
    val currencyGoals = savingsGoals.filter { it.currency.equals(currency, ignoreCase = true) }

    val availableBalance = currencyAccounts.sumOf { it.balance }
    val recordedIncome = completedTransactions
        .filter { it.type == TransactionType.INCOME }
        .sumOf { it.amount }
    val recordedExpenses = completedTransactions
        .filter { it.type == TransactionType.EXPENSE }
        .sumOf { it.amount }
    val assetValue = currencyAssets.sumOf { it.value }
    val budgetTotal = currencyBudgets.sumOf { it.limitAmount }
    val debtRemaining = currencyDebts.sumOf { it.remaining }
    val billsDue = currencyBills.sumOf { it.amount }
    val goalProgress = currencyGoals.sumOf { it.currentAmount }
    val goalTarget = currencyGoals.sumOf { it.targetAmount }
    val goalPercent = if (goalTarget > 0.0) (goalProgress / goalTarget * 100.0).toInt().coerceIn(0, 100) else 0
    val activeDebts = debts.filter { it.remaining > 0.0 }
    val outstandingDebtCount = activeDebts.size

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    WebI18n.tr("الرؤية المالية", "Financial overview", lang),
                    style = MaterialTheme.typography.labelMedium,
                    color = ByeProTheme.colors.goldPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    Strings.get("dashboard", lang),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    WebI18n.tr(
                        "ملخص حي من بيانات BYE PRO FINANCE الفعلية. الأرقام المجمعة تعرض عملة أساسية واحدة ولا تخلط العملات.",
                        "Live summary from your BYE PRO FINANCE data. Aggregates use one primary currency and never mix currencies.",
                        lang
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            DashboardMetric(
                label = WebI18n.tr("الرصيد المتاح", "Available balance", lang) + " · $currency",
                value = TextContent.money(availableBalance, currency, lang),
                detail = "${TextContent.number(accounts.size, lang, 0)} " + WebI18n.tr("حساب نشط", "active accounts", lang)
            )
        }
        item {
            DashboardMetric(
                label = WebI18n.tr("الدخل المسجل", "Recorded income", lang) + " · $currency",
                value = TextContent.money(recordedIncome, currency, lang),
                detail = WebI18n.tr("من المعاملات المكتملة", "From completed transactions", lang)
            )
        }
        item {
            DashboardMetric(
                label = WebI18n.tr("المصروفات المسجلة", "Recorded expenses", lang) + " · $currency",
                value = TextContent.money(recordedExpenses, currency, lang),
                detail = WebI18n.tr("من المعاملات المكتملة", "From completed transactions", lang)
            )
        }
        item {
            DashboardMetric(
                label = WebI18n.tr("قيمة الأصول", "Asset value", lang) + " · $currency",
                value = TextContent.money(assetValue, currency, lang),
                detail = "${TextContent.number(assets.size, lang, 0)} " + WebI18n.tr("أصل", "assets", lang)
            )
        }

        item {
            DashboardPanel(
                eyebrow = WebI18n.tr("آخر النشاط", "Recent activity", lang),
                title = WebI18n.tr("المعاملات الأخيرة", "Recent transactions", lang),
                count = TextContent.number(transactions.size, lang, 0)
            ) {
                val recent = transactions.take(6)
                if (recent.isEmpty()) {
                    DashboardEmpty(WebI18n.tr("لا توجد معاملات بعد.", "No transactions yet.", lang))
                } else {
                    recent.forEachIndexed { index, tx ->
                        val fallback = when (tx.type) {
                            TransactionType.INCOME, TransactionType.REFUND -> WebI18n.tr("دخل", "Income", lang)
                            TransactionType.EXPENSE -> WebI18n.tr("مصروف", "Expense", lang)
                            TransactionType.TRANSFER -> WebI18n.tr("تحويل", "Transfer", lang)
                            TransactionType.ADJUSTMENT -> WebI18n.tr("تسوية", "Adjustment", lang)
                        }
                        DashboardLine(
                            label = tx.description.ifBlank { fallback },
                            detail = WebI18n.date(Date(tx.date), lang),
                            value = (if (tx.type == TransactionType.EXPENSE) "−" else "+") +
                                TextContent.money(tx.amount, tx.currency, lang),
                            valueColor = if (tx.type == TransactionType.EXPENSE) ByeProTheme.colors.financialRed else ByeProTheme.colors.financialGreen,
                            showDivider = index != recent.lastIndex
                        )
                    }
                }
            }
        }

        item {
            DashboardPanel(
                eyebrow = WebI18n.tr("التزامات قريبة", "Upcoming obligations", lang),
                title = WebI18n.tr("الفواتير", "Bills", lang),
                count = TextContent.number(bills.size, lang, 0)
            ) {
                val upcoming = bills.take(5)
                if (upcoming.isEmpty()) {
                    DashboardEmpty(WebI18n.tr("لا توجد فواتير مستحقة.", "No bills due.", lang))
                } else {
                    upcoming.forEachIndexed { index, bill ->
                        val status = when {
                            bill.isPaid -> WebI18n.tr("مدفوعة", "Paid", lang)
                            bill.dueDate < System.currentTimeMillis() -> WebI18n.tr("متأخرة", "Overdue", lang)
                            else -> WebI18n.tr("قادمة", "Upcoming", lang)
                        }
                        DashboardLine(
                            label = bill.name,
                            detail = WebI18n.date(Date(bill.dueDate), lang) + " · " + status,
                            value = TextContent.money(bill.amount, bill.currency, lang),
                            showDivider = index != upcoming.lastIndex
                        )
                    }
                }
            }
        }

        item {
            DashboardPanel(
                eyebrow = WebI18n.tr("التخطيط", "Planning", lang),
                title = WebI18n.tr("الميزانيات", "Budgets", lang),
                count = TextContent.number(budgets.size, lang, 0)
            ) {
                DashboardLine(
                    label = WebI18n.tr("إجمالي المخصص النشط", "Total active allocation", lang) + " · $currency",
                    detail = "",
                    value = TextContent.money(budgetTotal, currency, lang),
                    valueColor = ByeProTheme.colors.goldPrimary,
                    showDivider = currencyBudgets.isNotEmpty()
                )
                budgets.take(4).forEachIndexed { index, budget ->
                    DashboardLine(
                        label = budget.name,
                        detail = budgetPeriodLabel(budget.period, lang),
                        value = TextContent.money(budget.limitAmount, budget.currency, lang),
                        showDivider = index != budgets.take(4).lastIndex
                    )
                }
                if (currencyBudgets.isEmpty()) DashboardEmpty(WebI18n.tr("لا توجد ميزانيات بهذه العملة.", "No budgets in this currency.", lang))
            }
        }

        item {
            DashboardPanel(
                eyebrow = WebI18n.tr("الأهداف", "Goals", lang),
                title = WebI18n.tr("الادخار", "Savings", lang),
                count = TextContent.number(savingsGoals.size, lang, 0)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(goalPercent / 100f).height(8.dp)
                            .clip(CircleShape).background(ByeProTheme.colors.goldPrimary)
                    )
                }
                Text("$goalPercent%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    TextContent.money(goalProgress, currency, lang) + " " +
                        WebI18n.tr("من", "of", lang) + " " + TextContent.money(goalTarget, currency, lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (currencyGoals.isEmpty()) DashboardEmpty(WebI18n.tr("لا توجد أهداف ادخار بهذه العملة.", "No savings goals in this currency.", lang))
            }
        }

        item {
            DashboardPanel(
                eyebrow = WebI18n.tr("الالتزامات", "Liabilities", lang),
                title = WebI18n.tr("الديون", "Debts", lang),
                count = TextContent.number(outstandingDebtCount, lang, 0)
            ) {
                DashboardLine(
                    label = WebI18n.tr("إجمالي المتبقي", "Total remaining", lang) + " · $currency",
                    detail = "",
                    value = TextContent.money(debtRemaining, currency, lang),
                    valueColor = ByeProTheme.colors.financialRed,
                    showDivider = currencyDebts.isNotEmpty()
                )
                activeDebts.take(3).forEachIndexed { index, debt ->
                    DashboardLine(
                        label = debt.name,
                        detail = if (debt.direction == "owed_to_me") WebI18n.tr("لي عنده", "Owed to me", lang)
                            else WebI18n.tr("عليّ له", "I owe", lang),
                        value = TextContent.money(debt.remaining, debt.currency, lang),
                        showDivider = index != activeDebts.take(3).lastIndex
                    )
                }
                if (currencyDebts.isEmpty()) DashboardEmpty(WebI18n.tr("لا توجد ديون قائمة بهذه العملة.", "No outstanding debts in this currency.", lang))
            }
        }

        item {
            DashboardPanel(
                eyebrow = WebI18n.tr("الاستحقاقات", "Due items", lang),
                title = WebI18n.tr("ملخص اليوم", "Today at a glance", lang),
                count = "LIVE"
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    DashboardQuickStat(TextContent.number(accounts.size, lang, 0), WebI18n.tr("حسابات", "Accounts", lang), Modifier.weight(1f))
                    DashboardQuickStat(TextContent.number(transactions.size, lang, 0), WebI18n.tr("معاملات", "Transactions", lang), Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    DashboardQuickStat(TextContent.number(bills.size, lang, 0), WebI18n.tr("الفواتير", "Bills", lang), Modifier.weight(1f))
                    DashboardQuickStat(TextContent.number(outstandingDebtCount, lang, 0), WebI18n.tr("الديون", "Debts", lang), Modifier.weight(1f))
                }
                Text(
                    WebI18n.tr("الفواتير القادمة:", "Upcoming bills:", lang) + " " + TextContent.money(billsDue, currency, lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun DashboardMetric(label: String, value: String, detail: String) {
    Card(
        modifier = Modifier.fillMaxWidth().heightIn(min = 132.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DashboardPanel(
    eyebrow: String,
    title: String,
    count: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(eyebrow, style = MaterialTheme.typography.labelSmall, color = ByeProTheme.colors.goldPrimary)
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Surface(shape = RoundedCornerShape(9.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text(count, modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp), color = ByeProTheme.colors.goldPrimary, style = MaterialTheme.typography.labelSmall)
                }
            }
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)))
            content()
        }
    }
}

@Composable
private fun DashboardLine(
    label: String,
    detail: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    showDivider: Boolean = true
) {
    Column(modifier = Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = valueColor)
        }
        if (showDivider) Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)))
    }
}

@Composable
private fun DashboardEmpty(message: String) {
    Text(message, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun DashboardQuickStat(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun budgetPeriodLabel(period: BudgetPeriod, lang: com.example.finance.model.AppLanguage): String =
    when (period) {
        BudgetPeriod.WEEKLY -> WebI18n.tr("أسبوعية", "Weekly", lang)
        BudgetPeriod.MONTHLY -> WebI18n.tr("شهرية", "Monthly", lang)
        BudgetPeriod.QUARTERLY -> WebI18n.tr("ربع سنوية", "Quarterly", lang)
        BudgetPeriod.YEARLY -> WebI18n.tr("سنوية", "Yearly", lang)
        BudgetPeriod.CUSTOM -> WebI18n.tr("مخصصة", "Custom", lang)
    }

@Composable
fun AccountCardMini(account: AccountEntity, lang: com.example.finance.model.AppLanguage, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .clickable(onClick = onClick)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            try {
                                Color(android.graphics.Color.parseColor(account.colorHex))
                            } catch (_: Exception) {
                                ByeProTheme.colors.financialGreen
                            }
                        )
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = account.accountNumberSafe.ifBlank { account.type.name },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = TextContent.money(account.balance, account.currency, lang),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ByeProTheme.colors.financialGreen
            )
        }
    }
}

@Composable
fun TransactionRowItem(transaction: TransactionEntity, lang: com.example.finance.model.AppLanguage, onClick: () -> Unit) {
    val isIncome = transaction.type == TransactionType.INCOME || transaction.type == TransactionType.REFUND
    val accentColor = if (isIncome) ByeProTheme.colors.financialGreen else ByeProTheme.colors.financialRed
    val sign = if (isIncome) "+" else "-"

    val dateFormatted = try {
        SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(transaction.date))
    } catch (_: Exception) {
        ""
    }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = transaction.description,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${transaction.category} • $dateFormatted",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "$sign${TextContent.money(transaction.amount, transaction.currency, lang)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }
    }
}
