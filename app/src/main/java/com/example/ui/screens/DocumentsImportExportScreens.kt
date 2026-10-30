package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.finance.i18n.TextContent
import com.example.finance.i18n.WebI18n
import com.example.finance.model.DocumentType
import com.example.finance.ui.FinanceViewModel
import com.example.ui.components.FinanceButton
import com.example.ui.components.FinanceOutlinedButton

@Composable
fun DocumentsScreen(viewModel: FinanceViewModel) {
    val lang by viewModel.appLanguage.collectAsState()
    val documents by viewModel.documents.collectAsState()
    val primaryCurrency by viewModel.primaryCurrency.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(WebI18n.tr("الأرشيف المالي", "Financial archive", lang), style = MaterialTheme.typography.titleLarge)
            Text(
                WebI18n.tr("المستندات والإيصالات المحفوظة داخل التطبيق.", "Documents and receipts saved in this app.", lang),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (documents.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Text(
                        WebI18n.tr("لا توجد مستندات محفوظة حتى الآن.", "No documents have been saved yet.", lang),
                        modifier = Modifier.padding(18.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(documents, key = { it.id }) { document ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(document.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                document.merchant.ifBlank { WebI18n.tr("مستند مالي", "Financial document", lang) },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                TextContent.money(document.totalAmount, primaryCurrency, lang),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { viewModel.deleteDocument(document) }) {
                            Icon(Icons.Default.Delete, contentDescription = WebI18n.tr("حذف المستند", "Delete document", lang))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImportExportScreen(viewModel: FinanceViewModel) {
    val lang by viewModel.appLanguage.collectAsState()
    val context = LocalContext.current
    var status by remember { mutableStateOf<String?>(null) }

    val csvExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            status = runCatching {
                val output = context.contentResolver.openOutputStream(uri)
                    ?: error("Unable to open export destination")
                output.bufferedWriter(Charsets.UTF_8).use { it.write(viewModel.generateCsvExport()) }
                WebI18n.tr("تم تصدير ملف CSV بنجاح.", "CSV export completed.", lang)
            }.getOrElse { WebI18n.tr("تعذر تصدير الملف: ", "Export failed: ", lang) + (it.message ?: "") }
        }
    }
    val jsonExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            status = runCatching {
                val output = context.contentResolver.openOutputStream(uri)
                    ?: error("Unable to open export destination")
                output.bufferedWriter(Charsets.UTF_8).use { it.write(viewModel.generateJsonExport()) }
                WebI18n.tr("تم تصدير ملف JSON بنجاح.", "JSON export completed.", lang)
            }.getOrElse { WebI18n.tr("تعذر تصدير الملف: ", "Export failed: ", lang) + (it.message ?: "") }
        }
    }
    val jsonImport = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val readResult = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    ?: error("Unable to read selected file")
            }
            readResult.onSuccess { json ->
                viewModel.importTransactionsFromJson(json) { result ->
                    status = result.fold(
                        { count -> WebI18n.tr("تم استيراد ", "Imported ", lang) + TextContent.number(count, lang, 0) + WebI18n.tr(" معاملة بنجاح.", " transactions successfully.", lang) },
                        { error -> WebI18n.tr("فشل الاستيراد: ", "Import failed: ", lang) + (error.message ?: "") }
                    )
                }
            }.onFailure { status = WebI18n.tr("تعذر قراءة الملف: ", "Unable to read file: ", lang) + (it.message ?: "") }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(WebI18n.tr("استيراد وتصدير البيانات", "Import & export", lang), style = MaterialTheme.typography.titleLarge)
            Text(
                WebI18n.tr("احفظ معاملاتك أو استعدها من ملف JSON متوافق.", "Export your transactions or restore them from a compatible JSON file.", lang),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(WebI18n.tr("تصدير البيانات", "Export data", lang), style = MaterialTheme.typography.titleMedium)
                    FinanceButton(onClick = { csvExport.launch("bye-pro-finance-transactions.csv") }, modifier = Modifier.fillMaxWidth()) {
                        Text(WebI18n.tr("تصدير المعاملات CSV", "Export transactions CSV", lang))
                    }
                    FinanceOutlinedButton(onClick = { jsonExport.launch("bye-pro-finance-transactions.json") }, modifier = Modifier.fillMaxWidth()) {
                        Text(WebI18n.tr("تصدير المعاملات JSON", "Export transactions JSON", lang))
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(WebI18n.tr("استيراد البيانات", "Import data", lang), style = MaterialTheme.typography.titleMedium)
                    Text(
                        WebI18n.tr("يجب أن يحتوي الملف على معاملات مرتبطة بحسابات موجودة وبالعملة نفسها. تتم العملية كوحدة واحدة؛ إذا فشل صف واحد يُلغى الاستيراد كله.", "The file must contain transactions linked to existing accounts in the same currency. Import is atomic: if one row fails, the entire import is rolled back.", lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FinanceButton(onClick = { jsonImport.launch("application/json") }, modifier = Modifier.fillMaxWidth()) {
                        Text(WebI18n.tr("اختيار ملف JSON للاستيراد", "Choose JSON file to import", lang))
                    }
                }
            }
        }
        if (status != null) {
            item {
                Text(status!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
        item { Spacer(modifier = Modifier.height(60.dp)) }
    }
}
