package com.example.finance.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.finance.ai.ExtractedReceiptData
import com.example.finance.ai.FinancialAssistantEngine
import com.example.finance.ai.GeminiFinanceService
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
    val aiService = GeminiFinanceService()
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
    fun askAiAssistant(query: String) {
        if (query.isBlank() || _isAiLoading.value) return
        val trimmedQuery = query.trim()
        _chatMessages.value = _chatMessages.value + ChatMessage("USER", trimmedQuery)
        _isAiLoading.value = true
        viewModelScope.launch {
            val history = _chatMessages.value.dropLast(1).takeLast(12)
                .joinToString("\n") { "${it.sender}: ${it.text}" }
            val response = runCatching {
                val intent = aiService.understandTransactionIntent(
                    userQuery = trimmedQuery,
                    recentConversation = history,
                    accounts = accounts.value,
                    defaultCurrency = primaryCurrency.value,
                    language = _appLanguage.value
                )
                when (intent?.intent) {
                    "CREATE_EXPENSE", "CREATE_INCOME" -> {
                        val action = intent ?: return@runCatching WebI18n.tr("لم أتمكن من فهم العملية.", "I could not understand the transaction.", _appLanguage.value)
                        if (action.confidence < 0.72 || action.amount == null || action.category.isNullOrBlank()) {
                            action.clarification ?: WebI18n.tr(
                                "فهمت أنك تريد تسجيل عملية مالية، لكن أحتاج توضيح المبلغ أو نوع العملية أو تصنيفها.",
                                "I understood that you want to record a financial transaction, but I need the amount, transaction type, or category clarified.",
                                _appLanguage.value
                            )
                        } else {
                            val currency = action.currency ?: primaryCurrency.value
                            val matchingAccounts = accounts.value.filter {
                                !it.isArchived && it.currency.equals(currency, ignoreCase = true)
                            }
                            val account = action.accountName?.let { requested ->
                                matchingAccounts.firstOrNull { it.name.equals(requested, ignoreCase = true) }
                            } ?: matchingAccounts.singleOrNull()
                            when {
                                account == null && matchingAccounts.isEmpty() -> WebI18n.tr(
                                    "لا يوجد حساب نشط بعملة $currency. أضف حساباً بهذه العملة أولاً.",
                                    "There is no active account in $currency. Add an account in this currency first.",
                                    _appLanguage.value
                                )
                                account == null -> action.clarification ?: WebI18n.tr(
                                    "لديك أكثر من حساب بعملة $currency. أي حساب تريد استخدامه؟",
                                    "You have more than one account in $currency. Which account should I use?",
                                    _appLanguage.value
                                )
                                else -> {
                                    val transactionType = if (action.intent == "CREATE_EXPENSE") TransactionType.EXPENSE else TransactionType.INCOME
                                    val transactionDate = action.dateIso?.let { date ->
                                        runCatching {
                                            java.time.LocalDate.parse(date).atStartOfDay(
                                                java.time.ZoneId.of(timeZone.value)
                                            ).toInstant().toEpochMilli()
                                        }.getOrNull()
                                    } ?: System.currentTimeMillis()
                                    val transaction = TransactionEntity(
                                        accountId = account.id,
                                        type = transactionType,
                                        amount = action.amount,
                                        currency = currency,
                                        category = action.category,
                                        date = transactionDate,
                                        description = action.description ?: action.category,
                                        status = TransactionStatus.COMPLETED
                                    )
                                    repository.addTransaction(transaction)
                                    WebI18n.tr(
                                        "تم تسجيل العملية بنجاح: ${action.amount} $currency — ${action.category} — ${account.name}.",
                                        "Transaction recorded: ${action.amount} $currency — ${action.category} — ${account.name}.",
                                        _appLanguage.value
                                    )
                                }
                            }
                        }
                    }
                    "CREATE_TRANSFER" -> {
                        val action = intent ?: return@runCatching "لم أتمكن من فهم التحويل."
                        val amount = action.amount
                        val currency = action.currency ?: primaryCurrency.value
                        if (action.confidence < 0.72 || amount == null || action.fromAccountName.isNullOrBlank() || action.toAccountName.isNullOrBlank()) {
                            action.clarification ?: WebI18n.tr(
                                "أحتاج معرفة المبلغ والحساب الذي سيُخصم منه والحساب الذي سيستقبل التحويل.",
                                "I need the amount, source account, and destination account for the transfer.",
                                _appLanguage.value
                            )
                        } else {
                            val source = accounts.value.firstOrNull { it.name.equals(action.fromAccountName, true) && !it.isArchived }
                            val destination = accounts.value.firstOrNull { it.name.equals(action.toAccountName, true) && !it.isArchived }
                            when {
                                source == null || destination == null -> WebI18n.tr("لم أتمكن من مطابقة أحد الحسابين. اذكر اسمي الحسابين كما يظهران في التطبيق.", "I could not match one of the accounts. Please use their exact names from the app.", _appLanguage.value)
                                source.id == destination.id -> WebI18n.tr("حساب التحويل المصدر والوجهة لا يمكن أن يكونا الحساب نفسه.", "The source and destination accounts must be different.", _appLanguage.value)
                                !source.currency.equals(destination.currency, true) || !source.currency.equals(currency, true) -> WebI18n.tr("يجب أن تكون عملة الحسابين وعملة التحويل متطابقة.", "Both accounts and the transfer must use the same currency.", _appLanguage.value)
                                else -> {
                                    repository.executeTransfer(source.id, destination.id, amount, 0.0, currency, action.description ?: "تحويل عبر المساعد الذكي", "")
                                    WebI18n.tr("تم تنفيذ التحويل: $amount $currency من ${source.name} إلى ${destination.name}.", "Transfer completed: $amount $currency from ${source.name} to ${destination.name}.", _appLanguage.value)
                                }
                            }
                        }
                    }
                    "CREATE_DEBT" -> {
                        val action = intent ?: return@runCatching "لم أتمكن من فهم الدين."
                        val amount = action.amount
                        val party = action.partyName ?: action.name
                        val direction = action.direction
                        if (action.confidence < 0.72 || amount == null || party.isNullOrBlank() || direction == null) {
                            action.clarification ?: WebI18n.tr("أحتاج معرفة اسم الشخص، المبلغ، وهل الدين عليك أم لك.", "I need the person's name, amount, and whether you owe them or they owe you.", _appLanguage.value)
                        } else {
                            repository.addDebt(DebtEntity(
                                name = party,
                                creditor = party,
                                principal = amount,
                                remaining = amount,
                                currency = action.currency ?: primaryCurrency.value,
                                dueDate = action.dueDateIso?.let { date -> runCatching { java.time.LocalDate.parse(date).atStartOfDay(java.time.ZoneId.of(timeZone.value)).toInstant().toEpochMilli() }.getOrNull() } ?: System.currentTimeMillis() + 90L * 24 * 60 * 60 * 1000,
                                notes = action.description ?: "",
                                direction = direction
                            ))
                            WebI18n.tr("تم تسجيل دين $party بقيمة $amount.", "Debt for $party recorded at $amount.", _appLanguage.value)
                        }
                    }
                    "PAY_DEBT" -> {
                        val action = intent ?: return@runCatching "لم أتمكن من تحديد الدين."
                        val party = action.partyName ?: action.name
                        val amount = action.amount
                        if (action.confidence < 0.72 || party.isNullOrBlank() || amount == null) {
                            action.clarification ?: WebI18n.tr("لمن دفعت؟ وما قيمة الدفعة؟", "Who did you pay, and how much was the payment?", _appLanguage.value)
                        } else {
                            val matchingDebts = debts.value.filter { it.status == DebtStatus.ACTIVE && (it.name.equals(party, true) || it.creditor.equals(party, true)) && (action.currency == null || it.currency.equals(action.currency, true)) }
                            when {
                                matchingDebts.isEmpty() -> WebI18n.tr("لم أجد ديناً نشطاً باسم $party.", "I could not find an active debt for $party.", _appLanguage.value)
                                matchingDebts.size > 1 -> WebI18n.tr("لديك أكثر من دين مرتبط بـ $party. حدّد الدين المقصود.", "There is more than one debt for $party. Please identify which one.", _appLanguage.value)
                                repository.recordDebtPayment(matchingDebts.first().id, amount, action.description ?: "دفعة مسجلة عبر المساعد الذكي") -> WebI18n.tr("تم تسجيل دفعة $amount للدين المرتبط بـ $party.", "Payment of $amount recorded for the debt linked to $party.", _appLanguage.value)
                                else -> WebI18n.tr("لم تُسجّل الدفعة. تحقق من المبلغ والرصيد المتبقي للدين.", "The payment was not recorded. Check the amount and remaining debt balance.", _appLanguage.value)
                            }
                        }
                    }
                    "CREATE_BILL" -> {
                        val action = intent ?: return@runCatching "لم أتمكن من فهم الفاتورة."
                        val name = action.name ?: action.description
                        val amount = action.amount
                        val dueDate = action.dueDateIso?.let { date -> runCatching { java.time.LocalDate.parse(date).atStartOfDay(java.time.ZoneId.of(timeZone.value)).toInstant().toEpochMilli() }.getOrNull() }
                        if (action.confidence < 0.72 || name.isNullOrBlank() || action.category.isNullOrBlank() || amount == null || dueDate == null) {
                            action.clarification ?: WebI18n.tr("أحتاج اسم الفاتورة وتصنيفها ومبلغها وتاريخ استحقاقها.", "I need the bill name, category, amount, and due date.", _appLanguage.value)
                        } else {
                            repository.addBill(BillEntity(name = name, category = action.category, amount = amount, currency = action.currency ?: primaryCurrency.value, dueDate = dueDate, isRecurring = action.isRecurring))
                            WebI18n.tr("تمت إضافة فاتورة $name بقيمة $amount.", "Bill $name added for $amount.", _appLanguage.value)
                        }
                    }
                    "CREATE_BUDGET" -> {
                        val action = intent ?: return@runCatching "لم أتمكن من فهم الميزانية."
                        val amount = action.amount
                        if (action.confidence < 0.72 || action.category.isNullOrBlank() || amount == null) {
                            action.clarification ?: WebI18n.tr("أحتاج تصنيف الميزانية والحد المالي المطلوب.", "I need the budget category and spending limit.", _appLanguage.value)
                        } else {
                            val period = runCatching { BudgetPeriod.valueOf(action.period ?: "MONTHLY") }.getOrDefault(BudgetPeriod.MONTHLY)
                            val today = java.time.LocalDate.now(java.time.ZoneId.of(timeZone.value))
                            val end = when (period) {
                                BudgetPeriod.WEEKLY -> today.plusWeeks(1)
                                BudgetPeriod.MONTHLY -> today.plusMonths(1)
                                BudgetPeriod.QUARTERLY -> today.plusMonths(3)
                                BudgetPeriod.YEARLY -> today.plusYears(1)
                                BudgetPeriod.CUSTOM -> today.plusMonths(1)
                            }
                            repository.addBudget(BudgetEntity(name = action.name ?: action.category, category = action.category, period = period, limitAmount = amount, currency = action.currency ?: primaryCurrency.value, startDate = today.atStartOfDay(java.time.ZoneId.of(timeZone.value)).toInstant().toEpochMilli(), endDate = end.atStartOfDay(java.time.ZoneId.of(timeZone.value)).toInstant().toEpochMilli()))
                            WebI18n.tr("تم إنشاء ميزانية ${action.category} بحد $amount.", "Budget for ${action.category} created with a limit of $amount.", _appLanguage.value)
                        }
                    }
                    "CREATE_SAVINGS_GOAL" -> {
                        val action = intent ?: return@runCatching "لم أتمكن من فهم هدف الادخار."
                        val name = action.name ?: action.description
                        val target = action.targetAmount ?: action.amount
                        val deadline = action.dueDateIso?.let { date -> runCatching { java.time.LocalDate.parse(date).atStartOfDay(java.time.ZoneId.of(timeZone.value)).toInstant().toEpochMilli() }.getOrNull() }
                        if (action.confidence < 0.72 || name.isNullOrBlank() || target == null || deadline == null) {
                            action.clarification ?: WebI18n.tr("أحتاج اسم هدف الادخار والمبلغ المستهدف والموعد النهائي.", "I need the savings goal name, target amount, and deadline.", _appLanguage.value)
                        } else {
                            repository.addSavingsGoal(SavingsGoalEntity(name = name, targetAmount = target, currency = action.currency ?: primaryCurrency.value, deadline = deadline))
                            WebI18n.tr("تم إنشاء هدف الادخار $name بقيمة مستهدفة $target.", "Savings goal $name created with a target of $target.", _appLanguage.value)
                        }
                    }
                    "CREATE_ASSET" -> {
                        val action = intent ?: return@runCatching "لم أتمكن من فهم الأصل."
                        val name = action.name ?: action.description
                        val value = action.amount
                        if (action.confidence < 0.72 || name.isNullOrBlank() || value == null) {
                            action.clarification ?: WebI18n.tr("أحتاج اسم الأصل وقيمته.", "I need the asset name and value.", _appLanguage.value)
                        } else {
                            val type = runCatching { AssetType.valueOf(action.assetType ?: "OTHER") }.getOrDefault(AssetType.OTHER)
                            repository.addAsset(AssetEntity(name = name, type = type, value = value, currency = action.currency ?: primaryCurrency.value, notes = action.description ?: ""))
                            WebI18n.tr("تمت إضافة الأصل $name بقيمة $value.", "Asset $name added with a value of $value.", _appLanguage.value)
                        }
                    }
                    "UPDATE", "DELETE" -> WebI18n.tr(
                        "فهمت أنك تريد تعديل أو حذف سجل، لكنني لن أغيّر أي بيانات قبل تحديد السجل بدقة وتأكيد العملية.",
                        "I understand you want to update or delete a record, but I will not change data until the exact record is identified and you confirm.",
                        _appLanguage.value
                    )
                    "CLARIFY" -> intent?.clarification ?: WebI18n.tr(
                        "لم أفهم قصدك بالكامل. ممكن توضح لي أكثر؟",
                        "I did not fully understand. Could you clarify?",
                        _appLanguage.value
                    )
                    else -> {
                        val aiResponse = aiService.askAssistant(
                            userQuery = trimmedQuery,
                            accounts = accounts.value,
                            transactions = transactions.value,
                            budgets = budgets.value,
                            debts = debts.value,
                            language = _appLanguage.value,
                            savingsGoals = savingsGoals.value,
                            assets = assets.value,
                            bills = bills.value,
                            conversationHistory = history
                        )
                        if (aiResponse.startsWith("تعذر الوصول إلى خدمة الذكاء الاصطناعي") ||
                            aiResponse.startsWith("The AI service is currently unavailable")) {
                            WebI18n.tr(
                                "خدمة الذكاء الاصطناعي غير متاحة حالياً. لم أسجل أو أعدّل أي بيانات. حاول مجدداً بعد عودة الاتصال بالخدمة.",
                                "The AI service is currently unavailable. I did not record or change any data. Please try again when the service is available.",
                                _appLanguage.value
                            )
                        } else aiResponse
                    }
                }
            }.getOrElse { error ->
                Log.e("BYE_PRO_FINANCE", "AI conversation failed", error)
                WebI18n.tr(
                    "تعذر إكمال الطلب: ${error.message ?: "خطأ غير معروف"}",
                    "Could not complete the request: ${error.message ?: "unknown error"}",
                    _appLanguage.value
                )
            }
            _isAiLoading.value = false
            _chatMessages.value = _chatMessages.value + ChatMessage("AI", response)
        }
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
