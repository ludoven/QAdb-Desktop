package com.ludoven.adbtool.agent

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AgentReadOnlyAnswersTest {
    private val apps = listOf(
        InstalledAgentApp("com.tencent.mm", "微信", enabled = true),
        InstalledAgentApp("com.example.notes", "便签", enabled = true)
    )

    @Test
    fun `installation question returns only the referenced application`() {
        listOf(
            "微信是否安装",
            "有没有安装微信",
            "不要打开微信，只告诉我是否安装"
        ).forEach { task ->
            val evidence = AgentReadOnlyAnswers.installedAppsEvidence("微信", apps)
            val serialized = evidence.facts.values.joinToString("\n")
            assertContains(serialized, "微信", message = task)
            assertFalse(serialized.contains("便签"), task)
        }
    }

    @Test
    fun `multiple application queries search the complete catalog before truncation`() {
        val largeCatalog = (0 until 165).map { index ->
            InstalledAgentApp("com.example.app$index", "示例应用$index", enabled = true)
        } + listOf(
            InstalledAgentApp("com.tencent.mm", "微信", enabled = true),
            InstalledAgentApp("com.miui.weather2", "天气", enabled = true)
        )

        val evidence = AgentReadOnlyAnswers.installedAppsEvidence(listOf("微信", "天气"), largeCatalog)

        assertEquals("167", evidence.facts["catalog_count"])
        assertEquals("2", evidence.facts["query_count"])
        assertEquals("true", evidence.facts["queries.0.found"])
        assertEquals("true", evidence.facts["queries.1.found"])
        assertEquals("1", evidence.facts["queries.0.match_count"])
        assertEquals("1", evidence.facts["queries.1.match_count"])
        assertEquals(setOf("微信", "天气"), setOf(
            evidence.facts["apps.0.label"],
            evidence.facts["apps.1.label"]
        ))
        assertEquals("false", evidence.facts["truncated"])
        assertEquals("true", evidence.facts["absence_conclusive"])
        assertTrue(evidence.complete)
    }

    @Test
    fun `unfiltered truncated catalog cannot prove application absence`() {
        val largeCatalog = (0 until 40).map { index ->
            InstalledAgentApp("com.example.app$index", "示例应用$index", enabled = true)
        }

        val evidence = AgentReadOnlyAnswers.installedAppsEvidence(emptyList(), largeCatalog)

        assertEquals("true", evidence.facts["truncated"])
        assertEquals("false", evidence.facts["absence_conclusive"])
        assertFalse(evidence.complete)
        assertTrue("complete_app_listing" in evidence.unavailableFields)
    }

    @Test
    fun `device evidence omits identifiers network addresses and raw build data`() {
        val evidence = AgentReadOnlyAnswers.deviceStatusEvidence(
            snapshot = DeviceStatusSnapshot(
                deviceId = "sensitive-device-id",
                observedAtMs = 123L,
                identity = DeviceIdentityStatus(
                    model = "Pixel",
                    manufacturer = "Google",
                    androidVersion = "16",
                    sdkVersion = 36,
                    kernelVersion = "sensitive-kernel",
                    romVersion = "sensitive-rom",
                    buildFingerprint = "sensitive-fingerprint"
                ),
                display = null,
                battery = DeviceBatteryStatus(18, DeviceBatteryState.DISCHARGING, 31.5),
                cpu = null,
                memory = null,
                storage = null,
                network = DeviceNetworkStatus(
                    DeviceConnectionType.WIFI,
                    ipAddress = "192.0.2.1",
                    macAddress = "00:11:22:33:44:55",
                    wifiEnabled = true,
                    wifiConnected = true
                ),
                foreground = null,
                adbLatencyMs = null,
                failures = emptyList()
            ),
            requestedFields = setOf(
                DeviceStatusField.IDENTITY,
                DeviceStatusField.ANDROID,
                DeviceStatusField.BATTERY,
                DeviceStatusField.NETWORK
            )
        )

        assertEquals(AgentTrustedEvidenceSource.DEVICE_STATUS, evidence.source)
        assertEquals("18", evidence.facts["battery.level_percent"])
        assertEquals("wifi", evidence.facts["network.adb_transport"])
        assertEquals("true", evidence.facts["network.wifi_enabled"])
        assertEquals("true", evidence.facts["network.wifi_connected"])
        assertFalse("network.connection_type" in evidence.facts)
        val serialized = evidence.facts.entries.joinToString("\n")
        listOf(
            "sensitive-device-id",
            "192.0.2.1",
            "00:11:22:33:44:55",
            "sensitive-kernel",
            "sensitive-rom",
            "sensitive-fingerprint"
        ).forEach { secret -> assertFalse(serialized.contains(secret), secret) }
    }

    @Test
    fun `system probe evidence contains only parsed state`() {
        val evidence = AgentReadOnlyAnswers.systemProbeEvidence(
            probeId = "rotation_locked",
            rawValue = "false"
        )

        assertEquals(AgentTrustedEvidenceSource.SYSTEM_PROBE, evidence.source)
        assertEquals(mapOf("probe_id" to "rotation_locked", "enabled" to "true"), evidence.facts)
    }
}
