package com.example.finance.ui

import android.app.Application
import android.util.Log
import android.net.Uri
import java.io.File
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.finance.ai.ExtractedReceiptData
import com.example.finance.ai.FinanceVoiceCommandParser
import com.example.finance.ai.FinancialAssistantEngine
import com.example.finance.ai.GemmaLocalAssistant
import com.example.finance.ai.OfflineFinanceService
import com.example.finance.auth.SupabaseAuthRepository
import com.example.finance.network.FinanceApiProvider
import com.example.finance.network.SpeechRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import com.example.finance.data.AppDatabase
import com.example.finance.data.FinanceExportService
import com.example.finance.data.FinanceRepository
import com.example.finance.i18n.WebI18n
import com.example.finance.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AccountActionFeedback(val operation: String, val succeeded: Boolean)
data class ChatMessage(val sender: String, val text: String, val timestamp: Long = System.currentTimeMillis())

class FinanceViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    val repository = FinanceRepository(db.financeDao(), db)
    val aiService = OfflineFinanceService()
    private val authRepository = SupabaseAuthRepository(application)
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
    fun setPrimaryCurrency(currency: String) { val normalized = currency.trim().uppercase(); if (normalized !in setOf("LYD", "USD", "EUR", "GBP", "SAR", "AED", "EGP")) return; _primaryCurrency.value = normalized; preferences.edit().putString("currency_code", normalized).apply() }
    fun setTimeZone(value: String) { if (value !in setOf("Africa/Tripoli", "Asia/Damascus", "Asia/Amman", "Europe/Istanbul", "Europe/London", "UTC")) return; _timeZone.value = value; preferences.edit().putString("time_zone", value).apply() }
    fun setDateFormat(value: String) { if (value !in setOf("YYYY-MM-DD", "DD/MM/YYYY", "MM/DD/YYYY")) return; _dateFormat.value = value; preferences.edit().putString("date_format", value).apply() }
    fun setNumberFormat(value: String) { if (value !in setOf("1,234.56", "1.234,56", "1 234,56")) return; _numberFormat.value = value; preferences.edit().putString("number_format", value).apply() }
    fun setEmailNotifications(value: Boolean) { _emailNotifications.value = value; preferences.edit().putBoolean("email_notifications", value).apply() }
    fun setTransactionAlerts(value: Boolean) { _transactionAlerts.value = value; preferences.edit().putBoolean("transaction_alerts", value).apply() }
    fun setBillReminders(value: Boolean) { _billReminders.value = value; preferences.edit().putBoolean("bill_reminders", value).apply() }
    fun setBudgetAlerts(value: Boolean) { _budgetAlerts.value = value; preferences.edit().putBoolean("budget_alerts", value).apply() }
    fun updateLocalProfile(user: UserEntity) { val normalized = user.name.trim(); if (normalized.length < 2 || normalized.length > 100) return; viewModelScope.launch { runCatching { repository.updateCurrentUser(user.copy(name = normalized)) }.onFailure { Log.e("BYE_PRO_FINANCE", "Update local profile failed", it) } } }
    private val _currentScreen = MutableStateFlow("dashboard")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()
    private val _scannedReceipt = MutableStateFlow<ExtractedReceiptData?>(null)
    val scannedReceipt: StateFlow<ExtractedReceiptData?> = _scannedReceipt.asStateFlow()
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()
    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()
    init { resetWelcomeMessage(); viewModelScope.launch { runCatching { repository.accounts.first(); repository.archivedAccounts.first() }; _accountsLoading.value = false } }
    private fun resetWelcomeMessage() { _chatMessages.value = listOf(ChatMessage("AI", WebI18n.tr("مرحباً بك في BYE PRO FINANCE! أنا مساعدك المالي الذكي. يمكنك سؤالي عن مصروفاتك، ميزانيتك، أو تحليل وضعك المالي بدقة.", "Welcome to BYE PRO FINANCE! I am your AI Financial Assistant. Ask me anything about your cash flow, budgets, or savings goals.", _appLanguage.value))) }
    fun setScreen(screen: String) { _currentScreen.value = screen }
    fun toggleLanguage() { setLanguage(if (_appLanguage.value == AppLanguage.AR) AppLanguage.EN else AppLanguage.AR) }
    fun setLanguage(lang: AppLanguage) { if (_appLanguage.value == lang) return; _appLanguage.value = lang; WebI18n.write(getApplication(), lang); if (_chatMessages.value.size <= 1) resetWelcomeMessage() }
    fun setDarkMode(dark: Boolean) { _isDarkMode.value = dark; WebI18n.writeDarkMode(getApplication(), dark) }
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
    suspend fun synthesizeSpeech(text: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        runCatching {
            require(text.isNotBlank() && text.length <= 5_000) { "SPEECH_TEXT_INVALID" }
            check(authRepository.restoreSession()) { "AUTH_REQUIRED" }
            val accessToken = authRepository.currentAccessToken() ?: error("AUTH_REQUIRED")
            val response = FinanceApiProvider.api.synthesizeSpeech(
                bearerToken = "Bearer $accessToken",
                request = SpeechRequest(text = text)
            )
            if (!response.isSuccessful) {
                val providerError = response.errorBody()?.use { it.string() }.orEmpty()
                throw IOException("SPEECH_REQUEST_FAILED_${response.code()}: ${providerError.take(120)}")
            }
            val body = response.body() ?: throw IOException("SPEECH_AUDIO_MISSING")
            body.use { audio ->
                val bytes = audio.bytes()
                require(bytes.isNotEmpty() && bytes.size <= 8 * 1024 * 1024) { "SPEECH_AUDIO_SIZE_INVALID" }
                bytes
            }
        }
    }
    fun askAiAssistant(query: String, imageUri: Uri? = null) {
        if ((query.isBlank() && imageUri == null) || _isAiLoading.value) return
        val trimmedQuery = query.trim().ifBlank {
            WebI18n.tr("حلّل الصورة المرفقة واشرح ما يظهر فيها.", "Analyze the attached image and explain what is visible.", _appLanguage.value)
        }
        _chatMessages.value = _chatMessages.value + ChatMessage("USER", if (imageUri == null) trimmedQuery else "📷 $trimmedQuery")
        _isAiLoading.value = true
        viewModelScope.launch {
            val response = runCatching {
                val app = getApplication<Application>()
                val modelFile = File(app.filesDir, "ai-models/gemma-4-E2B-it.litertlm")
                val imageFile = imageUri?.let { uri ->
                    val mime = app.contentResolver.getType(uri).orEmpty()
                    val ext = if (mime.contains("png")) "png" else if (mime.contains("webp")) "webp" else "jpg"
                    val target = File(app.cacheDir, "bye-pro-ai-image-${System.currentTimeMillis()}.$ext")
                    app.contentResolver.openInputStream(uri)?.use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    } ?: error("تعذر فتح الصورة المرفقة.")
                    require(target.length() in 1..20_000_000L) { "حجم الصورة كبير جداً؛ اختر صورة أصغر من 20 MB." }
                    target
                }
                try {
                    val expenseCommand = if (imageFile == null) FinanceVoiceCommandParser.parseExpense(trimmedQuery) else null
                    if (expenseCommand != null) {
                        val activeAccounts = accounts.value.filterNot { it.isArchived }
                        val account = activeAccounts.firstOrNull { it.currency.equals(primaryCurrency.value, ignoreCase = true) }
                            ?: activeAccounts.firstOrNull()
                        if (account == null) {
                            WebI18n.tr(
                                "لا يوجد حساب نشط لتسجيل المصروف. أضف حساباً أولاً من قسم الحسابات.",
                                "There is no active account for this expense. Add an account first.",
                                _appLanguage.value
                            )
                        } else {
                            val now = System.currentTimeMillis()
                            val duplicate = transactions.value.any { existing ->
                                existing.type == TransactionType.EXPENSE &&
                                    existing.accountId == account.id &&
                                    existing.amount == expenseCommand.amount &&
                                    existing.category.equals(expenseCommand.category, ignoreCase = true) &&
                                    existing.description.equals(expenseCommand.description, ignoreCase = true) &&
                                    kotlin.math.abs(existing.date - now) <= 120_000L
                            }
                            if (duplicate) {
                                WebI18n.tr(
                                    "هذه المعاملة مسجلة بالفعل قبل قليل؛ لم أكرر الخصم.",
                                    "This transaction was already recorded moments ago; I did not deduct it twice.",
                                    _appLanguage.value
                                )
                            } else {
                                repository.addTransaction(
                                    TransactionEntity(
                                        accountId = account.id,
                                        type = TransactionType.EXPENSE,
                                        amount = expenseCommand.amount,
                                        currency = account.currency,
                                        category = expenseCommand.category,
                                        date = now,
                                        description = expenseCommand.description,
                                        status = TransactionStatus.COMPLETED
                                    )
                                )
                                WebI18n.tr(
                                    "تم تسجيل مصروف " + expenseCommand.amount + " " + account.currency + " ضمن تصنيف السكن، من حساب «" + account.name + "»، وتم تحديث رصيده.",
                                    "Recorded an expense of " + expenseCommand.amount + " " + account.currency + " under Housing from account “" + account.name + "”; its balance was updated.",
                                    _appLanguage.value
                                )
                            }
                        }
                    } else if (imageFile == null && isFastFinancialQuestion(trimmedQuery)) {
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
                    } else if (GemmaLocalAssistant.isAvailable(modelFile)) {
                        val verifiedFinancialContext = FinancialAssistantEngine.answer(
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
                        val contextSummary = "App language: " + _appLanguage.value +
                            ". Verified local finance context (use only when relevant; never invent missing facts):\n" +
                            verifiedFinancialContext
                        GemmaLocalAssistant.reply(modelFile, contextSummary + "\n\n" + trimmedQuery, imageFile)
                    } else {
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
                    }
                } finally {
                    imageFile?.delete()
                }
            }.getOrElse { error ->
                Log.e("BYE_PRO_FINANCE", "On-device AI request failed", error)
                WebI18n.tr(
                    "تعذر تشغيل النموذج المحلي: ${error.message ?: "خطأ غير معروف"}",
                    "Could not run the local model: ${error.message ?: "unknown error"}",
                    _appLanguage.value
                )
            }
            _isAiLoading.value = false
            _chatMessages.value = _chatMessages.value + ChatMessage("AI", response)
        }
    }
    private fun isFastFinancialQuestion(query: String): Boolean {
        val q = query.lowercase(java.util.Locale.ROOT)
        return listOf(
            "مصروف", "مصروفات", "انفقت", "أنفقت", "إنفاق", "صرف", "دخل", "راتب",
            "ميزاني", "دين", "ديون", "فاتورة", "فواتير", "ادخار", "توفير",
            "صافي الثروة", "ملخص مالي", "تحليل مالي", "حلل", "ماذا تستطيع",
            "spending", "expenses", "expense", "income", "salary", "budget",
            "debt", "bills", "savings", "net worth", "cash flow", "financial summary"
        ).any(q::contains)
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
