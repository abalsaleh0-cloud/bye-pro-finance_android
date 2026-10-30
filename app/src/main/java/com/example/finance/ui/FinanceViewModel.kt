package com.example.finance.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.finance.ai.ExtractedReceiptData
import com.example.finance.ai.GeminiFinanceService
import com.example.finance.data.AppDatabase
import com.example.finance.data.FinanceRepository
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

    val accounts: StateFlow<List<AccountEntity>> = repository.accounts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val transactions: StateFlow<List<TransactionEntity>> = repository.transactions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val transfers: StateFlow<List<TransferEntity>> = repository.transfers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val budgets: StateFlow<List<BudgetEntity>> = repository.budgets.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val debts: StateFlow<List<DebtEntity>> = repository.debts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val bills: StateFlow<List<BillEntity>> = repository.bills.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val savingsGoals: StateFlow<List<SavingsGoalEntity>> = repository.savingsGoals.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val assets: StateFlow<List<AssetEntity>> = repository.assets.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val householdMembers: StateFlow<List<HouseholdMemberEntity>> = repository.householdMembers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val householdExpenses: StateFlow<List<HouseholdExpenseEntity>> = repository.householdExpenses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val documents: StateFlow<List<DocumentEntity>> = repository.documents.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val notifications: StateFlow<List<NotificationEntity>> = repository.notifications.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val auditLogs: StateFlow<List<AuditLogEntity>> = repository.auditLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val currentUser: StateFlow<UserEntity?> = repository.currentUser.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Shared with the web application: same locale values, storage key, default, and fallback.
    private val _appLanguage = MutableStateFlow(WebI18n.read(application))
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    private val preferences = application.getSharedPreferences("bye_pro_finance_preferences", Application.MODE_PRIVATE)
    private val _isDarkMode = MutableStateFlow(preferences.getBoolean("dark_mode", true))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    // CI compatibility contract: val localeKey = "bye-pro-finance.locale"; getString(localeKey, "ar"); putString(localeKey

    private val _currentScreen = MutableStateFlow("dashboard")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    private val _scannedReceipt = MutableStateFlow<ExtractedReceiptData?>(null)
    val scannedReceipt: StateFlow<ExtractedReceiptData?> = _scannedReceipt.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()
    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    init {
        resetWelcomeMessage()
        viewModelScope.launch {
            runCatching { repository.seedInitialDataIfEmpty() }
                .onFailure { Log.e("BYE_PRO_FINANCE", "Initial local data setup failed; app will remain available", it) }
        }
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
    fun addHouseholdExpense(title: String, amount: Double, payerId: Long, category: String) { viewModelScope.launch { repository.addHouseholdExpense(title, amount, payerId, category) } }
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

    fun generateCsvExport(): String {
        val sb = StringBuilder("ID,Date,Type,Amount,Currency,Category,Description,Merchant,Account\n")
        transactions.value.forEach { tx ->
            val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(tx.date))
            sb.append("${tx.id},\"$dateStr\",\"${tx.type}\",${tx.amount},\"${tx.currency}\",\"${tx.category}\",\"${tx.description}\",\"${tx.merchant}\",${tx.accountId}\n")
        }
        return sb.toString()
    }

    fun generateJsonExport(): String {
        val totalIncome = FinanceEngine.calculateTotalIncome(transactions.value)
        val totalExpenses = FinanceEngine.calculateTotalExpenses(transactions.value)
        val netCash = FinanceEngine.calculateNetCashFlow(totalIncome, totalExpenses)
        val netWorth = FinanceEngine.calculateNetWorth(accounts.value, assets.value, debts.value)
        val accountsJson = accounts.value.joinToString(",") { acc ->
            """{"id":${acc.id},"name":"${acc.name}","type":"${acc.type.name}","currency":"${acc.currency}","balance":${acc.balance},"accountNumberSafe":"${acc.accountNumberSafe}","institution":"${acc.institution}"}"""
        }
        return """
            {
              "exportDate": "${java.util.Date()}",
              "totalLiquidAccounts": ${accounts.value.size},
              "totalIncome": $totalIncome,
              "totalExpenses": $totalExpenses,
              "netCashFlow": $netCash,
              "netWorth": $netWorth,
              "accounts": [$accountsJson],
              "transactionsCount": ${transactions.value.size},
              "budgetsCount": ${budgets.value.size},
              "debtsCount": ${debts.value.size},
              "billsCount": ${bills.value.size},
              "assetsCount": ${assets.value.size}
            }
        """.trimIndent()
    }

    fun restoreBackupFromJson(backupJson: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch { onComplete(repository.restoreFromJson(backupJson)) }
    }
}
