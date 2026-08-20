package com.ludoven.adbtool.agent

data class AgentModelRequestContext(
    val task: String,
    val observation: AgentObservation,
    val completedSteps: List<AgentStep> = emptyList(),
    val memoryContext: String = "",
    val compactedHistory: String = "",
    val appKnowledgeContext: String = "",
    val observationDelta: AgentObservationDeltaContext? = null,
    val trustedEvidence: AgentTrustedEvidence? = null
)

/** Role-aware boundary: runtime code never selects an OpenAI-compatible endpoint directly. */
interface AgentModelGateway {
    fun supportsVision(): Boolean = false
    fun contextConfig(): AiModelConfig = AiModelConfig(model = "agent-gateway")
    fun pricing(role: AgentModelRole = AgentModelRole.EXECUTOR): AgentPricing = AgentPricing()
    fun userAnswerStreamingMode(): AgentStreamingMode = AgentStreamingMode.DISABLED
    suspend fun classifyIntent(task: String): AgentIntentClassificationDecision? = null
    suspend fun plan(context: AgentModelRequestContext): AgentPlanDecision?
    suspend fun decide(context: AgentModelRequestContext, preferVision: Boolean): AgentModelDecision
    suspend fun finish(context: AgentModelRequestContext, preferVision: Boolean): AgentModelDecision =
        decide(context, preferVision)
    suspend fun recover(context: AgentModelRequestContext, failure: String): AgentPlanDecision?
    suspend fun summarize(context: AgentModelRequestContext): AgentCompactionResult?
    suspend fun repairProtocolIssue(
        context: AgentModelRequestContext,
        preferVision: Boolean,
        issue: ModelProtocolIssue
    ): AgentModelDecision? = null

    suspend fun streamUserAnswer(
        context: AgentModelRequestContext,
        preferVision: Boolean,
        onText: (String) -> Unit
    ): AgentUserAnswerStreamResult {
        val decision = finish(context, preferVision)
        (decision.action as? AgentAction.Finish)?.summary?.let(onText)
        return AgentUserAnswerStreamResult(decision, AgentStreamingMode.DISABLED, usedStreaming = false)
    }
}

/** Provider-aware client contract used by the production route without widening the legacy test seam. */
interface ResolvedAgentModelClient : AgentModelClient {
    suspend fun brainDecision(
        provider: ResolvedAgentProvider,
        request: AgentBrainRequest
    ): AgentBrainResult

    suspend fun classifyTaskIntent(
        provider: ResolvedAgentProvider,
        task: String
    ): AgentIntentClassificationDecision

    suspend fun nextAction(
        provider: ResolvedAgentProvider,
        context: AgentModelContext,
        includeScreenshot: Boolean
    ): AgentModelDecision

    suspend fun finishTask(
        provider: ResolvedAgentProvider,
        context: AgentModelContext,
        includeScreenshot: Boolean
    ): AgentModelDecision

    suspend fun testConnection(provider: ResolvedAgentProvider)

    suspend fun planTask(
        provider: ResolvedAgentProvider,
        task: String,
        observation: AgentObservation
    ): AgentPlanDecision?

    suspend fun repairPlan(
        provider: ResolvedAgentProvider,
        task: String,
        observation: AgentObservation,
        completedSteps: List<AgentStep>,
        failure: String
    ): AgentPlanDecision?

    suspend fun compactContext(
        provider: ResolvedAgentProvider,
        context: AgentModelContext
    ): AgentCompactionResult?

    suspend fun repairProtocolIssue(
        provider: ResolvedAgentProvider,
        context: AgentModelContext,
        includeScreenshot: Boolean,
        issue: ModelProtocolIssue
    ): AgentModelDecision

    suspend fun streamUserAnswer(
        provider: ResolvedAgentProvider,
        context: AgentModelContext,
        includeScreenshot: Boolean,
        onText: (String) -> Unit
    ): AgentUserAnswerStreamResult
}

class RoutedAgentBrainGateway(
    private val providers: AgentProviderRepository = AgentProviderRuntime.repository,
    private val client: ResolvedAgentModelClient = OpenAiCompatibleClient()
) : AgentBrainGateway {
    override suspend fun decide(request: AgentBrainRequest): AgentBrainResult {
        providers.ensureMigration()
        val provider = requireNotNull(providers.resolve(AgentModelRole.BRAIN)) {
            "No enabled provider is configured for brain"
        }
        if (provider.profile.authType != AgentProviderAuthType.NONE) {
            require(!provider.authSecret.isNullOrBlank()) { "API key is missing for ${provider.profile.name}" }
        }
        val attestation = requireNotNull(providers.capabilityAttestation(provider.profile)) {
            "Test the configured BRAIN provider in Settings before starting an Agent task"
        }
        val tier = attestation.tier
        require(tier >= AgentCapabilityTier.L2_SEMANTIC_AGENT) {
            "The configured BRAIN provider did not pass the structured Agent capability test"
        }
        if (request.screenshotPng != null) {
            require(tier >= AgentCapabilityTier.L3_VISUAL_AGENT) {
                "The configured BRAIN provider does not support visual grounding"
            }
        }
        return try {
            client.brainDecision(provider, request).withProviderSnapshot(provider.toModelProviderSnapshot())
        } catch (failure: ModelProtocolIssueException) {
            throw ModelProtocolIssueException(
                failure.issue.copy(providerSnapshot = provider.toModelProviderSnapshot())
            )
        }
    }

    private fun AgentBrainResult.withProviderSnapshot(snapshot: AgentModelProviderSnapshot): AgentBrainResult =
        copy(providerSnapshot = snapshot)
}

class RoutedAgentModelGateway(
    private val providers: AgentProviderRepository = AgentProviderRuntime.repository,
    private val client: AgentModelClient = OpenAiCompatibleClient()
) : AgentModelGateway {
    override fun supportsVision(): Boolean =
        providers.providerFor(AgentModelRole.VISION)?.capabilities?.vision == true

    override fun contextConfig(): AiModelConfig =
        providers.providerFor(AgentModelRole.EXECUTOR)?.toLegacyConfig()
            ?: AiModelConfig(model = "routed-agent")

    override fun pricing(role: AgentModelRole): AgentPricing =
        providers.providerFor(role)?.pricing ?: AgentPricing()

    override fun userAnswerStreamingMode(): AgentStreamingMode =
        providers.providerFor(AgentModelRole.RESPONDER)?.streamingMode ?: AgentStreamingMode.DISABLED

    override suspend fun classifyIntent(task: String): AgentIntentClassificationDecision? =
        withRole(AgentModelRole.PLANNER) { provider ->
            val decision = resolvedClient()?.classifyTaskIntent(provider, task)
                ?: client.classifyTaskIntent(
                    provider.profile.toLegacyConfig(),
                    provider.authSecret.orEmpty(),
                    task
                )
            decision?.copy(providerSnapshot = provider.toModelProviderSnapshot())
        }

    override suspend fun plan(context: AgentModelRequestContext): AgentPlanDecision? = withRole(AgentModelRole.PLANNER) { provider ->
        val decision = resolvedClient()?.planTask(provider, context.task, context.observation)
            ?: client.planTask(provider.profile.toLegacyConfig(), provider.authSecret.orEmpty(), context.task, context.observation)
        decision?.copy(providerSnapshot = provider.toModelProviderSnapshot())
    }

    override suspend fun decide(context: AgentModelRequestContext, preferVision: Boolean): AgentModelDecision =
        withRole(if (preferVision) AgentModelRole.VISION else AgentModelRole.EXECUTOR) { provider ->
            val screenshot = preferVision && provider.capabilities.vision
            val decision = resolvedClient()?.nextAction(provider, context.toModelContext(), screenshot)
                ?: client.nextAction(provider.profile.toLegacyConfig(), provider.authSecret.orEmpty(), context.toModelContext(), screenshot)
            decision.copy(providerSnapshot = provider.toModelProviderSnapshot())
        }

    override suspend fun finish(context: AgentModelRequestContext, preferVision: Boolean): AgentModelDecision =
        withRole(AgentModelRole.RESPONDER) { provider ->
            val screenshot = preferVision && provider.capabilities.vision
            val decision = resolvedClient()?.finishTask(provider, context.toModelContext(), screenshot)
                ?: client.finishTask(provider.profile.toLegacyConfig(), provider.authSecret.orEmpty(), context.toModelContext(), screenshot)
            decision.copy(providerSnapshot = provider.toModelProviderSnapshot())
        }

    override suspend fun recover(context: AgentModelRequestContext, failure: String): AgentPlanDecision? = withRole(AgentModelRole.RECOVERY) { provider ->
        val decision = resolvedClient()?.repairPlan(provider, context.task, context.observation, context.completedSteps, failure)
            ?: client.repairPlan(provider.profile.toLegacyConfig(), provider.authSecret.orEmpty(), context.task, context.observation, context.completedSteps, failure)
        decision?.copy(providerSnapshot = provider.toModelProviderSnapshot())
    }

    override suspend fun summarize(context: AgentModelRequestContext): AgentCompactionResult? = withRole(AgentModelRole.SUMMARIZER) { provider ->
        val result = resolvedClient()?.compactContext(provider, context.toModelContext())
            ?: client.compactContext(provider.profile.toLegacyConfig(), provider.authSecret.orEmpty(), context.toModelContext())
        result?.copy(providerSnapshot = provider.toModelProviderSnapshot())
    }

    override suspend fun repairProtocolIssue(
        context: AgentModelRequestContext,
        preferVision: Boolean,
        issue: ModelProtocolIssue
    ): AgentModelDecision? = withRole(issue.role) { provider ->
        resolvedClient()?.repairProtocolIssue(
            provider = provider,
            context = context.toModelContext(),
            includeScreenshot = preferVision && provider.capabilities.vision,
            issue = issue
        )?.copy(providerSnapshot = provider.toModelProviderSnapshot())
    }

    override suspend fun streamUserAnswer(
        context: AgentModelRequestContext,
        preferVision: Boolean,
        onText: (String) -> Unit
    ): AgentUserAnswerStreamResult = withRole(AgentModelRole.RESPONDER) { provider ->
        val result = resolvedClient()?.streamUserAnswer(
            provider = provider,
            context = context.toModelContext(),
            includeScreenshot = preferVision && provider.capabilities.vision,
            onText = onText
        ) ?: run {
            val decision = client.finishTask(
                provider.profile.toLegacyConfig(),
                provider.authSecret.orEmpty(),
                context.toModelContext(),
                preferVision && provider.capabilities.vision
            )
            (decision.action as? AgentAction.Finish)?.summary?.let(onText)
            AgentUserAnswerStreamResult(decision, provider.streamingMode, usedStreaming = false)
        }
        result.copy(decision = result.decision.copy(providerSnapshot = provider.toModelProviderSnapshot()))
    }

    private fun resolvedClient(): ResolvedAgentModelClient? = client as? ResolvedAgentModelClient

    private suspend fun <T> withRole(role: AgentModelRole, block: suspend (ResolvedAgentProvider) -> T): T {
        providers.ensureMigration()
        val provider = requireNotNull(providers.resolve(role)) {
            "No enabled provider is configured for ${role.name.lowercase()}"
        }
        if (provider.profile.authType != AgentProviderAuthType.NONE) {
            require(!provider.authSecret.isNullOrBlank()) { "API key is missing for ${provider.profile.name}" }
        }
        return try {
            block(provider)
        } catch (failure: ModelProtocolIssueException) {
            throw ModelProtocolIssueException(
                failure.issue.copy(providerSnapshot = provider.toModelProviderSnapshot())
            )
        }
    }
}

private fun ResolvedAgentProvider.toModelProviderSnapshot(): AgentModelProviderSnapshot =
    AgentModelProviderSnapshot(providerId = id, pricing = profile.pricing)

private fun AgentModelRequestContext.toModelContext(): AgentModelContext = AgentModelContext(
    task = task,
    observation = observation,
    completedSteps = completedSteps,
    memoryContext = memoryContext,
    compactedHistory = compactedHistory,
    appKnowledgeContext = appKnowledgeContext,
    observationDelta = observationDelta,
    trustedEvidence = trustedEvidence
)

data class AgentCapabilityReport(
    val capabilities: AgentCapabilities,
    val checks: Map<String, String>,
    val tier: AgentCapabilityTier = capabilities.capabilityTier()
)

class AgentCapabilityProbe(private val client: AgentModelClient = OpenAiCompatibleClient()) {
    suspend fun probe(profile: AgentProviderProfile, apiKey: String): AgentCapabilityReport {
        return probeResolved(ResolvedAgentProvider(AgentModelRole.BRAIN, profile, apiKey))
    }

    suspend fun probe(provider: ResolvedAgentProvider): AgentCapabilityReport =
        probeResolved(provider)

    private suspend fun probeResolved(provider: ResolvedAgentProvider): AgentCapabilityReport {
        if (client is ResolvedAgentModelClient) client.testConnection(provider)
        else client.testConnection(provider.profile.toLegacyConfig(), provider.authSecret.orEmpty())
        val checks = linkedMapOf("text" to "passed")
        val resolvedClient = client as? ResolvedAgentModelClient
        val semanticAgentPassed = provider.capabilities.toolCalling && resolvedClient != null && runCatching {
            val result = resolvedClient.brainDecision(
                provider.copy(role = AgentModelRole.BRAIN),
                AgentBrainRequest(
                    task = CAPABILITY_TEXT_CHALLENGE,
                    phase = AgentBrainPhase.INITIAL,
                    remainingModelCalls = 1,
                    remainingVisionCalls = 0,
                    remainingDeviceActions = 0
                )
            )
            require(result.decision is AgentBrainDecision.Answer) {
                "The model did not return an ANSWER decision"
            }
        }.fold(
            onSuccess = { checks["semantic_agent"] = "passed"; true },
            onFailure = { checks["semantic_agent"] = "failed:${it.safeCapabilityFailure()}"; false }
        )
        if (!provider.capabilities.toolCalling) checks["semantic_agent"] = "disabled"
        if (resolvedClient == null) checks["semantic_agent"] = "unsupported-client"

        val visualAgentPassed = semanticAgentPassed && provider.capabilities.vision && runCatching {
            val result = requireNotNull(resolvedClient).brainDecision(
                provider.copy(role = AgentModelRole.BRAIN),
                AgentBrainRequest(
                    task = CAPABILITY_VISION_CHALLENGE,
                    phase = AgentBrainPhase.INITIAL,
                    screenshotPng = CAPABILITY_BLUE_PNG,
                    remainingModelCalls = 1,
                    remainingVisionCalls = 1,
                    remainingDeviceActions = 0
                )
            )
            require(result.usedVision) { "The provider did not consume the synthetic image request" }
            require(result.decision is AgentBrainDecision.Answer) {
                "The model did not return an ANSWER decision for the visual challenge"
            }
        }.fold(
            onSuccess = { checks["visual_agent"] = "passed"; true },
            onFailure = { checks["visual_agent"] = "failed:${it.safeCapabilityFailure()}"; false }
        )
        if (!provider.capabilities.vision) checks["visual_agent"] = "disabled"
        checks["structured_output"] = if (provider.capabilities.structuredOutput) "declared" else "not-declared"
        checks["reasoning"] = if (provider.capabilities.reasoning) "declared" else "not-declared"
        checks["usage"] = if (provider.capabilities.usageReporting) "declared" else "not-reported"

        val actual = provider.capabilities.copy(
            text = true,
            toolCalling = semanticAgentPassed,
            vision = visualAgentPassed
        )
        return AgentCapabilityReport(actual, checks, actual.capabilityTier())
    }
}

private fun Throwable.safeCapabilityFailure(): String = when (this) {
    is ModelHttpException -> "http_${statusCode}"
    is ModelProtocolIssueException -> issue.code.name.lowercase()
    is ModelProtocolException -> "protocol"
    else -> this::class.simpleName?.lowercase()?.take(40) ?: "unknown"
}

private const val CAPABILITY_TEXT_CHALLENGE =
    "This is a QADB capability test with no device data. Return one ANSWER decision with a short acknowledgement."
private const val CAPABILITY_VISION_CHALLENGE =
    "This is a QADB capability test using a synthetic image only. Return one ANSWER decision."

/** 1x1 opaque blue PNG, embedded to avoid filesystem, network, user-data and device-data access. */
private val CAPABILITY_BLUE_PNG = java.util.Base64.getDecoder().decode(
    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="
)
