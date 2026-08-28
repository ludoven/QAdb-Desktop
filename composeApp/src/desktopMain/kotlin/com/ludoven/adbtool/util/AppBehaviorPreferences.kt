package com.ludoven.adbtool.util

import java.util.prefs.Preferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppBehaviorPreferencesStore(
    private val preferences: Preferences = Preferences.userNodeForPackage(AdbPathManager::class.java)
) {
    private val _autoDetectDeviceOnLaunch = MutableStateFlow(
        preferences.getBoolean(KEY_AUTO_DETECT_DEVICE_ON_LAUNCH, true)
    )
    val autoDetectDeviceOnLaunch: StateFlow<Boolean> = _autoDetectDeviceOnLaunch.asStateFlow()

    private val _rememberLastDevice = MutableStateFlow(
        preferences.getBoolean(KEY_REMEMBER_LAST_DEVICE, true)
    )
    val rememberLastDevice: StateFlow<Boolean> = _rememberLastDevice.asStateFlow()

    private val _minimizeToTrayOnLaunch = MutableStateFlow(
        preferences.getBoolean(KEY_MINIMIZE_TO_TRAY_ON_LAUNCH, false)
    )
    val minimizeToTrayOnLaunch: StateFlow<Boolean> = _minimizeToTrayOnLaunch.asStateFlow()

    private val _minimizeToTrayOnClose = MutableStateFlow(
        preferences.getBoolean(KEY_MINIMIZE_TO_TRAY_ON_CLOSE, false)
    )
    val minimizeToTrayOnClose: StateFlow<Boolean> = _minimizeToTrayOnClose.asStateFlow()

    fun setAutoDetectDeviceOnLaunch(enabled: Boolean) {
        preferences.putBoolean(KEY_AUTO_DETECT_DEVICE_ON_LAUNCH, enabled)
        _autoDetectDeviceOnLaunch.value = enabled
    }

    fun setRememberLastDevice(enabled: Boolean) {
        preferences.putBoolean(KEY_REMEMBER_LAST_DEVICE, enabled)
        if (!enabled) preferences.remove(KEY_LAST_DEVICE_ID)
        _rememberLastDevice.value = enabled
    }

    fun setMinimizeToTrayOnLaunch(enabled: Boolean) {
        preferences.putBoolean(KEY_MINIMIZE_TO_TRAY_ON_LAUNCH, enabled)
        _minimizeToTrayOnLaunch.value = enabled
    }

    fun setMinimizeToTrayOnClose(enabled: Boolean) {
        preferences.putBoolean(KEY_MINIMIZE_TO_TRAY_ON_CLOSE, enabled)
        _minimizeToTrayOnClose.value = enabled
    }

    fun preferredDeviceId(): String? = if (_rememberLastDevice.value) {
        preferences.get(KEY_LAST_DEVICE_ID, null)?.trim()?.takeIf(String::isNotEmpty)
    } else {
        null
    }

    fun recordSelectedDevice(deviceId: String?) {
        if (!_rememberLastDevice.value) return
        val normalized = deviceId?.trim().orEmpty()
        if (normalized.isNotEmpty()) {
            preferences.put(KEY_LAST_DEVICE_ID, normalized)
        }
    }

    private companion object {
        const val KEY_AUTO_DETECT_DEVICE_ON_LAUNCH = "setting.auto_detect_device_on_launch"
        const val KEY_REMEMBER_LAST_DEVICE = "setting.remember_last_device"
        const val KEY_MINIMIZE_TO_TRAY_ON_LAUNCH = "setting.minimize_to_tray_on_launch"
        const val KEY_MINIMIZE_TO_TRAY_ON_CLOSE = "setting.minimize_to_tray_on_close"
        const val KEY_LAST_DEVICE_ID = "setting.last_device_id"
    }
}

object AppBehaviorPreferences {
    val store: AppBehaviorPreferencesStore by lazy { AppBehaviorPreferencesStore() }
}
