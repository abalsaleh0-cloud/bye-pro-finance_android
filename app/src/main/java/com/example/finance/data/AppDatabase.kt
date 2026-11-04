package com.example.finance.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.TypeConverters
import com.example.finance.model.AccountEntity
import com.example.finance.model.AssetEntity
import com.example.finance.model.AuditLogEntity
import com.example.finance.model.BillEntity
import com.example.finance.model.BudgetEntity
import com.example.finance.model.DebtEntity
import com.example.finance.model.DebtPaymentEntity
import com.example.finance.model.DocumentEntity
import com.example.finance.model.HouseholdExpenseEntity
import com.example.finance.model.HouseholdMemberEntity
import com.example.finance.model.NotificationEntity
import com.example.finance.model.SavingsGoalEntity
import com.example.finance.model.TransactionEntity
import com.example.finance.model.TransferEntity
import com.example.finance.model.UserEntity
import com.example.finance.model.WorkSourceEntity
import com.example.finance.model.WorkRecordEntity

@Database(
    entities = [
        UserEntity::class,
        AccountEntity::class,
        TransactionEntity::class,
        TransferEntity::class,
        BudgetEntity::class,
        DebtEntity::class,
        DebtPaymentEntity::class,
        BillEntity::class,
        SavingsGoalEntity::class,
        AssetEntity::class,
        HouseholdMemberEntity::class,
        HouseholdExpenseEntity::class,
        DocumentEntity::class,
        NotificationEntity::class,
        AuditLogEntity::class,
        WorkSourceEntity::class,
        WorkRecordEntity::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE household_expenses ADD COLUMN currency TEXT NOT NULL DEFAULT 'LYD'")
                database.execSQL("ALTER TABLE documents ADD COLUMN currency TEXT NOT NULL DEFAULT 'LYD'")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE accounts ADD COLUMN openingBalance REAL NOT NULL DEFAULT 0")
                database.execSQL("UPDATE accounts SET openingBalance = balance")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE debts ADD COLUMN direction TEXT NOT NULL DEFAULT 'i_owe'")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE debt_payments ADD COLUMN currency TEXT NOT NULL DEFAULT 'LYD'")
                database.execSQL("UPDATE debt_payments SET currency = (SELECT currency FROM debts WHERE debts.id = debt_payments.debtId) WHERE EXISTS (SELECT 1 FROM debts WHERE debts.id = debt_payments.debtId)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS work_sources (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, type TEXT NOT NULL, monthlySalary REAL NOT NULL, currency TEXT NOT NULL, isActive INTEGER NOT NULL, createdAt INTEGER NOT NULL)")
                database.execSQL("CREATE TABLE IF NOT EXISTS work_records (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, sourceId INTEGER, relatedRecordId INTEGER, type TEXT NOT NULL, amount REAL NOT NULL, currency TEXT NOT NULL, date INTEGER NOT NULL, period TEXT NOT NULL, notes TEXT NOT NULL, linkedAccountId INTEGER, linkedTransactionId INTEGER)")
            }
        }
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE work_records ADD COLUMN scope TEXT NOT NULL DEFAULT 'EMPLOYMENT'")
                database.execSQL("UPDATE work_records SET scope = (SELECT type FROM work_sources WHERE work_sources.id = work_records.sourceId) WHERE sourceId IS NOT NULL AND EXISTS (SELECT 1 FROM work_sources WHERE work_sources.id = work_records.sourceId)")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bye_pro_finance.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
