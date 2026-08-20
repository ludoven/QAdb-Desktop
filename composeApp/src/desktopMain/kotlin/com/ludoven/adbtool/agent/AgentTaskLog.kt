package com.ludoven.adbtool.agent

import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Types

data class AgentTaskLogEntry(
    val runId: String,
    val timestamp: Long,
    val phase: AgentRunPhase,
    val action: String,
    val status: String,
    val detail: String,
    val modelCalls: Int,
    val promptTokens: Int,
    val completionTokens: Int
)

data class AgentRunMetrics(
    val runId: String,
    val timestamp: Long,
    val result: AgentRunPhase,
    val strategy: AgentExecutionStrategy,
    val deviceChannel: AgentObservationSource,
    val elapsedMs: Long,
    val actionCount: Int,
    val modelCalls: Int,
    val totalTokens: Int,
    val visionCalls: Int,
    val recoveryCount: Int,
    val verificationFailure: String?,
    val benchmarkTaskId: String?,
    val observationTotalMs: Long,
    val observationHierarchyMs: Long,
    /** Null means the corresponding source did not report a trustworthy value for this run. */
    val intent: AgentTaskIntentKind? = null,
    val authority: AgentTaskAccessLevel? = null,
    val lastDecision: String? = null,
    val actionEffects: Set<AgentActionEffect> = emptySet(),
    val evidenceCount: Int? = null,
    val httpAttempts: Int? = null,
    val protocolRepairs: Int? = null,
    val firstFeedbackMs: Long? = null,
    val firstOutputMs: Long? = null,
    val engineVersion: AgentEngineVersion = AgentEngineVersion.V1,
    val semanticCommands: Int = 0,
    val primitiveActions: Int = 0,
    val visualGroundings: Int = 0,
    val localRecoveries: Int = 0,
    val groundingFailures: Int = 0,
    val failureSubsystem: AgentFailureSubsystem? = null,
    val failureStage: AgentFailureStage? = null,
    val failureCode: AgentFailureCode? = null
)

/**
 * Optional audit-only measurements emitted by the router, provider, and presentation layers.
 *
 * Every field is nullable on purpose: logical model calls are not HTTP attempts, and a missing
 * event must not be persisted as a fabricated zero. This object never accepts prompts, tool
 * arguments, headers, or other user/device content.
 */
data class AgentRunAuditTelemetry(
    val intent: AgentTaskIntentKind? = null,
    val authority: AgentTaskAccessLevel? = null,
    val lastDecision: String? = null,
    val actionEffects: Set<AgentActionEffect>? = null,
    val evidenceCount: Int? = null,
    val httpAttempts: Int? = null,
    val protocolRepairs: Int? = null,
    val firstFeedbackMs: Long? = null,
    val firstOutputMs: Long? = null
) {
    init {
        require(httpAttempts == null || httpAttempts >= 0) { "HTTP attempts cannot be negative" }
        require(protocolRepairs == null || protocolRepairs >= 0) { "Protocol repairs cannot be negative" }
        require(firstFeedbackMs == null || firstFeedbackMs >= 0) { "First feedback latency cannot be negative" }
        require(firstOutputMs == null || firstOutputMs >= 0) { "First output latency cannot be negative" }
        require(lastDecision == null || lastDecision.matches(Regex("[a-z_]+"))) {
            "Decision audit value must be a stable code"
        }
        require(evidenceCount == null || evidenceCount >= 0) { "Evidence count cannot be negative" }
    }

    internal fun merge(newer: AgentRunAuditTelemetry): AgentRunAuditTelemetry = AgentRunAuditTelemetry(
        intent = newer.intent ?: intent,
        authority = newer.authority ?: authority,
        lastDecision = newer.lastDecision ?: lastDecision,
        actionEffects = when {
            actionEffects == null && newer.actionEffects == null -> null
            else -> actionEffects.orEmpty() + newer.actionEffects.orEmpty()
        },
        evidenceCount = newer.evidenceCount ?: evidenceCount,
        httpAttempts = newer.httpAttempts ?: httpAttempts,
        protocolRepairs = newer.protocolRepairs ?: protocolRepairs,
        firstFeedbackMs = newer.firstFeedbackMs ?: firstFeedbackMs,
        firstOutputMs = newer.firstOutputMs ?: firstOutputMs
    )
}

interface AgentTaskLogStore {
    fun start(runId: String, task: String, state: AgentTaskUiState)
    fun record(runId: String, state: AgentTaskUiState)
    fun recordTelemetry(runId: String, telemetry: AgentRunAuditTelemetry) = Unit
    fun recent(limit: Int = 80): List<AgentTaskLogEntry>
    fun recentMetrics(limit: Int = 80): List<AgentRunMetrics> = emptyList()
}

/** Local, sanitized execution audit trail. It never stores input text or model request payloads. */
class SqliteAgentTaskLogStore(
    databaseFile: File = AgentDataPaths.memoryDatabase()
) : AgentTaskLogStore, AutoCloseable {
    private val connection: Connection
    private val lastFingerprintByRun = mutableMapOf<String, String>()
    private val startedAtByRun = mutableMapOf<String, Long>()
    private val telemetryByRun = mutableMapOf<String, AgentRunAuditTelemetry>()

    init {
        Class.forName("org.sqlite.JDBC")
        databaseFile.parentFile?.mkdirs()
        connection = DriverManager.getConnection("jdbc:sqlite:${databaseFile.absolutePath}")
        connection.createStatement().use { statement ->
            statement.execute(
                """CREATE TABLE IF NOT EXISTS agent_task_log (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    run_id TEXT NOT NULL,
                    created_at INTEGER NOT NULL,
                    phase TEXT NOT NULL,
                    action TEXT NOT NULL,
                    status TEXT NOT NULL,
                    detail TEXT NOT NULL,
                    model_calls INTEGER NOT NULL,
                    prompt_tokens INTEGER NOT NULL,
                    completion_tokens INTEGER NOT NULL
                )"""
            )
            statement.execute("CREATE INDEX IF NOT EXISTS agent_task_log_created_idx ON agent_task_log(created_at DESC)")
            statement.execute(
                """CREATE TABLE IF NOT EXISTS agent_run_metrics (
                    run_id TEXT PRIMARY KEY,
                    created_at INTEGER NOT NULL,
                    result TEXT NOT NULL,
                    strategy TEXT NOT NULL,
                    device_channel TEXT NOT NULL,
                    elapsed_ms INTEGER NOT NULL,
                    action_count INTEGER NOT NULL,
                    model_calls INTEGER NOT NULL,
                    total_tokens INTEGER NOT NULL,
                    vision_calls INTEGER NOT NULL,
                    recovery_count INTEGER NOT NULL,
                    verification_failure TEXT,
                    benchmark_task_id TEXT,
                    observation_total_ms INTEGER NOT NULL,
                    observation_hierarchy_ms INTEGER NOT NULL,
                    intent TEXT,
                    authority TEXT,
                    last_decision TEXT,
                    action_effects TEXT,
                    evidence_count INTEGER,
                    http_attempts INTEGER,
                    protocol_repairs INTEGER,
                    first_feedback_ms INTEGER,
                    first_output_ms INTEGER,
                    engine_version TEXT,
                    semantic_commands INTEGER,
                    primitive_actions INTEGER,
                    visual_groundings INTEGER,
                    local_recoveries INTEGER,
                    grounding_failures INTEGER,
                    failure_subsystem TEXT,
                    failure_stage TEXT,
                    failure_code TEXT
                )"""
            )
            statement.execute("CREATE INDEX IF NOT EXISTS agent_run_metrics_created_idx ON agent_run_metrics(created_at DESC)")
        }
        migrateRunMetricsSchema()
    }

    override fun start(runId: String, task: String, state: AgentTaskUiState) {
        synchronized(connection) {
            val startedAt = System.currentTimeMillis()
            insert(
                runId = runId,
                state = state,
                action = "task",
                status = "started",
                detail = "Task started (${task.codePointCount(0, task.length)} characters; text not stored)"
            )
            startedAtByRun[runId] = startedAt
        }
    }

    override fun record(runId: String, state: AgentTaskUiState) {
        synchronized(connection) {
            val terminal = state.phase in TERMINAL_PHASES
            if (terminal && hasRecordedMetrics(runId)) {
                clearRunState(runId)
                return@synchronized
            }
            val step = state.steps.lastOrNull()
            val detail = (state.failure?.technicalDetail?.takeIf(String::isStableAuditCode)
                ?: state.errorMessage?.toSafeFailureLogDetail()
                ?: step?.let { if (it.containsSensitiveData) "Sensitive action result omitted" else it.result }
                ?: state.executionDetails.lastOrNull()
                ?: "Phase ${state.phase.name.lowercase()}")
                .sanitizeForLog()
            val action = step?.action?.toLogLabel().orEmpty()
            val status = step?.status?.name?.lowercase() ?: state.phase.name.lowercase()
            val fingerprint = listOf(
                state.phase.name,
                step?.id.orEmpty(),
                status,
                detail,
                state.budgetStatus.modelCalls,
                state.usage.totalTokens
            ).joinToString("|")
            if (lastFingerprintByRun[runId] != fingerprint) {
                insert(runId, state, action, status, detail)
                lastFingerprintByRun[runId] = fingerprint
            }
            if (terminal) {
                insertMetrics(runId, state)
                clearRunState(runId)
            }
        }
    }

    override fun recordTelemetry(runId: String, telemetry: AgentRunAuditTelemetry) = synchronized(connection) {
        require(runId.isNotBlank()) { "Run id is required" }
        val merged = telemetryByRun[runId]?.merge(telemetry) ?: telemetry
        telemetryByRun[runId] = merged
        val updatedRows = connection.prepareStatement(
            """UPDATE agent_run_metrics SET
                intent = COALESCE(?, intent),
                authority = COALESCE(?, authority),
                last_decision = COALESCE(?, last_decision),
                action_effects = COALESCE(?, action_effects),
                evidence_count = COALESCE(?, evidence_count),
                http_attempts = COALESCE(?, http_attempts),
                protocol_repairs = COALESCE(?, protocol_repairs),
                first_feedback_ms = COALESCE(?, first_feedback_ms),
                first_output_ms = COALESCE(?, first_output_ms)
               WHERE run_id = ?"""
        ).use { statement ->
            statement.setNullableString(1, merged.intent?.name)
            statement.setNullableString(2, merged.authority?.name)
            statement.setNullableString(3, merged.lastDecision)
            statement.setNullableString(4, merged.actionEffects?.toAuditText())
            statement.setNullableInt(5, merged.evidenceCount)
            statement.setNullableInt(6, merged.httpAttempts)
            statement.setNullableInt(7, merged.protocolRepairs)
            statement.setNullableLong(8, merged.firstFeedbackMs)
            statement.setNullableLong(9, merged.firstOutputMs)
            statement.setString(10, runId)
            statement.executeUpdate()
        }
        if (updatedRows > 0) telemetryByRun.remove(runId)
        Unit
    }

    override fun recent(limit: Int): List<AgentTaskLogEntry> = synchronized(connection) {
        connection.prepareStatement(
            """SELECT run_id, created_at, phase, action, status, detail, model_calls, prompt_tokens, completion_tokens
               FROM agent_task_log ORDER BY id DESC LIMIT ?"""
        ).use { statement ->
            statement.setInt(1, limit.coerceIn(1, 500))
            statement.executeQuery().use { result ->
                buildList {
                    while (result.next()) {
                        add(
                            AgentTaskLogEntry(
                                runId = result.getString("run_id"),
                                timestamp = result.getLong("created_at"),
                                phase = runCatching { AgentRunPhase.valueOf(result.getString("phase")) }
                                    .getOrDefault(AgentRunPhase.IDLE),
                                action = result.getString("action"),
                                status = result.getString("status"),
                                detail = result.getString("detail"),
                                modelCalls = result.getInt("model_calls"),
                                promptTokens = result.getInt("prompt_tokens"),
                                completionTokens = result.getInt("completion_tokens")
                            )
                        )
                    }
                }
            }
        }
    }

    override fun recentMetrics(limit: Int): List<AgentRunMetrics> = synchronized(connection) {
        connection.prepareStatement(
            """SELECT run_id, created_at, result, strategy, device_channel, elapsed_ms, action_count,
               model_calls, total_tokens, vision_calls, recovery_count, verification_failure,
               benchmark_task_id, observation_total_ms, observation_hierarchy_ms, intent,
               authority, last_decision, action_effects, evidence_count,
               http_attempts, protocol_repairs, first_feedback_ms, first_output_ms,
               engine_version, semantic_commands, primitive_actions, visual_groundings,
               local_recoveries, grounding_failures, failure_subsystem, failure_stage, failure_code
               FROM agent_run_metrics ORDER BY created_at DESC LIMIT ?"""
        ).use { statement ->
            statement.setInt(1, limit.coerceIn(1, 500))
            statement.executeQuery().use { result ->
                buildList {
                    while (result.next()) {
                        add(AgentRunMetrics(
                            runId = result.getString("run_id"),
                            timestamp = result.getLong("created_at"),
                            result = runCatching { AgentRunPhase.valueOf(result.getString("result")) }
                                .getOrDefault(AgentRunPhase.FAILED),
                            strategy = runCatching { AgentExecutionStrategy.valueOf(result.getString("strategy")) }
                                .getOrDefault(AgentExecutionStrategy.INTERACTIVE),
                            deviceChannel = runCatching { AgentObservationSource.valueOf(result.getString("device_channel")) }
                                .getOrDefault(AgentObservationSource.ADB),
                            elapsedMs = result.getLong("elapsed_ms"),
                            actionCount = result.getInt("action_count"),
                            modelCalls = result.getInt("model_calls"),
                            totalTokens = result.getInt("total_tokens"),
                            visionCalls = result.getInt("vision_calls"),
                            recoveryCount = result.getInt("recovery_count"),
                            verificationFailure = result.getString("verification_failure"),
                            benchmarkTaskId = result.getString("benchmark_task_id"),
                            observationTotalMs = result.getLong("observation_total_ms"),
                            observationHierarchyMs = result.getLong("observation_hierarchy_ms"),
                            intent = result.getString("intent")?.let { value ->
                                runCatching { AgentTaskIntentKind.valueOf(value) }.getOrNull()
                            },
                            authority = result.getString("authority")?.let { value ->
                                runCatching { AgentTaskAccessLevel.valueOf(value) }.getOrNull()
                            },
                            lastDecision = result.getString("last_decision"),
                            actionEffects = result.getString("action_effects").toActionEffects(),
                            evidenceCount = result.getNullableInt("evidence_count"),
                            httpAttempts = result.getNullableInt("http_attempts"),
                            protocolRepairs = result.getNullableInt("protocol_repairs"),
                            firstFeedbackMs = result.getNullableLong("first_feedback_ms"),
                            firstOutputMs = result.getNullableLong("first_output_ms"),
                            engineVersion = result.getString("engine_version")?.let { value ->
                                runCatching { AgentEngineVersion.valueOf(value) }.getOrNull()
                            } ?: AgentEngineVersion.V1,
                            semanticCommands = result.getNullableInt("semantic_commands") ?: 0,
                            primitiveActions = result.getNullableInt("primitive_actions") ?: 0,
                            visualGroundings = result.getNullableInt("visual_groundings") ?: 0,
                            localRecoveries = result.getNullableInt("local_recoveries") ?: 0,
                            groundingFailures = result.getNullableInt("grounding_failures") ?: 0,
                            failureSubsystem = result.getString("failure_subsystem")?.let { value ->
                                runCatching { AgentFailureSubsystem.valueOf(value) }.getOrNull()
                            },
                            failureStage = result.getString("failure_stage")?.let { value ->
                                runCatching { AgentFailureStage.valueOf(value) }.getOrNull()
                            },
                            failureCode = result.getString("failure_code")?.let { value ->
                                runCatching { AgentFailureCode.valueOf(value) }.getOrNull()
                            }
                        ))
                    }
                }
            }
        }
    }

    private fun insertMetrics(runId: String, state: AgentTaskUiState) = synchronized(connection) {
        val now = System.currentTimeMillis()
        val telemetry = telemetryByRun[runId] ?: AgentRunAuditTelemetry()
        val startedAt = startedAtByRun[runId] ?: now
        val verificationFailure = state.failure?.technicalDetail?.toSafeStableFailureCode()
            ?: state.failure?.let { "failure_code=${it.code.name.lowercase()}" }
            ?: state.errorMessage?.toSafeVerificationFailure()
        connection.prepareStatement(
            """INSERT OR IGNORE INTO agent_run_metrics(
                run_id, created_at, result, strategy, device_channel, elapsed_ms, action_count,
                model_calls, total_tokens, vision_calls, recovery_count, verification_failure,
                benchmark_task_id, observation_total_ms, observation_hierarchy_ms, intent,
                authority, last_decision, action_effects, evidence_count,
                http_attempts, protocol_repairs, first_feedback_ms, first_output_ms,
                engine_version, semantic_commands, primitive_actions, visual_groundings,
                local_recoveries, grounding_failures, failure_subsystem, failure_stage, failure_code
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"""
        ).use { statement ->
            statement.setString(1, runId)
            statement.setLong(2, now)
            statement.setString(3, state.phase.name)
            statement.setString(4, state.executionStrategy.name)
            statement.setString(5, state.deviceChannel.name)
            statement.setLong(6, now - startedAt)
            statement.setInt(
                7,
                if (state.executionStrategy == AgentExecutionStrategy.SEMANTIC_V2) {
                    state.v2Metrics.primitiveActions
                } else {
                    state.steps.sumOf { step ->
                        if (step.action.countsAsDeviceAction()) step.executedActionCount else 0
                    }
                }
            )
            statement.setInt(8, state.budgetStatus.modelCalls)
            statement.setInt(9, state.usage.totalTokens)
            statement.setInt(10, state.budgetStatus.visionCalls)
            statement.setInt(11, state.budgetStatus.replans)
            statement.setString(12, verificationFailure)
            statement.setString(13, state.benchmarkTaskId)
            statement.setLong(14, state.observationTimings.totalMs)
            statement.setLong(15, state.observationTimings.hierarchyMs)
            statement.setNullableString(16, telemetry.intent?.name)
            statement.setNullableString(17, telemetry.authority?.name)
            statement.setNullableString(18, telemetry.lastDecision)
            statement.setNullableString(19, telemetry.actionEffects?.toAuditText())
            statement.setNullableInt(20, telemetry.evidenceCount)
            statement.setNullableInt(21, telemetry.httpAttempts)
            statement.setNullableInt(22, telemetry.protocolRepairs)
            statement.setNullableLong(23, telemetry.firstFeedbackMs)
            statement.setNullableLong(24, telemetry.firstOutputMs)
            statement.setString(
                25,
                if (state.executionStrategy == AgentExecutionStrategy.SEMANTIC_V2) {
                    AgentEngineVersion.V2.name
                } else {
                    AgentEngineVersion.SCREENSHOT.name
                }
            )
            statement.setInt(26, state.v2Metrics.semanticCommands)
            statement.setInt(27, state.v2Metrics.primitiveActions)
            statement.setInt(28, state.v2Metrics.visualGroundings)
            statement.setInt(29, state.v2Metrics.localRecoveries)
            statement.setInt(30, state.v2Metrics.groundingFailures)
            statement.setNullableString(31, state.failure?.subsystem?.name)
            statement.setNullableString(32, state.failure?.stage?.name)
            statement.setNullableString(33, state.failure?.code?.name)
            statement.executeUpdate()
        }
    }

    private fun hasRecordedMetrics(runId: String): Boolean = connection.prepareStatement(
        "SELECT 1 FROM agent_run_metrics WHERE run_id = ? LIMIT 1"
    ).use { statement ->
        statement.setString(1, runId)
        statement.executeQuery().use { it.next() }
    }

    private fun clearRunState(runId: String) {
        lastFingerprintByRun.remove(runId)
        startedAtByRun.remove(runId)
        telemetryByRun.remove(runId)
    }

    private fun migrateRunMetricsSchema() = synchronized(connection) {
        val existing = connection.createStatement().use { statement ->
            statement.executeQuery("PRAGMA table_info(agent_run_metrics)").use { result ->
                buildSet {
                    while (result.next()) add(result.getString("name"))
                }
            }
        }
        RUN_METRICS_ADDITIVE_COLUMNS.forEach { column ->
            if (column.name !in existing) {
                connection.createStatement().use { statement ->
                    statement.execute("ALTER TABLE agent_run_metrics ADD COLUMN ${column.name} ${column.type}")
                }
            }
        }
    }

    private fun insert(
        runId: String,
        state: AgentTaskUiState,
        action: String,
        status: String,
        detail: String
    ) = synchronized(connection) {
        connection.prepareStatement(
            """INSERT INTO agent_task_log(
                run_id, created_at, phase, action, status, detail, model_calls, prompt_tokens, completion_tokens
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"""
        ).use { statement ->
            statement.setString(1, runId)
            statement.setLong(2, System.currentTimeMillis())
            statement.setString(3, state.phase.name)
            statement.setString(4, action)
            statement.setString(5, status)
            statement.setString(6, detail.sanitizeForLog())
            statement.setInt(7, state.budgetStatus.modelCalls)
            statement.setInt(8, state.usage.promptTokens)
            statement.setInt(9, state.usage.completionTokens)
            statement.executeUpdate()
        }
    }

    override fun close() {
        synchronized(connection) {
            if (!connection.isClosed) connection.close()
        }
    }
}

object AgentTaskLogRuntime {
    val store: AgentTaskLogStore by lazy { SqliteAgentTaskLogStore() }
}

object NoopAgentTaskLogStore : AgentTaskLogStore {
    override fun start(runId: String, task: String, state: AgentTaskUiState) = Unit
    override fun record(runId: String, state: AgentTaskUiState) = Unit
    override fun recent(limit: Int): List<AgentTaskLogEntry> = emptyList()
}

private fun AgentAction.toLogLabel(): String = when (this) {
    is AgentAction.InputText -> "input_text (${text.codePointCount(0, text.length)} characters)"
    else -> toolName
}

private fun String.isStableAuditCode(): Boolean =
    matches(Regex("[a-z_]+(?:=[a-z0-9_]+)?"))

private fun String.toSafeStableFailureCode(): String? =
    takeIf(String::isStableAuditCode)

private fun AgentAction.countsAsDeviceAction(): Boolean = when (this) {
    AgentAction.Observe,
    is AgentAction.FindApp,
    is AgentAction.Wait,
    is AgentAction.Finish -> false
    else -> true
}

private fun String.sanitizeForLog(): String =
    replace(AUTHORIZATION_VALUE_REGEX, "$1<redacted>")
        .replace(SECRET_VALUE_REGEX, "$1<redacted>")
        .replace(STANDALONE_BEARER_REGEX, "$1<redacted>")
        .replace(Regex("[\\r\\n\\t]+"), " ")
        .take(800)

private fun String.toSafeFailureLogDetail(): String =
    "Task failed (category=${agentFailureFrom(this).category.name.lowercase()}; detail omitted)"

private fun String.toSafeVerificationFailure(): String? =
    takeIf { it.contains("verif", ignoreCase = true) || it.contains("goal", ignoreCase = true) }
        ?.let { SAFE_VERIFICATION_FAILURE }

private val TERMINAL_PHASES = setOf(AgentRunPhase.COMPLETED, AgentRunPhase.FAILED, AgentRunPhase.CANCELLED)
private const val SAFE_VERIFICATION_FAILURE = "verification_or_goal_failure"

private val AUTHORIZATION_VALUE_REGEX = Regex(
    """(?i)((?:["']?authorization["']?)\s*[:=]\s*)(?:"[^"]*"|'[^']*'|[^\r\n,;}\]]+)"""
)
private val SECRET_VALUE_REGEX = Regex(
    """(?i)((?:["']?(?:api[-_ ]?key|access[-_ ]?token|refresh[-_ ]?token|token|password|passwd|pwd|client[-_ ]?secret|secret)["']?)\s*[:=]\s*)(?:"[^"]*"|'[^']*'|[^\r\n,;}\]]+)"""
)
private val STANDALONE_BEARER_REGEX = Regex("""(?i)(\bbearer\s+)[^\s,;}\]]+""")

private data class AdditiveColumn(val name: String, val type: String)

private val RUN_METRICS_ADDITIVE_COLUMNS = listOf(
    AdditiveColumn("intent", "TEXT"),
    AdditiveColumn("authority", "TEXT"),
    AdditiveColumn("last_decision", "TEXT"),
    AdditiveColumn("action_effects", "TEXT"),
    AdditiveColumn("evidence_count", "INTEGER"),
    AdditiveColumn("http_attempts", "INTEGER"),
    AdditiveColumn("protocol_repairs", "INTEGER"),
    AdditiveColumn("first_feedback_ms", "INTEGER"),
    AdditiveColumn("first_output_ms", "INTEGER"),
    AdditiveColumn("engine_version", "TEXT"),
    AdditiveColumn("semantic_commands", "INTEGER"),
    AdditiveColumn("primitive_actions", "INTEGER"),
    AdditiveColumn("visual_groundings", "INTEGER"),
    AdditiveColumn("local_recoveries", "INTEGER"),
    AdditiveColumn("grounding_failures", "INTEGER"),
    AdditiveColumn("failure_subsystem", "TEXT"),
    AdditiveColumn("failure_stage", "TEXT"),
    AdditiveColumn("failure_code", "TEXT")
)

private fun java.sql.PreparedStatement.setNullableString(index: Int, value: String?) {
    if (value == null) setNull(index, Types.VARCHAR) else setString(index, value)
}

private fun java.sql.PreparedStatement.setNullableInt(index: Int, value: Int?) {
    if (value == null) setNull(index, Types.INTEGER) else setInt(index, value)
}

private fun java.sql.PreparedStatement.setNullableLong(index: Int, value: Long?) {
    if (value == null) setNull(index, Types.INTEGER) else setLong(index, value)
}

private fun java.sql.ResultSet.getNullableInt(column: String): Int? =
    getInt(column).let { value -> if (wasNull()) null else value }

private fun java.sql.ResultSet.getNullableLong(column: String): Long? =
    getLong(column).let { value -> if (wasNull()) null else value }

private fun Set<AgentActionEffect>.toAuditText(): String =
    sortedBy(AgentActionEffect::name).joinToString(",", transform = AgentActionEffect::name)

private fun String?.toActionEffects(): Set<AgentActionEffect> =
    orEmpty().split(',').mapNotNullTo(linkedSetOf()) { value ->
        value.takeIf(String::isNotBlank)?.let { runCatching { AgentActionEffect.valueOf(it) }.getOrNull() }
    }
