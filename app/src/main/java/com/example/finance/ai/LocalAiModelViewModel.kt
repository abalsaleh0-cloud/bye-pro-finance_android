package com.example.finance.ai

import android.app.Application
import android.os.StatFs
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import kotlinx.coroutines.Dispatchers
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkInfo
import androidx.lifecycle.Observer
import androidx.work.Constraints
import androidx.work.NetworkType
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.Executors
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.ensureActive

private const val MODEL_REPOSITORY = "litert-community/gemma-4-E2B-it-litert-lm"
private const val MODEL_FILE_NAME = "gemma-4-E2B-it.litertlm"
private const val MODEL_URL = "https://huggingface.co/$MODEL_REPOSITORY/resolve/main/$MODEL_FILE_NAME"
private const val MODEL_SHA256 = "181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c"
private const val MODEL_MIN_BYTES = 2_500_000_000L
private const val MODEL_MAX_BYTES = 2_700_000_000L
private const val MIN_FREE_BYTES = 3_200_000_000L

data class LocalAiModelState(
    val installed: Boolean = false,
    val downloading: Boolean = false,
    val loading: Boolean = false,
    val loaded: Boolean = false,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = -1L,
    val error: String? = null,
    val status: String = "النموذج غير مُنزّل",
    val testReply: String = ""
) {
    val progress: Float
        get() = if (totalBytes > 0L) (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
}

object LocalAiModelManager {
    fun modelFile(application: Application): File =
        File(File(application.filesDir, "ai-models"), MODEL_FILE_NAME)

    fun partialFile(application: Application): File =
        File(File(application.filesDir, "ai-models"), "$MODEL_FILE_NAME.part")

    fun expectedSha256(): String = MODEL_SHA256

    fun verifyModel(file: File): Boolean {
        if (!file.isFile || file.length() !in MODEL_MIN_BYTES..MODEL_MAX_BYTES) return false
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(1024 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) } == MODEL_SHA256
    }

    suspend fun download(
        application: Application,
        onProgress: (Long, Long) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val downloadContext = coroutineContext
        val modelDir = File(application.filesDir, "ai-models").apply { mkdirs() }
        val available = StatFs(application.filesDir.absolutePath).availableBytes
        require(available >= MIN_FREE_BYTES) {
            "المساحة الحرة غير كافية. يلزم توفير 3.2 GB على الأقل قبل التنزيل."
        }

        val partial = File(modelDir, "$MODEL_FILE_NAME.part")
        val destination = File(modelDir, MODEL_FILE_NAME)
        if (partial.isFile && partial.length() in MODEL_MIN_BYTES..MODEL_MAX_BYTES && verifyModel(partial)) {
            if (destination.exists()) destination.delete()
            check(partial.renameTo(destination)) { "تعذر تثبيت ملف النموذج في مساحة التطبيق." }
            return@withContext destination
        }
        if (partial.exists() && partial.length() >= MODEL_MAX_BYTES) partial.delete()
        val offset = partial.takeIf { it.exists() }?.length() ?: 0L
        val requestBuilder = Request.Builder().url(MODEL_URL)
        if (offset > 0L) requestBuilder.header("Range", "bytes=$offset-")

        val client = OkHttpClient.Builder().retryOnConnectionFailure(true).build()
        client.newCall(requestBuilder.build()).execute().use { response ->
            require(response.isSuccessful) { "فشل تنزيل النموذج (HTTP ${response.code})." }
            val body = requireNotNull(response.body) { "استجابة التنزيل فارغة." }
            val append = offset > 0L && response.code == 206
            if (offset > 0L && response.code == 200) partial.delete()
            val startBytes = if (append) offset else 0L
            val responseLength = body.contentLength()
            val total = if (responseLength > 0L) startBytes + responseLength else -1L
            var downloaded = startBytes
            onProgress(downloaded, total)

            body.byteStream().use { input ->
                FileOutputStream(partial, append).buffered(1024 * 1024).use { output ->
                    val buffer = ByteArray(1024 * 1024)
                    while (true) {
                        downloadContext.ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        downloaded += count
                        onProgress(downloaded, total)
                    }
                    output.flush()
                }
            }
        }

        require(partial.length() in MODEL_MIN_BYTES..MODEL_MAX_BYTES) {
            "حجم النموذج الذي تم تنزيله غير مطابق للحجم المتوقع."
        }
        require(verifyModel(partial)) {
            partial.delete()
            "فشل التحقق من سلامة النموذج (SHA-256). أُزيل الملف غير السليم."
        }
        if (destination.exists()) destination.delete()
        check(partial.renameTo(destination)) { "تعذر تثبيت ملف النموذج في مساحة التطبيق." }
        destination
    }
}

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
        activeConversation.sendMessageAsync(prompt).collect { chunk ->
            output.append(chunk.toString())
        }
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

class LocalAiModelViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(LocalAiModelState())
    val state: StateFlow<LocalAiModelState> = _state.asStateFlow()

    init { refresh(); observeBackgroundDownload() }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            val file = LocalAiModelManager.modelFile(getApplication())
            val installed = file.isFile && file.length() in MODEL_MIN_BYTES..MODEL_MAX_BYTES
            val partial = LocalAiModelManager.partialFile(getApplication())
            _state.value = _state.value.copy(
                installed = installed,
                downloadedBytes = if (installed) file.length() else partial.takeIf { it.exists() }?.length() ?: 0L,
                totalBytes = -1L,
                status = if (installed) "النموذج مُنزّل وجاهز للتهيئة" else "النموذج غير مُنزّل"
            )
            if (installed) _state.value = _state.value.copy(loaded = LocalAiRuntime.isLoaded())
        }
    }

    fun startDownload() {
        if (_state.value.downloading) return
        val request = OneTimeWorkRequestBuilder<LocalAiModelDownloadWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        _state.value = _state.value.copy(downloading = true, error = null, status = "جارٍ بدء التنزيل في الخلفية")
        WorkManager.getInstance(getApplication()).enqueueUniqueWork(
            LocalAiModelDownloadWorker.UNIQUE_WORK_NAME, ExistingWorkPolicy.KEEP, request
        )
    }

    private val workInfoObserver = Observer<List<WorkInfo>> { works ->
        val work = works.firstOrNull()
        if (work != null) {
            val downloaded = work.progress.getLong(LocalAiModelDownloadWorker.KEY_DOWNLOADED, 0L)
            val total = work.progress.getLong(LocalAiModelDownloadWorker.KEY_TOTAL, -1L)
            when (work.state) {
                WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING -> _state.value = _state.value.copy(
                    downloading = true, downloadedBytes = downloaded, totalBytes = total,
                    status = "جارٍ تنزيل النموذج في الخلفية", error = null
                )
                WorkInfo.State.SUCCEEDED -> refresh()
                WorkInfo.State.FAILED -> _state.value = _state.value.copy(
                    downloading = false, error = work.outputData.getString(LocalAiModelDownloadWorker.KEY_ERROR),
                    status = "تعذر إكمال التنزيل؛ يمكنك المحاولة مجدداً"
                )
                WorkInfo.State.CANCELLED -> _state.value = _state.value.copy(
                    downloading = false, status = "أُوقف التنزيل مؤقتاً؛ يمكنك استكماله لاحقاً"
                )
                else -> Unit
            }
        }
    }

    private fun observeBackgroundDownload() {
        WorkManager.getInstance(getApplication())
            .getWorkInfosForUniqueWorkLiveData(LocalAiModelDownloadWorker.UNIQUE_WORK_NAME)
            .observeForever(workInfoObserver)
    }

    override fun onCleared() {
        WorkManager.getInstance(getApplication())
            .getWorkInfosForUniqueWorkLiveData(LocalAiModelDownloadWorker.UNIQUE_WORK_NAME)
            .removeObserver(workInfoObserver)
        super.onCleared()
    }

    fun pauseDownload() {
        WorkManager.getInstance(getApplication()).cancelUniqueWork(LocalAiModelDownloadWorker.UNIQUE_WORK_NAME)
    }

    fun loadModel() {
        if (_state.value.loading || !_state.value.installed) return
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(loading = true, error = null, status = "جارٍ تهيئة النموذج على الهاتف…")
            runCatching {
                val file = LocalAiModelManager.modelFile(getApplication())
                require(LocalAiModelManager.verifyModel(file)) { "ملف النموذج غير سليم. أعد تنزيله." }
                LocalAiRuntime.ensureLoaded(getApplication(), file.absolutePath)
            }.onSuccess {
                _state.value = _state.value.copy(loading = false, loaded = true, status = "النموذج يعمل محلياً على هذا الهاتف")
            }.onFailure { error ->
                _state.value = _state.value.copy(loading = false, loaded = false, error = error.message, status = "تعذر تشغيل النموذج على هذا الهاتف")
            }
        }
    }

    fun runTest() {
        if (!_state.value.loaded) return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { LocalAiRuntime.ask("مرحباً. عرّف بنفسك باختصار باللغة العربية.") }
                .onSuccess { _state.value = _state.value.copy(testReply = it) }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }

    fun deleteModel() {
        viewModelScope.launch(Dispatchers.IO) {
            WorkManager.getInstance(getApplication()).cancelUniqueWork(LocalAiModelDownloadWorker.UNIQUE_WORK_NAME).result.get()
            runCatching { LocalAiRuntime.close() }
            LocalAiModelManager.modelFile(getApplication()).delete()
            LocalAiModelManager.partialFile(getApplication()).delete()
            _state.value = LocalAiModelState(status = "تم حذف النموذج من مساحة التطبيق")
        }
    }
}
