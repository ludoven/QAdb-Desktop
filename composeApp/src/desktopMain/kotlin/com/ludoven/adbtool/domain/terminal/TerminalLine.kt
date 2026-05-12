package com.ludoven.adbtool.domain.terminal

data class TerminalLine(
    val id: String,
    val type: TerminalLineType,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class TerminalLineType {
    PROMPT,
    INPUT,
    OUTPUT,
    ERROR,
    STATUS,
    SUCCESS,
    COMMAND
}
