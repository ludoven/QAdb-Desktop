package com.ludoven.adbtool.pages

data class AppInfo(
    val appName: String,
    val packageName: String,
    val apkPath: String = "",
    val isSystemApp: Boolean = false,
    val isDebuggable: Boolean = false,
    val isDisabled: Boolean = false,
    val versionName: String = "-",
    val installTime: String = "-",
    val size: String = "-",
    val sizeBytes: Long? = null,
    val installTimestamp: Long? = null,
    val lastUsedTimestamp: Long? = null,
    val isRunning: Boolean = false
)

internal enum class AppFilter(val key: String) {
    ALL("all"),
    USER("user"),
    SYSTEM("system"),
    DEBUGGABLE("debug"),
    RECENT("recent"),
    RUNNING("running");

    fun matches(app: AppInfo): Boolean = when (this) {
        ALL -> true
        USER -> !app.isSystemApp
        SYSTEM -> app.isSystemApp
        DEBUGGABLE -> app.isDebuggable
        RECENT -> (app.lastUsedTimestamp ?: app.installTimestamp ?: 0L) > 0L
        RUNNING -> app.isRunning
    }

    companion object {
        fun fromKey(value: String): AppFilter = entries.firstOrNull { it.key == value } ?: when (value) {
            "全部应用" -> ALL
            "用户应用" -> USER
            "系统应用" -> SYSTEM
            "可调试应用" -> DEBUGGABLE
            "最近使用" -> RECENT
            "运行中" -> RUNNING
            else -> ALL
        }
    }
}

internal enum class AppSortMode {
    Name,
    Size,
    Version,
    InstallTime,
    Recent
}

internal enum class PermissionFilter {
    ALL,
    DANGEROUS,
    PRIVACY,
    NORMAL
}

internal enum class AppListEmptyReason {
    NO_DEVICE,
    NO_RESULTS
}
