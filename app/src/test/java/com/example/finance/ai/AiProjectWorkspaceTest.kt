package com.example.finance.ai

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AiProjectWorkspaceTest {
    @Test
    fun savesAndRestoresProjectPlansAcrossWorkspaceInstances() {
        val file = File.createTempFile("ai-bye-pro-projects", ".properties")
        file.delete()
        try {
            val original = AiProjectPlan(
                id = "project-1",
                title = "Inventory app",
                objective = "Build an offline-first inventory application",
                technologyStack = "Kotlin\nRoom\nCompose",
                milestones = listOf("Requirements", "UI and data layer", "Testing"),
                tasks = listOf("Define entities", "Build screens", "Run tests"),
                createdAt = 123456L
            )
            AiProjectWorkspace(file).savePlan(original)
            val restored = AiProjectWorkspace(file).getPlan("project-1")
            assertNotNull(restored)
            assertEquals(original, restored)
            assertEquals(listOf(original), AiProjectWorkspace(file).listPlans())
        } finally {
            file.delete()
        }
    }
}
