package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.ui.components.FinanceOutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.example.finance.ai.GgufMetadataInspector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.io.File
import com.example.ui.theme.ByeProTheme
import com.example.finance.model.AppLanguage
import com.example.finance.i18n.WebI18n

private data class LocalModelFile(
    val name: String,
    val sizeBytes: Long,
    val locationLabel: String,
    val file: File
)

private data class SelectedModelInfo(
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val detectedFormat: String,
    val headerHex: String,
    val metadataSummary: String? = null
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
    val metadataSummary = if (format == "GGUF") {
        runCatching {
            context.contentResolver.openInputStream(uri)?.use(GgufMetadataInspector::inspect)?.toDisplayText()
                ?: "تعذر فتح ملف GGUF."
        }.getOrElse { "تعذر قراءة بيانات GGUF: ${it.message ?: "ملف غير صالح"}" }
    } else null

    return SelectedModelInfo(
        uri = uri,
        name = name,
        sizeBytes = size,
        detectedFormat = format,
        headerHex = header.joinToString(" ") { "%02X".format(it) }.ifBlank { "غير متاح" },
        metadataSummary = metadataSummary
    )
}

private inline fun ByteArray.takeWhileIndexed(predicate: (Int, Byte) -> Boolean): List<Byte> {
    val result = mutableListOf<Byte>()
    forEachIndexed { index, byte -> if (predicate(index, byte)) result.add(byte) }
    return result
}

@Composable
fun AiModelsSettingsScreen(
    lang: AppLanguage,
    autoSpeakReplies: Boolean,
    onAutoSpeakRepliesChange: (Boolean) -> Unit,
    cloudGeneralEnabled: Boolean,
    onCloudGeneralEnabledChange: (Boolean) -> Unit,
    localModelReady: Boolean,
    localModelLoading: Boolean,
    localModelProgress: Float,
    localModelStatus: String,
    onLoadModelFile: (File) -> Unit,
    onImportAndLoadModel: (Uri) -> Unit,
    onDownloadRecommendedModel: () -> Unit
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
                                candidates.add(LocalModelFile(file.name, file.length(), label, file))
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
            Card(
                modifier = Modifier.fillMaxWidth().border(1.dp, ByeProTheme.colors.goldPrimary.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = ByeProTheme.colors.surfaceMain)
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("MODEL LAB  /  AI BYE PRO", style = MaterialTheme.typography.labelSmall, color = ByeProTheme.colors.goldPrimary, fontWeight = FontWeight.Bold)
                    Text(WebI18n.tr("النماذج والقدرات", "Models & capabilities", lang), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = ByeProTheme.colors.textPrimary)
                    Text(WebI18n.tr("إدارة النماذج المحلية والهجينة، حالة التشغيل، الصوت والخصوصية من مساحة موحدة.", "Manage local and hybrid models, runtime status, voice and privacy in one workspace.", lang), style = MaterialTheme.typography.bodySmall, color = ByeProTheme.colors.textSecondary)
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = ByeProTheme.colors.cardMain)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        WebI18n.tr("النموذج الموصى به لهذا الهاتف", "Recommended model for this phone", lang),
                        fontWeight = FontWeight.SemiBold,
                        color = ByeProTheme.colors.textPrimary
                    )
                    Text(
                        "Gemma 3n E2B Instruct · GGUF · Q4_K_M · ≈ 2.79 GB",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ByeProTheme.colors.goldPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        WebI18n.tr(
                            "يمكن تنزيله مباشرة إلى مساحة التطبيق الخاصة. يتطلب اتصال إنترنت ومساحة تخزين كافية، وقد يحتاج إغلاق التطبيقات الأخرى قبل التحميل بسبب الذاكرة المطلوبة للتشغيل.",
                            "Download directly into the app's private storage. Internet and sufficient free storage are required; closing other apps may be necessary before loading due to runtime memory needs.",
                            lang
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FinanceOutlinedButton(
                        onClick = onDownloadRecommendedModel,
                        enabled = !localModelLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (localModelLoading) WebI18n.tr("جارٍ تجهيز النموذج...", "Preparing model...", lang)
                            else WebI18n.tr("تنزيل Gemma 3n داخل التطبيق", "Download Gemma 3n in app", lang)
                        )
                    }
                    Text(
                        WebI18n.tr(
                            "النموذج يُنزّل من Hugging Face وفق ترخيص Gemma. لا يُرسل ملف النموذج أو المحادثات إلى خادم BYE PRO.",
                            "The model is downloaded from Hugging Face under the Gemma license. The model file and conversations are not sent to BYE PRO servers.",
                            lang
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                WebI18n.tr("إدارة ملفات النماذج المحلية المحفوظة على هاتفك والتحقق من وجودها.", "Manage local model files stored on your phone and verify their availability.", lang),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = ByeProTheme.colors.cardMain)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = ByeProTheme.colors.goldPrimary)
                        Spacer(Modifier.size(8.dp))
                        Text(WebI18n.tr("فحص ملفات النماذج", "Model file scanner", lang), fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        text = when {
                            isScanning -> WebI18n.tr("جارٍ فحص مجلدات التطبيق…", "Scanning app folders…", lang)
                            !scanCompleted -> WebI18n.tr("لم يبدأ الفحص بعد", "Scan has not started", lang)
                            modelFiles.isEmpty() -> WebI18n.tr("لم يتم العثور على ملف نموذج كبير داخل مجلدات التطبيق التي أمكن فحصها. هذا لا يثبت أن النموذج حُذف؛ قد يكون محفوظاً في موقع آخر.", "No large model file was found in the app folders scanned. This does not prove the model was deleted; it may be stored elsewhere.", lang)
                            else -> WebI18n.tr("تم العثور على ${modelFiles.size} ملف كبير. الاسم والحجم أدناه لا يكفيان وحدهما لتأكيد نوع النموذج.", "Found ${modelFiles.size} large files. File name and size alone cannot confirm the model type.", lang)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FinanceOutlinedButton(
                        onClick = { scope.launch { scanModels() } },
                        enabled = !isScanning,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.size(6.dp))
                        Text(WebI18n.tr("إعادة فحص الملفات", "Rescan files", lang))
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = ByeProTheme.colors.cardMain)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(WebI18n.tr("استعادة النموذج الموجود على الهاتف", "Restore a model already on this phone", lang), fontWeight = FontWeight.SemiBold)
                    Text(
                        WebI18n.tr("اختر ملف النموذج الذي سبق تنزيله. سيقرأ التطبيق الاسم والحجم وأول 16 بايت فقط، ولن يرفع الملف أو ينزّله أو يحذفه.", "Choose a model file already downloaded. The app reads only its name, size and first 16 bytes; it will not upload, download or delete the file.", lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FinanceOutlinedButton(
                        onClick = { modelPicker.launch(arrayOf("*/*")) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Storage, contentDescription = null)
                        Spacer(Modifier.size(6.dp))
                        Text(WebI18n.tr("اختيار ملف النموذج من الهاتف", "Choose model file from phone", lang))
                    }
                    selectedModel?.let { model ->
                        Text(model.name, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (model.sizeBytes >= 0) String.format(Locale.US, "%.2f GB", model.sizeBytes / 1_073_741_824.0)
                            else WebI18n.tr("الحجم غير متاح من مزود الملفات", "File size unavailable from provider", lang),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(WebI18n.tr("الصيغة المكتشفة: ", "Detected format: ", lang) + model.detectedFormat, style = MaterialTheme.typography.bodySmall)
                        Text(WebI18n.tr("ترويسة الملف: ", "File header: ", lang) + model.headerHex, style = MaterialTheme.typography.labelSmall)
                        model.metadataSummary?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall)
                        }
                        if (model.detectedFormat == "GGUF") {
                            Text(
                                WebI18n.tr("تم التحقق من بيانات GGUF. سيُشغّل عبر محرك llama.cpp المحلي على CPU، دون رفع الملف إلى السحابة.", "GGUF metadata is validated. The file will run through the local llama.cpp CPU engine without cloud upload.", lang),
                                style = MaterialTheme.typography.bodySmall,
                                color = ByeProTheme.colors.goldPrimary
                            )
                        }
                        Text(
                            when {
                                model.detectedFormat == "GGUF" ->
                                    WebI18n.tr("سيتم نسخ GGUF إلى مساحة التطبيق الخاصة وتشغيله للمحادثة النصية فقط. لا يدعم هذا المحرك تحليل الصور.", "GGUF is copied into app-private storage and used for text chat only. This engine does not analyze images.", lang)
                                model.name.endsWith(".litertlm", ignoreCase = true) && model.name.contains("gemma-3n", ignoreCase = true) ->
                                    WebI18n.tr("هذا نموذج الصور Gemma 3n. سيُحفظ في مسار مستقل لتحليل الصور، ولن يستبدل نموذج المحادثة النصية أو إعداداته.", "This is the Gemma 3n vision model. It is saved separately for image analysis and will not replace your text chat model or its settings.", lang)
                                else ->
                                    WebI18n.tr("حزمة LiteRT-LM متوافقة للمحادثة المحلية. تُنسخ إلى مساحة التطبيق الخاصة ولا يتم إرسالها إلى الإنترنت.", "A compatible LiteRT-LM package for local chat. It is copied into app-private storage and is never sent to the internet.", lang)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = ByeProTheme.colors.goldPrimary
                        )
                        FinanceOutlinedButton(
                            onClick = { onImportAndLoadModel(model.uri) },
                            enabled = !localModelLoading && (
                                model.name.endsWith(".litertlm", ignoreCase = true) ||
                                    model.name.endsWith(".gguf", ignoreCase = true)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                when {
                                    localModelLoading -> WebI18n.tr("جارٍ تجهيز النموذج...", "Preparing model...", lang)
                                    model.name.endsWith(".litertlm", ignoreCase = true) && model.name.contains("gemma-3n", ignoreCase = true) ->
                                        WebI18n.tr("حفظ نموذج الصور بشكل مستقل", "Save vision model separately", lang)
                                    else -> WebI18n.tr("نسخ النموذج وتشغيله محلياً", "Copy and run model locally", lang)
                                }
                            )
                        }
                    }
                    if (localModelLoading) {
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = { localModelProgress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Text(
                        localModelStatus,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (localModelReady) ByeProTheme.colors.goldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    modelInspectionError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        items(modelFiles) { model ->
            Card(colors = CardDefaults.cardColors(containerColor = ByeProTheme.colors.surfaceMain)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(model.name, fontWeight = FontWeight.SemiBold)
                    Text(String.format(Locale.US, "%.2f GB", model.sizeBytes / 1_073_741_824.0))
                    Text(model.locationLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        WebI18n.tr("لا يمكن تحديد اسم النموذج من الحجم وحده. يمكن تشغيل حزم LiteRT-LM من هذا المجلد مباشرة.", "A model cannot be identified by file size alone. LiteRT-LM packages can be launched directly from this folder.", lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (model.file.extension.equals("litertlm", ignoreCase = true)) {
                        FinanceOutlinedButton(
                            onClick = { onLoadModelFile(model.file) },
                            enabled = !localModelLoading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(WebI18n.tr("تشغيل هذا النموذج دون نسخه", "Run this model without copying", lang))
                        }
                    }
                }
            }
        }
        item {
            Text(WebI18n.tr("إعدادات المساعد", "Assistant settings", lang), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = ByeProTheme.colors.textPrimary)
        }
        item {
            VoiceProviderSettingsCard(lang = lang)
        }
        item {
            HybridAssistantSettingsCard(
                lang = lang,
                cloudGeneralEnabled = cloudGeneralEnabled,
                onCloudGeneralEnabledChange = onCloudGeneralEnabledChange
            )
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = ByeProTheme.colors.cardMain)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = ByeProTheme.colors.goldPrimary)
                        Spacer(Modifier.size(8.dp))
                        Text(WebI18n.tr("الصوت والاستجابة", "Voice & response", lang), fontWeight = FontWeight.SemiBold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(WebI18n.tr("قراءة رد المساعد صوتياً بعد التحدث", "Speak assistant replies automatically", lang))
                            Text(
                                WebI18n.tr("يستخدم الصوت السحابي الطبيعي عند ضبط ElevenLabs، مع الرجوع إلى صوت الهاتف عند عدم الاتصال.", "Uses natural cloud speech when ElevenLabs is configured, with device speech as an offline fallback.", lang),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FinanceSwitch(checked = autoSpeakReplies, onCheckedChange = onAutoSpeakRepliesChange)
                    }
                    Text(
                        WebI18n.tr("لغة الرد: تتبع لغة التطبيق حالياً.", "Reply language currently follows the app language.", lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = ByeProTheme.colors.cardMain)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(WebI18n.tr("الخصوصية والحفاظ على النموذج", "Privacy & model retention", lang), fontWeight = FontWeight.SemiBold)
                    Text(
                        WebI18n.tr("لا تضغط على مسح بيانات التطبيق ولا تحذف التطبيق إذا كنت تريد الحفاظ على النموذج. تحديث APK فوق النسخة المثبتة يحافظ عادةً على ملفات التطبيق الخاصة، لكن مسح البيانات أو إلغاء التثبيت قد يحذفها.", "Do not clear app data or uninstall if you want to keep the model. Installing an APK update over the existing app normally preserves private app files, while clearing data or uninstalling may remove them.", lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        WebI18n.tr("يعمل محرك LiteRT-LM محلياً مع محاولة استخدام GPU ثم الرجوع إلى CPU عند عدم التوافق. لا تُحذف الملفات الأصلية؛ عند اختيار ملف من التنزيلات تُنشأ نسخة داخل التطبيق. تحليل الصور يستخدم نموذج Gemma 3n LiteRT-LM منفصلاً؛ دعم الصوت غير مفعّل بعد.", "LiteRT-LM runs locally, preferring GPU and falling back to CPU when unsupported. Original files are not deleted; selecting a Downloads file creates an app-private copy. Image analysis uses a separate Gemma 3n LiteRT-LM model; audio input is not enabled yet.", lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
