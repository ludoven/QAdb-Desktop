package com.ludoven.adbtool

import com.ludoven.adbtool.util.AdbPathManager
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class AdbPathManagerTest {

    @Test
    fun bundledAdbPathUsesPlatformAndCpuArchitecture() {
        val root = File("/tmp/qadb-test")

        assertEquals(
            File(root, "adb/macos/arm64/adb").absolutePath,
            AdbPathManager.resolveBundledAdbPath(root, "Mac OS X", "aarch64")?.absolutePath
        )
        assertEquals(
            File(root, "adb/macos/x64/adb").absolutePath,
            AdbPathManager.resolveBundledAdbPath(root, "Mac OS X", "x86_64")?.absolutePath
        )
        assertEquals(
            File(root, "adb/windows/adb.exe").absolutePath,
            AdbPathManager.resolveBundledAdbPath(root, "Windows 11", "amd64")?.absolutePath
        )
        assertEquals(
            File(root, "adb/linux/adb").absolutePath,
            AdbPathManager.resolveBundledAdbPath(root, "Linux", "amd64")?.absolutePath
        )
    }

    @Test
    fun friendlyInitializationErrorDoesNotExposeRawCommandFailure() {
        val message = AdbPathManager.friendlyInitializationError("adb: command not found")

        assertEquals("ADB 初始化失败，请检查 QADB 是否有执行权限，或手动选择 adb 路径。", message)
        assertFalse(message.contains("command not found"))
    }
}
