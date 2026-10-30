package com.example.ui.screens

import com.example.ui.components.FinanceAlertDialog
import com.example.ui.components.FinanceButton
import com.example.ui.components.FinanceOutlinedButton
import com.example.ui.components.FinanceTextField

import com.example.finance.i18n.WebI18n
import com.example.finance.i18n.TextContent
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.finance.ui.FinanceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransfersScreen(
    viewModel: FinanceViewModel
) {
    val lang by viewModel.appLanguage.collectAsState()
    val accounts by viewModel.accounts.collectAsState()

    var fromAccountId by remember(accounts) { mutableStateOf(accounts.firstOrNull()?.id ?: 0L) }
    var toAccountId by remember(accounts) { mutableStateOf(accounts.getOrNull(1)?.id ?: accounts.firstOrNull()?.id ?: 0L) }
    var amountStr by remember { mutableStateOf("") }
    var feeStr by remember { mutableStateOf("0") }
    var description by remember { mutableStateOf(WebI18n.tr("تحويل رصيد داخلي", "Internal balance transfer", lang)) }
    var reference by remember { mutableStateOf("REF-${System.currentTimeMillis() % 100000}") }

    var expandedFrom by remember { mutableStateOf(false) }
    var expandedTo by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val fromAccount = accounts.find { it.id == fromAccountId }
    val toAccount = accounts.find { it.id == toAccountId }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = Strings.get("transfers_title", lang),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = WebI18n.tr("تحويل فوري ذري يضمن تحديث كلا الحسابين أو التراجع الآمن في حال التعثر.", "Instant atomic transfer updates both accounts or safely rolls back if it fails.", lang),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // From Account Dropdown
                    ExposedDropdownMenuBox(
                        expanded = expandedFrom,
                        onExpandedChange = { expandedFrom = !expandedFrom }
                    ) {
                        FinanceTextField(
                            value = fromAccount?.let { "${it.name} (${TextContent.money(it.balance, it.currency, lang)})" } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(Strings.get("from_account", lang)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFrom) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .testTag("transfer_from_dropdown")
                        )
                        ExposedDropdownMenu(
                            expanded = expandedFrom,
                            onDismissRequest = { expandedFrom = false }
                        ) {
                            accounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text("${TextContent.isolate(acc.name)} - ${WebI18n.money(acc.balance, acc.currency, lang)}") },
                                    onClick = {
                                        fromAccountId = acc.id
                                        expandedFrom = false
                                    }
                                )
                            }
                        }
                    }

                    // To Account Dropdown
                    ExposedDropdownMenuBox(
                        expanded = expandedTo,
                        onExpandedChange = { expandedTo = !expandedTo }
                    ) {
                        FinanceTextField(
                            value = toAccount?.let { "${it.name} (${TextContent.money(it.balance, it.currency, lang)})" } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(Strings.get("to_account", lang)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTo) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .testTag("transfer_to_dropdown")
                        )
                        ExposedDropdownMenu(
                            expanded = expandedTo,
                            onDismissRequest = { expandedTo = false }
                        ) {
                            accounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text("${acc.name} - ${WebI18n.money(acc.balance, acc.currency, lang)}") },
                                    onClick = {
                                        toAccountId = acc.id
                                        expandedTo = false
                                    }
                                )
                            }
                        }
                    }

                    // Amount input
                    FinanceTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text(Strings.get("transfer_amount", lang)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("transfer_amount_input"),
                        singleLine = true
                    )

                    // Fee input
                    FinanceTextField(
                        value = feeStr,
                        onValueChange = { feeStr = it },
                        label = { Text(Strings.get("transfer_fee", lang)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Description & reference
                    FinanceTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(Strings.get("description", lang)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    FinanceButton(
                        onClick = {
                            val amt = amountStr.toDoubleOrNull() ?: 0.0
                            val fee = feeStr.toDoubleOrNull() ?: 0.0
                            if (amt > 0.0 && fromAccountId != toAccountId && fromAccount != null) {
                                if (fromAccount.balance >= (amt + fee)) {
                                    viewModel.executeTransfer(
                                        fromAccountId = fromAccountId,
                                        toAccountId = toAccountId,
                                        amount = amt,
                                        fee = fee,
                                        currency = fromAccount.currency,
                                        description = description,
                                        reference = reference
                                    ) { success ->
                                        if (success) {
                                            statusMessage = Strings.get("transfer_success", lang)
                                            amountStr = ""
                                        }
                                    }
                                } else {
                                    statusMessage = WebI18n.tr("عفواً، رصيد الحساب المصدر غير كافٍ لإتمام التحويل والرسوم!", "Sorry, the source account balance is insufficient to cover the transfer and fees!", lang)
                                }
                            } else if (fromAccountId == toAccountId) {
                                statusMessage = WebI18n.tr("لا يمكن التحويل لنفس الحساب!", "You cannot transfer to the same account!", lang)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("execute_transfer_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = ByeProTheme.colors.goldPrimary, contentColor = MaterialTheme.colorScheme.onPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null)
                        Text(
                            text = Strings.get("execute_transfer", lang),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    if (statusMessage != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ByeProTheme.colors.financialGreen)
                            Text(
                                text = statusMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
