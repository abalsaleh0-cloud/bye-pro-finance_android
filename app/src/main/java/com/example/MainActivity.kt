package com.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.finance.supabase.SupabaseConnection
import com.example.finance.ui.FinanceViewModel
import com.example.finance.worker.BillReminderWorker
import com.example.ui.navigation.MainAppScaffold
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private val financeViewModel: FinanceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        scheduleFinanceBackgroundWork()
        verifySupabaseConnection()
        setContent {
            val isDark by financeViewModel.isDarkMode.collectAsState()
            MyApplicationTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAppScaffold(viewModel = financeViewModel)
                }
            }
        }
    }

    private fun verifySupabaseConnection() {
        lifecycleScope.launch {
            SupabaseConnection.checkConnection()
                .onSuccess {
                    Log.i("BYE_PRO_SUPABASE", "Supabase connection established")
                }
                .onFailure { error ->
                    Log.e("BYE_PRO_SUPABASE", "Supabase connection unavailable", error)
                }
        }
    }

    private fun scheduleFinanceBackgroundWork() {
        runCatching {
            val request = PeriodicWorkRequestBuilder<BillReminderWorker>(1, TimeUnit.DAYS).build()
            WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
                "bye-pro-finance-bill-reminders",
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}
