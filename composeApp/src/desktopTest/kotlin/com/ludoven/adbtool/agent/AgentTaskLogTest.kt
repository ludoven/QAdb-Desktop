package com.ludoven.adbtool.agent

import java.io.File
import java.nio.file.Files
import java.sql.DriverManager
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AgentTaskLogTest {
    @Test
    fun `opening legacy database adds nullable audit columns and preserves rows`() = withTemporaryDatabase { database ->
        createLegacyMetricsDatabase(database)

        SqliteAgentTaskLogStore(database).use { store ->
            val metrics = store.recentMetrics()

            assertEquals(1, metrics.size)
            assertEquals("legacy-run", metrics.single().runId)
            assertNull(metrics.single().intent)
            assertNull(metrics.single().authority)
            assertNull(metrics.single().lastDecision)
            assertTrue(metrics.single().actionEffects.isEmpty())
            assertNull(metrics.single().evidenceCount)
            assertNull(metrics.single().httpAttempts)
            assertNull(metrics.single().protocolRepairs)
            assertNull(metrics.single().firstFeedbackMs)
            assertNull(metrics.single().firstOutputMs)
            assertNull(metrics.single().failureSubsystem)
            assertNull(metrics.single().failureStage)
            assertNull(metrics.single().failureCode)
        }

        DriverManager.getConnection("jdbc:sqlite:${database.absolutePath}").use { connection ->
            val columns = connection.createStatement().use { statement ->
                statement.executeQuery("PRAGMA table_info(agent_run_metrics)").use { result ->
                    buildSet {
                        while (result.next()) add(result.getString("name"))
                    }
                }
            }
            assertTrue(
                columns.containsAll(
                    setOf(
                        "intent", "authority", "last_decision", "action_effects", "evidence_count",
                        "http_attempts", "protocol_repairs", "first_feedback_ms", "first_output_ms",
                        "failure_subsystem", "failure_stage", "failure_code"
                    )
                )
            )
        }

        // A second open exercises idempotency: no duplicate-column migration is attempted.
        SqliteAgentTaskLogStore(database).use { store ->
            assertEquals("legacy-run", store.recentMetrics().single().runId)
        }
    }

    @Test
    fun `telemetry persists only explicitly reported values before or after completion`() =
        withTemporaryDatabase { database ->
            SqliteAgentTaskLogStore(database).use { store ->
                val runId = "status-run"
                store.start(runId, "我的 token=private-prompt", AgentTaskUiState(phase = AgentRunPhase.THINKING))
                store.recordTelemetry(
                    runId,
                    AgentRunAuditTelemetry(
                        intent = AgentTaskIntentKind.APP_CONTENT_READ,
                        authority = AgentTaskAccessLevel.NAVIGATION_READ_ONLY,
                        lastDecision = "execute_action",
                        actionEffects = setOf(AgentActionEffect.LAUNCH_APP, AgentActionEffect.NAVIGATION),
                        evidenceCount = 0,
                        httpAttempts = 1,
                        firstFeedbackMs = 42
                    )
                )
                store.record(
                    runId,
                    AgentTaskUiState(
                        phase = AgentRunPhase.COMPLETED,
                        usage = AgentUsage(promptTokens = 8, completionTokens = 4, totalTokens = 12),
                        budgetStatus = AgentBudgetStatus(modelCalls = 1),
                        observationTimings = AgentObservationTimings(totalMs = 80, hierarchyMs = 0)
                    )
                )
                store.recordTelemetry(
                    runId,
                    AgentRunAuditTelemetry(
                        lastDecision = "complete",
                        actionEffects = setOf(
                            AgentActionEffect.LAUNCH_APP,
                            AgentActionEffect.NAVIGATION,
                            AgentActionEffect.SCROLL
                        ),
                        evidenceCount = 1,
                        protocolRepairs = 0,
                        firstOutputMs = 77
                    )
                )

                val metrics = store.recentMetrics().single()
                assertEquals(AgentTaskIntentKind.APP_CONTENT_READ, metrics.intent)
                assertEquals(AgentTaskAccessLevel.NAVIGATION_READ_ONLY, metrics.authority)
                assertEquals("complete", metrics.lastDecision)
                assertEquals(
                    setOf(AgentActionEffect.LAUNCH_APP, AgentActionEffect.NAVIGATION, AgentActionEffect.SCROLL),
                    metrics.actionEffects
                )
                assertEquals(1, metrics.evidenceCount)
                assertEquals(1, metrics.httpAttempts)
                assertEquals(0, metrics.protocolRepairs)
                assertEquals(42, metrics.firstFeedbackMs)
                assertEquals(77, metrics.firstOutputMs)

                val logDetails = store.recent().map(AgentTaskLogEntry::detail)
                assertFalse(logDetails.any { it.contains("private-prompt") })
                assertFalse(logDetails.any { it.contains("token=", ignoreCase = true) })
            }
        }

    @Test
    fun `missing telemetry remains null instead of fabricated zero`() = withTemporaryDatabase { database ->
        SqliteAgentTaskLogStore(database).use { store ->
            store.start("plain-run", "hello", AgentTaskUiState())
            store.record("plain-run", AgentTaskUiState(phase = AgentRunPhase.COMPLETED))

            val metrics = store.recentMetrics().single()
            assertNull(metrics.intent)
            assertNull(metrics.authority)
            assertNull(metrics.lastDecision)
            assertTrue(metrics.actionEffects.isEmpty())
            assertNull(metrics.evidenceCount)
            assertNull(metrics.httpAttempts)
            assertNull(metrics.protocolRepairs)
            assertNull(metrics.firstFeedbackMs)
            assertNull(metrics.firstOutputMs)
            assertNull(metrics.failureSubsystem)
            assertNull(metrics.failureStage)
            assertNull(metrics.failureCode)
        }
    }

    @Test
    fun `structured stop code is audited without model reason text`() = withTemporaryDatabase { database ->
        SqliteAgentTaskLogStore(database).use { store ->
            val runId = "app-not-found-run"
            store.start(runId, "打开某个应用", AgentTaskUiState(phase = AgentRunPhase.THINKING))
            store.record(
                runId,
                AgentTaskUiState(
                    phase = AgentRunPhase.FAILED,
                    errorMessage = "未找到匹配应用",
                    failure = AgentFailure(
                        category = AgentFailureCategory.VERIFICATION,
                        retryable = true,
                        suggestedAction = AgentFailureAction.RETRY,
                        code = AgentFailureCode.VERIFICATION_FAILED,
                        technicalDetail = "stop_code=app_not_found"
                    )
                )
            )

            val details = store.recent().map(AgentTaskLogEntry::detail)
            assertTrue(details.contains("stop_code=app_not_found"))
            assertFalse(details.any { it.contains("未找到匹配应用") })
            val metrics = store.recentMetrics().single()
            assertEquals("stop_code=app_not_found", metrics.verificationFailure)
            assertEquals(AgentFailureSubsystem.VERIFICATION, metrics.failureSubsystem)
            assertEquals(AgentFailureStage.VERIFYING, metrics.failureStage)
            assertEquals(AgentFailureCode.VERIFICATION_FAILED, metrics.failureCode)
        }
    }

    @Test
    fun `read navigation gate code is stored without page content`() = withTemporaryDatabase { database ->
        SqliteAgentTaskLogStore(database).use { store ->
            val runId = "read-gate-run"
            store.start(runId, "读取应用内隐私内容", AgentTaskUiState(phase = AgentRunPhase.THINKING))
            store.record(
                runId,
                AgentTaskUiState(
                    phase = AgentRunPhase.FAILED,
                    errorMessage = "页面正文不应写入审计",
                    failure = agentFailure(
                        AgentFailureCategory.SAFETY_BLOCKED,
                        detail = "read_navigation_visual_coordinate"
                    )
                )
            )

            assertTrue(store.recent().map(AgentTaskLogEntry::detail).contains("read_navigation_visual_coordinate"))
            assertEquals(
                "read_navigation_visual_coordinate",
                store.recentMetrics().single().verificationFailure
            )
            assertFalse(store.recent().any { it.detail.contains("页面正文") })
        }
    }

    @Test
    fun `denied actions are not counted as executed device actions`() = withTemporaryDatabase { database ->
        SqliteAgentTaskLogStore(database).use { store ->
            val runId = "denied-run"
            store.start(runId, "卸载示例应用", AgentTaskUiState(phase = AgentRunPhase.THINKING))
            store.record(
                runId,
                AgentTaskUiState(
                    phase = AgentRunPhase.FAILED,
                    steps = listOf(
                        AgentStep(
                            id = "denied-step",
                            action = AgentAction.UninstallPackage("com.example.app"),
                            status = AgentStepStatus.DENIED,
                            result = "User denied this action"
                        )
                    )
                )
            )

            assertEquals(0, store.recentMetrics().single().actionCount)
        }
    }

    @Test
    fun `action count uses explicit execution evidence instead of failed status`() =
        withTemporaryDatabase { database ->
            SqliteAgentTaskLogStore(database).use { store ->
                val runId = "execution-evidence"
                store.start(runId, "safe task", AgentTaskUiState(phase = AgentRunPhase.THINKING))
                store.record(
                    runId,
                    AgentTaskUiState(
                        phase = AgentRunPhase.FAILED,
                        steps = listOf(
                            AgentStep(
                                id = "blocked-before-execute",
                                action = AgentAction.Tap(10, 10),
                                status = AgentStepStatus.FAILED,
                                riskLevel = AgentRiskLevel.BLOCKED
                            ),
                            AgentStep(
                                id = "validation-failed",
                                action = AgentAction.Tap(20, 20),
                                status = AgentStepStatus.FAILED
                            ),
                            AgentStep(
                                id = "entered-gateway",
                                action = AgentAction.Tap(30, 30),
                                status = AgentStepStatus.FAILED,
                                executedActionCount = 1
                            ),
                            AgentStep(
                                id = "scroll-until-swipes",
                                action = AgentAction.Swipe(10, 90, 10, 10, 300),
                                status = AgentStepStatus.COMPLETED,
                                executedActionCount = 8
                            )
                        )
                    )
                )

                assertEquals(9, store.recentMetrics().single().actionCount)
            }
        }

    @Test
    fun `v2 action count uses primitive metric without double counting steps`() =
        withTemporaryDatabase { database ->
            SqliteAgentTaskLogStore(database).use { store ->
                val runId = "v2-action-count"
                store.start(runId, "safe task", AgentTaskUiState(phase = AgentRunPhase.THINKING))
                store.record(
                    runId,
                    AgentTaskUiState(
                        phase = AgentRunPhase.COMPLETED,
                        executionStrategy = AgentExecutionStrategy.SEMANTIC_V2,
                        steps = listOf(
                            AgentStep(
                                id = "executed-step",
                                action = AgentAction.Tap(10, 10),
                                status = AgentStepStatus.COMPLETED,
                                executedActionCount = 1
                            )
                        ),
                        v2Metrics = AgentV2RunMetrics(primitiveActions = 1)
                    )
                )

                val metrics = store.recentMetrics().single()
                assertEquals(1, metrics.actionCount)
                assertEquals(1, metrics.primitiveActions)
            }
        }

    @Test
    fun `concurrent runs keep fingerprints telemetry and terminal metrics isolated`() =
        withTemporaryDatabase { database ->
            SqliteAgentTaskLogStore(database).use { store ->
                val runCount = 64
                val startGate = CountDownLatch(1)
                val executor = Executors.newFixedThreadPool(8)
                try {
                    val futures = (0 until runCount).map { index ->
                        executor.submit {
                            startGate.await()
                            val runId = "concurrent-$index"
                            store.start(runId, "task $index", AgentTaskUiState(phase = AgentRunPhase.THINKING))
                            store.recordTelemetry(
                                runId,
                                AgentRunAuditTelemetry(
                                    intent = AgentTaskIntentKind.DEVICE_STATUS,
                                    httpAttempts = index
                                )
                            )
                            store.record(
                                runId,
                                AgentTaskUiState(
                                    phase = AgentRunPhase.COMPLETED,
                                    usage = AgentUsage(totalTokens = index)
                                )
                            )
                        }
                    }
                    startGate.countDown()
                    futures.forEach { it.get(10, TimeUnit.SECONDS) }
                } finally {
                    executor.shutdownNow()
                }

                val metricsByRun = store.recentMetrics(runCount).associateBy(AgentRunMetrics::runId)
                assertEquals(runCount, metricsByRun.size)
                (0 until runCount).forEach { index ->
                    val metrics = metricsByRun.getValue("concurrent-$index")
                    assertEquals(index, metrics.httpAttempts)
                    assertEquals(index, metrics.totalTokens)
                    assertEquals(AgentTaskIntentKind.DEVICE_STATUS, metrics.intent)
                    assertTrue(metrics.elapsedMs >= 0)
                }
            }
        }

    @Test
    fun `terminal errors persist only stable local categories`() = withTemporaryDatabase { database ->
        SqliteAgentTaskLogStore(database).use { store ->
            val runId = "private-failure"
            val userText = "用户私密正文-不得落库"
            val bearerSecret = "sk-live-secret"
            val jsonSecret = "json-secret"
            val modelBlockedSummary =
                "Task blocked: goal verification failed; Authorization: Bearer $bearerSecret; " +
                    "{\"api_key\":\"$jsonSecret\"}; $userText"

            store.start(runId, userText, AgentTaskUiState(phase = AgentRunPhase.THINKING))
            store.record(
                runId,
                AgentTaskUiState(
                    phase = AgentRunPhase.FAILED,
                    errorMessage = modelBlockedSummary
                )
            )

            val failedEntry = store.recent().first { it.runId == runId && it.phase == AgentRunPhase.FAILED }
            assertEquals("Task failed (category=authentication; detail omitted)", failedEntry.detail)
            assertEquals(SAFE_VERIFICATION_MARKER, store.recentMetrics().single().verificationFailure)
            val persistedText = buildString {
                store.recent().forEach { appendLine(it.detail) }
                store.recentMetrics().forEach { appendLine(it.verificationFailure) }
            }
            assertFalse(persistedText.contains(userText))
            assertFalse(persistedText.contains(bearerSecret))
            assertFalse(persistedText.contains(jsonSecret))
            assertFalse(persistedText.contains("Bearer", ignoreCase = true))
            assertFalse(persistedText.contains("api_key", ignoreCase = true))

            val entriesBeforeDuplicate = store.recent().filter { it.runId == runId }
            store.record(
                runId,
                AgentTaskUiState(phase = AgentRunPhase.FAILED, errorMessage = modelBlockedSummary)
            )
            assertEquals(entriesBeforeDuplicate, store.recent().filter { it.runId == runId })
            assertEquals(1, store.recentMetrics().count { it.runId == runId })
            assertRunStateCleared(store, runId)
        }
    }

    @Test
    fun `step result is strictly sanitized before fingerprinting and persistence`() =
        withTemporaryDatabase { database ->
            SqliteAgentTaskLogStore(database).use { store ->
                val runId = "secret-step"
                store.start(runId, "safe task", AgentTaskUiState(phase = AgentRunPhase.THINKING))

                fun stateWithSecrets(suffix: String) = AgentTaskUiState(
                    phase = AgentRunPhase.THINKING,
                    steps = listOf(
                        AgentStep(
                            id = "same-step",
                            action = AgentAction.FindApp("settings"),
                            status = AgentStepStatus.FAILED,
                            result = "Authorization: Bearer auth-$suffix; " +
                                "{\"secret\":\"json-$suffix\"}; token=Bearer token-$suffix; " +
                                "Bearer standalone-$suffix"
                        )
                    )
                )

                store.record(runId, stateWithSecrets("one"))
                store.record(runId, stateWithSecrets("two"))

                val entries = store.recent().filter { it.runId == runId }
                assertEquals(2, entries.size, "Secret-only changes must not create a second fingerprint")
                val persisted = entries.joinToString("\n", transform = AgentTaskLogEntry::detail)
                listOf(
                    "auth-one", "auth-two",
                    "json-one", "json-two",
                    "token-one", "token-two",
                    "standalone-one", "standalone-two"
                ).forEach { secret -> assertFalse(persisted.contains(secret)) }
                assertTrue(persisted.contains("Authorization: <redacted>"))
                assertTrue(persisted.contains("\"secret\":<redacted>"))
                assertTrue(persisted.contains("Bearer <redacted>"))
            }
        }

    private fun createLegacyMetricsDatabase(database: File) {
        Class.forName("org.sqlite.JDBC")
        DriverManager.getConnection("jdbc:sqlite:${database.absolutePath}").use { connection ->
            connection.createStatement().use { statement ->
                statement.execute(
                    """CREATE TABLE agent_run_metrics (
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
                        observation_hierarchy_ms INTEGER NOT NULL
                    )"""
                )
                statement.execute(
                    """INSERT INTO agent_run_metrics VALUES (
                        'legacy-run', 123, 'COMPLETED', 'INTERACTIVE', 'ADB',
                        50, 0, 0, 0, 0, 0, NULL, NULL, 10, 8
                    )"""
                )
            }
        }
    }

    private fun withTemporaryDatabase(block: (File) -> Unit) {
        val directory = Files.createTempDirectory("qadb-agent-task-log-test").toFile()
        try {
            block(File(directory, "agent.db"))
        } finally {
            directory.deleteRecursively()
        }
    }

    private fun assertRunStateCleared(store: SqliteAgentTaskLogStore, runId: String) {
        listOf("lastFingerprintByRun", "startedAtByRun", "telemetryByRun").forEach { fieldName ->
            val field = SqliteAgentTaskLogStore::class.java.getDeclaredField(fieldName).apply {
                isAccessible = true
            }
            @Suppress("UNCHECKED_CAST")
            val retained = field.get(store) as Map<String, *>
            assertFalse(runId in retained, "$fieldName retained terminal run $runId")
        }
    }
}

private const val SAFE_VERIFICATION_MARKER = "verification_or_goal_failure"
