package com.ludoven.adbtool.util

import java.io.File

object ScrcpyPathManager {
    const val ERROR_BUNDLED_SCRCPY_NOT_FOUND = "bundled scrcpy not found"

    private const val APP_RESOURCES_DIR_PROPERTY = "compose.application.resources.dir"

    fun getScrcpyPath(): String? {
        val candidates = runtimeResourceRoots().flatMap { root ->
            listOfNotNull(
                resolvePackagedScrcpyPath(root),
                resolveBundledScrcpyPath(root)
            )
        }

        return candidates
            .firstOrNull { it.exists() && ensureExecutableIfPossible(it) }
            ?.absolutePath
    }

    fun resolveBundledScrcpyPath(
        resourceRoot: File,
        osName: String = System.getProperty("os.name"),
        osArch: String = System.getProperty("os.arch")
    ): File? {
        val platformDir = platformResourceDir(resourceRoot, osName, osArch) ?: return null
        return File(platformDir, "scrcpy/${executableName(osName)}")
    }

    private fun resolvePackagedScrcpyPath(resourceRoot: File): File {
        return File(resourceRoot, "scrcpy/${executableName()}")
    }

    private fun runtimeResourceRoots(): List<File> {
        val roots = mutableListOf<File>()
        System.getProperty(APP_RESOURCES_DIR_PROPERTY)
            ?.takeIf { it.isNotBlank() }
            ?.let { roots += File(it) }

        roots += File(System.getProperty("user.dir"), "composeApp/src/desktopMain/appResources")
        roots += File(System.getProperty("user.dir"), "src/desktopMain/appResources")
        return roots.distinctBy { it.absolutePath }
    }

    private fun platformResourceDir(
        resourceRoot: File,
        osName: String,
        osArch: String
    ): File? {
        val osId = when {
            osName.equals("Mac OS X", ignoreCase = true) || osName.lowercase().contains("mac") -> "macos"
            osName.startsWith("Win", ignoreCase = true) || osName.lowercase().contains("windows") -> "windows"
            else -> return null
        }
        val archId = when (osArch.lowercase()) {
            "aarch64", "arm64" -> "arm64"
            "x86_64", "amd64" -> "x64"
            else -> return null
        }
        return File(resourceRoot, "$osId-$archId")
    }

    private fun executableName(osName: String = System.getProperty("os.name")): String {
        return if (osName.startsWith("Win", ignoreCase = true) || osName.lowercase().contains("windows")) {
            "scrcpy.exe"
        } else {
            "scrcpy"
        }
    }

    private fun ensureExecutableIfPossible(file: File): Boolean {
        if (executableName().endsWith(".exe")) return true
        if (file.canExecute()) return true
        return runCatching { file.setExecutable(true, false) }.getOrDefault(false) && file.canExecute()
    }
}
