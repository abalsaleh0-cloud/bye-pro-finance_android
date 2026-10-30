package com.example.ui.screens

import com.example.finance.i18n.TextContent

import com.example.ui.components.FinanceAlertDialog
import com.example.ui.components.FinanceButton
import com.example.ui.components.FinanceOutlinedButton
import com.example.ui.components.FinanceTextField
import com.example.ui.components.FinanceIconButton
import com.example.ui.components.FinanceFloatingActionButton

import com.example.finance.i18n.WebI18n
import com.example.ui.theme.ByeProTheme
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.finance.engine.FinanceEngine
import com.example.finance.i18n.Strings
import com.example.finance.model.BudgetEntity
import com.example.finance.model.BudgetPeriod
import com.example.finance.ui.FinanceViewModel

@Composable
fun BudgetsScreen(
    viewModel: FinanceViewModel
) {
    val lang by viewModel.appLanguage.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val transactions by viewModel.transactions.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var budgetToDelete by remember { mutableStateOf<BudgetEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            FinanceFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = ByeProTheme.colors.goldPrimary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_budget_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = Strings.get("add_budget", lang))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = Strings.get("budgets_title", lang),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = WebI18n.tr("متابعة دقيقة لحدود الإنفاق مع تنبيهات تلقائية عند بلوغ 50%، 75%، 90%، 100%.", "Track spending limits with automatic alerts at 50%, 75%, 90%, and 100%.", lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (budgets.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(
                            text = WebI18n.tr("لا توجد ميزانيات محددة بعد. اضغط + لإنشاء ميزانية جديدة.", "No budgets yet. Tap + to create a budget.", lang),
                            modifier = Modifier.padding(32.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(budgets, key = { it.id }) { budget ->
                    val status = FinanceEngine.calculateBudgetStatus(budget, transactions)
                    BudgetItemCard(
                        budget = budget,
                        status = status,
                        lang = lang,
                        onDelete = { budgetToDelete = budget }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    if (showAddDialog) {
        AddBudgetDialog(
            lang = lang,
            onDismiss = { showAddDialog = false },
            onConfirm = { newBudget ->
                viewModel.addBudget(newBudget)
                showAddDialog = false
            }
        )
    }

    if (budgetToDelete != null) {
        val target = budgetToDelete!!
        FinanceAlertDialog(
            onDismissRequest = { budgetToDelete = null },
            title = { Text(text = Strings.get("delete", lang)) },
            text = { Text(WebI18n.tr("هل تريد حذف ميزانية", "Are you sure you want to delete budget", lang) + " '${target.name}'?") },
            confirmButton = {
                FinanceButton(
                    onClick = {
                        viewModel.deleteBudget(target)
                        budgetToDelete = null
                    },
                    modifier = Modifier.testTag("confirm_delete_budget_btn")
                ) {
                    Text(Strings.get("delete", lang))
                }
            },
            dismissButton = {
                FinanceOutlinedButton(onClick = { budgetToDelete = null }) {
                    Text(Strings.get("cancel", lang))
                }
            }
        )
    }
}

@Composable
fun BudgetItemCard(
    budget: BudgetEntity,
    status: FinanceEngine.BudgetStatus,
    lang: com.example.finance.model.AppLanguage,
    onDelete: () -> Unit
) {
    val progressFraction = (status.percentage / 100f).coerceIn(0f, 1f)
    val indicatorColor = when {
        status.isOverBudget -> ByeProTheme.colors.financialRed
        status.percentage >= 90f -> ByeProTheme.colors.financialRed
        status.percentage >= 75f -> ByeProTheme.colors.goldPrimary
        else -> ByeProTheme.colors.financialGreen
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = budget.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${budget.category} • ${budget.period.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (status.alertThreshold > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .border(1.dp, indicatorColor, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${status.percentage.toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = indicatorColor
                            )
                        }
                    }
                    FinanceIconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = WebI18n.tr("حذف", "Delete", lang),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = indicatorColor,
                trackColor = ByeProTheme.colors.borderBase.copy(alpha = 0.65f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${Strings.get("spent", lang)}: ${TextContent.money(status.spent, budget.currency, lang)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${Strings.get("limit_amount", lang)}: ${TextContent.money(status.limit, budget.currency, lang)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun AddBudgetDialog(
    lang: com.example.finance.model.AppLanguage,
    onDismiss: () -> Unit,
    onConfirm: (BudgetEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Groceries") }
    var limitStr by remember { mutableStateOf("") }

    FinanceAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = Strings.get("add_budget", lang), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FinanceTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(WebI18n.tr("اسم الميزانية", "Budget name", lang)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("budget_name_input")
                )
                FinanceTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text(Strings.get("category", lang)) },
                    modifier = Modifier.fillMaxWidth()
                )
                FinanceTextField(
                    value = limitStr,
                    onValueChange = { limitStr = it },
                    label = { Text(Strings.get("limit_amount", lang)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("budget_limit_input")
                )
            }
        },
        confirmButton = {
            FinanceButton(
                onClick = {
                    val limit = limitStr.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && limit > 0.0) {
                        onConfirm(
                            BudgetEntity(
                                name = name,
                                category = category,
                                limitAmount = limit
                            )
                        )
                    }
                },
                modifier = Modifier.testTag("submit_budget_btn")
            ) {
                Text(Strings.get("confirm", lang))
            }
        },
        dismissButton = {
            FinanceOutlinedButton(onClick = onDismiss) {
                Text(Strings.get("cancel", lang))
            }
        }
    )
}
