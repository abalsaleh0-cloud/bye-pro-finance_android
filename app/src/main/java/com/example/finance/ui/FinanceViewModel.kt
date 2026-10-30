package com.example.finance.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.finance.ai.ExtractedReceiptData
import com.example.finance.ai.GeminiFinanceService
import com.example.finance.data.AppDatabase
import com.example.finance.data.FinanceRepository
import com.example.finance.data.FinanceExportService
import com.example.finance.engine.FinanceEngine
import com.example.finance.i18n.WebI18n
import com.example.finance.model.AccountEntity
import com.example.finance.model.AppLanguage
import com.example.finance.model.AssetEntity
import com.example.finance.model.AuditLogEntity
import com.example.finance.model.BillEntity
import com.example.finance.model.BudgetEntity
import com.example.finance.model.DebtEntity
import com.example.finance.model.DocumentEntity
import com.example.finance.model.HouseholdExpenseEntity
import com.example.finance.model.HouseholdMemberEntity
import com.example.finance.model.NotificationEntity
import com.example.finance.model.SavingsGoalEntity
import com.example.finance.model.TransactionEntity
import com.example.finance.model.TransferEntity
import com.example.finance.model.UserEntity
import com.example.finance.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChatMessage(
    val sender: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = FinanceRepository(db.financeDao(), db)
    val aiService = GeminiFinanceService()

    val accounts: StateFlow<List<AccountEntity>> = repository.accounts.withStartupFallback(emptyList()).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
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

    // Shared with the web application: same locale values, storage key, default, and fallback.
    private val _appLanguage = MutableStateFlow(WebI18n.read(application))
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    private val preferences = application.getSharedPreferences("bye_pro_finance_preferences", Application.MODE_PRIVATE)
    private val _isDarkMode = MutableStateFlow(preferences.getBoolean("dark_mode", true))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _primaryCurrency = MutableStateFlow(preferences.getString("currency_code", "LYD") ?: "LYD")
    val primaryCurrency: StateFlow<String> = _primaryCurrency.asStateFlow()

    fun setPrimaryCurrency(currency: String) {
        val normalized = currency.trim().uppercase()
        if (normalized !in setOf("LYD", "USD", "EUR", "GBP", "SAR", "AED", "EGP")) return
        _primaryCurrency.value = normalized
        preferences.edit().putString("currency_code", normalized).apply()
    }

    private val _currentScreen = MutableStateFlow("dashboard")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    private val _scannedReceipt = MutableStateFlow<ExtractedReceiptData?>(null)
    val scannedReceipt: StateFlow<ExtractedReceiptData?> = _scannedReceipt.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()
    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    init {
        // Match the web app: never inject fictitious accounts or transactions into a new profile.
        resetWelcomeMessage()
    }

    private fun resetWelcomeMessage() {
        _chatMessages.value = listOf(
            ChatMessage(
                "AI",
                WebI18n.tr(
                    "مرحباً بك في BYE PRO FINANCE! أنا مساعدك المالي الذكي. يمكنك سؤالي عن مصروفاتك، ميزانيتك، أو تحليل وضعك المالي بدقة.",
                    "Welcome to BYE PRO FINANCE! I am your AI Financial Assistant. Ask me anything about your cash flow, budgets, or savings goals.",
                    _appLanguage.value
                )
            )
        )
    }

    fun setScreen(screen: String) { _currentScreen.value = screen }

    fun toggleLanguage() {
        setLanguage(if (_appLanguage.value == AppLanguage.AR) AppLanguage.EN else AppLanguage.AR)
    }

    fun setLanguage(lang: AppLanguage) {
        if (_appLanguage.value == lang) return
        _appLanguage.value = lang
        WebI18n.write(getApplication(), lang)
        if (_chatMessages.value.size <= 1) resetWelcomeMessage()
    }

    fun setDarkMode(dark: Boolean) {
        _isDarkMode.value = dark
        preferences.edit().putBoolean("dark_mode", dark).apply()
    }

    fun toggleTheme() { setDarkMode(!_isDarkMode.value) }

    fun addAccount(account: AccountEntity) { viewModelScope.launch { repository.addAccount(account) } }
    fun updateAccount(account: AccountEntity) { viewModelScope.launch { repository.updateAccount(account) } }
    fun deleteAccount(account: AccountEntity) { viewModelScope.launch { repository.deleteAccount(account) } }
    fun archiveAccount(accountId: Long, isArchived: Boolean) { viewModelScope.launch { repository.archiveAccount(accountId, isArchived) } }
    fun addTransaction(tx: TransactionEntity) { viewModelScope.launch { repository.addTransaction(tx) } }
    fun deleteTransaction(tx: TransactionEntity) { viewModelScope.launch { repository.deleteTransaction(tx) } }

    fun executeTransfer(fromAccountId: Long, toAccountId: Long, amount: Double, fee: Double, currency: String, description: String, reference: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            onComplete(repository.executeTransfer(fromAccountId, toAccountId, amount, fee, currency, description, reference))
        }
    }

    fun addBudget(budget: BudgetEntity) { viewModelScope.launch { repository.addBudget(budget) } }
    fun deleteBudget(budget: BudgetEntity) { viewModelScope.launch { repository.deleteBudget(budget) } }
    fun addDebt(debt: DebtEntity) { viewModelScope.launch { repository.addDebt(debt) } }
    fun payDebt(debtId: Long, amount: Double, notes: String) { viewModelScope.launch { repository.recordDebtPayment(debtId, amount, notes) } }
    fun deleteDebt(debt: DebtEntity) { viewModelScope.launch { repository.deleteDebt(debt) } }
    fun addBill(bill: BillEntity) { viewModelScope.launch { repository.addBill(bill) } }
    fun markBillPaid(billId: Long, accountId: Long) { viewModelScope.launch { repository.markBillPaid(billId, accountId) } }
    fun deleteBill(bill: BillEntity) { viewModelScope.launch { repository.deleteBill(bill) } }
    fun addSavingsGoal(goal: SavingsGoalEntity) { viewModelScope.launch { repository.addSavingsGoal(goal) } }
    fun contributeToSavings(goalId: Long, amount: Double, accountId: Long) { viewModelScope.launch { repository.contributeToSavings(goalId, amount, accountId) } }
    fun deleteSavingsGoal(goal: SavingsGoalEntity) { viewModelScope.launch { repository.deleteSavingsGoal(goal) } }
    fun addAsset(asset: AssetEntity) { viewModelScope.launch { repository.addAsset(asset) } }
    fun deleteAsset(asset: AssetEntity) { viewModelScope.launch { repository.deleteAsset(asset) } }
    fun addHouseholdMember(name: String, email: String, role: String, percent: Double) { viewModelScope.launch { repository.addHouseholdMember(name, email, role, percent) } }
    fun addHouseholdExpense(title: String, amount: Double, payerId: Long, category: String, currency: String = primaryCurrency.value) { viewModelScope.launch { repository.addHouseholdExpense(title, amount, payerId, category, currency) } }
    fun addDocument(doc: DocumentEntity) { viewModelScope.launch { repository.addDocument(doc) } }
    fun deleteDocument(doc: DocumentEntity) { viewModelScope.launch { repository.deleteDocument(doc) } }
    fun markNotificationRead(id: Long) { viewModelScope.launch { repository.markNotificationRead(id) } }
    fun clearNotifications() { viewModelScope.launch { repository.clearNotifications() } }

    fun toggleUserRole() {
        viewModelScope.launch {
            val user = currentUser.value ?: return@launch
            val newRole = if (user.role == UserRole.ADMIN) UserRole.USER else UserRole.ADMIN
            repository.updateCurrentUser(user.copy(role = newRole))
        }
    }

    fun askAiAssistant(query: String) {
        if (query.isBlank()) return
        _chatMessages.value = _chatMessages.value + ChatMessage("USER", query)
        _isAiLoading.value = true
        viewModelScope.launch {
            val response = aiService.askAssistant(query, accounts.value, transactions.value, budgets.value, debts.value, _appLanguage.value)
            _isAiLoading.value = false
            _chatMessages.value = _chatMessages.value + ChatMessage("AI", response)
        }
    }

    private val _ocrError = MutableStateFlow<String?>(null)
    val ocrError: StateFlow<String?> = _ocrError.asStateFlow()

    fun scanReceipt(sampleText: String? = null, imageBase64: String? = null) {
        viewModelScope.launch {
            _ocrError.value = null
            aiService.parseReceiptImage(sampleText, imageBase64, _appLanguage.value)
                .onSuccess { _scannedReceipt.value = it; _ocrError.value = null }
                .onFailure { _scannedReceipt.value = null; _ocrError.value = it.message ?: WebI18n.tr("تعذر استخراج بيانات الفاتورة آلياً", "Could not extract receipt data automatically", _appLanguage.value) }
        }
    }

    fun clearScannedReceipt() { _scannedReceipt.value = null; _ocrError.value = null }

    fun importTransactionsFromJson(json: String, onComplete: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            val result = runCatching {
                val rows = FinanceExportService.transactionsFromJson(json)
                repository.importTransactions(rows)
            }
            onComplete(result)
        }
    }

    fun generateCsvExport(): String =
        FinanceExportService.transactionsToCsv(transactions.value)

    fun generateJsonExport(): String =
        FinanceExportService.transactionsToJson(transactions.value)

    fun restoreBackupFromJson(backupJson: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch { onComplete(repository.restoreFromJson(backupJson)) }
    }
}


/**
 * A broken or older local database must not terminate the whole app while
 * observable Room streams are being collected. Keep the UI alive with safe
 * empty defaults and preserve the underlying exception in Logcat for repair.
 */
private fun <T> Flow<T>.withStartupFallback(fallback: T): Flow<T> =
    catch { error ->
        Log.e("BYE_PRO_FINANCE", "Local database stream failed during startup", error)
        emit(fallback)
    }
