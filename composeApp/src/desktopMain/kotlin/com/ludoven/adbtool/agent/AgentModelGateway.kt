package com.ludoven.adbtool.agent

data class AgentModelRequestContext(
    val task: String,
    val observation: AgentObservation,
    val completedSteps: List<AgentStep> = emptyList()
)

/** Role-aware boundary: runtime code never selects an OpenAI-compatible endpoint directly. */
interface AgentModelGateway {
    suspend fun plan(context: AgentModelRequestContext): AgentPlanDecision?
    suspend fun decide(context: AgentModelRequestContext, preferVision: Boolean): AgentModelDecision
    suspend fun recover(context: AgentModelRequestContext, failure: String): AgentPlanDecision?
    suspend fun summarize(context: AgentModelRequestContext): AgentCompactionResult?
}

class RoutedAgentModelGateway(
    private val providers: AgentProviderRepository = AgentProviderRuntime.repository,
    private val client: AgentModelClient = OpenAiCompatibleClient()
) : AgentModelGateway {
    override suspend fun plan(context: AgentModelRequestContext): AgentPlanDecision? = withRole(AgentModelRole.PLANNER) { config, key, _ ->
        client.planTask(config, key, context.task, context.observation)
    }

    override suspend fun decide(context: AgentModelRequestContext, preferVision: Boolean): AgentModelDecision =
        withRole(if (preferVision) AgentModelRole.VISION else AgentModelRole.EXECUTOR) { config, key, profile ->
            val screenshot = preferVision && profile.capabilities.vision
            client.nextAction(config, key, AgentModelContext(context.task, context.observation, context.completedSteps), screenshot)
        }

    override suspend fun recover(context: AgentModelRequestContext, failure: String): AgentPlanDecision? = withRole(AgentModelRole.RECOVERY) { config, key, _ ->
        client.repairPlan(config, key, context.task, context.observation, context.completedSteps, failure)
    }

    override suspend fun summarize(context: AgentModelRequestContext): AgentCompactionResult? = withRole(AgentModelRole.SUMMARIZER) { config, key, _ ->
        client.compactContext(config, key, AgentModelContext(context.task, context.observation, context.completedSteps))
    }

    private suspend fun <T> withRole(role: AgentModelRole, block: suspend (AiModelConfig, String, AgentProviderProfile) -> T): T {
        providers.ensureMigration()
        val profile = requireNotNull(providers.providerFor(role)) { "No enabled provider is configured for ${role.name.lowercase()}" }
        val key = requireNotNull(providers.apiKey(profile.id)) { "API key is missing for ${profile.name}" }
        return block(profile.toLegacyConfig(), key, profile)
    }
}

data class AgentCapabilityReport(
    val capabilities: AgentCapabilities,
    val checks: Map<String, String>
)

class AgentCapabilityProbe(private val client: AgentModelClient = OpenAiCompatibleClient()) {
    suspend fun probe(profile: AgentProviderProfile, apiKey: String): AgentCapabilityReport {
        client.testConnection(profile.toLegacyConfig(), apiKey)
        // A connection test is safe for every compatible endpoint. Optional capabilities are opt-in
        // until the provider has been successfully used, preventing an unsupported probe from running an action.
        val caps = profile.capabilities.copy(text = true)
        return AgentCapabilityReport(caps, buildMap {
            put("text", "passed")
            put("json", if (caps.structuredOutput) "enabled" else "fallback: tool/json")
            put("tool_calling", if (caps.toolCalling) "enabled" else "fallback: strict json")
            put("vision", if (caps.vision) "enabled" else "disabled")
            put("reasoning", if (caps.reasoning) "enabled" else "disabled")
            put("usage", if (caps.usageReporting) "enabled" else "not reported")
        })
    }
}
