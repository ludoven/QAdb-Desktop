package com.ludoven.adbtool

import com.ludoven.adbtool.util.ChildProcessRegistry
import com.ludoven.adbtool.util.AdbTool
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChildProcessRegistryTest {

    @Test
    fun terminateAllStopsRegisteredAliveProcess() {
        val process = startLongRunningProcess()
        ChildProcessRegistry.register(process)

        ChildProcessRegistry.terminateAll(timeoutMillis = 500)

        assertFalse(process.isAlive)
    }

    @Test
    fun terminateAllUsesOneSharedTimeoutForAllProcesses() {
        val firstProcess = StubbornProcess()
        val secondProcess = StubbornProcess()
        val thirdProcess = StubbornProcess()
        ChildProcessRegistry.register(firstProcess)
        ChildProcessRegistry.register(secondProcess)
        ChildProcessRegistry.register(thirdProcess)

        val startedAt = System.nanoTime()
        ChildProcessRegistry.terminateAll(timeoutMillis = 200)
        val elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)

        assertTrue(elapsedMillis < 450, "cleanup should use one shared timeout, but took ${elapsedMillis}ms")
        assertFalse(firstProcess.isAlive)
        assertFalse(secondProcess.isAlive)
        assertFalse(thirdProcess.isAlive)
    }

    @Test
    fun stopMirrorProcessUsesOneSharedTimeoutForGracefulAndForcedStop() {
        val process = StubbornProcess()

        val startedAt = System.nanoTime()
        val stopped = AdbTool.stopMirrorProcess(process, timeoutMillis = 200)
        val elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)

        assertTrue(stopped)
        assertTrue(elapsedMillis < 450, "mirror cleanup should use one shared timeout, but took ${elapsedMillis}ms")
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

    private class StubbornProcess : Process() {
        private var alive = true

        override fun getOutputStream(): OutputStream = OutputStream.nullOutputStream()

        override fun getInputStream(): InputStream = InputStream.nullInputStream()

        override fun getErrorStream(): InputStream = InputStream.nullInputStream()

        override fun waitFor(): Int = 0

        override fun waitFor(timeout: Long, unit: TimeUnit): Boolean {
            Thread.sleep(TimeUnit.NANOSECONDS.toMillis(unit.toNanos(timeout)))
            return false
        }

        override fun exitValue(): Int = 0

        override fun destroy() = Unit

        override fun destroyForcibly(): Process {
            alive = false
            return this
        }

        override fun isAlive(): Boolean = alive
    }
}
