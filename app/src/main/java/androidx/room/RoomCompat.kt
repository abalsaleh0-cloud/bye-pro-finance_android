package androidx.room

import com.example.finance.data.AppDatabase

/** Source-compatibility shim while legacy repository transaction blocks are remote operations. */
suspend fun <T> AppDatabase.withTransaction(block: suspend () -> T): T = block()
