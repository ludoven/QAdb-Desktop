package com.ludoven.adbtool

import com.ludoven.adbtool.entity.DeviceMirrorSettings
import com.ludoven.adbtool.util.AdbTool
import com.ludoven.adbtool.util.ScrcpyPathManager
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeviceMirrorTest {

    @Test
    fun `build scrcpy command targets selected device by serial`() {
        val command = AdbTool.buildScrcpyCommand(
            scrcpyPath = "/opt/homebrew/bin/scrcpy",
            deviceId = "emulator-5554",
            windowTitle = "QADB Device Mirror"
        )

        assertEquals(
            listOf(
                "/opt/homebrew/bin/scrcpy",
                "--serial",
                "emulator-5554",
                "--window-title",
                "QADB Device Mirror",
                "--always-on-top",
                "--max-size",
                "1280",
                "--max-fps",
                "60",
                "--video-bit-rate",
                "8M",
                "--stay-awake"
            ),
            command
        )
    }

    @Test
    fun `build scrcpy command includes runtime mirror settings`() {
        val command = AdbTool.buildScrcpyCommand(
            scrcpyPath = "/opt/homebrew/bin/scrcpy",
            deviceId = "emulator-5554",
            windowTitle = "QADB Device Mirror",
            settings = DeviceMirrorSettings(
                alwaysOnTop = true,
                fullscreen = true,
                borderless = true,
                maxSize = 1280,
                maxFps = 60,
                videoBitRate = "12M",
                audioEnabled = false,
                showTouches = true,
                stayAwake = true,
                turnScreenOffOnStart = true,
                powerOffOnClose = true
            )
        )

        assertEquals(
            listOf(
                "/opt/homebrew/bin/scrcpy",
                "--serial", "emulator-5554",
                "--window-title", "QADB Device Mirror",
                "--always-on-top",
                "--fullscreen",
                "--window-borderless",
                "--max-size", "1280",
                "--max-fps", "60",
                "--video-bit-rate", "12M",
                "--no-audio",
                "--show-touches",
                "--stay-awake",
                "--turn-screen-off",
                "--power-off-on-close"
            ),
            command
        )
    }

    @Test
    fun `bundled scrcpy path should resolve macOS arm64 resource`() {
        val root = File("/tmp/qadb-app-resources")

        assertEquals(
            File(root, "macos-arm64/scrcpy/scrcpy").absolutePath,
            ScrcpyPathManager.resolveBundledScrcpyPath(root, "Mac OS X", "aarch64")?.absolutePath
        )
    }

    @Test
    fun `bundled scrcpy path should resolve macOS x64 resource`() {
        val root = File("/tmp/qadb-app-resources")

        assertEquals(
            File(root, "macos-x64/scrcpy/scrcpy").absolutePath,
            ScrcpyPathManager.resolveBundledScrcpyPath(root, "Mac OS X", "x86_64")?.absolutePath
        )
    }

    @Test
    fun `bundled scrcpy path should resolve Windows x64 resource`() {
        val root = File("/tmp/qadb-app-resources")

        assertEquals(
            File(root, "windows-x64/scrcpy/scrcpy.exe").absolutePath,
            ScrcpyPathManager.resolveBundledScrcpyPath(root, "Windows 11", "amd64")?.absolutePath
        )
    }

    @Test
    fun `bundled scrcpy path should resolve Linux x64 resource`() {
        val root = File("/tmp/qadb-app-resources")

        assertEquals(
            File(root, "linux-x64/scrcpy/scrcpy").absolutePath,
            ScrcpyPathManager.resolveBundledScrcpyPath(root, "Linux", "amd64")?.absolutePath
        )
    }

    @Test
    fun `system scrcpy candidates should include common Linux paths`() {
        val home = File("/home/qadb")

        assertEquals(
            listOf(
                "/custom/bin/scrcpy",
                "/usr/bin/scrcpy",
                "/usr/local/bin/scrcpy",
                "/snap/bin/scrcpy",
                "/home/qadb/.local/bin/scrcpy"
            ),
            ScrcpyPathManager.buildSystemScrcpyCandidates(
                userHome = home,
                environment = mapOf("SCRCPY_PATH" to "/custom/bin/scrcpy"),
                osName = "Linux"
            )
        )
    }

    @Test
    fun `early process exit should be reported as startup failure`() {
        val process = startFailingProcess()

        val result = AdbTool.detectEarlyProcessExit(process, startupCheckTimeoutMillis = 1000)

        assertNotNull(result)
        assertFalse(result.success)
        assertTrue(result.errorMessage?.isNotBlank() == true)
    }

    @Test
    fun `running process should not be treated as startup failure`() {
        val process = startLongRunningProcess()
        try {
            val result = AdbTool.detectEarlyProcessExit(process, startupCheckTimeoutMillis = 100)
            assertNull(result)
        } finally {
            AdbTool.stopMirrorProcess(process, timeoutMillis = 500)
        }
    }

    @Test
    fun `stop mirror process should terminate alive process`() {
        val process = startLongRunningProcess()

        val stopped = AdbTool.stopMirrorProcess(process, timeoutMillis = 500)

        assertTrue(stopped)
        assertFalse(process.isAlive)
    }

    private fun startFailingProcess(): Process {
        val command = if (isWindows()) {
            listOf("cmd", "/c", "(echo fail) & exit /b 1")
        } else {
            listOf("sh", "-c", "echo fail && exit 1")
        }
        return ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()
    }

    private fun startLongRunningProcess(): Process {
        val command = if (isWindows()) {
            listOf("cmd", "/c", "ping -n 6 127.0.0.1 >NUL")
        } else {
            listOf("sh", "-c", "sleep 5")
        }
        return ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()
    }

    private fun isWindows(): Boolean {
        return System.getProperty("os.name").lowercase().contains("windows")
    }
}
