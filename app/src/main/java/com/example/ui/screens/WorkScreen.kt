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
    var sourceDialog by remember { mutableStateOf(false) }
    var recordDialog by remember { mutableStateOf(false) }
    var transferDialog by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    var deleteRecordId by remember { mutableStateOf<Long?>(null) }
    var reportPeriod by remember { mutableStateOf("month") }
    val period = java.time.YearMonth.now().toString()
    val currentDate = java.time.LocalDate.now()
    val money = remember(lang) { NumberFormat.getNumberInstance(if (ar) Locale("ar") else Locale.ENGLISH) }
    val salaryDue = sources.filter { it.type == WorkSourceType.EMPLOYMENT && it.currency.equals(currency, true) }.sumOf { WorkLedgerCalculator.salaryDue(it, records, period).toDouble() }
    val freelanceDue = records.filter { it.type == WorkRecordType.FREELANCE_INCOME && it.currency.equals(currency, true) }.sumOf { WorkLedgerCalculator.freelanceOutstanding(it, records).toDouble() }
    val periodRows = records.filter { row ->
        row.currency.equals(currency, true) && when (reportPeriod) {
            "day" -> java.time.Instant.ofEpochMilli(row.date).atZone(java.time.ZoneId.systemDefault()).toLocalDate() == currentDate
            "year" -> java.time.Instant.ofEpochMilli(row.date).atZone(java.time.ZoneId.systemDefault()).year == currentDate.year
            else -> row.period == period
        }
    }
    val received = periodRows.filter { it.type in setOf(WorkRecordType.WORK_INCOME, WorkRecordType.SALARY_ADVANCE, WorkRecordType.SALARY_PAYMENT, WorkRecordType.FREELANCE_PAYMENT) }.sumOf { it.amount }
    val expenses = periodRows.filter { it.type == WorkRecordType.FREELANCE_EXPENSE || it.type == WorkRecordType.WORK_EXPENSE }.sumOf { it.amount }
    val netProfit = received - expenses
    val balance = WorkLedgerCalculator.cashBalance(records, currency).toDouble()
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(if (ar) "العمل والدخل" else "Work & Income", style = MaterialTheme.typography.headlineSmall)
        Text(if (ar) "وظائف متعددة وأنشطة عمل حر، مع فصل المستحقات عن المقبوض الفعلي" else "Multiple jobs and freelance activities; amounts due stay separate from actual receipts", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WorkSummary(if (ar) "الراتب المستحق" else "Salary due", salaryDue, currency, Modifier.weight(1f), money)
            WorkSummary(if (ar) "مستحقات العمل الحر" else "Freelance due", freelanceDue, currency, Modifier.weight(1f), money)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WorkSummary(if (ar) "المقبوض" else "Received", received, currency, Modifier.weight(1f), money)
            WorkSummary(if (ar) "المصروفات" else "Expenses", expenses, currency, Modifier.weight(1f), money)
        }
        WorkSummary(if (ar) "صافي العمل" else "Work net", netProfit, currency, Modifier.fillMaxWidth(), money)
        WorkSummary(if (ar) "رصيد العمل" else "Work cash balance", balance, currency, Modifier.fillMaxWidth(), money)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = { reportPeriod = "day" }) { Text((if (reportPeriod == "day") "✓ " else "") + (if (ar) "يومي" else "Daily")) }
            TextButton(onClick = { reportPeriod = "month" }) { Text((if (reportPeriod == "month") "✓ " else "") + (if (ar) "شهري" else "Monthly")) }
            TextButton(onClick = { reportPeriod = "year" }) { Text((if (reportPeriod == "year") "✓ " else "") + (if (ar) "سنوي" else "Yearly")) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(onClick = { sourceDialog = true }) { Text(if (ar) "إضافة مصدر" else "Add source") }
            Button(onClick = { recordDialog = true }) { Text(if (ar) "تسجيل حركة" else "Add record") }
            TextButton(onClick = { transferDialog = true }) { Text(if (ar) "تحويل" else "Transfer") }
        }
        if (error) Text(if (ar) "تعذر الحفظ. تحقق من صحة البيانات." else "Could not save. Check the entered details.", color = MaterialTheme.colorScheme.error)
        Text(if (ar) "مصادر الدخل" else "Income sources", style = MaterialTheme.typography.titleMedium)
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(sources, key = { "source" + it.id }) { source ->
                val due = if (source.type == WorkSourceType.EMPLOYMENT) WorkLedgerCalculator.salaryDue(source, records, period).toDouble() else records.filter { it.sourceId == source.id && it.type == WorkRecordType.FREELANCE_INCOME }.sumOf { WorkLedgerCalculator.freelanceOutstanding(it, records).toDouble() }
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(source.name, style = MaterialTheme.typography.titleMedium)
                        Text(if (source.type == WorkSourceType.EMPLOYMENT) (if (ar) "وظيفة" else "Employment") else (if (ar) "عمل حر" else "Freelance"))
                        if (source.type == WorkSourceType.EMPLOYMENT) Text(money.format(source.monthlySalary) + " " + source.currency)
                        Text((if (ar) "المستحق: " else "Outstanding: ") + money.format(due) + " " + source.currency)
                    }
                }
            }
            item { Text(if (ar) "آخر الحركات" else "Recent records", style = MaterialTheme.typography.titleMedium) }
            items(records.take(25), key = { "record" + it.id }) { row ->
                val name = sources.firstOrNull { it.id == row.sourceId }?.name ?: (if (row.type == WorkRecordType.WORK_TRANSFER_IN || row.type == WorkRecordType.WORK_TRANSFER_OUT) (if (ar) "تحويل داخلي" else "Internal transfer") else (if (ar) "عمل عام" else "General work"))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(name + " · " + workRecordLabel(row.type, ar) + " · " + money.format(row.amount) + " " + row.currency, modifier = Modifier.weight(1f))
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
    if (sourceDialog) WorkSourceDialog(ar, currency, { sourceDialog = false }) { name, type, salary ->
        viewModel.addWorkSource(WorkSourceEntity(name = name, type = type, monthlySalary = salary, currency = currency)) { if (it) sourceDialog = false else error = true }
    }
    if (recordDialog) WorkRecordDialog(ar, currency, sources, records, accounts, { recordDialog = false }) { row, accountId ->
        viewModel.addWorkRecord(row, accountId) { if (it) { recordDialog = false; error = false } else error = true }
    }
    if (transferDialog) WorkTransferDialog(ar, currency, accounts, { transferDialog = false }) { type, amount, note, accountId ->
        viewModel.addWorkRecord(WorkRecordEntity(type = type, amount = amount, currency = currency, notes = note), accountId) { if (it) { transferDialog = false; error = false } else error = true }
    }
}
@Composable private fun WorkSummary(title: String, value: Double, currency: String, modifier: Modifier, format: NumberFormat) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(12.dp)) { Text(title, style = MaterialTheme.typography.labelMedium); Text(format.format(value) + " " + currency, style = MaterialTheme.typography.titleMedium) }
    }
}
@Composable private fun WorkSourceDialog(ar: Boolean, currency: String, dismiss: () -> Unit, save: (String, WorkSourceType, Double) -> Unit) {
    var name by remember { mutableStateOf("") }; var salary by remember { mutableStateOf("0") }; var type by remember { mutableStateOf(WorkSourceType.EMPLOYMENT) }
    AlertDialog(onDismissRequest = dismiss, title = { Text(if (ar) "مصدر دخل جديد" else "New income source") }, text = {
        Column { OutlinedTextField(name, { name = it }, label = { Text(if (ar) "اسم الوظيفة أو النشاط" else "Job or activity name") })
            Row { TextButton(onClick = { type = WorkSourceType.EMPLOYMENT }) { Text(if (ar) "وظيفة" else "Employment") }; TextButton(onClick = { type = WorkSourceType.FREELANCE }) { Text(if (ar) "عمل حر" else "Freelance") } }
            if (type == WorkSourceType.EMPLOYMENT) OutlinedTextField(salary, { salary = it }, label = { Text(if (ar) "الراتب الشهري (اختياري)" else "Monthly salary (optional)") })
        }
    }, confirmButton = {
        val amount = if (type == WorkSourceType.FREELANCE || salary.isBlank()) 0.0 else salary.toDoubleOrNull() ?: -1.0
        Button(onClick = { if (name.isNotBlank() && amount >= 0) save(name.trim(), type, amount) }, enabled = name.isNotBlank() && amount >= 0) { Text(if (ar) "حفظ" else "Save") }
    }, dismissButton = { TextButton(onClick = dismiss) { Text(if (ar) "إلغاء" else "Cancel") } })
}
@Composable private fun WorkRecordDialog(ar: Boolean, currency: String, sources: List<WorkSourceEntity>, records: List<WorkRecordEntity>, accounts: List<AccountEntity>, dismiss: () -> Unit, save: (WorkRecordEntity, Long?) -> Unit) {
    var sourceId by remember { mutableStateOf<Long?>(null) }
    val source = sources.firstOrNull { it.id == sourceId }
    var type by remember { mutableStateOf(WorkRecordType.WORK_INCOME) }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var related by remember { mutableStateOf<Long?>(null) }
    var currencyCode by remember(sourceId, currency) { mutableStateOf(source?.currency ?: currency) }
    val options = buildList {
        add(WorkRecordType.WORK_INCOME)
        add(WorkRecordType.WORK_EXPENSE)
        if (source == null || source.type == WorkSourceType.FREELANCE) add(WorkRecordType.FREELANCE_EXPENSE)
        if (source?.type == WorkSourceType.EMPLOYMENT) {
            addAll(listOf(WorkRecordType.SALARY_ADVANCE, WorkRecordType.SALARY_DEDUCTION, WorkRecordType.SALARY_BONUS, WorkRecordType.SALARY_OVERTIME, WorkRecordType.SALARY_PAYMENT))
        }
        if (source?.type == WorkSourceType.FREELANCE) {
            addAll(listOf(WorkRecordType.FREELANCE_INCOME, WorkRecordType.FREELANCE_PAYMENT))
        }
    }
    val cash = WorkLedgerCalculator.isCashMovement(type)
    val selectedCurrency = (source?.currency ?: currencyCode).trim().uppercase()
    val matching = accounts.filter { it.currency.equals(selectedCurrency, true) }
    var accountId by remember(selectedCurrency) { mutableStateOf<Long?>(null) }
    val receivables = records.filter {
        it.type == WorkRecordType.FREELANCE_INCOME && it.sourceId == sourceId &&
            WorkLedgerCalculator.freelanceOutstanding(it, records).toDouble() > 0.000001
    }
    AlertDialog(onDismissRequest = dismiss, title = { Text(if (ar) "تسجيل حركة عمل" else "Add work record") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(if (ar) "المصدر (اختياري)" else "Source (optional)")
            TextButton(onClick = { sourceId = null; type = WorkRecordType.WORK_INCOME; related = null; accountId = null }) {
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
            WorkRecordType.SALARY_BONUS, WorkRecordType.SALARY_OVERTIME,
            WorkRecordType.SALARY_PAYMENT, WorkRecordType.FREELANCE_INCOME,
            WorkRecordType.FREELANCE_PAYMENT
        )
        Button(
            onClick = {
                if (value != null && value > 0 && (!needsSource || source != null) &&
                    (type != WorkRecordType.FREELANCE_PAYMENT || related != null)) {
                    save(
                        WorkRecordEntity(
                            sourceId = sourceId,
                            relatedRecordId = related,
                            type = type,
                            amount = value,
                            currency = selectedCurrency,
                            period = java.time.YearMonth.now().toString(),
                            notes = note.trim()
                        ),
                        if (cash) accountId else null
                    )
                }
            },
            enabled = value != null && value > 0 && currencyCode.isNotBlank() &&
                (!needsSource || source != null) && (type != WorkRecordType.FREELANCE_PAYMENT || related != null)
        ) { Text(if (ar) "حفظ" else "Save") }
    }, dismissButton = { TextButton(onClick = dismiss) { Text(if (ar) "إلغاء" else "Cancel") } })
}
@Composable private fun WorkTransferDialog(ar: Boolean, currency: String, accounts: List<AccountEntity>, dismiss: () -> Unit, save: (WorkRecordType, Double, String, Long?) -> Unit) {
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
