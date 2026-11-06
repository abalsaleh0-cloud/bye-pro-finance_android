package com.example.finance.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.finance.data.AppDatabase
import com.example.finance.model.NotificationEntity
import com.example.finance.model.NotificationType
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/** Creates local bill reminders from the same financial data used by the UI. */
class BillReminderWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val db = AppDatabase.getDatabase(applicationContext)
        val bills = db.financeDao().getAllBills().first()
        val now = System.currentTimeMillis()
        val horizon = now + TimeUnit.DAYS.toMillis(3)
        bills.filter { !it.isPaid && it.dueDate in now..horizon }.forEach { bill ->
            db.financeDao().insertNotification(
                NotificationEntity(
                    title = "فاتورة مستحقة قريباً",
                    message = "${bill.name} — ${bill.amount} ${bill.currency}",
                    type = NotificationType.BILL_DUE,
                    timestamp = now
                )
            )
        }
        return Result.success()
    }
}
