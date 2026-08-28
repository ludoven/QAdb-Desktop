package com.ludoven.adbtool.domain.adb

import com.ludoven.adbtool.util.ChildProcessRegistry
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

sealed class CommandOutput {
    data class Command(val text: String) : CommandOutput()
    data class Stdout(val text: String) : CommandOutput()
    data class Stderr(val text: String) : CommandOutput()
    data class Exit(val code: Int, val durationMs: Long) : CommandOutput()
    data class Status(val text: String) : CommandOutput()
}

class RunningProcess(
    val outputs: Flow<CommandOutput>,
    private val onCancel: () -> Unit,
    private val onInterrupt: (() -> Unit)? = null,
    private val onWriteLine: ((String) -> Boolean)? = null
) {
    fun cancel() = onCancel()

    fun interrupt() {
        onInterrupt?.invoke() ?: onCancel()
    }

    fun writeLine(line: String): Boolean {
        return onWriteLine?.invoke(line) ?: false
    }
}

class AdbCommandExecutor(
    private val adbPathProvider: AdbPathProvider = DefaultAdbPathProvider()
) {

    fun start(command: AdbCommand, timeoutMs: Long? = null): RunningProcess {
        var process: Process? = null
        var stdin: BufferedWriter? = null

        val outputs = callbackFlow {
            val startedAt = System.currentTimeMillis()
            val adbPath = adbPathProvider.resolveAdbPath().getOrElse { error ->
                send(CommandOutput.Stderr(error.message ?: "未找到 ADB，请在设置中配置 ADB 路径，或启用 QADB 内置 ADB。"))
                send(CommandOutput.Exit(code = -1, durationMs = 0L))
                close()
                return@callbackFlow
            }

            val fullCommand = listOf(adbPath) + command.args
            send(CommandOutput.Command("$ ${fullCommand.joinToString(" ")}"))

            process = ProcessBuilder(fullCommand).start()
            ChildProcessRegistry.register(process!!)
            stdin = BufferedWriter(OutputStreamWriter(process!!.outputStream))

            val stdoutJob = launch(Dispatchers.IO) {
                runCatching {
                    process!!.inputStream.bufferedReader().useLines { lines ->
                        lines.forEach { line ->
                            runCatching { send(CommandOutput.Stdout(line)) }
                        }
                    }
                }
            }

            val stderrJob = launch(Dispatchers.IO) {
                runCatching {
                    process!!.errorStream.bufferedReader().useLines { lines ->
                        lines.forEach { line ->
                            runCatching { send(CommandOutput.Stderr(line)) }
                        }
                    }
                }
            }

            launch(Dispatchers.IO) {
                runCatching {
                    val finished = if (timeoutMs != null) {
                        process!!.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
                    } else {
                        process!!.waitFor()
                        true
                    }

                    if (!finished) {
                        runCatching { send(CommandOutput.Stderr("命令超时（${timeoutMs}ms），已终止。")) }
                        process!!.destroy()
                        if (process!!.isAlive) {
                            process!!.destroyForcibly()
                        }
                    }

                    stdoutJob.join()
                    stderrJob.join()

                    val exitCode = runCatching { process!!.exitValue() }.getOrDefault(-1)
                    runCatching { send(CommandOutput.Exit(code = exitCode, durationMs = System.currentTimeMillis() - startedAt)) }
                }
                ChildProcessRegistry.unregister(process)
                close()
            }

            awaitClose {
                runCatching { stdin?.close() }
                if (process?.isAlive == true) {
                    process?.destroy()
                    if (process?.waitFor(300, TimeUnit.MILLISECONDS) == false) {
                        process?.destroyForcibly()
                    }
                }
                ChildProcessRegistry.unregister(process)
            }
        }.buffer(Channel.UNLIMITED).flowOn(Dispatchers.IO)

        return RunningProcess(
            outputs = outputs,
            onCancel = {
                runCatching { stdin?.close() }
                process?.destroy()
                if (process?.isAlive == true) {
                    process?.destroyForcibly()
                }
                ChildProcessRegistry.unregister(process)
            },
            onInterrupt = {
                val writer = stdin ?: return@RunningProcess
                runCatching {
                    writer.write(3)
                    writer.newLine()
                    writer.flush()
                }.getOrElse {
                    process?.destroy()
                    ChildProcessRegistry.unregister(process)
                }
            },
            onWriteLine = { line ->
                val writer = stdin ?: return@RunningProcess false
                runCatching {
                    writer.write(line)
                    writer.newLine()
                    writer.flush()
                }.isSuccess
            }
        )
    }

    fun startInteractiveShell(deviceId: String): RunningProcess {
        val command = AdbCommand(
            rawInput = "shell",
            displayCommand = "adb -s $deviceId shell",
            executable = "adb",
            args = listOf("-s", deviceId, "shell"),
            requireDevice = true,
            entersShell = true
        )
        return start(command = command)
    }
}
