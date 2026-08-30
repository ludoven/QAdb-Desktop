package com.ludoven.adbtool.agent

internal enum class AgentReadiness {
    CHECKING,
    DEVICE_REQUIRED,
    MODEL_REQUIRED,
    READY
}

internal fun resolveAgentReadiness(
    deviceConnected: Boolean,
    configurationChecked: Boolean,
    modelReady: Boolean
): AgentReadiness = when {
    !deviceConnected -> AgentReadiness.DEVICE_REQUIRED
    !configurationChecked -> AgentReadiness.CHECKING
    !modelReady -> AgentReadiness.MODEL_REQUIRED
    else -> AgentReadiness.READY
}
