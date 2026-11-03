package com.example.finance.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException

class GgufMetadataInspectorTest {
    @Test
    fun readsGemmaArchitectureAndCoreMetadata() {
        val bytes = createGguf(
            listOf(
                stringEntry("general.architecture", "gemma4"),
                stringEntry("general.name", "Gemma 4 E4B"),
                uintEntry("gemma4.context_length", 8192),
                uintEntry("gemma4.block_count", 34),
                uintEntry("gemma4.embedding_length", 2560),
                uintEntry("general.file_type", 15)
            )
        )

        val result = GgufMetadataInspector.inspect(bytes.inputStream())

        assertEquals(3L, result.version)
        assertEquals(128L, result.tensorCount)
        assertEquals("gemma4", result.architecture)
        assertEquals("Gemma 4 E4B", result.modelName)
        assertEquals(8192L, result.contextLength)
        assertEquals(34L, result.blockCount)
        assertEquals(2560L, result.embeddingLength)
        assertEquals(15L, result.fileType)
    }

    @Test
    fun rejectsNonGgufFiles() {
        val error = assertThrows(IOException::class.java) {
            GgufMetadataInspector.inspect("NOPE".byteInputStream())
        }
        assertEquals(true, error.message?.contains("GGUF") == true)
    }

    private fun createGguf(entries: List<ByteArray>): ByteArray {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { out ->
            out.writeBytes("GGUF")
            out.writeInt(Integer.reverseBytes(3))
            out.writeLong(java.lang.Long.reverseBytes(128L))
            out.writeLong(java.lang.Long.reverseBytes(entries.size.toLong()))
            entries.forEach(out::write)
        }
        return bytes.toByteArray()
    }

    private fun stringEntry(key: String, value: String): ByteArray =
        entry(key, 8) { writeGgufString(value) }

    private fun uintEntry(key: String, value: Int): ByteArray =
        entry(key, 4) { writeInt(Integer.reverseBytes(value)) }

    private fun entry(key: String, type: Int, valueWriter: DataOutputStream.() -> Unit): ByteArray {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { out ->
            out.writeGgufString(key)
            out.writeInt(Integer.reverseBytes(type))
            out.valueWriter()
        }
        return bytes.toByteArray()
    }

    private fun DataOutputStream.writeGgufString(value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        writeLong(java.lang.Long.reverseBytes(bytes.size.toLong()))
        write(bytes)
    }
}
