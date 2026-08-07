package com.ludoven.adbtool.agent

import java.security.MessageDigest
import java.sql.Connection
import java.sql.DriverManager
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val WORKFLOW_SCHEMA_VERSION = 2
private const val MAX_KNOWLEDGE_LENGTH = 4_000
private const val DEFAULT_GUARD_THRESHOLD = 0.85

enum class WorkflowStatus { DRAFT, ENABLED, DISABLED }
enum class WorkflowParameterType { TEXT, PACKAGE_NAME, FILE_PATH }

data class WorkflowParameter(val name: String, val type: WorkflowParameterType, val defaultValue: String = "")
data class WorkflowStep(val action: AgentAction, val verification: VerificationRule? = null)
data class WorkflowVerification(val pageSignature: String, val requiredSelector: Selector? = null)
data class WorkflowStatistics(val successCount: Int = 0, val failureCount: Int = 0, val averageDurationMs: Long = 0) {
    val successRate: Double get() = successCount.toDouble() / (successCount + failureCount).coerceAtLeast(1)
}

/** A compact, non-reversible page checkpoint. Full UI text and element ids never leave memory. */
data class WorkflowStateGuard(
    val packageName: String,
    val activityName: String,
    val semanticKeyHashes: Set<String>,
    val threshold: Double = DEFAULT_GUARD_THRESHOLD
)

sealed interface WorkflowReplayAction {
    data class KeyEvent(val key: AgentKey) : WorkflowReplayAction
    data class LaunchPackage(val packageName: String) : WorkflowReplayAction
    data class Wait(val durationMs: Int) : WorkflowReplayAction
    data class TapSelector(val selector: Selector) : WorkflowReplayAction
}

data class WorkflowReplayStep(
    val action: WorkflowReplayAction,
    val before: WorkflowStateGuard,
    val after: WorkflowStateGuard
)

data class WorkflowStateMatch(val matches: Boolean, val score: Double, val reason: String = "")

data class AgentAppKnowledgeCard(
    val id: String = UUID.randomUUID().toString(),
    val packageName: String,
    val title: String,
    val guide: String,
    val enabled: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)

data class AgentWorkflow(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val taskPattern: String,
    val packageName: String = "",
    val appVersion: String = "",
    val status: WorkflowStatus = WorkflowStatus.DRAFT,
    val parameters: List<WorkflowParameter> = emptyList(),
    /** Legacy v1 data is retained for display only. */
    val steps: List<WorkflowStep> = emptyList(),
    val verification: WorkflowVerification? = null,
    val statistics: WorkflowStatistics = WorkflowStatistics(),
    val schemaVersion: Int = WORKFLOW_SCHEMA_VERSION,
    val initialGuard: WorkflowStateGuard? = null,
    val completionGuard: WorkflowStateGuard? = null,
    val replaySteps: List<WorkflowReplayStep> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt
) {
    val canEnable: Boolean get() = schemaVersion == WORKFLOW_SCHEMA_VERSION &&
        initialGuard != null && completionGuard != null && replaySteps.isNotEmpty()
}

typealias WorkflowDraft = AgentWorkflow

interface AgentAppKnowledgeStore {
    suspend fun listKnowledgeCards(packageName: String? = null, enabledOnly: Boolean = false): List<AgentAppKnowledgeCard>
    suspend fun saveKnowledgeCard(card: AgentAppKnowledgeCard): AgentAppKnowledgeCard
    suspend fun deleteKnowledgeCard(id: String)
}

interface AgentWorkflowStore : AutoCloseable, AgentAppKnowledgeStore {
    suspend fun list(includeDrafts: Boolean = true): List<AgentWorkflow>
    suspend fun save(workflow: AgentWorkflow): AgentWorkflow
    suspend fun delete(id: String)
    suspend fun findEnabled(task: String, state: DeviceState): AgentWorkflow?
    suspend fun recordResult(id: String, success: Boolean, durationMs: Long)
}

class SqliteAgentWorkflowStore : AgentWorkflowStore {
    private val connection: Connection = DriverManager.getConnection("jdbc:sqlite:${AgentDataPaths.memoryDatabase().absolutePath}")

    init {
        Class.forName("org.sqlite.JDBC")
        synchronized(connection) {
            connection.createStatement().use { statement ->
                statement.execute("""CREATE TABLE IF NOT EXISTS agent_workflow (
                    id TEXT PRIMARY KEY, name TEXT NOT NULL, task_pattern TEXT NOT NULL,
                    package_name TEXT NOT NULL, app_version TEXT NOT NULL, status TEXT NOT NULL,
                    steps TEXT NOT NULL, signature TEXT NOT NULL, success_count INTEGER NOT NULL,
                    failure_count INTEGER NOT NULL, average_duration INTEGER NOT NULL,
                    created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL
                )""".trimIndent())
                ensureColumn(statement, "agent_workflow", "schema_version", "INTEGER NOT NULL DEFAULT 1")
                ensureColumn(statement, "agent_workflow", "initial_guard", "TEXT NOT NULL DEFAULT ''")
                ensureColumn(statement, "agent_workflow", "completion_guard", "TEXT NOT NULL DEFAULT ''")
                ensureColumn(statement, "agent_workflow", "replay_steps", "TEXT NOT NULL DEFAULT ''")
                statement.execute("""CREATE TABLE IF NOT EXISTS agent_app_knowledge_card (
                    id TEXT PRIMARY KEY, package_name TEXT NOT NULL, title TEXT NOT NULL,
                    guide TEXT NOT NULL, enabled INTEGER NOT NULL, updated_at INTEGER NOT NULL
                )""".trimIndent())
            }
            connection.prepareStatement("UPDATE agent_workflow SET status='DISABLED' WHERE schema_version < ?").use {
                it.setInt(1, WORKFLOW_SCHEMA_VERSION); it.executeUpdate()
            }
        }
    }

    override suspend fun list(includeDrafts: Boolean): List<AgentWorkflow> = withContext(Dispatchers.IO) {
        synchronized(connection) {
            connection.prepareStatement("SELECT * FROM agent_workflow ${if (includeDrafts) "" else "WHERE status='ENABLED'"} ORDER BY updated_at DESC").use { statement ->
                statement.executeQuery().use { rows -> buildList { while (rows.next()) add(rows.toWorkflow()) } }
            }
        }
    }

    override suspend fun save(workflow: AgentWorkflow): AgentWorkflow = withContext(Dispatchers.IO) {
        require(WorkflowSanitizer.valid(workflow)) { "Workflow contains sensitive or unsupported content" }
        require(workflow.status != WorkflowStatus.ENABLED || workflow.canEnable) { "Only valid v2 workflows can be enabled" }
        val value = workflow.copy(updatedAt = System.currentTimeMillis())
        synchronized(connection) {
            connection.prepareStatement("""INSERT INTO agent_workflow(
                id,name,task_pattern,package_name,app_version,status,steps,signature,success_count,
                failure_count,average_duration,created_at,updated_at,schema_version,initial_guard,
                completion_guard,replay_steps
            ) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            ON CONFLICT(id) DO UPDATE SET name=excluded.name,task_pattern=excluded.task_pattern,
                package_name=excluded.package_name,app_version=excluded.app_version,status=excluded.status,
                steps=excluded.steps,signature=excluded.signature,success_count=excluded.success_count,
                failure_count=excluded.failure_count,average_duration=excluded.average_duration,
                updated_at=excluded.updated_at,schema_version=excluded.schema_version,
                initial_guard=excluded.initial_guard,completion_guard=excluded.completion_guard,
                replay_steps=excluded.replay_steps""").use { statement ->
                statement.setString(1, value.id); statement.setString(2, value.name); statement.setString(3, value.taskPattern)
                statement.setString(4, value.packageName); statement.setString(5, value.appVersion); statement.setString(6, value.status.name)
                statement.setString(7, WorkflowCodec.encodeLegacy(value.steps)); statement.setString(8, value.verification?.pageSignature.orEmpty())
                statement.setInt(9, value.statistics.successCount); statement.setInt(10, value.statistics.failureCount)
                statement.setLong(11, value.statistics.averageDurationMs); statement.setLong(12, value.createdAt); statement.setLong(13, value.updatedAt)
                statement.setInt(14, value.schemaVersion); statement.setString(15, WorkflowCodec.encodeGuard(value.initialGuard))
                statement.setString(16, WorkflowCodec.encodeGuard(value.completionGuard)); statement.setString(17, WorkflowCodec.encodeReplay(value.replaySteps))
                statement.executeUpdate()
            }
        }
        value
    }

    override suspend fun delete(id: String): Unit = withContext(Dispatchers.IO) {
        synchronized(connection) { connection.prepareStatement("DELETE FROM agent_workflow WHERE id=?").use { it.setString(1, id); it.executeUpdate() } }; Unit
    }

    override suspend fun findEnabled(task: String, state: DeviceState): AgentWorkflow? = list(false)
        .filter { workflow -> workflow.canEnable && normalizeWorkflowTask(workflow.taskPattern) == normalizeWorkflowTask(task) }
        .filter { workflow -> workflow.packageName == state.packageName }
        .map { it to WorkflowStateMatcher.match(it.initialGuard!!, state) }
        .filter { (_, match) -> match.matches }
        .sortedWith(compareByDescending<Pair<AgentWorkflow, WorkflowStateMatch>> { it.second.score }
            .thenByDescending { it.first.statistics.successRate }
            .thenByDescending { it.first.updatedAt })
        .firstOrNull()?.first

    override suspend fun recordResult(id: String, success: Boolean, durationMs: Long) {
        val item = list().firstOrNull { it.id == id } ?: return
        val count = item.statistics.successCount + item.statistics.failureCount
        save(item.copy(statistics = item.statistics.copy(
            successCount = item.statistics.successCount + if (success) 1 else 0,
            failureCount = item.statistics.failureCount + if (success) 0 else 1,
            averageDurationMs = (item.statistics.averageDurationMs * count + durationMs) / (count + 1)
        )))
    }

    override suspend fun listKnowledgeCards(packageName: String?, enabledOnly: Boolean): List<AgentAppKnowledgeCard> = withContext(Dispatchers.IO) {
        val where = buildList { if (packageName != null) add("package_name=?"); if (enabledOnly) add("enabled=1") }.joinToString(" AND ")
        synchronized(connection) {
            connection.prepareStatement("SELECT * FROM agent_app_knowledge_card ${if (where.isBlank()) "" else "WHERE $where"} ORDER BY updated_at DESC").use { statement ->
                var index = 1; if (packageName != null) statement.setString(index++, packageName)
                statement.executeQuery().use { rows -> buildList { while (rows.next()) add(AgentAppKnowledgeCard(rows.getString("id"), rows.getString("package_name"), rows.getString("title"), rows.getString("guide"), rows.getInt("enabled") != 0, rows.getLong("updated_at"))) } }
            }
        }
    }

    override suspend fun saveKnowledgeCard(card: AgentAppKnowledgeCard): AgentAppKnowledgeCard = withContext(Dispatchers.IO) {
        require(WorkflowSanitizer.validKnowledge(card)) { "Knowledge card contains invalid or sensitive content" }
        val value = card.copy(updatedAt = System.currentTimeMillis())
        synchronized(connection) { connection.prepareStatement("""INSERT INTO agent_app_knowledge_card(id,package_name,title,guide,enabled,updated_at)
            VALUES(?,?,?,?,?,?) ON CONFLICT(id) DO UPDATE SET package_name=excluded.package_name,title=excluded.title,
            guide=excluded.guide,enabled=excluded.enabled,updated_at=excluded.updated_at""").use { statement ->
            statement.setString(1, value.id); statement.setString(2, value.packageName); statement.setString(3, value.title)
            statement.setString(4, value.guide); statement.setInt(5, if (value.enabled) 1 else 0); statement.setLong(6, value.updatedAt); statement.executeUpdate()
        } }
        value
    }

    override suspend fun deleteKnowledgeCard(id: String): Unit = withContext(Dispatchers.IO) {
        synchronized(connection) { connection.prepareStatement("DELETE FROM agent_app_knowledge_card WHERE id=?").use { it.setString(1, id); it.executeUpdate() } }; Unit
    }

    override fun close() = connection.close()

    private fun ensureColumn(statement: java.sql.Statement, table: String, column: String, definition: String) {
        val columns = statement.executeQuery("PRAGMA table_info($table)").use { rows -> buildSet { while (rows.next()) add(rows.getString("name")) } }
        if (column !in columns) statement.execute("ALTER TABLE $table ADD COLUMN $column $definition")
    }

    private fun java.sql.ResultSet.toWorkflow(): AgentWorkflow {
        val schema = getInt("schema_version").takeIf { it > 0 } ?: 1
        val replay = WorkflowCodec.decodeReplay(getString("replay_steps"))
        return AgentWorkflow(
            id = getString("id"), name = getString("name"), taskPattern = getString("task_pattern"), packageName = getString("package_name"),
            appVersion = getString("app_version"), status = WorkflowStatus.valueOf(getString("status")),
            steps = WorkflowCodec.decodeLegacy(getString("steps")), verification = getString("signature").takeIf { it.isNotBlank() }?.let(::WorkflowVerification),
            statistics = WorkflowStatistics(getInt("success_count"), getInt("failure_count"), getLong("average_duration")),
            schemaVersion = schema, initialGuard = WorkflowCodec.decodeGuard(getString("initial_guard")),
            completionGuard = WorkflowCodec.decodeGuard(getString("completion_guard")), replaySteps = replay,
            createdAt = getLong("created_at"), updatedAt = getLong("updated_at")
        )
    }
}

object WorkflowStateMatcher {
    fun guard(state: DeviceState): WorkflowStateGuard = WorkflowStateGuard(
        packageName = state.packageName.orEmpty(), activityName = state.activityName.orEmpty(),
        semanticKeyHashes = state.nodes.mapNotNull(::semanticKey).map(::hash).toSet()
    )

    fun match(guard: WorkflowStateGuard, state: DeviceState): WorkflowStateMatch {
        if (guard.packageName != state.packageName) return WorkflowStateMatch(false, 0.0, "Foreground package differs")
        val actual = state.nodes.mapNotNull(::semanticKey).map(::hash).toSet()
        val union = guard.semanticKeyHashes union actual
        val similarity = if (union.isEmpty()) 1.0 else guard.semanticKeyHashes.intersect(actual).size.toDouble() / union.size
        val activityScore = if (guard.activityName.isBlank() || guard.activityName == state.activityName) 1.0 else 0.0
        val score = similarity * 0.85 + activityScore * 0.15
        return WorkflowStateMatch(score >= guard.threshold, score, if (score >= guard.threshold) "" else "Page similarity %.2f is below %.2f".format(score, guard.threshold))
    }

    private fun semanticKey(node: UiNodeSnapshot): String? = listOf(node.resourceId, node.role, node.className, node.text.take(80), node.contentDescription.take(80))
        .filter { it.isNotBlank() }.takeIf { it.isNotEmpty() }?.joinToString("|")
    private fun hash(value: String): String = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }.take(24)
}

object WorkflowDraftFactory {
    fun fromRecordedTask(task: String, initialState: DeviceState?, completed: List<WorkflowRecordedStep>): WorkflowDraft? {
        if (initialState == null || completed.isEmpty() || completed.any { !WorkflowSanitizer.safe(it.action) }) return null
        val replay = completed.mapNotNull { it.toReplayStep() }
        if (replay.size != completed.size) return null
        return AgentWorkflow(
            name = task.trim().take(48), taskPattern = task.trim(), packageName = initialState.packageName.orEmpty(),
            status = WorkflowStatus.DRAFT, schemaVersion = WORKFLOW_SCHEMA_VERSION,
            initialGuard = WorkflowStateMatcher.guard(initialState), completionGuard = replay.last().after, replaySteps = replay
        )
    }

    /** Compatibility overload: old callers generate no v2 workflow because guards are unavailable. */
    fun fromCompletedTask(task: String, state: DeviceState?, steps: List<AgentStep>): WorkflowDraft? = null
}

data class WorkflowRecordedStep(val action: AgentAction, val before: DeviceState, val after: DeviceState) {
    fun toReplayStep(): WorkflowReplayStep? = WorkflowSanitizer.toReplayAction(action, before.nodes)?.let {
        WorkflowReplayStep(it, WorkflowStateMatcher.guard(before), WorkflowStateMatcher.guard(after))
    }
}

object WorkflowSanitizer {
    private val sensitive = Regex("(?i)(api[_ -]?key|token|password|密码|验证码|authorization|bearer)")
    fun safe(action: AgentAction): Boolean = toReplayAction(action, emptyList()) != null || action is AgentAction.TapElement
    fun valid(workflow: AgentWorkflow): Boolean = workflow.name.isNotBlank() && workflow.taskPattern.isNotBlank() &&
        (workflow.schemaVersion < WORKFLOW_SCHEMA_VERSION || workflow.replaySteps.all { replaySafe(it.action) })
    fun validKnowledge(card: AgentAppKnowledgeCard): Boolean = KNOWLEDGE_PACKAGE_PATTERN.matches(card.packageName) && card.title.isNotBlank() &&
        card.guide.isNotBlank() && card.guide.length <= MAX_KNOWLEDGE_LENGTH && !sensitive.containsMatchIn(card.guide)

    fun toReplayAction(action: AgentAction, nodes: List<UiNodeSnapshot>): WorkflowReplayAction? = when (action) {
        is AgentAction.KeyEvent -> WorkflowReplayAction.KeyEvent(action.key)
        is AgentAction.LaunchPackage -> WorkflowReplayAction.LaunchPackage(action.packageName)
        is AgentAction.Wait -> WorkflowReplayAction.Wait(action.durationMs)
        is AgentAction.TapElement -> nodes.firstOrNull { it.elementId == action.elementId }?.let(SelectorResolver::from)?.let(WorkflowReplayAction::TapSelector)
        else -> null
    }

    private fun replaySafe(action: WorkflowReplayAction): Boolean = when (action) {
        is WorkflowReplayAction.KeyEvent, is WorkflowReplayAction.LaunchPackage, is WorkflowReplayAction.Wait -> true
        is WorkflowReplayAction.TapSelector -> action.selector.resourceId?.isNotBlank() == true ||
            action.selector.textAny.all { it.length <= 80 && !sensitive.containsMatchIn(it) } &&
            action.selector.contentDescriptionAny.all { it.length <= 80 && !sensitive.containsMatchIn(it) }
    }
}

private val KNOWLEDGE_PACKAGE_PATTERN = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+$")

private fun normalizeWorkflowTask(task: String): String = task.lowercase().replace(Regex("[\\s\\p{Punct}，。！？]+"), "")

private object WorkflowCodec {
    fun encodeLegacy(steps: List<WorkflowStep>): String = steps.joinToString("\n") { step -> when (val action = step.action) {
        is AgentAction.KeyEvent -> "key:${action.key.name}"; is AgentAction.FindApp -> "find:${action.query}"; is AgentAction.LaunchPackage -> "launch:${action.packageName}"; is AgentAction.Wait -> "wait:${action.durationMs}"; else -> ""
    } }.trim()
    fun decodeLegacy(value: String): List<WorkflowStep> = value.lines().mapNotNull { line -> when {
        line.startsWith("key:") -> runCatching { WorkflowStep(AgentAction.KeyEvent(AgentKey.valueOf(line.removePrefix("key:")))) }.getOrNull()
        line.startsWith("find:") -> WorkflowStep(AgentAction.FindApp(line.removePrefix("find:")))
        line.startsWith("launch:") -> WorkflowStep(AgentAction.LaunchPackage(line.removePrefix("launch:")))
        line.startsWith("wait:") -> line.removePrefix("wait:").toIntOrNull()?.let { WorkflowStep(AgentAction.Wait(it)) }
        else -> null
    } }
    fun encodeGuard(guard: WorkflowStateGuard?): String = guard?.let { listOf(it.packageName, it.activityName, it.threshold, it.semanticKeyHashes.joinToString(",")).joinToString("\t") }.orEmpty()
    fun decodeGuard(value: String): WorkflowStateGuard? = value.split('\t').takeIf { it.size == 4 }?.let { WorkflowStateGuard(it[0], it[1], it[3].split(',').filter(String::isNotBlank).toSet(), it[2].toDoubleOrNull() ?: DEFAULT_GUARD_THRESHOLD) }
    fun encodeReplay(steps: List<WorkflowReplayStep>): String = steps.joinToString("\n") { step -> listOf(encodeAction(step.action), encodeGuard(step.before), encodeGuard(step.after)).joinToString("\u001f") }
    fun decodeReplay(value: String): List<WorkflowReplayStep> = value.lines().mapNotNull { line -> line.split('\u001f').takeIf { it.size == 3 }?.let { parts -> decodeAction(parts[0])?.let { action -> decodeGuard(parts[1])?.let { before -> decodeGuard(parts[2])?.let { after -> WorkflowReplayStep(action, before, after) } } } } }
    private fun encodeAction(action: WorkflowReplayAction): String = when (action) {
        is WorkflowReplayAction.KeyEvent -> "key:${action.key.name}"; is WorkflowReplayAction.LaunchPackage -> "launch:${action.packageName}"; is WorkflowReplayAction.Wait -> "wait:${action.durationMs}"
        is WorkflowReplayAction.TapSelector -> "tap:${action.selector.resourceId.orEmpty()}|${action.selector.role.orEmpty()}|${action.selector.textAny.joinToString(",")}|${action.selector.contentDescriptionAny.joinToString(",")}" }
    private fun decodeAction(value: String): WorkflowReplayAction? = when {
        value.startsWith("key:") -> runCatching { WorkflowReplayAction.KeyEvent(AgentKey.valueOf(value.removePrefix("key:"))) }.getOrNull()
        value.startsWith("launch:") -> WorkflowReplayAction.LaunchPackage(value.removePrefix("launch:"))
        value.startsWith("wait:") -> value.removePrefix("wait:").toIntOrNull()?.let(WorkflowReplayAction::Wait)
        value.startsWith("tap:") -> value.removePrefix("tap:").split('|').takeIf { it.size == 4 }?.let { WorkflowReplayAction.TapSelector(Selector(it[0].ifBlank { null }, it[2].split(',').filter(String::isNotBlank), it[3].split(',').filter(String::isNotBlank), it[1].ifBlank { null })) }
        else -> null
    }
}

object AgentWorkflowRuntime { val store: AgentWorkflowStore by lazy { SqliteAgentWorkflowStore() } }
