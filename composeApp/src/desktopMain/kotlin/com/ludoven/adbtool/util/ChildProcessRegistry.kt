package com.ludoven.adbtool.util

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object ChildProcessRegistry {
    private val processes = ConcurrentHashMap.newKeySet<Process>()

    fun register(process: Process) {
        processes += process
    }

    fun unregister(process: Process?) {
        if (process != null) {
            processes -= process
        }
    }

    fun terminateAll(timeoutMillis: Long = 1_500L) {
        val snapshot = processes.toList()
        snapshot.forEach { process ->
            runCatching {
                if (process.isAlive) process.destroy()
            }
        }

        val deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis.coerceAtLeast(0L))
        snapshot.forEach { process ->
            val remainingNanos = deadlineNanos - System.nanoTime()
            if (remainingNanos > 0L) {
                runCatching {
                    if (process.isAlive) {
                        process.waitFor(remainingNanos, TimeUnit.NANOSECONDS)
                    }
                }
            }
        }

        snapshot.forEach { process ->
            runCatching {
                if (process.isAlive) process.destroyForcibly()
            }
            unregister(process)
        }
    }
}
