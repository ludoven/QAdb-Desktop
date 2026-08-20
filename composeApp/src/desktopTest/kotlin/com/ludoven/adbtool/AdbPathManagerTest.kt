package com.ludoven.adbtool

import com.ludoven.adbtool.util.AdbPathManager
import com.ludoven.adbtool.util.AdbProcessTimeoutContext
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdbPathManagerTest {

    @Test
    fun bundledAdbPathUsesPlatformAndCpuArchitecture() {
        val root = File("/tmp/qadb-test")

        assertEquals(
            File(root, "adb/macos/arm64/adb").absolutePath,
            AdbPathManager.resolveBundledAdbPath(root, "Mac OS X", "aarch64")?.absolutePath
        )
        assertEquals(
            File(root, "adb/macos/x64/adb").absolutePath,
            AdbPathManager.resolveBundledAdbPath(root, "Mac OS X", "x86_64")?.absolutePath
        )
        assertEquals(
            File(root, "adb/windows/adb.exe").absolutePath,
            AdbPathManager.resolveBundledAdbPath(root, "Windows 11", "amd64")?.absolutePath
        )
        assertEquals(
            File(root, "adb/linux/adb").absolutePath,
            AdbPathManager.resolveBundledAdbPath(root, "Linux", "amd64")?.absolutePath
        )
    }

    @Test
    fun friendlyInitializationErrorDoesNotExposeRawCommandFailure() {
        val message = AdbPathManager.friendlyInitializationError("adb: command not found")

        assertEquals("ADB 初始化失败，请检查 QADB 是否有执行权限，或手动选择 adb 路径。", message)
        assertFalse(message.contains("command not found"))
    }

    @Test
    fun defaultAdbCandidatesIncludeSdkEnvironmentAndUserHomeDefaults() {
        val candidates = AdbPathManager.buildSystemAdbCandidates(
            userHome = File("/Users/qadb"),
            environment = mapOf("ANDROID_HOME" to "/opt/android", "ANDROID_SDK_ROOT" to "/Volumes/sdk"),
            osName = "Mac OS X"
        )

        assertEquals("/opt/android/platform-tools/adb", candidates[0])
        assertEquals("/Volumes/sdk/platform-tools/adb", candidates[1])
        assertTrue(candidates.contains("/Users/qadb/Library/Android/sdk/platform-tools/adb"))
        assertTrue(candidates.contains("/Users/qadb/Library/Android/Sdk/platform-tools/adb"))
    }

    @Test
    fun defaultWindowsAdbCandidatesUseUserHomeAppDataPath() {
        val candidates = AdbPathManager.buildSystemAdbCandidates(
            userHome = File("C:\\Users\\qadb"),
            environment = emptyMap(),
            osName = "Windows 11"
        )

        assertTrue(candidates.contains("C:\\Users\\qadb\\AppData\\Local\\Android\\Sdk\\platform-tools\\adb.exe"))
    }

    @Test
    fun packagedAppResourceAdbPathUsesScrcpyAdbBesidePackagedResources() {
        val root = File("/tmp/qadb-app-resources")

        assertEquals(
            File(root, "scrcpy/adb").absolutePath,
            AdbPathManager.resolvePackagedAdbPath(root, "Mac OS X")?.absolutePath
        )
        assertEquals(
            File(root, "scrcpy/adb.exe").absolutePath,
            AdbPathManager.resolvePackagedAdbPath(root, "Windows 11")?.absolutePath
        )
    }

    @Test
    fun adbValidationCannotOutliveThePropagatedAgentDeadline() = runBlocking {
        if (System.getProperty("os.name").lowercase().contains("windows")) return@runBlocking
        val directory = Files.createTempDirectory("qadb-slow-adb").toFile().also(File::deleteOnExit)
        val executable = File(directory, "adb").also { file ->
            file.writeText("#!/bin/sh\nsleep 2\necho 'Android Debug Bridge version test'\n")
            assertTrue(file.setExecutable(true))
            file.deleteOnExit()
        }
        val validate = AdbPathManager::class.java
            .getDeclaredMethod("validateAdb", String::class.java)
            .also { it.isAccessible = true }

        val startedAtNanos = System.nanoTime()
        withContext(AdbProcessTimeoutContext.asContextElement(75L)) {
            validate.invoke(AdbPathManager, executable.absolutePath)
        }
        val elapsedMillis = (System.nanoTime() - startedAtNanos) / 1_000_000L

        assertTrue(elapsedMillis < 1_000L, "validation took ${elapsedMillis}ms")
    }
}
