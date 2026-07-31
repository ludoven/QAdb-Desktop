package com.ludoven.adbtool.agent

import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.time.Instant
import java.util.UUID
import java.util.prefs.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

enum class MemoryKind {
    USER_PREFERENCE,
    APP_ALIAS,
    DEVICE_FACT,
    VERIFIED_PROCEDURE,
    TASK_SUMMARY
}

enum class MemoryScopeType {
    GLOBAL,
    DEVICE,
    APP
}

enum class MemoryProvenance {
    USER_EXPLICIT,
    USER_EDITED,
    VERIFIED_ACTION,
    SANITIZED_TASK_SUMMARY
}

data class MemoryScope(
    val type: MemoryScopeType,
    val key: String = ""
) {
    companion object {
        val Global = MemoryScope(MemoryScopeType.GLOBAL)

        fun device(deviceId: String): MemoryScope =
            MemoryScope(MemoryScopeType.DEVICE, sha256(deviceId.trim()))

        fun app(packageName: String): MemoryScope =
            MemoryScope(MemoryScopeType.APP, packageName.trim())
    }
}

data class AgentMemory(
    val id: String = UUID.randomUUID().toString(),
    val kind: MemoryKind,
    val scope: MemoryScope = MemoryScope.Global,
    val content: String,
    val keywords: String = "",
    val importance: Double = 0.5,
    val provenance: MemoryProvenance,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    val lastUsedAt: Long? = null,
    val useCount: Int = 0,
    val archived: Boolean = false
)

data class AgentMemoryStats(
    val activeCount: Int = 0,
    val archivedCount: Int = 0,
    val taskSummaryCount: Int = 0
)

data class AgentMemoryQuery(
    val text: String,
    val deviceId: String? = null,
    val packageName: String? = null,
    val limit: Int = DEFAULT_MEMORY_RESULT_LIMIT,
    val maxChars: Int = DEFAULT_MEMORY_CONTEXT_CHARS
)

interface AgentMemoryStore : AutoCloseable {
    suspend fun search(query: AgentMemoryQuery): List<AgentMemory>
    suspend fun upsert(memory: AgentMemory): AgentMemory
    suspend fun update(memory: AgentMemory): AgentMemory
    suspend fun delete(id: String)
    suspend fun clear()
    suspend fun listActive(limit: Int = 200): List<AgentMemory>
    suspend fun stats(): AgentMemoryStats
    suspend fun prune()
    override fun close() = Unit
}

class SqliteAgentMemoryStore(
    databaseFile: File = AgentDataPaths.memoryDatabase()
) : AgentMemoryStore {
    private val connection: Connection

    init {
        databaseFile.parentFile?.mkdirs()
        restrictToOwner(databaseFile.parentFile)
        Class.forName("org.sqlite.JDBC")
        connection = DriverManager.getConnection("jdbc:sqlite:${databaseFile.absolutePath}")
        connection.createStatement().use { statement ->
            statement.execute("PRAGMA journal_mode=WAL")
            statement.execute("PRAGMA foreign_keys=ON")
            statement.execute("PRAGMA busy_timeout=5000")
        }
        migrate()
        restrictToOwner(databaseFile)
    }

    override suspend fun search(query: AgentMemoryQuery): List<AgentMemory> = withContext(Dispatchers.IO) {
        synchronized(connection) {
            val normalized = query.text.trim().take(MAX_MEMORY_QUERY_CHARS)
            if (normalized.isEmpty()) return@synchronized emptyList()
            val deviceScopeKey = query.deviceId?.takeIf { it.isNotBlank() }?.let(::sha256)
            val packageScopeKey = query.packageName?.trim()?.takeIf { it.isNotEmpty() }
            val candidates = if (normalized.codePointCount(0, normalized.length) < 3) {
                searchLike(normalized, deviceScopeKey, packageScopeKey)
            } else {
                searchFts(normalized, deviceScopeKey, packageScopeKey)
                    .ifEmpty { searchLike(normalized, deviceScopeKey, packageScopeKey) }
            }
            val selected = candidates
                .distinctBy { it.id }
                .sortedWith(
                    compareByDescending<AgentMemory> { scopeScore(it.scope, deviceScopeKey, packageScopeKey) }
                        .thenByDescending { it.importance }
                        .thenByDescending { it.lastUsedAt ?: it.updatedAt }
                )
                .take(query.limit.coerceIn(1, 20))
                .takeWithinChars(query.maxChars.coerceIn(200, MAX_MEMORY_CONTEXT_CHARS))
            markUsed(selected.map { it.id })
            selected
        }
    }

    override suspend fun upsert(memory: AgentMemory): AgentMemory = withContext(Dispatchers.IO) {
        val clean = validateAndSanitizeMemory(memory)
        synchronized(connection) {
            transaction {
                val existing = findDuplicate(clean)
                val value = if (existing != null) {
                    clean.copy(
                        id = existing.id,
                        createdAt = existing.createdAt,
                        updatedAt = System.currentTimeMillis(),
                        useCount = existing.useCount,
                        lastUsedAt = existing.lastUsedAt
                    )
                } else {
                    clean.copy(updatedAt = System.currentTimeMillis())
                }
                write(value)
                value
            }
        }.also { prune() }
    }

    override suspend fun update(memory: AgentMemory): AgentMemory = withContext(Dispatchers.IO) {
        val clean = validateAndSanitizeMemory(
            memory.copy(provenance = MemoryProvenance.USER_EDITED, updatedAt = System.currentTimeMillis())
        )
        synchronized(connection) {
            transaction {
                require(findById(clean.id) != null) { "Memory entry does not exist" }
                write(clean)
                clean
            }
        }
    }

    override suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        synchronized(connection) {
            transaction {
                connection.prepareStatement("DELETE FROM agent_memory_fts WHERE id = ?").use {
                    it.setString(1, id)
                    it.executeUpdate()
                }
                connection.prepareStatement("DELETE FROM agent_memory WHERE id = ?").use {
                    it.setString(1, id)
                    it.executeUpdate()
                }
            }
        }
        Unit
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        synchronized(connection) {
            transaction {
                connection.createStatement().use {
                    it.executeUpdate("DELETE FROM agent_memory_fts")
                    it.executeUpdate("DELETE FROM agent_memory")
                }
            }
        }
        Unit
    }

    override suspend fun listActive(limit: Int): List<AgentMemory> = withContext(Dispatchers.IO) {
        synchronized(connection) {
            connection.prepareStatement(
                """
                SELECT * FROM agent_memory
                WHERE archived = 0
                ORDER BY importance DESC, updated_at DESC
                LIMIT ?
                """.trimIndent()
            ).use { statement ->
                statement.setInt(1, limit.coerceIn(1, MAX_AUTOMATIC_MEMORIES))
                statement.executeQuery().use { result -> result.readMemories() }
            }
        }
    }

    override suspend fun stats(): AgentMemoryStats = withContext(Dispatchers.IO) {
        synchronized(connection) {
            connection.createStatement().use { statement ->
                statement.executeQuery(
                    """
                    SELECT
                      SUM(CASE WHEN archived = 0 THEN 1 ELSE 0 END) AS active_count,
                      SUM(CASE WHEN archived = 1 THEN 1 ELSE 0 END) AS archived_count,
                      SUM(CASE WHEN kind = 'TASK_SUMMARY' THEN 1 ELSE 0 END) AS summary_count
                    FROM agent_memory
                    """.trimIndent()
                ).use { result ->
                    if (!result.next()) AgentMemoryStats() else AgentMemoryStats(
                        activeCount = result.getInt("active_count"),
                        archivedCount = result.getInt("archived_count"),
                        taskSummaryCount = result.getInt("summary_count")
                    )
                }
            }
        }
    }

    override suspend fun prune() = withContext(Dispatchers.IO) {
        synchronized(connection) {
            transaction {
                val summaryCutoff = Instant.now().minusSeconds(TASK_SUMMARY_RETENTION_SECONDS).toEpochMilli()
                connection.prepareStatement(
                    "UPDATE agent_memory SET archived = 1 WHERE kind = 'TASK_SUMMARY' AND created_at < ?"
                ).use {
                    it.setLong(1, summaryCutoff)
                    it.executeUpdate()
                }
                connection.prepareStatement(
                    """
                    UPDATE agent_memory SET archived = 1
                    WHERE id IN (
                      SELECT id FROM agent_memory
                      WHERE archived = 0 AND provenance != 'USER_EXPLICIT' AND provenance != 'USER_EDITED'
                      ORDER BY importance DESC, COALESCE(last_used_at, updated_at) DESC
                      LIMIT -1 OFFSET ?
                    )
                    """.trimIndent()
                ).use {
                    it.setInt(1, MAX_AUTOMATIC_MEMORIES)
                    it.executeUpdate()
                }
                rebuildFts()
            }
        }
    }

    override fun close() {
        synchronized(connection) {
            runCatching { connection.close() }
        }
    }

    private fun migrate() = synchronized(connection) {
        transaction {
            connection.createStatement().use { statement ->
                statement.execute(
                    """
                    CREATE TABLE IF NOT EXISTS agent_memory (
                      id TEXT PRIMARY KEY,
                      kind TEXT NOT NULL,
                      scope_type TEXT NOT NULL,
                      scope_key TEXT NOT NULL,
                      content TEXT NOT NULL,
                      keywords TEXT NOT NULL,
                      importance REAL NOT NULL,
                      provenance TEXT NOT NULL,
                      created_at INTEGER NOT NULL,
                      updated_at INTEGER NOT NULL,
                      last_used_at INTEGER,
                      use_count INTEGER NOT NULL DEFAULT 0,
                      archived INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                statement.execute(
                    """
                    CREATE VIRTUAL TABLE IF NOT EXISTS agent_memory_fts
                    USING fts5(id UNINDEXED, content, keywords, tokenize='trigram')
                    """.trimIndent()
                )
                statement.execute("CREATE INDEX IF NOT EXISTS memory_scope_idx ON agent_memory(scope_type, scope_key)")
                statement.execute("CREATE INDEX IF NOT EXISTS memory_updated_idx ON agent_memory(updated_at DESC)")
                statement.execute("PRAGMA user_version=$SCHEMA_VERSION")
            }
            rebuildFts()
        }
    }

    private fun searchFts(query: String, deviceScopeKey: String?, packageScopeKey: String?): List<AgentMemory> {
        val sql =
            """
            SELECT m.* FROM agent_memory_fts f
            JOIN agent_memory m ON m.id = f.id
            WHERE agent_memory_fts MATCH ?
              AND m.archived = 0
              AND (
                m.scope_type = 'GLOBAL'
                OR (m.scope_type = 'DEVICE' AND m.scope_key = ?)
                OR (m.scope_type = 'APP' AND m.scope_key = ?)
              )
            ORDER BY bm25(agent_memory_fts), m.importance DESC
            LIMIT 30
            """.trimIndent()
        return connection.prepareStatement(sql).use { statement ->
            statement.setString(1, "\"${query.replace("\"", "\"\"")}\"")
            statement.setString(2, deviceScopeKey.orEmpty())
            statement.setString(3, packageScopeKey.orEmpty())
            statement.executeQuery().use { it.readMemories() }
        }
    }

    private fun searchLike(query: String, deviceScopeKey: String?, packageScopeKey: String?): List<AgentMemory> {
        val sql =
            """
            SELECT * FROM agent_memory
            WHERE archived = 0
              AND (content LIKE ? ESCAPE '\' OR keywords LIKE ? ESCAPE '\')
              AND (
                scope_type = 'GLOBAL'
                OR (scope_type = 'DEVICE' AND scope_key = ?)
                OR (scope_type = 'APP' AND scope_key = ?)
              )
            ORDER BY importance DESC, updated_at DESC
            LIMIT 30
            """.trimIndent()
        val pattern = "%${escapeLike(query)}%"
        return connection.prepareStatement(sql).use { statement ->
            statement.setString(1, pattern)
            statement.setString(2, pattern)
            statement.setString(3, deviceScopeKey.orEmpty())
            statement.setString(4, packageScopeKey.orEmpty())
            statement.executeQuery().use { it.readMemories() }
        }
    }

    private fun findDuplicate(memory: AgentMemory): AgentMemory? =
        connection.prepareStatement(
            """
            SELECT * FROM agent_memory
            WHERE kind = ? AND scope_type = ? AND scope_key = ?
              AND (lower(content) = lower(?) OR lower(keywords) = lower(?))
            LIMIT 1
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, memory.kind.name)
            statement.setString(2, memory.scope.type.name)
            statement.setString(3, memory.scope.key)
            statement.setString(4, memory.content)
            statement.setString(5, memory.keywords)
            statement.executeQuery().use { result ->
                if (result.next()) result.readMemory() else null
            }
        }

    private fun findById(id: String): AgentMemory? =
        connection.prepareStatement("SELECT * FROM agent_memory WHERE id = ? LIMIT 1").use { statement ->
            statement.setString(1, id)
            statement.executeQuery().use { result ->
                if (result.next()) result.readMemory() else null
            }
        }

    private fun write(memory: AgentMemory) {
        connection.prepareStatement(
            """
            INSERT INTO agent_memory(
              id, kind, scope_type, scope_key, content, keywords, importance, provenance,
              created_at, updated_at, last_used_at, use_count, archived
            ) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
              kind=excluded.kind, scope_type=excluded.scope_type, scope_key=excluded.scope_key,
              content=excluded.content, keywords=excluded.keywords, importance=excluded.importance,
              provenance=excluded.provenance, updated_at=excluded.updated_at,
              last_used_at=excluded.last_used_at, use_count=excluded.use_count, archived=excluded.archived
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, memory.id)
            statement.setString(2, memory.kind.name)
            statement.setString(3, memory.scope.type.name)
            statement.setString(4, memory.scope.key)
            statement.setString(5, memory.content)
            statement.setString(6, memory.keywords)
            statement.setDouble(7, memory.importance)
            statement.setString(8, memory.provenance.name)
            statement.setLong(9, memory.createdAt)
            statement.setLong(10, memory.updatedAt)
            memory.lastUsedAt?.let { statement.setLong(11, it) } ?: statement.setNull(11, java.sql.Types.BIGINT)
            statement.setInt(12, memory.useCount)
            statement.setInt(13, if (memory.archived) 1 else 0)
            statement.executeUpdate()
        }
        connection.prepareStatement("DELETE FROM agent_memory_fts WHERE id = ?").use {
            it.setString(1, memory.id)
            it.executeUpdate()
        }
        if (!memory.archived) {
            connection.prepareStatement(
                "INSERT INTO agent_memory_fts(id, content, keywords) VALUES(?, ?, ?)"
            ).use {
                it.setString(1, memory.id)
                it.setString(2, memory.content)
                it.setString(3, memory.keywords)
                it.executeUpdate()
            }
        }
    }

    private fun rebuildFts() {
        connection.createStatement().use { statement ->
            statement.executeUpdate("DELETE FROM agent_memory_fts")
            statement.executeUpdate(
                """
                INSERT INTO agent_memory_fts(id, content, keywords)
                SELECT id, content, keywords FROM agent_memory WHERE archived = 0
                """.trimIndent()
            )
        }
    }

    private fun markUsed(ids: List<String>) {
        if (ids.isEmpty()) return
        connection.prepareStatement(
            "UPDATE agent_memory SET last_used_at = ?, use_count = use_count + 1 WHERE id = ?"
        ).use { statement ->
            val now = System.currentTimeMillis()
            ids.forEach { id ->
                statement.setLong(1, now)
                statement.setString(2, id)
                statement.addBatch()
            }
            statement.executeBatch()
        }
    }

    private fun <T> transaction(block: () -> T): T {
        val oldAutoCommit = connection.autoCommit
        connection.autoCommit = false
        return try {
            block().also { connection.commit() }
        } catch (error: Throwable) {
            connection.rollback()
            throw error
        } finally {
            connection.autoCommit = oldAutoCommit
        }
    }

    private fun ResultSet.readMemories(): List<AgentMemory> = buildList {
        while (next()) add(readMemory())
    }

    private fun ResultSet.readMemory(): AgentMemory = AgentMemory(
        id = getString("id"),
        kind = MemoryKind.valueOf(getString("kind")),
        scope = MemoryScope(
            type = MemoryScopeType.valueOf(getString("scope_type")),
            key = getString("scope_key")
        ),
        content = getString("content"),
        keywords = getString("keywords"),
        importance = getDouble("importance"),
        provenance = MemoryProvenance.valueOf(getString("provenance")),
        createdAt = getLong("created_at"),
        updatedAt = getLong("updated_at"),
        lastUsedAt = getLong("last_used_at").takeIf { !wasNull() },
        useCount = getInt("use_count"),
        archived = getInt("archived") != 0
    )
}

class AgentMemoryPreferences(
    private val preferences: Preferences = Preferences.userNodeForPackage(AgentMemoryPreferences::class.java)
) {
    private val _enabled = MutableStateFlow(
        preferences.getBoolean(KEY_ENABLED, false) &&
            preferences.getInt(KEY_CONSENT_VERSION, 0) >= CURRENT_CONSENT_VERSION
    )
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    val needsConsent: Boolean
        get() = preferences.getInt(KEY_CONSENT_VERSION, 0) < CURRENT_CONSENT_VERSION

    fun acceptConsent() {
        preferences.putInt(KEY_CONSENT_VERSION, CURRENT_CONSENT_VERSION)
        setEnabled(true)
    }

    fun declineConsent() {
        preferences.putInt(KEY_CONSENT_VERSION, CURRENT_CONSENT_VERSION)
        preferences.putBoolean(KEY_ENABLED, false)
        preferences.flush()
        _enabled.value = false
    }

    fun setEnabled(enabled: Boolean) {
        preferences.putBoolean(KEY_ENABLED, enabled)
        preferences.flush()
        _enabled.value = enabled && !needsConsent
    }

    companion object {
        private const val KEY_ENABLED = "agent.memory.enabled"
        private const val KEY_CONSENT_VERSION = "agent.memory.consent.version"
        private const val CURRENT_CONSENT_VERSION = 1
    }
}

object AgentMemoryRuntime {
    val preferences: AgentMemoryPreferences by lazy { AgentMemoryPreferences() }
    val store: AgentMemoryStore by lazy { SqliteAgentMemoryStore() }
}

object AgentDataPaths {
    fun memoryDatabase(): File = File(agentDataDirectory(), "agent-memory.db")

    fun agentDataDirectory(): File {
        val home = System.getProperty("user.home")
        val os = System.getProperty("os.name").lowercase()
        val base = when {
            os.contains("mac") -> File(home, "Library/Application Support/QADB")
            os.contains("windows") -> File(
                System.getenv("APPDATA")?.takeIf { it.isNotBlank() } ?: home,
                "QADB"
            )
            else -> File(
                System.getenv("XDG_DATA_HOME")?.takeIf { it.isNotBlank() }
                    ?: File(home, ".local/share").absolutePath,
                "qadb"
            )
        }
        return File(base, "agent").also {
            it.mkdirs()
            restrictToOwner(it)
        }
    }
}

fun validateAndSanitizeMemory(memory: AgentMemory): AgentMemory {
    val content = memory.content.trim().replace(Regex("\\s+"), " ").take(MAX_MEMORY_CONTENT_CHARS)
    val keywords = memory.keywords.trim().replace(Regex("\\s+"), " ").take(MAX_MEMORY_KEYWORDS_CHARS)
    require(content.length >= 3) { "Memory content is too short" }
    require(memory.importance in 0.0..1.0) { "Memory importance is invalid" }
    require(!containsSensitiveMemoryContent(content) && !containsSensitiveMemoryContent(keywords)) {
        "Sensitive content cannot be saved to Agent memory"
    }
    require(!containsInstructionInjection(content)) { "Instruction-like device content cannot be saved to Agent memory" }
    return memory.copy(content = content, keywords = keywords)
}

fun memoryScopeFor(candidate: AgentMemoryCandidate, deviceId: String?, packageName: String?): MemoryScope =
    when (candidate.scope) {
        MemoryScopeType.GLOBAL -> MemoryScope.Global
        MemoryScopeType.DEVICE -> deviceId?.let(MemoryScope::device) ?: MemoryScope.Global
        MemoryScopeType.APP -> packageName?.let(MemoryScope::app) ?: MemoryScope.Global
    }

fun sanitizeTaskSummary(summary: String): String {
    var value = summary
        .replace(Regex("(?i)(api[_ -]?key|authorization|bearer|password|密码|验证码|otp)\\s*[:=]?\\s*\\S+"), "[已脱敏]")
        .replace(Regex("[\\r\\n\\t]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
    value = value.replace(Regex("([\"“']).{2,500}?([\"”'])"), "[内容已省略]")
    return value.take(MAX_TASK_SUMMARY_CHARS)
}

private fun containsSensitiveMemoryContent(value: String): Boolean {
    val normalized = value.lowercase()
    return listOf(
        "authorization:",
        "bearer ",
        "api_key",
        "apikey",
        "password=",
        "密码：",
        "验证码",
        "one-time password",
        "private key"
    ).any(normalized::contains) ||
        Regex("sk-[A-Za-z0-9_-]{12,}").containsMatchIn(value)
}

private fun containsInstructionInjection(value: String): Boolean {
    val normalized = value.lowercase()
    return listOf(
        "ignore previous",
        "ignore all previous",
        "system prompt",
        "developer message",
        "调用任意",
        "忽略之前",
        "忽略所有",
        "覆盖系统"
    ).any(normalized::contains)
}

private fun List<AgentMemory>.takeWithinChars(maxChars: Int): List<AgentMemory> {
    var used = 0
    return takeWhile {
        val size = it.content.length + it.keywords.length + 8
        (used + size <= maxChars).also { fits -> if (fits) used += size }
    }
}

private fun scopeScore(scope: MemoryScope, deviceScopeKey: String?, packageScopeKey: String?): Int =
    when {
        scope.type == MemoryScopeType.APP && scope.key == packageScopeKey -> 3
        scope.type == MemoryScopeType.DEVICE && scope.key == deviceScopeKey -> 2
        scope.type == MemoryScopeType.GLOBAL -> 1
        else -> 0
    }

private fun escapeLike(value: String): String =
    value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

private fun sha256(value: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

private fun restrictToOwner(file: File?) {
    if (file == null || !file.exists()) return
    runCatching {
        file.setReadable(false, false)
        file.setWritable(false, false)
        file.setExecutable(false, false)
        file.setReadable(true, true)
        file.setWritable(true, true)
        if (file.isDirectory) file.setExecutable(true, true)
    }
}

const val DEFAULT_MEMORY_RESULT_LIMIT = 4
const val DEFAULT_MEMORY_CONTEXT_CHARS = 1_600
private const val MAX_MEMORY_CONTEXT_CHARS = 4_000
private const val MAX_MEMORY_QUERY_CHARS = 300
private const val MAX_MEMORY_CONTENT_CHARS = 600
private const val MAX_MEMORY_KEYWORDS_CHARS = 200
private const val MAX_TASK_SUMMARY_CHARS = 500
private const val MAX_AUTOMATIC_MEMORIES = 200
private const val TASK_SUMMARY_RETENTION_SECONDS = 90L * 24L * 60L * 60L
private const val SCHEMA_VERSION = 1
