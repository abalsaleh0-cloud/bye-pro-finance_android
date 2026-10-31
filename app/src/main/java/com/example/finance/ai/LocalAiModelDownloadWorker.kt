package com.example.finance.ai

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.CancellationException

class LocalAiModelDownloadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        createNotificationChannel()
        return try {
            setForeground(foregroundInfo("بدء تنزيل نموذج الذكاء الاصطناعي", 0, false))
            val file = LocalAiModelManager.download(applicationContext as android.app.Application) { downloaded, total ->
                val progress = if (total > 0L) ((downloaded * 100L) / total).toInt().coerceIn(0, 100) else 0
                setProgressAsync(workDataOf(KEY_DOWNLOADED to downloaded, KEY_TOTAL to total))
                val notification = buildNotification(
                    "جارٍ تنزيل نموذج Gemma 4 E2B",
                    progress,
                    total > 0L,
                    true
                )
                notificationManager().notify(NOTIFICATION_ID, notification)
            }
            notificationManager().notify(
                NOTIFICATION_ID,
                buildNotification("اكتمل تنزيل نموذج BYE PRO AI والتحقق منه", 100, true, false)
            )
            Result.success(workDataOf(KEY_DOWNLOADED to file.length(), KEY_TOTAL to file.length()))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            notificationManager().notify(
                NOTIFICATION_ID,
                buildNotification("تعذر تنزيل نموذج BYE PRO AI: ${error.message ?: "خطأ غير معروف"}", 0, false, false)
            )
            Result.failure(workDataOf(KEY_ERROR to (error.message ?: "تعذر تنزيل النموذج")))
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "تنزيل نموذج الذكاء الاصطناعي",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "حالة تنزيل نموذج BYE PRO AI في الخلفية" }
            notificationManager().createNotificationChannel(channel)
        }
    }

    private fun foregroundInfo(title: String, progress: Int, indeterminate: Boolean): ForegroundInfo {
        val notification = buildNotification(title, progress, !indeterminate, true)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(title: String, progress: Int, determinate: Boolean, ongoing: Boolean) =
        NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("BYE PRO FINANCE")
            .setContentText(title)
            .setOnlyAlertOnce(true)
            .setOngoing(ongoing)
            .setProgress(100, progress, !determinate)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    private fun notificationManager() =
        applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val UNIQUE_WORK_NAME = "bye-pro-local-ai-model-download"
        const val KEY_DOWNLOADED = "downloaded_bytes"
        const val KEY_TOTAL = "total_bytes"
        const val KEY_ERROR = "download_error"
        private const val CHANNEL_ID = "bye_pro_ai_model_download"
        private const val NOTIFICATION_ID = 4107
    }
}
