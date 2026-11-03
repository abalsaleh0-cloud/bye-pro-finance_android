package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.finance.i18n.WebI18n
import com.example.finance.model.AppLanguage
import com.example.finance.model.WorkRecordEntity
import com.example.finance.model.WorkRecordType
import com.example.finance.model.WorkSourceEntity
import com.example.finance.model.WorkSourceType
import com.example.finance.ui.FinanceViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun WorkScreen(viewModel: FinanceViewModel) {
    val sources by viewModel.workSources.collectAsState()
    val records by viewModel.workRecords.collectAsState()
    val language by viewModel.appLanguage.collectAsState()
    val currency by viewModel.primaryCurrency.collectAsState()
    val ar = language == AppLanguage.AR
    var showSourceDialog by remember { mutableStateOf(false) }
    var showRecordDialog by remember { mutableStateOf(false) }
    val money = remember(language) { NumberFormat.getNumberInstance(if (ar) Locale("ar") else Locale.ENGLISH) }

    val employmentIds = sources.filter { it.type == WorkSourceType.EMPLOYMENT }.map { it.id }.toSet()
    val freelanceIds = sources.filter { it.type == WorkSourceType.FREELANCE }.map { it.id }.toSet()
    val salary = sources.filter { it.type == WorkSourceType.EMPLOYMENT }.sumOf { it.monthlySalary }
    val advances = records.filter { it.type == WorkRecordType.ADVANCE && it.sourceId in employmentIds }.sumOf { it.amount }
    val deductions = records.filter { it.type == WorkRecordType.DEDUCTION && it.sourceId in employmentIds }.sumOf { it.amount }
    val freelanceIncome = records.filter { it.type == WorkRecordType.FREELANCE_INCOME && it.sourceId in freelanceIds }.sumOf { it.amount }
    val freelanceExpenses = records.filter { it.type == WorkRecordType.FREELANCE_EXPENSE && it.sourceId in freelanceIds }.sumOf { it.amount }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (ar) "العمل والدخل" else "Work & Income", style = MaterialTheme.typography.headlineSmall)
        Text(if (ar) "إدارة الوظائف والراتب والعمل الحر بشكل مستقل" else "Manage employment and freelance income separately", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryCard(if (ar) "الراتب الشهري" else "Monthly salary", salary, currency, Modifier.weight(1f), money)
            SummaryCard(if (ar) "السلف" else "Advances", advances, currency, Modifier.weight(1f), money)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryCard(if (ar) "دخل العمل الحر" else "Freelance income", freelanceIncome, currency, Modifier.weight(1f), money)
            SummaryCard(if (ar) "مصروفات العمل الحر" else "Freelance expenses", freelanceExpenses, currency, Modifier.weight(1f), money)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { showSourceDialog = true }) { Text(if (ar) "إضافة مصدر دخل" else "Add income source") }
            Button(onClick = { showRecordDialog = true }, enabled = sources.isNotEmpty()) { Text(if (ar) "تسجيل حركة" else "Add record") }
        }
        Text(if (ar) "مصادر الدخل" else "Income sources", style = MaterialTheme.typography.titleMedium)
        if (sources.isEmpty()) {
            Text(if (ar) "أضف وظيفتك أو نشاط العمل الحر للبدء." else "Add an employment or freelance source to get started.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(sources, key = { "source-${it.id}" }) { source ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(source.name, style = MaterialTheme.typography.titleMedium)
                        Text(if (source.type == WorkSourceType.EMPLOYMENT) (if (ar) "وظيفة · راتب شهري" else "Employment · Monthly salary") else (if (ar) "عمل حر" else "Freelance"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (source.type == WorkSourceType.EMPLOYMENT) Text("${money.format(source.monthlySalary)} ${source.currency}")
                    }
                }
            }
        }
        Text(if (ar) "آخر الحركات" else "Recent records", style = MaterialTheme.typography.titleMedium)
        records.take(20).forEach { record ->
            val sourceName = sources.firstOrNull { it.id == record.sourceId }?.name ?: ""
            Text("${sourceName} · ${recordLabel(record.type, ar)} · ${money.format(record.amount)} ${record.currency}", style = MaterialTheme.typography.bodyMedium)
        }
    }

    if (showSourceDialog) {
        AddWorkSourceDialog(ar, currency, onDismiss = { showSourceDialog = false }) { name, type, salaryAmount ->
            viewModel.addWorkSource(WorkSourceEntity(name = name, type = type, monthlySalary = salaryAmount, currency = currency))
            showSourceDialog = false
        }
    }
    if (showRecordDialog) {
        AddWorkRecordDialog(ar, currency, sources, onDismiss = { showRecordDialog = false }) { sourceId, type, amount, note ->
            viewModel.addWorkRecord(WorkRecordEntity(sourceId = sourceId, type = type, amount = amount, currency = currency, notes = note))
            showRecordDialog = false
        }
    }
}

@Composable
private fun SummaryCard(title: String, amount: Double, currency: String, modifier: Modifier, format: NumberFormat) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(4.dp))
            Text("${format.format(amount)} $currency", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun AddWorkSourceDialog(ar: Boolean, currency: String, onDismiss: () -> Unit, onSave: (String, WorkSourceType, Double) -> Unit) {
    var name by remember { mutableStateOf("") }
    var salary by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(WorkSourceType.EMPLOYMENT) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (ar) "مصدر دخل جديد" else "New income source") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text(if (ar) "اسم الوظيفة أو النشاط" else "Job or activity name") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { type = WorkSourceType.EMPLOYMENT }) { Text(if (ar) "وظيفة" else "Employment") }
                    TextButton(onClick = { type = WorkSourceType.FREELANCE }) { Text(if (ar) "عمل حر" else "Freelance") }
                }
                if (type == WorkSourceType.EMPLOYMENT) OutlinedTextField(salary, { salary = it }, label = { Text(if (ar) "الراتب الشهري" else "Monthly salary") }, singleLine = true)
            }
        },
        confirmButton = { Button(onClick = { val value = salary.toDoubleOrNull() ?: 0.0; if (name.isNotBlank() && value >= 0) onSave(name.trim(), type, value) }, enabled = name.isNotBlank() && (type == WorkSourceType.FREELANCE || (salary.toDoubleOrNull() ?: -1.0) >= 0)) { Text(if (ar) "حفظ" else "Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(if (ar) "إلغاء" else "Cancel") } }
    )
}

@Composable
private fun AddWorkRecordDialog(ar: Boolean, currency: String, sources: List<WorkSourceEntity>, onDismiss: () -> Unit, onSave: (Long, WorkRecordType, Double, String) -> Unit) {
    var sourceId by remember(sources) { mutableStateOf(sources.firstOrNull()?.id ?: 0L) }
    var type by remember { mutableStateOf(WorkRecordType.ADVANCE) }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val selected = sources.firstOrNull { it.id == sourceId }
    val choices = if (selected?.type == WorkSourceType.EMPLOYMENT) listOf(WorkRecordType.ADVANCE, WorkRecordType.DEDUCTION, WorkRecordType.BONUS, WorkRecordType.OVERTIME, WorkRecordType.SALARY_PAYMENT) else listOf(WorkRecordType.FREELANCE_INCOME, WorkRecordType.FREELANCE_EXPENSE)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (ar) "تسجيل حركة عمل" else "Add work record") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                sources.forEach { source -> TextButton(onClick = { sourceId = source.id; type = if (source.type == WorkSourceType.EMPLOYMENT) WorkRecordType.ADVANCE else WorkRecordType.FREELANCE_INCOME }) { Text(source.name) } }
                choices.forEach { choice -> TextButton(onClick = { type = choice }) { Text(recordLabel(choice, ar)) } }
                OutlinedTextField(amount, { amount = it }, label = { Text(if (ar) "المبلغ" else "Amount") }, singleLine = true)
                OutlinedTextField(note, { note = it }, label = { Text(if (ar) "ملاحظات" else "Notes") })
            }
        },
        confirmButton = { Button(onClick = { val value = amount.toDoubleOrNull(); if (value != null && value > 0 && selected != null && type in choices) onSave(sourceId, type, value, note) }, enabled = amount.toDoubleOrNull()?.let { it > 0 } == true && selected != null && type in choices) { Text(if (ar) "حفظ" else "Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(if (ar) "إلغاء" else "Cancel") } }
    )
}

private fun recordLabel(type: WorkRecordType, ar: Boolean): String = when (type) {
    WorkRecordType.ADVANCE -> if (ar) "سلفة" else "Advance"
    WorkRecordType.DEDUCTION -> if (ar) "خصم" else "Deduction"
    WorkRecordType.BONUS -> if (ar) "مكافأة" else "Bonus"
    WorkRecordType.OVERTIME -> if (ar) "عمل إضافي" else "Overtime"
    WorkRecordType.FREELANCE_INCOME -> if (ar) "دخل عمل حر" else "Freelance income"
    WorkRecordType.FREELANCE_EXPENSE -> if (ar) "مصروف عمل حر" else "Freelance expense"
    WorkRecordType.SALARY_PAYMENT -> if (ar) "دفعة راتب" else "Salary payment"
}
