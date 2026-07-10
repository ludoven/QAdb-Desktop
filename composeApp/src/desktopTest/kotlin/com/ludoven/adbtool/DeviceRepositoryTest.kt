package com.ludoven.adbtool

import com.ludoven.adbtool.domain.adb.DeviceRepository
import kotlin.test.Test
import kotlin.test.assertEquals

class DeviceRepositoryTest {

    @Test
    fun `parser should preserve a device serial containing spaces`() {
        val output = listOf(
            "List of devices attached",
            "adb-0123456789ABCDEF-RY0GTI (2)._adb-tls-connect._tcp\tdevice"
        ).joinToString("\n")

        assertEquals(
            listOf("adb-0123456789ABCDEF-RY0GTI (2)._adb-tls-connect._tcp"),
            DeviceRepository.parseDevices(output)
        )
    }

    @Test
    fun `parser should accept ordinary usb and emulator serials`() {
        val output = listOf(
            "List of devices attached",
            "R58M123456A\tdevice",
            "emulator-5554 device"
        ).joinToString("\n")

        assertEquals(
            listOf("R58M123456A", "emulator-5554"),
            DeviceRepository.parseDevices(output)
        )
    }

    @Test
    fun `parser should exclude unavailable device states`() {
        val output = listOf(
            "List of devices attached",
            "192.168.198.31:5555\toffline",
            "unauthorized-device\tunauthorized",
            "online-device\tdevice"
        ).joinToString("\n")

        assertEquals(listOf("online-device"), DeviceRepository.parseDevices(output))
    }

    @Test
    fun `parser should ignore headers daemon messages and blank output`() {
        val output = """
            * daemon started successfully
            List of devices attached

        """.trimIndent()

        assertEquals(emptyList(), DeviceRepository.parseDevices(output))
        assertEquals(emptyList(), DeviceRepository.parseDevices(""))
    }
}
