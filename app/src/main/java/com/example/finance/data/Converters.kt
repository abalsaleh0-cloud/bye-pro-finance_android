package com.example.finance.data

import androidx.room.TypeConverter
import com.example.finance.model.AccountType
import com.example.finance.model.AppLanguage
import com.example.finance.model.AssetType
import com.example.finance.model.BudgetPeriod
import com.example.finance.model.DebtStatus
import com.example.finance.model.DocumentType
import com.example.finance.model.NotificationType
import com.example.finance.model.TransactionStatus
import com.example.finance.model.TransactionType
import com.example.finance.model.UserRole

class Converters {
    @TypeConverter fun fromAccountType(value: AccountType): String = value.name
    @TypeConverter fun toAccountType(value: String): AccountType = AccountType.valueOf(value)

    @TypeConverter fun fromTransactionType(value: TransactionType): String = value.name
    @TypeConverter fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)

    @TypeConverter fun fromTransactionStatus(value: TransactionStatus): String = value.name
    @TypeConverter fun toTransactionStatus(value: String): TransactionStatus = TransactionStatus.valueOf(value)

    @TypeConverter fun fromBudgetPeriod(value: BudgetPeriod): String = value.name
    @TypeConverter fun toBudgetPeriod(value: String): BudgetPeriod = BudgetPeriod.valueOf(value)

    @TypeConverter fun fromDebtStatus(value: DebtStatus): String = value.name
    @TypeConverter fun toDebtStatus(value: String): DebtStatus = DebtStatus.valueOf(value)

    @TypeConverter fun fromAssetType(value: AssetType): String = value.name
    @TypeConverter fun toAssetType(value: String): AssetType = AssetType.valueOf(value)

    @TypeConverter fun fromDocumentType(value: DocumentType): String = value.name
    @TypeConverter fun toDocumentType(value: String): DocumentType = DocumentType.valueOf(value)

    @TypeConverter fun fromNotificationType(value: NotificationType): String = value.name
    @TypeConverter fun toNotificationType(value: String): NotificationType = NotificationType.valueOf(value)

    @TypeConverter fun fromUserRole(value: UserRole): String = value.name
    @TypeConverter fun toUserRole(value: String): UserRole = UserRole.valueOf(value)

    @TypeConverter fun fromAppLanguage(value: AppLanguage): String = value.name
    @TypeConverter fun toAppLanguage(value: String): AppLanguage = AppLanguage.valueOf(value)
}
