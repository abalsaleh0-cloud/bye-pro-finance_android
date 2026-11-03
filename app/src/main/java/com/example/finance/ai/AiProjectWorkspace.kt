package com.example.finance.ai

import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Properties

data class AiProjectPlan(
    val id: String,
    val title: String,
    val objective: String,
    val technologyStack: String,
    val milestones: List<String>,
    val tasks: List<String>,
    val createdAt: Long
)

/** Private, offline-first project-plan storage in app-private files. */
class AiProjectWorkspace(private val storageFile: File) {
    @Synchronized
    fun savePlan(plan: AiProjectPlan) {
        require(plan.id.isNotBlank())
        val properties = loadProperties()
        val prefix = "project.${plan.id}."
        properties.setProperty(prefix + "title", encode(plan.title))
        properties.setProperty(prefix + "objective", encode(plan.objective))
        properties.setProperty(prefix + "technologyStack", encode(plan.technologyStack))
        properties.setProperty(prefix + "milestones", encode(plan.milestones.joinToString("\n")))
        properties.setProperty(prefix + "tasks", encode(plan.tasks.joinToString("\n")))
        properties.setProperty(prefix + "createdAt", plan.createdAt.toString())
        storageFile.parentFile?.mkdirs()
        storageFile.outputStream().use { properties.store(it, "AI Bye Pro project workspace") }
    }

    @Synchronized
    fun listPlans(): List<AiProjectPlan> {
        val properties = loadProperties()
        return properties.stringPropertyNames()
            .filter { it.startsWith("project.") && it.endsWith(".title") }
            .mapNotNull { titleKey ->
                val id = titleKey.removePrefix("project.").removeSuffix(".title")
                readPlan(properties, id)
            }
            .sortedByDescending { it.createdAt }
    }

    @Synchronized
    fun getPlan(id: String): AiProjectPlan? = readPlan(loadProperties(), id)

    private fun readPlan(properties: Properties, id: String): AiProjectPlan? {
        val prefix = "project.$id."
        val encodedTitle = properties.getProperty(prefix + "title") ?: return null
        fun decoded(key: String): String = decode(properties.getProperty(prefix + key).orEmpty())
        return AiProjectPlan(
            id = id,
            title = decode(encodedTitle),
            objective = decoded("objective"),
            technologyStack = decoded("technologyStack"),
            milestones = decoded("milestones").lines().filter(String::isNotBlank),
            tasks = decoded("tasks").lines().filter(String::isNotBlank),
            createdAt = properties.getProperty(prefix + "createdAt")?.toLongOrNull() ?: 0L
        )
    }

    private fun loadProperties(): Properties = Properties().also { properties ->
        if (storageFile.isFile) storageFile.inputStream().use(properties::load)
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")
    private fun decode(value: String): String = URLDecoder.decode(value, "UTF-8")
}
