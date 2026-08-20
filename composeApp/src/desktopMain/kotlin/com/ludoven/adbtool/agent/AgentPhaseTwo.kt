package com.ludoven.adbtool.agent

import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.delay

const val CURRENT_AGENT_PROVIDER_SCHEMA_VERSION = 2

/** Configuration that is safe to keep in Preferences. Secrets are addressed by provider id. */
data class AgentProviderProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val schemaVersion: Int = CURRENT_AGENT_PROVIDER_SCHEMA_VERSION,
    val protocol: AgentModelProtocol = AgentModelProtocol.OPENAI_COMPATIBLE,
    val authType: AgentProviderAuthType = AgentProviderAuthType.BEARER,
    val authHeaderName: String = AgentProviderAuthType.BEARER.defaultHeaderName,
    val streamingMode: AgentStreamingMode = AgentStreamingMode.AUTO,
    val baseUrl: String,
    val defaultModel: String,
    val enabled: Boolean = true,
    val capabilities: AgentCapabilities = AgentCapabilities(),
    val limits: AgentProviderLimits = AgentProviderLimits(),
    val requestOptions: AgentRequestOptions = AgentRequestOptions(),
    val pricing: AgentPricing = AgentPricing()
)

enum class AgentModelProtocol { OPENAI_COMPATIBLE, CUSTOM_ADAPTER }

enum class AgentProviderAuthType(val defaultHeaderName: String) {
    BEARER("Authorization"),
    API_KEY_HEADER("X-API-Key"),
    NONE("")
}

/** Tool decisions always remain non-streaming; this setting applies to user-facing answer delivery. */
enum class AgentStreamingMode { DISABLED, AUTO, SSE }

data class AgentCapabilities(
    val text: Boolean = true,
    val vision: Boolean = false,
    val toolCalling: Boolean = true,
    val structuredOutput: Boolean = false,
    val reasoning: Boolean = false,
    val promptCache: Boolean = false,
    val usageReporting: Boolean = false
)

data class AgentProviderLimits(
    val contextWindowTokens: Int? = null,
    val maxOutputTokens: Int = 8_192,
    val timeoutMs: Long = 30_000,
    val maxRetries: Int = 2
)

data class AgentRequestOptions(
    val extraBodyJson: String = "",
    /** Header names only. Values live in SecretStore under providerHeaderSecretAccount(). */
    val secretHeaderNames: List<String> = emptyList()
)

/** Fully resolved runtime configuration. Secrets must never be persisted or logged from this value. */
data class ResolvedAgentProvider(
    val role: AgentModelRole,
    val profile: AgentProviderProfile,
    val authSecret: String?,
    val secretHeaders: Map<String, String> = emptyMap()
) {
    val id: String get() = profile.id
    val model: String get() = profile.defaultModel
    val capabilities: AgentCapabilities get() = profile.capabilities
    val limits: AgentProviderLimits get() = profile.limits
    val requestOptions: AgentRequestOptions get() = profile.requestOptions
    val streamingMode: AgentStreamingMode get() = profile.streamingMode

    override fun toString(): String =
        "ResolvedAgentProvider(role=$role, providerId=${profile.id}, authSecret=<redacted>, " +
            "secretHeaders=<redacted:${secretHeaders.size}>)"
}

data class AgentPricing(
    val inputPerMillion: Double = 0.0,
    val cachedInputPerMillion: Double = 0.0,
    val outputPerMillion: Double = 0.0,
    val currency: String = "CNY"
)

/** Immutable, non-secret billing metadata captured when a provider request is routed. */
data class AgentModelProviderSnapshot(
    val providerId: String,
    val pricing: AgentPricing
)

/** Per-attempt billing data retained until one logical model call is consumed by the budget. */
data class AgentModelBillableAttempt(
    val usage: AgentUsage,
    val providerSnapshot: AgentModelProviderSnapshot
)

data class AgentModelBilling(
    val attempts: List<AgentModelBillableAttempt> = emptyList()
) {
    val usage: AgentUsage
        get() = attempts.fold(AgentUsage()) { total, attempt -> total + attempt.usage }

    fun withAttempt(usage: AgentUsage, providerSnapshot: AgentModelProviderSnapshot): AgentModelBilling =
        copy(attempts = attempts + AgentModelBillableAttempt(usage, providerSnapshot))
}

/**
 * V2 uses only [BRAIN]. The remaining roles are retained while the V1 runtime is available for
 * rollback and while existing provider bindings are migrated.
 */
enum class AgentModelRole { BRAIN, PLANNER, EXECUTOR, VISION, RECOVERY, SUMMARIZER, VERIFIER, RESPONDER }

data class AgentRoleBindings(
    val providers: Map<AgentModelRole, String> = emptyMap()
) {
    fun providerIdFor(role: AgentModelRole, fallbackProviderId: String): String =
        providers[role] ?: fallbackProviderId
}

enum class AgentModelOperation {
    CONNECTION_TEST,
    BRAIN_DECISION,
    INTENT_CLASSIFICATION,
    PLAN,
    ACTION,
    FINISH,
    RECOVERY,
    COMPACTION,
    PROTOCOL_REPAIR,
    USER_ANSWER
}

enum class AgentModelAttemptOutcome {
    SUCCEEDED,
    RETRYABLE_HTTP,
    HTTP_FAILURE,
    NETWORK_FAILURE,
    INVALID_RESPONSE,
    CANCELLED
}

data class AgentModelAttemptEvent(
    val providerId: String,
    val role: AgentModelRole,
    val operation: AgentModelOperation,
    val attempt: Int,
    val maxAttempts: Int,
    val outcome: AgentModelAttemptOutcome,
    val elapsedMs: Long,
    val statusCode: Int? = null,
    val usage: AgentUsage = AgentUsage()
)

data class AgentModelMetrics(
    val logicalCalls: Int = 0,
    val httpAttempts: Int = 0,
    val successfulResponses: Int = 0,
    val protocolRepairs: Int = 0,
    val elapsedMs: Long = 0,
    val usage: AgentUsage = AgentUsage()
)

fun interface AgentModelMetricsSink {
    fun record(event: AgentModelAttemptEvent)
}

object NoopAgentModelMetricsSink : AgentModelMetricsSink {
    override fun record(event: AgentModelAttemptEvent) = Unit
}

/** Associates provider attempts with the current Agent run without leaking run ids into requests. */
object AgentModelMetricsRuntime : AgentModelMetricsSink {
    private data class Accumulator(var metrics: AgentModelMetrics = AgentModelMetrics())

    private val currentRunId = ThreadLocal<String?>()
    private val metricsByRun = ConcurrentHashMap<String, Accumulator>()

    fun contextElement(runId: String): ThreadContextElement<String?> {
        require(runId.isNotBlank()) { "Run id is required" }
        return currentRunId.asContextElement(runId)
    }

    override fun record(event: AgentModelAttemptEvent) {
        val runId = currentRunId.get() ?: return
        val accumulator = metricsByRun.computeIfAbsent(runId) { Accumulator() }
        synchronized(accumulator) {
            val current = accumulator.metrics
            accumulator.metrics = current.copy(
                logicalCalls = current.logicalCalls + if (event.attempt == 1) 1 else 0,
                httpAttempts = current.httpAttempts + 1,
                successfulResponses = current.successfulResponses +
                    if (event.outcome == AgentModelAttemptOutcome.SUCCEEDED) 1 else 0,
                protocolRepairs = current.protocolRepairs +
                    if (event.attempt == 1 && event.operation == AgentModelOperation.PROTOCOL_REPAIR) 1 else 0,
                elapsedMs = current.elapsedMs + event.elapsedMs.coerceAtLeast(0),
                usage = current.usage + event.usage
            )
        }
    }

    fun snapshot(runId: String): AgentModelMetrics {
        val accumulator = metricsByRun[runId] ?: return AgentModelMetrics()
        return synchronized(accumulator) { accumulator.metrics }
    }

    fun clear(runId: String) {
        metricsByRun.remove(runId)
    }
}

enum class ModelProtocolIssueCode {
    INVALID_RESPONSE_JSON,
    RESPONSE_LIMIT_EXCEEDED,
    STREAM_INTERRUPTED,
    MISSING_CHOICES,
    MISSING_MESSAGE,
    MISSING_TOOL_CALL,
    MULTIPLE_TOOL_CALLS,
    MISSING_FUNCTION,
    MISSING_TOOL_NAME,
    INVALID_ARGUMENTS_JSON,
    MISSING_ARGUMENT,
    MISSING_OR_INVALID_ARGUMENT,
    UNSUPPORTED_TOOL,
    UNEXPECTED_TOOL,
    WRONG_TERMINAL_ACTION,
    UNKNOWN
}

/** Safe, bounded protocol failure metadata suitable for one caller-controlled repair request. */
data class ModelProtocolIssue(
    val code: ModelProtocolIssueCode,
    val operation: AgentModelOperation,
    val role: AgentModelRole,
    val message: String,
    val toolName: String? = null,
    val argumentName: String? = null,
    val repairable: Boolean = true,
    val repairAttempted: Boolean = false,
    val usage: AgentUsage = AgentUsage(),
    val providerSnapshot: AgentModelProviderSnapshot? = null,
    val billing: AgentModelBilling? = null
)

class ModelProtocolIssueException(
    val issue: ModelProtocolIssue
) : AgentException(issue.message)

data class AgentUserAnswerStreamResult(
    val decision: AgentModelDecision,
    val requestedMode: AgentStreamingMode,
    val usedStreaming: Boolean
)

data class AgentBudget(
    val maxModelCalls: Int = 16,
    val maxInputTokens: Int = 100_000,
    val maxOutputTokens: Int = 10_000,
    val maxVisionCalls: Int = 2,
    val maxReplans: Int = 2,
    val enforceLimits: Boolean = true
)

enum class AgentBudgetMode { NORMAL, DIFF_ONLY, NO_OPTIONAL_VISION, FINAL_RECOVERY_ONLY, EXHAUSTED }

data class AgentBudgetStatus(
    val usage: AgentUsage = AgentUsage(),
    val modelCalls: Int = 0,
    val visionCalls: Int = 0,
    val replans: Int = 0,
    val modelCallLimit: Int = AgentBudget().maxModelCalls,
    val visionCallLimit: Int = AgentBudget().maxVisionCalls,
    val replanLimit: Int = AgentBudget().maxReplans,
    val mode: AgentBudgetMode = AgentBudgetMode.NORMAL,
    val stopReason: String? = null,
    val estimatedCost: Double = 0.0
)

class AgentBudgetTracker(
    private val budget: AgentBudget,
    private val defaultPricing: AgentPricing = AgentPricing()
) {
    private var status = AgentBudgetStatus(
        modelCallLimit = budget.maxModelCalls.takeIf { budget.enforceLimits } ?: 0,
        visionCallLimit = budget.maxVisionCalls.takeIf { budget.enforceLimits } ?: 0,
        replanLimit = budget.maxReplans.takeIf { budget.enforceLimits } ?: 0
    )

    fun current(): AgentBudgetStatus = status

    fun remainingModelCalls(): Int = if (budget.enforceLimits) {
        (budget.maxModelCalls - status.modelCalls).coerceAtLeast(0)
    } else {
        Int.MAX_VALUE
    }

    fun shouldForceFinalResponse(): Boolean = budget.enforceLimits && remainingModelCalls() == 1

    /** Model compaction is optional, so preserve enough calls for an action and terminal result. */
    fun canSpendOnCompaction(): Boolean =
        status.mode == AgentBudgetMode.NORMAL && remainingModelCalls() > RESERVED_MODEL_CALLS

    fun canRequest(includeVision: Boolean, isReplan: Boolean): Boolean {
        if (!budget.enforceLimits) return true
        if (status.mode == AgentBudgetMode.EXHAUSTED) return false
        if (status.modelCalls >= budget.maxModelCalls) {
            return exhaust("Model call budget reached (${status.modelCalls}/${budget.maxModelCalls})")
        }
        if (includeVision && status.visionCalls >= budget.maxVisionCalls) return false
        if (isReplan && status.replans >= budget.maxReplans) return false
        return true
    }

    fun record(
        usage: AgentUsage,
        usedVision: Boolean,
        isReplan: Boolean = false,
        pricing: AgentPricing = defaultPricing,
        billing: AgentModelBilling? = null
    ): AgentBudgetStatus {
        val billableUsage = billing?.usage ?: usage
        val nextUsage = status.usage + billableUsage
        val next = status.copy(
            usage = nextUsage,
            modelCalls = status.modelCalls + 1,
            visionCalls = status.visionCalls + if (usedVision) 1 else 0,
            replans = status.replans + if (isReplan) 1 else 0,
            estimatedCost = status.estimatedCost + (billing?.estimatedCost() ?: estimateCost(usage, pricing))
        )
        status = next.copy(mode = modeFor(next))
        if (
            budget.enforceLimits &&
            (nextUsage.promptTokens > budget.maxInputTokens || nextUsage.completionTokens > budget.maxOutputTokens)
        ) {
            exhaust("Token budget reached")
        }
        return status
    }

    private fun modeFor(value: AgentBudgetStatus): AgentBudgetMode {
        if (!budget.enforceLimits) return AgentBudgetMode.NORMAL
        val ratio = maxOf(
            value.modelCalls.toDouble() / budget.maxModelCalls.coerceAtLeast(1),
            value.usage.promptTokens.toDouble() / budget.maxInputTokens.coerceAtLeast(1),
            value.usage.completionTokens.toDouble() / budget.maxOutputTokens.coerceAtLeast(1)
        )
        return when {
            budget.maxModelCalls - value.modelCalls <= 1 -> AgentBudgetMode.FINAL_RECOVERY_ONLY
            ratio >= 0.90 -> AgentBudgetMode.FINAL_RECOVERY_ONLY
            ratio >= 0.75 -> AgentBudgetMode.NO_OPTIONAL_VISION
            ratio >= 0.50 -> AgentBudgetMode.DIFF_ONLY
            else -> AgentBudgetMode.NORMAL
        }
    }

    private fun exhaust(reason: String): Boolean {
        status = status.copy(mode = AgentBudgetMode.EXHAUSTED, stopReason = reason)
        return false
    }

    private fun estimateCost(usage: AgentUsage, pricing: AgentPricing): Double =
        (usage.promptTokens - usage.cachedTokens).coerceAtLeast(0) / 1_000_000.0 * pricing.inputPerMillion +
            usage.cachedTokens / 1_000_000.0 * pricing.cachedInputPerMillion +
            usage.completionTokens / 1_000_000.0 * pricing.outputPerMillion

    private fun AgentModelBilling.estimatedCost(): Double = attempts.sumOf { attempt ->
        estimateCost(attempt.usage, attempt.providerSnapshot.pricing)
    }

    private companion object {
        const val RESERVED_MODEL_CALLS = 2
    }
}

data class DeviceState(
    val packageName: String?,
    val activityName: String?,
    val pageSignature: PageSignature,
    val nodes: List<UiNodeSnapshot>,
    val capturedAt: Long = System.currentTimeMillis()
)

data class PageSignature(val value: String, val packageName: String, val activityName: String)

data class PageDiff(
    val changed: Boolean,
    val from: PageSignature?,
    val to: PageSignature,
    val addedElementIds: List<String> = emptyList(),
    val removedElementIds: List<String> = emptyList()
)

object PageSignatureEngine {
    fun state(observation: AgentObservation): DeviceState = DeviceState(
        packageName = observation.uiNodes.firstOrNull { it.packageName.isNotBlank() }?.packageName,
        activityName = normalizeActivityComponent(observation.currentActivity).ifBlank { null },
        pageSignature = signature(observation),
        nodes = observation.uiNodes
    )

    fun signature(observation: AgentObservation): PageSignature {
        val packageName = observation.uiNodes.firstOrNull { it.packageName.isNotBlank() }?.packageName.orEmpty()
        val activityName = normalizeActivityComponent(observation.currentActivity)
        val keyNodes = observation.uiNodes.take(32).joinToString("|") { node ->
            listOf(node.resourceId, node.text, node.contentDescription, node.role, node.clickable, node.editable).joinToString(":")
        }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$packageName\n$activityName\n$keyNodes".toByteArray())
            .joinToString("") { "%02x".format(it) }
        return PageSignature(digest.take(20), packageName, activityName)
    }

    fun diff(before: DeviceState?, after: DeviceState): PageDiff {
        val beforeIds = before?.nodes?.map { it.stableIdentity() }.orEmpty().toSet()
        val afterIds = after.nodes.map { it.stableIdentity() }.toSet()
        return PageDiff(
            changed = before?.pageSignature != after.pageSignature,
            from = before?.pageSignature,
            to = after.pageSignature,
            addedElementIds = (afterIds - beforeIds).take(32),
            removedElementIds = (beforeIds - afterIds).take(32)
        )
    }
}

internal fun normalizeActivityComponent(value: String): String {
    val trimmed = value.trim()
    return ACTIVITY_COMPONENT_PATTERN.find(trimmed)?.groupValues?.get(1).orEmpty().ifBlank { trimmed }
}

private val ACTIVITY_COMPONENT_PATTERN =
    Regex("([A-Za-z][A-Za-z0-9_]*(?:\\.[A-Za-z0-9_]+)*/[A-Za-z0-9_.$]+)")

data class AgentSelector(
    val resourceId: String? = null,
    val textAny: List<String> = emptyList(),
    val contentDescriptionAny: List<String> = emptyList(),
    val role: String? = null,
    val requireEnabled: Boolean = true,
    val ancestor: AgentSelector? = null
)

typealias Selector = AgentSelector

sealed interface SelectorResolution {
    data class Resolved(val node: UiNodeSnapshot) : SelectorResolution
    data object Missing : SelectorResolution
    data class Ambiguous(val matchCount: Int) : SelectorResolution
}

object SelectorResolver {
    fun resolve(selector: AgentSelector, nodes: List<UiNodeSnapshot>): UiNodeSnapshot? =
        (resolveUnique(selector, nodes) as? SelectorResolution.Resolved)?.node

    fun resolveUnique(selector: AgentSelector, nodes: List<UiNodeSnapshot>): SelectorResolution {
        val matches = nodes
        .asSequence()
        .filter { !selector.requireEnabled || it.enabled }
        .filter { selector.resourceId.isNullOrBlank() || it.resourceId == selector.resourceId }
        .filter { selector.role.isNullOrBlank() || it.role == selector.role }
        .filter { selector.textAny.isEmpty() || selector.textAny.any { text -> it.text.equals(text, true) } }
        .filter { selector.contentDescriptionAny.isEmpty() || selector.contentDescriptionAny.any { text -> it.contentDescription.equals(text, true) } }
        .filter { node -> selector.ancestor?.let { matchesAncestor(it, node) } != false }
        .sortedByDescending { score(selector, it) }
        .toList()
        return when (matches.size) {
            0 -> SelectorResolution.Missing
            1 -> SelectorResolution.Resolved(matches.single())
            else -> {
                val bestScore = score(selector, matches.first())
                val best = matches.takeWhile { score(selector, it) == bestScore }
                if (best.size == 1) SelectorResolution.Resolved(best.single())
                else SelectorResolution.Ambiguous(best.size)
            }
        }
    }

    fun from(node: UiNodeSnapshot): AgentSelector {
        val resourceId = node.resourceId.takeIf { it.isNotBlank() }
        return AgentSelector(
            resourceId = resourceId,
            textAny = node.text.takeIf { it.isNotBlank() && it.length <= 80 }?.let(::listOf).orEmpty(),
            contentDescriptionAny = node.contentDescription
                .takeIf { it.isNotBlank() && it.length <= 80 }
                ?.let(::listOf)
                .orEmpty(),
            role = node.role.takeIf { it.isNotBlank() },
            ancestor = resourceId?.let { null }
                ?: node.ancestorResourceIds.lastOrNull()?.let { AgentSelector(resourceId = it) }
        )
    }

    private fun matchesAncestor(selector: AgentSelector, node: UiNodeSnapshot): Boolean =
        (selector.resourceId.isNullOrBlank() || selector.resourceId in node.ancestorResourceIds) &&
            (selector.role.isNullOrBlank() || selector.role in node.ancestorRoles)

    private fun score(selector: AgentSelector, node: UiNodeSnapshot): Int =
        (if (!selector.resourceId.isNullOrBlank() && selector.resourceId == node.resourceId) 16 else 0) +
            (if (selector.textAny.any { it.equals(node.text, true) }) 8 else 0) +
            (if (selector.contentDescriptionAny.any { it.equals(node.contentDescription, true) }) 4 else 0) +
            (if (!selector.role.isNullOrBlank() && selector.role == node.role) 2 else 0) +
            (if (selector.ancestor != null && matchesAncestor(selector.ancestor, node)) 1 else 0)
}

data class VerificationRule(val selector: Selector, val timeoutMs: Long = 8_000)
data class RecoveryPlan(val level: Int, val reason: String, val retryAction: AgentAction? = null)

suspend fun waitForStableObservation(
    timeoutMs: Long = 8_000,
    pollIntervalMs: Long = 250,
    stableSamples: Int = 2,
    observe: suspend () -> AgentObservation
): AgentObservation? {
    val deadline = System.currentTimeMillis() + timeoutMs
    var previous: PageSignature? = null
    var stable = 0
    while (System.currentTimeMillis() < deadline) {
        val observation = observe()
        val signature = PageSignatureEngine.signature(observation)
        stable = if (signature == previous) stable + 1 else 1
        if (stable >= stableSamples) return observation
        previous = signature
        delay(pollIntervalMs)
    }
    return null
}

private fun UiNodeSnapshot.stableIdentity(): String = listOf(resourceId, text, contentDescription, role, bounds).joinToString("|")

fun providerSecretAccount(providerId: String): String = "openai-compatible-api-key:$providerId"
fun providerHeaderSecretAccount(providerId: String): String = "openai-compatible-headers:$providerId"
