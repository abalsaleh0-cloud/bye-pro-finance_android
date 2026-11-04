package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.finance.ai.AiAppToolSet
import com.example.finance.ai.AiProjectWorkspace
import com.example.finance.data.AppDatabase
import com.example.finance.data.FinanceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AiAppToolSetTest {

    @Test
    fun structuredManifestExposesAllAnnotatedToolsAndArgumentNames() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repository = FinanceRepository(database.financeDao(), database)
            val tools = AiAppToolSet(
                repository,
                AiProjectWorkspace(File(context.cacheDir, "ai-tools-test.properties"))
            ) { }
            val manifest = tools.structuredToolManifest()
            assertEquals(59, manifest.lines().count { it.isNotBlank() })
            assertTrue(manifest.contains("proposeCreateTransaction(accountId:"))
            assertTrue(manifest.contains("proposeCreateDocument(title:"))
            assertTrue(manifest.contains("proposeClearNotifications()"))
            val exposedMethods = tools.javaClass.methods.filter { method ->
                manifest.lineSequence().any { it.startsWith("${method.name}(") }
            }
            assertEquals(59, exposedMethods.size)
            exposedMethods.forEach { method ->
                val signature = manifest.lineSequence().first { it.startsWith("${method.name}(") }
                val argumentText = signature.substringAfter("(").substringBefore("):").trim()
                val manifestArgumentCount = if (argumentText.isBlank()) 0 else argumentText.split(", ").size
                assertEquals("Argument order/count mismatch for ${method.name}", method.parameterCount, manifestArgumentCount)
            }
        } finally {
            database.close()
        }
    }

    @Test
    fun structuredDispatchCreatesOnlyPreviewAndCannotOverwritePendingAction() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repository = FinanceRepository(database.financeDao(), database)
            val tools = AiAppToolSet(
                repository,
                AiProjectWorkspace(File(context.cacheDir, "ai-tools-dispatch-test.properties"))
            ) { }

            val first = tools.invokeStructuredToolCall(
                "proposeCreateAccount",
                """["Primary wallet","CASH","LYD",0.0]"""
            )
            assertTrue(first["success"] == true)
            assertTrue(first["requires_user_confirmation"] == true)
            val previewId = first["preview_id"] as? String
            assertNotNull(previewId)
            assertTrue(repository.accounts.first().isEmpty())

            val second = tools.invokeStructuredToolCall(
                "proposeCreateBudget",
                """["Monthly budget","Food",100.0,"LYD"]"""
            )
            assertFalse(second["success"] == true)
            assertTrue(repository.accounts.first().isEmpty())
            assertTrue(repository.budgets.first().isEmpty())

            tools.confirmPendingAction(checkNotNull(previewId))
            assertEquals("Primary wallet", repository.accounts.first().single().name)
            assertTrue(repository.budgets.first().isEmpty())
        } finally {
            database.close()
        }
    }

    @Test
    fun documentRegisterMetadataIsPersistedOnlyAfterConfirmation() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repository = FinanceRepository(database.financeDao(), database)
            val tools = AiAppToolSet(
                repository,
                AiProjectWorkspace(File(context.cacheDir, "ai-tools-document-test.properties"))
            ) { }
            val preview = tools.proposeCreateDocument(
                "Purchase receipt", "RECEIPT", "Local shop",
                25.0, 0.0, "LYD", System.currentTimeMillis(), "", 0L
            )
            assertTrue(preview["success"] == true)
            assertTrue(preview["requires_user_confirmation"] == true)
            assertTrue(repository.documents.first().isEmpty())

            tools.confirmPendingAction(checkNotNull(preview["preview_id"] as? String))
            assertEquals("Purchase receipt", repository.documents.first().single().title)
        } finally {
            database.close()
        }
    }

    @Test
    fun cancellingPreviewDoesNotPersistAnyRecord() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repository = FinanceRepository(database.financeDao(), database)
            val tools = AiAppToolSet(
                repository,
                AiProjectWorkspace(File(context.cacheDir, "ai-tools-cancel-test.properties"))
            ) { }
            val result = tools.proposeCreateAccount("Temporary wallet", "CASH", "LYD", 0.0)
            val previewId = result["preview_id"] as String
            tools.cancelPendingAction(previewId)
            assertTrue(repository.accounts.first().isEmpty())
        } finally {
            database.close()
        }
    }
}
