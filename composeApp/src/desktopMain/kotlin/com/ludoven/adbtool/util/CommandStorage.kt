package com.ludoven.adbtool.util

import java.io.File
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

// ──────────────────────────────────────────────
// Custom Command data class
// ──────────────────────────────────────────────

@Serializable
data class CustomCommand(
    val id: String = "custom_${UUID.randomUUID()}",
    val title: String,
    val description: String = "",
    val commandPreview: String,
    val categoryKey: String = "device",
    val shellCommand: String = ""
)

// ──────────────────────────────────────────────
// CommandFavoritesManager
// ──────────────────────────────────────────────

object CommandFavoritesManager {

    private val file = File(System.getProperty("user.home"), ".qadb_favorites.json")

    @Volatile
    private var cached: Set<String> = emptySet()

    fun load(): Set<String> = synchronized(this) {
        if (!file.exists()) return emptySet<String>().also { cached = it }
        runCatching {
            val arr = json.parseToJsonElement(file.readText()).jsonArray
            cached = arr.map { it.jsonPrimitive.content }.toSet()
        }.getOrElse { emptySet<String>().also { cached = it } }
        cached
    }

    private fun save(ids: Set<String>): Set<String> = synchronized(this) {
        cached = ids
        runCatching {
            file.parentFile?.mkdirs()
            file.writeText(buildJsonArray { ids.forEach { add(JsonPrimitive(it)) } }.toString())
        }
        cached
    }

    fun toggle(id: String): Set<String> {
        val current = if (cached.isEmpty() && file.exists()) load() else cached
        val updated = if (id in current) current - id else current + id
        return save(updated)
    }

    fun isFavorite(id: String): Boolean = id in cached
}

// ──────────────────────────────────────────────
// CustomCommandManager
// ──────────────────────────────────────────────

object CustomCommandManager {

    private val file = File(System.getProperty("user.home"), ".qadb_custom_commands.json")

    @Volatile
    private var cached: List<CustomCommand> = emptyList()

    fun load(): List<CustomCommand> = synchronized(this) {
        if (!file.exists()) return emptyList<CustomCommand>().also { cached = it }
        runCatching {
            val arr = json.parseToJsonElement(file.readText()).jsonArray
            cached = arr.map { el ->
                val o = el.jsonObject
                CustomCommand(
                    id = o["id"]?.jsonPrimitive?.content ?: "custom_${UUID.randomUUID()}",
                    title = o["title"]?.jsonPrimitive?.content ?: "",
                    description = o["description"]?.jsonPrimitive?.content ?: "",
                    commandPreview = o["commandPreview"]?.jsonPrimitive?.content ?: "",
                    categoryKey = o["categoryKey"]?.jsonPrimitive?.content ?: "device",
                    shellCommand = o["shellCommand"]?.jsonPrimitive?.content ?: ""
                )
            }
        }.getOrElse { emptyList<CustomCommand>().also { cached = it } }
        cached
    }

    private fun save(commands: List<CustomCommand>): List<CustomCommand> = synchronized(this) {
        cached = commands
        runCatching {
            file.parentFile?.mkdirs()
            val arr = buildJsonArray {
                commands.forEach { cmd ->
                    add(buildJsonObject {
                        put("id", JsonPrimitive(cmd.id))
                        put("title", JsonPrimitive(cmd.title))
                        put("description", JsonPrimitive(cmd.description))
                        put("commandPreview", JsonPrimitive(cmd.commandPreview))
                        put("categoryKey", JsonPrimitive(cmd.categoryKey))
                        put("shellCommand", JsonPrimitive(cmd.shellCommand))
                    })
                }
            }
            file.writeText(arr.toString())
        }
        cached
    }

    fun add(command: CustomCommand): List<CustomCommand> {
        val current = if (cached.isEmpty() && file.exists()) load() else cached
        return save(current + command)
    }

    fun remove(id: String): List<CustomCommand> {
        val current = if (cached.isEmpty() && file.exists()) load() else cached
        return save(current.filter { it.id != id })
    }

    fun update(command: CustomCommand): List<CustomCommand> {
        val current = if (cached.isEmpty() && file.exists()) load() else cached
        return save(current.map { if (it.id == command.id) command else it })
    }
}
