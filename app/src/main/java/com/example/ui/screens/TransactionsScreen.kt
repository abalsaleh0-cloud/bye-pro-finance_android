package com.example.ui.screens

import com.example.ui.components.FinanceAlertDialog
import com.example.ui.components.FinanceButton
import com.example.ui.components.FinanceOutlinedButton
import com.example.ui.components.FinanceTextField

import com.example.finance.i18n.WebI18n
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.finance.engine.FinanceEngine
import com.example.finance.i18n.Strings
import com.example.finance.model.AccountEntity
import com.example.finance.model.TransactionEntity
import com.example.finance.model.TransactionType
import com.example.finance.ui.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionsScreen(
    viewModel: FinanceViewModel,
    initialAddOpen: Boolean = false
) {
    val lang by viewModel.appLanguage.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val accounts by viewModel.accounts.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf<TransactionType?>(null) }
    var showAddDialog by remember { mutableStateOf(initialAddOpen) }
    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }

    val filteredTransactions = transactions.filter { tx ->
        val matchesType = selectedTypeFilter == null || tx.type == selectedTypeFilter
        val matchesSearch = searchQuery.isBlank() ||
                tx.description.contains(searchQuery, ignoreCase = true) ||
                tx.merchant.contains(searchQuery, ignoreCase = true) ||
                tx.category.contains(searchQuery, ignoreCase = true)
        matchesType && matchesSearch
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = ByeProTheme.colors.goldPrimary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_transaction_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = Strings.get("add_transaction", lang))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = Strings.get("transactions_title", lang),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Search field
                FinanceTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(Strings.get("search_transactions", lang)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_transactions_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Filter chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedTypeFilter == null,
                        onClick = { selectedTypeFilter = null },
                        label = { Text(Strings.get("filter_all", lang)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ByeProTheme.colors.goldPrimary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                    FilterChip(
                        selected = selectedTypeFilter == TransactionType.INCOME,
                        onClick = { selectedTypeFilter = TransactionType.INCOME },
                        label = { Text(Strings.get("type_income", lang)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ByeProTheme.colors.financialGreen,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                    FilterChip(
                        selected = selectedTypeFilter == TransactionType.EXPENSE,
                        onClick = { selectedTypeFilter = TransactionType.EXPENSE },
                        label = { Text(Strings.get("type_expense", lang)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ByeProTheme.colors.financialRed,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                    FilterChip(
                        selected = selectedTypeFilter == TransactionType.TRANSFER,
                        onClick = { selectedTypeFilter = TransactionType.TRANSFER },
                        label = { Text(Strings.get("type_transfer", lang)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ByeProTheme.colors.goldPrimary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            if (filteredTransactions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(
                            text = WebI18n.tr("لا توجد نتائج مطابقة لبحثك", "No results match your search", lang),
                            modifier = Modifier.padding(32.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filteredTransactions, key = { it.id }) { tx ->
                    TransactionItemCard(
                        transaction = tx,
                        lang = lang,
                        onDelete = { transactionToDelete = tx }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    // Add Transaction Dialog
    if (showAddDialog) {
        AddTransactionDialog(
            accounts = accounts,
            lang = lang,
            onDismiss = { showAddDialog = false },
            onConfirm = { newTx ->
                viewModel.addTransaction(newTx)
                showAddDialog = false
            }
        )
    }

    // Delete confirmation dialog
    if (transactionToDelete != null) {
        val target = transactionToDelete!!
        FinanceAlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text(text = Strings.get("delete", lang)) },
            text = { Text(WebI18n.tr("هل تريد حذف المعاملة", "Delete this transaction", lang) + " '${target.description}' " + WebI18n.tr("وإعادة تعديل رصيد الحساب تلقائياً؟", "and automatically adjust the account balance?", lang)) },
            confirmButton = {
                FinanceButton(
                    onClick = {
                        viewModel.deleteTransaction(target)
                        transactionToDelete = null
                    },
                    modifier = Modifier.testTag("confirm_delete_tx_btn")
                ) {
                    Text(Strings.get("delete", lang))
                }
            },
            dismissButton = {
                FinanceOutlinedButton(onClick = { transactionToDelete = null }) {
                    Text(Strings.get("cancel", lang))
                }
            }
        )
    }
}

@Composable
fun TransactionItemCard(
    transaction: TransactionEntity,
    lang: com.example.finance.model.AppLanguage,
    onDelete: () -> Unit
) {
    val isIncome = transaction.type == TransactionType.INCOME || transaction.type == TransactionType.REFUND
    val accentColor = when (transaction.type) {
        TransactionType.INCOME, TransactionType.REFUND -> ByeProTheme.colors.financialGreen
        TransactionType.EXPENSE -> ByeProTheme.colors.financialRed
        TransactionType.TRANSFER -> ByeProTheme.colors.goldPrimary
        TransactionType.ADJUSTMENT -> ByeProTheme.colors.goldPrimary
    }
    val sign = when (transaction.type) {
        TransactionType.INCOME, TransactionType.REFUND -> "+"
        TransactionType.EXPENSE -> "-"
        else -> ""
    }

    val dateFormatted = try {
        SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(transaction.date))
    } catch (_: Exception) {
        ""
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(14.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = transaction.description,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${transaction.category} • ${transaction.merchant.ifBlank { "N/A" }} • $dateFormatted",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$sign${FinanceEngine.formatMoney(transaction.amount, transaction.currency)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_tx_${transaction.id}")
                ) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    accounts: List<AccountEntity>,
    lang: com.example.finance.model.AppLanguage,
    onDismiss: () -> Unit,
    onConfirm: (TransactionEntity) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Groceries") }
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: 1L) }

    val categories = listOf("Groceries", "Dining", "Utilities", "Salary", "Shopping", "Health", "Investments", "Transport", "Other")

    FinanceAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = Strings.get("add_transaction", lang), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Type selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FinanceButton(
                        onClick = { selectedType = TransactionType.EXPENSE },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == TransactionType.EXPENSE) ByeProTheme.colors.financialRed else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selectedType == TransactionType.EXPENSE) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(Strings.get("type_expense", lang), fontWeight = FontWeight.Bold)
                    }
                    FinanceButton(
                        onClick = { selectedType = TransactionType.INCOME },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == TransactionType.INCOME) ByeProTheme.colors.financialGreen else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selectedType == TransactionType.INCOME) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(Strings.get("type_income", lang), fontWeight = FontWeight.Bold)
                    }
                }

                FinanceTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text(Strings.get("amount", lang)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_amount_input"),
                    singleLine = true
                )

                FinanceTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(Strings.get("description", lang)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_desc_input"),
                    singleLine = true
                )

                FinanceTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text(Strings.get("merchant", lang)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Category chips
                Text(text = Strings.get("category", lang), style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            FinanceButton(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (amt > 0.0 && description.isNotBlank()) {
                        onConfirm(
                            TransactionEntity(
                                accountId = selectedAccountId,
                                type = selectedType,
                                amount = amt,
                                category = category,
                                description = description,
                                merchant = merchant
                            )
                        )
                    }
                },
                modifier = Modifier.testTag("submit_tx_btn")
            ) {
                Text(Strings.get("confirm", lang))
            }
        },
        dismissButton = {
            FinanceOutlinedButton(onClick = onDismiss) {
                Text(Strings.get("cancel", lang))
            }
        }
    )
}
