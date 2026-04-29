package com.ludoven.adbtool.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ludoven.adbtool.util.AdbTool
import com.ludoven.adbtool.entity.DeviceInfoData
import com.ludoven.adbtool.entity.DeviceCenterInfoData
import com.ludoven.adbtool.entity.BatteryStatus
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DevicesViewModel : ViewModel() {
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

    fun refreshDevices() {
        viewModelScope.launch {
            _isLoading.value = true
            val newDevices = withContext(Dispatchers.IO) { AdbTool.getConnectedDevices() }
            _devices.value = newDevices
            _deviceDisplayNames.value = withContext(Dispatchers.IO) {
                buildDeviceDisplayNameMap(newDevices)
            }
            _selectedDevice.value = when {
                _selectedDevice.value == null && newDevices.isNotEmpty() -> newDevices.first()
                _selectedDevice.value != null && _selectedDevice.value !in newDevices -> newDevices.firstOrNull()
                else -> _selectedDevice.value
            }
            selectDevice(_selectedDevice.value)
            if (_selectedDevice.value == null) {
                updateLastRefreshTime()
            }
            _isLoading.value = false
        }
    }

    fun selectDevice(deviceId: String?) {
        _selectedDevice.value = deviceId
        if (deviceId != null) {
            AdbTool.selectDeviceId = deviceId
            loadDeviceInfo(deviceId)
        } else {
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
            println("断开设备 $deviceId 的结果: $result")
            refreshDevices()
            _isLoading.value = false
        }
    }

    private fun loadDeviceInfo(deviceId: String) {
        viewModelScope.launch {
            _isLoading.value = true

            withContext(Dispatchers.IO) {
                val propsOutput = AdbTool.executeAdbCommand("-s", deviceId, "shell", "getprop")
                val propMap = mutableMapOf<String, String>()

                propsOutput.lines().forEach { line ->
                    Regex("\\[(.*?)]\\s*:\\s*\\[(.*?)]")
                        .find(line)?.let { match ->
                        propMap[match.groupValues[1]] = match.groupValues[2]
                    }
                }

                val androidVersion = propMap["ro.build.version.release"] ?: ""
                val sdkVersion = propMap["ro.build.version.sdk"] ?: ""
                val deviceModel = propMap["ro.product.model"] ?: ""
                val manufacturer = propMap["ro.product.manufacturer"] ?: ""
                val romVersion = propMap["ro.build.display.id"] ?: ""
                val buildFingerprint = propMap["ro.build.fingerprint"] ?: ""
                val kernelVersion = AdbTool.executeAdbCommand("-s", deviceId, "shell", "uname", "-r")
                    .lineSequence()
                    .firstOrNull { it.isNotBlank() }
                    ?.trim()
                    .orEmpty()
                val screenResolution = formatScreenResolution(
                    sizeOutput = AdbTool.executeAdbCommand("-s", deviceId, "shell", "wm", "size"),
                    densityOutput = AdbTool.executeAdbCommand("-s", deviceId, "shell", "wm", "density")
                )
                val fontScale = formatFontScale(
                    AdbTool.executeAdbCommand("-s", deviceId, "shell", "settings", "get", "system", "font_scale")
                )

                // IP 和 MAC
                val ifconfigOutput = AdbTool.executeAdbCommand("-s", deviceId, "shell", "ip addr show wlan0")
                val ipMatch = Regex("inet ([0-9.]+)").find(ifconfigOutput)
                val macMatch = Regex("link/ether ([0-9a-f:]+)").find(ifconfigOutput)
                val ipAddress = ipMatch?.groupValues?.get(1) ?: ""
                val macAddress = macMatch?.groupValues?.get(1) ?: ""

                // CPU 使用率（解析 idle 和总时间计算）
                val cpuStatOutput = AdbTool.executeAdbCommand("-s", deviceId, "shell", "cat", "/proc/stat")
                val cpuLine = cpuStatOutput.lines().firstOrNull { it.startsWith("cpu ") }
                val cpuUsage = cpuLine?.let {
                    val values = it.split(Regex("\\s+")).drop(1).mapNotNull { it.toLongOrNull() }
                    if (values.size >= 7) {
                        val idle = values[3] + values[4]
                        val total = values.sum()
                        // 简单快照不太准，可以两次取值做差更准，这里简单用 snapshot
                        val usagePercent = 100 - ((idle * 100) / total)
                        "$usagePercent%"
                    } else {
                        ""
                    }
                } ?: ""

                // 内存使用率
                val memOutput = AdbTool.executeAdbCommand("-s", deviceId, "shell", "cat", "/proc/meminfo")
                val totalMem = Regex("MemTotal:\\s+(\\d+)").find(memOutput)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                val freeMem = Regex("MemAvailable:\\s+(\\d+)").find(memOutput)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                val memoryUsage = if (totalMem > 0) {
                    val usedPercent = ((totalMem - freeMem) * 100 / totalMem)
                    "$usedPercent%"
                } else {
                    ""
                }

                // 存储空间（剩余/总共, 单位 GB）
                val dfOutput = AdbTool.executeAdbCommand("-s", deviceId, "shell", "df", "/data")
                val storageLine = dfOutput.lines().drop(1).firstOrNull()
                val storageParts = storageLine?.split(Regex("\\s+")) ?: emptyList()
                val storageUsage = if (storageParts.size >= 5) {
                    val total = storageParts[1].toLongOrNull() ?: 0L
                    val used = storageParts[2].toLongOrNull() ?: 0L
                    val totalGB = total / 1024.0 / 1024.0
                    val usedGB = used / 1024.0 / 1024.0
                    String.format(Locale.US, "%.1f/%.1fG", usedGB, totalGB)
                } else {
                    ""
                }

                // 电量
                val batteryOutput = AdbTool.executeAdbCommand("-s", deviceId, "shell", "dumpsys", "battery")
                val level = Regex("level: (\\d+)").find(batteryOutput)?.groupValues?.get(1)
                val status = Regex("status: (\\d+)").find(batteryOutput)?.groupValues?.get(1)
                val batteryStatus = BatteryStatus.fromStatusCode(status)
                val batteryLevel = if (level != null) "$level%" else ""

                // 创建数据对象
                val deviceInfo = DeviceInfoData(
                    androidVersion = androidVersion,
                    sdkVersion = sdkVersion,
                    kernelVersion = kernelVersion,
                    deviceModel = deviceModel,
                    manufacturer = manufacturer,
                    romVersion = romVersion,
                    screenResolution = screenResolution,
                    fontScale = fontScale,
                    buildFingerprint = buildFingerprint,
                    ipAddress = ipAddress,
                    macAddress = macAddress
                )

                val centerInfo = DeviceCenterInfoData(
                    cpuUsage = cpuUsage,
                    memoryUsage = memoryUsage,
                    storageUsage = storageUsage,
                    batteryLevel = batteryLevel,
                    batteryStatus = batteryStatus
                )

                _deviceInfo.value = deviceInfo
                _centerInfo.value = centerInfo
                if (deviceModel.isNotBlank()) {
                    _deviceDisplayNames.update { it + (deviceId to deviceModel) }
                }
                updateLastRefreshTime()
            }

            _isLoading.value = false
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
            AdbTool.executeAdbCommand("-s", deviceId, "shell", "getprop", "ro.product.model")
                .lineSequence()
                .firstOrNull { it.isNotBlank() }
                ?.trim()
                .orEmpty()
        }.getOrDefault("")
    }

    private fun formatScreenResolution(sizeOutput: String, densityOutput: String): String {
        val size = parseWmMetric(sizeOutput, metric = "size")
        val density = parseWmMetric(densityOutput, metric = "density")
        return when {
            size.isNotBlank() && density.isNotBlank() -> "$size(${density}dpi)"
            size.isNotBlank() -> size
            else -> ""
        }
    }

    private fun parseWmMetric(output: String, metric: String): String {
        val override = Regex("Override $metric:\\s*([^\\n\\r]+)").find(output)?.groupValues?.get(1)?.trim()
        if (!override.isNullOrBlank()) return override
        return Regex("Physical $metric:\\s*([^\\n\\r]+)").find(output)?.groupValues?.get(1)?.trim().orEmpty()
    }

    private fun formatFontScale(rawScale: String): String {
        val normalizedRaw = rawScale.trim()
        if (normalizedRaw.isBlank() || normalizedRaw.equals("null", ignoreCase = true)) return ""
        val scale = normalizedRaw.toFloatOrNull() ?: return normalizedRaw
        val scaleText = if (scale.toInt().toFloat() == scale) {
            scale.toInt().toString()
        } else {
            String.format(Locale.US, "%.2f", scale).trimEnd('0').trimEnd('.')
        }
        return "${scaleText}x"
    }

    private fun updateLastRefreshTime() {
        _lastRefreshTime.value = LocalDateTime.now().format(refreshTimeFormatter)
    }
}
