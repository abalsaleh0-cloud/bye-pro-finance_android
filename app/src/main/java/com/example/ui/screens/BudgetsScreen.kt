package com.example.ui.screens

import com.example.ui.components.FinanceIcon

import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.*

import androidx.compose.material.icons.filled.*

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.finance.engine.FinanceEngine
import com.example.finance.i18n.Strings
import com.example.finance.i18n.TextContent
import com.example.finance.i18n.WebI18n
import com.example.finance.model.AppLanguage
import com.example.finance.model.BudgetEntity
import com.example.finance.model.BudgetPeriod
import com.example.finance.ui.FinanceViewModel
import com.example.ui.components.FinanceTextField
import com.example.ui.components.FinanceTextButton
import com.example.ui.components.FinanceAlertDialog
import com.example.ui.components.FinanceIconButton
import com.example.ui.components.FinanceFloatingActionButton
import com.example.ui.theme.ByeProTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val budgetCurrencies = listOf("LYD", "USD", "EUR", "GBP", "SYP", "JOD")
private val budgetPeriods = listOf(BudgetPeriod.WEEKLY, BudgetPeriod.MONTHLY, BudgetPeriod.QUARTERLY, BudgetPeriod.YEARLY, BudgetPeriod.CUSTOM)

private fun budgetDate(value: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(value))

private fun parseBudgetDate(value: String): Long? = runCatching {
    SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(value)?.time
}.getOrNull()

private fun periodLabel(period: BudgetPeriod, lang: AppLanguage): String = when (period) {
    BudgetPeriod.WEEKLY -> WebI18n.tr("أسبوعية", "Weekly", lang)
    BudgetPeriod.MONTHLY -> WebI18n.tr("شهرية", "Monthly", lang)
    BudgetPeriod.QUARTERLY -> WebI18n.tr("ربع سنوية", "Quarterly", lang)
    BudgetPeriod.YEARLY -> WebI18n.tr("سنوية", "Yearly", lang)
    BudgetPeriod.CUSTOM -> WebI18n.tr("مخصصة", "Custom", lang)
}

@Composable
fun BudgetsScreen(viewModel: FinanceViewModel) {
    val lang by viewModel.appLanguage.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    var showForm by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<BudgetEntity?>(null) }
    var deleting by remember { mutableStateOf<BudgetEntity?>(null) }
    var search by remember { mutableStateOf("") }

    val visible = remember(budgets, search) {
        val q = search.trim()
        if (q.isEmpty()) budgets else budgets.filter {
            it.name.contains(q, true) || it.category.contains(q, true) ||
                it.currency.contains(q, true) || it.period.name.contains(q, true)
        }
    }

    Scaffold(
        floatingActionButton = {
            FinanceFloatingActionButton(
                onClick = { editing = null; showForm = true },
                containerColor = ByeProTheme.colors.goldPrimary,
                modifier = Modifier.testTag("add_budget_fab")
            ) { FinanceIcon(Lucide.Plus, contentDescription = WebI18n.tr("إضافة ميزانية", "Add budget", lang)) }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                Text(WebI18n.tr("الميزانيات", "Budgets", lang), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(WebI18n.tr("خطط الإنفاق مع متابعة المبلغ المخصص لكل فترة.", "Plan spending and track allocations for each period.", lang),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                FinanceTextField(
                    value = search, onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth().testTag("budget_search_input"),
                    label = { Text(WebI18n.tr("بحث في الميزانيات…", "Search budgets…", lang)) },
                    singleLine = true
                )
                Text(
                    WebI18n.tr("عدد الميزانيات: ", "Budgets: ", lang) + visible.size,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (visible.isEmpty()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (budgets.isEmpty()) WebI18n.tr("لا توجد ميزانيات بعد. أضف ميزانيتك الأولى.", "No budgets yet. Add your first budget.", lang)
                                else WebI18n.tr("لا توجد نتائج مطابقة للبحث.", "No matching budgets found.", lang))
                        }
                    }
                }
            } else {
                items(visible, key = { it.id }) { budget ->
                    BudgetItemCard(
                        budget = budget,
                        status = FinanceEngine.calculateBudgetStatus(budget, transactions),
                        lang = lang,
                        onEdit = { editing = budget; showForm = true },
                        onDelete = { deleting = budget }
                    )
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showForm) {
        BudgetFormDialog(
            lang = lang,
            initial = editing,
            onDismiss = { showForm = false; editing = null },
            onConfirm = { budget ->
                if (editing == null) viewModel.addBudget(budget) else viewModel.updateBudget(budget)
                showForm = false
                editing = null
            }
        )
    }

    deleting?.let { target ->
        FinanceAlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(WebI18n.tr("تأكيد الحذف", "Confirm deletion", lang)) },
            text = { Text(WebI18n.tr("هل تريد حذف الميزانية", "Delete budget", lang) + " «${target.name}»؟") },
            confirmButton = {
                FinanceTextButton(onClick = { viewModel.deleteBudget(target); deleting = null }) {
                    Text(Strings.get("delete", lang), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { FinanceTextButton(onClick = { deleting = null }) { Text(Strings.get("cancel", lang)) } }
        )
    }
}

@Composable
private fun BudgetItemCard(
    budget: BudgetEntity,
    status: FinanceEngine.BudgetStatus,
    lang: AppLanguage,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val fraction = (status.percentage / 100f).coerceIn(0f, 1f)
    val progressColor = when {
        status.isOverBudget || status.percentage >= 90f -> ByeProTheme.colors.financialRed
        status.percentage >= 75f -> ByeProTheme.colors.goldPrimary
        else -> ByeProTheme.colors.financialGreen
    }
    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .35f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(budget.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${budget.currency} • ${periodLabel(budget.period, lang)}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row {
                    FinanceIconButton(onClick = onEdit, modifier = Modifier.testTag("edit_budget_${budget.id}")) {
                        FinanceIcon(Lucide.Pencil, contentDescription = WebI18n.tr("تعديل", "Edit", lang))
                    }
                    FinanceIconButton(onClick = onDelete, modifier = Modifier.testTag("delete_budget_${budget.id}")) {
                        FinanceIcon(Lucide.Trash2, contentDescription = WebI18n.tr("حذف", "Delete", lang), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Text("${WebI18n.tr("من", "From", lang)}: ${budgetDate(budget.startDate)}   •   ${WebI18n.tr("إلى", "To", lang)}: ${budgetDate(budget.endDate)}",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = progressColor, trackColor = MaterialTheme.colorScheme.surface)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${Strings.get("spent", lang)}: ${TextContent.money(status.spent, budget.currency, lang)}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${Strings.get("limit_amount", lang)}: ${TextContent.money(status.limit, budget.currency, lang)}",
                    style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
            Text("${TextContent.number(status.percentage.toInt(), lang, 0)}%",
                style = MaterialTheme.typography.labelMedium, color = progressColor, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BudgetFormDialog(
    lang: AppLanguage,
    initial: BudgetEntity?,
    onDismiss: () -> Unit,
    onConfirm: (BudgetEntity) -> Unit
) {
    val today = remember { budgetDate(System.currentTimeMillis()) }
    var name by remember(initial) { mutableStateOf(initial?.name.orEmpty()) }
    var amount by remember(initial) { mutableStateOf(initial?.limitAmount?.toString().orEmpty()) }
    var currency by remember(initial) { mutableStateOf(initial?.currency ?: "LYD") }
    var period by remember(initial) { mutableStateOf(initial?.period ?: BudgetPeriod.MONTHLY) }
    var startDate by remember(initial) { mutableStateOf(initial?.startDate?.let(::budgetDate) ?: today) }
    var endDate by remember(initial) { mutableStateOf(initial?.endDate?.let(::budgetDate) ?: today) }
    var category by remember(initial) { mutableStateOf(initial?.category ?: "All") }
    var error by remember { mutableStateOf("") }
    var currencyMenu by remember { mutableStateOf(false) }
    var periodMenu by remember { mutableStateOf(false) }

    FinanceAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) WebI18n.tr("إضافة ميزانية", "Add budget", lang) else WebI18n.tr("تعديل الميزانية", "Edit budget", lang), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FinanceTextField(name, { name = it.take(100); error = "" }, label = { Text(WebI18n.tr("اسم الميزانية", "Budget name", lang)) },
                    modifier = Modifier.fillMaxWidth().testTag("budget_name_input"), singleLine = true)
                FinanceTextField(amount, { amount = it; error = "" }, label = { Text(WebI18n.tr("المبلغ", "Amount", lang)) },
                    modifier = Modifier.fillMaxWidth().testTag("budget_limit_input"), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                Box {
                    FinanceTextField(currency, {}, label = { Text(WebI18n.tr("العملة", "Currency", lang)) },
                        modifier = Modifier.fillMaxWidth().testTag("budget_currency_input"), readOnly = true)
                    Box(Modifier.matchParentSize().clickable { currencyMenu = true })
                    DropdownMenu(expanded = currencyMenu, onDismissRequest = { currencyMenu = false }) {
                        budgetCurrencies.forEach { code -> DropdownMenuItem(text = { Text(code) }, onClick = { currency = code; currencyMenu = false }) }
                    }
                }
                Box {
                    FinanceTextField(periodLabel(period, lang), {}, label = { Text(WebI18n.tr("الفترة", "Period", lang)) },
                        modifier = Modifier.fillMaxWidth().testTag("budget_period_input"), readOnly = true)
                    Box(Modifier.matchParentSize().clickable { periodMenu = true })
                    DropdownMenu(expanded = periodMenu, onDismissRequest = { periodMenu = false }) {
                        budgetPeriods.forEach { option -> DropdownMenuItem(text = { Text(periodLabel(option, lang)) },
                            onClick = { period = option; periodMenu = false }) }
                    }
                }
                FinanceTextField(startDate, { startDate = it; error = "" }, label = { Text(WebI18n.tr("تاريخ البداية (YYYY-MM-DD)", "Start date (YYYY-MM-DD)", lang)) },
                    modifier = Modifier.fillMaxWidth().testTag("budget_start_date_input"), singleLine = true)
                FinanceTextField(endDate, { endDate = it; error = "" }, label = { Text(WebI18n.tr("تاريخ النهاية (YYYY-MM-DD)", "End date (YYYY-MM-DD)", lang)) },
                    modifier = Modifier.fillMaxWidth().testTag("budget_end_date_input"), singleLine = true)
                if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            FinanceTextButton(onClick = {
                val parsedAmount = amount.toDoubleOrNull()
                val start = parseBudgetDate(startDate)
                val end = parseBudgetDate(endDate)
                error = when {
                    name.isBlank() -> WebI18n.tr("أدخل اسم الميزانية.", "Enter a budget name.", lang)
                    parsedAmount == null || !parsedAmount.isFinite() || parsedAmount <= 0.0 -> WebI18n.tr("أدخل مبلغاً أكبر من الصفر.", "Enter an amount greater than zero.", lang)
                    start == null -> WebI18n.tr("تاريخ البداية غير صالح.", "Start date is invalid.", lang)
                    end == null -> WebI18n.tr("تاريخ النهاية غير صالح.", "End date is invalid.", lang)
                    end < start -> WebI18n.tr("تاريخ النهاية يجب ألا يسبق البداية.", "End date must not be before start date.", lang)
                    else -> ""
                }
                if (error.isEmpty()) onConfirm(BudgetEntity(
                    id = initial?.id ?: 0L,
                    name = name.trim(),
                    category = category,
                    period = period,
                    limitAmount = parsedAmount!!,
                    currency = currency,
                    startDate = start!!,
                    endDate = end!!
                ))
            }, modifier = Modifier.testTag("submit_budget_btn")) {
                Text(if (initial == null) WebI18n.tr("حفظ", "Save", lang) else WebI18n.tr("حفظ التعديلات", "Save changes", lang))
            }
        },
        dismissButton = { FinanceTextButton(onClick = onDismiss) { Text(WebI18n.tr("إلغاء", "Cancel", lang)) } }
    )
}
