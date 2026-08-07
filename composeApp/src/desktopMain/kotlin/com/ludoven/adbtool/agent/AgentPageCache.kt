package com.ludoven.adbtool.agent

import java.sql.Connection
import java.sql.DriverManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CachedSelector(
    val pageSignature: String,
    val actionName: String,
    val selector: Selector,
    val appVersion: String = "",
    val successCount: Int = 0,
    val failureCount: Int = 0
) {
    val successRate: Double get() = successCount.toDouble() / (successCount + failureCount).coerceAtLeast(1)
    fun reusable(): Boolean = successCount >= 3 && successRate >= 0.95
}

/** Shares the local Agent SQLite database, but keeps reusable UI knowledge separate from user memory. */
class AgentPageCacheStore : AutoCloseable {
    private val connection: Connection

    init {
        Class.forName("org.sqlite.JDBC")
        connection = DriverManager.getConnection("jdbc:sqlite:${AgentDataPaths.memoryDatabase().absolutePath}")
        connection.createStatement().use { statement ->
            statement.execute("PRAGMA busy_timeout=5000")
            statement.execute(
                """CREATE TABLE IF NOT EXISTS agent_selector_cache (
                    page_signature TEXT NOT NULL, action_name TEXT NOT NULL, app_version TEXT NOT NULL DEFAULT '',
                    resource_id TEXT, text_value TEXT, content_desc TEXT, role TEXT,
                    success_count INTEGER NOT NULL DEFAULT 0, failure_count INTEGER NOT NULL DEFAULT 0,
                    updated_at INTEGER NOT NULL, PRIMARY KEY(page_signature, action_name, app_version)
                )"""
            )
        }
    }

    suspend fun find(page: PageSignature, actionName: String, appVersion: String = ""): CachedSelector? = withContext(Dispatchers.IO) {
        synchronized(connection) {
            connection.prepareStatement(
                "SELECT * FROM agent_selector_cache WHERE page_signature=? AND action_name=? AND app_version=?"
            ).use { statement ->
                statement.setString(1, page.value); statement.setString(2, actionName); statement.setString(3, appVersion)
                statement.executeQuery().use { result -> if (!result.next()) null else CachedSelector(
                    page.value, actionName,
                    Selector(result.getString("resource_id"), result.getString("text_value")?.let(::listOf).orEmpty(), result.getString("content_desc")?.let(::listOf).orEmpty(), result.getString("role")),
                    appVersion, result.getInt("success_count"), result.getInt("failure_count")
                ) }
            }
        }
    }

    suspend fun record(page: PageSignature, actionName: String, selector: Selector, success: Boolean, appVersion: String = "") = withContext(Dispatchers.IO) {
        synchronized(connection) {
            connection.prepareStatement(
                """INSERT INTO agent_selector_cache(page_signature,action_name,app_version,resource_id,text_value,content_desc,role,success_count,failure_count,updated_at)
                   VALUES(?,?,?,?,?,?,?,?,?,?)
                   ON CONFLICT(page_signature,action_name,app_version) DO UPDATE SET
                   success_count=success_count+excluded.success_count, failure_count=failure_count+excluded.failure_count, updated_at=excluded.updated_at"""
            ).use { statement ->
                statement.setString(1, page.value); statement.setString(2, actionName); statement.setString(3, appVersion)
                statement.setString(4, selector.resourceId); statement.setString(5, selector.textAny.firstOrNull()); statement.setString(6, selector.contentDescriptionAny.firstOrNull()); statement.setString(7, selector.role)
                statement.setInt(8, if (success) 1 else 0); statement.setInt(9, if (success) 0 else 1); statement.setLong(10, System.currentTimeMillis())
                statement.executeUpdate()
            }
        }
    }

    override fun close() = connection.close()
}
