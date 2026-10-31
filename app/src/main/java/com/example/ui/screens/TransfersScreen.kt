package com.example.ui.screens

import com.composables.icons.lucide.Lucide

import com.example.ui.components.FinanceIcon

import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.example.finance.i18n.Strings
import com.example.finance.i18n.TextContent
import com.example.finance.i18n.WebI18n
import com.example.finance.model.AccountEntity
import com.example.finance.model.TransferEntity
import com.example.finance.ui.FinanceViewModel
import com.example.ui.components.FinanceButton
import com.example.ui.components.FinanceTextField
import com.example.ui.theme.ByeProTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransfersScreen(viewModel: FinanceViewModel) {
    val lang by viewModel.appLanguage.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val transfers by viewModel.transfers.collectAsState()
    val accountsLoading by viewModel.accountsLoading.collectAsState()

    var fromAccountId by remember(accounts) { mutableStateOf(accounts.firstOrNull()?.id ?: 0L) }
    var toAccountId by remember(accounts) { mutableStateOf(accounts.firstOrNull { it.id != fromAccountId }?.id ?: 0L) }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var fromExpanded by remember { mutableStateOf(false) }
    var toExpanded by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var feedbackSuccess by remember { mutableStateOf(false) }

    val fromAccount = accounts.firstOrNull { it.id == fromAccountId && !it.isArchived }
    val toAccount = accounts.firstOrNull { it.id == toAccountId && !it.isArchived }
    val currencyMatches = fromAccount != null && toAccount != null &&
        fromAccount.currency.equals(toAccount.currency, ignoreCase = true)

    fun showError(message: String) {
        feedback = message
        feedbackSuccess = false
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = Strings.get("transfers_title", lang),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = WebI18n.tr(
                        "نقل الأموال بين حساباتك مع حفظ سجل مستقل للتحويلات.",
                        "Move money between your accounts and keep a separate transfer history.",
                        lang
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = fromExpanded,
                        onExpandedChange = { fromExpanded = !fromExpanded }
                    ) {
                        FinanceTextField(
                            value = fromAccount?.let { "${it.name} · ${it.currency}" } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(Strings.get("from_account", lang)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor().testTag("transfer_from_dropdown")
                        )
                        ExposedDropdownMenu(expanded = fromExpanded, onDismissRequest = { fromExpanded = false }) {
                            accounts.filterNot { it.isArchived }.forEach { account ->
                                DropdownMenuItem(
                                    text = { Text("${TextContent.isolate(account.name)} · ${account.currency}") },
                                    onClick = {
                                        fromAccountId = account.id
                                        if (account.id == toAccountId) {
                                            toAccountId = accounts.firstOrNull { it.id != account.id && !it.isArchived }?.id ?: 0L
                                        }
                                        feedback = null
                                        fromExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = toExpanded,
                        onExpandedChange = { toExpanded = !toExpanded }
                    ) {
                        FinanceTextField(
                            value = toAccount?.let { "${it.name} · ${it.currency}" } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(Strings.get("to_account", lang)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor().testTag("transfer_to_dropdown")
                        )
                        ExposedDropdownMenu(expanded = toExpanded, onDismissRequest = { toExpanded = false }) {
                            accounts.filter { !it.isArchived && it.id != fromAccountId }.forEach { account ->
                                DropdownMenuItem(
                                    text = { Text("${TextContent.isolate(account.name)} · ${account.currency}") },
                                    onClick = {
                                        toAccountId = account.id
                                        feedback = null
                                        toExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    FinanceTextField(
                        value = amount,
                        onValueChange = { amount = it; feedback = null },
                        label = { Text(Strings.get("transfer_amount", lang)) },
                        modifier = Modifier.fillMaxWidth().testTag("transfer_amount_input"),
                        singleLine = true
                    )
                    FinanceTextField(
                        value = fromAccount?.currency ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(WebI18n.tr("العملة", "Currency", lang)) },
                        modifier = Modifier.fillMaxWidth().testTag("transfer_currency_field"),
                        singleLine = true
                    )
                    FinanceTextField(
                        value = description,
                        onValueChange = { description = it.take(2000) },
                        label = { Text(Strings.get("description", lang)) },
                        modifier = Modifier.fillMaxWidth().testTag("transfer_description_input"),
                        singleLine = true
                    )

                    if (fromAccount != null) {
                        Text(
                            text = WebI18n.tr(
                                "الرصيد المتاح: ${TextContent.money(fromAccount.balance, fromAccount.currency, lang)}",
                                "Available balance: ${TextContent.money(fromAccount.balance, fromAccount.currency, lang)}",
                                lang
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    FinanceButton(
                        onClick = {
                            val value = amount.trim().replace(',', '.').toDoubleOrNull()
                            when {
                                fromAccount == null || toAccount == null ->
                                    showError(WebI18n.tr("اختر حسابين نشطين.", "Choose two active accounts.", lang))
                                fromAccount.id == toAccount.id ->
                                    showError(WebI18n.tr("يجب اختيار حسابين مختلفين.", "Choose two different accounts.", lang))
                                !currencyMatches ->
                                    showError(WebI18n.tr("يجب أن تتطابق عملة الحسابين.", "Both accounts must use the same currency.", lang))
                                value == null || !value.isFinite() || value <= 0.0 ->
                                    showError(WebI18n.tr("أدخل مبلغاً صحيحاً أكبر من الصفر.", "Enter a valid amount greater than zero.", lang))
                                fromAccount.balance < value ->
                                    showError(WebI18n.tr("رصيد الحساب المصدر غير كافٍ.", "The source account has insufficient funds.", lang))
                                else -> {
                                    saving = true
                                    feedback = null
                                    viewModel.executeTransfer(
                                        fromAccountId = fromAccount.id,
                                        toAccountId = toAccount.id,
                                        amount = value,
                                        fee = 0.0,
                                        currency = fromAccount.currency,
                                        description = description.trim(),
                                        reference = ""
                                    ) { success ->
                                        saving = false
                                        feedbackSuccess = success
                                        feedback = if (success) {
                                            amount = ""
                                            description = ""
                                            WebI18n.tr("تم تنفيذ التحويل بنجاح.", "Transfer completed successfully.", lang)
                                        } else {
                                            WebI18n.tr("تعذر تنفيذ التحويل. تحقق من الحسابين والرصيد.", "Transfer failed. Check both accounts and the available balance.", lang)
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("execute_transfer_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ByeProTheme.colors.goldPrimary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        FinanceIcon(Lucide.ArrowLeftRight, contentDescription = null)
                        Text(
                            text = if (saving) WebI18n.tr("جارٍ التنفيذ…", "Processing…", lang)
                            else Strings.get("execute_transfer", lang),
                            modifier = Modifier.padding(start = 8.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (feedback != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (feedbackSuccess) FinanceIcon(Lucide.CircleCheck, contentDescription = null, tint = ByeProTheme.colors.financialGreen)
                            Text(
                                text = feedback.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (feedbackSuccess) ByeProTheme.colors.financialGreen else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    if (accountsLoading) {
                        Text(WebI18n.tr("جارٍ تحميل الحسابات…", "Loading accounts…", lang), style = MaterialTheme.typography.bodySmall)
                    } else if (accounts.size < 2) {
                        Text(
                            WebI18n.tr("أضف حسابين نشطين على الأقل لإجراء تحويل.", "Add at least two active accounts to make a transfer.", lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = WebI18n.tr("سجل التحويلات", "Transfer history", lang),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (transfers.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        text = WebI18n.tr("لا توجد تحويلات بعد.", "No transfers yet.", lang),
                        modifier = Modifier.padding(24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(transfers, key = { it.id }) { transfer ->
                TransferHistoryCard(transfer = transfer, accounts = accounts, lang = lang)
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun TransferHistoryCard(
    transfer: TransferEntity,
    accounts: List<AccountEntity>,
    lang: com.example.finance.model.AppLanguage
) {
    val fromName = accounts.firstOrNull { it.id == transfer.fromAccountId }?.name
        ?: WebI18n.tr("حساب محذوف", "Deleted account", lang)
    val toName = accounts.firstOrNull { it.id == transfer.toAccountId }?.name
        ?: WebI18n.tr("حساب محذوف", "Deleted account", lang)
    val date = remember(transfer.date) {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT).format(Date(transfer.date))
    }
    Card(
        modifier = Modifier.fillMaxWidth().border(
            1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(14.dp)
        ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "$fromName → $toName",
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = transfer.description.ifBlank { WebI18n.tr("تحويل مالي", "Money transfer", lang) },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = TextContent.money(transfer.amount, transfer.currency, lang),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
