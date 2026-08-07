package com.ludoven.adbtool.util

import java.util.prefs.Preferences
import java.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class CommandHistoryTask {
    INSTALL_APK,
    UNINSTALL_APP,
    SCREENSHOT
}

data class CommandHistoryRecord(
    val task: CommandHistoryTask,
    val succeeded: Boolean,
    val occurredAtMillis: Long,
    val target: String = ""
)

object CommandHistoryStore {
    private const val HistoryKey = "command_history"
    private const val MaxRecords = 5

    private val preferences = Preferences.userNodeForPackage(CommandHistoryStore::class.java)
    private val mutableRecords = MutableStateFlow(loadRecords())

    val records: StateFlow<List<CommandHistoryRecord>> = mutableRecords.asStateFlow()

    @Synchronized
    fun record(task: CommandHistoryTask, succeeded: Boolean, target: String = "") {
        val updated = listOf(
            CommandHistoryRecord(
                task = task,
                succeeded = succeeded,
                occurredAtMillis = System.currentTimeMillis(),
                target = target.trim()
            )
        ).plus(mutableRecords.value).take(MaxRecords)

        mutableRecords.value = updated
        preferences.put(
            HistoryKey,
            updated.joinToString(";") { record ->
                val base = "${record.task.name},${record.succeeded},${record.occurredAtMillis}"
                if (record.target.isBlank()) base else "$base,${encodeTarget(record.target)}"
            }
        )
        runCatching { preferences.flush() }
    }

    private fun loadRecords(): List<CommandHistoryRecord> = runCatching {
        preferences.get(HistoryKey, "")
            .split(';')
            .mapNotNull { serialized ->
                val parts = serialized.split(',')
                if (parts.size !in 3..4) return@mapNotNull null

                val task = runCatching { CommandHistoryTask.valueOf(parts[0]) }.getOrNull()
                    ?: return@mapNotNull null
                val succeeded = parts[1].toBooleanStrictOrNull() ?: return@mapNotNull null
                val occurredAtMillis = parts[2].toLongOrNull() ?: return@mapNotNull null
                val target = parts.getOrNull(3)?.let(::decodeTarget).orEmpty()
                CommandHistoryRecord(task, succeeded, occurredAtMillis, target)
            }
            .sortedByDescending(CommandHistoryRecord::occurredAtMillis)
            .take(MaxRecords)
    }.getOrDefault(emptyList())

    private fun encodeTarget(target: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(target.toByteArray(Charsets.UTF_8))

    private fun decodeTarget(encoded: String): String = runCatching {
        String(Base64.getUrlDecoder().decode(encoded), Charsets.UTF_8)
    }.getOrDefault("")
}
