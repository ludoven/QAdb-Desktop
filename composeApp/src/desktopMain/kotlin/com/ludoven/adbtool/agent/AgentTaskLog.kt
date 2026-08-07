package com.ludoven.adbtool.agent

import java.sql.Connection
import java.sql.DriverManager

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

interface AgentTaskLogStore {
    fun start(runId: String, task: String, state: AgentTaskUiState)
    fun record(runId: String, state: AgentTaskUiState)
    fun recent(limit: Int = 80): List<AgentTaskLogEntry>
}

/** Local, sanitized execution audit trail. It never stores input text or model request payloads. */
class SqliteAgentTaskLogStore : AgentTaskLogStore {
    private val connection: Connection
    private val lastFingerprintByRun = mutableMapOf<String, String>()

    init {
        Class.forName("org.sqlite.JDBC")
        connection = DriverManager.getConnection("jdbc:sqlite:${AgentDataPaths.memoryDatabase().absolutePath}")
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
        }
    }

    override fun start(runId: String, task: String, state: AgentTaskUiState) {
        insert(
            runId = runId,
            state = state,
            action = "task",
            status = "started",
            detail = "Task started (${task.codePointCount(0, task.length)} characters; text not stored)"
        )
    }

    override fun record(runId: String, state: AgentTaskUiState) {
        val step = state.steps.lastOrNull()
        val detail = state.errorMessage
            ?: state.executionDetails.lastOrNull()
            ?: step?.result
            ?: "Phase ${state.phase.name.lowercase()}"
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
        if (lastFingerprintByRun[runId] == fingerprint) return
        lastFingerprintByRun[runId] = fingerprint
        insert(runId, state, action, status, detail)
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

private fun String.sanitizeForLog(): String =
    replace(Regex("(?i)(api[_-]?key|authorization|token|password|passwd|pwd)\\s*[:=]\\s*[^\\s,;]+"), "$1=<redacted>")
        .replace(Regex("[\\r\\n\\t]+"), " ")
        .take(800)
