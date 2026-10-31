package com.example.ui.screens

import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Users
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.Trash2
import com.example.ui.components.FinanceIcon
import com.example.finance.i18n.TextContent
import com.example.ui.components.FinanceAlertDialog
import com.example.ui.components.FinanceButton
import com.example.ui.components.FinanceOutlinedButton
import com.example.ui.components.FinanceTextField
import com.example.ui.components.FinanceIconButton
import com.example.ui.components.FinanceFloatingActionButton
import com.example.finance.i18n.WebI18n
import com.example.ui.theme.ByeProTheme
import com.example.finance.i18n.Strings
import com.example.finance.model.HouseholdExpenseEntity
import com.example.finance.model.HouseholdMemberEntity
import com.example.finance.ui.FinanceViewModel
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val householdRoles = listOf("owner", "partner", "child", "parent", "family", "other")
private val householdCategories = listOf("طعام", "كهرباء", "ماء", "إيجار", "غاز", "إنترنت", "تعليم", "صحة", "مواصلات", "تسوق", "عام")
private val householdPeriods = listOf("weekly", "monthly", "yearly")

private fun householdDate(timestamp: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(timestamp))

private fun householdDateTimestamp(value: String): Long =
    runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(value)?.time }.getOrNull()
        ?: System.currentTimeMillis()

private fun householdRoleLabel(role: String, lang: com.example.finance.model.AppLanguage): String {
    val labels = mapOf(
        "owner" to ("المالك" to "Owner"),
        "partner" to ("الشريك" to "Partner"),
        "child" to ("الابن/الابنة" to "Child"),
        "parent" to ("الوالد/الوالدة" to "Parent"),
        "family" to ("فرد الأسرة" to "Family member"),
        "other" to ("آخر" to "Other")
    )
    val pair = labels[role] ?: labels.getValue("other")
    return WebI18n.tr(pair.first, pair.second, lang)
}

private fun householdCategoryLabel(category: String, lang: com.example.finance.model.AppLanguage): String {
    val labels = mapOf(
        "طعام" to "Food", "كهرباء" to "Electricity", "ماء" to "Water",
        "إيجار" to "Rent", "غاز" to "Gas", "إنترنت" to "Internet",
        "تعليم" to "Education", "صحة" to "Health", "مواصلات" to "Transport",
        "تسوق" to "Shopping", "عام" to "General"
    )
    return if (lang == com.example.finance.model.AppLanguage.EN) labels[category] ?: category else category
}

@Composable
fun HouseholdScreen(viewModel: FinanceViewModel) {
    val lang by viewModel.appLanguage.collectAsState()
    val members by viewModel.householdMembers.collectAsState()
    val expenses by viewModel.householdExpenses.collectAsState()
    val budgets by viewModel.householdBudgets.collectAsState()
    val currency by viewModel.primaryCurrency.collectAsState()

    var editingMember by remember { mutableStateOf<HouseholdMemberEntity?>(null) }
    var editingExpense by remember { mutableStateOf<HouseholdExpenseEntity?>(null) }
    var showMemberDialog by remember { mutableStateOf(false) }
    var showExpenseDialog by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Triple<String, Long, String>?>(null) }

    val cal = Calendar.getInstance()
    cal.set(Calendar.DAY_OF_MONTH, 1)
    cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
    val monthStart = cal.timeInMillis
    cal.add(Calendar.MONTH, 1)
    val monthEnd = cal.timeInMillis
    val monthExpenses = expenses.filter { it.date >= monthStart && it.date < monthEnd }
    val primaryCurrencyExpenses = monthExpenses.filter { it.currency.equals(currency, true) }
    val totalMonth = primaryCurrencyExpenses.sumOf { it.amount }
    val recurring = primaryCurrencyExpenses.filter { it.isRecurring }.sumOf { it.amount }
    val monthlyBudget = budgets.filter { it.period == "monthly" && it.currencyCode.equals(currency, true) }.sumOf { it.amount }
    val remaining = (monthlyBudget - totalMonth).coerceAtLeast(0.0)
    val progress = if (monthlyBudget > 0.0) (totalMonth / monthlyBudget * 100.0).coerceIn(0.0, 100.0) else 0.0
    val money: (Double, String) -> String = { amount, code -> TextContent.money(amount, code, lang) }

    Scaffold(
        floatingActionButton = {
            FinanceFloatingActionButton(
                onClick = { editingExpense = null; showExpenseDialog = true },
                containerColor = ByeProTheme.colors.primaryButtonBg,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_household_expense_fab")
            ) { FinanceIcon(Lucide.Plus, contentDescription = Strings.get("add_shared_expense", lang)) }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(WebI18n.tr("مصروفات المنزل", "Household expenses", lang), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(WebI18n.tr("إدارة مصروفات الأسرة والالتزامات والميزانيات المشتركة", "Manage family expenses, shared obligations, and household budgets", lang), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    FinanceOutlinedButton(onClick = { editingMember = null; showMemberDialog = true }, shape = RoundedCornerShape(8.dp)) {
                        FinanceIcon(Lucide.Users, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text(WebI18n.tr("إضافة فرد", "Add member", lang), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        HouseholdStatCard(WebI18n.tr("مصروف هذا الشهر", "This month", lang), money(totalMonth, currency), Modifier.weight(1f))
                        HouseholdStatCard(WebI18n.tr("ميزانية الشهر", "Monthly budget", lang), money(monthlyBudget, currency), Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        HouseholdStatCard(WebI18n.tr("المتبقي", "Remaining", lang), money(remaining, currency), Modifier.weight(1f))
                        HouseholdStatCard(WebI18n.tr("المتكرر", "Recurring", lang), money(recurring, currency), Modifier.weight(1f))
                    }
                }
            }
            item {
                HouseholdSectionTitle("01", WebI18n.tr("مصروفات الشهر", "Monthly expenses", lang), monthExpenses.size.toString())
                if (monthlyBudget > 0.0) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(WebI18n.tr("استهلاك الميزانية", "Budget used", lang), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(progress.toInt().toString() + "%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    LinearProgressIndicator(progress = (progress / 100.0).toFloat(), modifier = Modifier.fillMaxWidth())
                }
            }
            if (monthExpenses.isEmpty()) {
                item { Text(WebI18n.tr("لا توجد مصروفات لهذا الشهر.", "No expenses for this month yet.", lang), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
            } else {
                items(monthExpenses, key = { "expense-" + it.id }) { expense ->
                    val payer = members.firstOrNull { it.id == expense.payerMemberId }?.name ?: WebI18n.tr("الأسرة", "Household", lang)
                    HouseholdRecordCard(
                        title = expense.title,
                        subtitle = householdDate(expense.date) + " · " + householdCategoryLabel(expense.category, lang) + " · " + payer + if (expense.isRecurring) " · " + WebI18n.tr("متكرر", "Recurring", lang) else "",
                        trailing = money(expense.amount, expense.currency),
                        onEdit = { editingExpense = expense; showExpenseDialog = true },
                        onDelete = { deleteTarget = Triple("expense", expense.id, expense.title) },
                        lang = lang
                    )
                    if (expense.notes.isNotBlank()) Text(expense.notes, modifier = Modifier.padding(start = 12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item { HouseholdSectionTitle("02", WebI18n.tr("أفراد الأسرة", "Household members", lang), members.count { it.isActive }.toString()) }
            if (members.none { it.isActive }) {
                item { Text(WebI18n.tr("لم تتم إضافة أفراد بعد.", "No household members yet.", lang), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
            } else {
                items(members.filter { it.isActive }, key = { "member-" + it.id }) { member ->
                    HouseholdRecordCard(
                        title = member.name,
                        subtitle = householdRoleLabel(member.role, lang) + " · " + WebI18n.tr("المساهمة الشهرية", "Monthly contribution", lang) + ": " + money(member.monthlyContribution, currency),
                        trailing = "",
                        onEdit = { editingMember = member; showMemberDialog = true },
                        onDelete = { deleteTarget = Triple("member", member.id, member.name) },
                        lang = lang
                    )
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    HouseholdSectionTitle("03", WebI18n.tr("ميزانيات المنزل", "Household budgets", lang))
                    FinanceOutlinedButton(onClick = { showBudgetDialog = true }, shape = RoundedCornerShape(8.dp)) {
                        FinanceIcon(Lucide.Plus, contentDescription = null, modifier = Modifier.size(15.dp))
                        Text(WebI18n.tr("إضافة ميزانية", "Add budget", lang), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            if (budgets.none { it.isActive }) {
                item { Text(WebI18n.tr("لا توجد ميزانيات بعد.", "No household budgets yet.", lang), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
            } else {
                items(budgets.filter { it.isActive }, key = { "budget-" + it.id }) { budget ->
                    val period = when (budget.period) {
                        "weekly" -> WebI18n.tr("أسبوعي", "Weekly", lang)
                        "yearly" -> WebI18n.tr("سنوي", "Yearly", lang)
                        else -> WebI18n.tr("شهري", "Monthly", lang)
                    }
                    HouseholdRecordCard(
                        title = budget.name,
                        subtitle = period + " · " + householdDate(budget.startDate),
                        trailing = money(budget.amount, budget.currencyCode),
                        onEdit = null,
                        onDelete = { deleteTarget = Triple("budget", budget.id, budget.name) },
                        lang = lang
                    )
                }
            }
            item { Spacer(Modifier.height(76.dp)) }
        }
    }

    if (showMemberDialog) {
        HouseholdMemberDialog(
            initial = editingMember, currency = currency, lang = lang,
            onDismiss = { showMemberDialog = false; editingMember = null },
            onSave = { name, role, contribution ->
                val old = editingMember
                if (old == null) viewModel.addHouseholdMember(name, "", role, contribution)
                else viewModel.updateHouseholdMember(old.copy(name = name, role = role, monthlyContribution = contribution))
                showMemberDialog = false; editingMember = null
            }
        )
    }
    if (showExpenseDialog) {
        HouseholdExpenseDialog(
            initial = editingExpense, members = members.filter { it.isActive }, currency = currency, lang = lang,
            onDismiss = { showExpenseDialog = false; editingExpense = null },
            onSave = { title, amount, payerId, category, expenseCurrency, date, isRecurring, notes ->
                val old = editingExpense
                if (old == null) viewModel.addHouseholdExpense(title, amount, payerId, category, expenseCurrency, date, isRecurring, notes)
                else viewModel.updateHouseholdExpense(old.copy(title = title, amount = amount, payerMemberId = payerId, category = category, currency = expenseCurrency, date = date, isRecurring = isRecurring, notes = notes))
                showExpenseDialog = false; editingExpense = null
            }
        )
    }
    if (showBudgetDialog) {
        HouseholdBudgetDialog(currency, lang, { showBudgetDialog = false }) { name, amount, code, period ->
            viewModel.addHouseholdBudget(name, amount, code, period)
            showBudgetDialog = false
        }
    }
    deleteTarget?.let { target ->
        FinanceAlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(WebI18n.tr("تأكيد الحذف", "Confirm deletion", lang), fontWeight = FontWeight.Bold) },
            text = { Text(WebI18n.tr("هل تريد حذف «" + target.third + "» نهائياً؟", "Do you want to permanently delete “" + target.third + "”?", lang)) },
            confirmButton = {
                FinanceButton(onClick = {
                    when (target.first) {
                        "member" -> members.firstOrNull { it.id == target.second }?.let(viewModel::deleteHouseholdMember)
                        "expense" -> expenses.firstOrNull { it.id == target.second }?.let(viewModel::deleteHouseholdExpense)
                        "budget" -> budgets.firstOrNull { it.id == target.second }?.let(viewModel::deleteHouseholdBudget)
                    }
                    deleteTarget = null
                }) { Text(WebI18n.tr("حذف نهائياً", "Delete permanently", lang)) }
            },
            dismissButton = { FinanceOutlinedButton(onClick = { deleteTarget = null }) { Text(Strings.get("cancel", lang)) } }
        )
    }
}

@Composable
private fun HouseholdStatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(14.dp)), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ByeProTheme.colors.goldPrimary)
        }
    }
}

@Composable
private fun HouseholdSectionTitle(number: String, title: String, trailing: String? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(number, style = MaterialTheme.typography.labelSmall, color = ByeProTheme.colors.goldPrimary, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        trailing?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun HouseholdRecordCard(
    title: String,
    subtitle: String,
    trailing: String,
    onEdit: (() -> Unit)?,
    onDelete: () -> Unit,
    lang: com.example.finance.model.AppLanguage
) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (trailing.isNotBlank()) Text(trailing, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = ByeProTheme.colors.goldPrimary)
            onEdit?.let { FinanceIconButton(onClick = it) { FinanceIcon(Lucide.Pencil, contentDescription = WebI18n.tr("تعديل", "Edit", lang), tint = MaterialTheme.colorScheme.onSurfaceVariant) } }
            FinanceIconButton(onClick = onDelete) { FinanceIcon(Lucide.Trash2, contentDescription = WebI18n.tr("حذف", "Delete", lang), tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun HouseholdMemberDialog(
    initial: HouseholdMemberEntity?,
    currency: String,
    lang: com.example.finance.model.AppLanguage,
    onDismiss: () -> Unit,
    onSave: (String, String, Double) -> Unit
) {
    var name by remember(initial?.id) { mutableStateOf(initial?.name.orEmpty()) }
    var role by remember(initial?.id) { mutableStateOf(initial?.role ?: "family") }
    var contribution by remember(initial?.id) { mutableStateOf(initial?.monthlyContribution?.takeIf { it > 0 }?.toString().orEmpty()) }
    var error by remember { mutableStateOf("") }
    FinanceAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(WebI18n.tr(if (initial == null) "إضافة فرد الأسرة" else "تعديل فرد الأسرة", if (initial == null) "Add household member" else "Edit household member", lang), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
                FinanceTextField(value = name, onValueChange = { name = it }, label = { Text(WebI18n.tr("اسم الفرد", "Member name", lang)) }, modifier = Modifier.fillMaxWidth())
                HouseholdDropdown(role, householdRoles, { householdRoleLabel(it, lang) }, { role = it }, WebI18n.tr("دور الفرد", "Member role", lang))
                FinanceTextField(value = contribution, onValueChange = { contribution = it }, label = { Text(WebI18n.tr("المساهمة الشهرية", "Monthly contribution", lang) + " (" + currency + ")") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            FinanceButton(onClick = {
                val value = contribution.toDoubleOrNull() ?: 0.0
                if (name.isBlank() || !value.isFinite() || value < 0.0) error = WebI18n.tr("أدخل اسماً ومساهمة صحيحة.", "Enter a name and a valid contribution.", lang)
                else onSave(name.trim(), role, value)
            }) { Text(Strings.get("confirm", lang)) }
        },
        dismissButton = { FinanceOutlinedButton(onClick = onDismiss) { Text(Strings.get("cancel", lang)) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HouseholdExpenseDialog(
    initial: HouseholdExpenseEntity?,
    members: List<HouseholdMemberEntity>,
    currency: String,
    lang: com.example.finance.model.AppLanguage,
    onDismiss: () -> Unit,
    onSave: (String, Double, Long?, String, String, Long, Boolean, String) -> Unit
) {
    var title by remember(initial?.id) { mutableStateOf(initial?.title.orEmpty()) }
    var amount by remember(initial?.id) { mutableStateOf(initial?.amount?.toString().orEmpty()) }
    var payer by remember(initial?.id, members.size) { mutableStateOf(initial?.payerMemberId?.toString().orEmpty()) }
    var category by remember(initial?.id) { mutableStateOf(initial?.category ?: "عام") }
    var currencyCode by remember(initial?.id) { mutableStateOf(initial?.currency ?: currency) }
    var date by remember(initial?.id) { mutableStateOf(householdDate(initial?.date ?: System.currentTimeMillis())) }
    var recurring by remember(initial?.id) { mutableStateOf(initial?.isRecurring ?: false) }
    var notes by remember(initial?.id) { mutableStateOf(initial?.notes.orEmpty()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    FinanceAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(WebI18n.tr(if (initial == null) "إضافة مصروف" else "تعديل المصروف", if (initial == null) "Add expense" else "Edit expense", lang), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
                FinanceTextField(value = title, onValueChange = { title = it }, label = { Text(WebI18n.tr("وصف المصروف", "Expense description", lang)) }, modifier = Modifier.fillMaxWidth())
                FinanceTextField(value = amount, onValueChange = { amount = it }, label = { Text(WebI18n.tr("المبلغ", "Amount", lang)) }, modifier = Modifier.fillMaxWidth())
                HouseholdDropdown(category, householdCategories, { householdCategoryLabel(it, lang) }, { category = it }, WebI18n.tr("تصنيف المصروف", "Expense category", lang))
                HouseholdDropdown(payer, listOf("") + members.map { it.id.toString() }, { id -> if (id.isBlank()) WebI18n.tr("الأسرة / غير محدد", "Household / Unassigned", lang) else members.firstOrNull { it.id.toString() == id }?.name.orEmpty() }, { payer = it }, WebI18n.tr("من دفع؟", "Paid by", lang))
                FinanceTextField(value = currencyCode, onValueChange = { currencyCode = it.uppercase(Locale.ROOT).take(3) }, label = { Text(WebI18n.tr("العملة", "Currency", lang)) }, modifier = Modifier.fillMaxWidth())
                FinanceOutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) { Text(WebI18n.tr("تاريخ المصروف", "Expense date", lang) + ": " + date) }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Checkbox(checked = recurring, onCheckedChange = { recurring = it })
                    Text(WebI18n.tr("مصروف متكرر", "Recurring expense", lang), style = MaterialTheme.typography.bodyMedium)
                }
                FinanceTextField(value = notes, onValueChange = { notes = it }, label = { Text(WebI18n.tr("ملاحظات", "Notes", lang)) }, modifier = Modifier.fillMaxWidth(), singleLine = false, minLines = 2)
            }
        },
        confirmButton = {
            FinanceButton(onClick = {
                val value = amount.toDoubleOrNull()
                if (title.isBlank() || value == null || !value.isFinite() || value <= 0.0 || category.isBlank() || !currencyCode.matches(Regex("[A-Z]{3}"))) {
                    error = WebI18n.tr("تحقق من الوصف والمبلغ والتصنيف والعملة.", "Check the description, amount, category, and currency.", lang)
                } else onSave(title.trim(), value, payer.toLongOrNull(), category, currencyCode, householdDateTimestamp(date), recurring, notes.trim())
            }) { Text(WebI18n.tr("تسجيل المصروف", "Record expense", lang)) }
        },
        dismissButton = { FinanceOutlinedButton(onClick = onDismiss) { Text(Strings.get("cancel", lang)) } }
    )
    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = householdDateTimestamp(date))
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { state.selectedDateMillis?.let { date = householdDate(it) }; showDatePicker = false }) { Text(Strings.get("confirm", lang)) } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(Strings.get("cancel", lang)) } }
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun HouseholdBudgetDialog(
    currency: String,
    lang: com.example.finance.model.AppLanguage,
    onDismiss: () -> Unit,
    onSave: (String, Double, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var currencyCode by remember { mutableStateOf(currency) }
    var period by remember { mutableStateOf("monthly") }
    var error by remember { mutableStateOf("") }
    FinanceAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(WebI18n.tr("إضافة ميزانية المنزل", "Add household budget", lang), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
                FinanceTextField(value = name, onValueChange = { name = it }, label = { Text(WebI18n.tr("اسم الميزانية", "Budget name", lang)) }, modifier = Modifier.fillMaxWidth())
                FinanceTextField(value = amount, onValueChange = { amount = it }, label = { Text(WebI18n.tr("المبلغ", "Amount", lang)) }, modifier = Modifier.fillMaxWidth())
                FinanceTextField(value = currencyCode, onValueChange = { currencyCode = it.uppercase(Locale.ROOT).take(3) }, label = { Text(WebI18n.tr("العملة", "Currency", lang)) }, modifier = Modifier.fillMaxWidth())
                HouseholdDropdown(period, householdPeriods, { value ->
                    when (value) {
                        "weekly" -> WebI18n.tr("أسبوعي", "Weekly", lang)
                        "yearly" -> WebI18n.tr("سنوي", "Yearly", lang)
                        else -> WebI18n.tr("شهري", "Monthly", lang)
                    }
                }, { period = it }, WebI18n.tr("فترة الميزانية", "Budget period", lang))
            }
        },
        confirmButton = {
            FinanceButton(onClick = {
                val value = amount.toDoubleOrNull()
                if (name.isBlank() || value == null || !value.isFinite() || value <= 0.0 || !currencyCode.matches(Regex("[A-Z]{3}"))) {
                    error = WebI18n.tr("أدخل اسماً ومبلغاً وعملة صحيحة.", "Enter a valid name, amount, and currency.", lang)
                } else onSave(name.trim(), value, currencyCode, period)
            }) { Text(WebI18n.tr("إضافة ميزانية", "Add budget", lang)) }
        },
        dismissButton = { FinanceOutlinedButton(onClick = onDismiss) { Text(Strings.get("cancel", lang)) } }
    )
}

@Composable
private fun HouseholdDropdown(
    selected: String,
    options: List<String>,
    labelFor: (String) -> String,
    onSelected: (String) -> Unit,
    label: String
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FinanceOutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(labelFor(selected).ifBlank { label }, style = MaterialTheme.typography.bodyMedium)
                Text("▾", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(labelFor(option)) }, onClick = { onSelected(option); expanded = false })
            }
        }
    }
}
