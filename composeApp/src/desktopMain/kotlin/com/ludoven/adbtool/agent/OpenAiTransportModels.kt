package com.ludoven.adbtool.agent

import java.io.IOException
import kotlinx.serialization.json.JsonObject

internal data class ProviderHttpResponse(
    val body: JsonObject,
    val billing: AgentModelBilling
) {
    val usage: AgentUsage get() = billing.usage
}

internal data class StreamedUserAnswer(
    val text: String,
    val usage: AgentUsage
)

internal class SseUserAnswerProgress {
    @Volatile
    var usage: AgentUsage = AgentUsage()
}

internal class ProviderResponseTimeoutException(val usage: AgentUsage = AgentUsage()) : Exception()
internal class ProviderResponseLimitException : IOException()
internal data class ProviderDeadlineValue<T>(val value: T)
