package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.finance.ai.LocalAiModelViewModel
import java.util.Locale

@Composable
fun LocalAiModelScreen(modelViewModel: LocalAiModelViewModel = viewModel()) {
    val state by modelViewModel.state.collectAsState()
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) modelViewModel.startDownload()
    }
    val requestDownload = {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            modelViewModel.startDownload()
        }
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Gemma 4 E2B", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("نموذج ذكاء اصطناعي محلي مستقل يعمل على الهاتف دون اتصال بعد اكتمال التنزيل. يستمر تنزيله في الخلفية مع إشعار بالتقدم.")
                Text("حجم التنزيل التقريبي: 2.59 GB")
                Text("التشغيل: LiteRT-LM · المعالج CPU")
                Text("الملف يُحفظ داخل مساحة التطبيق ولا يدخل ضمن APK.")
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(state.status, fontWeight = FontWeight.SemiBold)
                if (state.downloading) {
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "${formatBytes(state.downloadedBytes)} / " +
                            if (state.totalBytes > 0L) formatBytes(state.totalBytes) else "جارٍ تحديد الحجم"
                    )
                    Text("${(state.progress * 100).toInt()}%")
                } else if (state.downloadedBytes > 0L && !state.installed) {
                    Text("ملف تنزيل جزئي محفوظ: ${formatBytes(state.downloadedBytes)}")
                }
                state.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                state.testReply.takeIf { it.isNotBlank() }?.let {
                    Text("اختبار النموذج:", fontWeight = FontWeight.Bold)
                    Text(it)
                }
            }
        }

        if (!state.installed) {
            Button(
                onClick = requestDownload,
                enabled = !state.downloading,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (state.downloadedBytes > 0L) "استكمال تنزيل النموذج" else "تنزيل النموذج داخل التطبيق") }
        }
        if (state.downloading) {
            OutlinedButton(onClick = modelViewModel::pauseDownload, modifier = Modifier.fillMaxWidth()) {
                Text("إيقاف مؤقت")
            }
        }
        if (state.installed && !state.loaded) {
            Button(
                onClick = modelViewModel::loadModel,
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (state.loading) "جارٍ تشغيل النموذج…" else "تهيئة وتشغيل النموذج") }
        }
        if (state.loaded) {
            Button(onClick = modelViewModel::runTest, modifier = Modifier.fillMaxWidth()) {
                Text("اختبار عمل النموذج")
            }
        }
        if (state.installed || state.downloadedBytes > 0L) {
            OutlinedButton(onClick = modelViewModel::deleteModel, enabled = !state.downloading, modifier = Modifier.fillMaxWidth()) {
                Text("حذف النموذج وملف التنزيل")
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "يستمر التنزيل عند مغادرة الشاشة أو إغلاق التطبيق، ويظهر إشعار دائم بنسبة التقدم. يتم التحقق من بصمة SHA-256 قبل اعتماد الملف، ويُحفظ الجزء المكتمل لاستكماله لاحقاً.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatBytes(value: Long): String {
    if (value < 1L) return "0 MB"
    return String.format(Locale.US, "%.2f GB", value / 1_000_000_000.0)
}
