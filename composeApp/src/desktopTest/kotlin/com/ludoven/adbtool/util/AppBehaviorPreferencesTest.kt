package com.ludoven.adbtool.util

import java.util.UUID
import java.util.prefs.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppBehaviorPreferencesTest {
    @Test
    fun `window and startup preferences persist with safe defaults`() {
        val node = Preferences.userRoot().node("/qadb-tests/app-behavior/${UUID.randomUUID()}")
        try {
            val preferences = AppBehaviorPreferencesStore(node)
            assertTrue(preferences.autoDetectDeviceOnLaunch.value)
            assertTrue(preferences.rememberLastDevice.value)
            assertFalse(preferences.minimizeToTrayOnLaunch.value)
            assertFalse(preferences.minimizeToTrayOnClose.value)

            preferences.setAutoDetectDeviceOnLaunch(false)
            preferences.setMinimizeToTrayOnLaunch(true)
            preferences.setMinimizeToTrayOnClose(true)

            val restored = AppBehaviorPreferencesStore(node)
            assertFalse(restored.autoDetectDeviceOnLaunch.value)
            assertTrue(restored.minimizeToTrayOnLaunch.value)
            assertTrue(restored.minimizeToTrayOnClose.value)
        } finally {
            node.removeNode()
        }
    }

    @Test
    fun `remembered device is restored only while preference is enabled`() {
        val node = Preferences.userRoot().node("/qadb-tests/app-behavior-device/${UUID.randomUUID()}")
        try {
            val preferences = AppBehaviorPreferencesStore(node)
            preferences.recordSelectedDevice(" emulator-5554 ")
            assertEquals("emulator-5554", preferences.preferredDeviceId())

            preferences.setRememberLastDevice(false)
            assertNull(preferences.preferredDeviceId())

            preferences.recordSelectedDevice("device-2")
            preferences.setRememberLastDevice(true)
            assertNull(preferences.preferredDeviceId())
        } finally {
            node.removeNode()
        }
    }
}
