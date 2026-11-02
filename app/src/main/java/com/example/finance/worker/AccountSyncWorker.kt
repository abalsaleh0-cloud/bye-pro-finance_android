package com.example.finance.worker

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.finance.sync.AccountSyncCoordinator
import java.util.concurrent.TimeUnit

/** Durable retry worker for the account sync vertical slice. */
class AccountSyncWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = try {
        AccountSyncCoordinator(applicationContext).syncOnce()
        Result.success()
    } catch (error: Exception) {
        Log.e(TAG, "Account sync will retry; local data and auth session are preserved", error)
        Result.retry()
    }

    companion object {
        private const val TAG = "AccountSyncWorker"
        private const val IMMEDIATE_WORK = "bye-pro-finance-account-sync-immediate"
        private const val PERIODIC_WORK = "bye-pro-finance-account-sync-periodic"

        fun enqueueNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<AccountSyncWorker>()
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                IMMEDIATE_WORK,
                ExistingWorkPolicy.KEEP,
                request
            )
        }

        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<AccountSyncWorker>(15, TimeUnit.MINUTES)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                PERIODIC_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
