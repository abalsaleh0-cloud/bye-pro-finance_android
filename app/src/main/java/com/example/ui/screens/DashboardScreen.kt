package com.example.ui.screens

import com.example.ui.theme.ByeProTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
    val notifications by viewModel.notifications.collectAsState()

    // Engine calculations
    val totalIncome = FinanceEngine.calculateTotalIncome(transactions)
    val totalExpenses = FinanceEngine.calculateTotalExpenses(transactions)
    val netCashFlow = FinanceEngine.calculateNetCashFlow(totalIncome, totalExpenses)
    val netWorth = FinanceEngine.calculateNetWorth(accounts, assets, debts)

    var totalLiquid = 0.0
    accounts.forEach { if (!it.isArchived) totalLiquid += it.balance }

    val budgetStatuses = budgets.map { FinanceEngine.calculateBudgetStatus(it, transactions) }
    var totalDebtAmount = BigDecimal.ZERO
    debts.forEach { totalDebtAmount = totalDebtAmount.add(BigDecimal.valueOf(it.remaining)) }

    val healthScore = FinanceEngine.calculateFinancialHealthScore(
        totalIncome = totalIncome,
        totalExpenses = totalExpenses,
        netWorth = netWorth,
        totalDebts = totalDebtAmount,
        budgetStatuses = budgetStatuses
    )

    val healthLabel = when {
        healthScore >= 80 -> Strings.get("excellent_health", lang)
        healthScore >= 60 -> Strings.get("good_health", lang)
        else -> Strings.get("warning_health", lang)
    }

    val unreadNotifications = notifications.count { !it.isRead }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Unread Alert Banner if any
            if (unreadNotifications > 0) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("notifications") }
                        .border(1.dp, ByeProTheme.colors.goldPrimary.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = ByeProTheme.colors.goldPrimary.copy(alpha = 0.12f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = ByeProTheme.colors.goldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (lang == com.example.finance.model.AppLanguage.AR)
                                "لديك $unreadNotifications تنبيهات مالية مهمة (فواتير/ميزانيات)"
                            else "You have $unreadNotifications important financial alerts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Health Score Card
            HealthScoreCard(
                score = healthScore,
                label = healthLabel,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Primary Financial Summary Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FinancialMetricCard(
                    title = Strings.get("total_balance", lang),
                    amount = FinanceEngine.formatMoney(totalLiquid, "SAR"),
                    subtitle = "${accounts.size} ${Strings.get("active_accounts", lang)}",
                    icon = Icons.Default.AccountBalance,
                    accentColor = ByeProTheme.colors.financialGreen,
                    modifier = Modifier.weight(1f)
                )
                FinancialMetricCard(
                    title = Strings.get("net_worth", lang),
                    amount = FinanceEngine.formatMoney(netWorth, "SAR"),
                    subtitle = "الأصول - الالتزامات",
                    icon = Icons.Default.AttachMoney,
                    accentColor = ByeProTheme.colors.goldPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FinancialMetricCard(
                    title = Strings.get("monthly_income", lang),
                    amount = FinanceEngine.formatMoney(totalIncome, "SAR"),
                    icon = Icons.Default.ArrowDownward,
                    accentColor = ByeProTheme.colors.financialGreen,
                    modifier = Modifier.weight(1f)
                )
                FinancialMetricCard(
                    title = Strings.get("monthly_expense", lang),
                    amount = FinanceEngine.formatMoney(totalExpenses, "SAR"),
                    icon = Icons.Default.ArrowUpward,
                    accentColor = ByeProTheme.colors.financialRed,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Quick Actions Row
        item {
            SectionHeader(title = Strings.get("quick_actions", lang))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionButton(
                    icon = Icons.Default.Add,
                    label = Strings.get("action_add_expense", lang),
                    tag = "action_add_expense_btn",
                    accentColor = ByeProTheme.colors.financialRed,
                    onClick = onOpenAddTransaction
                )
                QuickActionButton(
                    icon = Icons.Default.SwapHoriz,
                    label = Strings.get("action_transfer", lang),
                    tag = "action_transfer_btn",
                    accentColor = ByeProTheme.colors.goldPrimary,
                    onClick = { onNavigate("transfers") }
                )
                QuickActionButton(
                    icon = Icons.Default.ReceiptLong,
                    label = Strings.get("action_scan_receipt", lang),
                    tag = "action_scan_btn",
                    accentColor = ByeProTheme.colors.goldPrimary,
                    onClick = { onNavigate("smart_scan") }
                )
                QuickActionButton(
                    icon = Icons.Default.AutoAwesome,
                    label = Strings.get("nav_ai_assistant", lang),
                    tag = "action_ai_assistant_btn",
                    accentColor = ByeProTheme.colors.financialGreen,
                    onClick = { onNavigate("ai_assistant") }
                )
            }
        }

        // Cash Flow Comparison Chart
        item {
            InteractiveCashFlowChart(
                income = totalIncome.toDouble(),
                expense = totalExpenses.toDouble(),
                currency = "SAR"
            )
        }

        // Accounts Carousel
        item {
            SectionHeader(
                title = Strings.get("nav_accounts", lang),
                actionLabel = Strings.get("view_all", lang),
                onAction = { onNavigate("accounts") }
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                accounts.forEach { acc ->
                    AccountCardMini(account = acc, onClick = { onNavigate("accounts") })
                }
            }
        }

        // Recent Transactions Section
        item {
            SectionHeader(
                title = Strings.get("recent_transactions", lang),
                actionLabel = Strings.get("view_all", lang),
                onAction = { onNavigate("transactions") }
            )
        }

        val recentTxs = transactions.take(6)
        if (recentTxs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        text = "لا توجد معاملات مسجلة بعد",
                        modifier = Modifier.padding(24.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(recentTxs, key = { it.id }) { tx ->
                TransactionRowItem(
                    transaction = tx,
                    onClick = { onNavigate("transactions") }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun AccountCardMini(account: AccountEntity, onClick: () -> Unit) {
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
                text = FinanceEngine.formatMoney(account.balance, account.currency),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ByeProTheme.colors.financialGreen
            )
        }
    }
}

@Composable
fun TransactionRowItem(transaction: TransactionEntity, onClick: () -> Unit) {
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
                text = "$sign${FinanceEngine.formatMoney(transaction.amount, transaction.currency)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }
    }
}
