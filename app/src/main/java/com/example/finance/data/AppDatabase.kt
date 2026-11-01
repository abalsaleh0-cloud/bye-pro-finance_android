package com.example.finance.data

import android.content.Context

/** Compatibility facade retained for existing repository transaction boundaries; no local database exists. */
class AppDatabase private constructor(context: Context) {
    private val dao = FinanceDao(context.applicationContext)
    fun financeDao(): FinanceDao = dao
    suspend fun <T> withTransaction(block: suspend () -> T): T = block()
    companion object { @Volatile private var instance: AppDatabase? = null; fun getDatabase(context: Context): AppDatabase = instance ?: synchronized(this) { instance ?: AppDatabase(context).also { instance = it } } }
}
