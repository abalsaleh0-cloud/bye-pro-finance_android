package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
    viewModel: FinanceViewModel
) {
    val lang by viewModel.appLanguage.collectAsState()
    val debts by viewModel.debts.collectAsState()
    val bills by viewModel.bills.collectAsState()
    val accounts by viewModel.accounts.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showAddDebtDialog by remember { mutableStateOf(false) }
    var showAddBillDialog by remember { mutableStateOf(false) }
    var debtToPay by remember { mutableStateOf<DebtEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
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
                    // Debts list
                    if (debts.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Text(
                                    text = WebI18n.tr("لا توجد ديون أو التزامات قائمة. وضعك المالي حر تماماً!", "No outstanding debts or liabilities. You are debt-free!", lang),
                                    modifier = Modifier.padding(24.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(debts, key = { it.id }) { debt ->
                            val payoffMonths = FinanceEngine.calculateDebtPayoffMonths(
                                remainingPrincipal = debt.remaining,
                                annualInterestRatePercent = debt.interestRate,
                                monthlyPayment = if (debt.minimumPayment > 0) debt.minimumPayment else 1000.0
                            )

                            DebtItemCard(
                                debt = debt,
                                payoffMonths = payoffMonths,
                                lang = lang,
                                onPay = { debtToPay = debt },
                                onDelete = { viewModel.deleteDebt(debt) }
                            )
                        }
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
                            BillItemCard(
                                bill = bill,
                                lang = lang,
                                onMarkPaid = {
                                    val defaultAcc = accounts.firstOrNull()?.id ?: 1L
                                    viewModel.markBillPaid(bill.id, defaultAcc)
                                },
                                onDelete = { viewModel.deleteBill(bill) }
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

    // Pay Debt Dialog
    if (debtToPay != null) {
        val target = debtToPay!!
        var payAmountStr by remember { mutableStateOf(target.minimumPayment.toString()) }
        var payNotes by remember { mutableStateOf(WebI18n.tr("دفعة سداد شهرية", "Monthly debt payment", lang)) }

        AlertDialog(
            onDismissRequest = { debtToPay = null },
            title = { Text("تسجيل سداد: ${target.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("المتبقي الحالي: ${FinanceEngine.formatMoney(target.remaining, target.currency)}")
                    OutlinedTextField(
                        value = payAmountStr,
                        onValueChange = { payAmountStr = it },
                        label = { Text(WebI18n.tr("مبلغ السداد", "Payment amount", lang)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = payNotes,
                        onValueChange = { payNotes = it },
                        label = { Text(WebI18n.tr("ملاحظات", "Notes", lang)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = payAmountStr.toDoubleOrNull() ?: 0.0
                        if (amt > 0.0) {
                            viewModel.payDebt(target.id, amt, payNotes)
                            debtToPay = null
                        }
                    }
                ) {
                    Text(Strings.get("confirm", lang))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { debtToPay = null }) {
                    Text(Strings.get("cancel", lang))
                }
            }
        )
    }

    // Add Debt Dialog
    if (showAddDebtDialog) {
        AddDebtDialog(
            lang = lang,
            onDismiss = { showAddDebtDialog = false },
            onConfirm = { newDebt ->
                viewModel.addDebt(newDebt)
                showAddDebtDialog = false
            }
        )
    }

    // Add Bill Dialog
    if (showAddBillDialog) {
        AddBillDialog(
            lang = lang,
            onDismiss = { showAddBillDialog = false },
            onConfirm = { newBill ->
                viewModel.addBill(newBill)
                showAddBillDialog = false
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
                        text = "${debt.creditor} • مرابحة ${debt.interestRate}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
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
                        FinanceEngine.formatMoney(debt.remaining, debt.currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ByeProTheme.colors.financialRed
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(WebI18n.tr("القسط الشهري", "Monthly installment", lang), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        FinanceEngine.formatMoney(debt.minimumPayment, debt.currency),
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
                    text = "المدة التقديرية للإنهاء: $payoffMonths شهراً",
                    style = MaterialTheme.typography.labelSmall,
                    color = ByeProTheme.colors.goldPrimary
                )
                Button(
                    onClick = onPay,
                    colors = ButtonDefaults.buttonColors(containerColor = ByeProTheme.colors.goldPrimary, contentColor = MaterialTheme.colorScheme.onPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
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
                    text = "${bill.category} • الاستحقاق: $dateStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = FinanceEngine.formatMoney(bill.amount, bill.currency),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (bill.isPaid) ByeProTheme.colors.financialGreen else ByeProTheme.colors.financialRed
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!bill.isPaid) {
                    Button(
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
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AddDebtDialog(
    lang: com.example.finance.model.AppLanguage,
    onDismiss: () -> Unit,
    onConfirm: (DebtEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var creditor by remember { mutableStateOf("") }
    var principalStr by remember { mutableStateOf("") }
    var minPaymentStr by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = Strings.get("add_debt", lang), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(WebI18n.tr("اسم التمويل / القرض", "Financing / loan name", lang)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = creditor,
                    onValueChange = { creditor = it },
                    label = { Text(Strings.get("creditor", lang)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = principalStr,
                    onValueChange = { principalStr = it },
                    label = { Text(Strings.get("principal", lang)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = minPaymentStr,
                    onValueChange = { minPaymentStr = it },
                    label = { Text(Strings.get("min_payment", lang)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = principalStr.toDoubleOrNull() ?: 0.0
                    val min = minPaymentStr.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && p > 0.0) {
                        onConfirm(
                            DebtEntity(
                                name = name,
                                creditor = creditor,
                                principal = p,
                                remaining = p,
                                minimumPayment = min
                            )
                        )
                    }
                }
            ) {
                Text(Strings.get("confirm", lang))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(Strings.get("cancel", lang))
            }
        }
    )
}

@Composable
fun AddBillDialog(
    lang: com.example.finance.model.AppLanguage,
    onDismiss: () -> Unit,
    onConfirm: (BillEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Utilities") }
    var amountStr by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = Strings.get("add_bill", lang), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(WebI18n.tr("اسم الفاتورة", "Bill name", lang)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text(Strings.get("category", lang)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text(Strings.get("amount", lang)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && amt > 0.0) {
                        onConfirm(
                            BillEntity(
                                name = name,
                                category = category,
                                amount = amt,
                                dueDate = System.currentTimeMillis() + (10L * 24 * 60 * 60 * 1000)
                            )
                        )
                    }
                }
            ) {
                Text(Strings.get("confirm", lang))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(Strings.get("cancel", lang))
            }
        }
    )
}
