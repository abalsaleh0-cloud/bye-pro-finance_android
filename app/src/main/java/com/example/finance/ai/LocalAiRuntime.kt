package com.example.finance.ai

import android.app.Application
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.Executors

object LocalAiRuntime {
    private val executor = Executors.newSingleThreadExecutor()
    private val dispatcher = executor.asCoroutineDispatcher()
    private val mutex = Mutex()
    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private var loadedPath: String? = null

    suspend fun ensureLoaded(application: Application, modelPath: String) {
        mutex.withLock {
            withContext(dispatcher) {
                if (loadedPath == modelPath && engine != null && conversation != null) return@withContext
                conversation?.close()
                conversation = null
                engine?.close()
                engine = null
                loadedPath = null

                val candidate = Engine(
                    EngineConfig(
                        modelPath = modelPath,
                        backend = Backend.CPU(),
                        cacheDir = application.cacheDir.absolutePath
                    )
                )
                candidate.initialize()
                engine = candidate
                conversation = candidate.createConversation(
                    ConversationConfig(
                        systemInstruction = Contents.of(
                            "You are BYE PRO FINANCE, a careful bilingual financial assistant. " +
                                "Reply in the user's language. Never invent account balances, transactions, " +
                                "or financial facts. Explain that exact calculations must use verified app data. " +
                                "Do not request passwords, OTPs, or secret keys."
                        )
                    )
                )
                loadedPath = modelPath
            }
        }
    }

    suspend fun ask(prompt: String): String = withContext(dispatcher) {
        val activeConversation = checkNotNull(conversation) { "حمّل النموذج أولاً." }
        val output = StringBuilder()
        activeConversation.sendMessageAsync(prompt).collect { chunk -> output.append(chunk.toString()) }
        output.toString()
    }

    suspend fun isLoaded(): Boolean = withContext(dispatcher) { engine != null && conversation != null }

    suspend fun close() {
        mutex.withLock {
            withContext(dispatcher) {
                conversation?.close()
                conversation = null
                engine?.close()
                engine = null
                loadedPath = null
            }
        }
    }
}
