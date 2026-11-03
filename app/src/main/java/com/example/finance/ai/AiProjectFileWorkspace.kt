package com.example.finance.ai

import java.io.File
import java.io.IOException

data class AiProjectFileEntry(val path: String, val sizeBytes: Long, val modifiedAt: Long)
data class AiProjectFileWrite(val path: String, val content: String)

/** App-private project files. All writes are confirmation-gated by AiAppToolSet. */
class AiProjectFileWorkspace(private val rootDirectory: File) {
    fun listFiles(projectId: String): List<AiProjectFileEntry> {
        val directory = projectDirectory(projectId)
        if (!directory.isDirectory) return emptyList()
        return directory.walkTopDown()
            .filter { it.isFile && !it.name.startsWith(".tmp-") }
            .take(MAX_FILES)
            .map { file -> AiProjectFileEntry(file.relativeTo(directory).invariantSeparatorsPath, file.length(), file.lastModified()) }
            .toList()
    }

    fun readFile(projectId: String, relativePath: String): String? {
        val file = resolveFile(projectId, relativePath)
        if (!file.isFile) return null
        require(file.length() <= MAX_FILE_BYTES) { "Project file exceeds the 1 MiB read limit." }
        return file.readText(Charsets.UTF_8)
    }

    fun writeFile(projectId: String, relativePath: String, content: String): AiProjectFileEntry {
        val bytes = content.toByteArray(Charsets.UTF_8)
        require(bytes.size <= MAX_FILE_BYTES) { "Project file exceeds the 1 MiB write limit." }
        val file = resolveFile(projectId, relativePath)
        require(!file.exists() || file.isFile) { "Project path is not a regular file." }
        file.parentFile?.mkdirs()
        val temporary = File(file.parentFile, ".tmp-" + file.name + "-" + System.nanoTime())
        try {
            temporary.writeBytes(bytes)
            if (!temporary.renameTo(file)) throw IOException("Could not atomically replace the project file.")
        } finally {
            if (temporary.exists()) temporary.delete()
        }
        return AiProjectFileEntry(file.relativeTo(projectDirectory(projectId)).invariantSeparatorsPath, file.length(), file.lastModified())
    }

    fun validateWrites(projectId: String, writes: List<AiProjectFileWrite>) {
        require(writes.isNotEmpty() && writes.size <= MAX_BATCH_FILES) { "A batch must contain 1 to 20 files." }
        val paths = writes.map { it.path.trim() }
        require(paths.distinct().size == paths.size) { "Duplicate paths are not allowed in one batch." }
        val prepared = writes.mapIndexed { index, write ->
            val bytes = write.content.toByteArray(Charsets.UTF_8)
            require(bytes.size <= MAX_FILE_BYTES) { "Project file exceeds the 1 MiB write limit: ${write.path}" }
            Triple(resolveFile(projectId, paths[index]), bytes, paths[index])
        }
        require(prepared.sumOf { it.second.size.toLong() } <= MAX_BATCH_BYTES) {
            "The total project batch exceeds the 5 MiB limit."
        }
        prepared.forEach { (file, _, path) ->
            require(!file.exists() || file.isFile) { "Project path is not a regular file: $path" }
        }
    }

    /** Validate the full batch before changing anything; restore prior files if a later write fails. */
    fun writeFiles(projectId: String, writes: List<AiProjectFileWrite>): List<AiProjectFileEntry> {
        require(writes.isNotEmpty() && writes.size <= MAX_BATCH_FILES) { "A batch must contain 1 to 20 files." }
        val normalizedPaths = writes.map { it.path.trim() }
        require(normalizedPaths.distinct().size == normalizedPaths.size) { "Duplicate paths are not allowed in one batch." }
        val prepared = writes.mapIndexed { index, write ->
            val bytes = write.content.toByteArray(Charsets.UTF_8)
            require(bytes.size <= MAX_FILE_BYTES) { "Project file exceeds the 1 MiB write limit: ${write.path}" }
            Triple(resolveFile(projectId, normalizedPaths[index]), bytes, normalizedPaths[index])
        }
        require(prepared.sumOf { it.second.size.toLong() } <= MAX_BATCH_BYTES) {
            "The total project batch exceeds the 5 MiB limit."
        }
        prepared.forEach { (file, _, path) ->
            require(!file.exists() || file.isFile) { "Project path is not a regular file: $path" }
        }
        val previous = prepared.associate { (file, _, path) ->
            path to if (file.isFile) file.readBytes() else null
        }
        val completed = mutableListOf<String>()
        try {
            return prepared.map { (_, bytes, path) ->
                writeFile(projectId, path, String(bytes, Charsets.UTF_8)).also { completed += path }
            }
        } catch (error: Throwable) {
            completed.asReversed().forEach { path ->
                runCatching {
                    val old = previous[path]
                    if (old == null) resolveFile(projectId, path).delete()
                    else writeFile(projectId, path, String(old, Charsets.UTF_8))
                }
            }
            throw error
        }
    }

    private fun projectDirectory(projectId: String): File {
        require(projectId.matches(Regex("[A-Za-z0-9_-]{1,80}"))) { "Invalid project identifier." }
        val root = rootDirectory.canonicalFile
        val directory = File(root, projectId).canonicalFile
        require(directory.path.startsWith(root.path + File.separator)) { "Project directory escapes private storage." }
        return directory
    }

    private fun resolveFile(projectId: String, relativePath: String): File {
        val directory = projectDirectory(projectId)
        val path = relativePath.trim()
        require(path.isNotBlank() && path.length <= 240 && !path.startsWith("/") && !path.contains(92.toChar())) {
            "Absolute or invalid project paths are not allowed."
        }
        val segments = path.split("/")
        require(segments.all { it.isNotBlank() && it != "." && it != ".." }) { "Path traversal is not allowed." }
        val file = File(directory, segments.joinToString(File.separator)).canonicalFile
        require(file.path.startsWith(directory.path + File.separator)) { "Project file escapes its project directory." }
        return file
    }

    private companion object {
        const val MAX_FILE_BYTES = 1024L * 1024L
        const val MAX_FILES = 200
        const val MAX_BATCH_FILES = 20
        const val MAX_BATCH_BYTES = 5L * 1024L * 1024L
    }
}