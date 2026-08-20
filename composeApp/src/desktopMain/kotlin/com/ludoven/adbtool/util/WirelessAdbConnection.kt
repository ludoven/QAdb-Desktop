package com.ludoven.adbtool.util

import java.util.prefs.Preferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class WirelessAdbOperation {
    CONNECT,
    PAIR
}

enum class WirelessAdbIssue {
    EMPTY_ADDRESS,
    INVALID_ADDRESS,
    PORT_REQUIRED,
    INVALID_PORT,
    INVALID_PAIRING_CODE,
    TIMEOUT,
    UNREACHABLE,
    AUTHENTICATION,
    NAME_RESOLUTION,
    COMMAND_FAILED
}

data class WirelessAdbOperationResult(
    val operation: WirelessAdbOperation,
    val success: Boolean,
    val endpoint: String? = null,
    val issue: WirelessAdbIssue? = null,
    val detail: String = ""
)

internal data class WirelessEndpointValidation(
    val endpoint: String? = null,
    val issue: WirelessAdbIssue? = null
) {
    val isValid: Boolean get() = endpoint != null && issue == null
}

private const val DEFAULT_WIRELESS_ADB_PORT = 5555
private const val MAX_WIRELESS_HISTORY_SIZE = 5
private const val HISTORY_KEY = "wireless.connection.history"
private val wirelessHostRegex = Regex("^[A-Za-z0-9][A-Za-z0-9._-]*$")
private val wirelessIpv6HostRegex = Regex("^\\[[0-9A-Fa-f:.]+(?:%[A-Za-z0-9._-]+)?]$")
private val pairingCodeRegex = Regex("^\\d{6}$")

internal fun validateWirelessEndpoint(
    input: String,
    requirePort: Boolean
): WirelessEndpointValidation {
    val value = input.trim()
    if (value.isEmpty()) return WirelessEndpointValidation(issue = WirelessAdbIssue.EMPTY_ADDRESS)
    if (value.any(Char::isWhitespace)) {
        return WirelessEndpointValidation(issue = WirelessAdbIssue.INVALID_ADDRESS)
    }

    val host: String
    val portText: String?
    if (value.startsWith("[")) {
        val closingBracket = value.indexOf(']')
        if (closingBracket <= 1) {
            return WirelessEndpointValidation(issue = WirelessAdbIssue.INVALID_ADDRESS)
        }
        host = value.substring(0, closingBracket + 1)
        if (!wirelessIpv6HostRegex.matches(host) || ':' !in host) {
            return WirelessEndpointValidation(issue = WirelessAdbIssue.INVALID_ADDRESS)
        }
        val suffix = value.substring(closingBracket + 1)
        portText = when {
            suffix.isEmpty() -> null
            suffix.startsWith(":") && suffix.length > 1 -> suffix.substring(1)
            else -> return WirelessEndpointValidation(issue = WirelessAdbIssue.INVALID_ADDRESS)
        }
    } else {
        if (value.count { it == ':' } > 1) {
            return WirelessEndpointValidation(issue = WirelessAdbIssue.INVALID_ADDRESS)
        }
        val separatorIndex = value.lastIndexOf(':')
        host = if (separatorIndex >= 0) value.substring(0, separatorIndex) else value
        portText = if (separatorIndex >= 0) value.substring(separatorIndex + 1) else null
        if (!wirelessHostRegex.matches(host)) {
            return WirelessEndpointValidation(issue = WirelessAdbIssue.INVALID_ADDRESS)
        }
    }

    if (requirePort && portText == null) {
        return WirelessEndpointValidation(issue = WirelessAdbIssue.PORT_REQUIRED)
    }
    if (portText != null && (portText.isEmpty() || !portText.all(Char::isDigit))) {
        return WirelessEndpointValidation(issue = WirelessAdbIssue.INVALID_PORT)
    }
    val port = portText?.toIntOrNull() ?: DEFAULT_WIRELESS_ADB_PORT
    if (port !in 1..65535) {
        return WirelessEndpointValidation(issue = WirelessAdbIssue.INVALID_PORT)
    }
    return WirelessEndpointValidation(endpoint = "$host:$port")
}

internal fun validatePairingCode(code: String): WirelessAdbIssue? =
    if (pairingCodeRegex.matches(code.trim())) null else WirelessAdbIssue.INVALID_PAIRING_CODE

internal fun interpretWirelessAdbResult(
    operation: WirelessAdbOperation,
    endpoint: String,
    adbResult: AdbTool.AdbResult
): WirelessAdbOperationResult {
    val detail = listOf(adbResult.output, adbResult.errorMessage)
        .filter { !it.isNullOrBlank() }
        .joinToString("\n")
        .trim()
    val normalized = detail.lowercase()
    val successMarker = when (operation) {
        WirelessAdbOperation.CONNECT -> normalized.contains("connected to") ||
            normalized.contains("already connected")
        WirelessAdbOperation.PAIR -> normalized.contains("successfully paired") ||
            normalized.contains("already paired")
    }
    val failureMarker = normalized.contains("failed") ||
        normalized.contains("unable") ||
        normalized.contains("cannot") ||
        normalized.contains("timed out") ||
        normalized.contains("no route")

    if ((adbResult.success && !failureMarker) || successMarker) {
        return WirelessAdbOperationResult(
            operation = operation,
            success = true,
            endpoint = endpoint,
            detail = detail
        )
    }

    val issue = when {
        normalized.contains("timed out") || normalized.contains("timeout") -> WirelessAdbIssue.TIMEOUT
        normalized.contains("no route") || normalized.contains("refused") ||
            normalized.contains("unable to connect") -> WirelessAdbIssue.UNREACHABLE
        normalized.contains("authenticate") || normalized.contains("authentication") ->
            WirelessAdbIssue.AUTHENTICATION
        normalized.contains("resolve") || normalized.contains("unknown host") ->
            WirelessAdbIssue.NAME_RESOLUTION
        else -> WirelessAdbIssue.COMMAND_FAILED
    }
    return WirelessAdbOperationResult(
        operation = operation,
        success = false,
        endpoint = endpoint,
        issue = issue,
        detail = detail
    )
}

internal fun decodeWirelessConnectionHistory(raw: String?): List<String> = raw
    .orEmpty()
    .lineSequence()
    .map(String::trim)
    .filter(String::isNotEmpty)
    .distinct()
    .take(MAX_WIRELESS_HISTORY_SIZE)
    .toList()

internal fun encodeWirelessConnectionHistory(endpoints: List<String>): String = endpoints
    .map(String::trim)
    .filter(String::isNotEmpty)
    .distinct()
    .take(MAX_WIRELESS_HISTORY_SIZE)
    .joinToString("\n")

class WirelessConnectionHistoryStore(
    private val preferences: Preferences = Preferences.userNodeForPackage(WirelessConnectionHistoryStore::class.java)
) {
    private val _endpoints = MutableStateFlow(
        decodeWirelessConnectionHistory(preferences.get(HISTORY_KEY, ""))
    )
    val endpoints: StateFlow<List<String>> = _endpoints.asStateFlow()

    fun recordSuccessful(endpoint: String) {
        update(listOf(endpoint) + _endpoints.value.filterNot { it == endpoint })
    }

    fun remove(endpoint: String) {
        update(_endpoints.value.filterNot { it == endpoint })
    }

    private fun update(endpoints: List<String>) {
        val normalized = decodeWirelessConnectionHistory(encodeWirelessConnectionHistory(endpoints))
        _endpoints.value = normalized
        runCatching {
            preferences.put(HISTORY_KEY, encodeWirelessConnectionHistory(normalized))
            preferences.flush()
        }.onFailure { error ->
            println("QADB wireless connection history persistence failed: ${error.message}")
        }
    }
}

class WirelessAdbConnectionManager(
    private val historyStore: WirelessConnectionHistoryStore = WirelessConnectionHistoryStore(),
    private val connectCommand: suspend (String) -> AdbTool.AdbResult = { AdbTool.connectDevice(it) },
    private val pairCommand: suspend (String, String) -> AdbTool.AdbResult = { endpoint, code ->
        AdbTool.pairDevice(endpoint, code)
    }
) {
    val history: StateFlow<List<String>> = historyStore.endpoints

    suspend fun connect(address: String): WirelessAdbOperationResult {
        val validation = validateWirelessEndpoint(address, requirePort = false)
        val endpoint = validation.endpoint ?: return WirelessAdbOperationResult(
            operation = WirelessAdbOperation.CONNECT,
            success = false,
            issue = validation.issue
        )
        val result = interpretWirelessAdbResult(
            operation = WirelessAdbOperation.CONNECT,
            endpoint = endpoint,
            adbResult = connectCommand(endpoint)
        )
        if (result.success) historyStore.recordSuccessful(endpoint)
        return result
    }

    suspend fun pair(address: String, pairingCode: String): WirelessAdbOperationResult {
        val validation = validateWirelessEndpoint(address, requirePort = true)
        val endpoint = validation.endpoint ?: return WirelessAdbOperationResult(
            operation = WirelessAdbOperation.PAIR,
            success = false,
            issue = validation.issue
        )
        val codeIssue = validatePairingCode(pairingCode)
        if (codeIssue != null) {
            return WirelessAdbOperationResult(
                operation = WirelessAdbOperation.PAIR,
                success = false,
                endpoint = endpoint,
                issue = codeIssue
            )
        }
        return interpretWirelessAdbResult(
            operation = WirelessAdbOperation.PAIR,
            endpoint = endpoint,
            adbResult = pairCommand(endpoint, pairingCode.trim())
        )
    }

    fun removeHistory(endpoint: String) {
        historyStore.remove(endpoint)
    }
}
