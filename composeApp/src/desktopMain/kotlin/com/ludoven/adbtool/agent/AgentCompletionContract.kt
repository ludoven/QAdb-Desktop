package com.ludoven.adbtool.agent

import java.security.MessageDigest

enum class CompletionVerificationVerdict { MATCHED, NOT_MATCHED, NOT_APPLICABLE }

data class CompletionActionRevision(
    val beforeRevision: Long,
    val afterRevision: Long,
    val executed: Boolean,
    val effect: AgentActionEffect? = null
) {
    init {
        require(beforeRevision >= 0 && afterRevision >= 0) { "Completion revisions cannot be negative" }
    }
}

data class CompletionEvidence(
    val freshRevision: Long?,
    val deviceActionCount: Int,
    val deterministicGoalFingerprint: String?,
    val verification: CompletionVerificationVerdict,
    val actionRevisions: List<CompletionActionRevision> = emptyList(),
    val typedBinding: Boolean = true
) {
    init {
        require(freshRevision == null || freshRevision >= 0) { "Fresh revision cannot be negative" }
        require(deviceActionCount >= 0) { "Device action count cannot be negative" }
        require(actionRevisions.count { it.executed } <= deviceActionCount) {
            "Executed revision evidence cannot exceed the device action count"
        }
    }

    val hasFreshEvidence: Boolean get() = freshRevision != null
    val deterministicGoalProvided: Boolean get() = deterministicGoalFingerprint != null
    val deterministicGoalMatched: Boolean get() = verification == CompletionVerificationVerdict.MATCHED

    companion object {
        fun legacy(
            hasFreshEvidence: Boolean,
            deviceActionCount: Int,
            deterministicGoalProvided: Boolean,
            deterministicGoalMatched: Boolean
        ) = CompletionEvidence(
            freshRevision = 0L.takeIf { hasFreshEvidence },
            deviceActionCount = deviceActionCount,
            deterministicGoalFingerprint = "legacy".takeIf { deterministicGoalProvided },
            verification = when {
                deterministicGoalMatched -> CompletionVerificationVerdict.MATCHED
                deterministicGoalProvided -> CompletionVerificationVerdict.NOT_MATCHED
                else -> CompletionVerificationVerdict.NOT_APPLICABLE
            },
            typedBinding = false
        )
    }
}

data class CompletionContractInput(
    val intent: AgentTaskIntent,
    val evidence: CompletionEvidence,
    val steps: List<AgentStep>
) {
    constructor(
        intent: AgentTaskIntent,
        hasFreshEvidence: Boolean,
        deviceActionCount: Int,
        deterministicGoalProvided: Boolean,
        deterministicGoalMatched: Boolean,
        steps: List<AgentStep>
    ) : this(
        intent = intent,
        evidence = CompletionEvidence.legacy(
            hasFreshEvidence,
            deviceActionCount,
            deterministicGoalProvided,
            deterministicGoalMatched
        ),
        steps = steps
    )
}

sealed interface CompletionContractResult {
    data object Passed : CompletionContractResult
    data class Rejected(val reason: String) : CompletionContractResult
}

/** Kotlin-owned completion boundary. A model finish call can never override these checks. */
class CompletionContract {
    fun evaluate(input: CompletionContractInput): CompletionContractResult {
        val unresolved = input.steps.count { step ->
            step.status in setOf(
                AgentStepStatus.RUNNING,
                AgentStepStatus.AWAITING_CONFIRMATION,
                AgentStepStatus.DENIED,
                AgentStepStatus.UNVERIFIED,
                AgentStepStatus.FAILED
            )
        }
        if (unresolved > 0) {
            return CompletionContractResult.Rejected("$unresolved step(s) remain failed or unverified")
        }

        val evidence = input.evidence
        if (evidence.typedBinding) {
            if (!evidence.hasFreshEvidence) {
                return CompletionContractResult.Rejected("typed completion evidence has no fresh revision")
            }
            if (evidence.actionRevisions.any { it.executed && it.afterRevision < it.beforeRevision }) {
                return CompletionContractResult.Rejected("typed completion evidence contains a regressed revision")
            }
        }

        return if (input.intent.accessLevel == AgentTaskAccessLevel.MUTATING) {
            when {
                !evidence.deterministicGoalProvided -> CompletionContractResult.Rejected(
                    "no deterministic completion condition was provided for the device operation"
                )
                !evidence.deterministicGoalMatched -> CompletionContractResult.Rejected(
                    "the deterministic completion condition was not verified on fresh evidence"
                )
                evidence.typedBinding && evidence.deviceActionCount > 0 && evidence.actionRevisions.none { it.executed } ->
                    CompletionContractResult.Rejected("typed completion evidence is not bound to an executed action")
                else -> CompletionContractResult.Passed
            }
        } else if (input.intent.accessLevel == AgentTaskAccessLevel.NAVIGATION_READ_ONLY) {
            val executedActions = evidence.actionRevisions.filter(CompletionActionRevision::executed)
            when {
                !evidence.deterministicGoalProvided -> CompletionContractResult.Rejected(
                    "no deterministic completion condition was provided for the navigation read"
                )
                !evidence.deterministicGoalMatched -> CompletionContractResult.Rejected(
                    "the navigation read was not verified on fresh evidence"
                )
                evidence.typedBinding &&
                    evidence.deviceActionCount > 0 &&
                    executedActions.size != evidence.deviceActionCount ->
                    CompletionContractResult.Rejected(
                        "navigation-read actions are not fully bound to typed effect evidence"
                    )
                evidence.typedBinding && executedActions.any { it.effect == null } -> CompletionContractResult.Rejected(
                    "navigation-read action evidence is missing an effect"
                )
                evidence.typedBinding && executedActions.any { action ->
                    action.effect?.let(input.intent.authority::allows) != true
                } -> CompletionContractResult.Rejected(
                    "a navigation-read task attempted an effect outside its frozen authority"
                )
                !evidence.hasFreshEvidence -> CompletionContractResult.Rejected(
                    "the navigation-read answer has no fresh evidence"
                )
                else -> CompletionContractResult.Passed
            }
        } else {
            when {
                evidence.deviceActionCount != 0 -> CompletionContractResult.Rejected(
                    "a read-only task attempted ${evidence.deviceActionCount} device action(s)"
                )
                !evidence.hasFreshEvidence -> CompletionContractResult.Rejected(
                    "the read-only answer has no fresh evidence"
                )
                else -> CompletionContractResult.Passed
            }
        }
    }
}

internal fun SemanticGoal.completionFingerprint(): String {
    val canonical = listOf(
        kind.name,
        appRef.orEmpty(),
        app.orEmpty(),
        target.orEmpty(),
        value.orEmpty(),
        readContentSpec?.surface.orEmpty(),
        readContentSpec?.mode?.name.orEmpty(),
        readContentSpec?.query.orEmpty(),
        finalNavigation.name
    )
        .joinToString("\u0000")
    return MessageDigest.getInstance("SHA-256")
        .digest(canonical.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
