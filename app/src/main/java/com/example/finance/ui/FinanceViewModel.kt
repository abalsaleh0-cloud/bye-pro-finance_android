package com.example.finance.ui

import android.app.ActivityManager
import android.app.Application
import android.net.Uri
import android.os.StatFs
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.finance.ai.ExtractedReceiptData
import com.example.finance.ai.FinancialAssistantEngine
import com.example.finance.ai.OfflineFinanceService
import com.example.finance.ai.LiteRtLocalAssistant
import com.example.finance.ai.GgufLocalAssistant
import com.example.finance.ai.GgufMetadataInspector
import com.example.finance.ai.AiAppToolSet
import com.example.finance.ai.AiProjectWorkspace
import com.example.finance.auth.SupabaseAuthRepository
import com.example.finance.settings.SupabaseUserSettingsRepository
import com.example.finance.network.AiRequest
import com.example.finance.network.FinanceApiProvider
import com.example.finance.data.AppDatabase
import com.example.finance.data.FinanceExportService
import com.example.finance.data.FinanceRepository
import com.example.finance.i18n.WebI18n
import com.example.finance.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.io.FileOutputStream

data class AccountActionFeedback(val operation: String, val succeeded: Boolean)
data class ChatMessage(val sender: String, val text: String, val timestamp: Long = System.currentTimeMillis(), val attachments: List<String> = emptyList())

class FinanceViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    val repository = FinanceRepository(db.financeDao(), db)
    val aiService = OfflineFinanceService()
    private val aiModelPreferences = application.getSharedPreferences("bye_pro_finance_ai_models", Application.MODE_PRIVATE)
    private val authRepository = SupabaseAuthRepository(application)
    private val userSettingsRepository = SupabaseUserSettingsRepository(authRepository)
    private val settingsSyncMutex = Mutex()
    private val aiProjectWorkspace = AiProjectWorkspace(File(application.filesDir, "ai_projects.properties"))
    private val aiAppTools = AiAppToolSet(repository, aiProjectWorkspace) { section -> setScreen(section) }
    val pendingAiAction = aiAppTools.pendingAction
    private val localLlm = LiteRtLocalAssistant(application.cacheDir, aiAppTools)
    private val ggufLlm = GgufLocalAssistant()
    val accounts: StateFlow<List<AccountEntity>> = repository.accounts.withStartupFallback(emptyList()).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val archivedAccounts: StateFlow<List<AccountEntity>> = repository.archivedAccounts.withStartupFallback(emptyList()).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _accountsLoading = MutableStateFlow(true)
    val accountsLoading: StateFlow<Boolean> = _accountsLoading.asStateFlow()
    private val _accountActionFeedback = MutableStateFlow<AccountActionFeedback?>(null)
    val accountActionFeedback: StateFlow<AccountActionFeedback?> = _accountActionFeedback.asStateFlow()
    val transactions: StateFlow<List<TransactionEntity>> = repository.transactions.withStartupFallback(emptyList()).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val transfers: StateFlow<List<TransferEntity>> = repository.transfers.withStartupFallback(emptyList()).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val budgets: StateFlow<List<BudgetEntity>> = repository.budgets.withStartupFallback(emptyList()).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val debts: StateFlow<List<DebtEntity>> = repository.debts.withStartupFallback(emptyList()).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val bills: StateFlow<List<BillEntity>> = repository.bills.withStartupFallback(emptyList()).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val savingsGoals: StateFlow<List<SavingsGoalEntity>> = repository.savingsGoals.withStartupFallback(emptyList()).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val assets: StateFlow<List<AssetEntity>> = repository.assets.withStartupFallback(emptyList()).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val householdMembers: StateFlow<List<HouseholdMemberEntity>> = repository.householdMembers.withStartupFallback(emptyList()).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val householdExpenses: StateFlow<List<HouseholdExpenseEntity>> = repository.householdExpenses.withStartupFallback(emptyList()).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val documents: StateFlow<List<DocumentEntity>> = repository.documents.withStartupFallback(emptyList()).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val notifications: StateFlow<List<NotificationEntity>> = repository.notifications.withStartupFallback(emptyList()).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val auditLogs: StateFlow<List<AuditLogEntity>> = repository.auditLogs.withStartupFallback(emptyList()).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val currentUser: StateFlow<UserEntity?> = repository.currentUser.withStartupFallback(null).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    private val _appLanguage = MutableStateFlow(WebI18n.read(application))
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()
    private val preferences = application.getSharedPreferences("bye_pro_finance_preferences", Application.MODE_PRIVATE)
    private val _isDarkMode = MutableStateFlow(WebI18n.readDarkMode(application))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()
    private val _primaryCurrency = MutableStateFlow(preferences.getString("currency_code", "LYD") ?: "LYD")
    val primaryCurrency: StateFlow<String> = _primaryCurrency.asStateFlow()
    private val _timeZone = MutableStateFlow(preferences.getString("time_zone", "Africa/Tripoli") ?: "Africa/Tripoli")
    val timeZone: StateFlow<String> = _timeZone.asStateFlow()
    private val _dateFormat = MutableStateFlow(preferences.getString("date_format", "YYYY-MM-DD") ?: "YYYY-MM-DD")
    val dateFormat: StateFlow<String> = _dateFormat.asStateFlow()
    private val _numberFormat = MutableStateFlow(preferences.getString("number_format", "1,234.56") ?: "1,234.56")
    val numberFormat: StateFlow<String> = _numberFormat.asStateFlow()
    private val _emailNotifications = MutableStateFlow(preferences.getBoolean("email_notifications", true))
    val emailNotifications: StateFlow<Boolean> = _emailNotifications.asStateFlow()
    private val _transactionAlerts = MutableStateFlow(preferences.getBoolean("transaction_alerts", true))
    val transactionAlerts: StateFlow<Boolean> = _transactionAlerts.asStateFlow()
    private val _billReminders = MutableStateFlow(preferences.getBoolean("bill_reminders", true))
    val billReminders: StateFlow<Boolean> = _billReminders.asStateFlow()
    private val _budgetAlerts = MutableStateFlow(preferences.getBoolean("budget_alerts", true))
    val budgetAlerts: StateFlow<Boolean> = _budgetAlerts.asStateFlow()
    fun setPrimaryCurrency(currency: String) { val normalized = currency.trim().uppercase(); if (normalized !in setOf("LYD", "USD", "EUR", "GBP", "SAR", "AED", "EGP")) return; _primaryCurrency.value = normalized; preferences.edit().putString("currency_code", normalized).apply(); scheduleUserSettingsSync() }
    fun setTimeZone(value: String) { if (value !in setOf("Africa/Tripoli", "Asia/Damascus", "Asia/Amman", "Europe/Istanbul", "Europe/London", "UTC")) return; _timeZone.value = value; preferences.edit().putString("time_zone", value).apply(); scheduleUserSettingsSync() }
    fun setDateFormat(value: String) { if (value !in setOf("YYYY-MM-DD", "DD/MM/YYYY", "MM/DD/YYYY")) return; _dateFormat.value = value; preferences.edit().putString("date_format", value).apply(); scheduleUserSettingsSync() }
    fun setNumberFormat(value: String) { if (value !in setOf("1,234.56", "1.234,56", "1 234,56")) return; _numberFormat.value = value; preferences.edit().putString("number_format", value).apply(); scheduleUserSettingsSync() }
    fun setEmailNotifications(value: Boolean) { _emailNotifications.value = value; preferences.edit().putBoolean("email_notifications", value).apply(); scheduleUserSettingsSync() }
    fun setTransactionAlerts(value: Boolean) { _transactionAlerts.value = value; preferences.edit().putBoolean("transaction_alerts", value).apply(); scheduleUserSettingsSync() }
    fun setBillReminders(value: Boolean) { _billReminders.value = value; preferences.edit().putBoolean("bill_reminders", value).apply(); scheduleUserSettingsSync() }
    fun setBudgetAlerts(value: Boolean) { _budgetAlerts.value = value; preferences.edit().putBoolean("budget_alerts", value).apply(); scheduleUserSettingsSync() }
    fun updateLocalProfile(user: UserEntity) { val normalized = user.name.trim(); if (normalized.length < 2 || normalized.length > 100) return; viewModelScope.launch { runCatching { repository.updateCurrentUser(user.copy(name = normalized)) }.onFailure { Log.e("BYE_PRO_FINANCE", "Update local profile failed", it) } } }
    private val _currentScreen = MutableStateFlow("dashboard")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()
    private val _scannedReceipt = MutableStateFlow<ExtractedReceiptData?>(null)
    val scannedReceipt: StateFlow<ExtractedReceiptData?> = _scannedReceipt.asStateFlow()
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()
    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()
    private val _localModelReady = MutableStateFlow(false)
    val localModelReady: StateFlow<Boolean> = _localModelReady.asStateFlow()
    private val _localModelLoading = MutableStateFlow(false)
    val localModelLoading: StateFlow<Boolean> = _localModelLoading.asStateFlow()
    private val _localModelProgress = MutableStateFlow(0f)
    val localModelProgress: StateFlow<Float> = _localModelProgress.asStateFlow()
    private val _localModelStatus = MutableStateFlow("لم يتم تحميل نموذج محلي بعد.")
    val localModelStatus: StateFlow<String> = _localModelStatus.asStateFlow()
    init { resetWelcomeMessage(); viewModelScope.launch { restoreUserSettingsFromCloud(); resetWelcomeMessage(); runCatching { repository.accounts.first(); repository.archivedAccounts.first() }; _accountsLoading.value = false } }
    private fun currentUserSettingsPayload(): JSONObject = JSONObject()
        .put("theme", if (_isDarkMode.value) "dark" else "light")
        .put("locale", if (_appLanguage.value == AppLanguage.EN) "en" else "ar")
        .put("timezone", _timeZone.value)
        .put("currency_code", _primaryCurrency.value)
        .put("date_format", _dateFormat.value)
        .put("number_format", _numberFormat.value)
        .put("email_notifications", _emailNotifications.value)
        .put("transaction_alerts", _transactionAlerts.value)
        .put("bill_reminders", _billReminders.value)
        .put("budget_alerts", _budgetAlerts.value)

    private fun scheduleUserSettingsSync() {
        viewModelScope.launch {
            settingsSyncMutex.withLock {
                userSettingsRepository.save(currentUserSettingsPayload())
                    .onFailure { Log.w("BYE_PRO_FINANCE", "Cloud settings sync deferred; local preferences remain saved", it) }
            }
        }
    }

    private suspend fun restoreUserSettingsFromCloud() {
        userSettingsRepository.load().onSuccess { row ->
            if (row == null) return@onSuccess
            val language = if (row.optString("locale") == "en") AppLanguage.EN else AppLanguage.AR
            val dark = row.optString("theme", "dark") != "light"
            val currency = row.optString("currency_code", "LYD")
            val timezone = row.optString("timezone", "Africa/Tripoli")
            val date = row.optString("date_format", "YYYY-MM-DD")
            val number = row.optString("number_format", "1,234.56")
            _appLanguage.value = language
            _isDarkMode.value = dark
            _primaryCurrency.value = currency
            _timeZone.value = timezone
            _dateFormat.value = date
            _numberFormat.value = number
            _emailNotifications.value = row.optBoolean("email_notifications", true)
            _transactionAlerts.value = row.optBoolean("transaction_alerts", true)
            _billReminders.value = row.optBoolean("bill_reminders", true)
            _budgetAlerts.value = row.optBoolean("budget_alerts", true)
            preferences.edit()
                .putString("currency_code", currency)
                .putString("time_zone", timezone)
                .putString("date_format", date)
                .putString("number_format", number)
                .putBoolean("email_notifications", _emailNotifications.value)
                .putBoolean("transaction_alerts", _transactionAlerts.value)
                .putBoolean("bill_reminders", _billReminders.value)
                .putBoolean("budget_alerts", _budgetAlerts.value)
                .apply()
            WebI18n.write(getApplication(), language)
            WebI18n.writeDarkMode(getApplication(), dark)
        }.onFailure {
            Log.w("BYE_PRO_FINANCE", "Could not restore cloud settings; keeping local preferences", it)
        }
    }

    private fun resetWelcomeMessage() { _chatMessages.value = listOf(ChatMessage("AI", WebI18n.tr("مرحباً بك! أنا AI Bye Pro، مساعدك الذكي. تحدث معي بشكل طبيعي عن أي موضوع؛ أستطيع مساعدتك في المعرفة والكتابة والترجمة والتخطيط والتحليل، وكذلك شؤونك المالية عند طلبك.", "Welcome! I am AI Bye Pro, your general-purpose AI assistant. Talk to me naturally about any topic: knowledge, writing, translation, planning, analysis, and your finances whenever you ask.", _appLanguage.value))) }
    fun setScreen(screen: String) { _currentScreen.value = screen }
    fun toggleLanguage() { setLanguage(if (_appLanguage.value == AppLanguage.AR) AppLanguage.EN else AppLanguage.AR) }
    fun setLanguage(lang: AppLanguage) { if (_appLanguage.value == lang) return; _appLanguage.value = lang; WebI18n.write(getApplication(), lang); if (_chatMessages.value.size <= 1) resetWelcomeMessage(); scheduleUserSettingsSync() }
    fun setDarkMode(dark: Boolean) { _isDarkMode.value = dark; WebI18n.writeDarkMode(getApplication(), dark); scheduleUserSettingsSync() }
    fun toggleTheme() { setDarkMode(!_isDarkMode.value) }
    fun addAccount(account: AccountEntity) { viewModelScope.launch { _accountActionFeedback.value = AccountActionFeedback("add", runCatching { repository.addAccount(account) }.isSuccess) } }
    fun clearAccountActionFeedback() { _accountActionFeedback.value = null }
    fun updateAccount(account: AccountEntity) { viewModelScope.launch { repository.updateAccount(account) } }
    fun deleteAccount(account: AccountEntity) { viewModelScope.launch { repository.deleteAccount(account) } }
    fun archiveAccount(accountId: Long, isArchived: Boolean) { viewModelScope.launch { _accountActionFeedback.value = AccountActionFeedback(if (isArchived) "deactivate" else "activate", runCatching { repository.archiveAccount(accountId, isArchived) }.isSuccess) } }
    fun addTransaction(tx: TransactionEntity) { viewModelScope.launch { repository.addTransaction(tx) } }
    fun updateTransactionStatus(tx: TransactionEntity, status: TransactionStatus) { viewModelScope.launch { repository.updateTransactionStatus(tx, status) } }
    fun deleteTransaction(tx: TransactionEntity) { viewModelScope.launch { repository.deleteTransaction(tx) } }
    fun executeTransfer(fromAccountId: Long, toAccountId: Long, amount: Double, fee: Double, currency: String, description: String, reference: String, onComplete: (Boolean) -> Unit) { viewModelScope.launch { onComplete(repository.executeTransfer(fromAccountId, toAccountId, amount, fee, currency, description, reference)) } }
    fun addBudget(budget: BudgetEntity) { viewModelScope.launch { repository.addBudget(budget) } }
    fun updateBudget(budget: BudgetEntity) { viewModelScope.launch { repository.updateBudget(budget) } }
    fun deleteBudget(budget: BudgetEntity) { viewModelScope.launch { repository.deleteBudget(budget) } }
    fun addDebt(debt: DebtEntity) { viewModelScope.launch { repository.addDebt(debt) } }
    fun updateDebt(debt: DebtEntity) { viewModelScope.launch { repository.updateDebt(debt) } }
    fun debtPayments(debtId: Long): Flow<List<DebtPaymentEntity>> = repository.debtPayments(debtId)
    fun payDebt(debtId: Long, amount: Double, notes: String, paymentDate: Long = System.currentTimeMillis()) { viewModelScope.launch { repository.recordDebtPayment(debtId, amount, notes, paymentDate) } }
    fun deleteDebt(debt: DebtEntity) { viewModelScope.launch { repository.deleteDebt(debt) } }
    fun addBill(bill: BillEntity) { viewModelScope.launch { repository.addBill(bill) } }
    fun updateBill(bill: BillEntity) { viewModelScope.launch { repository.updateBill(bill) } }
    fun markBillPaid(billId: Long, accountId: Long) { viewModelScope.launch { repository.markBillPaid(billId, accountId) } }
    fun deleteBill(bill: BillEntity) { viewModelScope.launch { repository.deleteBill(bill) } }
    fun addSavingsGoal(goal: SavingsGoalEntity) { viewModelScope.launch { repository.addSavingsGoal(goal) } }
    fun updateSavingsGoal(goal: SavingsGoalEntity) { viewModelScope.launch { repository.updateSavingsGoal(goal) } }
    fun contributeToSavings(goalId: Long, amount: Double, accountId: Long) { viewModelScope.launch { repository.contributeToSavings(goalId, amount, accountId) } }
    fun deleteSavingsGoal(goal: SavingsGoalEntity) { viewModelScope.launch { repository.deleteSavingsGoal(goal) } }
    fun addAsset(asset: AssetEntity) { viewModelScope.launch { repository.addAsset(asset) } }
    fun updateAsset(asset: AssetEntity) { viewModelScope.launch { repository.updateAsset(asset) } }
    fun deleteAsset(asset: AssetEntity) { viewModelScope.launch { repository.deleteAsset(asset) } }
    fun addHouseholdMember(name: String, email: String, role: String, percent: Double) { viewModelScope.launch { runCatching { repository.addHouseholdMember(name, email, role, percent) }.onFailure { Log.e("BYE_PRO_FINANCE", "Add household member failed", it) } } }
    fun updateHouseholdMember(member: HouseholdMemberEntity) { viewModelScope.launch { runCatching { repository.updateHouseholdMember(member) }.onFailure { Log.e("BYE_PRO_FINANCE", "Update household member failed", it) } } }
    fun deleteHouseholdMember(member: HouseholdMemberEntity) { viewModelScope.launch { runCatching { repository.deleteHouseholdMember(member) }.onFailure { Log.e("BYE_PRO_FINANCE", "Delete household member failed", it) } } }
    fun addHouseholdExpense(title: String, amount: Double, payerId: Long, category: String, currency: String = primaryCurrency.value) { viewModelScope.launch { runCatching { repository.addHouseholdExpense(title, amount, payerId, category, currency) }.onFailure { Log.e("BYE_PRO_FINANCE", "Add household expense failed", it) } } }
    fun updateHouseholdExpense(expense: HouseholdExpenseEntity) { viewModelScope.launch { runCatching { repository.updateHouseholdExpense(expense) }.onFailure { Log.e("BYE_PRO_FINANCE", "Update household expense failed", it) } } }
    fun deleteHouseholdExpense(expense: HouseholdExpenseEntity) { viewModelScope.launch { runCatching { repository.deleteHouseholdExpense(expense) }.onFailure { Log.e("BYE_PRO_FINANCE", "Delete household expense failed", it) } } }
    fun addDocument(doc: DocumentEntity) { viewModelScope.launch { runCatching { repository.addDocument(doc) }.onFailure { Log.e("BYE_PRO_FINANCE", "Save document failed", it) } } }
    fun deleteDocument(doc: DocumentEntity) { viewModelScope.launch { runCatching { repository.deleteDocument(doc) }.onFailure { Log.e("BYE_PRO_FINANCE", "Delete document failed", it) } } }
    fun markNotificationRead(id: Long) { viewModelScope.launch { runCatching { repository.markNotificationRead(id) }.onFailure { Log.e("BYE_PRO_FINANCE", "Mark notification read failed", it) } } }
    fun markAllNotificationsRead() { viewModelScope.launch { runCatching { repository.markAllNotificationsRead() }.onFailure { Log.e("BYE_PRO_FINANCE", "Mark all notifications read failed", it) } } }
    fun deleteNotification(id: Long) { viewModelScope.launch { runCatching { repository.deleteNotification(id) }.onFailure { Log.e("BYE_PRO_FINANCE", "Delete notification failed", it) } } }
    fun clearNotifications() { viewModelScope.launch { runCatching { repository.clearNotifications() }.onFailure { Log.e("BYE_PRO_FINANCE", "Clear notifications failed", it) } } }
    fun clearAiChat() { if (_isAiLoading.value) return; resetWelcomeMessage() }
    fun loadLocalModelFile(file: File) {
        if (_localModelLoading.value) return
        viewModelScope.launch {
            _localModelLoading.value = true
            _localModelProgress.value = 0f
            _localModelStatus.value = "جارٍ تهيئة محرك النموذج المحلي..."
            try {
                loadLocalModelIntoRuntime(file)
            } catch (error: Throwable) {
                _localModelReady.value = false
                _localModelStatus.value = "تعذر تحميل النموذج: " + (error.message ?: "خطأ غير معروف")
                Log.e("BYE_PRO_LOCAL_AI", "Local model load failed", error)
            } finally {
                _localModelLoading.value = false
            }
        }
    }

    fun importAndLoadLocalModel(uri: Uri) {
        if (_localModelLoading.value) return
        viewModelScope.launch {
            _localModelLoading.value = true
            _localModelProgress.value = 0f
            _localModelStatus.value = "جارٍ نسخ ملف النموذج إلى مساحة التطبيق الخاصة..."
            try {
                val modelFile = copyLocalModelIntoApp(uri)
                loadLocalModelIntoRuntime(modelFile)
            } catch (error: Throwable) {
                _localModelReady.value = false
                _localModelStatus.value = "تعذر تجهيز أو تشغيل النموذج: " + (error.message ?: "خطأ غير معروف")
                Log.e("BYE_PRO_LOCAL_AI", "Local model import/load failed", error)
            } finally {
                _localModelLoading.value = false
            }
        }
    }

    private suspend fun loadLocalModelIntoRuntime(modelFile: File) {
        require(modelFile.isFile && modelFile.canRead() && modelFile.length() > 0L) {
            "ملف النموذج غير موجود أو فارغ أو غير قابل للقراءة."
        }
        when (modelFile.extension.lowercase()) {
            "gguf" -> {
                val metadata = withContext(Dispatchers.IO) {
                    modelFile.inputStream().use(GgufMetadataInspector::inspect)
                }
                require(!metadata.architecture.isNullOrBlank()) {
                    "تعذر تحديد بنية GGUF؛ الملف لا يحتوي على بيانات معمارية صالحة."
                }
                // Release any previous local runtime before estimating memory for GGUF.
                localLlm.unload()
                ggufLlm.unload()
                val manager = getApplication<Application>().getSystemService(ActivityManager::class.java)
                val memory = ActivityManager.MemoryInfo()
                manager.getMemoryInfo(memory)
                val estimatedBytes = (modelFile.length() * 1.20).toLong() + 512L * 1024L * 1024L
                require(memory.availMem >= estimatedBytes) {
                    "الذاكرة المتاحة غير كافية لتشغيل هذا النموذج بأمان. يلزم تقريباً " +
                        String.format(java.util.Locale.US, "%.2f GB", estimatedBytes / 1_073_741_824.0) +
                        " متاحة حالياً؛ أغلق التطبيقات الأخرى أو استخدم نسخة GGUF أصغر."
                }
                _localModelStatus.value = "جارٍ تحميل GGUF عبر llama.cpp على المعالج..."
                ggufLlm.load(modelFile)
                aiModelPreferences.edit()
                    .putString("active_model_path", modelFile.absolutePath)
                    .putString("active_model_format", "GGUF")
                    .putString("active_model_architecture", metadata.architecture)
                    .putBoolean("active_model_supports_vision", false)
                    .apply()
                _localModelReady.value = true
                _localModelStatus.value = "محرك llama.cpp جاهز. نموذج GGUF يعمل محلياً على الهاتف."
            }
            "litertlm" -> {
                ggufLlm.unload()
                _localModelStatus.value = "جارٍ تحميل حزمة LiteRT-LM على المعالج..."
                localLlm.load(modelFile)
                aiModelPreferences.edit()
                    .putString("active_model_path", modelFile.absolutePath)
                    .putString("active_model_format", "LITERTLM")
                    .remove("active_model_architecture")
                    .putBoolean("active_model_supports_vision", modelFile.name.contains("gemma-3n", ignoreCase = true))
                    .apply()
                _localModelReady.value = true
                _localModelStatus.value = "محرك LiteRT-LM جاهز. النموذج يعمل محلياً على الهاتف."
            }
            else -> error("صيغة النموذج غير مدعومة. اختر ملف .gguf أو .litertlm.")
        }
    }

    private suspend fun copyLocalModelIntoApp(uri: Uri): File = withContext(Dispatchers.IO) {
        var displayName = "selected-model.gguf"
        var expectedSize = -1L
        getApplication<Application>().contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex >= 0) displayName = cursor.getString(nameIndex) ?: displayName
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) expectedSize = cursor.getLong(sizeIndex)
            }
        }
        require(
            displayName.endsWith(".litertlm", ignoreCase = true) ||
                displayName.endsWith(".gguf", ignoreCase = true)
        ) {
            "صيغة الملف غير مدعومة. اختر ملف GGUF أو LiteRT-LM."
        }

        val directory = File(getApplication<Application>().filesDir, "ai-models")
        check(directory.exists() || directory.mkdirs()) { "تعذر إنشاء مجلد النموذج." }
        val requiredBytes = if (expectedSize > 0) expectedSize + 128L * 1024L * 1024L else 128L * 1024L * 1024L
        check(StatFs(directory.absolutePath).availableBytes >= requiredBytes) {
            "المساحة الحرة غير كافية لنسخ النموذج؛ يلزم توفير مساحة إضافية على الهاتف."
        }

        val safeModelName = displayName.replace(Regex("[^A-Za-z0-9._-]"), "_")
            .takeIf {
                it.endsWith(".litertlm", ignoreCase = true) ||
                    it.endsWith(".gguf", ignoreCase = true)
            } ?: "selected-model.gguf"
        val destination = File(directory, safeModelName)
        val temporary = File(directory, "$safeModelName.part")
        var copied = 0L
        try {
            val input = getApplication<Application>().contentResolver.openInputStream(uri)
                ?: error("تعذر فتح ملف النموذج المحدد.")
            input.use { source ->
                FileOutputStream(temporary).buffered(1024 * 1024).use { output ->
                    val buffer = ByteArray(1024 * 1024)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = source.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        copied += count
                        if (expectedSize > 0) {
                            _localModelProgress.value = (copied.toFloat() / expectedSize.toFloat()).coerceIn(0f, 1f)
                        }
                    }
                    output.flush()
                }
            }
            check(copied > 0L) { "ملف النموذج فارغ." }
            if (expectedSize > 0) check(copied == expectedSize) {
                "اكتمل نسخ حجم مختلف عن حجم الملف الأصلي؛ لم يتم اعتماد النسخة."
            }

            val backup = File(directory, "selected-model.previous")
            if (backup.exists()) check(backup.delete()) { "تعذر تنظيف نسخة النموذج المؤقتة السابقة." }
            if (destination.exists()) check(destination.renameTo(backup)) {
                "تعذر تجهيز ملف النموذج الحالي للاستبدال."
            }
            if (!temporary.renameTo(destination)) {
                if (backup.exists()) backup.renameTo(destination)
                error("تعذر تثبيت ملف النموذج بعد النسخ.")
            }
            backup.delete()
            _localModelProgress.value = 1f
            destination
        } catch (error: Throwable) {
            temporary.delete()
            throw error
        }
    }

    private suspend fun prepareLocalImageFiles(attachments: List<String>): List<File> =
        withContext(Dispatchers.IO) {
            require(attachments.isNotEmpty() && attachments.size <= 4) {
                "يُسمح بإرفاق صورة واحدة إلى أربع صور."
            }
            val resolver = getApplication<Application>().contentResolver
            val prepared = mutableListOf<File>()
            try {
                attachments.forEachIndexed { index, rawUri ->
                    val uri = Uri.parse(rawUri)
                    val mimeType = resolver.getType(uri)
                    require(mimeType?.startsWith("image/") == true) {
                        "أحد المرفقات ليس ملف صورة صالحاً."
                    }
                    val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "img"
                    val destination = File.createTempFile(
                        "bye-pro-vision-$index-",
                        ".$extension",
                        getApplication<Application>().cacheDir
                    )
                    try {
                        val input = resolver.openInputStream(uri)
                            ?: error("تعذر فتح إحدى الصور المرفقة.")
                        input.use { source ->
                            FileOutputStream(destination).use { output ->
                                val buffer = ByteArray(64 * 1024)
                                var totalBytes = 0L
                                while (true) {
                                    currentCoroutineContext().ensureActive()
                                    val count = source.read(buffer)
                                    if (count < 0) break
                                    totalBytes += count
                                    require(totalBytes <= 10L * 1024L * 1024L) {
                                        "حجم الصورة الواحدة يتجاوز الحد المسموح وهو 10 ميغابايت."
                                    }
                                    output.write(buffer, 0, count)
                                }
                            }
                        }
                        require(destination.length() > 0L) { "إحدى الصور المرفقة فارغة." }
                        prepared.add(destination)
                    } catch (error: Throwable) {
                        destination.delete()
                        throw error
                    }
                }
                prepared
            } catch (error: Throwable) {
                prepared.forEach { it.delete() }
                throw error
            }
        }

    private suspend fun askCloudGeneralAssistant(query: String): String? {
        if (!aiModelPreferences.getBoolean("cloud_general_enabled", true)) return null
        val token = authRepository.getValidAccessToken() ?: return null
        return runCatching {
            // Only the user's general question is sent to the cloud. Personal
            // financial records remain on-device unless a separate consent flow
            // is implemented in a future version.
            val envelope = FinanceApiProvider.api.ai("Bearer $token", AiRequest(query))
            val data = envelope.data ?: return@runCatching null
            listOf("answer", "response", "text", "message", "content")
                .firstNotNullOfOrNull { key ->
                    data[key]?.toString()?.trim()?.takeIf(String::isNotBlank)
                }
        }.onFailure { error ->
            Log.w("BYE_PRO_CLOUD_AI", "Cloud general assistant request failed; using local fallback.", error)
        }.getOrNull()
    }

    private fun isFinancialQuestion(query: String): Boolean {
        val q = query.lowercase(Locale.ROOT)
        return listOf(
            "مال", "حساب", "رصيد", "مصروف", "انفاق", "إنفاق", "دخل", "راتب",
            "معامل", "تحويل", "ميزاني", "دين", "دائن", "مدين", "فاتور",
            "ادخار", "توفير", "أصل", "أصول", "financial", "account", "balance",
            "spend", "expense", "income", "salary", "transaction", "transfer",
            "budget", "debt", "bill", "saving", "asset", "net worth"
        ).any(q::contains)
    }

    private fun currentDateAnswer(query: String): String? {
        // Normalize Arabic diacritics and punctuation so colloquial variants are
        // answered from the device clock without invoking the language model.
        val normalized = query.trim()
            .lowercase(Locale.ROOT)
            .replace(Regex("[\\u064B-\\u065F\\u0670]"), "")
            .replace(Regex("[؟?!،,.؛:]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        val asksForToday = listOf(
            "ماهو اليوم", "ما هو اليوم", "اي يوم اليوم", "أي يوم اليوم",
            "ما اليوم", "شو اليوم", "ايش اليوم", "اليوم اي يوم", "اليوم أي يوم",
            "اي يوم نحنا", "أي يوم نحن", "ما تاريخ اليوم", "ما هو تاريخ اليوم",
            "تاريخ اليوم", "كم التاريخ", "كم تاريخ اليوم", "شو تاريخ اليوم",
            "what day is it", "what day is today", "what is the day today",
            "what is today's date", "what's today's date", "what is the date today",
            "what date is it", "current date", "today's date"
        ).any(normalized::contains)
        if (!asksForToday) return null
        val locale = if (_appLanguage.value == AppLanguage.AR) Locale("ar") else Locale.ENGLISH
        val pattern = if (_appLanguage.value == AppLanguage.AR) "EEEE، d MMMM yyyy" else "EEEE, MMMM d, yyyy"
        val today = SimpleDateFormat(pattern, locale).format(Date())
        return if (_appLanguage.value == AppLanguage.AR) "اليوم هو $today." else "Today is $today."
    }

    fun askAiAssistant(query: String, attachments: List<String> = emptyList()) {
        if ((query.isBlank() && attachments.isEmpty()) || _isAiLoading.value) return
        val trimmedQuery = query.trim()
        _chatMessages.value = _chatMessages.value + ChatMessage("USER", trimmedQuery, attachments = attachments)
        _isAiLoading.value = true
        viewModelScope.launch {
            val currentDateResponse = if (attachments.isEmpty()) currentDateAnswer(trimmedQuery) else null
            if (currentDateResponse != null) {
                _chatMessages.value = _chatMessages.value + ChatMessage("AI", currentDateResponse)
                _isAiLoading.value = false
                return@launch
            }
            val response = if (attachments.isNotEmpty()) {
                runCatching {
                    require(attachments.size <= 4) { "يمكن تحليل أربع صور كحد أقصى في الرسالة الواحدة." }
                    require(
                        aiModelPreferences.getString("active_model_format", "LITERTLM") != "GGUF" &&
                            aiModelPreferences.getBoolean("active_model_supports_vision", false)
                    ) {
                        "تحليل الصور يحتاج نموذج Gemma 3n متوافقاً بصيغة .litertlm؛ محرك GGUF الحالي مخصص للمحادثة النصية."
                    }
                    val savedPath = aiModelPreferences.getString("active_model_path", null)
                        ?: error("لم يتم تثبيت نموذج محلي يدعم الصور بعد.")
                    val savedModel = File(savedPath)
                    require(savedModel.isFile) { "ملف النموذج المحلي غير موجود؛ أعد اختياره من إعدادات النماذج." }
                    if (!_localModelReady.value) {
                        _localModelLoading.value = true
                        try {
                            localLlm.load(savedModel)
                            _localModelReady.value = true
                        } finally {
                            _localModelLoading.value = false
                        }
                    }
                    val imageFiles = prepareLocalImageFiles(attachments)
                    try {
                        val imagePrompt = trimmedQuery.ifBlank {
                            WebI18n.tr("حلّل الصور المرفقة واشرح ما يظهر فيها بدقة.", "Analyze the attached images and explain what is visible.", _appLanguage.value)
                        }
                        localLlm.sendMessageWithImages(imagePrompt, imageFiles)
                    } finally {
                        imageFiles.forEach { it.delete() }
                    }
                }.getOrElse { error ->
                    Log.e("BYE_PRO_LOCAL_VISION", "On-device image analysis failed.", error)
                    WebI18n.tr(
                        "تعذر تحليل الصور محلياً: " + (error.message ?: "خطأ غير معروف"),
                        "On-device image analysis failed: " + (error.message ?: "unknown error"),
                        _appLanguage.value
                    )
                }
            } else runCatching {
                val financialContext = if (isFinancialQuestion(trimmedQuery)) {
                    FinancialAssistantEngine.answer(
                        query = trimmedQuery,
                        accounts = accounts.value,
                        transactions = transactions.value,
                        budgets = budgets.value,
                        debts = debts.value,
                        savingsGoals = savingsGoals.value,
                        assets = assets.value,
                        bills = bills.value,
                        language = _appLanguage.value
                    )
                } else ""
                val savedPath = aiModelPreferences.getString("active_model_path", null)
                val savedModel = savedPath?.let(::File)
                if (savedModel != null && savedModel.isFile) {
                    try {
                        val format = aiModelPreferences.getString(
                            "active_model_format",
                            if (savedModel.extension.equals("gguf", ignoreCase = true)) "GGUF" else "LITERTLM"
                        )
                        if (!_localModelReady.value) {
                            _localModelLoading.value = true
                            _localModelStatus.value = "جارٍ استعادة النموذج المحلي..."
                            try {
                                if (format == "GGUF") {
                                    ggufLlm.load(savedModel)
                                } else {
                                    localLlm.load(savedModel)
                                }
                                _localModelReady.value = true
                                _localModelStatus.value = "النموذج المحلي جاهز."
                            } finally {
                                _localModelLoading.value = false
                            }
                        }
                        if (format == "GGUF") {
                            ggufLlm.sendMessage(trimmedQuery, financialContext)
                        } else {
                            localLlm.sendMessage(trimmedQuery, financialContext)
                        }
                    } catch (localError: Throwable) {
                        Log.e("BYE_PRO_LOCAL_AI", "Local assistant failed; trying cloud general assistant.", localError)
                        askCloudGeneralAssistant(trimmedQuery) ?: financialContext
                    }
                } else {
                    askCloudGeneralAssistant(trimmedQuery) ?: financialContext
                }
            }.getOrElse { error ->
                Log.e("BYE_PRO_FINANCE", "Assistant request failed", error)
                WebI18n.tr(
                    "تعذر تشغيل المساعد حالياً: " + (error.message ?: "خطأ غير معروف"),
                    "The assistant could not respond right now: " + (error.message ?: "unknown error"),
                    _appLanguage.value
                )
            }
            _isAiLoading.value = false
            _chatMessages.value = _chatMessages.value + ChatMessage("AI", response)
        }
    }

    fun confirmPendingAiAction(actionId: String) {
        viewModelScope.launch {
            runCatching { aiAppTools.confirmPendingAction(actionId) }
                .onSuccess { message -> _chatMessages.value = _chatMessages.value + ChatMessage("AI", message) }
                .onFailure { error -> _chatMessages.value = _chatMessages.value + ChatMessage("AI", "لم يتم تنفيذ العملية: ${error.message ?: "خطأ غير معروف"}") }
        }
    }

    fun cancelPendingAiAction(actionId: String) { aiAppTools.cancelPendingAction(actionId) }

    override fun onCleared() {
        localLlm.shutdown()
        ggufLlm.shutdown()
        super.onCleared()
    }
    private val _ocrError = MutableStateFlow<String?>(null)
    val ocrError: StateFlow<String?> = _ocrError.asStateFlow()
    fun scanReceipt(sampleText: String? = null, imageBase64: String? = null) { viewModelScope.launch { _ocrError.value = null; aiService.parseReceiptImage(sampleText, imageBase64, _appLanguage.value).onSuccess { _scannedReceipt.value = it; _ocrError.value = null }.onFailure { _scannedReceipt.value = null; _ocrError.value = it.message ?: WebI18n.tr("تعذر استخراج بيانات الفاتورة آلياً", "Could not extract receipt data automatically", _appLanguage.value) } } }
    fun clearScannedReceipt() { _scannedReceipt.value = null; _ocrError.value = null }
    fun importTransactionsFromJson(json: String, onComplete: (Result<Int>) -> Unit) { viewModelScope.launch { onComplete(runCatching { repository.importTransactions(FinanceExportService.transactionsFromJson(json)) }) } }
    fun importTransactionsFromCsv(csv: String, onComplete: (Result<Int>) -> Unit) { viewModelScope.launch { onComplete(runCatching { repository.importTransactions(FinanceExportService.transactionsFromCsv(csv)) }) } }
    fun generateCsvExport(): String = FinanceExportService.transactionsToCsv(transactions.value)
    fun generateJsonExport(): String = FinanceExportService.transactionsToJson(transactions.value)
    fun restoreBackupFromJson(backupJson: String, onComplete: (Boolean) -> Unit) { viewModelScope.launch { onComplete(repository.restoreFromJson(backupJson)) } }
}
private fun <T> Flow<T>.withStartupFallback(fallback: T): Flow<T> = catch { error -> Log.e("BYE_PRO_FINANCE", "Local database stream failed during startup", error); emit(fallback) }
