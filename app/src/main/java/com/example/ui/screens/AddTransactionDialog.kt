package com.example.ui.screens

import androidx.compose.runtime.Composable
import com.example.finance.model.AccountEntity
import com.example.finance.model.AppLanguage
import com.example.finance.model.TransactionEntity

/** Compatibility entry point used by the global quick-add action. */
@Composable
fun AddTransactionDialog(
    accounts: List<AccountEntity>,
    lang: AppLanguage,
    onDismiss: () -> Unit,
    onConfirm: (TransactionEntity) -> Unit
) {
    AddTransactionForm(
        accounts = accounts,
        lang = lang,
        onCancel = onDismiss,
        onSave = onConfirm
    )
}
