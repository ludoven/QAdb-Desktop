package com.ludoven.adbtool

import com.ludoven.adbtool.entity.LogEntry
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
}
