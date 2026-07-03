package com.ludoven.adbtool

import com.ludoven.adbtool.pages.HomeDiagnosticCode
import com.ludoven.adbtool.pages.homeDiagnosticCodes
import kotlin.test.Test
import kotlin.test.assertEquals

class HomeDiagnosticsTest {
    @Test
    fun `not connected should report no device only`() {
        assertEquals(
            listOf(HomeDiagnosticCode.NO_DEVICE),
            homeDiagnosticCodes(
                isConnected = false,
                storageProgress = 0.96f,
                latencyMs = 500,
                batteryProgress = 0.1f
            )
        )
    }

    @Test
    fun `connected device should report resource warnings`() {
        assertEquals(
            listOf(HomeDiagnosticCode.STORAGE_LOW, HomeDiagnosticCode.LATENCY_HIGH, HomeDiagnosticCode.BATTERY_LOW),
            homeDiagnosticCodes(
                isConnected = true,
                storageProgress = 0.9f,
                latencyMs = 320,
                batteryProgress = 0.18f
            )
        )
    }

    @Test
    fun `connected device without warnings should be healthy`() {
        assertEquals(
            listOf(HomeDiagnosticCode.HEALTHY),
            homeDiagnosticCodes(
                isConnected = true,
                storageProgress = 0.52f,
                latencyMs = 120,
                batteryProgress = 0.8f
            )
        )
    }
}
