package com.example.ui.screens

import com.example.ui.components.FinanceIcon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*


import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*


import androidx.compose.material.icons.filled.*

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
import androidx.compose.material.icons.filled.Edit
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
    var editingGoal by remember { mutableStateOf<SavingsGoalEntity?>(null) }
    var editingAsset by remember { mutableStateOf<AssetEntity?>(null) }
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
                FinanceIcon(imageVector = Icons.Default.Add, contentDescription = null)
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
                                onEdit = { editingGoal = goal },
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
                                onEdit = { editingAsset = asset },
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
    if (showAddGoalDialog || editingGoal != null) {
        AddSavingsGoalDialog(
            lang = lang,
            initialGoal = editingGoal,
            onDismiss = { showAddGoalDialog = false; editingGoal = null },
            onConfirm = { newGoal ->
                if (editingGoal == null) viewModel.addSavingsGoal(newGoal) else viewModel.updateSavingsGoal(newGoal)
                showAddGoalDialog = false
                editingGoal = null
            }
        )
    }

    // Add Asset Dialog
    if (showAddAssetDialog || editingAsset != null) {
        AddAssetDialog(
            lang = lang,
            initialAsset = editingAsset,
            onDismiss = { showAddAssetDialog = false; editingAsset = null },
            onConfirm = { newAsset ->
                if (editingAsset == null) viewModel.addAsset(newAsset) else viewModel.updateAsset(newAsset)
                showAddAssetDialog = false
                editingAsset = null
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
    onEdit: () -> Unit,
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
                    FinanceIcon(
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
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FinanceOutlinedButton(onClick = onEdit) { Text(WebI18n.tr("تعديل", "Edit", lang)) }
                    FinanceButton(
                    onClick = onContribute,
                    colors = ButtonDefaults.buttonColors(containerColor = ByeProTheme.colors.goldPrimary, contentColor = MaterialTheme.colorScheme.onPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    FinanceIcon(Icons.Default.Savings, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(Strings.get("contribute", lang), modifier = Modifier.padding(start = 4.dp), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun AssetCardItem(
    asset: AssetEntity,
    lang: AppLanguage,
    onEdit: () -> Unit,
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
                FinanceIconButton(onClick = onEdit) {
                    FinanceIcon(imageVector = Icons.Default.Add, contentDescription = WebI18n.tr("تعديل", "Edit", lang), modifier = Modifier.size(18.dp))
                }
                FinanceIconButton(onClick = onDelete) {
                    FinanceIcon(
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
    initialGoal: SavingsGoalEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (SavingsGoalEntity) -> Unit
) {
    var name by remember(initialGoal?.id) { mutableStateOf(initialGoal?.name ?: "") }
    var targetStr by remember(initialGoal?.id) { mutableStateOf(initialGoal?.targetAmount?.toString() ?: "") }
    var currentStr by remember(initialGoal?.id) { mutableStateOf(initialGoal?.currentAmount?.toString() ?: "0") }
    var currency by remember(initialGoal?.id) { mutableStateOf(initialGoal?.currency ?: "LYD") }
    var monthlyStr by remember(initialGoal?.id) { mutableStateOf(initialGoal?.targetMonthlyDeposit?.toString() ?: "0") }
    var deadlineText by remember(initialGoal?.id) { mutableStateOf(java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(initialGoal?.deadline ?: (System.currentTimeMillis() + 180L * 24 * 60 * 60 * 1000)))) }
    var error by remember { mutableStateOf("") }
    FinanceAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialGoal == null) Strings.get("add_goal", lang) else WebI18n.tr("تعديل هدف الادخار", "Edit savings goal", lang), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                FinanceTextField(value = name, onValueChange = { name = it }, label = { Text(WebI18n.tr("اسم الهدف الادخاري", "Savings goal name", lang)) }, modifier = Modifier.fillMaxWidth())
                FinanceTextField(value = targetStr, onValueChange = { targetStr = it }, label = { Text(Strings.get("target_amount", lang)) }, modifier = Modifier.fillMaxWidth())
                FinanceTextField(value = currentStr, onValueChange = { currentStr = it }, label = { Text(WebI18n.tr("الرصيد المدخر حالياً", "Current saved balance", lang)) }, modifier = Modifier.fillMaxWidth())
                FinanceTextField(value = currency, onValueChange = { currency = it.uppercase(java.util.Locale.ROOT) }, label = { Text(WebI18n.tr("العملة", "Currency", lang)) }, modifier = Modifier.fillMaxWidth())
                FinanceTextField(value = monthlyStr, onValueChange = { monthlyStr = it }, label = { Text(WebI18n.tr("الإيداع الشهري المستهدف", "Target monthly deposit", lang)) }, modifier = Modifier.fillMaxWidth())
                FinanceTextField(value = deadlineText, onValueChange = { deadlineText = it }, label = { Text(WebI18n.tr("الموعد النهائي YYYY-MM-DD", "Target date YYYY-MM-DD", lang)) }, modifier = Modifier.fillMaxWidth())
                if (error.isNotBlank()) Text(error, color = ByeProTheme.colors.financialRed, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            FinanceButton(onClick = {
                val target = targetStr.toDoubleOrNull() ?: Double.NaN
                val current = currentStr.toDoubleOrNull() ?: Double.NaN
                val monthly = monthlyStr.toDoubleOrNull() ?: Double.NaN
                val deadline = try { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply { isLenient = false }.parse(deadlineText)?.time } catch (_: Exception) { null }
                error = when {
                    name.isBlank() -> WebI18n.tr("أدخل اسم الهدف.", "Enter a goal name.", lang)
                    !target.isFinite() || target <= 0.0 -> WebI18n.tr("قيمة الهدف يجب أن تكون أكبر من صفر.", "Target must be greater than zero.", lang)
                    !current.isFinite() || current < 0.0 || current > target -> WebI18n.tr("الرصيد الحالي يجب أن يكون بين صفر وقيمة الهدف.", "Current savings must be between zero and the target.", lang)
                    !monthly.isFinite() || monthly < 0.0 -> WebI18n.tr("أدخل إيداعاً شهرياً صحيحاً.", "Enter a valid monthly deposit.", lang)
                    !currency.matches(Regex("[A-Z]{3}")) -> WebI18n.tr("رمز العملة يجب أن يتكون من ثلاثة أحرف.", "Currency must be a three-letter code.", lang)
                    deadline == null -> WebI18n.tr("أدخل تاريخاً صحيحاً.", "Enter a valid date.", lang)
                    else -> ""
                }
                if (error.isBlank() && deadline != null) onConfirm(SavingsGoalEntity(id = initialGoal?.id ?: 0, name = name.trim(), targetAmount = target, currentAmount = current, currency = currency, deadline = deadline, targetMonthlyDeposit = monthly, colorHex = initialGoal?.colorHex ?: "#D6B56D"))
            }) { Text(Strings.get("confirm", lang)) }
        },
        dismissButton = { FinanceOutlinedButton(onClick = onDismiss) { Text(Strings.get("cancel", lang)) } }
    )
}

@Composable
fun AddAssetDialog(
    lang: com.example.finance.model.AppLanguage,
    initialAsset: AssetEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (AssetEntity) -> Unit
) {
    var name by remember(initialAsset?.id) { mutableStateOf(initialAsset?.name ?: "") }
    var valueStr by remember(initialAsset?.id) { mutableStateOf(initialAsset?.value?.toString() ?: "") }
    var currency by remember(initialAsset?.id) { mutableStateOf(initialAsset?.currency ?: "LYD") }
    var typeText by remember(initialAsset?.id) { mutableStateOf((initialAsset?.type ?: AssetType.PROPERTY).name) }
    var purchaseDateText by remember(initialAsset?.id) { mutableStateOf(java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(initialAsset?.purchaseDate ?: System.currentTimeMillis()))) }
    var notes by remember(initialAsset?.id) { mutableStateOf(initialAsset?.notes ?: "") }
    var error by remember { mutableStateOf("") }
    FinanceAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialAsset == null) Strings.get("add_asset", lang) else WebI18n.tr("تعديل الأصل", "Edit asset", lang), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                FinanceTextField(value = name, onValueChange = { name = it }, label = { Text(WebI18n.tr("اسم الأصل", "Asset name", lang)) }, modifier = Modifier.fillMaxWidth())
                FinanceTextField(value = valueStr, onValueChange = { valueStr = it }, label = { Text(Strings.get("asset_value", lang)) }, modifier = Modifier.fillMaxWidth())
                FinanceTextField(value = currency, onValueChange = { currency = it.uppercase(java.util.Locale.ROOT) }, label = { Text(WebI18n.tr("العملة", "Currency", lang)) }, modifier = Modifier.fillMaxWidth())
                FinanceTextField(value = typeText, onValueChange = { typeText = it.uppercase(java.util.Locale.ROOT) }, label = { Text(WebI18n.tr("النوع: PROPERTY / GOLD / VEHICLE / CASH / INVESTMENTS / OTHER", "Type: PROPERTY / GOLD / VEHICLE / CASH / INVESTMENTS / OTHER", lang)) }, modifier = Modifier.fillMaxWidth())
                FinanceTextField(value = purchaseDateText, onValueChange = { purchaseDateText = it }, label = { Text(WebI18n.tr("تاريخ الشراء YYYY-MM-DD", "Purchase date YYYY-MM-DD", lang)) }, modifier = Modifier.fillMaxWidth())
                FinanceTextField(value = notes, onValueChange = { notes = it }, label = { Text(Strings.get("notes", lang)) }, modifier = Modifier.fillMaxWidth())
                if (error.isNotBlank()) Text(error, color = ByeProTheme.colors.financialRed, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            FinanceButton(onClick = {
                val value = valueStr.toDoubleOrNull() ?: Double.NaN
                val type = runCatching { AssetType.valueOf(typeText.trim().uppercase(java.util.Locale.ROOT)) }.getOrNull()
                val purchaseDate = try { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply { isLenient = false }.parse(purchaseDateText)?.time } catch (_: Exception) { null }
                error = when {
                    name.isBlank() -> WebI18n.tr("أدخل اسم الأصل.", "Enter an asset name.", lang)
                    !value.isFinite() || value < 0.0 -> WebI18n.tr("أدخل قيمة صحيحة غير سالبة.", "Enter a valid non-negative value.", lang)
                    !currency.matches(Regex("[A-Z]{3}")) -> WebI18n.tr("رمز العملة يجب أن يتكون من ثلاثة أحرف.", "Currency must be a three-letter code.", lang)
                    type == null -> WebI18n.tr("نوع الأصل غير صحيح.", "Asset type is invalid.", lang)
                    purchaseDate == null -> WebI18n.tr("أدخل تاريخ شراء صحيحاً.", "Enter a valid purchase date.", lang)
                    else -> ""
                }
                if (error.isBlank() && type != null && purchaseDate != null) onConfirm(AssetEntity(id = initialAsset?.id ?: 0, name = name.trim(), type = type, value = value, currency = currency, purchaseDate = purchaseDate, notes = notes.trim()))
            }) { Text(Strings.get("confirm", lang)) }
        },
        dismissButton = { FinanceOutlinedButton(onClick = onDismiss) { Text(Strings.get("cancel", lang)) } }
    )
}
