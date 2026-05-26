package com.ludoven.adbtool

import com.ludoven.adbtool.entity.DeviceMirrorSettings
import com.ludoven.adbtool.util.AdbTool
import com.ludoven.adbtool.util.ScrcpyPathManager
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

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
}
