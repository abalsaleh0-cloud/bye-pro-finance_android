package com.example.finance.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.File

class AiProjectFileWorkspaceTest {
    @Test
    fun writesReadsAndListsProjectFiles() {
        val root = createTempDir(prefix = "ai-project-files")
        try {
            val workspace = AiProjectFileWorkspace(root)
            val entry = workspace.writeFile("project-1", "src/Main.kt", "fun main() = Unit")
            assertEquals("src/Main.kt", entry.path)
            assertEquals("fun main() = Unit", workspace.readFile("project-1", "src/Main.kt"))
            assertEquals(listOf("src/Main.kt"), workspace.listFiles("project-1").map { it.path })
        } finally { root.deleteRecursively() }
    }

    @Test
    fun rejectsTraversalInvalidIdsAndOversizedWrites() {
        val root = createTempDir(prefix = "ai-project-safe")
        try {
            val workspace = AiProjectFileWorkspace(root)
            assertThrows(IllegalArgumentException::class.java) { workspace.writeFile("project-1", "../outside.txt", "no") }
            assertThrows(IllegalArgumentException::class.java) { workspace.writeFile("../bad", "a.txt", "no") }
            assertThrows(IllegalArgumentException::class.java) { workspace.writeFile("project-1", "large.txt", "x".repeat(1024 * 1024 + 1)) }
            assertNull(workspace.readFile("project-1", "missing.txt"))
        } finally { root.deleteRecursively() }
    }
    @Test
    fun writesMultipleProjectFilesAsOneValidatedBatch() {
        val root = createTempDir(prefix = "ai-project-batch")
        try {
            val workspace = AiProjectFileWorkspace(root)
            val entries = workspace.writeFiles(
                "project-2",
                listOf(
                    AiProjectFileWrite("src/Main.kt", "fun main() = Unit"),
                    AiProjectFileWrite("README.md", "# Project")
                )
            )
            assertEquals(listOf("src/Main.kt", "README.md"), entries.map { it.path })
            assertEquals("fun main() = Unit", workspace.readFile("project-2", "src/Main.kt"))
            assertEquals("# Project", workspace.readFile("project-2", "README.md"))
        } finally { root.deleteRecursively() }
    }

    @Test
    fun rejectsInvalidBatchBeforeWritingAnyFile() {
        val root = createTempDir(prefix = "ai-project-batch-invalid")
        try {
            val workspace = AiProjectFileWorkspace(root)
            assertThrows(IllegalArgumentException::class.java) {
                workspace.writeFiles(
                    "project-3",
                    listOf(
                        AiProjectFileWrite("src/One.kt", "one"),
                        AiProjectFileWrite("../outside.txt", "no")
                    )
                )
            }
            assertNull(workspace.readFile("project-3", "src/One.kt"))
            assertThrows(IllegalArgumentException::class.java) {
                workspace.writeFiles(
                    "project-3",
                    listOf(AiProjectFileWrite("same.txt", "one"), AiProjectFileWrite("same.txt", "two"))
                )
            }
        } finally { root.deleteRecursively() }
    }

}