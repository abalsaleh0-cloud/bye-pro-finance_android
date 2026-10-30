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
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.finance.model.AssetEntity
import com.example.finance.model.AssetType
import com.example.finance.model.AppLanguage
import com.example.finance.model.SavingsGoalEntity
import com.example.finance.ui.FinanceViewModel

@Composable
fun SavingsAssetsScreen(
    viewModel: FinanceViewModel,
    initialTabIndex: Int = 0
) {
    val lang by viewModel.appLanguage.collectAsState()
    val savingsGoals by viewModel.savingsGoals.collectAsState()
    val assets by viewModel.assets.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val debts by viewModel.debts.collectAsState()
    val primaryCurrency by viewModel.primaryCurrency.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(initialTabIndex.coerceIn(0, 1)) }
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var showAddAssetDialog by remember { mutableStateOf(false) }
    var goalToContribute by remember { mutableStateOf<SavingsGoalEntity?>(null) }

    val netWorth = FinanceEngine.calculateNetWorth(accounts, assets, debts, primaryCurrency)

    Scaffold(
        floatingActionButton = {
            FinanceFloatingActionButton(
                onClick = {
                    if (selectedTabIndex == 0) showAddGoalDialog = true else showAddAssetDialog = true
                },
                containerColor = ByeProTheme.colors.goldPrimary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_savings_asset_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = ByeProTheme.colors.financialGreen
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text(Strings.get("savings_title", lang), fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text(Strings.get("assets_title", lang), fontWeight = FontWeight.Bold) }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    if (selectedTabIndex == 1) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, ByeProTheme.colors.goldPrimary.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = Strings.get("net_worth", lang),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ByeProTheme.colors.goldPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = TextContent.money(netWorth, primaryCurrency, lang),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ByeProTheme.colors.financialGreen
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = WebI18n.tr("إجمالي الأصول المقومة ناقصاً الالتزامات والديون", "Total valued assets minus liabilities and debts", lang),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (selectedTabIndex == 0) {
                    if (savingsGoals.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Text(
                                    text = WebI18n.tr("لا توجد أهداف ادخارية حالياً. ابدأ بإنشاء هدفك الآن!", "No savings goals yet. Create your first goal now!", lang),
                                    modifier = Modifier.padding(24.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(savingsGoals, key = { it.id }) { goal ->
                            val progress = FinanceEngine.calculateSavingsProgress(
                                targetAmount = goal.targetAmount,
                                currentAmount = goal.currentAmount,
                                deadlineTimestamp = goal.deadline
                            )

                            SavingsGoalCard(
                                goal = goal,
                                progress = progress,
                                lang = lang,
                                onContribute = { goalToContribute = goal },
                                onDelete = { viewModel.deleteSavingsGoal(goal) }
                            )
                        }
                    }
                } else {
                    if (assets.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Text(
                                    text = WebI18n.tr("سجل الأصول فارغ. أضف عقاراتك، الذهب، والمركبات لحساب صافي الثروة بدقة.", "Your asset register is empty. Add property, gold, and vehicles to calculate net worth.", lang),
                                    modifier = Modifier.padding(24.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(assets, key = { it.id }) { asset ->
                            AssetCardItem(
                                asset = asset,
                                lang = lang,
                                onDelete = { viewModel.deleteAsset(asset) }
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }
    }

    // Contribute dialog
    if (goalToContribute != null) {
        val target = goalToContribute!!
        var contributeAmountStr by remember { mutableStateOf("") }

        FinanceAlertDialog(
            onDismissRequest = { goalToContribute = null },
            title = { Text(WebI18n.tr("إيداع في الهدف:", "Contribute to goal:", lang) + " ${target.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(WebI18n.tr("المبلغ الحالي:", "Current amount:", lang) + " ${TextContent.money(target.currentAmount, target.currency, lang)}")
                    FinanceTextField(
                        value = contributeAmountStr,
                        onValueChange = { contributeAmountStr = it },
                        label = { Text(WebI18n.tr("مبلغ الإيداع", "Deposit amount", lang)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                FinanceButton(
                    onClick = {
                        val amt = contributeAmountStr.toDoubleOrNull() ?: 0.0
                        if (amt > 0.0) {
                            val defaultAcc = accounts.firstOrNull()?.id ?: 1L
                            viewModel.contributeToSavings(target.id, amt, defaultAcc)
                            goalToContribute = null
                        }
                    }
                ) {
                    Text(Strings.get("confirm", lang))
                }
            },
            dismissButton = {
                FinanceOutlinedButton(onClick = { goalToContribute = null }) {
                    Text(Strings.get("cancel", lang))
                }
            }
        )
    }

    // Add Goal Dialog
    if (showAddGoalDialog) {
        AddSavingsGoalDialog(
            lang = lang,
            onDismiss = { showAddGoalDialog = false },
            onConfirm = { newGoal ->
                viewModel.addSavingsGoal(newGoal)
                showAddGoalDialog = false
            }
        )
    }

    // Add Asset Dialog
    if (showAddAssetDialog) {
        AddAssetDialog(
            lang = lang,
            onDismiss = { showAddAssetDialog = false },
            onConfirm = { newAsset ->
                viewModel.addAsset(newAsset)
                showAddAssetDialog = false
            }
        )
    }
}

@Composable
fun SavingsGoalCard(
    goal: SavingsGoalEntity,
    progress: FinanceEngine.SavingsProgress,
    lang: com.example.finance.model.AppLanguage,
    onContribute: () -> Unit,
    onDelete: () -> Unit
) {
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
                        text = goal.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = WebI18n.tr("النسبة المحققة:", "Progress:", lang) + " ${TextContent.number(progress.percentage.toInt(), lang, 0)}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = ByeProTheme.colors.financialGreen,
                        fontWeight = FontWeight.SemiBold
                    )
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

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { progress.percentage / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = ByeProTheme.colors.financialGreen,
                trackColor = ByeProTheme.colors.borderBase.copy(alpha = 0.65f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = WebI18n.tr("المدخر:", "Saved:", lang) + " ${TextContent.money(progress.current, goal.currency, lang)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = WebI18n.tr("الهدف:", "Target:", lang) + " ${TextContent.money(progress.target, goal.currency, lang)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = WebI18n.tr("مطلوب شهرياً:", "Required monthly:", lang) + " ${TextContent.money(progress.requiredMonthlySaving, goal.currency, lang)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ByeProTheme.colors.goldPrimary
                )
                FinanceButton(
                    onClick = onContribute,
                    colors = ButtonDefaults.buttonColors(containerColor = ByeProTheme.colors.goldPrimary, contentColor = MaterialTheme.colorScheme.onPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Savings, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(Strings.get("contribute", lang), modifier = Modifier.padding(start = 4.dp), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun AssetCardItem(
    asset: AssetEntity,
    lang: AppLanguage,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = asset.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${asset.type.name} • ${asset.notes.ifBlank { WebI18n.tr("مؤكد", "Verified", lang) }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = TextContent.money(asset.value, asset.currency, lang),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = ByeProTheme.colors.goldPrimary
                )
                Spacer(modifier = Modifier.size(8.dp))
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
    }
}

@Composable
fun AddSavingsGoalDialog(
    lang: com.example.finance.model.AppLanguage,
    onDismiss: () -> Unit,
    onConfirm: (SavingsGoalEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var targetStr by remember { mutableStateOf("") }
    var currentStr by remember { mutableStateOf("0") }

    FinanceAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = Strings.get("add_goal", lang), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FinanceTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(WebI18n.tr("اسم الهدف الادخاري", "Savings goal name", lang)) },
                    modifier = Modifier.fillMaxWidth()
                )
                FinanceTextField(
                    value = targetStr,
                    onValueChange = { targetStr = it },
                    label = { Text(Strings.get("target_amount", lang)) },
                    modifier = Modifier.fillMaxWidth()
                )
                FinanceTextField(
                    value = currentStr,
                    onValueChange = { currentStr = it },
                    label = { Text(WebI18n.tr("الرصيد المدخر حالياً", "Current saved balance", lang)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            FinanceButton(
                onClick = {
                    val target = targetStr.toDoubleOrNull() ?: 0.0
                    val current = currentStr.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && target > 0.0) {
                        onConfirm(
                            SavingsGoalEntity(
                                name = name,
                                targetAmount = target,
                                currentAmount = current,
                                deadline = System.currentTimeMillis() + (180L * 24 * 60 * 60 * 1000)
                            )
                        )
                    }
                }
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

@Composable
fun AddAssetDialog(
    lang: com.example.finance.model.AppLanguage,
    onDismiss: () -> Unit,
    onConfirm: (AssetEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var valueStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    FinanceAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = Strings.get("add_asset", lang), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FinanceTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(WebI18n.tr("اسم الأصل (عقار، ذهب، مركبة)", "Asset name (property, gold, vehicle)", lang)) },
                    modifier = Modifier.fillMaxWidth()
                )
                FinanceTextField(
                    value = valueStr,
                    onValueChange = { valueStr = it },
                    label = { Text(Strings.get("asset_value", lang)) },
                    modifier = Modifier.fillMaxWidth()
                )
                FinanceTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(WebI18n.tr("ملاحظات", "Notes", lang)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            FinanceButton(
                onClick = {
                    val value = valueStr.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && value > 0.0) {
                        onConfirm(
                            AssetEntity(
                                name = name,
                                type = AssetType.PROPERTY,
                                value = value,
                                notes = notes
                            )
                        )
                    }
                }
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
