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
            withContext(Dispatchers.IO) {
                val propsOutput = AdbTool.executeAdbCommand("-s", deviceId, "shell", "getprop")
                val propMap = mutableMapOf<String, String>()

                // 先把所有的 [key]: [value] 提取到 map 中
                propsOutput.lines().forEach { line ->
                    Regex("\\[(.*?)]\\s*:\\s*\\[(.*?)]").find(line)?.let { match ->
                        propMap[match.groupValues[1]] = match.groupValues[2]
                    }
                }

                // 只取你想要的字段
                info["Android 版本"] = propMap["ro.build.version.release"] ?: "未知"
                info["SDK"] = propMap["ro.build.version.sdk"] ?: "未知"
                info["设备型号"] = propMap["ro.product.model"] ?: "未知"
                info["厂商"] = propMap["ro.product.manufacturer"] ?: "未知"
                info["ROM版本"] = propMap["ro.build.display.id"] ?: "未知"
                info["Build Fingerprint"] = propMap["ro.build.fingerprint"] ?: "未知"

                // 解析 IP 和 MAC（只看 wlan0）
                val ifconfigOutput = AdbTool.executeAdbCommand("-s", deviceId, "shell", "ifconfig", "wlan0")
                val ipMatch = Regex("inet addr:([0-9.]+)").find(ifconfigOutput)
                val macMatch = Regex("HWaddr ([0-9a-fA-F:]+)").find(ifconfigOutput)
                info["IP 地址"] = ipMatch?.groupValues?.get(1) ?: "N/A"
                info["MAC 地址"] = macMatch?.groupValues?.get(1) ?: "N/A"
            }
            _deviceInfo.value = info
            _isLoading.value = false
        }
    }

}
