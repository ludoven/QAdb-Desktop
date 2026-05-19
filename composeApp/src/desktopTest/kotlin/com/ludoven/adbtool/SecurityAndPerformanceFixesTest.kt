package com.ludoven.adbtool

import com.ludoven.adbtool.entity.LogEntry
import com.ludoven.adbtool.pages.AppFilter
import com.ludoven.adbtool.pages.AppInfo
import com.ludoven.adbtool.pages.appFilterCounts
import com.ludoven.adbtool.pages.filterApps
import com.ludoven.adbtool.util.AdbTool
import com.ludoven.adbtool.viewmodel.LogViewModel
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
}
