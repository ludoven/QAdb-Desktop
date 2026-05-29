package com.ludoven.adbtool

import com.ludoven.adbtool.entity.LogEntry
import com.ludoven.adbtool.pages.AppFilter
import com.ludoven.adbtool.pages.AppInfo
import com.ludoven.adbtool.pages.appFilterCounts
import com.ludoven.adbtool.pages.filterApps
import com.ludoven.adbtool.util.AdbTool
import com.ludoven.adbtool.viewmodel.LogViewModel
import com.ludoven.adbtool.viewmodel.PrimaryStorageSummary
import com.ludoven.adbtool.viewmodel.appendStorageHistory
import com.ludoven.adbtool.viewmodel.calculateMemUsedMb
import com.ludoven.adbtool.viewmodel.calculateMemUsedPercent
import com.ludoven.adbtool.viewmodel.parsePackageDuSizeMb
import com.ludoven.adbtool.viewmodel.parseRunningPackagesFromActivityProcesses
import com.ludoven.adbtool.viewmodel.selectPrimaryStorageSummary
import com.ludoven.adbtool.viewmodel.StoragePartition
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
}
