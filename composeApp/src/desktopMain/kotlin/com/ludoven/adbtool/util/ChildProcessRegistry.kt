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
            if (process.isAlive) {
                process.destroy()
            }
        }
        snapshot.forEach { process ->
            if (process.isAlive) {
                val stopped = runCatching {
                    process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)
                }.getOrDefault(false)
                if (!stopped && process.isAlive) {
                    process.destroyForcibly()
                }
            }
            unregister(process)
        }
    }
}
