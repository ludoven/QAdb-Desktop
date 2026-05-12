package com.ludoven.adbtool.domain.adb

import com.ludoven.adbtool.util.AdbPathManager

interface AdbPathProvider {
    suspend fun resolveAdbPath(): Result<String>
}

class DefaultAdbPathProvider : AdbPathProvider {
    override suspend fun resolveAdbPath(): Result<String> {
        val path = AdbPathManager.getAdbPath()
        return if (!path.isNullOrBlank()) {
            Result.success(path)
        } else {
            val msg = AdbPathManager.adbEnvironment.value.message
                ?: "未找到 ADB，请在设置中配置 ADB 路径，或启用 QADB 内置 ADB。"
            Result.failure(IllegalStateException(msg))
        }
    }
}
