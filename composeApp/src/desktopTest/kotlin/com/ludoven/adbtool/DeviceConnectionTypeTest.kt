package com.ludoven.adbtool

import com.ludoven.adbtool.util.isWirelessAdbConnection
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeviceConnectionTypeTest {

    @Test
    fun `ip port device id should be wireless`() {
        assertTrue(isWirelessAdbConnection("192.168.1.10:38123"))
    }

    @Test
    fun `tls connect device id should be wireless`() {
        assertTrue(isWirelessAdbConnection("adb-123456._adb-tls-connect._tcp"))
    }

    @Test
    fun `usb serial should not be wireless`() {
        assertFalse(isWirelessAdbConnection("R58M1234ABC"))
    }
}
