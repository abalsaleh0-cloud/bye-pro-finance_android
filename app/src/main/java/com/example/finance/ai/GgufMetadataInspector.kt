package com.example.finance.ai

import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream

/**
 * Bounded GGUF header/metadata reader for model identification.
 * This is deliberately not an inference runtime and does not claim that a
 * model can be loaded until the native llama.cpp backend is integrated.
 */
data class GgufMetadataSummary(
    val version: Long,
    val tensorCount: Long,
    val architecture: String?,
    val modelName: String?,
    val fileType: Long?,
    val quantizationVersion: Long?,
    val contextLength: Long?,
    val blockCount: Long?,
    val embeddingLength: Long?
) {
    fun toDisplayText(): String = buildList {
        add("GGUF v$version · $tensorCount tensors")
        architecture?.let { add("Architecture: $it") }
        modelName?.let { add("Model: $it") }
        fileType?.let { add("GGUF file type code: $it") }
        quantizationVersion?.let { add("Quantization metadata version: $it") }
        contextLength?.let { add("Training context: $it") }
        blockCount?.let { add("Transformer blocks: $it") }
        embeddingLength?.let { add("Embedding length: $it") }
    }.joinToString("\n")
}

object GgufMetadataInspector {
    private const val MAX_METADATA_ENTRIES = 100_000L
    private const val MAX_TENSORS = 10_000_000L
    private const val MAX_STRING_BYTES = 8L * 1024L * 1024L
    private const val MAX_ARRAY_ITEMS = 1_000_000L

    @Throws(IOException::class)
    fun inspect(input: InputStream): GgufMetadataSummary {
        val data = DataInputStream(BufferedInputStream(input))
        val magic = ByteArray(4)
        data.readFully(magic)
        if (magic.decodeToString() != "GGUF") throw IOException("ملف GGUF غير صالح: الترويسة غير مطابقة.")

        val version = data.readUInt32LE()
        if (version !in 2L..3L) throw IOException("إصدار GGUF غير مدعوم: $version")
        val tensorCount = data.readInt64LE()
        val metadataCount = data.readInt64LE()
        if (tensorCount !in 0..MAX_TENSORS) throw IOException("عدد tensors غير منطقي.")
        if (metadataCount !in 0..MAX_METADATA_ENTRIES) throw IOException("عدد metadata entries غير منطقي.")

        val values = HashMap<String, Any?>()
        repeat(metadataCount.toInt()) {
            val key = data.readGgufString()
            val valueType = data.readUInt32LE()
            val keepValue = key in SELECTED_KEYS || key.substringAfterLast('.') in ARCHITECTURE_KEYS
            val value = data.readValue(valueType)
            if (keepValue) values[key] = value
        }

        val architecture = values["general.architecture"] as? String
        fun architectureValue(suffix: String): Long? =
            architecture?.let { values["$it.$suffix"] as? Long }

        return GgufMetadataSummary(
            version = version,
            tensorCount = tensorCount,
            architecture = architecture,
            modelName = values["general.name"] as? String,
            fileType = values["general.file_type"] as? Long,
            quantizationVersion = values["general.quantization_version"] as? Long,
            contextLength = architectureValue("context_length"),
            blockCount = architectureValue("block_count"),
            embeddingLength = architectureValue("embedding_length")
        )
    }

    private fun DataInputStream.readValue(type: Long): Any? = when (type) {
        0L -> readUnsignedByte().toLong()
        1L -> readByte().toLong()
        2L -> readUnsignedShortLE().toLong()
        3L -> readShortLE().toLong()
        4L -> readUInt32LE()
        5L -> readIntLE().toLong()
        6L -> readIntLE().let { Float.fromBits(it) }
        7L -> readUnsignedByte().let { it != 0 }
        8L -> readGgufString()
        9L -> {
            val elementType = readUInt32LE()
            val count = readInt64LE()
            if (count !in 0..MAX_ARRAY_ITEMS) throw IOException("GGUF array length is invalid.")
            repeat(count.toInt()) { readValue(elementType) }
            null
        }
        10L -> readInt64LE().toULong().toLong()
        11L -> readInt64LE()
        12L -> java.lang.Double.longBitsToDouble(readInt64LE())
        else -> throw IOException("GGUF metadata value type is not supported: $type")
    }

    private fun DataInputStream.readGgufString(): String {
        val length = readInt64LE()
        if (length !in 0..MAX_STRING_BYTES) throw IOException("GGUF string length is invalid.")
        val bytes = ByteArray(length.toInt())
        readFully(bytes)
        return bytes.decodeToString(throwOnInvalidSequence = true)
    }

    private fun DataInputStream.readUInt32LE(): Long =
        Integer.toUnsignedLong(Integer.reverseBytes(readInt()))

    private fun DataInputStream.readIntLE(): Int = Integer.reverseBytes(readInt())

    private fun DataInputStream.readInt64LE(): Long = java.lang.Long.reverseBytes(readLong())

    private fun DataInputStream.readUnsignedShortLE(): Int =
        java.lang.Short.toUnsignedInt(java.lang.Short.reverseBytes(readShort()))

    private fun DataInputStream.readShortLE(): Short = java.lang.Short.reverseBytes(readShort())

    private val ARCHITECTURE_KEYS = setOf("context_length", "block_count", "embedding_length")
\n    private val SELECTED_KEYS = setOf(
        "general.architecture",
        "general.name",
        "general.file_type",
        "general.quantization_version"
    )
}
