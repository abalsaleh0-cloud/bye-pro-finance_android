package com.example.ui.screens

import com.example.ui.components.FinanceIcon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

import androidx.compose.material.icons.filled.*

import androidx.compose.material.icons.Icons


import android.content.Intent
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.finance.engine.FinanceEngine
import com.example.finance.i18n.Strings
import com.example.finance.model.DocumentEntity
import com.example.finance.model.DocumentType
import com.example.finance.model.TransactionEntity
import com.example.finance.model.TransactionType
import com.example.finance.ui.FinanceViewModel

@Composable
fun SmartScanScreen(
    viewModel: FinanceViewModel
) {
    val lang by viewModel.appLanguage.collectAsState()
    val scannedReceipt by viewModel.scannedReceipt.collectAsState()
    val documents by viewModel.documents.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val primaryCurrency by viewModel.primaryCurrency.collectAsState()
    val context = LocalContext.current
    var selectedReceiptUri by remember { mutableStateOf<String?>(null) }

    var statusMessage by remember { mutableStateOf<String?>(null) }
    var manualReceiptText by remember { mutableStateOf("") }

    // Editable confirmation fields
    var merchantField by remember(scannedReceipt) { mutableStateOf(scannedReceipt?.merchant ?: "") }
    var amountField by remember(scannedReceipt) { mutableStateOf(scannedReceipt?.amount?.toString() ?: "") }
    var taxField by remember(scannedReceipt) { mutableStateOf(scannedReceipt?.tax?.toString() ?: "") }
    var categoryField by remember(scannedReceipt) { mutableStateOf(scannedReceipt?.category ?: "Groceries") }

    val ocrError by viewModel.ocrError.collectAsState()

    val receiptPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: error("Unable to read the selected receipt image")
                require(bytes.isNotEmpty() && bytes.size <= 10 * 1024 * 1024) {
                    "Receipt image must be between 1 byte and 10 MB"
                }
                selectedReceiptUri = uri.toString()
                statusMessage = WebI18n.tr("جارٍ تحليل صورة الإيصال…", "Analyzing receipt image…", lang)
                viewModel.scanReceipt(imageBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP))
            }.onFailure {
                statusMessage = WebI18n.tr("تعذر فتح صورة الإيصال: ", "Could not open receipt image: ", lang) + (it.message ?: "")
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = Strings.get("smart_scan_title", lang),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = Strings.get("scan_instruction", lang),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Action Buttons
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
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .border(1.dp, ByeProTheme.colors.financialGreen, CircleShape)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        FinanceIcon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = ByeProTheme.colors.financialGreen,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Text(
                        text = WebI18n.tr("محرك المسح الضوئي الذكي (Gemini OCR Vision)", "Smart scanning engine (Gemini OCR Vision)", lang),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    FinanceButton(
                        onClick = {
                            statusMessage = null
                            receiptPicker.launch(arrayOf("image/jpeg", "image/png", "image/webp"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ByeProTheme.colors.goldPrimary, contentColor = MaterialTheme.colorScheme.onPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("pick_receipt_image_btn")
                    ) {
                        FinanceIcon(Icons.Default.Receipt, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(WebI18n.tr("اختيار صورة إيصال", "Choose receipt image", lang), fontWeight = FontWeight.Bold)
                    }

                    Text(
                        WebI18n.tr("أو الصق نص الإيصال المستخرج من أي ماسح ضوئي", "Or paste receipt text from any scanner", lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FinanceTextField(
                        value = manualReceiptText,
                        onValueChange = { manualReceiptText = it },
                        modifier = Modifier.fillMaxWidth().testTag("manual_receipt_text_input"),
                        minLines = 3,
                        maxLines = 6,
                        label = { Text(WebI18n.tr("نص الإيصال", "Receipt text", lang)) }
                    )
                    FinanceOutlinedButton(
                        onClick = {
                            statusMessage = null
                            viewModel.scanReceipt(sampleText = manualReceiptText)
                        },
                        enabled = manualReceiptText.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FinanceIcon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(WebI18n.tr("تحليل النص", "Analyze text", lang), color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        // Error message if OCR fails without mock fallback
        if (ocrError != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ByeProTheme.colors.financialRed.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = ByeProTheme.colors.financialRed.copy(alpha = 0.12f))
                ) {
                    Text(
                        text = ocrError!!,
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = ByeProTheme.colors.financialRed,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Extracted Confirmation Card (User must review and approve before saving!)
        if (scannedReceipt != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ByeProTheme.colors.goldPrimary.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FinanceIcon(Icons.Default.CheckCircle, contentDescription = null, tint = ByeProTheme.colors.goldPrimary)
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(
                                text = WebI18n.tr("مراجعة وتأكيد البيانات المستخرجة من الإيصال", "Review and confirm extracted receipt data", lang),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = ByeProTheme.colors.goldPrimary
                            )
                        }

                        FinanceTextField(
                            value = merchantField,
                            onValueChange = { merchantField = it },
                            label = { Text(Strings.get("extracted_merchant", lang)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ocr_merchant_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FinanceTextField(
                                value = amountField,
                                onValueChange = { amountField = it },
                                label = { Text(Strings.get("extracted_amount", lang)) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("ocr_amount_input")
                            )
                            FinanceTextField(
                                value = taxField,
                                onValueChange = { taxField = it },
                                label = { Text(Strings.get("extracted_tax", lang)) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        FinanceTextField(
                            value = categoryField,
                            onValueChange = { categoryField = it },
                            label = { Text(Strings.get("extracted_category", lang)) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = WebI18n.tr("الأصناف:", "Items:", lang) + " ${scannedReceipt!!.itemsSummary}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FinanceButton(
                                onClick = {
                                    val amt = amountField.toDoubleOrNull() ?: 0.0
                                    val tax = taxField.toDoubleOrNull() ?: 0.0
                                    val selectedAccount = accounts.firstOrNull()
                                    val receiptCurrency = scannedReceipt?.currency?.trim()?.uppercase()?.takeIf { it.isNotBlank() }
                                    val currencyMatches = selectedAccount != null &&
                                        (receiptCurrency == null || receiptCurrency.equals(selectedAccount.currency, ignoreCase = true))
                                    if (amt.isFinite() && amt > 0.0 && tax.isFinite() && tax >= 0.0 && selectedAccount != null && currencyMatches) {
                                        viewModel.addTransaction(
                                            TransactionEntity(
                                                accountId = selectedAccount.id,
                                                type = TransactionType.EXPENSE,
                                                amount = amt,
                                                currency = selectedAccount.currency,
                                                category = categoryField,
                                                description = WebI18n.tr("فاتورة مشتريات:", "Purchase receipt:", lang) + " $merchantField",
                                                merchant = merchantField,
                                                notes = WebI18n.tr("ضريبة:", "Tax:", lang) + " $tax ${selectedAccount.currency} - extracted via Smart Scan"
                                            )
                                        )
                                        viewModel.addDocument(
                                            DocumentEntity(
                                                title = WebI18n.tr("إيصال:", "Receipt:", lang) + " $merchantField",
                                                type = DocumentType.RECEIPT,
                                                merchant = merchantField,
                                                totalAmount = amt,
                                                taxAmount = tax,
                                                currency = selectedAccount.currency,
                                                fileUri = selectedReceiptUri.orEmpty(),
                                                notes = scannedReceipt!!.itemsSummary
                                            )
                                        )
                                        statusMessage = WebI18n.tr("تم حفظ الفاتورة وتحديث رصيد الحساب المالي بنجاح ✓", "Receipt saved and account balance updated successfully ✓", lang)
                                        selectedReceiptUri = null
                                        viewModel.clearScannedReceipt()
                                    } else if (selectedAccount != null && !currencyMatches) {
                                        statusMessage = WebI18n.tr(
                                            "عملة الإيصال لا تطابق عملة الحساب. لا يمكن حفظه دون سعر صرف موثوق.",
                                            "Receipt currency does not match the account currency. It cannot be saved without a verified exchange rate.",
                                            lang
                                        )
                                    } else {
                                        statusMessage = WebI18n.tr(
                                            "أضف حساباً مالياً أولاً، وأدخل مبلغاً وضريبة صحيحين قبل حفظ الإيصال.",
                                            "Add a financial account first and enter a valid amount and tax before saving the receipt.",
                                            lang
                                        )
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ByeProTheme.colors.financialGreen, contentColor = MaterialTheme.colorScheme.onPrimary),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("confirm_save_ocr_btn")
                            ) {
                                Text(Strings.get("confirm_and_save", lang), fontWeight = FontWeight.Bold)
                            }

                            FinanceOutlinedButton(
                                onClick = { viewModel.clearScannedReceipt() }
                            ) {
                                Text(Strings.get("cancel", lang))
                            }
                        }
                    }
                }
            }
        }

        if (statusMessage != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ByeProTheme.colors.financialGreen.copy(alpha = 0.15f))
                ) {
                    Text(
                        text = statusMessage!!,
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ByeProTheme.colors.financialGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Saved Documents History
        item {
            Text(
                text = WebI18n.tr("المستندات والإيصالات المحفوظة", "Saved documents and receipts", lang) + " (${TextContent.number(documents.size, lang, 0)})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (documents.isEmpty()) {
            item {
                Text(
                    text = WebI18n.tr("لم يتم حفظ أي مستندات أو إيصالات بعد.", "No documents or receipts have been saved yet.", lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(documents, key = { it.id }) { doc ->
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
                            Text(doc.title, fontWeight = FontWeight.Bold)
                            Text(WebI18n.tr("التاجر:", "Merchant:", lang) + " ${doc.merchant} • " + WebI18n.tr("الضريبة:", "Tax:", lang) + " ${TextContent.money(doc.taxAmount, doc.currency, lang)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(TextContent.money(doc.totalAmount, doc.currency, lang), fontWeight = FontWeight.Bold, color = ByeProTheme.colors.financialGreen)
                            FinanceIconButton(onClick = { viewModel.deleteDocument(doc) }) {
                                FinanceIcon(Icons.Default.Delete, contentDescription = WebI18n.tr("حذف", "Delete", lang), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
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
