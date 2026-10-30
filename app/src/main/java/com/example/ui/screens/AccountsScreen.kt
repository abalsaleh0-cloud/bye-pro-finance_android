package com.example.ui.screens

import com.example.finance.i18n.WebI18n
import com.example.ui.theme.ByeProTheme
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.finance.engine.FinanceEngine
import com.example.finance.i18n.Strings
import com.example.finance.model.AccountEntity
import com.example.finance.model.AccountType
import com.example.finance.ui.FinanceViewModel

@Composable
fun AccountsScreen(
    viewModel: FinanceViewModel
) {
    val lang by viewModel.appLanguage.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    var selectedAccountForDelete by remember { mutableStateOf<AccountEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = ByeProTheme.colors.goldPrimary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_account_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = Strings.get("add_account", lang))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = Strings.get("accounts_title", lang),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                val totalLiquid = accounts.sumOf { if (!it.isArchived) it.balance else 0.0 }
                Text(
                    text = "${Strings.get("total_balance", lang)}: ${FinanceEngine.formatMoney(totalLiquid, "SAR")}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ByeProTheme.colors.financialGreen,
                    fontWeight = FontWeight.SemiBold
                )
            }

            items(accounts, key = { it.id }) { acc ->
                AccountDetailedCard(
                    account = acc,
                    onArchive = { viewModel.archiveAccount(acc.id, true) },
                    onDelete = { selectedAccountForDelete = acc }
                )
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    // Add Account Dialog
    if (showAddDialog) {
        AddAccountDialog(
            lang = lang,
            onDismiss = { showAddDialog = false },
            onConfirm = { newAccount ->
                viewModel.addAccount(newAccount)
                showAddDialog = false
            }
        )
    }

    // Delete confirmation
    if (selectedAccountForDelete != null) {
        val target = selectedAccountForDelete!!
        AlertDialog(
            onDismissRequest = { selectedAccountForDelete = null },
            title = { Text(text = Strings.get("delete", lang)) },
            text = { Text("هل تريد بالتأكيد حذف حساب '${target.name}'؟") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAccount(target)
                        selectedAccountForDelete = null
                    },
                    modifier = Modifier.testTag("confirm_delete_account_btn")
                ) {
                    Text(Strings.get("delete", lang))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { selectedAccountForDelete = null }) {
                    Text(Strings.get("cancel", lang))
                }
            }
        )
    }
}

@Composable
fun AccountDetailedCard(
    account: AccountEntity,
    onArchive: () -> Unit,
    onDelete: () -> Unit
) {
    val icon: ImageVector = when (account.type) {
        AccountType.BANK -> Icons.Default.AccountBalance
        AccountType.CASH -> Icons.Default.Payments
        AccountType.SAVINGS -> Icons.Default.Savings
        AccountType.INVESTMENT -> Icons.Default.TrendingUp
        AccountType.CREDIT_CARD -> Icons.Default.CreditCard
        else -> Icons.Default.AccountBalance
    }

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(ByeProTheme.colors.goldPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = icon, contentDescription = null, tint = ByeProTheme.colors.goldPrimary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = account.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${account.institution} • ${account.accountNumberSafe.ifBlank { account.type.name }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onArchive,
                        modifier = Modifier.testTag("archive_account_${account.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = "Archive",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_account_${account.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = ByeProTheme.colors.financialRed.copy(alpha = 0.85f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = WebI18n.tr("الرصيد الفعلي", "Actual Balance", lang),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = FinanceEngine.formatMoney(account.balance, account.currency),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = ByeProTheme.colors.financialGreen
                )
            }
        }
    }
}

@Composable
fun AddAccountDialog(
    lang: com.example.finance.model.AppLanguage,
    onDismiss: () -> Unit,
    onConfirm: (AccountEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var balanceStr by remember { mutableStateOf("") }
    var institution by remember { mutableStateOf("") }
    var accNumber by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(AccountType.BANK) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = Strings.get("add_account", lang), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(Strings.get("account_name", lang)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("account_name_input")
                )
                OutlinedTextField(
                    value = balanceStr,
                    onValueChange = { balanceStr = it },
                    label = { Text(Strings.get("initial_balance", lang)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("account_balance_input")
                )
                OutlinedTextField(
                    value = institution,
                    onValueChange = { institution = it },
                    label = { Text(Strings.get("institution", lang)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = accNumber,
                    onValueChange = { accNumber = it },
                    label = { Text(Strings.get("safe_number", lang)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val bal = balanceStr.toDoubleOrNull() ?: 0.0
                        onConfirm(
                            AccountEntity(
                                name = name,
                                type = selectedType,
                                balance = bal,
                                institution = institution,
                                accountNumberSafe = accNumber
                            )
                        )
                    }
                },
                modifier = Modifier.testTag("submit_account_btn")
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
