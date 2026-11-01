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

    LaunchedEffect(Unit) { scanModels() }

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
        items(modelFiles) { model ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(model.name, fontWeight = FontWeight.SemiBold)
                    Text(String.format(Locale.US, "%.2f GB", model.sizeBytes / 1_073_741_824.0))
                    Text(model.locationLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (model.name.contains("gemma", ignoreCase = true) || model.sizeBytes in 2_300_000_000L..2_900_000_000L) {
                        Text(
                            "قد يتوافق حجمه مع Gemma 3n E2B؛ يلزم التحقق من بيانات النموذج قبل الجزم.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
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
