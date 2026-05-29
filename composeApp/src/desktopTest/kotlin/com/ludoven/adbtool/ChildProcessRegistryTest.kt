package com.ludoven.adbtool

import com.ludoven.adbtool.util.ChildProcessRegistry
import kotlin.test.Test
import kotlin.test.assertFalse

class ChildProcessRegistryTest {

    @Test
    fun terminateAllStopsRegisteredAliveProcess() {
        val process = startLongRunningProcess()
        ChildProcessRegistry.register(process)

        ChildProcessRegistry.terminateAll(timeoutMillis = 500)

        assertFalse(process.isAlive)
    }

    private fun startLongRunningProcess(): Process {
        val command = if (System.getProperty("os.name").lowercase().contains("windows")) {
            listOf("cmd", "/c", "ping -n 6 127.0.0.1 >NUL")
        } else {
            listOf("sh", "-c", "sleep 5")
        }
        return ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()
    }
}
