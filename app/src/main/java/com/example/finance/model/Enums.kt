package com.example.finance.model

enum class AccountType {
    CASH,
    BANK,
    WALLET,
    CREDIT_CARD,
    SAVINGS,
    INVESTMENT,
    OTHER
}

enum class TransactionType {
    INCOME,
    EXPENSE,
    TRANSFER,
    REFUND,
    ADJUSTMENT
}

enum class TransactionStatus {
    COMPLETED,
    PENDING,
    CANCELLED
}

enum class BudgetPeriod {
    WEEKLY,
    MONTHLY,
    YEARLY,
    CUSTOM
}

enum class DebtStatus {
    ACTIVE,
    PAID,
    DEFAULTED
}

enum class AssetType {
    PROPERTY,
    VEHICLE,
    CASH,
    INVESTMENTS,
    GOLD,
    OTHER
}

enum class DocumentType {
    RECEIPT,
    INVOICE,
    STATEMENT,
    CONTRACT,
    OTHER
}

enum class NotificationType {
    BUDGET_WARNING,
    BILL_DUE,
    DEBT_DUE,
    SAVINGS_GOAL,
    UNUSUAL_SPENDING,
    AI_INSIGHT,
    SECURITY_ALERT
}

enum class UserRole {
    ADMIN,
    USER
}

enum class AppLanguage {
    AR,
    EN
}
