package com.example.ui.screens

import androidx.compose.material.icons.filled.*

import androidx.compose.material.icons.Icons

import com.composables.icons.lucide.*

import com.example.finance.i18n.TextContent
import com.example.finance.i18n.WebI18n
import com.example.finance.model.AccountEntity
import com.example.finance.model.AppLanguage
import com.example.finance.model.TransactionEntity
import com.example.finance.model.TransactionStatus
import com.example.finance.model.TransactionType
import com.example.finance.ui.FinanceViewModel
import com.example.ui.components.FinanceAlertDialog
import com.example.ui.components.FinanceButton
import com.example.ui.components.FinanceFloatingActionButton
import com.example.ui.components.FinanceIconButton
import com.example.ui.components.FinanceOutlinedButton
import com.example.ui.components.FinanceTextField
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.finance.engine.FinanceEngine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val webTransactionTypes = listOf(TransactionType.EXPENSE, TransactionType.INCOME, TransactionType.ADJUSTMENT)
private val webCategories = mapOf(
    TransactionType.EXPENSE to listOf("Groceries", "Dining", "Utilities", "Shopping", "Health", "Transport", "Other"),
    TransactionType.INCOME to listOf("Salary", "Investments", "Other"),
    TransactionType.ADJUSTMENT to listOf("Adjustment", "Other")
)

private fun txTypeLabel(type: TransactionType, lang: AppLanguage) = when (type) {
    TransactionType.INCOME -> WebI18n.tr("دخل", "Income", lang)
    TransactionType.EXPENSE -> WebI18n.tr("مصروف", "Expense", lang)
    TransactionType.ADJUSTMENT -> WebI18n.tr("تسوية", "Adjustment", lang)
    else -> WebI18n.tr("تحويل", "Transfer", lang)
}

private fun statusLabel(status: TransactionStatus, lang: AppLanguage) = when (status) {
    TransactionStatus.PENDING -> WebI18n.tr("معلّقة", "Pending", lang)
    TransactionStatus.COMPLETED -> WebI18n.tr("مكتملة", "Completed", lang)
    TransactionStatus.CANCELLED -> WebI18n.tr("ملغاة", "Cancelled", lang)
}

@Composable
fun TransactionsScreen(viewModel: FinanceViewModel, initialAddOpen: Boolean = false) {
    val lang by viewModel.appLanguage.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf<TransactionType?>(null) }
    var showForm by remember { mutableStateOf(initialAddOpen) }
    var deleteTarget by remember { mutableStateOf<TransactionEntity?>(null) }

    val activeAccounts = accounts.filter { !it.isArchived }
    val visible = transactions.filter { tx ->
        val accountName = accounts.firstOrNull { it.id == tx.accountId }?.name.orEmpty()
        val q = search.trim()
        (filter == null || tx.type == filter) &&
            (q.isBlank() || tx.description.contains(q, true) || accountName.contains(q, true))
    }
    val summary = visible.filter { it.status != TransactionStatus.CANCELLED }.groupBy { it.currency }.mapValues { (_, rows) ->
        val income = rows.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expense = rows.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        Triple(income, expense, rows.size)
    }

    Scaffold(
        floatingActionButton = {
            FinanceFloatingActionButton(
                onClick = { showForm = !showForm },
                containerColor = ByeProTheme.colors.goldPrimary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_transaction_fab")
            ) { Icon(Icons.Default.Add, contentDescription = WebI18n.tr("إضافة معاملة", "Add transaction", lang)) }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(Modifier.height(4.dp))
                Text(WebI18n.tr("الحركة المالية", "Financial activity", lang), style = MaterialTheme.typography.labelMedium, color = ByeProTheme.colors.goldPrimary)
                Text(WebI18n.tr("المعاملات", "Transactions", lang), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(WebI18n.tr("تسجيل ومراجعة الدخل والمصروفات والتسويات دون خلط العملات.", "Record and review income, expenses, and adjustments without mixing currencies.", lang), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                FinanceTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text(WebI18n.tr("ابحث في الوصف أو الحساب…", "Search description or account…", lang)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().testTag("transaction_search_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text(WebI18n.tr("كل الأنواع", "All types", lang)) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ByeProTheme.colors.goldPrimary))
                    webTransactionTypes.forEach { type ->
                        FilterChip(selected = filter == type, onClick = { filter = type }, label = { Text(txTypeLabel(type, lang)) })
                    }
                }
            }

            if (summary.isNotEmpty()) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        summary.forEach { (currency, values) ->
                            val (income, expense, count) = values
                            Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(currency, fontWeight = FontWeight.Bold)
                                    Text(TextContent.money(income - expense, currency, lang), fontWeight = FontWeight.Bold, color = if (income >= expense) ByeProTheme.colors.financialGreen else ByeProTheme.colors.financialRed)
                                    Text("${TextContent.money(income, currency, lang)} ${WebI18n.tr("دخل", "income", lang)} · ${TextContent.money(expense, currency, lang)} ${WebI18n.tr("مصروف", "expense", lang)} · $count ${WebI18n.tr("معاملة", "transactions", lang)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            if (showForm) {
                item {
                    AddTransactionForm(accounts = activeAccounts, lang = lang, onCancel = { showForm = false }, onSave = { viewModel.addTransaction(it); showForm = false })
                }
            }

            if (visible.isEmpty()) {
                item {
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(WebI18n.tr("لا توجد معاملات مطابقة", "No matching transactions", lang), fontWeight = FontWeight.Bold)
                            Text(WebI18n.tr("سجّل أول معاملة أو غيّر الفلتر.", "Record your first transaction or change the filter.", lang), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(visible, key = { it.id }) { tx ->
                    TransactionItemCard(transaction = tx, account = accounts.firstOrNull { it.id == tx.accountId }, lang = lang, onDelete = { deleteTarget = tx }, onToggleStatus = { viewModel.updateTransactionStatus(tx, if (tx.status == TransactionStatus.COMPLETED) TransactionStatus.PENDING else TransactionStatus.COMPLETED) })
                }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }

    deleteTarget?.let { target ->
        FinanceAlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(WebI18n.tr("حذف المعاملة", "Delete transaction", lang)) },
            text = { Text(WebI18n.tr("هل تريد حذف هذه المعاملة وإعادة تعديل رصيد الحساب تلقائياً؟", "Delete this transaction and automatically adjust the account balance?", lang)) },
            confirmButton = { FinanceButton(onClick = { viewModel.deleteTransaction(target); deleteTarget = null }) { Text(WebI18n.tr("حذف", "Delete", lang)) } },
            dismissButton = { FinanceOutlinedButton(onClick = { deleteTarget = null }) { Text(WebI18n.tr("إلغاء", "Cancel", lang)) } }
        )
    }
}

@Composable
fun AddTransactionForm(accounts: List<AccountEntity>, lang: AppLanguage, onCancel: () -> Unit, onSave: (TransactionEntity) -> Unit) {
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }
    var accountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: 0L) }
    var category by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) }
    var description by remember { mutableStateOf("") }
    var accountMenu by remember { mutableStateOf(false) }
    var categoryMenu by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val account = accounts.firstOrNull { it.id == accountId }
    val categories = webCategories[type].orEmpty()

    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(WebI18n.tr("إضافة معاملة", "Add transaction", lang), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (accounts.isEmpty()) Text(WebI18n.tr("أضف حساباً نشطاً أولاً من قسم الحسابات.", "Add an active account in Accounts first.", lang), color = MaterialTheme.colorScheme.error)

            Text(WebI18n.tr("نوع المعاملة", "Transaction type", lang), style = MaterialTheme.typography.labelMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                webTransactionTypes.forEach { candidate ->
                    FinanceButton(
                        onClick = { type = candidate; category = "" },
                        colors = ButtonDefaults.buttonColors(containerColor = if (type == candidate) ByeProTheme.colors.goldPrimary else MaterialTheme.colorScheme.surface),
                        modifier = Modifier.weight(1f).testTag("transaction_type_${candidate.name.lowercase()}")
                    ) { Text(txTypeLabel(candidate, lang)) }
                }
            }

            Text(WebI18n.tr("الحساب", "Account", lang), style = MaterialTheme.typography.labelMedium)
            Box(Modifier.fillMaxWidth()) {
                FinanceOutlinedButton(onClick = { accountMenu = true }, enabled = accounts.isNotEmpty(), modifier = Modifier.fillMaxWidth().testTag("transaction_account_input")) { Text(account?.let { "${it.name} · ${it.currency}" } ?: WebI18n.tr("اختر حساباً", "Select an account", lang)) }
                DropdownMenu(expanded = accountMenu, onDismissRequest = { accountMenu = false }) { accounts.forEach { item -> DropdownMenuItem(text = { Text("${item.name} · ${item.currency}") }, onClick = { accountId = item.id; accountMenu = false }) } }
            }

            Text(WebI18n.tr("التصنيف", "Category", lang), style = MaterialTheme.typography.labelMedium)
            Box(Modifier.fillMaxWidth()) {
                FinanceOutlinedButton(onClick = { categoryMenu = true }, modifier = Modifier.fillMaxWidth().testTag("transaction_category_input")) { Text(category.ifBlank { WebI18n.tr("بدون تصنيف", "No category", lang) }) }
                DropdownMenu(expanded = categoryMenu, onDismissRequest = { categoryMenu = false }) { categories.forEach { item -> DropdownMenuItem(text = { Text(item) }, onClick = { category = item; categoryMenu = false }) } }
            }

            FinanceTextField(value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' }.take(18) }, label = { Text(WebI18n.tr("المبلغ", "Amount", lang)) }, modifier = Modifier.fillMaxWidth().testTag("transaction_amount_input"), singleLine = true)
            FinanceTextField(value = date, onValueChange = { date = it.take(10) }, label = { Text(WebI18n.tr("التاريخ", "Date", lang)) }, modifier = Modifier.fillMaxWidth().testTag("transaction_date_input"), singleLine = true)
            FinanceTextField(value = description, onValueChange = { description = it.take(200) }, label = { Text(WebI18n.tr("الوصف", "Description", lang)) }, modifier = Modifier.fillMaxWidth().testTag("transaction_description_input"), singleLine = true)
            Text(WebI18n.tr("الوصف اختياري، بحد أقصى 200 حرفاً.", "Description is optional, up to 200 characters.", lang), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("transaction_form_error"))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FinanceOutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text(WebI18n.tr("إلغاء", "Cancel", lang)) }
                FinanceButton(onClick = {
                    val numeric = amount.toDoubleOrNull()
                    val parsedDate = runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(date)?.time }.getOrNull()
                    error = when {
                        account == null -> WebI18n.tr("أضف حساباً نشطاً أولاً من قسم الحسابات.", "Add an active account in Accounts first.", lang)
                        numeric == null || !numeric.isFinite() || numeric <= 0.0 -> WebI18n.tr("أدخل مبلغاً أكبر من الصفر.", "Enter an amount greater than zero.", lang)
                        parsedDate == null -> WebI18n.tr("أدخل تاريخاً صحيحاً بصيغة YYYY-MM-DD.", "Enter a valid date in YYYY-MM-DD format.", lang)
                        else -> ""
                    }
                    if (error.isBlank() && account != null && numeric != null && parsedDate != null) {
                        onSave(TransactionEntity(accountId = account.id, type = type, amount = numeric, currency = account.currency, category = category.ifBlank { "Other" }, date = parsedDate, description = description.trim().ifBlank { txTypeLabel(type, lang) }, status = TransactionStatus.COMPLETED))
                    }
                }, modifier = Modifier.weight(1f).testTag("submit_transaction_btn")) { Text(WebI18n.tr("حفظ المعاملة", "Save transaction", lang)) }
            }
        }
    }
}

@Composable
fun TransactionItemCard(transaction: TransactionEntity, account: AccountEntity?, lang: AppLanguage, onDelete: () -> Unit, onToggleStatus: () -> Unit) {
    val incomeLike = transaction.type == TransactionType.INCOME || transaction.type == TransactionType.REFUND || transaction.type == TransactionType.ADJUSTMENT
    val color = if (transaction.type == TransactionType.EXPENSE) ByeProTheme.colors.financialRed else ByeProTheme.colors.financialGreen
    val sign = if (transaction.type == TransactionType.EXPENSE) "−" else "+"
    val dateText = runCatching { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(transaction.date)) }.getOrDefault("")
    Surface(Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .25f), RoundedCornerShape(14.dp)), color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(color.copy(alpha = .15f)), contentAlignment = Alignment.Center) { Icon(if (incomeLike) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward, contentDescription = null, tint = color) }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(transaction.description, fontWeight = FontWeight.Bold, maxLines = 1)
                Text("${account?.name ?: WebI18n.tr("حساب", "Account", lang)} · $dateText", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("$sign${TextContent.money(transaction.amount, transaction.currency, lang)}", fontWeight = FontWeight.Bold, color = color)
                Text(statusLabel(transaction.status, lang), style = MaterialTheme.typography.labelSmall)
                if (transaction.status != TransactionStatus.CANCELLED) Text(if (transaction.status == TransactionStatus.COMPLETED) WebI18n.tr("تعليق", "Set pending", lang) else WebI18n.tr("إكمال", "Complete", lang), style = MaterialTheme.typography.labelSmall, color = ByeProTheme.colors.goldPrimary, modifier = Modifier.testTag("toggle_tx_status_${transaction.id}").clip(RoundedCornerShape(6.dp)).background(ByeProTheme.colors.goldPrimary.copy(alpha = .08f)).clickable(onClick = onToggleStatus).padding(4.dp))
            }
            FinanceIconButton(onClick = onDelete, modifier = Modifier.testTag("delete_tx_${transaction.id}")) { Icon(Icons.Default.Delete, contentDescription = WebI18n.tr("حذف", "Delete", lang), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .6f)) }
        }
    }
}
