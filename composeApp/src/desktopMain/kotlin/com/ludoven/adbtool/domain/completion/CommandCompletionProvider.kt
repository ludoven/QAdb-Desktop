package com.ludoven.adbtool.domain.completion

import com.ludoven.adbtool.domain.adb.AdbCommandParser

class CommandCompletionProvider(
    private val parser: AdbCommandParser = AdbCommandParser()
) {
    fun complete(input: String): List<String> {
        val prefix = input.trim().lowercase()
        if (prefix.isBlank()) return parser.commandTemplates()
        return parser.commandTemplates().filter { it.lowercase().startsWith(prefix) }
    }
}
