package com.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.finance.auth.SupabaseAuthRepository
import com.example.finance.supabase.SupabaseConnection
import com.example.finance.ui.FinanceViewModel
import com.example.finance.worker.BillReminderWorker
import com.example.ui.navigation.MainAppScaffold
import com.example.ui.screens.AuthScreen
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
            val language by financeViewModel.appLanguage.collectAsState()
            var authReady by remember { mutableStateOf(false) }
            var isAuthenticated by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                isAuthenticated = SupabaseAuthRepository(applicationContext).restoreSession()
                authReady = true
            }

            MyApplicationTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when {
                        !authReady -> Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
                        isAuthenticated -> MainAppScaffold(viewModel = financeViewModel)
                        else -> AuthScreen(
                            language = language,
                            onToggleLanguage = { financeViewModel.toggleLanguage() },
                            onAuthenticated = { isAuthenticated = true }
                        )
                    }
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
