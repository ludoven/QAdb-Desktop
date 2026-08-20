package com.ludoven.adbtool.agent

import java.io.File

/** Shared private storage used by Agent logs, page cache and packaged input helpers. */
object AgentDataPaths {
    fun memoryDatabase(): File = File(agentDataDirectory(), "agent-memory.db")

    fun agentDataDirectory(): File {
        val userHome = System.getProperty("user.home")
        val osName = System.getProperty("os.name").lowercase()
        val base = when {
            osName.contains("mac") -> File(userHome, "Library/Application Support/QADB")
            osName.contains("windows") -> File(
                System.getenv("APPDATA")?.takeIf(String::isNotBlank) ?: userHome,
                "QADB"
            )
            else -> File(
                System.getenv("XDG_DATA_HOME")?.takeIf(String::isNotBlank)
                    ?: File(userHome, ".local/share").absolutePath,
                "qadb"
            )
        }
        return File(base, "agent").also {
            it.mkdirs()
            restrictToOwner(it)
        }
    }

    private fun restrictToOwner(file: File) {
        if (!file.exists()) return
        runCatching {
            file.setReadable(false, false)
            file.setWritable(false, false)
            file.setExecutable(false, false)
            file.setReadable(true, true)
            file.setWritable(true, true)
            if (file.isDirectory) file.setExecutable(true, true)
        }
    }
}

/** Legacy response fields retained until the old provider response parser is removed. */
enum class MemoryKind { USER_PREFERENCE, APP_ALIAS, DEVICE_FACT, VERIFIED_PROCEDURE, TASK_SUMMARY }

/** Legacy response fields retained until the old provider response parser is removed. */
enum class MemoryScopeType { GLOBAL, DEVICE, APP }

internal fun estimateTokens(vararg values: Any): Int {
    var estimated = 0.0
    values.forEach { value ->
        val text = when (value) {
            is String -> value
            is Iterable<*> -> value.joinToString("\n")
            else -> value.toString()
        }
        text.forEach { char ->
            estimated += when {
                char.isWhitespace() -> 0.1
                char.code in 0x2E80..0x9FFF -> 1.0
                char.isLetterOrDigit() -> 0.25
                else -> 0.5
            }
        }
    }
    return estimated.toInt().coerceAtLeast(1)
}
