package com.ludoven.adbtool.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ludoven.adbtool.util.AdbTool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DevicesViewModel : ViewModel() {

    private val _devices = MutableStateFlow<List<String>>(emptyList())
    val devices: StateFlow<List<String>> = _devices.asStateFlow()

    private val _selectedDevice = MutableStateFlow<String?>(null)
    val selectedDevice: StateFlow<String?> = _selectedDevice.asStateFlow()

    private val _deviceInfo = MutableStateFlow<Map<String, String>>(emptyMap())
    val deviceInfo: StateFlow<Map<String, String>> = _deviceInfo.asStateFlow()

    private val _centerInfo = MutableStateFlow<Map<String, String>>(emptyMap())
    val centerInfo: StateFlow<Map<String, String>> = _centerInfo.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun refreshDevices() {
        viewModelScope.launch {
            _isLoading.value = true
            val newDevices = withContext(Dispatchers.IO) { AdbTool.getConnectedDevices() }
            _devices.value = newDevices
            _selectedDevice.value = when {
                _selectedDevice.value == null && newDevices.isNotEmpty() -> newDevices.first()
                _selectedDevice.value != null && _selectedDevice.value !in newDevices -> newDevices.firstOrNull()
                else -> _selectedDevice.value
            }
            selectDevice(_selectedDevice.value)
            _isLoading.value = false
        }
    }

    fun selectDevice(deviceId: String?) {
        _selectedDevice.value = deviceId
        if (deviceId != null) {
            AdbTool.selectDeviceId = deviceId
            loadDeviceInfo(deviceId)
        } else {
            _deviceInfo.value = emptyMap()
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
            val info = mutableMapOf<String, String>()
            val centerInfo = mutableMapOf<String, String>()

            withContext(Dispatchers.IO) {
                val propsOutput = AdbTool.executeAdbCommand("-s", deviceId, "shell", "getprop")
                val propMap = mutableMapOf<String, String>()

                propsOutput.lines().forEach { line ->
                    Regex("\\[(.*?)]\\s*:\\s*\\[(.*?)]").find(line)?.let { match ->
                        propMap[match.groupValues[1]] = match.groupValues[2]
                    }
                }

                info["Android 版本"] = propMap["ro.build.version.release"] ?: "未知"
                info["SDK"] = propMap["ro.build.version.sdk"] ?: "未知"
                info["设备型号"] = propMap["ro.product.model"] ?: "未知"
                info["厂商"] = propMap["ro.product.manufacturer"] ?: "未知"
                info["ROM版本"] = propMap["ro.build.display.id"] ?: "未知"
                info["Build Fingerprint"] = propMap["ro.build.fingerprint"] ?: "未知"

                // IP 和 MAC
                val ifconfigOutput = AdbTool.executeAdbCommand("-s", deviceId, "shell", "ip addr show wlan0")
                val ipMatch = Regex("inet ([0-9.]+)").find(ifconfigOutput)
                val macMatch = Regex("link/ether ([0-9a-f:]+)").find(ifconfigOutput)
                info["IP 地址"] = ipMatch?.groupValues?.get(1) ?: "N/A"
                info["MAC 地址"] = macMatch?.groupValues?.get(1) ?: "N/A"

                // CPU 使用率（解析 idle 和总时间计算）
                val cpuStatOutput = AdbTool.executeAdbCommand("-s", deviceId, "shell", "cat", "/proc/stat")
                val cpuLine = cpuStatOutput.lines().firstOrNull { it.startsWith("cpu ") }
                cpuLine?.let {
                    val values = it.split(Regex("\\s+")).drop(1).mapNotNull { it.toLongOrNull() }
                    if (values.size >= 7) {
                        val idle = values[3] + values[4]
                        val total = values.sum()
                        // 简单快照不太准，可以两次取值做差更准，这里简单用 snapshot
                        val usagePercent = 100 - ((idle * 100) / total)
                        centerInfo["CPU"] = "$usagePercent%"
                    } else {
                        centerInfo["CPU"] = "未知"
                    }
                } ?: run {
                    centerInfo["CPU"] = "未知"
                }

                // 内存使用率
                val memOutput = AdbTool.executeAdbCommand("-s", deviceId, "shell", "cat", "/proc/meminfo")
                val totalMem = Regex("MemTotal:\\s+(\\d+)").find(memOutput)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                val freeMem = Regex("MemAvailable:\\s+(\\d+)").find(memOutput)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                if (totalMem > 0) {
                    val usedPercent = ((totalMem - freeMem) * 100 / totalMem)
                    centerInfo["内存"] = "$usedPercent%"
                } else {
                    centerInfo["内存"] = "未知"
                }

                // 存储空间（剩余/总共, 单位 GB）
                val dfOutput = AdbTool.executeAdbCommand("-s", deviceId, "shell", "df", "/data")
                val storageLine = dfOutput.lines().drop(1).firstOrNull()
                val storageParts = storageLine?.split(Regex("\\s+")) ?: emptyList()
                if (storageParts.size >= 5) {
                    val total = storageParts[1].toLongOrNull() ?: 0L
                    val used = storageParts[2].toLongOrNull() ?: 0L
                    val available = storageParts[3].toLongOrNull() ?: 0L
                    val totalGB = total / 1024.0 / 1024.0
                    val availableGB = available / 1024.0 / 1024.0
                    centerInfo["存储"] = String.format("%.1fG / %.1fG", (totalGB - availableGB), totalGB)
                } else {
                    centerInfo["存储"] = "未知"
                }

                // 电量
                val batteryOutput = AdbTool.executeAdbCommand("-s", deviceId, "shell", "dumpsys", "battery")
                val level = Regex("level: (\\d+)").find(batteryOutput)?.groupValues?.get(1)
                val status = Regex("status: (\\d+)").find(batteryOutput)?.groupValues?.get(1)
                val charging = when (status) {
                    "2" -> "正在充电"
                    "3" -> "放电中"
                    "5" -> "已充满"
                    else -> "未知"
                }
                centerInfo["电量"] = "$level%（$charging）"
            }

            _deviceInfo.value = info
            _centerInfo.value = centerInfo
            _isLoading.value = false
        }
    }
}
