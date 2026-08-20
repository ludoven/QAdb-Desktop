package com.ludoven.adbtool

import com.ludoven.adbtool.util.AdbTool
import com.ludoven.adbtool.util.WirelessAdbIssue
import com.ludoven.adbtool.util.WirelessAdbOperation
import com.ludoven.adbtool.util.decodeWirelessConnectionHistory
import com.ludoven.adbtool.util.encodeWirelessConnectionHistory
import com.ludoven.adbtool.util.interpretWirelessAdbResult
import com.ludoven.adbtool.util.validatePairingCode
import com.ludoven.adbtool.util.validateWirelessEndpoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WirelessAdbConnectionTest {

    @Test
    fun `connect endpoint trims input and supplies default port`() {
        val result = validateWirelessEndpoint(" 192.168.1.20 ", requirePort = false)

        assertTrue(result.isValid)
        assertEquals("192.168.1.20:5555", result.endpoint)
    }

    @Test
    fun `pairing endpoint requires an explicit valid port`() {
        assertEquals(
            WirelessAdbIssue.PORT_REQUIRED,
            validateWirelessEndpoint("pixel.local", requirePort = true).issue
        )
        assertEquals(
            WirelessAdbIssue.INVALID_PORT,
            validateWirelessEndpoint("pixel.local:", requirePort = true).issue
        )
        assertEquals(
            WirelessAdbIssue.INVALID_PORT,
            validateWirelessEndpoint("pixel.local:65536", requirePort = true).issue
        )
    }

    @Test
    fun `wireless endpoint supports bracketed ipv6 only`() {
        assertEquals(
            "[fe80::1]:37145",
            validateWirelessEndpoint("[fe80::1]:37145", requirePort = true).endpoint
        )
        assertEquals(
            WirelessAdbIssue.INVALID_ADDRESS,
            validateWirelessEndpoint("fe80::1:37145", requirePort = true).issue
        )
        assertEquals(
            WirelessAdbIssue.INVALID_ADDRESS,
            validateWirelessEndpoint("[not-an-ipv6]:37145", requirePort = true).issue
        )
    }

    @Test
    fun `pairing code must contain exactly six digits`() {
        assertNull(validatePairingCode(" 123456 "))
        assertEquals(WirelessAdbIssue.INVALID_PAIRING_CODE, validatePairingCode("12345"))
        assertEquals(WirelessAdbIssue.INVALID_PAIRING_CODE, validatePairingCode("12A456"))
    }

    @Test
    fun `adb connect result uses semantic output instead of exit status alone`() {
        val failure = interpretWirelessAdbResult(
            operation = WirelessAdbOperation.CONNECT,
            endpoint = "192.168.1.20:5555",
            adbResult = AdbTool.AdbResult(
                success = true,
                output = "failed to connect to 192.168.1.20:5555: Connection refused"
            )
        )
        val success = interpretWirelessAdbResult(
            operation = WirelessAdbOperation.CONNECT,
            endpoint = "192.168.1.20:5555",
            adbResult = AdbTool.AdbResult(
                success = true,
                output = "already connected to 192.168.1.20:5555"
            )
        )

        assertFalse(failure.success)
        assertEquals(WirelessAdbIssue.UNREACHABLE, failure.issue)
        assertTrue(success.success)
    }

    @Test
    fun `adb pairing success output is recognized`() {
        val result = interpretWirelessAdbResult(
            operation = WirelessAdbOperation.PAIR,
            endpoint = "192.168.1.20:37145",
            adbResult = AdbTool.AdbResult(
                success = true,
                output = "Successfully paired to 192.168.1.20:37145 [guid=adb-test]"
            )
        )

        assertTrue(result.success)
    }

    @Test
    fun `history is de duplicated ordered and capped`() {
        val encoded = encodeWirelessConnectionHistory(
            listOf(
                "device-a:5555",
                " device-b:5555 ",
                "device-a:5555",
                "device-c:5555",
                "device-d:5555",
                "device-e:5555",
                "device-f:5555"
            )
        )

        assertEquals(
            listOf(
                "device-a:5555",
                "device-b:5555",
                "device-c:5555",
                "device-d:5555",
                "device-e:5555"
            ),
            decodeWirelessConnectionHistory(encoded)
        )
    }
}
