package com.ludoven.adbtool

import com.ludoven.adbtool.entity.LogEntry
import com.ludoven.adbtool.pages.AppFilter
import com.ludoven.adbtool.pages.AppInfo
import com.ludoven.adbtool.pages.AppListEmptyReason
import com.ludoven.adbtool.pages.appListEmptyReason
import com.ludoven.adbtool.pages.appFilterCounts
import com.ludoven.adbtool.pages.commonCommandRunEnabled
import com.ludoven.adbtool.pages.customAdbArgs
import com.ludoven.adbtool.pages.fileBrowserAvailableSpaceCommand
import com.ludoven.adbtool.pages.fileBrowserDeviceActionsEnabled
import com.ludoven.adbtool.pages.filterApps
import com.ludoven.adbtool.pages.homeDeviceConnected
import com.ludoven.adbtool.pages.logCaptureActionsEnabled
import com.ludoven.adbtool.pages.parsePsOutput
import com.ludoven.adbtool.pages.parseTopOutput
import com.ludoven.adbtool.viewmodel.DeviceMirrorViewModel
import com.ludoven.adbtool.util.AdbTool
import com.ludoven.adbtool.util.clipboardPlainText
import com.ludoven.adbtool.util.plainTextSelection
import com.ludoven.adbtool.viewmodel.AppViewModel
import com.ludoven.adbtool.viewmodel.CommonModel
import com.ludoven.adbtool.viewmodel.deviceInfoLoadShouldApply
import com.ludoven.adbtool.viewmodel.deviceInfoLoadShouldCancelForSelectionChange
import com.ludoven.adbtool.viewmodel.DevicesViewModel
import com.ludoven.adbtool.viewmodel.FileBrowserViewModel
import com.ludoven.adbtool.viewmodel.KeyEventViewModel
import com.ludoven.adbtool.viewmodel.LogViewModel
import com.ludoven.adbtool.viewmodel.appListLoadShouldApply
import com.ludoven.adbtool.viewmodel.fileListLoadShouldApply
import com.ludoven.adbtool.viewmodel.parseRunningPackagesFromActivityProcesses
import java.util.ArrayDeque
import kotlin.test.Test
import kotlin.test.assertEquals

class SecurityAndPerformanceFixesTest {

    @Test
    fun `shell quote should escape single quote and keep payload literal`() {
        val raw = "abc'\"; \$(whoami) `id`"
        val quoted = AdbTool.shellQuote(raw)
        assertEquals("'abc'\\''\"; \$(whoami) `id`'", quoted)
    }

    @Test
    fun `build shell command should quote each token`() {
        val command = AdbTool.buildShellCommand("rm", "-rf", "--", "/sdcard/a b;\$(id)")
        assertEquals("'rm' '-rf' '--' '/sdcard/a b;\$(id)'", command)
    }

    @Test
    fun `app shell command should quote package and permission arguments`() {
        assertEquals(
            "'pm' 'grant' 'com.example.app; reboot' 'android.permission.CAMERA'",
            AdbTool.appShellCommand("pm", "grant", "com.example.app; reboot", "android.permission.CAMERA")
        )
    }

    @Test
    fun `app view model shell commands should quote package and path arguments`() {
        assertEquals(
            "'dumpsys' 'package' 'com.example.app; reboot'",
            AppViewModel.appPackageShellCommand("dumpsys", "package", "com.example.app; reboot")
        )
        assertEquals(
            "'stat' '-c' '%s' '/data/app/Alice'\\''s App/base.apk'",
            AppViewModel.appPathShellCommand("stat", "-c", "%s", "/data/app/Alice's App/base.apk")
        )
    }

    @Test
    fun `app list load result should only apply to current device`() {
        assertEquals(true, appListLoadShouldApply(requestedDeviceId = "device-a", currentDeviceId = "device-a"))
        assertEquals(true, appListLoadShouldApply(requestedDeviceId = " device-a ", currentDeviceId = "device-a"))
        assertEquals(false, appListLoadShouldApply(requestedDeviceId = "device-a", currentDeviceId = "device-b"))
        assertEquals(false, appListLoadShouldApply(requestedDeviceId = "device-a", currentDeviceId = null))
        assertEquals(false, appListLoadShouldApply(requestedDeviceId = "   ", currentDeviceId = "device-a"))
    }

    @Test
    fun `app list blank device request should clear stale adb target`() {
        AdbTool.selectDeviceId = "stale-device"

        AppViewModel().getAppList(null)

        assertEquals(null, AdbTool.selectDeviceId)
    }

    @Test
    fun `clipboard plain text selection should preserve command text`() {
        val command = "adb shell input text 'hello world'"

        assertEquals(command, clipboardPlainText(plainTextSelection(command)))
    }

    @Test
    fun `custom adb command should target selected device when needed`() {
        assertEquals(
            listOf("-s", "device-1", "shell", "getprop", "ro.product.model"),
            customAdbArgs(
                rawCommand = "adb shell getprop ro.product.model",
                deviceId = "device-1",
                packageName = "com.example.app",
                text = "hello"
            )
        )
    }

    @Test
    fun `custom adb command should preserve explicit device selector and placeholders`() {
        assertEquals(
            listOf("-s", "explicit-device", "shell", "pm", "clear", "com.demo"),
            customAdbArgs(
                rawCommand = "adb -s explicit-device shell pm clear {package}",
                deviceId = "selected-device",
                packageName = "com.demo",
                text = "hello"
            )
        )
    }

    @Test
    fun `appendWithLimit should keep only latest entries`() {
        val buffer = ArrayDeque<LogEntry>()
        repeat(10_005) { index ->
            LogViewModel.appendWithLimit(
                buffer = buffer,
                entry = LogEntry(pid = index),
                maxEntries = 10_000
            )
        }
        assertEquals(10_000, buffer.size)
        assertEquals(5, buffer.first().pid)
        assertEquals(10_004, buffer.last().pid)
    }

    @Test
    fun `log ui publishing should be capped by batch size or interval`() {
        assertEquals(false, LogViewModel.shouldPublishUiBatch(pendingUpdates = 1, elapsedMs = 99))
        assertEquals(true, LogViewModel.shouldPublishUiBatch(pendingUpdates = 1, elapsedMs = 100))
        assertEquals(true, LogViewModel.shouldPublishUiBatch(pendingUpdates = 40, elapsedMs = 0))
    }

    @Test
    fun `log ui publishing should ignore an empty batch`() {
        assertEquals(false, LogViewModel.shouldPublishUiBatch(pendingUpdates = 0, elapsedMs = 1_000))
    }

    @Test
    fun `app filters should use stable keys instead of localized labels`() {
        val apps = listOf(
            AppInfo(appName = "User App", packageName = "com.example.user", isSystemApp = false),
            AppInfo(appName = "System App", packageName = "android.system", isSystemApp = true),
            AppInfo(appName = "Debug App", packageName = "com.example.debug", isDebuggable = true),
            AppInfo(appName = "Running App", packageName = "com.example.running", isRunning = true),
            AppInfo(appName = "Recent App", packageName = "com.example.recent", installTimestamp = 1_000L)
        )

        assertEquals(
            listOf("com.example.user", "com.example.debug", "com.example.running", "com.example.recent"),
            filterApps(apps, AppFilter.USER, "").map { it.packageName }
        )
        assertEquals(
            listOf("android.system"),
            filterApps(apps, AppFilter.SYSTEM, "").map { it.packageName }
        )
        assertEquals(
            listOf("com.example.debug"),
            filterApps(apps, AppFilter.DEBUGGABLE, "debug").map { it.packageName }
        )

        val counts = appFilterCounts(apps)
        assertEquals(5, counts.getValue(AppFilter.ALL))
        assertEquals(4, counts.getValue(AppFilter.USER))
        assertEquals(1, counts.getValue(AppFilter.SYSTEM))
        assertEquals(1, counts.getValue(AppFilter.RECENT))
        assertEquals(1, counts.getValue(AppFilter.RUNNING))
    }

    @Test
    fun `running package parser should strip process suffixes`() {
        val output = """
            ProcessRecord{12a u0 com.example.player/u0a123}
            ProcessRecord{34b u0 com.example.player:remote/u0a123}
            ProcessRecord{56c u0 com.android.systemui/u0a55}
            unrelated line
        """.trimIndent()

        assertEquals(
            setOf("com.example.player", "com.android.systemui"),
            parseRunningPackagesFromActivityProcesses(output)
        )
    }

    @Test
    fun `adb output text should preserve old string contract for structured results`() {
        assertEquals(
            "ok",
            AdbTool.outputText(AdbTool.AdbResult(success = true, output = "ok"))
        )
        assertEquals(
            "Command failed",
            AdbTool.outputText(AdbTool.AdbResult(success = false, output = "raw", errorMessage = "Command failed"))
        )
        assertEquals(
            "raw",
            AdbTool.outputText(AdbTool.AdbResult(success = false, output = "raw", errorMessage = null))
        )
        assertEquals(
            "Unknown error",
            AdbTool.outputText(AdbTool.AdbResult(success = false, output = "", errorMessage = null))
        )
    }

    @Test
    fun `app list empty reason should distinguish no device from empty filtered results`() {
        assertEquals(
            AppListEmptyReason.NO_DEVICE,
            appListEmptyReason(hasSelectedDevice = false, displayedListIsEmpty = true, isLoading = false)
        )
        assertEquals(
            AppListEmptyReason.NO_RESULTS,
            appListEmptyReason(hasSelectedDevice = true, displayedListIsEmpty = true, isLoading = false)
        )
        assertEquals(
            null,
            appListEmptyReason(hasSelectedDevice = false, displayedListIsEmpty = true, isLoading = true)
        )
        assertEquals(
            null,
            appListEmptyReason(hasSelectedDevice = true, displayedListIsEmpty = false, isLoading = false)
        )
    }

    @Test
    fun `file browser device actions should require selected device`() {
        assertEquals(false, fileBrowserDeviceActionsEnabled(null))
        assertEquals(false, fileBrowserDeviceActionsEnabled(""))
        assertEquals(false, fileBrowserDeviceActionsEnabled("   "))
        assertEquals(true, fileBrowserDeviceActionsEnabled("emulator-5554"))
    }

    @Test
    fun `file browser directory commands should shell quote paths`() {
        val path = "/sdcard/Alice's Downloads"

        assertEquals(
            "'ls' '-la' '/sdcard/Alice'\\''s Downloads/'",
            FileBrowserViewModel.listDirectoryCommand(path, longFormat = true)
        )
        assertEquals(
            "'ls' '-l' '/sdcard/Alice'\\''s Downloads/'",
            FileBrowserViewModel.listDirectoryCommand(path, longFormat = false)
        )
        assertEquals("/", FileBrowserViewModel.normalizeDirectoryPathForListing("/"))
        assertEquals("/sdcard/", FileBrowserViewModel.normalizeDirectoryPathForListing("/sdcard"))
        assertEquals("/sdcard/Download/", FileBrowserViewModel.normalizeDirectoryPathForListing(" /sdcard/Download/ "))
    }

    @Test
    fun `blank directory path should normalize to root`() {
        assertEquals("/", FileBrowserViewModel.normalizeDirectoryPathForListing("   "))
    }

    @Test
    fun `directory normalization should collapse trailing separators`() {
        assertEquals("/sdcard/Download/", FileBrowserViewModel.normalizeDirectoryPathForListing("/sdcard/Download///"))
    }

    @Test
    fun `root listing command should not add a duplicate separator`() {
        assertEquals("'ls' '-la' '/'", FileBrowserViewModel.listDirectoryCommand("/", longFormat = true))
    }

    @Test
    fun `file browser available space command should shell quote paths`() {
        assertEquals(
            "'df' '-h' '/sdcard/Alice'\\''s Downloads'",
            fileBrowserAvailableSpaceCommand("/sdcard/Alice's Downloads")
        )
    }

    @Test
    fun `file browser load result should only apply to current path and device`() {
        assertEquals(
            true,
            fileListLoadShouldApply(
                requestedPath = "/sdcard/Download",
                requestedDeviceId = "device-a",
                currentPath = "/sdcard/Download",
                currentDeviceId = "device-a"
            )
        )
        assertEquals(
            true,
            fileListLoadShouldApply(
                requestedPath = " /sdcard/Download ",
                requestedDeviceId = " device-a ",
                currentPath = "/sdcard/Download",
                currentDeviceId = "device-a"
            )
        )
        assertEquals(
            false,
            fileListLoadShouldApply(
                requestedPath = "/sdcard/Download",
                requestedDeviceId = "device-a",
                currentPath = "/sdcard/DCIM",
                currentDeviceId = "device-a"
            )
        )
        assertEquals(
            false,
            fileListLoadShouldApply(
                requestedPath = "/sdcard/Download",
                requestedDeviceId = "device-a",
                currentPath = "/sdcard/Download",
                currentDeviceId = "device-b"
            )
        )
        assertEquals(
            false,
            fileListLoadShouldApply(
                requestedPath = "/sdcard/Download",
                requestedDeviceId = null,
                currentPath = "/sdcard/Download",
                currentDeviceId = "device-a"
            )
        )
    }

    @Test
    fun `clearing selected device should clear global adb target`() {
        AdbTool.selectDeviceId = "emulator-5554"

        DevicesViewModel().selectDevice(null)

        assertEquals(null, AdbTool.selectDeviceId)
    }

    @Test
    fun `device selection should normalize blank device ids`() {
        assertEquals(null, DevicesViewModel.normalizedDeviceId(null))
        assertEquals(null, DevicesViewModel.normalizedDeviceId(""))
        assertEquals(null, DevicesViewModel.normalizedDeviceId("   "))
        assertEquals("emulator-5554", DevicesViewModel.normalizedDeviceId("emulator-5554"))
        assertEquals("emulator-5554", DevicesViewModel.normalizedDeviceId(" emulator-5554 "))
    }

    @Test
    fun `device info load result should only apply to selected device`() {
        assertEquals(true, deviceInfoLoadShouldApply(requestedDeviceId = "device-a", selectedDeviceId = "device-a"))
        assertEquals(true, deviceInfoLoadShouldApply(requestedDeviceId = " device-a ", selectedDeviceId = "device-a"))
        assertEquals(false, deviceInfoLoadShouldApply(requestedDeviceId = "device-a", selectedDeviceId = "device-b"))
        assertEquals(false, deviceInfoLoadShouldApply(requestedDeviceId = "device-a", selectedDeviceId = null))
        assertEquals(false, deviceInfoLoadShouldApply(requestedDeviceId = "   ", selectedDeviceId = "device-a"))
    }

    @Test
    fun `device info load should cancel when selected device changes`() {
        assertEquals(false, deviceInfoLoadShouldCancelForSelectionChange(activeLoadDeviceId = null, nextSelectedDeviceId = "device-a"))
        assertEquals(false, deviceInfoLoadShouldCancelForSelectionChange(activeLoadDeviceId = "device-a", nextSelectedDeviceId = "device-a"))
        assertEquals(true, deviceInfoLoadShouldCancelForSelectionChange(activeLoadDeviceId = "device-a", nextSelectedDeviceId = "device-b"))
        assertEquals(true, deviceInfoLoadShouldCancelForSelectionChange(activeLoadDeviceId = "device-a", nextSelectedDeviceId = null))
        assertEquals(true, deviceInfoLoadShouldCancelForSelectionChange(activeLoadDeviceId = "device-a", nextSelectedDeviceId = "   "))
    }

    @Test
    fun `common command run button should require selected device`() {
        assertEquals(false, commonCommandRunEnabled(null))
        assertEquals(false, commonCommandRunEnabled(""))
        assertEquals(false, commonCommandRunEnabled("   "))
        assertEquals(true, commonCommandRunEnabled("192.168.1.20:5555"))
    }

    @Test
    fun `log capture actions should require selected device`() {
        assertEquals(false, logCaptureActionsEnabled(null))
        assertEquals(false, logCaptureActionsEnabled(""))
        assertEquals(false, logCaptureActionsEnabled("   "))
        assertEquals(true, logCaptureActionsEnabled("emulator-5554"))
    }

    @Test
    fun `log capture should stop when selected device changes away from active capture`() {
        assertEquals(
            false,
            LogViewModel.shouldStopCaptureForDeviceChange(
                isCapturing = false,
                activeCaptureDevice = "emulator-5554",
                nextSelectedDevice = null
            )
        )
        assertEquals(
            false,
            LogViewModel.shouldStopCaptureForDeviceChange(
                isCapturing = true,
                activeCaptureDevice = "emulator-5554",
                nextSelectedDevice = "emulator-5554"
            )
        )
        assertEquals(
            true,
            LogViewModel.shouldStopCaptureForDeviceChange(
                isCapturing = true,
                activeCaptureDevice = "emulator-5554",
                nextSelectedDevice = null
            )
        )
        assertEquals(
            true,
            LogViewModel.shouldStopCaptureForDeviceChange(
                isCapturing = true,
                activeCaptureDevice = "emulator-5554",
                nextSelectedDevice = "192.168.1.20:5555"
            )
        )
    }

    @Test
    fun `mirror connection watcher target should ignore global adb target`() {
        assertEquals(
            "selected-device",
            DeviceMirrorViewModel.connectionWatcherTarget(
                requestedDeviceId = "selected-device",
                activeDeviceId = "active-device"
            )
        )
        assertEquals(
            "active-device",
            DeviceMirrorViewModel.connectionWatcherTarget(
                requestedDeviceId = "   ",
                activeDeviceId = "active-device"
            )
        )
        assertEquals(
            null,
            DeviceMirrorViewModel.connectionWatcherTarget(
                requestedDeviceId = null,
                activeDeviceId = "   "
            )
        )
    }

    @Test
    fun `mirror action target should ignore global adb target`() {
        assertEquals(
            "selected-device",
            DeviceMirrorViewModel.mirrorActionTarget(
                requestedDeviceId = "selected-device",
                activeDeviceId = "active-device"
            )
        )
        assertEquals(
            "active-device",
            DeviceMirrorViewModel.mirrorActionTarget(
                requestedDeviceId = "   ",
                activeDeviceId = "active-device"
            )
        )
        assertEquals(
            null,
            DeviceMirrorViewModel.mirrorActionTarget(
                requestedDeviceId = null,
                activeDeviceId = "   "
            )
        )
    }

    @Test
    fun `top parser should support android args process column`() {
        val output = """
            Tasks: 245 total,   1 running, 244 sleeping
              PID USER         PR  NI VIRT  RES  SHR S[%CPU] %MEM     TIME+ ARGS
             1234 u0_a123      20   0 1.2G 120M  40M S  8.5   3.1   0:12.34 com.example.app
             2345 system       20   0 800M  64M  24M S  1.0   1.6   0:02.00 system_server
        """.trimIndent()

        val parsed = parseTopOutput(output)

        assertEquals(2, parsed.size)
        assertEquals("com.example.app", parsed[0].name)
        assertEquals("8.5%", parsed[0].cpuPercent)
        assertEquals("0:12.34", parsed[0].cpuTime)
        assertEquals("120M", parsed[0].memory)
        assertEquals("1234", parsed[0].pid)
        assertEquals("u0_a123", parsed[0].user)
    }

    @Test
    fun `ps parser should support cmdline and rss columns`() {
        val output = """
            USER           PID  PPID   VSZ   RSS WCHAN            ADDR S CMDLINE
            u0_a123       1234   899 1624M 120M 0                   0 S com.example.app:remote
            system        2345     1  800M  64M 0                   0 S system_server
        """.trimIndent()

        val parsed = parsePsOutput(output)

        assertEquals(2, parsed.size)
        assertEquals("1234", parsed[0].pid)
        assertEquals("u0_a123", parsed[0].user)
        assertEquals("120M", parsed[0].memory)
        assertEquals("com.example.app:remote", parsed[0].name)
    }

    @Test
    fun `key event actions should require selected device`() {
        assertEquals(false, KeyEventViewModel.keyEventDeviceActionsEnabled(null))
        assertEquals(false, KeyEventViewModel.keyEventDeviceActionsEnabled(""))
        assertEquals(false, KeyEventViewModel.keyEventDeviceActionsEnabled("   "))
        assertEquals(true, KeyEventViewModel.keyEventDeviceActionsEnabled("emulator-5554"))
    }

    @Test
    fun `common model device actions should require selected device`() {
        assertEquals(false, CommonModel.deviceActionsEnabled(null))
        assertEquals(false, CommonModel.deviceActionsEnabled(""))
        assertEquals(false, CommonModel.deviceActionsEnabled("   "))
        assertEquals(true, CommonModel.deviceActionsEnabled("adb-1234"))
    }

    @Test
    fun `home connected state should require selected device`() {
        assertEquals(false, homeDeviceConnected(null))
        assertEquals(false, homeDeviceConnected(""))
        assertEquals(false, homeDeviceConnected("   "))
        assertEquals(true, homeDeviceConnected("emulator-5554"))
    }

}
