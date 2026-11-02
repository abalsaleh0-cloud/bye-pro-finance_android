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
import com.example.finance.model.SyncEntityMapEntity
import com.example.finance.model.SyncOutboxEntity

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
        SyncEntityMapEntity::class,
        SyncOutboxEntity::class
    ],
    version = 6,
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
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS sync_entity_map (
                        entityType TEXT NOT NULL,
                        localId INTEGER NOT NULL,
                        remoteId TEXT NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        PRIMARY KEY(entityType, localId)
                    )
                """.trimIndent())
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_sync_entity_map_remoteId ON sync_entity_map(remoteId)")
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS sync_outbox (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        entityType TEXT NOT NULL,
                        localId INTEGER NOT NULL,
                        operation TEXT NOT NULL,
                        payload TEXT NOT NULL,
                        state TEXT NOT NULL,
                        attemptCount INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        lastAttemptAt INTEGER,
                        lastError TEXT
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS index_sync_outbox_state_createdAt ON sync_outbox(state, createdAt)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_sync_outbox_entityType_localId ON sync_outbox(entityType, localId)")
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
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
