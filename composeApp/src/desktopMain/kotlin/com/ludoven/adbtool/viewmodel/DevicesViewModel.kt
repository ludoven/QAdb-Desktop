package com.ludoven.adbtool.viewmodel

import androidx.lifecycle.viewModelScope
import com.ludoven.adbtool.agent.DeviceBatteryState
import com.ludoven.adbtool.agent.DeviceConnectionType
import com.ludoven.adbtool.agent.DeviceStatusRepository
import com.ludoven.adbtool.agent.DeviceStatusRuntime
import com.ludoven.adbtool.agent.DeviceStatusSnapshot
import com.ludoven.adbtool.entity.DeviceInfoData
import com.ludoven.adbtool.entity.DeviceCenterInfoData
import com.ludoven.adbtool.entity.BatteryStatus
import com.ludoven.adbtool.entity.MsgContent
import com.ludoven.adbtool.util.AdbTool
import com.ludoven.adbtool.util.AppBehaviorPreferences
import com.ludoven.adbtool.util.AppBehaviorPreferencesStore
import com.ludoven.adbtool.util.l10n
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun deviceInfoLoadShouldApply(requestedDeviceId: String?, selectedDeviceId: String?): Boolean {
    val requested = requestedDeviceId?.trim().orEmpty()
    val selected = selectedDeviceId?.trim().orEmpty()
    return requested.isNotEmpty() && requested == selected
}

internal fun deviceInfoLoadShouldCancelForSelectionChange(
    activeLoadDeviceId: String?,
    nextSelectedDeviceId: String?
): Boolean {
    val active = activeLoadDeviceId?.trim().orEmpty()
    if (active.isEmpty()) return false
    return active != nextSelectedDeviceId?.trim().orEmpty()
}

class DevicesViewModel(
    private val deviceStatusRepository: DeviceStatusRepository = DeviceStatusRuntime.repository,
    private val behaviorPreferences: AppBehaviorPreferencesStore = AppBehaviorPreferences.store
) : BaseViewModel() {
    companion object {
        internal fun normalizedDeviceId(deviceId: String?): String? =
            deviceId?.trim()?.takeIf { it.isNotBlank() }
    }

    private val refreshTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    private val _devices = MutableStateFlow<List<String>>(emptyList())
    val devices: StateFlow<List<String>> = _devices.asStateFlow()

    private val _selectedDevice = MutableStateFlow<String?>(null)
    val selectedDevice: StateFlow<String?> = _selectedDevice.asStateFlow()

    private val _deviceDisplayNames = MutableStateFlow<Map<String, String>>(emptyMap())
    val deviceDisplayNames: StateFlow<Map<String, String>> = _deviceDisplayNames.asStateFlow()

    private val _deviceInfo = MutableStateFlow<DeviceInfoData?>(null)
    val deviceInfo: StateFlow<DeviceInfoData?> = _deviceInfo.asStateFlow()

    private val _centerInfo = MutableStateFlow<DeviceCenterInfoData?>(null)
    val centerInfo: StateFlow<DeviceCenterInfoData?> = _centerInfo.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _lastRefreshTime = MutableStateFlow("--")
    val lastRefreshTime: StateFlow<String> = _lastRefreshTime.asStateFlow()

    private var deviceInfoLoadJob: Job? = null
    private var deviceInfoLoadDeviceId: String? = null

    fun refreshDevices() {
        // Avoid re-entry: ignore duplicate refresh requests while one is active.
        if (_isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val newDevices = withContext(Dispatchers.IO) { AdbTool.getConnectedDevices() }
                _devices.value = newDevices
                _deviceDisplayNames.value = withContext(Dispatchers.IO) {
                    buildDeviceDisplayNameMap(newDevices)
                }
                val preferredDeviceId = behaviorPreferences.preferredDeviceId()
                _selectedDevice.value = when {
                    _selectedDevice.value == null && preferredDeviceId in newDevices -> preferredDeviceId
                    _selectedDevice.value == null && newDevices.isNotEmpty() -> newDevices.first()
                    _selectedDevice.value != null && _selectedDevice.value !in newDevices -> newDevices.firstOrNull()
                    else -> _selectedDevice.value
                }
                selectDevice(_selectedDevice.value)
                if (_selectedDevice.value == null) {
                    updateLastRefreshTime()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                // Keep the current state on failure and let the user choose whether to retry.
                showToast(MsgContent.Text(l10n("设备刷新失败，请重试", "Device refresh failed, try again")))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectDevice(deviceId: String?) {
        val normalizedDeviceId = normalizedDeviceId(deviceId)
        if (deviceInfoLoadShouldCancelForSelectionChange(deviceInfoLoadDeviceId, normalizedDeviceId)) {
            deviceInfoLoadJob?.cancel()
            deviceInfoLoadJob = null
            deviceInfoLoadDeviceId = null
            _isLoading.value = false
        }
        _selectedDevice.value = normalizedDeviceId
        if (normalizedDeviceId != null) {
            behaviorPreferences.recordSelectedDevice(normalizedDeviceId)
            AdbTool.selectDeviceId = normalizedDeviceId
            loadDeviceInfo(normalizedDeviceId)
        } else {
            deviceInfoLoadJob?.cancel()
            deviceInfoLoadJob = null
            deviceInfoLoadDeviceId = null
            AdbTool.selectDeviceId = null
            _deviceInfo.value = null
            _centerInfo.value = null
            updateLastRefreshTime()
        }
    }

    fun disconnectSelectedDevice() {
        val deviceId = _selectedDevice.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val result = withContext(Dispatchers.IO) { AdbTool.disconnectDevice(deviceId) }
            showToast(MsgContent.Text(if (result.success) result.output else (result.errorMessage ?: result.output)))
            refreshDevices()
            _isLoading.value = false
        }
    }

    private fun loadDeviceInfo(deviceId: String) {
        deviceInfoLoadJob?.cancel()
        deviceInfoLoadDeviceId = deviceId
        deviceInfoLoadJob = viewModelScope.launch {
            _isLoading.value = true
            try {
                val snapshot = withContext(Dispatchers.IO) {
                    deviceStatusRepository.readStatus(deviceId)
                }
                if (!deviceInfoLoadShouldApply(deviceId, _selectedDevice.value)) return@launch

                val deviceInfo = snapshot.toDeviceInfoData()
                _deviceInfo.value = deviceInfo
                _centerInfo.value = snapshot.toDeviceCenterInfoData()
                if (deviceInfo.deviceModel.isNotBlank()) {
                    _deviceDisplayNames.update { it + (deviceId to deviceInfo.deviceModel) }
                }
                updateLastRefreshTime()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } finally {
                if (deviceInfoLoadShouldApply(deviceId, _selectedDevice.value)) {
                    _isLoading.value = false
                }
            }
        }
    }

    private suspend fun buildDeviceDisplayNameMap(deviceIds: List<String>): Map<String, String> {
        if (deviceIds.isEmpty()) return emptyMap()
        val result = mutableMapOf<String, String>()
        deviceIds.forEach { deviceId ->
            val model = readDeviceModel(deviceId)
            if (model.isNotBlank()) {
                result[deviceId] = model
            }
        }
        return result
    }

    private suspend fun readDeviceModel(deviceId: String): String {
        return runCatching {
            AdbTool.execAdbOutputAsync("-s", deviceId, "shell", "getprop", "ro.product.model")
                .lineSequence()
                .firstOrNull { it.isNotBlank() }
                ?.trim()
                .orEmpty()
        }.getOrDefault("")
    }

    private fun updateLastRefreshTime() {
        _lastRefreshTime.value = LocalDateTime.now().format(refreshTimeFormatter)
    }
}

private fun DeviceStatusSnapshot.toDeviceInfoData(): DeviceInfoData {
    val resolution = display?.let { value ->
        val size = if (value.widthPx != null && value.heightPx != null) "${value.widthPx}x${value.heightPx}" else ""
        when {
            size.isNotEmpty() && value.densityDpi != null -> "$size(${value.densityDpi}dpi)"
            size.isNotEmpty() -> size
            value.densityDpi != null -> "${value.densityDpi}dpi"
            else -> ""
        }
    }.orEmpty()
    return DeviceInfoData(
        androidVersion = identity?.androidVersion.orEmpty(),
        sdkVersion = identity?.sdkVersion?.toString().orEmpty(),
        kernelVersion = identity?.kernelVersion.orEmpty(),
        deviceModel = identity?.model.orEmpty(),
        manufacturer = identity?.manufacturer.orEmpty(),
        romVersion = identity?.romVersion.orEmpty(),
        screenResolution = resolution,
        fontScale = display?.fontScale?.let(::formatFontScale).orEmpty(),
        buildFingerprint = identity?.buildFingerprint.orEmpty(),
        ipAddress = network.ipAddress.orEmpty(),
        macAddress = network.macAddress.orEmpty(),
        latency = adbLatencyMs?.let { "${it}ms" } ?: "--",
        connectionSpeed = when (network.connectionType) {
            DeviceConnectionType.USB -> "USB"
            DeviceConnectionType.WIFI -> "Wi-Fi"
        }
    )
}

private fun DeviceStatusSnapshot.toDeviceCenterInfoData(): DeviceCenterInfoData = DeviceCenterInfoData(
    cpuUsage = cpu?.usagePercent?.let(::formatPercent).orEmpty(),
    memoryUsage = memory?.usedPercent?.let(::formatPercent).orEmpty(),
    storageUsage = storage?.let { value ->
        String.format(
            Locale.US,
            "%.1f/%.1fG",
            value.usedBytes / BYTES_PER_GIBIBYTE,
            value.totalBytes / BYTES_PER_GIBIBYTE
        )
    }.orEmpty(),
    batteryLevel = battery?.levelPercent?.let { "$it%" }.orEmpty(),
    batteryStatus = when (battery?.state) {
        DeviceBatteryState.CHARGING -> BatteryStatus.CHARGING
        DeviceBatteryState.DISCHARGING, DeviceBatteryState.NOT_CHARGING -> BatteryStatus.DISCHARGING
        DeviceBatteryState.FULL -> BatteryStatus.FULL
        DeviceBatteryState.UNKNOWN, null -> BatteryStatus.UNKNOWN
    }
)

private fun formatFontScale(scale: Float): String {
    val scaleText = if (scale.toInt().toFloat() == scale) {
        scale.toInt().toString()
    } else {
        String.format(Locale.US, "%.2f", scale).trimEnd('0').trimEnd('.')
    }
    return "${scaleText}x"
}

private fun formatPercent(value: Double): String = String.format(Locale.US, "%.0f%%", value)

private const val BYTES_PER_GIBIBYTE = 1024.0 * 1024.0 * 1024.0
