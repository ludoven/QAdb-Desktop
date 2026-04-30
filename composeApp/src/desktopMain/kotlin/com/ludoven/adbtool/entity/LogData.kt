package com.ludoven.adbtool.entity

data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel = LogLevel.INFO,
    val tag: String = "",
    val message: String = "",
    val pid: Int = 0,
    val tid: Int = 0
)

enum class LogLevel(val displayName: String) {
    VERBOSE("V"),
    DEBUG("D"),
    INFO("I"),
    WARN("W"),
    ERROR("E"),
    FATAL("F")
}

data class LogFilter(
    val level: LogLevel? = null,
    val keyword: String = "",
    val tag: String = "",
    val startTime: Long? = null,
    val endTime: Long? = null
)
