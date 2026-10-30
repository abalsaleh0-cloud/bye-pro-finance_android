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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Receipt
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

    var statusMessage by remember { mutableStateOf<String?>(null) }

    // Editable confirmation fields
    var merchantField by remember(scannedReceipt) { mutableStateOf(scannedReceipt?.merchant ?: "") }
    var amountField by remember(scannedReceipt) { mutableStateOf(scannedReceipt?.amount?.toString() ?: "") }
    var taxField by remember(scannedReceipt) { mutableStateOf(scannedReceipt?.tax?.toString() ?: "") }
    var categoryField by remember(scannedReceipt) { mutableStateOf(scannedReceipt?.category ?: "Groceries") }

    val ocrError by viewModel.ocrError.collectAsState()

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
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
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
                            viewModel.scanReceipt("شركة أسواق التميمي - فرع الرياض\nتاريخ: 2026-10-30\nالمجموع الفرعي: 162.40 ر.س\nالضريبة: 24.35 ر.س\nالإجمالي: 186.75 ر.س")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ByeProTheme.colors.financialGreen, contentColor = MaterialTheme.colorScheme.onPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("scan_sample_receipt_btn")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(Strings.get("scan_sample", lang), fontWeight = FontWeight.Bold)
                    }

                    FinanceOutlinedButton(
                        onClick = {
                            statusMessage = null
                            viewModel.scanReceipt("مقهى ومحمصة النخبة\nتاريخ: 2026-10-30\nالضريبة: 8.35 ر.س\nالإجمالي: 64.00 ر.س")
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(WebI18n.tr("مسح إيصال مقهى تجريبي", "Scan sample cafe receipt", lang), color = MaterialTheme.colorScheme.onSurface)
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
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ByeProTheme.colors.goldPrimary)
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
                                    val defaultAcc = accounts.firstOrNull()?.id ?: 1L

                                    if (amt > 0.0) {
                                        // 1. Add Transaction
                                        viewModel.addTransaction(
                                            TransactionEntity(
                                                accountId = defaultAcc,
                                                type = TransactionType.EXPENSE,
                                                amount = amt,
                                                currency = "SAR",
                                                category = categoryField,
                                                description = WebI18n.tr("فاتورة مشتريات:", "Purchase receipt:", lang) + " $merchantField",
                                                merchant = merchantField,
                                                notes = WebI18n.tr("ضريبة:", "Tax:", lang) + " $tax SAR - extracted via Smart Scan"
                                            )
                                        )

                                        // 2. Add Document
                                        viewModel.addDocument(
                                            DocumentEntity(
                                                title = WebI18n.tr("إيصال:", "Receipt:", lang) + " $merchantField",
                                                type = DocumentType.RECEIPT,
                                                merchant = merchantField,
                                                totalAmount = amt,
                                                taxAmount = tax,
                                                notes = scannedReceipt!!.itemsSummary
                                            )
                                        )

                                        statusMessage = WebI18n.tr("تم حفظ الفاتورة وتحديث رصيد الحساب المالي بنجاح ✓", "Receipt saved and account balance updated successfully ✓", lang)
                                        viewModel.clearScannedReceipt()
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
                text = WebI18n.tr("المستندات والإيصالات المحفوظة", "Saved documents and receipts", lang) + " (${documents.size})",
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
                            Text(WebI18n.tr("التاجر:", "Merchant:", lang) + " ${doc.merchant} • " + WebI18n.tr("الضريبة:", "Tax:", lang) + " ${TextContent.money(doc.taxAmount, "SAR", lang)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(TextContent.money(doc.totalAmount, "SAR", lang), fontWeight = FontWeight.Bold, color = ByeProTheme.colors.financialGreen)
                            FinanceIconButton(onClick = { viewModel.deleteDocument(doc) }) {
                                Icon(Icons.Default.Delete, contentDescription = WebI18n.tr("حذف", "Delete", lang), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
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
