package com.ludoven.adbtool.domain.terminal

data class TerminalSession(
    val id: String,
    val title: String,
    val deviceId: String?,
    val mode: TerminalMode,
    val history: List<String>,
    val isRunning: Boolean,
    val runningCommand: String?,
    val lines: List<TerminalLine>,
    val devices: List<String>,
    val deviceDisplayNames: Map<String, String>
)
