package com.ludoven.adbtool.agent

import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.delay

/** Configuration that is safe to keep in Preferences. Secrets are addressed by provider id. */
data class AgentProviderProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val protocol: AgentModelProtocol = AgentModelProtocol.OPENAI_COMPATIBLE,
    val baseUrl: String,
    val defaultModel: String,
    val enabled: Boolean = true,
    val capabilities: AgentCapabilities = AgentCapabilities(),
    val limits: AgentProviderLimits = AgentProviderLimits(),
    val requestOptions: AgentRequestOptions = AgentRequestOptions(),
    val pricing: AgentPricing = AgentPricing()
)

enum class AgentModelProtocol { OPENAI_COMPATIBLE, CUSTOM_ADAPTER }

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

data class AgentPricing(
    val inputPerMillion: Double = 0.0,
    val cachedInputPerMillion: Double = 0.0,
    val outputPerMillion: Double = 0.0,
    val currency: String = "CNY"
)

enum class AgentModelRole { PLANNER, EXECUTOR, VISION, RECOVERY, SUMMARIZER, VERIFIER }

data class AgentRoleBindings(
    val providers: Map<AgentModelRole, String> = emptyMap()
) {
    fun providerIdFor(role: AgentModelRole, fallbackProviderId: String): String =
        providers[role] ?: fallbackProviderId
}

data class AgentBudget(
    val maxModelCalls: Int = 16,
    val maxInputTokens: Int = 100_000,
    val maxOutputTokens: Int = 10_000,
    val maxVisionCalls: Int = 5,
    val maxReplans: Int = 2
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

class AgentBudgetTracker(private val budget: AgentBudget, private val pricing: AgentPricing = AgentPricing()) {
    private var status = AgentBudgetStatus(
        modelCallLimit = budget.maxModelCalls,
        visionCallLimit = budget.maxVisionCalls,
        replanLimit = budget.maxReplans
    )

    fun current(): AgentBudgetStatus = status

    fun remainingModelCalls(): Int = (budget.maxModelCalls - status.modelCalls).coerceAtLeast(0)

    fun shouldForceFinalResponse(): Boolean = remainingModelCalls() == 1

    /** Model compaction is optional, so preserve enough calls for an action and terminal result. */
    fun canSpendOnCompaction(): Boolean =
        status.mode == AgentBudgetMode.NORMAL && remainingModelCalls() > RESERVED_MODEL_CALLS

    fun canRequest(includeVision: Boolean, isReplan: Boolean): Boolean {
        if (status.mode == AgentBudgetMode.EXHAUSTED) return false
        if (status.modelCalls >= budget.maxModelCalls) {
            return exhaust("Model call budget reached (${status.modelCalls}/${budget.maxModelCalls})")
        }
        if (includeVision && status.visionCalls >= budget.maxVisionCalls) return false
        if (isReplan && status.replans >= budget.maxReplans) return false
        return true
    }

    fun record(usage: AgentUsage, usedVision: Boolean, isReplan: Boolean = false): AgentBudgetStatus {
        val nextUsage = status.usage + usage
        val next = status.copy(
            usage = nextUsage,
            modelCalls = status.modelCalls + 1,
            visionCalls = status.visionCalls + if (usedVision) 1 else 0,
            replans = status.replans + if (isReplan) 1 else 0,
            estimatedCost = estimateCost(nextUsage)
        )
        status = next.copy(mode = modeFor(next))
        if (nextUsage.promptTokens > budget.maxInputTokens || nextUsage.completionTokens > budget.maxOutputTokens) {
            exhaust("Token budget reached")
        }
        return status
    }

    private fun modeFor(value: AgentBudgetStatus): AgentBudgetMode {
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

    private fun estimateCost(usage: AgentUsage): Double =
        (usage.promptTokens - usage.cachedTokens).coerceAtLeast(0) / 1_000_000.0 * pricing.inputPerMillion +
            usage.cachedTokens / 1_000_000.0 * pricing.cachedInputPerMillion +
            usage.completionTokens / 1_000_000.0 * pricing.outputPerMillion

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
        activityName = observation.currentActivity.ifBlank { null },
        pageSignature = signature(observation),
        nodes = observation.uiNodes
    )

    fun signature(observation: AgentObservation): PageSignature {
        val packageName = observation.uiNodes.firstOrNull { it.packageName.isNotBlank() }?.packageName.orEmpty()
        val keyNodes = observation.uiNodes.take(32).joinToString("|") { node ->
            listOf(node.resourceId, node.text, node.contentDescription, node.role, node.clickable, node.editable).joinToString(":")
        }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$packageName\n${observation.currentActivity}\n$keyNodes".toByteArray())
            .joinToString("") { "%02x".format(it) }
        return PageSignature(digest.take(20), packageName, observation.currentActivity)
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

data class Selector(
    val resourceId: String? = null,
    val textAny: List<String> = emptyList(),
    val contentDescriptionAny: List<String> = emptyList(),
    val role: String? = null,
    val requireEnabled: Boolean = true
)

object SelectorResolver {
    fun resolve(selector: Selector, nodes: List<UiNodeSnapshot>): UiNodeSnapshot? = nodes
        .asSequence()
        .filter { !selector.requireEnabled || it.enabled }
        .filter { selector.resourceId.isNullOrBlank() || it.resourceId == selector.resourceId }
        .filter { selector.role.isNullOrBlank() || it.role == selector.role }
        .filter { selector.textAny.isEmpty() || selector.textAny.any { text -> it.text.equals(text, true) } }
        .filter { selector.contentDescriptionAny.isEmpty() || selector.contentDescriptionAny.any { text -> it.contentDescription.equals(text, true) } }
        .sortedByDescending { score(selector, it) }
        .firstOrNull()

    fun from(node: UiNodeSnapshot): Selector {
        val resourceId = node.resourceId.takeIf { it.isNotBlank() }
        return if (resourceId != null) {
            Selector(resourceId = resourceId, role = node.role.takeIf { it.isNotBlank() })
        } else {
            Selector(
                textAny = node.text.takeIf { it.isNotBlank() && it.length <= 80 }?.let(::listOf).orEmpty(),
                contentDescriptionAny = node.contentDescription.takeIf { it.isNotBlank() && it.length <= 80 }?.let(::listOf).orEmpty(),
                role = node.role.takeIf { it.isNotBlank() }
            )
        }
    }

    private fun score(selector: Selector, node: UiNodeSnapshot): Int =
        (if (!selector.resourceId.isNullOrBlank() && selector.resourceId == node.resourceId) 16 else 0) +
            (if (selector.textAny.any { it.equals(node.text, true) }) 8 else 0) +
            (if (selector.contentDescriptionAny.any { it.equals(node.contentDescription, true) }) 4 else 0) +
            (if (!selector.role.isNullOrBlank() && selector.role == node.role) 2 else 0)
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
