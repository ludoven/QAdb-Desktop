package com.ludoven.adbtool.agent

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeviceStatusRepositoryTest {
    @Test
    fun `repository parses a typed snapshot and samples cpu twice`() = runBlocking {
        val runner = FakeStatusRunner()
        val repository = repository(runner)

        val snapshot = repository.readStatus("emulator-5554")

        assertEquals("Pixel 9", snapshot.identity?.model)
        assertEquals(36, snapshot.identity?.sdkVersion)
        assertEquals(1080, snapshot.display?.widthPx)
        assertEquals(2400, snapshot.display?.heightPx)
        assertEquals(420, snapshot.display?.densityDpi)
        assertEquals(24, snapshot.battery?.levelPercent)
        assertEquals(DeviceBatteryState.DISCHARGING, snapshot.battery?.state)
        assertEquals(31.5, snapshot.battery?.temperatureCelsius)
        assertEquals(70.0, snapshot.cpu?.usagePercent)
        assertEquals(75.0, snapshot.memory?.usedPercent)
        assertEquals(60.0, snapshot.storage?.usedPercent)
        assertEquals(DeviceConnectionType.USB, snapshot.network.connectionType)
        assertEquals(true, snapshot.network.wifiEnabled)
        assertEquals(true, snapshot.network.wifiConnected)
        assertEquals("192.168.1.8", snapshot.network.ipAddress)
        assertEquals("com.android.settings", snapshot.foreground?.packageName)
        assertEquals(".Settings", snapshot.foreground?.activityName)
        assertEquals(2, runner.count("cat", "/proc/stat"))
        assertTrue(snapshot.failures.isEmpty())
        assertTrue(snapshot.hasFreshEvidence())
    }

    @Test
    fun `mdns tls device id is a wifi adb transport and does not replace wifi radio state`() = runBlocking {
        val snapshot = repository(FakeStatusRunner()).readStatus(
            "adb-c0faed14-NifgBj._adb-tls-connect._tcp"
        )

        assertEquals(DeviceConnectionType.WIFI, snapshot.network.connectionType)
        assertEquals(true, snapshot.network.wifiEnabled)
        assertEquals(true, snapshot.network.wifiConnected)
        assertTrue(isNetworkDeviceId("192.168.1.8:5555"))
        assertFalse(isNetworkDeviceId("emulator-5554"))
    }

    @Test
    fun `cache is reused for three seconds and force refresh bypasses it`() = runBlocking {
        val runner = FakeStatusRunner()
        var now = 1_000L
        val repository = repository(runner, clockMillis = { now })

        val first = repository.readStatus("device-a")
        now = 3_999L
        val cached = repository.readStatus("device-a")
        val forced = repository.readStatus("device-a", forceRefresh = true)

        assertEquals(first, cached)
        assertEquals(2, runner.count("getprop"))
        assertEquals(3_999L, forced.observedAtMs)

        now = 7_000L
        repository.readStatus("device-a")
        assertEquals(3, runner.count("getprop"))
    }

    @Test
    fun `concurrent readers share one in flight collection`() = runBlocking {
        val runner = FakeStatusRunner(commandDelayMs = 20L)
        val repository = repository(runner)

        val snapshots = coroutineScope {
            List(6) { async { repository.readStatus("device-a") } }.awaitAll()
        }

        assertEquals(1, snapshots.map { it.observedAtMs }.distinct().size)
        assertEquals(1, runner.count("getprop"))
        assertEquals(2, runner.count("cat", "/proc/stat"))
    }

    @Test
    fun `one failed probe is reported without discarding other fields`() = runBlocking {
        val runner = FakeStatusRunner(failingCommand = listOf("cat", "/proc/meminfo"))
        val snapshot = repository(runner).readStatus("device-a")

        assertNull(snapshot.memory)
        assertNotNull(snapshot.battery)
        assertNotNull(snapshot.identity)
        val failure = snapshot.failures.single { it.probe == DeviceStatusProbe.MEMORY }
        assertEquals("probe unavailable", failure.reason)
        assertFalse(failure.timedOut)
    }

    @Test
    fun `each slow probe is bounded and returned as a field timeout`() = runBlocking {
        val runner = FakeStatusRunner(commandDelayMs = 100L)
        val repository = repository(
            runner = runner,
            probeTimeoutMillis = 15L,
            totalTimeoutMillis = 150L
        )

        val snapshot = repository.readStatus("device-a")

        assertFalse(snapshot.hasFreshEvidence())
        assertEquals(DeviceStatusProbe.entries.size, snapshot.failures.size)
        assertTrue(snapshot.failures.all { it.timedOut })
    }

    private fun repository(
        runner: DeviceStatusCommandRunner,
        clockMillis: () -> Long = System::currentTimeMillis,
        probeTimeoutMillis: Long = 3_000L,
        totalTimeoutMillis: Long = 5_000L
    ) = DeviceStatusRepository(
        commandRunner = runner,
        clockMillis = clockMillis,
        sleeper = { },
        cacheTtlMillis = 3_000L,
        probeTimeoutMillis = probeTimeoutMillis,
        totalTimeoutMillis = totalTimeoutMillis
    )
}

private class FakeStatusRunner(
    private val commandDelayMs: Long = 0L,
    private val failingCommand: List<String>? = null
) : DeviceStatusCommandRunner {
    private val counts = ConcurrentHashMap<String, Int>()

    override suspend fun execute(deviceId: String, vararg shellArgs: String): String {
        val key = shellArgs.joinToString("\u0000")
        counts.compute(key) { _, previous -> (previous ?: 0) + 1 }
        if (commandDelayMs > 0) delay(commandDelayMs)
        if (shellArgs.toList() == failingCommand) error("probe unavailable")

        return when (shellArgs.toList()) {
            listOf("getprop") -> """
                [ro.product.model]: [Pixel 9]
                [ro.product.manufacturer]: [Google]
                [ro.build.version.release]: [16]
                [ro.build.version.sdk]: [36]
                [ro.build.display.id]: [BP2A.test]
                [ro.build.fingerprint]: [google/fingerprint]
            """.trimIndent()
            listOf("uname", "-r") -> "6.1.0-android"
            listOf("wm", "size") -> "Physical size: 1080x2400"
            listOf("wm", "density") -> "Physical density: 420"
            listOf("settings", "get", "system", "font_scale") -> "1.1"
            listOf("ip", "addr", "show", "wlan0") -> """
                4: wlan0: <UP>
                    link/ether aa:bb:cc:dd:ee:ff
                    inet 192.168.1.8/24 scope global wlan0
            """.trimIndent()
            listOf("cmd", "wifi", "status") -> """
                Wifi is enabled
                Wifi scanning is always available
                Wifi is connected to "Test Network"
            """.trimIndent()
            listOf("cat", "/proc/stat") -> if (count("cat", "/proc/stat") == 1) {
                "cpu 100 0 50 850 0 0 0 0"
            } else {
                "cpu 150 0 70 880 0 0 0 0"
            }
            listOf("cat", "/proc/meminfo") -> """
                MemTotal:        8000000 kB
                MemAvailable:    2000000 kB
            """.trimIndent()
            listOf("df", "-k", "/data") -> """
                Filesystem 1K-blocks Used Available Use% Mounted on
                /dev/block/dm-10 100000 60000 40000 60% /data
            """.trimIndent()
            listOf("dumpsys", "battery") -> """
                Current Battery Service state:
                  status: 3
                  level: 24
                  temperature: 315
            """.trimIndent()
            listOf("dumpsys", "activity", "activities") ->
                "mResumedActivity: ActivityRecord{1 u0 com.android.settings/.Settings t10}"
            listOf("echo", "ping") -> "ping"
            else -> ""
        }
    }

    fun count(vararg command: String): Int = counts[command.joinToString("\u0000")] ?: 0
}
