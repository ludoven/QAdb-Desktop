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
import com.ludoven.adbtool.viewmodel.PrimaryStorageSummary
import com.ludoven.adbtool.viewmodel.ResourceViewModel
import com.ludoven.adbtool.viewmodel.SystemViewModel
import com.ludoven.adbtool.viewmodel.appendStorageHistory
import com.ludoven.adbtool.viewmodel.appListLoadShouldApply
import com.ludoven.adbtool.viewmodel.calculateMemUsedMb
import com.ludoven.adbtool.viewmodel.calculateMemUsedPercent
import com.ludoven.adbtool.viewmodel.fileListLoadShouldApply
import com.ludoven.adbtool.viewmodel.parsePackageDuSizeMb
import com.ludoven.adbtool.viewmodel.parseRunningPackagesFromActivityProcesses
import com.ludoven.adbtool.viewmodel.resourceRefreshShouldCancelForDeviceChange
import com.ludoven.adbtool.viewmodel.selectPrimaryStorageSummary
import com.ludoven.adbtool.viewmodel.StoragePartition
import com.ludoven.adbtool.viewmodel.systemInfoLoadShouldCancelForDeviceChange
import com.ludoven.adbtool.viewmodel.systemInfoLoadShouldApply
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
    fun `du parser should convert kilobytes to megabytes`() {
        assertEquals(12L, parsePackageDuSizeMb("12288\t/data/app/example/base.apk"))
        assertEquals(null, parsePackageDuSizeMb("du: /data/app/example/base.apk: Permission denied"))
    }

    @Test
    fun `memory derived metrics should report used size and percent`() {
        assertEquals(6144L, calculateMemUsedMb(totalMb = 8192L, availableMb = 2048L))
        assertEquals(75f, calculateMemUsedPercent(totalMb = 8192L, availableMb = 2048L))
        assertEquals(0f, calculateMemUsedPercent(totalMb = 0L, availableMb = 0L))
    }

    @Test
    fun `primary storage summary should prefer data mount then writable partition then largest used`() {
        val withData = listOf(
            StoragePartition("/system", totalGb = 8.0, usedGb = 5.0, availGb = 3.0),
            StoragePartition("/data", totalGb = 128.0, usedGb = 64.0, availGb = 64.0)
        )
        assertEquals(
            PrimaryStorageSummary("/data", totalGb = 128.0, usedGb = 64.0, availGb = 64.0, usedPercent = 50f),
            selectPrimaryStorageSummary(withData)
        )

        val writableFallback = listOf(
            StoragePartition("/metadata", totalGb = 1.0, usedGb = 0.3, availGb = 0.7),
            StoragePartition("/sdcard", totalGb = 256.0, usedGb = 100.0, availGb = 156.0)
        )
        assertEquals(
            "/sdcard",
            selectPrimaryStorageSummary(writableFallback)?.mount
        )

        val largestUsedFallback = listOf(
            StoragePartition("/a", totalGb = 4.0, usedGb = 1.0, availGb = 3.0),
            StoragePartition("/b", totalGb = 4.0, usedGb = 2.5, availGb = 1.5)
        )
        assertEquals(
            "/b",
            selectPrimaryStorageSummary(largestUsedFallback)?.mount
        )
    }

    @Test
    fun `storage history should append primary usage and keep max size`() {
        val initial = List(60) { 10f + it }
        val updated = appendStorageHistory(
            history = initial,
            summary = PrimaryStorageSummary("/data", totalGb = 128.0, usedGb = 96.0, availGb = 32.0, usedPercent = 75f),
            maxHistory = 60
        )

        assertEquals(60, updated.size)
        assertEquals(11f, updated.first())
        assertEquals(75f, updated.last())
        assertEquals(updated, appendStorageHistory(updated, null, maxHistory = 60))
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
            "'ls' '-la' '/sdcard/Alice'\\''s Downloads'",
            FileBrowserViewModel.listDirectoryCommand(path, longFormat = true)
        )
        assertEquals(
            "'ls' '-l' '/sdcard/Alice'\\''s Downloads'",
            FileBrowserViewModel.listDirectoryCommand(path, longFormat = false)
        )
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
    fun `system view model should clear device scoped data when selected device is cleared`() {
        val viewModel = SystemViewModel()
        viewModel.replaceDeviceScopedDataForTest(
            systemProps = mapOf("ro.product.model" to "Pixel"),
            batteryInfo = mapOf("level" to "80"),
            cpuInfo = listOf("Processor" to "ARM"),
            screenInfo = mapOf("Physical size" to "1080x2400")
        )

        viewModel.setDevice("   ")

        assertEquals(null, viewModel.selectedDevice.value)
        assertEquals(emptyMap(), viewModel.systemProps.value)
        assertEquals(emptyMap(), viewModel.batteryInfo.value)
        assertEquals(emptyList(), viewModel.cpuInfo.value)
        assertEquals(emptyMap(), viewModel.screenInfo.value)
        assertEquals(false, viewModel.isLoading.value)
    }

    @Test
    fun `system info load result should only apply to selected device`() {
        assertEquals(true, systemInfoLoadShouldApply(requestedDeviceId = "device-a", selectedDeviceId = "device-a"))
        assertEquals(true, systemInfoLoadShouldApply(requestedDeviceId = " device-a ", selectedDeviceId = "device-a"))
        assertEquals(false, systemInfoLoadShouldApply(requestedDeviceId = "device-a", selectedDeviceId = "device-b"))
        assertEquals(false, systemInfoLoadShouldApply(requestedDeviceId = "device-a", selectedDeviceId = null))
        assertEquals(false, systemInfoLoadShouldApply(requestedDeviceId = "   ", selectedDeviceId = "device-a"))
    }

    @Test
    fun `system info load should cancel when selected device changes`() {
        assertEquals(false, systemInfoLoadShouldCancelForDeviceChange(activeLoadDeviceId = null, nextSelectedDeviceId = "device-a"))
        assertEquals(false, systemInfoLoadShouldCancelForDeviceChange(activeLoadDeviceId = "device-a", nextSelectedDeviceId = "device-a"))
        assertEquals(true, systemInfoLoadShouldCancelForDeviceChange(activeLoadDeviceId = "device-a", nextSelectedDeviceId = "device-b"))
        assertEquals(true, systemInfoLoadShouldCancelForDeviceChange(activeLoadDeviceId = "device-a", nextSelectedDeviceId = null))
        assertEquals(true, systemInfoLoadShouldCancelForDeviceChange(activeLoadDeviceId = "device-a", nextSelectedDeviceId = "   "))
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

    @Test
    fun `resource monitoring actions should require selected device`() {
        assertEquals(false, ResourceViewModel.resourceDeviceActionsEnabled(null))
        assertEquals(false, ResourceViewModel.resourceDeviceActionsEnabled(""))
        assertEquals(false, ResourceViewModel.resourceDeviceActionsEnabled("   "))
        assertEquals(true, ResourceViewModel.resourceDeviceActionsEnabled("emulator-5554"))
    }

    @Test
    fun `resource device id should be normalized before adb use`() {
        assertEquals(null, ResourceViewModel.normalizedResourceDeviceId(null))
        assertEquals(null, ResourceViewModel.normalizedResourceDeviceId(""))
        assertEquals(null, ResourceViewModel.normalizedResourceDeviceId("   "))
        assertEquals("emulator-5554", ResourceViewModel.normalizedResourceDeviceId("emulator-5554"))
        assertEquals("emulator-5554", ResourceViewModel.normalizedResourceDeviceId(" emulator-5554 "))
    }

    @Test
    fun `resource refresh should cancel when device changes`() {
        assertEquals(false, resourceRefreshShouldCancelForDeviceChange(activeRefreshDeviceId = null, nextDeviceId = "device-a"))
        assertEquals(false, resourceRefreshShouldCancelForDeviceChange(activeRefreshDeviceId = "device-a", nextDeviceId = "device-a"))
        assertEquals(true, resourceRefreshShouldCancelForDeviceChange(activeRefreshDeviceId = "device-a", nextDeviceId = "device-b"))
        assertEquals(true, resourceRefreshShouldCancelForDeviceChange(activeRefreshDeviceId = "device-a", nextDeviceId = null))
        assertEquals(true, resourceRefreshShouldCancelForDeviceChange(activeRefreshDeviceId = "device-a", nextDeviceId = "   "))
    }

    @Test
    fun `system actions should require selected device`() {
        assertEquals(false, SystemViewModel.systemDeviceActionsEnabled(null))
        assertEquals(false, SystemViewModel.systemDeviceActionsEnabled(""))
        assertEquals(false, SystemViewModel.systemDeviceActionsEnabled("   "))
        assertEquals(true, SystemViewModel.systemDeviceActionsEnabled("emulator-5554"))
    }
}
