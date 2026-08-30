package com.ludoven.adbtool.pages

internal enum class HomeDiagnosticCode {
    NO_DEVICE,
    STORAGE_LOW,
    LATENCY_HIGH,
    BATTERY_LOW,
    HEALTHY
}

internal fun homeDiagnosticCodes(
    isConnected: Boolean,
    storageProgress: Float,
    latencyMs: Int?,
    batteryProgress: Float
): List<HomeDiagnosticCode> {
    if (!isConnected) return listOf(HomeDiagnosticCode.NO_DEVICE)

    val codes = buildList {
        if (storageProgress >= 0.85f) add(HomeDiagnosticCode.STORAGE_LOW)
        if (latencyMs != null && latencyMs >= 250) add(HomeDiagnosticCode.LATENCY_HIGH)
        if (batteryProgress > 0f && batteryProgress <= 0.2f) add(HomeDiagnosticCode.BATTERY_LOW)
    }
    return codes.ifEmpty { listOf(HomeDiagnosticCode.HEALTHY) }
}
