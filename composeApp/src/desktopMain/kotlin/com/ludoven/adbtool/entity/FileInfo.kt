package com.ludoven.adbtool.entity

data class FileInfo(
    val permissions: String = "",
    val owner: String = "",
    val group: String = "",
    val size: String = "",
    val date: String = "",
    val time: String = "",
    val name: String = "",
    val isDirectory: Boolean = false,
    val isSymlink: Boolean = false,
    val symlinkTarget: String = ""
) {
    val displaySize: String
        get() {
            val bytes = size.toLongOrNull() ?: return size
            return when {
                bytes < 1024 -> "${bytes}B"
                bytes < 1024 * 1024 -> "${bytes / 1024}K"
                bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes / (1024.0 * 1024))}M"
                else -> "${"%.2f".format(bytes / (1024.0 * 1024 * 1024))}G"
            }
        }
}

enum class FileSortBy {
    NAME, SIZE, DATE
}

enum class FileSortOrder {
    ASC, DESC
}
