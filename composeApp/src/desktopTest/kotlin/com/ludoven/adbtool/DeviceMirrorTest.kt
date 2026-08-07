package com.ludoven.adbtool

import com.ludoven.adbtool.entity.DeviceMirrorSettings
import com.ludoven.adbtool.util.AdbTool
import com.ludoven.adbtool.util.ScrcpyPathManager
import com.ludoven.adbtool.viewmodel.isUnexpectedMirrorExit
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeviceMirrorTest {

    @Test
    fun `mirror settings should not be always on top by default`() {
        assertFalse(DeviceMirrorSettings().alwaysOnTop)
    }

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
    fun `build scrcpy tcpip command targets a wireless endpoint`() {
        assertEquals(
            listOf(
                "/opt/homebrew/bin/scrcpy",
                "--tcpip=192.168.198.157:43253",
                "--force-adb-forward",
                "--window-title",
                "QADB Device Mirror",
                "--max-size",
                "1280",
                "--max-fps",
                "60",
                "--video-bit-rate",
                "8M",
                "--stay-awake"
            ),
            AdbTool.buildScrcpyTcpIpCommand(
                scrcpyPath = "/opt/homebrew/bin/scrcpy",
                endpoint = "192.168.198.157:43253",
                windowTitle = "QADB Device Mirror"
            )
        )
    }

    @Test
    fun `wireless endpoint should use the route source and TLS port`() {
        assertEquals(
            "192.168.198.157:43253",
            AdbTool.parseScrcpyTcpIpEndpoint(
                routeOutput = "192.168.198.0/24 dev wlan0 proto kernel scope link src 192.168.198.157",
                tlsPortOutput = "43253\n\n"
            )
        )
    }

    @Test
    fun `wireless endpoint should resolve an mdns service name containing spaces`() {
        val mdnsOutput = """
            List of discovered mdns services
            adb-0123456789ABCDEF-RY0GTI (2)    _adb-tls-connect._tcp    192.168.198.157:42419
            adb-0123456789ABCDEF-RY0GTI        _adb-tls-connect._tcp    192.168.198.157:35103
        """.trimIndent()

        assertEquals(
            "192.168.198.157:42419",
            AdbTool.parseScrcpyMdnsEndpoint(
                mdnsOutput,
                "adb-0123456789ABCDEF-RY0GTI (2)._adb-tls-connect._tcp"
            )
        )
    }

    @Test
    fun `wireless endpoint should reject another mdns instance or invalid port`() {
        assertNull(
            AdbTool.parseScrcpyMdnsEndpoint(
                "adb-device    _adb-tls-connect._tcp    192.168.198.157:70000",
                "adb-device._adb-tls-connect._tcp"
            )
        )
        assertNull(
            AdbTool.parseScrcpyMdnsEndpoint(
                "adb-device    _adb-tls-connect._tcp    192.168.198.157:42419",
                "another-device._adb-tls-connect._tcp"
            )
        )
    }

    @Test
    fun `wireless endpoint should reject a missing route or invalid port`() {
        assertNull(AdbTool.parseScrcpyTcpIpEndpoint("default via 192.168.198.1 dev wlan0", "43253"))
        assertNull(AdbTool.parseScrcpyTcpIpEndpoint("src 192.168.198.157", "70000"))
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

    @Test
    fun `closing the mirror window normally should not be treated as an unexpected exit`() {
        assertFalse(isUnexpectedMirrorExit(0))
        assertFalse(isUnexpectedMirrorExit(null))
    }

    @Test
    fun `a non-zero mirror exit code should be treated as an unexpected exit`() {
        assertTrue(isUnexpectedMirrorExit(1))
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
