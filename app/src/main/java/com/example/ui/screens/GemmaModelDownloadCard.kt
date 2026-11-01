package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.finance.ai.GemmaModelDownloadWorker

@Composable
fun GemmaModelDownloadCard() {
    val context = LocalContext.current
    val manager = WorkManager.getInstance(context)
    val infos by manager.getWorkInfosForUniqueWorkFlow(GemmaModelDownloadWorker.UNIQUE_WORK_NAME)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val work = infos.firstOrNull()
    val progress = work?.progress?.getInt(GemmaModelDownloadWorker.KEY_PROGRESS, 0) ?: 0
    val active = work?.state == WorkInfo.State.RUNNING || work?.state == WorkInfo.State.ENQUEUED
    val ready = work?.state == WorkInfo.State.SUCCEEDED
    val status = when (work?.state) {
        WorkInfo.State.ENQUEUED -> "التنزيل بانتظار اتصال بالإنترنت"
        WorkInfo.State.RUNNING -> work.progress.getString(GemmaModelDownloadWorker.KEY_STATUS) ?: "جارٍ التنزيل"
        WorkInfo.State.SUCCEEDED -> "اكتمل التنزيل والتحقق من سلامة النموذج"
        WorkInfo.State.FAILED -> work.outputData.getString(GemmaModelDownloadWorker.KEY_ERROR) ?: "فشل التنزيل"
        WorkInfo.State.CANCELLED -> "تم إيقاف التنزيل؛ يمكن استكماله لاحقاً"
        else -> "النموذج غير محمّل"
    }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Gemma 4 E2B", style = MaterialTheme.typography.titleMedium)
            Text("نموذج محلي مجاني بحجم يقارب 2.58 GB. يُنزّل مرة واحدة ويُحفظ في مساحة التطبيق الدائمة.", style = MaterialTheme.typography.bodySmall)
            Text(status, style = MaterialTheme.typography.bodyMedium)
            if (active || ready) LinearProgressIndicator(
                progress = { if (ready) 1f else progress / 100f },
                modifier = Modifier.fillMaxWidth()
            )
            if (active) Text("$progress%", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = !active && !ready, onClick = {
                    val request = OneTimeWorkRequestBuilder<GemmaModelDownloadWorker>()
                        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                        .build()
                    manager.enqueueUniqueWork(GemmaModelDownloadWorker.UNIQUE_WORK_NAME, ExistingWorkPolicy.KEEP, request)
                }) {
                    Text(if (work?.state == WorkInfo.State.FAILED || work?.state == WorkInfo.State.CANCELLED) "إعادة المحاولة" else "تنزيل النموذج")
                }
                OutlinedButton(enabled = active, onClick = {
                    manager.cancelUniqueWork(GemmaModelDownloadWorker.UNIQUE_WORK_NAME)
                }) { Text("إيقاف") }
            }
            if (ready) Text("الموقع: مساحة التطبيق الخاصة — يبقى عادةً عند تحديث التطبيق فوق النسخة المثبتة.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
