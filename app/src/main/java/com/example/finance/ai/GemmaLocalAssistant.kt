package com.example.finance.ai

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.File

/**
 * Process-scoped owner for the on-device Gemma runtime.
 * Engine and Conversation are created, used and closed behind one mutex.
 */
object GemmaLocalAssistant {
    private val mutex = Mutex()
    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private var loadedPath: String? = null

    suspend fun isAvailable(modelFile: File): Boolean =
        withContext(Dispatchers.IO) { modelFile.isFile && modelFile.length() > 2_300_000_000L }

    suspend fun reply(modelFile: File, prompt: String, imageFile: File? = null): String =
        mutex.withLock {
            withContext(Dispatchers.IO) {
                require(modelFile.isFile && modelFile.length() > 2_300_000_000L) {
                    "ملف Gemma 4 E2B غير موجود أو غير مكتمل."
                }
                if (imageFile != null) require(imageFile.isFile && imageFile.length() > 0L) {
                    "تعذر قراءة الصورة المرفقة."
                }

                if (engine == null || loadedPath != modelFile.absolutePath) {
                    closeInternal()
                    val created = Engine(
                        EngineConfig(
                            modelPath = modelFile.absolutePath,
                            backend = Backend.CPU(),
                            visionBackend = Backend.CPU()
                        )
                    )
                    try {
                        created.initialize()
                    } catch (error: Throwable) {
                        // Do not close an Engine whose initialize() did not complete.
                        throw error
                    }
                    engine = created
                    loadedPath = modelFile.absolutePath
                    conversation = created.createConversation(
                        ConversationConfig(
                            systemInstruction = Contents.of(
                                Content.Text(
                                    "You are BYE PRO FINANCE's private on-device AI assistant. " +
                                    "Speak naturally, clearly and conversationally. Reply in the user's language. " +
                                    "Understand general questions, ask a short clarifying question when uncertain, " +
                                    "and never invent account balances or financial records. You may explain images, " +
                                    "receipts, invoices, documents and handwriting; mark uncertain or unreadable details. " +
                                    "Never execute financial operations."
                                )
                            ),
                            samplerConfig = SamplerConfig(topK = 40, topP = 0.92, temperature = 0.75)
                        )
                    )
                }

                val activeConversation = checkNotNull(conversation)
                val message = if (imageFile == null) {
                    activeConversation.sendMessage(prompt)
                } else {
                    activeConversation.sendMessage(
                        Contents.of(
                            Content.Text(prompt),
                            Content.ImageFile(imageFile.absolutePath)
                        )
                    )
                }
                message.toString().trim().ifBlank { "لم يُرجع النموذج إجابة نصية. أعد صياغة السؤال." }
            }
        }

    suspend fun close() = mutex.withLock { withContext(Dispatchers.IO) { closeInternal() } }

    private fun closeInternal() {
        runCatching { conversation?.close() }
        conversation = null
        runCatching { engine?.close() }
        engine = null
        loadedPath = null
    }
}
