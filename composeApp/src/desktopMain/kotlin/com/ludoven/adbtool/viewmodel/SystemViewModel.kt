package com.ludoven.adbtool.viewmodel

import androidx.lifecycle.viewModelScope
import com.ludoven.adbtool.entity.MsgContent
import com.ludoven.adbtool.util.AdbTool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun systemInfoLoadShouldApply(requestedDeviceId: String?, selectedDeviceId: String?): Boolean {
    val requested = requestedDeviceId?.trim().orEmpty()
    val selected = selectedDeviceId?.trim().orEmpty()
    return requested.isNotEmpty() && requested == selected
}

internal fun systemInfoLoadShouldCancelForDeviceChange(activeLoadDeviceId: String?, nextSelectedDeviceId: String?): Boolean {
    val active = activeLoadDeviceId?.trim().orEmpty()
    if (active.isEmpty()) return false
    return active != nextSelectedDeviceId?.trim().orEmpty()
}

@OptIn(FlowPreview::class)
class SystemViewModel : BaseViewModel() {
    companion object {
        internal fun systemDeviceActionsEnabled(deviceId: String?): Boolean =
            !deviceId.isNullOrBlank()
    }

    private val propRegex = Regex("\\[(.*?)]\\s*:\\s*\\[(.*?)]")

    private val _selectedDevice = MutableStateFlow<String?>(null)
    val selectedDevice: StateFlow<String?> = _selectedDevice.asStateFlow()

    private val _systemProps = MutableStateFlow<Map<String, String>>(emptyMap())
    val systemProps: StateFlow<Map<String, String>> = _systemProps.asStateFlow()

    private val _batteryInfo = MutableStateFlow<Map<String, String>>(emptyMap())
    val batteryInfo: StateFlow<Map<String, String>> = _batteryInfo.asStateFlow()

    private val _cpuInfo = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val cpuInfo: StateFlow<List<Pair<String, String>>> = _cpuInfo.asStateFlow()

    private val _screenInfo = MutableStateFlow<Map<String, String>>(emptyMap())
    val screenInfo: StateFlow<Map<String, String>> = _screenInfo.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _propSearchText = MutableStateFlow("")
    val propSearchText: StateFlow<String> = _propSearchText.asStateFlow()

    private var systemInfoLoadJob: Job? = null
    private var systemInfoLoadDeviceId: String? = null
    private var loadedSystemInfoDeviceId: String? = null

    val filteredProps: StateFlow<Map<String, String>> = combine(
        _systemProps,
        _propSearchText.debounce(300L)
    ) { props, searchText ->
        if (searchText.isBlank()) {
            props
        } else {
            val lowerSearch = searchText.lowercase()
            props.filter { (key, value) ->
                key.lowercase().contains(lowerSearch) || value.lowercase().contains(lowerSearch)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun setDevice(deviceId: String?) {
        val normalizedDeviceId = deviceId?.trim()?.takeIf { it.isNotBlank() }
        val changed = _selectedDevice.value != normalizedDeviceId
        if (systemInfoLoadShouldCancelForDeviceChange(systemInfoLoadDeviceId, normalizedDeviceId)) {
            systemInfoLoadJob?.cancel()
            systemInfoLoadJob = null
            systemInfoLoadDeviceId = null
            _isLoading.value = false
        }
        _selectedDevice.value = normalizedDeviceId
        if (_selectedDevice.value == null) {
            systemInfoLoadJob?.cancel()
            systemInfoLoadJob = null
            systemInfoLoadDeviceId = null
            loadedSystemInfoDeviceId = null
            clearDeviceScopedData()
        } else if (changed) {
            loadedSystemInfoDeviceId = null
            clearDeviceScopedData()
        }
    }

    fun updatePropSearch(text: String) {
        _propSearchText.value = text
    }

    fun loadSystemInfo(deviceId: String, forceRefresh: Boolean = false) {
        val normalizedDeviceId = deviceId.trim()
        if (!systemDeviceActionsEnabled(normalizedDeviceId)) {
            systemInfoLoadJob?.cancel()
            systemInfoLoadJob = null
            systemInfoLoadDeviceId = null
            loadedSystemInfoDeviceId = null
            _isLoading.value = false
            return
        }
        if (!forceRefresh && systemInfoLoadDeviceId == normalizedDeviceId && systemInfoLoadJob?.isActive == true) {
            return
        }
        if (!forceRefresh && loadedSystemInfoDeviceId == normalizedDeviceId) {
            _isLoading.value = false
            return
        }
        systemInfoLoadJob?.cancel()
        systemInfoLoadDeviceId = normalizedDeviceId
        systemInfoLoadJob = viewModelScope.launch {
            _isLoading.value = true

            try {
                withContext(Dispatchers.IO) {
                    val propsDeferred = async { AdbTool.execAdbOutputAsync("-s", normalizedDeviceId, "shell", "getprop") }
                    val batteryDeferred = async { AdbTool.execAdbOutputAsync("-s", normalizedDeviceId, "shell", "dumpsys", "battery") }
                    val cpuDeferred = async { AdbTool.execAdbOutputAsync("-s", normalizedDeviceId, "shell", "cat", "/proc/cpuinfo") }
                    val screenSizeDeferred = async { AdbTool.execAdbOutputAsync("-s", normalizedDeviceId, "shell", "wm", "size") }
                    val screenDensityDeferred = async { AdbTool.execAdbOutputAsync("-s", normalizedDeviceId, "shell", "wm", "density") }

                    val propsOutput = propsDeferred.await()
                    val batteryOutput = batteryDeferred.await()
                    val cpuOutput = cpuDeferred.await()
                    val screenSizeOutput = screenSizeDeferred.await()
                    val screenDensityOutput = screenDensityDeferred.await()

                    if (!systemInfoLoadShouldApply(normalizedDeviceId, _selectedDevice.value)) return@withContext
                    loadedSystemInfoDeviceId = normalizedDeviceId
                    _systemProps.value = parseSystemProps(propsOutput)
                    _batteryInfo.value = parseBatteryInfo(batteryOutput)
                    _cpuInfo.value = parseCpuInfo(cpuOutput)
                    _screenInfo.value = parseScreenInfo(screenSizeOutput, screenDensityOutput)
                }
            } catch (e: Exception) {
                showTipDialog(MsgContent.Text("Failed to load system info: ${e.message}"), autoDismiss = true)
            }

            if (systemInfoLoadShouldApply(normalizedDeviceId, _selectedDevice.value)) {
                _isLoading.value = false
            }
        }
    }

    fun cancelActiveLoad() {
        systemInfoLoadJob?.cancel()
        systemInfoLoadJob = null
        systemInfoLoadDeviceId = null
        _isLoading.value = false
    }

    fun rebootNormal(deviceId: String) {
        if (!systemDeviceActionsEnabled(deviceId)) return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                AdbTool.execAdbOutputAsync("-s", deviceId, "reboot")
            }
            showTipDialog(
                MsgContent.Text(if (result.isBlank()) "Reboot command sent successfully" else result),
                autoDismiss = true
            )
        }
    }

    fun rebootRecovery(deviceId: String) {
        if (!systemDeviceActionsEnabled(deviceId)) return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                AdbTool.execAdbOutputAsync("-s", deviceId, "reboot", "recovery")
            }
            showTipDialog(
                MsgContent.Text(if (result.isBlank()) "Reboot to recovery command sent successfully" else result),
                autoDismiss = true
            )
        }
    }

    fun rebootBootloader(deviceId: String) {
        if (!systemDeviceActionsEnabled(deviceId)) return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                AdbTool.execAdbOutputAsync("-s", deviceId, "reboot", "bootloader")
            }
            showTipDialog(
                MsgContent.Text(if (result.isBlank()) "Reboot to bootloader command sent successfully" else result),
                autoDismiss = true
            )
        }
    }

    fun setScreenSize(deviceId: String, size: String) {
        if (!systemDeviceActionsEnabled(deviceId)) return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                AdbTool.execAdbOutputAsync("-s", deviceId, "shell", "wm", "size", size)
            }
            showTipDialog(
                MsgContent.Text(if (result.isBlank()) "Screen size set to $size" else result),
                autoDismiss = true
            )
        }
    }

    fun resetScreenSize(deviceId: String) {
        if (!systemDeviceActionsEnabled(deviceId)) return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                AdbTool.execAdbOutputAsync("-s", deviceId, "shell", "wm", "size", "reset")
            }
            showTipDialog(
                MsgContent.Text(if (result.isBlank()) "Screen size reset to default" else result),
                autoDismiss = true
            )
        }
    }

    fun setScreenDensity(deviceId: String, density: String) {
        if (!systemDeviceActionsEnabled(deviceId)) return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                AdbTool.execAdbOutputAsync("-s", deviceId, "shell", "wm", "density", density)
            }
            showTipDialog(
                MsgContent.Text(if (result.isBlank()) "Screen density set to ${density}dpi" else result),
                autoDismiss = true
            )
        }
    }

    fun resetScreenDensity(deviceId: String) {
        if (!systemDeviceActionsEnabled(deviceId)) return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                AdbTool.execAdbOutputAsync("-s", deviceId, "shell", "wm", "density", "reset")
            }
            showTipDialog(
                MsgContent.Text(if (result.isBlank()) "Screen density reset to default" else result),
                autoDismiss = true
            )
        }
    }

    private fun parseSystemProps(output: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        output.lines().forEach { line ->
            propRegex.find(line)?.let { match ->
                result[match.groupValues[1]] = match.groupValues[2]
            }
        }
        return result
    }

    private fun parseBatteryInfo(output: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        output.lines().forEach { line ->
            val trimmed = line.trim()
            val colonIndex = trimmed.indexOf(':')
            if (colonIndex > 0) {
                val key = trimmed.substring(0, colonIndex).trim()
                val value = trimmed.substring(colonIndex + 1).trim()
                if (key.isNotEmpty() && value.isNotEmpty()) {
                    result[key] = value
                }
            }
        }
        return result
    }

    private fun parseCpuInfo(output: String): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        val entries = output.split(Regex("\\n\\s*\\n"))
        entries.forEach { entry ->
            val parsed = mutableListOf<Pair<String, String>>()
            entry.lines().forEach { line ->
                val colonIndex = line.indexOf(':')
                if (colonIndex > 0) {
                    val key = line.substring(0, colonIndex).trim()
                    val value = line.substring(colonIndex + 1).trim()
                    if (key.isNotEmpty()) {
                        parsed.add(key to value)
                    }
                }
            }
            if (parsed.isNotEmpty()) {
                result.addAll(parsed)
                result.add("" to "")
            }
        }
        return result
    }

    private fun parseScreenInfo(sizeOutput: String, densityOutput: String): Map<String, String> {
        val result = mutableMapOf<String, String>()

        val overrideSize = Regex("Override size:\\s*([^\\n\\r]+)").find(sizeOutput)
            ?.groupValues?.get(1)?.trim()
        val physicalSize = Regex("Physical size:\\s*([^\\n\\r]+)").find(sizeOutput)
            ?.groupValues?.get(1)?.trim()

        val overrideDensity = Regex("Override density:\\s*([^\\n\\r]+)").find(densityOutput)
            ?.groupValues?.get(1)?.trim()
        val physicalDensity = Regex("Physical density:\\s*([^\\n\\r]+)").find(densityOutput)
            ?.groupValues?.get(1)?.trim()

        result["Physical size"] = physicalSize ?: ""
        if (!overrideSize.isNullOrBlank()) {
            result["Override size"] = overrideSize
        }
        result["Physical density"] = physicalDensity?.let { "${it}dpi" } ?: ""
        if (!overrideDensity.isNullOrBlank()) {
            result["Override density"] = "${overrideDensity}dpi"
        }

        return result
    }

    private fun clearDeviceScopedData() {
        _systemProps.value = emptyMap()
        _batteryInfo.value = emptyMap()
        _cpuInfo.value = emptyList()
        _screenInfo.value = emptyMap()
        _isLoading.value = false
    }

    internal fun replaceDeviceScopedDataForTest(
        systemProps: Map<String, String>,
        batteryInfo: Map<String, String>,
        cpuInfo: List<Pair<String, String>>,
        screenInfo: Map<String, String>
    ) {
        _systemProps.value = systemProps
        _batteryInfo.value = batteryInfo
        _cpuInfo.value = cpuInfo
        _screenInfo.value = screenInfo
        _isLoading.value = true
    }
}
