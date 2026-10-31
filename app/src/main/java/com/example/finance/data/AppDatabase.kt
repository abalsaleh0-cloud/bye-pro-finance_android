package com.example.finance.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.TypeConverters
import com.example.finance.auth.SupabaseAuthRepository
import com.example.finance.model.*
import com.example.finance.supabase.HouseholdSyncRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class, AccountEntity::class, TransactionEntity::class, TransferEntity::class,
        BudgetEntity::class, DebtEntity::class, DebtPaymentEntity::class, BillEntity::class,
        SavingsGoalEntity::class, AssetEntity::class, HouseholdMemberEntity::class,
        HouseholdExpenseEntity::class, HouseholdBudgetEntity::class, DocumentEntity::class,
        NotificationEntity::class, AuditLogEntity::class
    ], version = 7, exportSchema = false
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
            override fun migrate(database: SupportSQLiteDatabase) { database.execSQL("ALTER TABLE debts ADD COLUMN direction TEXT NOT NULL DEFAULT 'i_owe'") }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE debt_payments ADD COLUMN currency TEXT NOT NULL DEFAULT 'LYD'")
                database.execSQL("UPDATE debt_payments SET currency = (SELECT currency FROM debts WHERE debts.id = debt_payments.debtId) WHERE EXISTS (SELECT 1 FROM debts WHERE debts.id = debt_payments.debtId)")
            }
        }
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE household_members ADD COLUMN monthlyContribution REAL NOT NULL DEFAULT 0.0")
                database.execSQL("ALTER TABLE household_members ADD COLUMN isActive INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE household_expenses RENAME TO household_expenses_old")
                database.execSQL("""CREATE TABLE household_expenses (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,title TEXT NOT NULL,amount REAL NOT NULL,payerMemberId INTEGER,category TEXT NOT NULL,currency TEXT NOT NULL DEFAULT 'LYD',date INTEGER NOT NULL,isSettled INTEGER NOT NULL,isRecurring INTEGER NOT NULL DEFAULT 0,notes TEXT NOT NULL DEFAULT '')""")
                database.execSQL("""INSERT INTO household_expenses (id,title,amount,payerMemberId,category,currency,date,isSettled) SELECT id,title,amount,payerMemberId,category,currency,date,isSettled FROM household_expenses_old""")
                database.execSQL("DROP TABLE household_expenses_old")
                database.execSQL("""CREATE TABLE IF NOT EXISTS household_budgets (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,name TEXT NOT NULL,amount REAL NOT NULL,currencyCode TEXT NOT NULL DEFAULT 'LYD',period TEXT NOT NULL DEFAULT 'monthly',startDate INTEGER NOT NULL,endDate INTEGER,isActive INTEGER NOT NULL DEFAULT 1)""")
            }
        }
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE household_members ADD COLUMN remoteId TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE household_expenses ADD COLUMN remoteId TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE household_budgets ADD COLUMN remoteId TEXT NOT NULL DEFAULT ''")
            }
        }

        @Volatile private var INSTANCE: AppDatabase? = null
        private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun getDatabase(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            val appContext = context.applicationContext
            val instance = Room.databaseBuilder(appContext, AppDatabase::class.java, "bye_pro_finance.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                .build()
            INSTANCE = instance
            syncScope.launch {
                runCatching { HouseholdSyncRepository(instance.financeDao(), SupabaseAuthRepository(appContext)).sync() }
            }
            instance
        }
    }
}
