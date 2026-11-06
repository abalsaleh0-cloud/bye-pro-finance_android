package com.example.finance.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.LayoutDirection
import com.example.finance.model.AppLanguage

val LocalAppLanguage = compositionLocalOf { AppLanguage.AR }

object Strings {
    fun get(key: String, lang: AppLanguage = AppLanguage.AR): String {
        val map = if (lang == AppLanguage.AR) arStrings else enStrings
        return map[key] ?: enStrings[key] ?: key
    }

    val arStrings = mapOf(
        "app_title" to "BYE PRO FINANCE",
        "tagline" to "النظام المالي الذكي والمتكامل",
        
        // Navigation Tabs
        "nav_dashboard" to "الرئيسية",
        "nav_accounts" to "الحسابات",
        "nav_transactions" to "المعاملات",
        "nav_budgets" to "الميزانيات",
        "nav_debts" to "الديون",
        "nav_bills" to "الفواتير",
        "nav_savings" to "الادخار",
        "nav_assets" to "الأصول",
        "nav_reports" to "التقارير",
        "nav_household" to "المنزل",
        "nav_smart_scan" to "المسح الذكي",
        "nav_ai_assistant" to "المساعد المالي",
        "nav_ai_insights" to "التحليلات الذكية",
        "nav_notifications" to "الإشعارات",
        "nav_admin" to "الإدارة والتدقيق",
        "nav_settings" to "الإعدادات",

        // Dashboard Metrics
        "total_balance" to "إجمالي الرصيد السائل",
        "net_worth" to "صافي الثروة",
        "monthly_income" to "الدخل الشهري",
        "monthly_expense" to "المصروفات الشهرية",
        "net_cash_flow" to "صافي التدفق النقدي",
        "health_score" to "مؤشر الصحة المالية",
        "excellent_health" to "ممتاز ومستقر",
        "good_health" to "جيد جداً",
        "warning_health" to "يحتاج إلى ترشيد",
        "quick_actions" to "إجراءات سريعة",
        "action_add_income" to "إيداع دخل",
        "action_add_expense" to "تسجيل مصروف",
        "action_transfer" to "تحويل أموال",
        "action_scan_receipt" to "مسح فاتورة",
        "recent_transactions" to "أحدث المعاملات",
        "view_all" to "عرض الكل",
        "active_accounts" to "الحسابات النشطة",
        "upcoming_bills" to "فواتير قادمة",
        "budget_health" to "حالة الميزانيات",

        // Accounts
        "accounts_title" to "إدارة الحسابات والمحافظ",
        "add_account" to "إضافة حساب جديد",
        "account_name" to "اسم الحساب",
        "account_type" to "نوع الحساب",
        "initial_balance" to "الرصيد الافتتاحي",
        "currency" to "العملة",
        "institution" to "المصرف / الجهة",
        "safe_number" to "رقم الحساب (آمن)",
        "account_type_bank" to "حساب بنكي",
        "account_type_cash" to "محفظة كاش",
        "account_type_wallet" to "محفظة إلكترونية",
        "account_type_savings" to "حساب ادخار",
        "account_type_invest" to "محفظة استثمار",
        "account_type_credit" to "بطاقة ائتمانية",

        // Transactions
        "transactions_title" to "سجل المعاملات المالية",
        "add_transaction" to "إضافة معاملة",
        "type" to "النوع",
        "amount" to "المبلغ",
        "category" to "التصنيف",
        "date" to "التاريخ",
        "description" to "الوصف",
        "merchant" to "التاجر / المستفيد",
        "select_account" to "اختر الحساب",
        "search_transactions" to "بحث في المعاملات والتاجر...",
        "filter_all" to "الكل",
        "type_income" to "دخل",
        "type_expense" to "مصروف",
        "type_transfer" to "تحويل",
        "type_refund" to "استرداد",

        // Transfers
        "transfers_title" to "التحويل بين الحسابات",
        "from_account" to "من حساب",
        "to_account" to "إلى حساب",
        "transfer_amount" to "مبلغ التحويل",
        "transfer_fee" to "رسوم التحويل (إن وجدت)",
        "execute_transfer" to "تنفيذ التحويل الفوري",
        "transfer_success" to "تم التحويل بنجاح وبشكل ذري وآمن",

        // Budgets
        "budgets_title" to "ميزانيات الإنفاق",
        "add_budget" to "إنشاء ميزانية",
        "limit_amount" to "الحد الأقصى للإنفاق",
        "spent" to "المصروف",
        "remaining" to "المتبقي",
        "over_budget" to "تجاوز الميزانية!",

        // Debts
        "debts_title" to "الديون والالتزامات",
        "add_debt" to "إضافة التزام / دين",
        "creditor" to "الجهة الدائنة",
        "principal" to "أصل الدين",
        "interest_rate" to "نسبة المرابحة / الفائدة",
        "min_payment" to "القسط الشهري الأدنى",
        "pay_debt" to "سداد دفعة",
        "debt_paid_off" to "تم السداد بالكامل",

        // Bills
        "bills_title" to "الفواتير المتكررة",
        "add_bill" to "إضافة فاتورة",
        "due_date" to "تاريخ الاستحقاق",
        "mark_paid" to "تأكيد الدفع",
        "paid_status" to "مدفوعة",
        "unpaid_status" to "مستحقة",

        // Savings
        "savings_title" to "أهداف الادخار والتوفير",
        "add_goal" to "إضافة هدف ادخاري",
        "target_amount" to "المبلغ المستهدف",
        "deadline" to "الموعد النهائي",
        "contribute" to "إيداع في الهدف",
        "monthly_needed" to "المطلوب شهرياً",

        // Assets
        "assets_title" to "سجل الأصول وصافي الثروة",
        "add_asset" to "إضافة أصل جديد",
        "asset_value" to "القيمة الحالية التقديرية",
        "asset_type_property" to "عقارات وأراضي",
        "asset_type_gold" to "ذهب ومعادن ثمينة",
        "asset_type_vehicle" to "مركبات وسيارات",
        "asset_type_investment" to "أسهم وصناديق",

        // Reports
        "reports_title" to "التقارير والتحليلات المالية",
        "export_csv" to "تصدير CSV",
        "export_json" to "تصدير تقرير JSON",
        "spending_by_category" to "توزيع المصروفات حسب التصنيف",
        "cash_flow_trend" to "مسار التدفق النقدي",

        // Household
        "household_title" to "مصروفات المنزل المشتركة",
        "add_member" to "إضافة فرد",
        "add_shared_expense" to "إضافة مصروف عائلي",
        "settle_up" to "تسوية الأرصدة",

        // Smart Scan OCR
        "smart_scan_title" to "المسح الذكي واستخراج بيانات الفواتير (OCR)",
        "scan_instruction" to "ارفع صورة الفاتورة أو الإيصال؛ سيقوم الذكاء الاصطناعي باستخراج التاجر والمبلغ والتصنيف والضريبة تلقائياً.",
        "select_receipt" to "اختيار صورة إيصال / فاتورة",
        "scan_sample" to "تجربة عينة فاتورة حقيقية",
        "extracted_merchant" to "التاجر المستخرج",
        "extracted_amount" to "المبلغ المستخرج",
        "extracted_tax" to "ضريبة القيمة المضافة",
        "extracted_category" to "التصنيف المقترح",
        "confirm_and_save" to "مراجعة وتأكيد حفظ المعاملة",

        // AI Assistant
        "ai_assistant_title" to "المساعد المالي الذكي (Gemini)",
        "ai_disclaimer" to "ملاحظة: هذا التحليل استرشادي مبني على بياناتك المالية المسجلة وليس استشارة مهنية مرخصة.",
        "ask_placeholder" to "اسأل المساعد المالي عن نفقاتك، ميزانيتك، أو نصائح التوفير...",
        "send" to "إرسال",
        "ai_chip_spending" to "كم إجمالي إنفاقي هذا الشهر؟",
        "ai_chip_highest" to "ما أعلى تصنيف أنفقت عليه؟",
        "ai_chip_budget" to "هل أنا في خطر تجاوز الميزانيات؟",
        "ai_chip_savings" to "كيف أستطيع زيادة نسبة مدخراتي؟",

        // Admin & Audit
        "admin_title" to "لوحة الإدارة وسجل التدقيق الأمني",
        "audit_logs" to "سجل العمليات الحساسة (Audit Trail)",
        "system_health" to "حالة النظام والأمان",
        "active_role" to "الدور الحالي",
        "switch_role" to "تبديل الصلاحية (Admin / User)",
        "encryption_status" to "تشفير البيانات: AES-256 مفعل محلياً",
        "database_records" to "إجمالي العمليات المسجلة",

        // Settings
        "settings_title" to "الإعدادات وتفضيلات النظام",
        "language" to "اللغة",
        "appearance_title" to "مظهر التطبيق",
        "dark_mode" to "الوضع الداكن",
        "light_mode" to "الوضع الفاتح",
        "appearance_hint" to "اختر ألوان BYE PRO FINANCE المناسبة لك.",
        "currency_pref" to "العملة الأساسية",
        "arabic" to "العربية",
        "english" to "English",
        "save_changes" to "حفظ التغييرات",
        "cancel" to "إلغاء",
        "delete" to "حذف",
        "confirm" to "تأكيد"
    )

    val enStrings = mapOf(
        "app_title" to "BYE PRO FINANCE",
        "tagline" to "Intelligent Comprehensive Financial System",
        
        // Navigation Tabs
        "nav_dashboard" to "Dashboard",
        "nav_accounts" to "Accounts",
        "nav_transactions" to "Transactions",
        "nav_budgets" to "Budgets",
        "nav_debts" to "Debts",
        "nav_bills" to "Bills",
        "nav_savings" to "Savings",
        "nav_assets" to "Assets",
        "nav_reports" to "Reports",
        "nav_household" to "Household",
        "nav_smart_scan" to "Smart Scan",
        "nav_ai_assistant" to "AI Assistant",
        "nav_ai_insights" to "AI Insights",
        "nav_notifications" to "Alerts",
        "nav_admin" to "Admin & Audit",
        "nav_settings" to "Settings",

        // Dashboard Metrics
        "total_balance" to "Total Liquid Balance",
        "net_worth" to "Net Worth",
        "monthly_income" to "Monthly Income",
        "monthly_expense" to "Monthly Expenses",
        "net_cash_flow" to "Net Cash Flow",
        "health_score" to "Financial Health Score",
        "excellent_health" to "Excellent & Solid",
        "good_health" to "Very Good",
        "warning_health" to "Needs Optimization",
        "quick_actions" to "Quick Actions",
        "action_add_income" to "Deposit Income",
        "action_add_expense" to "Log Expense",
        "action_transfer" to "Transfer Funds",
        "action_scan_receipt" to "Scan Receipt",
        "recent_transactions" to "Recent Transactions",
        "view_all" to "View All",
        "active_accounts" to "Active Accounts",
        "upcoming_bills" to "Upcoming Bills",
        "budget_health" to "Budget Health",

        // Accounts
        "accounts_title" to "Accounts & Portfolios",
        "add_account" to "Add New Account",
        "account_name" to "Account Name",
        "account_type" to "Account Type",
        "initial_balance" to "Initial Balance",
        "currency" to "Currency",
        "institution" to "Institution / Bank",
        "safe_number" to "Account Number (Masked)",
        "account_type_bank" to "Bank Account",
        "account_type_cash" to "Cash Wallet",
        "account_type_wallet" to "Digital Wallet",
        "account_type_savings" to "Savings Account",
        "account_type_invest" to "Investment Portfolio",
        "account_type_credit" to "Credit Card",

        // Transactions
        "transactions_title" to "Transaction Ledger",
        "add_transaction" to "Add Transaction",
        "type" to "Type",
        "amount" to "Amount",
        "category" to "Category",
        "date" to "Date",
        "description" to "Description",
        "merchant" to "Merchant / Payee",
        "select_account" to "Select Account",
        "search_transactions" to "Search transactions or merchant...",
        "filter_all" to "All",
        "type_income" to "Income",
        "type_expense" to "Expense",
        "type_transfer" to "Transfer",
        "type_refund" to "Refund",

        // Transfers
        "transfers_title" to "Account-to-Account Transfers",
        "from_account" to "From Account",
        "to_account" to "To Account",
        "transfer_amount" to "Transfer Amount",
        "transfer_fee" to "Transfer Fee (if any)",
        "execute_transfer" to "Execute Atomic Transfer",
        "transfer_success" to "Transfer completed atomically and securely",

        // Budgets
        "budgets_title" to "Spending Budgets",
        "add_budget" to "Create Budget",
        "limit_amount" to "Budget Limit",
        "spent" to "Spent",
        "remaining" to "Remaining",
        "over_budget" to "Over Budget!",

        // Debts
        "debts_title" to "Debts & Liabilities",
        "add_debt" to "Add Debt / Loan",
        "creditor" to "Creditor / Bank",
        "principal" to "Principal Amount",
        "interest_rate" to "Interest Rate %",
        "min_payment" to "Minimum Payment",
        "pay_debt" to "Record Payment",
        "debt_paid_off" to "Fully Paid Off",

        // Bills
        "bills_title" to "Recurring Bills",
        "add_bill" to "Add Bill",
        "due_date" to "Due Date",
        "mark_paid" to "Mark as Paid",
        "paid_status" to "Paid",
        "unpaid_status" to "Unpaid",

        // Savings
        "savings_title" to "Savings Goals",
        "add_goal" to "Add Savings Goal",
        "target_amount" to "Target Amount",
        "deadline" to "Target Date",
        "contribute" to "Contribute Funds",
        "monthly_needed" to "Monthly Deposit Required",

        // Assets
        "assets_title" to "Assets & Net Worth Portfolio",
        "add_asset" to "Add Asset",
        "asset_value" to "Current Estimated Value",
        "asset_type_property" to "Real Estate",
        "asset_type_gold" to "Gold & Precious Metals",
        "asset_type_vehicle" to "Vehicles & Automobiles",
        "asset_type_investment" to "Stocks & Funds",

        // Reports
        "reports_title" to "Financial Reports & Analytics",
        "export_csv" to "Export CSV",
        "export_json" to "Export JSON",
        "spending_by_category" to "Expense Breakdown by Category",
        "cash_flow_trend" to "Cash Flow Trend",

        // Household
        "household_title" to "Shared Household Expenses",
        "add_member" to "Add Member",
        "add_shared_expense" to "Add Household Expense",
        "settle_up" to "Settle Balances",

        // Smart Scan OCR
        "smart_scan_title" to "Smart OCR Document & Receipt Scanner",
        "scan_instruction" to "Upload receipt or document photo. AI will parse merchant, amount, tax, and category with confirmation before saving.",
        "select_receipt" to "Upload Receipt / Document",
        "scan_sample" to "Try Real Receipt Sample",
        "extracted_merchant" to "Extracted Merchant",
        "extracted_amount" to "Extracted Total",
        "extracted_tax" to "Extracted Tax / VAT",
        "extracted_category" to "Suggested Category",
        "confirm_and_save" to "Review & Save Transaction",

        // AI Assistant
        "ai_assistant_title" to "AI Financial Assistant (Gemini)",
        "ai_disclaimer" to "Note: This is an analytical tool based on recorded data, not professional licensed financial advice.",
        "ask_placeholder" to "Ask about your spending, budget health, or savings strategies...",
        "send" to "Send",
        "ai_chip_spending" to "What is my total spending this month?",
        "ai_chip_highest" to "What is my largest expense category?",
        "ai_chip_budget" to "Am I at risk of exceeding budgets?",
        "ai_chip_savings" to "How can I increase my savings rate?",

        // Admin & Audit
        "admin_title" to "Admin Dashboard & Audit Trail",
        "audit_logs" to "Security Audit Trail",
        "system_health" to "System & Security Health",
        "active_role" to "Current Role",
        "switch_role" to "Toggle Role (Admin / User)",
        "encryption_status" to "Storage Encryption: AES-256 local database active",
        "database_records" to "Total Ledger Records",

        // Settings
        "settings_title" to "Settings & Preferences",
        "language" to "Language",
        "appearance_title" to "Appearance",
        "dark_mode" to "Dark mode",
        "light_mode" to "Light mode",
        "appearance_hint" to "Choose your preferred BYE PRO FINANCE color theme.",
        "currency_pref" to "Primary Currency",
        "arabic" to "Arabic",
        "english" to "English",
        "save_changes" to "Save Changes",
        "cancel" to "Cancel",
        "delete" to "Delete",
        "confirm" to "Confirm"
    )
}
