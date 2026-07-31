package com.ludoven.adbtool.agent

import java.util.prefs.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AgentContextSnapshot(
    val memoryText: String = "",
    val memoryIds: List<String> = emptyList(),
    val compactedHistory: String = "",
    val recentSteps: List<AgentStep> = emptyList(),
    val estimatedTokens: Int = 0,
    val compactionCount: Int = 0
)

class AgentContextManager(
    private val memoryStore: AgentMemoryStore?,
    private val memoryEnabled: Boolean,
    private val budgetStore: AgentContextBudgetStore = AgentContextBudgetStore()
) {
    suspend fun prepare(
        task: String,
        deviceId: String,
        packageName: String?,
        steps: List<AgentStep>,
        observation: AgentObservation,
        config: AiModelConfig,
        existingCompactions: Int
    ): AgentContextSnapshot {
        val memories = if (memoryEnabled && memoryStore != null) {
            memoryStore.search(
                AgentMemoryQuery(
                    text = buildString {
                        append(task)
                        packageName?.let { append(' ').append(it) }
                    },
                    deviceId = deviceId,
                    packageName = packageName
                )
            )
        } else {
            emptyList()
        }
        val memoryText = memories.joinToString("\n") {
            "- [${it.kind.name.lowercase()}] ${it.content}"
        }
        return compact(
            task = task,
            memoryText = memoryText,
            memoryIds = memories.map { it.id },
            steps = steps,
            observation = observation,
            config = config,
            existingCompactions = existingCompactions
        )
    }

    fun compact(
        task: String,
        memoryText: String,
        memoryIds: List<String>,
        steps: List<AgentStep>,
        observation: AgentObservation,
        config: AiModelConfig,
        existingCompactions: Int
    ): AgentContextSnapshot {
        val budget = budgetStore.effectiveBudget(config)
        val initialTokens = estimateTokens(task, memoryText, steps, observation.asText())
        if (initialTokens < (budget * COMPACTION_THRESHOLD_RATIO).toInt() || steps.size <= RECENT_STEP_COUNT) {
            return AgentContextSnapshot(
                memoryText = memoryText,
                memoryIds = memoryIds,
                recentSteps = steps,
                estimatedTokens = initialTokens,
                compactionCount = existingCompactions
            )
        }

        val oldSteps = steps.dropLast(RECENT_STEP_COUNT)
        val recent = steps.takeLast(RECENT_STEP_COUNT)
        val ledger = oldSteps.joinToString("\n") { step ->
            val status = step.status.name.lowercase()
            val result = step.result
                .replace(Regex("\\s+"), " ")
                .take(COMPACT_RESULT_CHARS)
                .ifBlank { status }
            "- ${step.action.toolName}: $status ($result)"
        }
        val estimated = estimateTokens(task, memoryText, recent, ledger + observation.asText())
        return AgentContextSnapshot(
            memoryText = memoryText,
            memoryIds = memoryIds,
            compactedHistory = ledger,
            recentSteps = recent,
            estimatedTokens = estimated,
            compactionCount = existingCompactions + 1
        )
    }

    suspend fun saveSuccessfulTask(
        task: String,
        deviceId: String,
        packageName: String?,
        finish: AgentAction.Finish,
        steps: List<AgentStep>
    ): Int {
        if (!memoryEnabled || memoryStore == null) return 0
        val store = memoryStore
        if (steps.any { it.status == AgentStepStatus.FAILED || it.status == AgentStepStatus.UNVERIFIED }) return 0
        var saved = 0
        finish.memoryCandidates.take(MAX_MEMORY_CANDIDATES_PER_TASK).forEach { candidate ->
            if (!candidateAllowed(candidate, task, steps)) return@forEach
            val provenance = if (
                candidate.kind == MemoryKind.USER_PREFERENCE &&
                candidate.sourceQuote.isNotBlank()
            ) {
                MemoryProvenance.USER_EXPLICIT
            } else {
                MemoryProvenance.VERIFIED_ACTION
            }
            val result = runCatching {
                store.upsert(
                    AgentMemory(
                        kind = candidate.kind,
                        scope = memoryScopeFor(candidate, deviceId, packageName),
                        content = candidate.content,
                        keywords = candidate.keywords,
                        importance = if (provenance == MemoryProvenance.USER_EXPLICIT) 0.9 else 0.65,
                        provenance = provenance
                    )
                )
            }
            if (result.isSuccess) saved += 1
        }

        val taskSummary = sanitizeTaskSummary(finish.summary)
        if (taskSummary.length >= 3) {
            val packages = steps.mapNotNull {
                when (val action = it.action) {
                    is AgentAction.LaunchPackage -> action.packageName
                    is AgentAction.ForceStopPackage -> action.packageName
                    is AgentAction.ClearAppData -> action.packageName
                    is AgentAction.UninstallPackage -> action.packageName
                    else -> null
                }
            }.distinct()
            runCatching {
                store.upsert(
                    AgentMemory(
                        kind = MemoryKind.TASK_SUMMARY,
                        scope = packageName?.let(MemoryScope::app) ?: MemoryScope.device(deviceId),
                        content = taskSummary,
                        keywords = (packages + steps.map { it.action.toolName }).distinct().joinToString(" "),
                        importance = 0.35,
                        provenance = MemoryProvenance.SANITIZED_TASK_SUMMARY
                    )
                )
            }.onSuccess { saved += 1 }
        }
        store.prune()
        return saved
    }

    fun recordContextOverflow(config: AiModelConfig) {
        budgetStore.reduceBudget(config)
    }

    fun needsModelCompaction(snapshot: AgentContextSnapshot): Boolean =
        snapshot.compactedHistory.isNotBlank() &&
            snapshot.estimatedTokens > MODEL_COMPACTION_TARGET_TOKENS

    fun applyModelCompaction(
        snapshot: AgentContextSnapshot,
        summary: String,
        task: String,
        observation: AgentObservation
    ): AgentContextSnapshot {
        val boundedSummary = summary.trim().take(MAX_MODEL_COMPACTION_CHARS)
        return snapshot.copy(
            compactedHistory = boundedSummary,
            estimatedTokens = estimateTokens(
                task,
                snapshot.memoryText,
                snapshot.recentSteps,
                boundedSummary,
                observation.asText()
            ),
            compactionCount = snapshot.compactionCount + 1
        )
    }

    private fun candidateAllowed(
        candidate: AgentMemoryCandidate,
        task: String,
        steps: List<AgentStep>
    ): Boolean {
        if (candidate.content.isBlank()) return false
        if (candidate.kind == MemoryKind.USER_PREFERENCE) {
            return candidate.sourceQuote.isNotBlank() &&
                task.contains(candidate.sourceQuote, ignoreCase = true)
        }
        if (candidate.kind == MemoryKind.APP_ALIAS) {
            return steps.any { it.action is AgentAction.FindApp && it.status == AgentStepStatus.COMPLETED }
        }
        if (candidate.kind == MemoryKind.VERIFIED_PROCEDURE) {
            return steps.isNotEmpty() && steps.all {
                it.status == AgentStepStatus.COMPLETED || it.status == AgentStepStatus.DENIED
            }
        }
        return candidate.kind == MemoryKind.DEVICE_FACT &&
            steps.any { it.status == AgentStepStatus.COMPLETED }
    }
}

class AgentContextBudgetStore(
    private val preferences: Preferences = Preferences.userNodeForPackage(AgentContextBudgetStore::class.java)
) {
    fun effectiveBudget(config: AiModelConfig): Int {
        config.contextWindowTokens?.let { return it }
        return preferences.getInt(key(config), DEFAULT_CONTEXT_BUDGET_TOKENS)
            .coerceIn(MIN_CONTEXT_BUDGET_TOKENS, MAX_CONTEXT_BUDGET_TOKENS)
    }

    fun reduceBudget(config: AiModelConfig) {
        if (config.contextWindowTokens != null) return
        val current = effectiveBudget(config)
        preferences.putInt(
            key(config),
            (current / 2).coerceAtLeast(MIN_CONTEXT_BUDGET_TOKENS)
        )
        preferences.flush()
    }

    private fun key(config: AiModelConfig): String =
        "agent.context.budget.${(config.baseUrl.trim() + "|" + config.model.trim()).hashCode()}"
}

internal fun estimateTokens(vararg values: Any): Int {
    var estimated = 0.0
    values.forEach { value ->
        val text = when (value) {
            is String -> value
            is Iterable<*> -> value.joinToString("\n")
            else -> value.toString()
        }
        text.forEach { char ->
            estimated += when {
                char.isWhitespace() -> 0.1
                char.code in 0x2E80..0x9FFF -> 1.0
                char.isLetterOrDigit() -> 0.25
                else -> 0.5
            }
        }
    }
    return estimated.toInt().coerceAtLeast(1)
}

private const val DEFAULT_CONTEXT_BUDGET_TOKENS = 16_000
private const val MIN_CONTEXT_BUDGET_TOKENS = 4_000
private const val MAX_CONTEXT_BUDGET_TOKENS = 1_000_000
private const val COMPACTION_THRESHOLD_RATIO = 0.60
private const val MODEL_COMPACTION_TARGET_TOKENS = 6_000
private const val MAX_MODEL_COMPACTION_CHARS = 2_800
private const val RECENT_STEP_COUNT = 6
private const val COMPACT_RESULT_CHARS = 160
private const val MAX_MEMORY_CANDIDATES_PER_TASK = 4
