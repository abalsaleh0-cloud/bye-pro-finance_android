package com.example.ui.screens

import com.composables.icons.lucide.*

import com.example.ui.components.FinanceChoiceButton

import androidx.compose.material.icons.filled.*

import androidx.compose.material.icons.Icons

import com.example.finance.i18n.TextContent

import com.example.ui.components.FinanceAlertDialog
import com.example.ui.components.FinanceButton
import com.example.ui.components.FinanceOutlinedButton
import com.example.ui.components.FinanceTextField
import com.example.ui.components.FinanceAlertDialog
import com.example.ui.components.FinanceIconButton
import com.example.ui.components.FinanceFloatingActionButton

import com.example.finance.i18n.WebI18n
import com.example.ui.theme.ByeProTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.finance.engine.FinanceEngine
import com.example.finance.i18n.Strings
import com.example.finance.model.BillEntity
import com.example.finance.model.DebtEntity
import com.example.finance.model.DebtStatus
import com.example.finance.ui.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DebtsBillsScreen(
    viewModel: FinanceViewModel,
    initialTabIndex: Int = 0
) {
    val lang by viewModel.appLanguage.collectAsState()
    val debts by viewModel.debts.collectAsState()
    val bills by viewModel.bills.collectAsState()
    val accounts by viewModel.accounts.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(initialTabIndex.coerceIn(0, 1)) }
    var showAddDebtDialog by remember { mutableStateOf(false) }
    var newDebtDirection by remember { mutableStateOf("owed_to_me") }
    var showAddBillDialog by remember { mutableStateOf(false) }
    var editingBill by remember { mutableStateOf<BillEntity?>(null) }
    var debtToPay by remember { mutableStateOf<DebtEntity?>(null) }
    var editingDebt by remember { mutableStateOf<DebtEntity?>(null) }
    var historyDebt by remember { mutableStateOf<DebtEntity?>(null) }
    var deleteDebtTarget by remember { mutableStateOf<DebtEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            FinanceFloatingActionButton(
                onClick = {
                    if (selectedTabIndex == 0) showAddDebtDialog = true else showAddBillDialog = true
                },
                containerColor = ByeProTheme.colors.goldPrimary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_debt_bill_fab")
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
                    text = { Text(Strings.get("debts_title", lang), fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text(Strings.get("bills_title", lang), fontWeight = FontWeight.Bold) }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (selectedTabIndex == 0) {
                    item {
                        DebtManagementPanel(
                            debts = debts,
                            lang = lang,
                            selectedDirection = newDebtDirection,
                            onDirectionChange = { newDebtDirection = it },
                            onAdd = { selectedDirection -> newDebtDirection = selectedDirection; showAddDebtDialog = true },
                            onPay = { debtToPay = it },
                            onEdit = { editingDebt = it },
                            onHistory = { historyDebt = it },
                            onDelete = { deleteDebtTarget = it }
                        )
                    }
                } else {
                    // Bills list
                    if (bills.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Text(
                                    text = WebI18n.tr("لا توجد فواتير دورية مسجلة حالياً.", "No recurring bills recorded.", lang),
                                    modifier = Modifier.padding(24.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(bills, key = { it.id }) { bill ->
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                BillItemCard(
                                    bill = bill,
                                    lang = lang,
                                    onMarkPaid = {
                                        val matchingAccount = accounts.firstOrNull { it.currency.equals(bill.currency, true) && it.balance >= bill.amount }
                                        if (matchingAccount != null) viewModel.markBillPaid(bill.id, matchingAccount.id)
                                    },
                                    onDelete = { viewModel.deleteBill(bill) }
                                )
                                FinanceOutlinedButton(onClick = { editingBill = bill }, modifier = Modifier.fillMaxWidth()) {
                                    Text(WebI18n.tr("تعديل الفاتورة", "Edit bill", lang))
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }
    }

    if (deleteDebtTarget != null) {
        val target = deleteDebtTarget!!
        FinanceAlertDialog(
            onDismissRequest = { deleteDebtTarget = null },
            title = { Text(WebI18n.tr("تأكيد حذف الدين", "Confirm debt deletion", lang)) },
            text = { Text(WebI18n.tr("هل تريد حذف هذا السجل نهائياً؟", "Do you want to permanently delete this record?", lang)) },
            confirmButton = {
                FinanceButton(onClick = { viewModel.deleteDebt(target); deleteDebtTarget = null }) {
                    Text(WebI18n.tr("حذف نهائياً", "Delete permanently", lang))
                }
            },
            dismissButton = { FinanceOutlinedButton(onClick = { deleteDebtTarget = null }) { Text(Strings.get("cancel", lang)) } }
        )
    }

    if (historyDebt != null) {
        DebtPaymentHistoryDialog(debt = historyDebt!!, viewModel = viewModel, lang = lang, onDismiss = { historyDebt = null })
    }

    // Pay Debt Dialog
    if (debtToPay != null) {
        val target = debtToPay!!
        var payAmountStr by remember { mutableStateOf(target.minimumPayment.toString()) }
        var payNotes by remember { mutableStateOf(WebI18n.tr("دفعة سداد", "Debt payment", lang)) }
        var paymentDateText by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) }
        var paymentError by remember { mutableStateOf("") }

        FinanceAlertDialog(
            onDismissRequest = { debtToPay = null },
            title = { Text(WebI18n.tr("تسجيل سداد:", "Record payment:", lang) + " ${target.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(WebI18n.tr("المتبقي الحالي:", "Current remaining:", lang) + " ${TextContent.money(target.remaining, target.currency, lang)}")
                    FinanceTextField(
                        value = payAmountStr,
                        onValueChange = { payAmountStr = it },
                        label = { Text(WebI18n.tr("مبلغ السداد", "Payment amount", lang)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    FinanceTextField(
                        value = paymentDateText,
                        onValueChange = { paymentDateText = it },
                        label = { Text(WebI18n.tr("تاريخ الدفعة (YYYY-MM-DD)", "Payment date (YYYY-MM-DD)", lang)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    FinanceTextField(
                        value = payNotes,
                        onValueChange = { payNotes = it },
                        label = { Text(WebI18n.tr("ملاحظات", "Notes", lang)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (paymentError.isNotBlank()) Text(paymentError, color = ByeProTheme.colors.financialRed, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                FinanceButton(
                    onClick = {
                        val amt = payAmountStr.toDoubleOrNull() ?: 0.0
                        val parsedDate = try { SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(paymentDateText)?.time } catch (_: Exception) { null }
                        paymentError = when {
                            !amt.isFinite() || amt <= 0.0 -> WebI18n.tr("أدخل مبلغ دفعة صحيحاً.", "Enter a valid payment amount.", lang)
                            amt > target.remaining -> WebI18n.tr("مبلغ الدفعة أكبر من المتبقي.", "Payment exceeds remaining balance.", lang)
                            parsedDate == null -> WebI18n.tr("أدخل تاريخاً صحيحاً.", "Enter a valid date.", lang)
                            else -> ""
                        }
                        if (paymentError.isBlank() && parsedDate != null) {
                            viewModel.payDebt(target.id, amt, payNotes, parsedDate)
                            debtToPay = null
                        }
                    }
                ) {
                    Text(Strings.get("confirm", lang))
                }
            },
            dismissButton = {
                FinanceOutlinedButton(onClick = { debtToPay = null }) {
                    Text(Strings.get("cancel", lang))
                }
            }
        )
    }

    if (showAddDebtDialog || editingDebt != null) {
        AddDebtDialog(
            lang = lang,
            initialDebt = editingDebt,
            initialDirection = newDebtDirection,
            onDismiss = { showAddDebtDialog = false; editingDebt = null },
            onConfirm = { newDebt ->
                if (editingDebt == null) viewModel.addDebt(newDebt) else viewModel.updateDebt(newDebt)
                showAddDebtDialog = false
                editingDebt = null
            }
        )
    }

    // Add Bill Dialog
    if (showAddBillDialog || editingBill != null) {
        AddBillDialog(
            lang = lang,
            initialBill = editingBill,
            onDismiss = { showAddBillDialog = false; editingBill = null },
            onConfirm = { newBill ->
                if (editingBill == null) viewModel.addBill(newBill) else viewModel.updateBill(newBill)
                showAddBillDialog = false
                editingBill = null
            }
        )
    }
}

@Composable
fun DebtItemCard(
    debt: DebtEntity,
    payoffMonths: Int,
    lang: com.example.finance.model.AppLanguage,
    onPay: () -> Unit,
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
                        text = debt.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${debt.creditor} • " + WebI18n.tr("مرابحة", "Profit rate", lang) + " ${TextContent.number(debt.interestRate, lang)}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(WebI18n.tr("المتبقي للسداد", "Remaining to pay", lang), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        TextContent.money(debt.remaining, debt.currency, lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ByeProTheme.colors.financialRed
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(WebI18n.tr("القسط الشهري", "Monthly installment", lang), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        TextContent.money(debt.minimumPayment, debt.currency, lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = WebI18n.tr("المدة التقديرية للإنهاء:", "Estimated payoff time:", lang) + " $payoffMonths " + WebI18n.tr("شهراً", "months", lang),
                    style = MaterialTheme.typography.labelSmall,
                    color = ByeProTheme.colors.goldPrimary
                )
                FinanceButton(
                    onClick = onPay,
                    colors = ButtonDefaults.buttonColors(containerColor = ByeProTheme.colors.goldPrimary, contentColor = MaterialTheme.colorScheme.onPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(Strings.get("pay_debt", lang), modifier = Modifier.padding(start = 6.dp), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun BillItemCard(
    bill: BillEntity,
    lang: com.example.finance.model.AppLanguage,
    onMarkPaid: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = try {
        SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(bill.dueDate))
    } catch (_: Exception) {
        ""
    }

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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bill.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${bill.category} • " + WebI18n.tr("الاستحقاق:", "Due:", lang) + " $dateStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = TextContent.money(bill.amount, bill.currency, lang),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (bill.isPaid) ByeProTheme.colors.financialGreen else ByeProTheme.colors.financialRed
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!bill.isPaid) {
                    FinanceButton(
                        onClick = onMarkPaid,
                        colors = ButtonDefaults.buttonColors(containerColor = ByeProTheme.colors.goldPrimary, contentColor = MaterialTheme.colorScheme.onPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(Strings.get("mark_paid", lang), modifier = Modifier.padding(start = 4.dp), style = MaterialTheme.typography.labelMedium)
                    }
                } else {
                    Text(
                        text = WebI18n.tr("مدفوعة ✓", "Paid ✓", lang),
                        color = ByeProTheme.colors.financialGreen,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall
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
        }
    }
}

@Composable
private fun DebtDirectionSelector(
    selectedDirection: String,
    lang: com.example.finance.model.AppLanguage,
    tagPrefix: String,
    onDirectionChange: (String) -> Unit
) {
    val choices = listOf(
        "owed_to_me" to WebI18n.tr("دائن — لي عنده", "Creditor — owed to me", lang),
        "i_owe" to WebI18n.tr("مدين — عليّ له", "Debtor — I owe", lang)
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        choices.forEach { (value, label) ->
            val selected = selectedDirection == value
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) ByeProTheme.colors.goldPrimary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, if (selected) ByeProTheme.colors.goldPrimary else ByeProTheme.colors.fieldBorder, RoundedCornerShape(10.dp))
                    .selectable(selected = selected, onClick = { onDirectionChange(value) }, role = Role.RadioButton)
                    .testTag("${tagPrefix}_${value}")
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.RadioButton(selected = selected, onClick = { onDirectionChange(value) })
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun DebtManagementPanel(
    debts: List<DebtEntity>,
    lang: com.example.finance.model.AppLanguage,
    selectedDirection: String,
    onDirectionChange: (String) -> Unit,
    onAdd: (String) -> Unit,
    onPay: (DebtEntity) -> Unit,
    onEdit: (DebtEntity) -> Unit,
    onHistory: (DebtEntity) -> Unit,
    onDelete: (DebtEntity) -> Unit
) {
    var search by remember { mutableStateOf("") }
    val filtered = debts.filter { debt ->
        debt.direction == selectedDirection &&
            (search.isBlank() || debt.name.contains(search, true) || debt.creditor.contains(search, true) || debt.notes.contains(search, true))
    }
    val currencyTotals = filtered.groupBy { it.currency }.mapValues { (_, rows) -> rows.sumOf { it.remaining } }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        DebtDirectionSelector(
            selectedDirection = selectedDirection,
            lang = lang,
            tagPrefix = "debts_tab",
            onDirectionChange = onDirectionChange
        )
        FinanceTextField(
            value = search,
            onValueChange = { search = it },
            label = { Text(WebI18n.tr("بحث في الديون", "Search debts", lang)) },
            modifier = Modifier.fillMaxWidth()
        )
        if (currencyTotals.isNotEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(WebI18n.tr("إجمالي المتبقي حسب العملة", "Remaining totals by currency", lang), fontWeight = FontWeight.Bold)
                    currencyTotals.forEach { (currency, total) ->
                        Text("${TextContent.money(total, currency, lang)}", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
        if (filtered.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Text(
                    if (search.isBlank()) WebI18n.tr("لا توجد سجلات في هذا التصنيف بعد.", "No records in this debt category yet.", lang)
                    else WebI18n.tr("لا توجد نتائج مطابقة للبحث.", "No matching search results.", lang),
                    modifier = Modifier.padding(20.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (debts.none { it.direction == selectedDirection }) {
                FinanceButton(onClick = { onAdd(selectedDirection) }) { Text(WebI18n.tr("إضافة أول سجل", "Add first record", lang)) }
            }
        } else {
            Text(WebI18n.tr("${filtered.size} سجل", "${filtered.size} records", lang), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            filtered.forEach { debt ->
                val payoffMonths = FinanceEngine.calculateDebtPayoffMonths(
                    remainingPrincipal = debt.remaining,
                    annualInterestRatePercent = debt.interestRate,
                    monthlyPayment = if (debt.minimumPayment > 0) debt.minimumPayment else 1000.0
                )
                DebtItemCard(
                    debt = debt,
                    payoffMonths = payoffMonths,
                    lang = lang,
                    onPay = { onPay(debt) },
                    onDelete = { onDelete(debt) }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FinanceOutlinedButton(onClick = { onEdit(debt) }) { Text(WebI18n.tr("تعديل", "Edit", lang)) }
                    FinanceOutlinedButton(onClick = { onHistory(debt) }) { Text(WebI18n.tr("سجل السداد", "Payment history", lang)) }
                }
            }
        }
    }
}

@Composable
private fun DebtPaymentHistoryDialog(
    debt: DebtEntity,
    viewModel: FinanceViewModel,
    lang: com.example.finance.model.AppLanguage,
    onDismiss: () -> Unit
) {
    val payments by remember(debt.id) { viewModel.debtPayments(debt.id) }.collectAsState(initial = emptyList())
    FinanceAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(WebI18n.tr("سجل السداد", "Payment history", lang) + " — " + debt.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(WebI18n.tr("المتبقي", "Remaining", lang) + ": " + TextContent.money(debt.remaining, debt.currency, lang))
                if (payments.isEmpty()) {
                    Text(WebI18n.tr("لا توجد دفعات مسجلة لهذا الدين بعد.", "No payments recorded for this debt yet.", lang))
                } else {
                    payments.forEach { payment ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                                Text(TextContent.money(payment.amount, payment.currency, lang), fontWeight = FontWeight.Bold)
                                Text(SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(payment.date)), style = MaterialTheme.typography.bodySmall)
                                if (payment.notes.isNotBlank()) Text(payment.notes, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { FinanceButton(onClick = onDismiss) { Text(WebI18n.tr("إغلاق", "Close", lang)) } }
    )
}

@Composable
fun AddDebtDialog(
    lang: com.example.finance.model.AppLanguage,
    initialDebt: DebtEntity? = null,
    initialDirection: String = "owed_to_me",
    onDismiss: () -> Unit,
    onConfirm: (DebtEntity) -> Unit
) {
    var name by remember(initialDebt?.id) { mutableStateOf(initialDebt?.name ?: "") }
    var creditor by remember(initialDebt?.id) { mutableStateOf(initialDebt?.creditor ?: "") }
    var principalStr by remember(initialDebt?.id) { mutableStateOf(initialDebt?.principal?.toString() ?: "") }
    var minPaymentStr by remember(initialDebt?.id) { mutableStateOf(initialDebt?.minimumPayment?.toString() ?: "") }
    var currency by remember(initialDebt?.id) { mutableStateOf(initialDebt?.currency ?: "LYD") }
    var direction by remember(initialDebt?.id, initialDirection) { mutableStateOf(initialDebt?.direction ?: initialDirection) }
    var dueDateText by remember(initialDebt?.id) {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(initialDebt?.dueDate ?: (System.currentTimeMillis() + 90L * 24 * 60 * 60 * 1000))))
    }
    var notes by remember(initialDebt?.id) { mutableStateOf(initialDebt?.notes ?: "") }
    var error by remember { mutableStateOf("") }

    FinanceAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = if (initialDebt == null) Strings.get("add_debt", lang) else WebI18n.tr("تعديل الدين", "Edit debt", lang), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                FinanceTextField(value = name, onValueChange = { name = it }, label = { Text(WebI18n.tr("اسم الشخص أو الجهة", "Person or organization", lang)) }, modifier = Modifier.fillMaxWidth())
                FinanceTextField(value = creditor, onValueChange = { creditor = it }, label = { Text(WebI18n.tr("تفاصيل الطرف / الدائن", "Counterparty details", lang)) }, modifier = Modifier.fillMaxWidth())
                Text(
                    WebI18n.tr("نوع الدين", "Debt direction", lang),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                DebtDirectionSelector(
                    selectedDirection = direction,
                    lang = lang,
                    tagPrefix = "debt_direction",
                    onDirectionChange = { direction = it }
                )
                FinanceTextField(value = principalStr, onValueChange = { principalStr = it }, label = { Text(WebI18n.tr("المبلغ الأصلي", "Original amount", lang)) }, modifier = Modifier.fillMaxWidth())
                FinanceTextField(value = currency, onValueChange = { currency = it.uppercase(Locale.ROOT) }, label = { Text(WebI18n.tr("العملة (LYD / USD / EUR ...)", "Currency (LYD / USD / EUR ...)", lang)) }, modifier = Modifier.fillMaxWidth())
                FinanceTextField(value = minPaymentStr, onValueChange = { minPaymentStr = it }, label = { Text(WebI18n.tr("القسط / الدفعة المتوقعة", "Expected installment", lang)) }, modifier = Modifier.fillMaxWidth())
                FinanceTextField(value = dueDateText, onValueChange = { dueDateText = it }, label = { Text(WebI18n.tr("تاريخ الاستحقاق (YYYY-MM-DD)", "Due date (YYYY-MM-DD)", lang)) }, modifier = Modifier.fillMaxWidth())
                FinanceTextField(value = notes, onValueChange = { notes = it }, label = { Text(Strings.get("notes", lang)) }, modifier = Modifier.fillMaxWidth())
                if (error.isNotBlank()) Text(error, color = ByeProTheme.colors.financialRed, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            FinanceButton(onClick = {
                val principal = principalStr.toDoubleOrNull() ?: Double.NaN
                val installment = if (minPaymentStr.isBlank()) 0.0 else minPaymentStr.toDoubleOrNull() ?: Double.NaN
                val dueDate = try { SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(dueDateText)?.time } catch (_: Exception) { null }
                error = when {
                    name.isBlank() -> WebI18n.tr("أدخل اسم الشخص أو الجهة.", "Enter a person or organization.", lang)
                    !principal.isFinite() || principal <= 0.0 -> WebI18n.tr("أدخل مبلغاً أصلياً صحيحاً أكبر من صفر.", "Enter a valid original amount greater than zero.", lang)
                    !installment.isFinite() || installment < 0.0 -> WebI18n.tr("أدخل قيمة قسط صحيحة.", "Enter a valid installment.", lang)
                    !currency.matches(Regex("[A-Z]{3}")) -> WebI18n.tr("أدخل رمز عملة من ثلاثة أحرف.", "Enter a three-letter currency code.", lang)
                    dueDate == null -> WebI18n.tr("أدخل تاريخ استحقاق صحيحاً.", "Enter a valid due date.", lang)
                    initialDebt != null && principal < initialDebt.remaining -> WebI18n.tr("المبلغ الأصلي لا يمكن أن يقل عن المتبقي.", "Original amount cannot be lower than the remaining balance.", lang)
                    else -> ""
                }
                if (error.isBlank() && dueDate != null) {
                    onConfirm(
                        DebtEntity(
                            id = initialDebt?.id ?: 0,
                            name = name.trim(),
                            creditor = creditor.trim(),
                            principal = principal,
                            remaining = initialDebt?.remaining ?: principal,
                            interestRate = initialDebt?.interestRate ?: 0.0,
                            dueDate = dueDate,
                            minimumPayment = installment,
                            currency = currency,
                            status = if ((initialDebt?.remaining ?: principal) <= 0.0) DebtStatus.PAID else DebtStatus.ACTIVE,
                            notes = notes.trim(),
                            direction = direction
                        )
                    )
                }
            }) { Text(Strings.get("confirm", lang)) }
        },
        dismissButton = { FinanceOutlinedButton(onClick = onDismiss) { Text(Strings.get("cancel", lang)) } }
    )
}

@Composable
fun AddBillDialog(
    lang: com.example.finance.model.AppLanguage,
    initialBill: BillEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (BillEntity) -> Unit
) {
    var name by remember(initialBill?.id) { mutableStateOf(initialBill?.name ?: "") }
    var category by remember(initialBill?.id) { mutableStateOf(initialBill?.category ?: "Utilities") }
    var amountStr by remember(initialBill?.id) { mutableStateOf(initialBill?.amount?.toString() ?: "") }
    var currency by remember(initialBill?.id) { mutableStateOf(initialBill?.currency ?: "LYD") }
    var dueDateText by remember(initialBill?.id) { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(initialBill?.dueDate ?: (System.currentTimeMillis() + 10L * 24 * 60 * 60 * 1000)))) }
    var recurring by remember(initialBill?.id) { mutableStateOf(initialBill?.isRecurring ?: true) }
    var intervalText by remember(initialBill?.id) { mutableStateOf((initialBill?.recurrenceIntervalDays ?: 30).toString()) }
    var reminderText by remember(initialBill?.id) { mutableStateOf((initialBill?.reminderDaysBefore ?: 3).toString()) }
    var error by remember { mutableStateOf("") }
    FinanceAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialBill == null) Strings.get("add_bill", lang) else WebI18n.tr("تعديل الفاتورة", "Edit bill", lang), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                FinanceTextField(value = name, onValueChange = { name = it }, label = { Text(WebI18n.tr("اسم الفاتورة", "Bill name", lang)) }, modifier = Modifier.fillMaxWidth())
                FinanceTextField(value = category, onValueChange = { category = it }, label = { Text(Strings.get("category", lang)) }, modifier = Modifier.fillMaxWidth())
                FinanceTextField(value = amountStr, onValueChange = { amountStr = it }, enabled = initialBill?.isPaid != true, label = { Text(Strings.get("amount", lang)) }, modifier = Modifier.fillMaxWidth())
                FinanceTextField(value = currency, onValueChange = { currency = it.uppercase(Locale.ROOT) }, enabled = initialBill?.isPaid != true, label = { Text(WebI18n.tr("العملة", "Currency", lang)) }, modifier = Modifier.fillMaxWidth())
                if (initialBill?.isPaid == true) Text(WebI18n.tr("لا يمكن تغيير مبلغ أو عملة فاتورة مدفوعة لأنها مسجلة في دفتر المصروفات.", "Amount and currency are locked for paid bills because the expense is already posted.", lang), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FinanceTextField(value = dueDateText, onValueChange = { dueDateText = it }, label = { Text(WebI18n.tr("تاريخ الاستحقاق YYYY-MM-DD", "Due date YYYY-MM-DD", lang)) }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FinanceOutlinedButton(onClick = { recurring = true }) { Text(WebI18n.tr("متكررة", "Recurring", lang)) }
                    FinanceOutlinedButton(onClick = { recurring = false }) { Text(WebI18n.tr("لمرة واحدة", "One-time", lang)) }
                }
                if (recurring) FinanceTextField(value = intervalText, onValueChange = { intervalText = it }, label = { Text(WebI18n.tr("التكرار كل (أيام)", "Repeat every (days)", lang)) }, modifier = Modifier.fillMaxWidth())
                FinanceTextField(value = reminderText, onValueChange = { reminderText = it }, label = { Text(WebI18n.tr("التذكير قبل الاستحقاق (أيام)", "Reminder before due date (days)", lang)) }, modifier = Modifier.fillMaxWidth())
                if (error.isNotBlank()) Text(error, color = ByeProTheme.colors.financialRed, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            FinanceButton(onClick = {
                val amount = amountStr.toDoubleOrNull() ?: Double.NaN
                val dueDate = try { SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(dueDateText)?.time } catch (_: Exception) { null }
                val interval = intervalText.toIntOrNull() ?: 0
                val reminder = reminderText.toIntOrNull() ?: -1
                error = when {
                    name.isBlank() || category.isBlank() -> WebI18n.tr("أدخل اسم الفاتورة وتصنيفها.", "Enter a bill name and category.", lang)
                    !amount.isFinite() || amount <= 0.0 -> WebI18n.tr("أدخل مبلغاً صحيحاً أكبر من صفر.", "Enter a valid amount greater than zero.", lang)
                    !currency.matches(Regex("[A-Z]{3}")) -> WebI18n.tr("رمز العملة يجب أن يتكون من ثلاثة أحرف.", "Currency must be a three-letter code.", lang)
                    dueDate == null -> WebI18n.tr("أدخل تاريخ استحقاق صحيحاً.", "Enter a valid due date.", lang)
                    (recurring && interval <= 0) || reminder < 0 -> WebI18n.tr("تحقق من فترة التكرار ومدة التذكير.", "Check recurrence and reminder values.", lang)
                    else -> ""
                }
                if (error.isBlank() && dueDate != null) onConfirm(BillEntity(id = initialBill?.id ?: 0, name = name.trim(), category = category.trim(), amount = amount, currency = currency, dueDate = dueDate, isRecurring = recurring, recurrenceIntervalDays = interval.coerceAtLeast(1), isPaid = initialBill?.isPaid ?: false, reminderDaysBefore = reminder))
            }) { Text(Strings.get("confirm", lang)) }
        },
        dismissButton = { FinanceOutlinedButton(onClick = onDismiss) { Text(Strings.get("cancel", lang)) } }
    )
}
