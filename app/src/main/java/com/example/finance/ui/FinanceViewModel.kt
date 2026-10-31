package com.example.finance.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.finance.ai.ExtractedReceiptData
import com.example.finance.ai.WebAiService
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
    val repository = FinanceRepository(application.applicationContext)
    val aiService = WebAiService(application.applicationContext)
    val accounts = repository.accounts.withStartupFallback(emptyList()).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val archivedAccounts = repository.archivedAccounts.withStartupFallback(emptyList()).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _accountsLoading = MutableStateFlow(true)
    val accountsLoading = _accountsLoading.asStateFlow()
    private val _accountActionFeedback = MutableStateFlow<AccountActionFeedback?>(null)
    val accountActionFeedback = _accountActionFeedback.asStateFlow()
    val transactions = repository.transactions.withStartupFallback(emptyList()).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val transfers = repository.transfers.withStartupFallback(emptyList()).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val budgets = repository.budgets.withStartupFallback(emptyList()).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val debts = repository.debts.withStartupFallback(emptyList()).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val bills = repository.bills.withStartupFallback(emptyList()).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val savingsGoals = repository.savingsGoals.withStartupFallback(emptyList()).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val assets = repository.assets.withStartupFallback(emptyList()).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val householdMembers = repository.householdMembers.withStartupFallback(emptyList()).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val householdExpenses = repository.householdExpenses.withStartupFallback(emptyList()).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val householdBudgets = repository.householdBudgets.withStartupFallback(emptyList()).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val documents = repository.documents.withStartupFallback(emptyList()).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val notifications = repository.notifications.withStartupFallback(emptyList()).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val auditLogs = repository.auditLogs.withStartupFallback(emptyList()).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val currentUser = repository.currentUser.withStartupFallback(null).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    private val _appLanguage = MutableStateFlow(WebI18n.read(application))
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()
    private val preferences = application.getSharedPreferences("bye_pro_finance_preferences", Application.MODE_PRIVATE)
    private val _isDarkMode = MutableStateFlow(preferences.getBoolean("dark_mode", true))
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
    fun setPrimaryCurrency(currency: String) { val normalized = currency.trim().uppercase(); if (normalized !in setOf("LYD","USD","EUR","GBP","SAR","AED","EGP")) return; _primaryCurrency.value=normalized; preferences.edit().putString("currency_code",normalized).apply() }
    fun setTimeZone(value: String) { if (value !in setOf("Africa/Tripoli","Asia/Damascus","Asia/Amman","Europe/Istanbul","Europe/London","UTC")) return; _timeZone.value=value; preferences.edit().putString("time_zone",value).apply() }
    fun setDateFormat(value: String) { if (value !in setOf("YYYY-MM-DD","DD/MM/YYYY","MM/DD/YYYY")) return; _dateFormat.value=value; preferences.edit().putString("date_format",value).apply() }
    fun setNumberFormat(value: String) { if (value !in setOf("1,234.56","1.234,56","1 234,56")) return; _numberFormat.value=value; preferences.edit().putString("number_format",value).apply() }
    fun setEmailNotifications(value:Boolean){_emailNotifications.value=value;preferences.edit().putBoolean("email_notifications",value).apply()}
    fun setTransactionAlerts(value:Boolean){_transactionAlerts.value=value;preferences.edit().putBoolean("transaction_alerts",value).apply()}
    fun setBillReminders(value:Boolean){_billReminders.value=value;preferences.edit().putBoolean("bill_reminders",value).apply()}
    fun setBudgetAlerts(value:Boolean){_budgetAlerts.value=value;preferences.edit().putBoolean("budget_alerts",value).apply()}
    fun updateLocalProfile(user:UserEntity){val normalized=user.name.trim();if(normalized.length<2||normalized.length>100)return;viewModelScope.launch{runCatching{repository.updateCurrentUser(user.copy(name=normalized))}.onFailure{Log.e("BYE_PRO_FINANCE","Update profile failed",it)}}}
    private val _currentScreen=MutableStateFlow("dashboard")
    val currentScreen:StateFlow<String> = _currentScreen.asStateFlow()
    private val _scannedReceipt=MutableStateFlow<ExtractedReceiptData?>(null)
    val scannedReceipt:StateFlow<ExtractedReceiptData?> = _scannedReceipt.asStateFlow()
    private val _chatMessages=MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages:StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()
    private val _isAiLoading=MutableStateFlow(false)
    val isAiLoading:StateFlow<Boolean> = _isAiLoading.asStateFlow()
    init { resetWelcomeMessage(); viewModelScope.launch { runCatching { repository.accounts.first(); repository.archivedAccounts.first() }; _accountsLoading.value=false } }
    private fun resetWelcomeMessage(){_chatMessages.value=listOf(ChatMessage("AI",WebI18n.tr("مرحباً بك في BYE PRO FINANCE! أنا مساعدك المالي الذكي. يمكنك سؤالي عن مصروفاتك، ميزانيتك، أو تحليل وضعك المالي بدقة.","Welcome to BYE PRO FINANCE! I am your AI Financial Assistant. Ask me anything about your cash flow, budgets, or savings goals.",_appLanguage.value)))}
    fun setScreen(screen:String){_currentScreen.value=screen}
    fun toggleLanguage(){setLanguage(if(_appLanguage.value==AppLanguage.AR)AppLanguage.EN else AppLanguage.AR)}
    fun setLanguage(lang:AppLanguage){if(_appLanguage.value==lang)return;_appLanguage.value=lang;WebI18n.write(getApplication(),lang);if(_chatMessages.value.size<=1)resetWelcomeMessage()}
    fun setDarkMode(dark:Boolean){_isDarkMode.value=dark;preferences.edit().putBoolean("dark_mode",dark).apply()}
    fun toggleTheme(){setDarkMode(!_isDarkMode.value)}
    fun addAccount(account:AccountEntity){viewModelScope.launch{_accountActionFeedback.value=AccountActionFeedback("add",runCatching{repository.addAccount(account)}.isSuccess)}}
    fun clearAccountActionFeedback(){_accountActionFeedback.value=null}
    fun updateAccount(account:AccountEntity){viewModelScope.launch{repository.updateAccount(account)}}
    fun deleteAccount(account:AccountEntity){viewModelScope.launch{repository.deleteAccount(account)}}
    fun archiveAccount(accountId:Long,isArchived:Boolean){viewModelScope.launch{_accountActionFeedback.value=AccountActionFeedback(if(isArchived)"deactivate" else "activate",runCatching{repository.archiveAccount(accountId,isArchived)}.isSuccess)}}
    fun addTransaction(tx:TransactionEntity){viewModelScope.launch{repository.addTransaction(tx)}}
    fun updateTransactionStatus(tx:TransactionEntity,status:TransactionStatus){viewModelScope.launch{repository.updateTransactionStatus(tx,status)}}
    fun deleteTransaction(tx:TransactionEntity){viewModelScope.launch{repository.deleteTransaction(tx)}}
    fun executeTransfer(fromAccountId:Long,toAccountId:Long,amount:Double,fee:Double,currency:String,description:String,reference:String,onComplete:(Boolean)->Unit){viewModelScope.launch{onComplete(repository.executeTransfer(fromAccountId,toAccountId,amount,fee,currency,description,reference))}}
    fun addBudget(budget:BudgetEntity){viewModelScope.launch{repository.addBudget(budget)}}
    fun updateBudget(budget:BudgetEntity){viewModelScope.launch{repository.updateBudget(budget)}}
    fun deleteBudget(budget:BudgetEntity){viewModelScope.launch{repository.deleteBudget(budget)}}
    fun addDebt(debt:DebtEntity){viewModelScope.launch{repository.addDebt(debt)}}
    fun updateDebt(debt:DebtEntity){viewModelScope.launch{repository.updateDebt(debt)}}
    fun debtPayments(debtId:Long):Flow<List<DebtPaymentEntity>> = repository.debtPayments(debtId)
    fun payDebt(debtId:Long,amount:Double,notes:String,paymentDate:Long=System.currentTimeMillis()){viewModelScope.launch{repository.recordDebtPayment(debtId,amount,notes,paymentDate)}}
    fun deleteDebt(debt:DebtEntity){viewModelScope.launch{repository.deleteDebt(debt)}}
    fun addBill(bill:BillEntity){viewModelScope.launch{repository.addBill(bill)}}
    fun updateBill(bill:BillEntity){viewModelScope.launch{repository.updateBill(bill)}}
    fun markBillPaid(billId:Long,accountId:Long){viewModelScope.launch{repository.markBillPaid(billId,accountId)}}
    fun deleteBill(bill:BillEntity){viewModelScope.launch{repository.deleteBill(bill)}}
    fun addSavingsGoal(goal:SavingsGoalEntity){viewModelScope.launch{repository.addSavingsGoal(goal)}}
    fun updateSavingsGoal(goal:SavingsGoalEntity){viewModelScope.launch{repository.updateSavingsGoal(goal)}}
    fun contributeToSavings(goalId:Long,amount:Double,accountId:Long){viewModelScope.launch{repository.contributeToSavings(goalId,amount,accountId)}}
    fun deleteSavingsGoal(goal:SavingsGoalEntity){viewModelScope.launch{repository.deleteSavingsGoal(goal)}}
    fun addAsset(asset:AssetEntity){viewModelScope.launch{repository.addAsset(asset)}}
    fun updateAsset(asset:AssetEntity){viewModelScope.launch{repository.updateAsset(asset)}}
    fun deleteAsset(asset:AssetEntity){viewModelScope.launch{repository.deleteAsset(asset)}}
    fun addHouseholdMember(name:String,email:String,role:String,monthlyContribution:Double){viewModelScope.launch{runCatching{repository.addHouseholdMember(name,email,role,monthlyContribution)}}}
    fun updateHouseholdMember(member:HouseholdMemberEntity){viewModelScope.launch{runCatching{repository.updateHouseholdMember(member)}}}
    fun deleteHouseholdMember(member:HouseholdMemberEntity){viewModelScope.launch{runCatching{repository.deleteHouseholdMember(member)}}}
    fun addHouseholdExpense(title:String,amount:Double,payerId:Long?,category:String,currency:String=primaryCurrency.value,date:Long=System.currentTimeMillis(),isRecurring:Boolean=false,notes:String=""){viewModelScope.launch{runCatching{repository.addHouseholdExpense(title,amount,payerId,category,currency,date,isRecurring,notes)}}}
    fun updateHouseholdExpense(expense:HouseholdExpenseEntity){viewModelScope.launch{runCatching{repository.updateHouseholdExpense(expense)}}}
    fun deleteHouseholdExpense(expense:HouseholdExpenseEntity){viewModelScope.launch{runCatching{repository.deleteHouseholdExpense(expense)}}}
    fun addHouseholdBudget(name:String,amount:Double,currencyCode:String,period:String){viewModelScope.launch{runCatching{repository.addHouseholdBudget(name,amount,currencyCode,period)}}}
    fun deleteHouseholdBudget(budget:HouseholdBudgetEntity){viewModelScope.launch{runCatching{repository.deleteHouseholdBudget(budget)}}}
    fun addDocument(doc:DocumentEntity){viewModelScope.launch{runCatching{repository.addDocument(doc)}}}
    fun deleteDocument(doc:DocumentEntity){viewModelScope.launch{runCatching{repository.deleteDocument(doc)}}}
    fun markNotificationRead(id:Long){viewModelScope.launch{runCatching{repository.markNotificationRead(id)}}}
    fun markAllNotificationsRead(){viewModelScope.launch{runCatching{repository.markAllNotificationsRead()}}}
    fun deleteNotification(id:Long){viewModelScope.launch{runCatching{repository.deleteNotification(id)}}}
    fun clearNotifications(){viewModelScope.launch{runCatching{repository.clearNotifications()}}}
    fun clearAiChat(){if(!_isAiLoading.value)resetWelcomeMessage()}
    fun askAiAssistant(query:String){if(query.isBlank()||_isAiLoading.value)return;_chatMessages.value=_chatMessages.value+ChatMessage("USER",query.trim());_isAiLoading.value=true;viewModelScope.launch{val response=aiService.askAssistant(query.trim(),_appLanguage.value);_isAiLoading.value=false;_chatMessages.value=_chatMessages.value+ChatMessage("AI",response)}}
    private val _ocrError=MutableStateFlow<String?>(null)
    val ocrError:StateFlow<String?>=_ocrError.asStateFlow()
    fun scanReceipt(sampleText:String?=null,imageBase64:String?=null){viewModelScope.launch{_ocrError.value=null;aiService.parseReceiptImage(sampleText,imageBase64,_appLanguage.value).onSuccess{_scannedReceipt.value=it}.onFailure{_scannedReceipt.value=null;_ocrError.value=it.message?:WebI18n.tr("استخراج الفواتير غير متاح في محرك الويب الحالي","Receipt extraction is not available in the current Web AI engine",_appLanguage.value)}}}
    fun clearScannedReceipt(){_scannedReceipt.value=null;_ocrError.value=null}
    fun importTransactionsFromJson(json:String,onComplete:(Result<Int>)->Unit){viewModelScope.launch{onComplete(runCatching{repository.importTransactions(FinanceExportService.transactionsFromJson(json))})}}
    fun importTransactionsFromCsv(csv:String,onComplete:(Result<Int>)->Unit){viewModelScope.launch{onComplete(runCatching{repository.importTransactions(FinanceExportService.transactionsFromCsv(csv))})}}
    fun generateCsvExport():String=FinanceExportService.transactionsToCsv(transactions.value)
    fun generateJsonExport():String=FinanceExportService.transactionsToJson(transactions.value)
    fun restoreBackupFromJson(backupJson:String,onComplete:(Boolean)->Unit){viewModelScope.launch{onComplete(repository.restoreFromJson(backupJson))}}
}
private fun <T> Flow<T>.withStartupFallback(fallback:T):Flow<T> = catch{error->Log.e("BYE_PRO_FINANCE","Remote data stream failed during startup",error);emit(fallback)}
