package com.example.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.finance.engine.WorkLedgerCalculator
import com.example.finance.model.*
import com.example.finance.ui.FinanceViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun WorkScreen(viewModel: FinanceViewModel) {
    val sources by viewModel.workSources.collectAsState()
    val records by viewModel.workRecords.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val lang by viewModel.appLanguage.collectAsState()
    val currency by viewModel.primaryCurrency.collectAsState()
    val ar = lang == AppLanguage.AR
    var selectedScope by remember { mutableStateOf(WorkSourceType.EMPLOYMENT) }
    var sourceDialog by remember { mutableStateOf(false) }
    var editingSource by remember { mutableStateOf<WorkSourceEntity?>(null) }
    var recordDialog by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<WorkRecordEntity?>(null) }
    var transferDialog by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    var deleteRecordId by remember { mutableStateOf<Long?>(null) }
    var reportPeriod by remember { mutableStateOf("month") }
    var searchQuery by remember { mutableStateOf("") }
    val scopedSources = sources.filter { it.type == selectedScope }
    val scopedRecords = records.filter { it.scope == selectedScope }
    val query = searchQuery.trim()
    val visibleSources = scopedSources.filter { query.isBlank() || it.name.contains(query, true) }
    val visibleRecords = scopedRecords.filter { query.isBlank() || it.notes.contains(query, true) || workRecordLabel(it.type, ar).contains(query, true) || it.amount.toString().contains(query) }
    val period = java.time.YearMonth.now().toString()
    val currentDate = java.time.LocalDate.now()
    val money = remember(lang) { NumberFormat.getNumberInstance(if (ar) Locale("ar") else Locale.ENGLISH) }
    val salaryDue = if (selectedScope == WorkSourceType.EMPLOYMENT) visibleSources.filter { it.currency.equals(currency, true) }.sumOf { WorkLedgerCalculator.salaryDue(it, records, period).toDouble() } else 0.0
    val freelanceDue = if (selectedScope == WorkSourceType.FREELANCE) scopedRecords.filter { it.type == WorkRecordType.FREELANCE_INCOME && it.currency.equals(currency, true) }.sumOf { WorkLedgerCalculator.freelanceOutstanding(it, scopedRecords).toDouble() } else 0.0
    val periodRows = scopedRecords.filter { row ->
        row.currency.equals(currency, true) && when (reportPeriod) {
            "day" -> java.time.Instant.ofEpochMilli(row.date).atZone(java.time.ZoneId.systemDefault()).toLocalDate() == currentDate
            "year" -> java.time.Instant.ofEpochMilli(row.date).atZone(java.time.ZoneId.systemDefault()).year == currentDate.year
            else -> row.period == period
        }
    }
    val received = periodRows.filter { it.type in setOf(WorkRecordType.WORK_INCOME, WorkRecordType.SALARY_ADVANCE, WorkRecordType.SALARY_PAYMENT, WorkRecordType.FREELANCE_PAYMENT) }.sumOf { it.amount }
    val expenses = periodRows.filter { it.type == WorkRecordType.FREELANCE_EXPENSE || it.type == WorkRecordType.WORK_EXPENSE }.sumOf { it.amount }
    val netProfit = received - expenses
    val balance = WorkLedgerCalculator.cashBalance(scopedRecords, currency, selectedScope).toDouble()
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(if (ar) "العمل" else "Work", style = MaterialTheme.typography.headlineSmall)
        Text(if (ar) "اختر نوع العمل لإدارة سجلاته وبياناته بشكل مستقل" else "Choose a work type to manage its records independently", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { selectedScope = WorkSourceType.EMPLOYMENT }, modifier = Modifier.weight(1f), enabled = selectedScope != WorkSourceType.EMPLOYMENT) {
                Text(if (ar) "العمل الوظيفي" else "Employment")
            }
            Button(onClick = { selectedScope = WorkSourceType.FREELANCE }, modifier = Modifier.weight(1f), enabled = selectedScope != WorkSourceType.FREELANCE) {
                Text(if (ar) "العمل الحر" else "Freelance")
            }
        }
        Text(
            if (selectedScope == WorkSourceType.EMPLOYMENT) {
                if (ar) "إدارة الوظائف والرواتب والبدلات والخصومات والسلف والمستحقات" else "Manage employers, salaries, allowances, deductions, advances and receivables"
            } else {
                if (ar) "إدارة العملاء والمشاريع والإيرادات والمصروفات والمدفوعات والمستحقات" else "Manage clients, projects, income, expenses, payments and receivables"
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WorkSummary(if (ar) (if (selectedScope == WorkSourceType.EMPLOYMENT) "الراتب المستحق" else "مستحقات العملاء") else (if (selectedScope == WorkSourceType.EMPLOYMENT) "Salary due" else "Client receivables"), if (selectedScope == WorkSourceType.EMPLOYMENT) salaryDue else freelanceDue, currency, Modifier.weight(1f), money)
            WorkSummary(if (ar) "المقبوض" else "Received", received, currency, Modifier.weight(1f), money)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WorkSummary(if (ar) "المصروفات" else "Expenses", expenses, currency, Modifier.weight(1f), money)
        }
        WorkSummary(if (ar) "صافي العمل" else "Work net", netProfit, currency, Modifier.fillMaxWidth(), money)
        WorkSummary(if (ar) "رصيد العمل" else "Work cash balance", balance, currency, Modifier.fillMaxWidth(), money)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = { reportPeriod = "day" }) { Text((if (reportPeriod == "day") "✓ " else "") + (if (ar) "يومي" else "Daily")) }
            TextButton(onClick = { reportPeriod = "month" }) { Text((if (reportPeriod == "month") "✓ " else "") + (if (ar) "شهري" else "Monthly")) }
            TextButton(onClick = { reportPeriod = "year" }) { Text((if (reportPeriod == "year") "✓ " else "") + (if (ar) "سنوي" else "Yearly")) }
        }
        OutlinedTextField(searchQuery, { searchQuery = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text(if (ar) "بحث في السجلات والمصادر" else "Search sources and records") })
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(onClick = { editingSource = null; sourceDialog = true }) { Text(if (ar) (if (selectedScope == WorkSourceType.EMPLOYMENT) "إضافة جهة عمل" else "إضافة نشاط / عميل") else (if (selectedScope == WorkSourceType.EMPLOYMENT) "Add employer" else "Add activity / client")) }
            Button(onClick = { editingRecord = null; recordDialog = true }) { Text(if (ar) "تسجيل حركة" else "Add record") }
            TextButton(onClick = { transferDialog = true }) { Text(if (ar) "تحويل" else "Transfer") }
        }
        if (error) Text(if (ar) "تعذر الحفظ. تحقق من صحة البيانات." else "Could not save. Check the entered details.", color = MaterialTheme.colorScheme.error)
        Text(if (ar) (if (selectedScope == WorkSourceType.EMPLOYMENT) "جهات العمل" else "العملاء والأنشطة") else (if (selectedScope == WorkSourceType.EMPLOYMENT) "Employers" else "Clients and activities"), style = MaterialTheme.typography.titleMedium)
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (visibleSources.isEmpty()) item {
                Text(if (ar) "لا توجد جهات أو أنشطة مسجلة تطابق البحث." else "No employers or activities match this search.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(visibleSources, key = { "source" + it.id }) { source ->
                val due = if (source.type == WorkSourceType.EMPLOYMENT) WorkLedgerCalculator.salaryDue(source, records, period).toDouble() else records.filter { it.sourceId == source.id && it.type == WorkRecordType.FREELANCE_INCOME }.sumOf { WorkLedgerCalculator.freelanceOutstanding(it, records).toDouble() }
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(source.name, style = MaterialTheme.typography.titleMedium)
                        Text(if (source.type == WorkSourceType.EMPLOYMENT) (if (ar) "وظيفة" else "Employment") else (if (ar) "عمل حر" else "Freelance"))
                        if (source.type == WorkSourceType.EMPLOYMENT) Text(money.format(source.monthlySalary) + " " + source.currency)
                        Text((if (ar) "المستحق: " else "Outstanding: ") + money.format(due) + " " + source.currency)
                        if (!source.isActive) Text(if (ar) "مؤرشف" else "Archived", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { editingSource = source; sourceDialog = true }) { Text(if (ar) "تعديل" else "Edit") }
                            TextButton(onClick = { viewModel.updateWorkSource(source.copy(isActive = !source.isActive)) { if (!it) error = true } }) {
                                Text(if (source.isActive) (if (ar) "أرشفة" else "Archive") else (if (ar) "استعادة" else "Restore"))
                            }
                        }
                    }
                }
            }
            item { Text(if (ar) "آخر الحركات" else "Recent records", style = MaterialTheme.typography.titleMedium) }
            if (visibleRecords.isEmpty()) item {
                Text(if (ar) "لا توجد حركات مسجلة في هذا القسم بعد." else "No records have been added to this workspace yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(visibleRecords, key = { "record" + it.id }) { row ->
                val name = sources.firstOrNull { it.id == row.sourceId }?.name ?: (if (row.type == WorkRecordType.WORK_TRANSFER_IN || row.type == WorkRecordType.WORK_TRANSFER_OUT) (if (ar) "تحويل داخلي" else "Internal transfer") else (if (ar) "عمل عام" else "General work"))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(name + " · " + workRecordLabel(row.type, ar) + " · " + money.format(row.amount) + " " + row.currency, modifier = Modifier.weight(1f))
                    TextButton(onClick = { editingRecord = row; recordDialog = true }) { Text(if (ar) "تعديل" else "Edit") }
                    TextButton(onClick = { deleteRecordId = row.id }) { Text(if (ar) "حذف" else "Delete") }
                }
            }
        }
    }
    if (deleteRecordId != null) {
        AlertDialog(
            onDismissRequest = { deleteRecordId = null },
            title = { Text(if (ar) "حذف حركة العمل؟" else "Delete work record?") },
            text = { Text(if (ar) "سيتم حذف الحركة المرتبطة بها، وعكس أثرها على الحساب الشخصي إذا كانت مرتبطة به." else "The linked work record will be deleted and its posted effect on the personal account will be reversed.") },
            confirmButton = {
                TextButton(onClick = {
                    val id = deleteRecordId ?: return@TextButton
                    viewModel.deleteWorkRecord(id) { success ->
                        if (!success) error = true
                        deleteRecordId = null
                    }
                }) { Text(if (ar) "حذف نهائياً" else "Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteRecordId = null }) { Text(if (ar) "إلغاء" else "Cancel") } }
        )
    }
    if (sourceDialog) WorkSourceDialog(ar, currency, selectedScope, editingSource, { sourceDialog = false; editingSource = null }) { name, type, salary ->
        val existing = editingSource
        if (existing == null) {
            viewModel.addWorkSource(WorkSourceEntity(name = name, type = type, monthlySalary = salary, currency = currency)) { if (it) { sourceDialog = false; error = false } else error = true }
        } else {
            viewModel.updateWorkSource(existing.copy(name = name, monthlySalary = salary, currency = currency)) { if (it) { sourceDialog = false; editingSource = null; error = false } else error = true }
        }
    }
    if (recordDialog) WorkRecordDialog(ar, currency, selectedScope, scopedSources, scopedRecords, accounts, editingRecord, { recordDialog = false; editingRecord = null }) { row, accountId ->
        val existing = editingRecord
        if (existing == null) viewModel.addWorkRecord(row, accountId) { if (it) { recordDialog = false; error = false } else error = true }
        else viewModel.updateWorkRecord(row.copy(id = existing.id, date = existing.date, period = existing.period), accountId) { if (it) { recordDialog = false; editingRecord = null; error = false } else error = true }
    }
    if (transferDialog) WorkTransferDialog(ar, currency, selectedScope, accounts, { transferDialog = false }) { type, amount, note, accountId ->
        viewModel.addWorkRecord(WorkRecordEntity(type = type, amount = amount, currency = currency, notes = note, scope = selectedScope), accountId) { if (it) { transferDialog = false; error = false } else error = true }
    }
}
@Composable private fun WorkSummary(title: String, value: Double, currency: String, modifier: Modifier, format: NumberFormat) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(12.dp)) { Text(title, style = MaterialTheme.typography.labelMedium); Text(format.format(value) + " " + currency, style = MaterialTheme.typography.titleMedium) }
    }
}
@Composable private fun WorkSourceDialog(ar: Boolean, currency: String, initialType: WorkSourceType, existing: WorkSourceEntity?, dismiss: () -> Unit, save: (String, WorkSourceType, Double) -> Unit) {
    var name by remember(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }; var salary by remember(existing?.id) { mutableStateOf(existing?.monthlySalary?.toString() ?: "0") }; val type = existing?.type ?: initialType
    AlertDialog(onDismissRequest = dismiss, title = { Text(if (existing == null) (if (ar) "مصدر عمل جديد" else "New work source") else (if (ar) "تعديل مصدر العمل" else "Edit work source")) }, text = {
        Column { OutlinedTextField(name, { name = it }, label = { Text(if (ar) (if (type == WorkSourceType.EMPLOYMENT) "اسم جهة العمل أو الوظيفة" else "اسم العميل أو النشاط") else (if (type == WorkSourceType.EMPLOYMENT) "Employer or job name" else "Client or activity name")) })
            Text(if (ar) (if (type == WorkSourceType.EMPLOYMENT) "نوع المصدر: جهة عمل" else "نوع المصدر: نشاط عمل حر") else (if (type == WorkSourceType.EMPLOYMENT) "Source type: employer" else "Source type: freelance activity"))
            if (type == WorkSourceType.EMPLOYMENT) OutlinedTextField(salary, { salary = it }, label = { Text(if (ar) "الراتب الشهري (اختياري)" else "Monthly salary (optional)") })
        }
    }, confirmButton = {
        val amount = if (type == WorkSourceType.FREELANCE || salary.isBlank()) 0.0 else salary.toDoubleOrNull() ?: -1.0
        Button(onClick = { if (name.isNotBlank() && amount >= 0) save(name.trim(), type, amount) }, enabled = name.isNotBlank() && amount >= 0) { Text(if (ar) "حفظ" else "Save") }
    }, dismissButton = { TextButton(onClick = dismiss) { Text(if (ar) "إلغاء" else "Cancel") } })
}
@Composable private fun WorkRecordDialog(ar: Boolean, currency: String, scope: WorkSourceType, sources: List<WorkSourceEntity>, records: List<WorkRecordEntity>, accounts: List<AccountEntity>, existing: WorkRecordEntity?, dismiss: () -> Unit, save: (WorkRecordEntity, Long?) -> Unit) {
    var sourceId by remember(existing?.id, scope) { mutableStateOf(existing?.sourceId) }
    val source = sources.firstOrNull { it.id == sourceId }
    var type by remember(existing?.id, scope) { mutableStateOf(existing?.type ?: WorkRecordType.WORK_INCOME) }
    var amount by remember(existing?.id) { mutableStateOf(existing?.amount?.toString().orEmpty()) }
    var note by remember(existing?.id) { mutableStateOf(existing?.notes.orEmpty()) }
    var period by remember(existing?.id) { mutableStateOf(existing?.period ?: java.time.YearMonth.now().toString()) }
    var related by remember(existing?.id) { mutableStateOf(existing?.relatedRecordId) }
    var currencyCode by remember(existing?.id, sourceId, currency) { mutableStateOf(source?.currency ?: existing?.currency ?: currency) }
    val options = buildList {
        add(WorkRecordType.WORK_INCOME)
        add(WorkRecordType.WORK_EXPENSE)
        if (scope == WorkSourceType.FREELANCE && (source == null || source.type == WorkSourceType.FREELANCE)) add(WorkRecordType.FREELANCE_EXPENSE)
        if (scope == WorkSourceType.EMPLOYMENT) {
            addAll(listOf(WorkRecordType.SALARY_ADVANCE, WorkRecordType.SALARY_DEDUCTION, WorkRecordType.SALARY_ALLOWANCE, WorkRecordType.SALARY_BONUS, WorkRecordType.SALARY_OVERTIME, WorkRecordType.SALARY_PAYMENT))
        }
        if (scope == WorkSourceType.FREELANCE) {
            addAll(listOf(WorkRecordType.FREELANCE_INCOME, WorkRecordType.FREELANCE_PAYMENT))
        }
        existing?.type?.takeIf { it == WorkRecordType.WORK_TRANSFER_IN || it == WorkRecordType.WORK_TRANSFER_OUT }?.let { add(it) }
    }
    val cash = WorkLedgerCalculator.isCashMovement(type)
    val selectedCurrency = (source?.currency ?: currencyCode).trim().uppercase()
    val matching = accounts.filter { it.currency.equals(selectedCurrency, true) }
    var accountId by remember(selectedCurrency, existing?.id) { mutableStateOf(existing?.linkedAccountId) }
    val receivables = records.filter {
        it.type == WorkRecordType.FREELANCE_INCOME && it.sourceId == sourceId &&
            WorkLedgerCalculator.freelanceOutstanding(it, records).toDouble() > 0.000001
    }
    AlertDialog(onDismissRequest = dismiss, title = { Text(if (existing == null) (if (ar) "تسجيل حركة عمل" else "Add work record") else (if (ar) "تعديل حركة العمل" else "Edit work record")) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(if (ar) "المصدر (اختياري)" else "Source (optional)")
            TextButton(onClick = { sourceId = null; type = WorkRecordType.WORK_INCOME; related = null; accountId = null; currencyCode = currency }) {
                Text((if (sourceId == null) "✓ " else "") + if (ar) "تسجيل عام بدون مصدر" else "General record without source")
            }
            sources.filter { it.isActive }.forEach { item ->
                TextButton(onClick = {
                    sourceId = item.id
                    currencyCode = item.currency
                    type = if (item.type == WorkSourceType.EMPLOYMENT) WorkRecordType.SALARY_ADVANCE else WorkRecordType.FREELANCE_INCOME
                    related = null
                    accountId = null
                }) {
                    Text((if (sourceId == item.id) "✓ " else "") + item.name)
                }
            }
            Text(if (ar) "نوع الحركة" else "Record type")
            options.forEach { option ->
                TextButton(onClick = { type = option; related = null }) {
                    Text((if (type == option) "✓ " else "") + workRecordLabel(option, ar))
                }
            }
            if (source == null) {
                OutlinedTextField(currencyCode, { currencyCode = it.uppercase().take(8) }, label = { Text(if (ar) "رمز العملة" else "Currency code") }, singleLine = true)
            }
            if (type == WorkRecordType.FREELANCE_PAYMENT) {
                Text(if (ar) "المستحق المرتبط بالدفعة" else "Receivable linked to this payment")
                receivables.forEach { row ->
                    TextButton(onClick = { related = row.id }) {
                        Text((if (related == row.id) "✓ " else "") + NumberFormat.getNumberInstance().format(WorkLedgerCalculator.freelanceOutstanding(row, records).toDouble()) + " · " + row.notes)
                    }
                }
            }
            OutlinedTextField(amount, { amount = it }, label = { Text(if (ar) "المبلغ" else "Amount") }, singleLine = true)
            OutlinedTextField(period, { period = it.take(7) }, label = { Text(if (ar) "الفترة (YYYY-MM)" else "Period (YYYY-MM)") }, singleLine = true)
            OutlinedTextField(note, { note = it }, label = { Text(if (ar) "الوصف / الملاحظات" else "Description / notes") })
            if (cash) {
                Text(if (ar) "ربط اختياري بحساب شخصي" else "Optional link to a personal account")
                TextButton(onClick = { accountId = null }) {
                    Text((if (accountId == null) "✓ " else "") + if (ar) "حفظ في سجل العمل فقط" else "Save only in work ledger")
                }
                matching.forEach { row ->
                    TextButton(onClick = { accountId = row.id }) {
                        Text((if (accountId == row.id) "✓ " else "") + row.name + " · " + row.currency)
                    }
                }
            } else {
                Text(if (ar) "سيُسجل كمستحق دون تغيير رصيد الحساب." else "Saved as an amount due; personal account balance is unchanged.")
            }
        }
    }, confirmButton = {
        val value = amount.toDoubleOrNull()
        val needsSource = type in setOf(
            WorkRecordType.SALARY_ADVANCE, WorkRecordType.SALARY_DEDUCTION,
            WorkRecordType.SALARY_ALLOWANCE, WorkRecordType.SALARY_BONUS, WorkRecordType.SALARY_OVERTIME,
            WorkRecordType.SALARY_PAYMENT, WorkRecordType.FREELANCE_INCOME,
            WorkRecordType.FREELANCE_PAYMENT
        )
        Button(
            onClick = {
                if (value != null && value > 0 && (!needsSource || source != null) &&
                    (type != WorkRecordType.FREELANCE_PAYMENT || related != null)) {
                    save(
                        WorkRecordEntity(
                            id = existing?.id ?: 0L,
                            sourceId = sourceId,
                            scope = scope,
                            relatedRecordId = related,
                            type = type,
                            amount = value,
                            currency = selectedCurrency,
                            date = existing?.date ?: System.currentTimeMillis(),
                            period = period.trim(),
                            notes = note.trim()
                        ),
                        if (cash) accountId else null
                    )
                }
            },
            enabled = value != null && value.isFinite() && value > 0 && currencyCode.isNotBlank() &&
                period.trim().matches(Regex("\\d{4}-\\d{2}")) && (!needsSource || source != null) && (type != WorkRecordType.FREELANCE_PAYMENT || related != null)
        ) { Text(if (ar) "حفظ" else "Save") }
    }, dismissButton = { TextButton(onClick = dismiss) { Text(if (ar) "إلغاء" else "Cancel") } })
}
@Composable private fun WorkTransferDialog(ar: Boolean, currency: String, scope: WorkSourceType, accounts: List<AccountEntity>, dismiss: () -> Unit, save: (WorkRecordType, Double, String, Long?) -> Unit) {
    var type by remember { mutableStateOf(WorkRecordType.WORK_TRANSFER_IN) }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val matching = accounts.filter { it.currency.equals(currency, true) }
    var accountId by remember { mutableStateOf<Long?>(null) }
    AlertDialog(onDismissRequest = dismiss, title = { Text(if (ar) "تحويل بين الشخصي والعمل" else "Personal ↔ Work transfer") }, text = {
        Column {
            TextButton(onClick = { type = WorkRecordType.WORK_TRANSFER_IN }) { Text((if (type == WorkRecordType.WORK_TRANSFER_IN) "✓ " else "") + (if (ar) "من الشخصي إلى العمل" else "Personal to work")) }
            TextButton(onClick = { type = WorkRecordType.WORK_TRANSFER_OUT }) { Text((if (type == WorkRecordType.WORK_TRANSFER_OUT) "✓ " else "") + (if (ar) "من العمل إلى الشخصي" else "Work to personal")) }
            OutlinedTextField(amount, { amount = it }, label = { Text(if (ar) "المبلغ" else "Amount") }, singleLine = true)
            OutlinedTextField(note, { note = it }, label = { Text(if (ar) "ملاحظات" else "Notes") })
            TextButton(onClick = { accountId = null }) { Text((if (accountId == null) "✓ " else "") + if (ar) "بدون ربط بحساب شخصي" else "No personal account link") }
            matching.forEach { row -> TextButton(onClick = { accountId = row.id }) { Text((if (accountId == row.id) "✓ " else "") + row.name) } }
        }
    }, confirmButton = {
        val value = amount.toDoubleOrNull()
        Button(onClick = { if (value != null && value > 0) save(type, value, note, accountId) }, enabled = value != null && value > 0) { Text(if (ar) "تأكيد" else "Confirm") }
    }, dismissButton = { TextButton(onClick = dismiss) { Text(if (ar) "إلغاء" else "Cancel") } })
}

private fun workRecordLabel(type: WorkRecordType, ar: Boolean): String = when (type) {
    WorkRecordType.WORK_INCOME -> if (ar) "دخل عمل مباشر" else "Direct work income"
    WorkRecordType.SALARY_ADVANCE -> if (ar) "سلفة راتب مستلمة" else "Salary advance received"
    WorkRecordType.SALARY_DEDUCTION -> if (ar) "خصم راتب" else "Salary deduction"
    WorkRecordType.SALARY_ALLOWANCE -> if (ar) "بدل وظيفي مستحق" else "Employment allowance accrued"
    WorkRecordType.SALARY_BONUS -> if (ar) "مكافأة مستحقة" else "Bonus accrued"
    WorkRecordType.SALARY_OVERTIME -> if (ar) "عمل إضافي مستحق" else "Overtime accrued"
    WorkRecordType.SALARY_PAYMENT -> if (ar) "دفعة راتب مستلمة" else "Salary payment received"
    WorkRecordType.FREELANCE_INCOME -> if (ar) "دخل عمل حر مستحق" else "Freelance receivable"
    WorkRecordType.FREELANCE_PAYMENT -> if (ar) "دفعة عمل حر مستلمة" else "Freelance payment received"
    WorkRecordType.FREELANCE_EXPENSE -> if (ar) "مصروف عمل حر" else "Freelance expense"
    WorkRecordType.WORK_EXPENSE -> if (ar) "مصروف عمل" else "Work expense"
    WorkRecordType.WORK_TRANSFER_IN -> if (ar) "تحويل إلى رصيد العمل" else "Transfer into work"
    WorkRecordType.WORK_TRANSFER_OUT -> if (ar) "تحويل إلى الحساب الشخصي" else "Transfer to personal"
}
