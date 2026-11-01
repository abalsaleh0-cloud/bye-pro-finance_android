package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

private data class LocalModelFile(
    val name: String,
    val sizeBytes: Long,
    val locationLabel: String
)

private data class SelectedModelInfo(
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val detectedFormat: String,
    val headerHex: String
)

private fun inspectSelectedModel(context: android.content.Context, uri: Uri): SelectedModelInfo {
    var name = uri.lastPathSegment?.substringAfterLast('/') ?: "ملف بدون اسم"
    var size = -1L
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
        ?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex >= 0) name = cursor.getString(nameIndex) ?: name
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
            }
        }
    val header = context.contentResolver.openInputStream(uri)?.use { input ->
        ByteArray(16).also { buffer ->
            var offset = 0
            while (offset < buffer.size) {
                val count = input.read(buffer, offset, buffer.size - offset)
                if (count <= 0) break
                offset += count
            }
        }.takeWhileIndexed { index, byte -> index < 16 && byte != 0.toByte() || index < 4 }
            .toByteArray()
    } ?: byteArrayOf()
    val extension = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
    val format = when {
        header.size >= 4 && header.copyOfRange(0, 4).decodeToString() == "GGUF" -> "GGUF"
        header.size >= 8 && header.copyOfRange(4, 8).decodeToString() == "TFL3" -> "TensorFlow Lite"
        extension == "litertlm" -> "LiteRT-LM (امتداد الملف؛ يحتاج تحققاً إضافياً)"
        extension == "task" -> "ملف Task (الامتداد لا يثبت توافق المحرك)"
        extension == "onnx" -> "ONNX"
        extension == "safetensors" -> "SafeTensors"
        extension == "bin" -> "BIN غير محدد"
        else -> "صيغة غير معروفة"
    }
    return SelectedModelInfo(
        uri = uri,
        name = name,
        sizeBytes = size,
        detectedFormat = format,
        headerHex = header.joinToString(" ") { "%02X".format(it) }.ifBlank { "غير متاح" }
    )
}

private inline fun ByteArray.takeWhileIndexed(predicate: (Int, Byte) -> Boolean): List<Byte> {
    val result = mutableListOf<Byte>()
    forEachIndexed { index, byte -> if (predicate(index, byte)) result.add(byte) }
    return result
}

@Composable
fun AiModelsSettingsScreen(
    autoSpeakReplies: Boolean,
    onAutoSpeakRepliesChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var modelFiles by remember { mutableStateOf<List<LocalModelFile>>(emptyList()) }
    var isScanning by remember { mutableStateOf(false) }
    var scanCompleted by remember { mutableStateOf(false) }
    var selectedModel by remember { mutableStateOf<SelectedModelInfo?>(null) }
    var modelInspectionError by remember { mutableStateOf<String?>(null) }
    val modelPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                modelInspectionError = null
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                runCatching {
                    withContext(Dispatchers.IO) { inspectSelectedModel(context, uri) }
                }.onSuccess { info ->
                    selectedModel = info
                    context.getSharedPreferences("bye_pro_finance_ai_models", android.content.Context.MODE_PRIVATE)
                        .edit().putString("selected_model_uri", uri.toString()).apply()
                }.onFailure { error ->
                    modelInspectionError = error.message ?: "تعذر قراءة بيانات الملف"
                }
            }
        }
    }

    suspend fun scanModels() {
        isScanning = true
        modelFiles = withContext(Dispatchers.IO) {
            val roots = buildList {
                add(context.filesDir to "تخزين التطبيق")
                add(context.noBackupFilesDir to "تخزين النماذج")
                context.getExternalFilesDirs(null).filterNotNull().forEach {
                    add(it to "التخزين الخارجي الخاص بالتطبيق")
                }
                add(context.cacheDir to "ذاكرة مؤقتة")
            }
            val candidates = mutableListOf<LocalModelFile>()
            roots.forEach { (root, label) ->
                if (root.exists() && root.isDirectory) {
                    runCatching {
                        root.walkTopDown().maxDepth(5)
                            .filter { it.isFile && it.length() >= 100L * 1024L * 1024L }
                            .take(30)
                            .forEach { file ->
                                candidates.add(LocalModelFile(file.name, file.length(), label))
                            }
                    }
                }
            }
            candidates.distinctBy { it.name to it.sizeBytes }
                .sortedByDescending { it.sizeBytes }
                .take(12)
        }
        scanCompleted = true
        isScanning = false
    }

    LaunchedEffect(Unit) {
        scanModels()
        val savedUri = context.getSharedPreferences("bye_pro_finance_ai_models", android.content.Context.MODE_PRIVATE)
            .getString("selected_model_uri", null)
        if (!savedUri.isNullOrBlank()) {
            runCatching {
                withContext(Dispatchers.IO) { inspectSelectedModel(context, Uri.parse(savedUri)) }
            }.onSuccess { selectedModel = it }
                .onFailure { modelInspectionError = "تعذر الوصول إلى الملف المحفوظ؛ اختر الملف مجدداً." }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("النماذج المحلية", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "إدارة ملفات النماذج المحلية المحفوظة على هاتفك والتحقق من وجودها.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.size(8.dp))
                        Text("فحص ملفات النماذج", fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        text = when {
                            isScanning -> "جارٍ فحص مجلدات التطبيق…"
                            !scanCompleted -> "لم يبدأ الفحص بعد"
                            modelFiles.isEmpty() -> "لم يتم العثور على ملف نموذج كبير داخل مجلدات التطبيق التي أمكن فحصها. هذا لا يثبت أن النموذج حُذف؛ قد يكون محفوظاً في موقع آخر."
                            else -> "تم العثور على ${modelFiles.size} ملف كبير. الاسم والحجم أدناه لا يكفيان وحدهما لتأكيد نوع النموذج."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { scope.launch { scanModels() } },
                        enabled = !isScanning,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.size(6.dp))
                        Text("إعادة فحص الملفات")
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("استعادة النموذج الموجود على الهاتف", fontWeight = FontWeight.SemiBold)
                    Text(
                        "اختر ملف النموذج الذي سبق تنزيله. سيقرأ التطبيق الاسم والحجم وأول 16 بايت فقط، ولن يرفع الملف أو ينزّله أو يحذفه.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { modelPicker.launch(arrayOf("*/*")) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Storage, contentDescription = null)
                        Spacer(Modifier.size(6.dp))
                        Text("اختيار ملف النموذج من الهاتف")
                    }
                    selectedModel?.let { model ->
                        Text(model.name, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (model.sizeBytes >= 0) String.format(Locale.US, "%.2f GB", model.sizeBytes / 1_073_741_824.0)
                            else "الحجم غير متاح من مزود الملفات",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text("الصيغة المكتشفة: ${model.detectedFormat}", style = MaterialTheme.typography.bodySmall)
                        Text("ترويسة الملف: ${model.headerHex}", style = MaterialTheme.typography.labelSmall)
                        Text(
                            "تم حفظ إذن الوصول إلى الملف. هذه قراءة بيانات فقط؛ لم يتم بعد تأكيد أن الملف هو Gemma 4 E2B أو أنه متوافق مع LiteRT-LM.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    modelInspectionError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        items(modelFiles) { model ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(model.name, fontWeight = FontWeight.SemiBold)
                    Text(String.format(Locale.US, "%.2f GB", model.sizeBytes / 1_073_741_824.0))
                    Text(model.locationLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "لا يمكن تحديد اسم النموذج من الحجم وحده؛ اختر الملف أعلاه لفحص ترويسته.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item {
            Text("إعدادات المساعد", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.size(8.dp))
                        Text("الصوت والاستجابة", fontWeight = FontWeight.SemiBold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("قراءة رد المساعد صوتياً بعد التحدث")
                            Text(
                                "يستخدم محرك تحويل النص إلى كلام المتاح على الهاتف.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = autoSpeakReplies, onCheckedChange = onAutoSpeakRepliesChange)
                    }
                    Text(
                        "لغة الرد: تتبع لغة التطبيق حالياً.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("الخصوصية والحفاظ على النموذج", fontWeight = FontWeight.SemiBold)
                    Text(
                        "لا تضغط على مسح بيانات التطبيق ولا تحذف التطبيق إذا كنت تريد الحفاظ على النموذج. تحديث APK فوق النسخة المثبتة يحافظ عادةً على ملفات التطبيق الخاصة، لكن مسح البيانات أو إلغاء التثبيت قد يحذفها.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "هذه الصفحة تفحص الملفات وتعرض الإعدادات فقط؛ لا تحذف النماذج ولا تنزّل نموذجاً جديداً ولا تدّعي تشغيله ما لم يتم ربط محرك محلي فعلي.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
