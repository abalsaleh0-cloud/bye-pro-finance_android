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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.finance.engine.FinanceEngine
import com.example.finance.i18n.Strings
import com.example.finance.model.HouseholdExpenseEntity
import com.example.finance.model.HouseholdMemberEntity
import com.example.finance.ui.FinanceViewModel

@Composable
fun HouseholdScreen(
    viewModel: FinanceViewModel
) {
    val lang by viewModel.appLanguage.collectAsState()
    val members by viewModel.householdMembers.collectAsState()
    val expenses by viewModel.householdExpenses.collectAsState()
    val primaryCurrency by viewModel.primaryCurrency.collectAsState()

    var showAddMemberDialog by remember { mutableStateOf(false) }
    var editingMember by remember { mutableStateOf<HouseholdMemberEntity?>(null) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }

    val currencyExpenses = expenses.filter { it.currency.equals(primaryCurrency, ignoreCase = true) }
    val totalExpenses = currencyExpenses.sumOf { it.amount }

    Scaffold(
        floatingActionButton = {
            FinanceFloatingActionButton(
                onClick = { showAddExpenseDialog = true },
                containerColor = ByeProTheme.colors.goldPrimary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_household_expense_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = Strings.get("add_shared_expense", lang))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = Strings.get("household_title", lang),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = WebI18n.tr("إدارة المصاريف المشتركة وتوزيع الحصص والتحاسب العائلي", "Manage shared expenses, split contributions, and settle household balances", lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FinanceOutlinedButton(
                        onClick = { editingMember = null; showAddMemberDialog = true },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(Strings.get("add_member", lang), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // Summary Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = WebI18n.tr("إجمالي المصاريف العائلية المشتركة", "Total shared household expenses", lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = TextContent.money(totalExpenses, primaryCurrency, lang),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = ByeProTheme.colors.goldPrimary
                        )
                    }
                }
            }

            // Members
            item {
                Text(
                    text = WebI18n.tr("أفراد الأسرة المشاركون", "Participating household members", lang) + " (${TextContent.number(members.size, lang, 0)})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(members, key = { it.id }) { m ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, ByeProTheme.colors.financialGreen, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = ByeProTheme.colors.financialGreen)
                            }
                            Spacer(modifier = Modifier.size(10.dp))
                            Column {
                                Text(m.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(m.role, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = WebI18n.tr("نسبة المشاركة:", "Contribution:", lang) + " ${TextContent.number(m.contributionPercent.toInt(), lang, 0)}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = ByeProTheme.colors.financialGreen,
                                fontWeight = FontWeight.Bold
                            )
                            FinanceIconButton(onClick = { editingMember = m; showAddMemberDialog = true }) {
                                Icon(Icons.Default.Edit, contentDescription = WebI18n.tr("تعديل العضو", "Edit member", lang), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Shared expenses
            item {
                Text(
                    text = WebI18n.tr("سجل المصاريف المشتركة", "Shared expense history", lang),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (expenses.isEmpty()) {
                item {
                    Text(
                        text = WebI18n.tr("لا توجد مصاريف مشتركة مسجلة بعد.", "No shared expenses recorded yet.", lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(expenses, key = { it.id }) { exp ->
                    val payerName = members.find { it.id == exp.payerMemberId }?.name ?: WebI18n.tr("عضو", "Member", lang)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(exp.title, fontWeight = FontWeight.Bold)
                                Text(WebI18n.tr("الدافع:", "Paid by:", lang) + " $payerName • ${exp.category}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    TextContent.money(exp.amount, exp.currency, lang),
                                    color = ByeProTheme.colors.financialRed,
                                    fontWeight = FontWeight.Bold
                                )
                                FinanceIconButton(onClick = { viewModel.deleteHouseholdExpense(exp) }) {
                                    Icon(Icons.Default.Delete, contentDescription = WebI18n.tr("حذف المصروف", "Delete expense", lang), tint = MaterialTheme.colorScheme.error)
                                }
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

    // Add Member Dialog
    if (showAddMemberDialog) {
        var name by remember(editingMember?.id) { mutableStateOf(editingMember?.name.orEmpty()) }
        var role by remember(editingMember?.id) { mutableStateOf(editingMember?.role ?: WebI18n.tr("عضو", "Member", lang)) }
        var percentStr by remember(editingMember?.id) { mutableStateOf(editingMember?.contributionPercent?.toString() ?: "50") }

        FinanceAlertDialog(
            onDismissRequest = { showAddMemberDialog = false; editingMember = null },
            title = { Text(if (editingMember == null) Strings.get("add_member", lang) else WebI18n.tr("تعديل عضو الأسرة", "Edit household member", lang), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FinanceTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(WebI18n.tr("الاسم", "Name", lang)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    FinanceTextField(
                        value = role,
                        onValueChange = { role = it },
                        label = { Text(WebI18n.tr("الدور (رب الأسرة، شريك، ابن)", "Role (head of household, partner, child)", lang)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    FinanceTextField(
                        value = percentStr,
                        onValueChange = { percentStr = it },
                        label = { Text(WebI18n.tr("نسبة المساهمة %", "Contribution percentage %", lang)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                FinanceButton(
                    onClick = {
                        val p = percentStr.toDoubleOrNull()
                        if (name.isNotBlank() && role.isNotBlank() && p != null && p.isFinite() && p in 0.0..100.0) {
                            val existing = editingMember
                            if (existing == null) viewModel.addHouseholdMember(name, "", role, p)
                            else viewModel.updateHouseholdMember(existing.copy(name = name, role = role, contributionPercent = p))
                            showAddMemberDialog = false
                            editingMember = null
                        }
                    }
                ) {
                    Text(Strings.get("confirm", lang))
                }
            },
            dismissButton = {
                FinanceOutlinedButton(onClick = { showAddMemberDialog = false; editingMember = null }) {
                    Text(Strings.get("cancel", lang))
                }
            }
        )
    }

    // Add Shared Expense Dialog
    if (showAddExpenseDialog) {
        var title by remember { mutableStateOf("") }
        var amountStr by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Home Supplies") }
        val payerId = members.firstOrNull()?.id

        FinanceAlertDialog(
            onDismissRequest = { showAddExpenseDialog = false },
            title = { Text(Strings.get("add_shared_expense", lang), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (payerId == null) Text(WebI18n.tr("أضف فرداً من الأسرة أولاً قبل تسجيل مصروف مشترك.", "Add a household member before recording a shared expense.", lang), color = MaterialTheme.colorScheme.error)
                    FinanceTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(WebI18n.tr("وصف المصروف", "Expense description", lang)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    FinanceTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text(Strings.get("amount", lang)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    FinanceTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text(Strings.get("category", lang)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                FinanceButton(
                    onClick = {
                        val amt = amountStr.toDoubleOrNull() ?: 0.0
                        if (title.isNotBlank() && category.isNotBlank() && amt.isFinite() && amt > 0.0 && payerId != null) {
                            viewModel.addHouseholdExpense(title, amt, payerId, category)
                            showAddExpenseDialog = false
                        }
                    }
                ) {
                    Text(Strings.get("confirm", lang))
                }
            },
            dismissButton = {
                FinanceOutlinedButton(onClick = { showAddExpenseDialog = false }) {
                    Text(Strings.get("cancel", lang))
                }
            }
        )
    }
}
