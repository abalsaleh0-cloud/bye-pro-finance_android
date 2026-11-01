package com.example.finance.ai

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.StatFs
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

/** Resumable, checksum-pinned download stored in app-private persistent storage. */
class GemmaModelDownloadWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val dir = File(applicationContext.filesDir, MODEL_DIRECTORY)
        if (!dir.exists() && !dir.mkdirs()) return Result.failure(workDataOf(KEY_ERROR to "تعذر إنشاء مجلد النموذج"))
        val target = File(dir, MODEL_FILE)
        val part = File(dir, MODEL_FILE + ".part")
        if (target.isFile && target.length() > MIN_MODEL_BYTES && sha256(target) == EXPECTED_SHA256)
            return Result.success(workDataOf(KEY_MODEL_PATH to target.absolutePath))
        if (StatFs(dir.absolutePath).availableBytes < MIN_FREE_BYTES)
            return Result.failure(workDataOf(KEY_ERROR to "المساحة الحرة غير كافية؛ يلزم توفير 3.1 GB على الأقل"))
        setForeground(foreground(0, "جارٍ تجهيز تنزيل Gemma 4 E2B"))
        return try {
            download(part)
            coroutineContext.ensureActive()
            if (!part.isFile || part.length() < MIN_MODEL_BYTES)
                return Result.failure(workDataOf(KEY_ERROR to "حجم النموذج بعد التنزيل أصغر من المتوقع"))
            setProgress(workDataOf(KEY_PROGRESS to 99, KEY_STATUS to "التحقق من سلامة النموذج"))
            if (sha256(part) != EXPECTED_SHA256) {
                part.delete()
                return Result.failure(workDataOf(KEY_ERROR to "فشل التحقق من بصمة SHA-256؛ حُذف الملف غير السليم"))
            }
            if (target.exists() && !target.delete())
                return Result.failure(workDataOf(KEY_ERROR to "تعذر استبدال ملف النموذج القديم"))
            if (!part.renameTo(target))
                return Result.failure(workDataOf(KEY_ERROR to "تعذر تثبيت ملف النموذج بعد التنزيل"))
            setProgress(workDataOf(KEY_PROGRESS to 100, KEY_STATUS to "اكتمل التنزيل والتحقق"))
            Result.success(workDataOf(KEY_MODEL_PATH to target.absolutePath))
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: IOException) {
            Result.retry()
        } catch (e: Exception) {
            Result.failure(workDataOf(KEY_ERROR to (e.message ?: "حدث خطأ غير متوقع أثناء التنزيل")))
        }
    }

    private suspend fun download(part: File) {
        var offset = if (part.exists()) part.length() else 0L
        val connection = (URL(DOWNLOAD_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 30_000
            readTimeout = 60_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "BYE-PRO-FINANCE-Android/10.0.65")
            if (offset > 0L) setRequestProperty("Range", "bytes=" + offset + "-")
        }
        try {
            connection.connect()
            val code = connection.responseCode
            if (code !in 200..299) throw IOException("HTTP " + code)
            if (offset > 0L && code != HttpURLConnection.HTTP_PARTIAL) {
                part.delete()
                offset = 0L
            }
            val remain = connection.contentLengthLong.coerceAtLeast(0L)
            val total = if (code == HttpURLConnection.HTTP_PARTIAL)
                connection.getHeaderField("Content-Range")?.substringAfterLast('/')?.toLongOrNull() ?: (offset + remain)
            else remain
            connection.inputStream.use { input ->
                FileOutputStream(part, offset > 0L).use { output ->
                    val buffer = ByteArray(1024 * 1024)
                    var countTotal = offset
                    var previous = -1
                    while (true) {
                        coroutineContext.ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        countTotal += count
                        val percent = if (total > 0L) ((countTotal * 100L) / total).toInt().coerceIn(0, 99) else -1
                        if (percent != previous) {
                            previous = percent
                            setProgress(workDataOf(KEY_PROGRESS to percent, KEY_STATUS to "جارٍ تنزيل Gemma 4 E2B"))
                            setForeground(foreground(percent, "جارٍ تنزيل Gemma 4 E2B"))
                        }
                    }
                    output.fd.sync()
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered(1024 * 1024).use { input ->
            val buffer = ByteArray(1024 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun foreground(progress: Int, message: String): ForegroundInfo {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "تنزيل نموذج الذكاء الاصطناعي", NotificationManager.IMPORTANCE_LOW))
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("BYE PRO AI")
            .setContentText(message)
            .setProgress(100, progress.coerceAtLeast(0), progress < 0)
            .setOngoing(true).setOnlyAlertOnce(true).build()
        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    companion object {
        const val UNIQUE_WORK_NAME = "bye_pro_gemma4_e2b_download"
        const val KEY_PROGRESS = "download_progress"
        const val KEY_STATUS = "download_status"
        const val KEY_MODEL_PATH = "model_path"
        const val KEY_ERROR = "download_error"
        const val MODEL_FILE = "gemma-4-E2B-it.litertlm"
        const val MODEL_DIRECTORY = "ai-models"
        const val EXPECTED_SHA256 = "181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c"
        const val MIN_MODEL_BYTES = 2_300_000_000L
        const val MIN_FREE_BYTES = 3_100_000_000L
        const val DOWNLOAD_URL = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/6b78abd019e61a1ca4cbe3b212d2c9ce8ff38a94/gemma-4-E2B-it.litertlm"
        private const val CHANNEL_ID = "bye_pro_gemma_download"
        private const val NOTIFICATION_ID = 6402
    }
}
