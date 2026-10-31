package com.example.ui.screens

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.finance.i18n.TextContent
import com.example.finance.i18n.WebI18n
import com.example.finance.model.AccountEntity
import com.example.finance.model.AccountType
import com.example.finance.model.AppLanguage
import com.example.finance.ui.FinanceViewModel
import com.example.ui.components.FinanceAlertDialog
import com.example.ui.components.FinanceButton
import com.example.ui.components.FinanceFloatingActionButton
import com.example.ui.components.FinanceOutlinedButton
import com.example.ui.components.FinanceTextField
import com.example.ui.theme.ByeProTheme

private val accountCurrencies = listOf("LYD", "USD", "EUR", "GBP", "SYP", "JOD")
private val webAccountTypes = listOf(
    AccountType.CASH,
    AccountType.BANK,
    AccountType.WALLET,
    AccountType.CREDIT_CARD,
    AccountType.INVESTMENT,
    AccountType.OTHER
)

@Composable
fun AccountsScreen(viewModel: FinanceViewModel) {
    val lang by viewModel.appLanguage.collectAsState()
    val activeAccounts by viewModel.accounts.collectAsState()
    val archivedAccounts by viewModel.archivedAccounts.collectAsState()
    val accounts = (activeAccounts + archivedAccounts).sortedByDescending { it.createdAt }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FinanceFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = ByeProTheme.colors.goldPrimary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_account_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = WebI18n.tr("إضافة حساب", "Add account", lang))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    WebI18n.tr("حساباتك", "Your accounts", lang),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    WebI18n.tr("إدارة الأرصدة", "Account balances", lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val totals = accounts.filter { !it.isArchived }.groupBy { it.currency }
                .mapValues { (_, rows) -> rows.sumOf { it.balance } }
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(WebI18n.tr("إجمالي الأرصدة النشطة", "Active balances total", lang), fontWeight = FontWeight.SemiBold)
                    if (totals.isEmpty()) {
                        Text("—", style = MaterialTheme.typography.titleLarge)
                        Text(
                            WebI18n.tr("أضف حسابًا لعرض الرصيد حسب العملة.", "Add an account to view balances by currency.", lang),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        totals.toSortedMap().forEach { (currency, total) ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("$currency · ${accounts.count { !it.isArchived && it.currency == currency }} " +
                                    WebI18n.tr("حساب نشط", "active accounts", lang))
                                Text(TextContent.money(total, currency, lang), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (accounts.isEmpty()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Wallet, null, modifier = Modifier.size(42.dp), tint = ByeProTheme.colors.goldPrimary)
                        Text(WebI18n.tr("لا توجد حسابات بعد", "No accounts yet", lang), fontWeight = FontWeight.Bold)
                        Text(
                            WebI18n.tr("أضف حسابك الأول لبدء تنظيم أموالك.", "Add your first account to start organizing your money.", lang),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FinanceOutlinedButton(onClick = { showAddDialog = true }) {
                            Text(WebI18n.tr("إضافة أول حساب", "Add your first account", lang))
                        }
                    }
                }
            } else {
                items(accounts, key = { it.id }) { account ->
                    AccountDetailedCard(
                        account = account,
                        lang = lang,
                        onToggleActive = { viewModel.archiveAccount(account.id, !account.isArchived) }
                    )
                }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }

    if (showAddDialog) {
        AddAccountDialog(
            lang = lang,
            onDismiss = { showAddDialog = false },
            onConfirm = { account ->
                viewModel.addAccount(account)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AccountDetailedCard(
    account: AccountEntity,
    lang: AppLanguage,
    onToggleActive: () -> Unit
) {
    val icon = when (account.type) {
        AccountType.CASH -> Icons.Default.Payments
        AccountType.BANK -> Icons.Default.AccountBalance
        AccountType.WALLET -> Icons.Default.Wallet
        AccountType.CREDIT_CARD -> Icons.Default.CreditCard
        AccountType.INVESTMENT -> Icons.Default.TrendingUp
        else -> Icons.Default.AccountBalance
    }
    Card(
        modifier = Modifier.fillMaxWidth().border(
            1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp)
        ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(42.dp).clip(CircleShape)
                        .background(ByeProTheme.colors.goldPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) { Icon(icon, null, tint = ByeProTheme.colors.goldPrimary) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(account.name, fontWeight = FontWeight.Bold)
                    Text(
                        account.institution.ifBlank { accountTypeLabel(account.type, lang) } +
                            if (account.accountNumberSafe.isNotBlank()) " · •••• ${account.accountNumberSafe}" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (account.isArchived) WebI18n.tr("متوقف", "Inactive", lang)
                        else WebI18n.tr("نشط", "Active", lang),
                        color = if (account.isArchived) MaterialTheme.colorScheme.error else ByeProTheme.colors.financialGreen,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(WebI18n.tr("الرصيد الحالي", "Current balance", lang), style = MaterialTheme.typography.labelSmall)
                    Text(TextContent.money(account.balance, account.currency, lang), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                }
                FinanceOutlinedButton(onClick = onToggleActive, modifier = Modifier.testTag("toggle_account_${account.id}")) {
                    Text(
                        if (account.isArchived) WebI18n.tr("تفعيل الحساب", "Activate account", lang)
                        else WebI18n.tr("إيقاف الحساب", "Deactivate account", lang)
                    )
                }
            }
        }
    }
}

@Composable
fun AddAccountDialog(
    lang: AppLanguage,
    onDismiss: () -> Unit,
    onConfirm: (AccountEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var openingBalance by remember { mutableStateOf("0") }
    var institution by remember { mutableStateOf("") }
    var lastFour by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(AccountType.CASH) }
    var selectedCurrency by remember { mutableStateOf("LYD") }
    var typeMenuExpanded by remember { mutableStateOf(false) }
    var currencyMenuExpanded by remember { mutableStateOf(false) }
    var validationMessage by remember { mutableStateOf("") }

    FinanceAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(WebI18n.tr("إضافة حساب", "Add account", lang), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(WebI18n.tr("نوع الحساب", "Account type", lang), style = MaterialTheme.typography.labelMedium)
                Box {
                    OutlinedButton(
                        onClick = { typeMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth().testTag("account_type_selector")
                    ) { Text(accountTypeLabel(selectedType, lang)) }
                    DropdownMenu(typeMenuExpanded, { typeMenuExpanded = false }) {
                        webAccountTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(accountTypeLabel(type, lang)) },
                                onClick = { selectedType = type; typeMenuExpanded = false }
                            )
                        }
                    }
                }

                Text(WebI18n.tr("العملة", "Currency", lang), style = MaterialTheme.typography.labelMedium)
                Box {
                    OutlinedButton(
                        onClick = { currencyMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth().testTag("account_currency_selector")
                    ) { Text(currencyLabel(selectedCurrency, lang)) }
                    DropdownMenu(currencyMenuExpanded, { currencyMenuExpanded = false }) {
                        accountCurrencies.forEach { currency ->
                            DropdownMenuItem(
                                text = { Text(currencyLabel(currency, lang)) },
                                onClick = { selectedCurrency = currency; currencyMenuExpanded = false }
                            )
                        }
                    }
                }

                FinanceTextField(
                    value = name,
                    onValueChange = { name = it.take(80); validationMessage = "" },
                    label = { Text("Account name *") },
                    placeholder = { Text("e.g. Daily wallet") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("account_name_input"),
                    supportingText = { Text("${name.length}/80") },
                    isError = validationMessage.isNotEmpty() && name.isBlank()
                )
                FinanceTextField(
                    value = openingBalance,
                    onValueChange = { openingBalance = it },
                    label = { Text(WebI18n.tr("الرصيد الافتتاحي", "Opening balance", lang)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("account_balance_input")
                )
                FinanceTextField(
                    value = institution,
                    onValueChange = { institution = it.take(100) },
                    label = { Text(WebI18n.tr("البنك أو الجهة (اختياري)", "Bank or institution (optional)", lang)) },
                    supportingText = { Text("${institution.length}/100") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("account_institution_input")
                )
                FinanceTextField(
                    value = lastFour,
                    onValueChange = { value -> lastFour = value.filter(Char::isDigit).take(4); validationMessage = "" },
                    label = { Text(WebI18n.tr("آخر 4 أرقام (اختياري)", "Last 4 digits (optional)", lang)) },
                    placeholder = { Text("1234") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("account_last_four_input"),
                    isError = validationMessage.isNotEmpty() && lastFour.isNotEmpty() && lastFour.length != 4
                )
                if (validationMessage.isNotEmpty()) {
                    Text(validationMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            FinanceButton(
                onClick = {
                    val amount = openingBalance.toDoubleOrNull()
                    validationMessage = when {
                        name.trim().isEmpty() -> WebI18n.tr("اسم الحساب مطلوب.", "Account name is required.", lang)
                        name.trim().length > 80 -> WebI18n.tr("اسم الحساب يجب ألا يتجاوز 80 حرفًا.", "Account name must be 80 characters or fewer.", lang)
                        amount == null || !amount.isFinite() -> WebI18n.tr("أدخل رصيدًا افتتاحيًا صحيحًا.", "Enter a valid opening balance.", lang)
                        institution.length > 100 -> WebI18n.tr("اسم البنك أو الجهة يجب ألا يتجاوز 100 حرف.", "Bank or institution must be 100 characters or fewer.", lang)
                        lastFour.isNotEmpty() && !lastFour.matches(Regex("[0-9]{4}")) -> WebI18n.tr("أدخل 4 أرقام بالضبط.", "Enter exactly 4 digits.", lang)
                        else -> ""
                    }
                    if (validationMessage.isEmpty()) {
                        onConfirm(
                            AccountEntity(
                                name = name.trim(),
                                type = selectedType,
                                currency = selectedCurrency,
                                balance = amount!!,
                                institution = institution.trim(),
                                accountNumberSafe = lastFour
                            )
                        )
                    }
                },
                modifier = Modifier.testTag("submit_account_btn")
            ) { Text(WebI18n.tr("حفظ الحساب", "Save account", lang)) }
        },
        dismissButton = {
            FinanceOutlinedButton(onClick = onDismiss) { Text(WebI18n.tr("إلغاء", "Cancel", lang)) }
        }
    )
}

private fun accountTypeLabel(type: AccountType, lang: AppLanguage): String = when (type) {
    AccountType.CASH -> WebI18n.tr("نقدي", "Cash", lang)
    AccountType.BANK -> WebI18n.tr("حساب بنكي", "Bank account", lang)
    AccountType.WALLET -> WebI18n.tr("محفظة إلكترونية", "E-wallet", lang)
    AccountType.CREDIT_CARD -> WebI18n.tr("بطاقة ائتمان", "Credit card", lang)
    AccountType.INVESTMENT -> WebI18n.tr("استثمار", "Investment", lang)
    else -> WebI18n.tr("أخرى", "Other", lang)
}

private fun currencyLabel(currency: String, lang: AppLanguage): String = when (currency) {
    "LYD" -> WebI18n.tr("دينار ليبي", "Libyan dinar", lang) + " (LYD)"
    "USD" -> WebI18n.tr("دولار أمريكي", "US dollar", lang) + " (USD)"
    "EUR" -> WebI18n.tr("يورو", "Euro", lang) + " (EUR)"
    "GBP" -> WebI18n.tr("جنيه إسترليني", "British pound", lang) + " (GBP)"
    "SYP" -> WebI18n.tr("ليرة سورية", "Syrian pound", lang) + " (SYP)"
    else -> WebI18n.tr("دينار أردني", "Jordanian dinar", lang) + " (JOD)"
}
