package com.ludoven.adbtool.agent

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

/** Production V2 boundary: fixed local page-navigation skills are never reachable through this engine. */
class CuratedDeviceCommandEngine(deviceGateway: AgentDeviceGateway) : DeviceEngine {
    private val delegate: DeviceEngine = DeterministicSystemCommandEngine(deviceGateway)
    override suspend fun execute(
        deviceId: String,
        goal: SemanticGoal,
        budget: V2ExecutionBudget,
        confirmSensitiveAction: suspend (AgentStep) -> Boolean
    ): DeviceGoalEvidence {
        require(goal.kind in CURATED_LOCAL_GOALS) {
            "Application navigation must be decided step-by-step by V2 Brain"
        }
        return delegate.execute(deviceId, goal, budget, confirmSensitiveAction)
    }
}

/** Production-only executor for deterministic system capabilities. It contains no application UI flow. */
private class DeterministicSystemCommandEngine(
    private val deviceGateway: AgentDeviceGateway,
    private val riskEvaluator: AgentRiskEvaluator = AgentRiskEvaluator()
) : DeviceEngine {
    override suspend fun execute(
        deviceId: String,
        goal: SemanticGoal,
        budget: V2ExecutionBudget,
        confirmSensitiveAction: suspend (AgentStep) -> Boolean
    ): DeviceGoalEvidence = try {
        withTimeout(budget.operationTimeoutMs) {
            require(deviceGateway.isConnected(deviceId)) { "The selected device is no longer connected" }
            require(goal.kind in CURATED_LOCAL_GOALS) { "The goal is not a deterministic system command" }
            when (goal.kind) {
                SemanticGoalKind.SYSTEM_SETTING -> changeSetting(deviceId, goal)
                SemanticGoalKind.UNINSTALL_APP -> executePackageAction(
                    deviceId,
                    goal,
                    AgentAction.UninstallPackage(requireExactPackage(goal)),
                    confirmSensitiveAction
                )
                SemanticGoalKind.CLEAR_APP_DATA -> executePackageAction(
                    deviceId,
                    goal,
                    AgentAction.ClearAppData(requireExactPackage(goal)),
                    confirmSensitiveAction
                )
                else -> error("Application navigation is not a deterministic system command")
            }
        }
    } catch (_: TimeoutCancellationException) {
        failedSystemEvidence(goal, "The deterministic system operation timed out")
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        failedSystemEvidence(goal, failure.safeDeviceEngineMessage())
    }

    private suspend fun changeSetting(deviceId: String, goal: SemanticGoal): DeviceGoalEvidence {
        val gateway = deviceGateway as? CuratedDeviceCommandGateway
            ?: return failedSystemEvidence(goal, "The active device channel does not support curated settings")
        val setting = parseSetting(goal.target)
            ?: return failedSystemEvidence(goal, "The requested setting is not in the curated allowlist")
        val requested = parseSettingValue(setting, goal.value)
            ?: return failedSystemEvidence(goal, "The requested setting value is invalid")
        val result = gateway.writeSetting(deviceId, setting, requested)
        val verified = result.success && gateway.readSetting(deviceId, setting) == requested
        return systemEvidence(
            goal = goal,
            verified = verified,
            actionName = "set_${setting.name.lowercase()}",
            detail = if (verified) "Verified the new system value" else "The system value did not match after writing",
            executedActionCount = 1
        )
    }

    private suspend fun executePackageAction(
        deviceId: String,
        goal: SemanticGoal,
        action: AgentAction,
        confirmSensitiveAction: suspend (AgentStep) -> Boolean
    ): DeviceGoalEvidence {
        val observation = deviceGateway.observeLightweight(deviceId, includeUiHierarchy = false)
        validateAgentAction(action, observation).getOrElse {
            return failedSystemEvidence(goal, it.message.orEmpty())
        }
        val risk = riskEvaluator.evaluate(
            action,
            observation,
            deviceGateway.confirmationRequirement(deviceId, action, observation)
        )
        if (risk.level == AgentRiskLevel.BLOCKED) {
            return failedSystemEvidence(goal, risk.reason, DeviceGoalTerminalReason.POLICY_BLOCKED)
        }
        if (risk.level == AgentRiskLevel.CONFIRMATION_REQUIRED) {
            val approved = confirmSensitiveAction(
                AgentStep(
                    id = "v2-system-confirm",
                    action = action,
                    status = AgentStepStatus.AWAITING_CONFIRMATION,
                    riskLevel = risk.level,
                    confirmationReason = risk.reason
                )
            )
            if (!approved) {
                return failedSystemEvidence(
                    goal,
                    "The user denied the high-risk action",
                    DeviceGoalTerminalReason.USER_DENIED
                )
            }
            val approvalBound = (deviceGateway as? AgentPreparedActionApprovalGateway)
                ?.approvePreparedAction(deviceId, action, observation.observationId)
                ?: true
            if (!approvalBound) {
                return failedSystemEvidence(goal, "The high-risk action approval expired before execution")
            }
        }

        val result = deviceGateway.execute(deviceId, action)
        val verified = when (action) {
            is AgentAction.UninstallPackage -> result.success &&
                (deviceGateway as? AgentAppCatalogGateway)
                    ?.readInstalledApps(deviceId, forceRefresh = true)
                    ?.none { it.packageName == action.packageName } == true
            is AgentAction.ClearAppData -> result.success
            else -> false
        }
        val detail = when {
            verified && action is AgentAction.UninstallPackage -> "Verified that the application is no longer installed"
            verified -> "The package manager confirmed the system operation"
            else -> result.output.ifBlank { "The system operation could not be verified" }
        }
        return systemEvidence(goal, verified, action.toolName, detail, executedActionCount = 1)
    }

    private fun requireExactPackage(goal: SemanticGoal): String = requireNotNull(goal.app?.takeIf(String::isNotBlank)) {
        "The deterministic system command requires an exact resolved package"
    }

    private fun systemEvidence(
        goal: SemanticGoal,
        verified: Boolean,
        actionName: String,
        detail: String,
        executedActionCount: Int
    ): DeviceGoalEvidence = DeviceGoalEvidence(
        goal = goal,
        verified = verified,
        observationRevision = 0,
        actionEvidence = listOf(
            DeviceActionEvidence(
                actionName = actionName,
                progress = if (verified) DeviceActionProgress.PROGRESSED else DeviceActionProgress.FAILED,
                observationRevision = 0,
                detail = detail.take(MAX_EVIDENCE_DETAIL_CHARS),
                executedActionCount = executedActionCount
            )
        ),
        summary = detail.take(MAX_EVIDENCE_DETAIL_CHARS),
        perceptionSource = AgentPerceptionSource.ADB,
        primitiveActionCount = executedActionCount,
        terminalReason = if (verified) DeviceGoalTerminalReason.NONE else DeviceGoalTerminalReason.FAILED
    )

    private fun failedSystemEvidence(
        goal: SemanticGoal,
        reason: String,
        terminalReason: DeviceGoalTerminalReason = DeviceGoalTerminalReason.FAILED
    ): DeviceGoalEvidence = DeviceGoalEvidence(
        goal = goal,
        verified = false,
        observationRevision = 0,
        actionEvidence = emptyList(),
        summary = reason.take(MAX_EVIDENCE_DETAIL_CHARS),
        perceptionSource = AgentPerceptionSource.ADB,
        primitiveActionCount = 0,
        terminalReason = terminalReason
    )
}

private val CURATED_LOCAL_GOALS = setOf(
    SemanticGoalKind.SYSTEM_SETTING,
    SemanticGoalKind.UNINSTALL_APP,
    SemanticGoalKind.CLEAR_APP_DATA
)

/** Legacy test oracle for the pre-Harness page-flow behavior. Never wire this into production. */
internal class LegacyLocalDeviceEngine(
    private val deviceGateway: AgentDeviceGateway,
    private val observationCoordinator: AgentObservationCoordinator = AgentObservationCoordinator(deviceGateway),
    private val riskEvaluator: AgentRiskEvaluator = AgentRiskEvaluator()
) : DeviceEngine {
    override suspend fun execute(
        deviceId: String,
        goal: SemanticGoal,
        budget: V2ExecutionBudget,
        confirmSensitiveAction: suspend (AgentStep) -> Boolean
    ): DeviceGoalEvidence {
        val session = ExecutionSession(deviceId, goal, budget, confirmSensitiveAction)
        return try {
            withTimeout(budget.operationTimeoutMs) {
                session.execute()
            }
        } catch (_: TimeoutCancellationException) {
            session.failureEvidence("The semantic device operation timed out")
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            session.failureEvidence(failure.safeDeviceEngineMessage())
        }
    }

    private inner class ExecutionSession(
        private val deviceId: String,
        private val goal: SemanticGoal,
        private val budget: V2ExecutionBudget,
        private val confirmSensitiveAction: suspend (AgentStep) -> Boolean
    ) {
        private val evidence = mutableListOf<DeviceActionEvidence>()
        private var primitiveActions = 0
        private var terminalReason = DeviceGoalTerminalReason.NONE
        private lateinit var frame: AgentObservationFrame

        suspend fun execute(): DeviceGoalEvidence {
            require(deviceGateway.isConnected(deviceId)) { "The selected device is no longer connected" }
            frame = observe(ObservationFreshness.REQUIRE_FRESH)
            return when (goal.kind) {
                SemanticGoalKind.OPEN_APP -> openApp()
                SemanticGoalKind.READ_APP_CONTENT -> failed(
                    "Application content reads require the V2 semantic observation loop"
                )
                SemanticGoalKind.FIND_AND_CLICK -> findAndClick()
                SemanticGoalKind.INPUT_TEXT -> inputText()
                SemanticGoalKind.OPEN_CHAT -> openChat()
                SemanticGoalKind.SEND_MESSAGE -> sendMessage()
                SemanticGoalKind.SYSTEM_SETTING -> changeSystemSetting()
                SemanticGoalKind.UNINSTALL_APP -> uninstallApp()
                SemanticGoalKind.CLEAR_APP_DATA -> clearAppData()
                SemanticGoalKind.DELETE_CONTENT -> deleteContent()
            }
        }

        private suspend fun openApp(): DeviceGoalEvidence {
            val app = goal.app ?: goal.target
            if (app.isNullOrBlank()) return failed("No application name was provided")
            val result = perform(AgentAction.OpenApp(app))
            val expectedPackage = result.resolvedPackages.singleOrNull()
            val verified = result.success && expectedPackage != null && frame.observation.belongsToPackage(expectedPackage)
            return complete(
                verified,
                if (verified) "Verified that $app is in the foreground" else "The application did not reach the foreground"
            )
        }

        private suspend fun findAndClick(): DeviceGoalEvidence {
            val target = goal.target?.takeIf(String::isNotBlank) ?: return failed("No UI target was provided")
            val node = uniqueNode(target, requireClickable = true)
            val action = node?.let(::tap) ?: visualTap(goal.visualGrounding)
                ?: return failed("The requested UI target is missing or ambiguous")
            val result = perform(action)
            return complete(result.success && evidence.last().progress == DeviceActionProgress.PROGRESSED, evidence.last().detail)
        }

        private suspend fun inputText(): DeviceGoalEvidence {
            val value = goal.value?.takeIf(String::isNotBlank) ?: return failed("No input text was provided")
            val node = inputNode(goal.target) ?: return failed("No unique editable field was found")
            val result = perform(input(node, value))
            val observedValue = currentNode(node)?.text.orEmpty()
            return complete(result.success && observedValue == value, if (observedValue == value) {
                "Verified the input field value"
            } else {
                "Text input was not verified on fresh device evidence"
            })
        }

        private suspend fun openChat(): DeviceGoalEvidence {
            val recipient = goal.target?.takeIf(String::isNotBlank) ?: return failed("No chat recipient was provided")
            if (!goal.app.isNullOrBlank()) {
                val launch = perform(AgentAction.OpenApp(goal.app))
                if (!launch.success) return failed("The messaging application could not be opened")
            }
            val opened = reachChat(recipient)
            return complete(opened, if (opened) {
                "Verified the target chat and message composer"
            } else {
                "The target chat could not be located unambiguously"
            })
        }

        private suspend fun sendMessage(): DeviceGoalEvidence {
            val recipient = requireNotNull(goal.target)
            val message = requireNotNull(goal.value)
            if (!goal.app.isNullOrBlank()) {
                val launch = perform(AgentAction.OpenApp(goal.app))
                if (!launch.success) return failed("The messaging application could not be opened")
            }
            if (!reachChat(recipient)) return failed("The target chat could not be verified")

            val beforeCount = visibleMessageCount(message)
            val composer = inputNode(null) ?: return failed("No unique message composer was found")
            if (!perform(input(composer, message)).success) return failed("The message could not be entered")
            val enteredComposer = inputNode(null)
            if (enteredComposer == null || currentNode(enteredComposer)?.text != message) {
                return failed("The message composer content could not be verified")
            }
            val send = uniqueNodeByTerms(SEND_TERMS, requireClickable = true)
                ?: return failed("No unique send control was found")
            if (!perform(tap(send)).success) return failed("The send control did not complete")

            val afterCount = visibleMessageCount(message)
            val composerCleared = frame.observation.uiNodes.filter { it.editable && !it.password }
                .none { it.text == message }
            val verified = afterCount > beforeCount && composerCleared
            return complete(verified, if (verified) {
                "Verified a new outgoing message in the target chat"
            } else {
                "The send action completed, but a new outgoing message was not verified"
            })
        }

        private suspend fun changeSystemSetting(): DeviceGoalEvidence {
            val gateway = deviceGateway as? CuratedDeviceCommandGateway
                ?: return failed("The active device channel does not support curated settings")
            val setting = parseSetting(goal.target) ?: return failed("The requested setting is not in the curated allowlist")
            val requested = parseSettingValue(setting, goal.value)
                ?: return failed("The requested setting value is invalid")
            ensureActionBudget()
            val result = gateway.writeSetting(deviceId, setting, requested)
            primitiveActions += 1
            observationCoordinator.markMutation(deviceId)
            val observed = if (result.success) gateway.readSetting(deviceId, setting) else null
            val verified = result.success && observed == requested
            evidence += DeviceActionEvidence(
                actionName = "set_${setting.name.lowercase()}",
                progress = if (verified) DeviceActionProgress.PROGRESSED else DeviceActionProgress.FAILED,
                observationRevision = frame.observation.revision,
                detail = if (verified) "Verified the new system value" else "The system value did not match after writing",
                executedActionCount = 1
            )
            return complete(verified, evidence.last().detail)
        }

        private suspend fun uninstallApp(): DeviceGoalEvidence {
            val app = goal.app ?: goal.target
            if (app.isNullOrBlank()) return failed("No application was provided for uninstall")
            val packageName = resolveInstalledPackage(app)
                ?: return failed("The application name is missing or ambiguous")
            val result = perform(AgentAction.UninstallPackage(packageName))
            if (!result.success) return failed(result.output)
            val remaining = (deviceGateway as? AgentAppCatalogGateway)
                ?.readInstalledApps(deviceId, forceRefresh = true)
                ?.any { it.packageName == packageName }
            val verified = remaining == false
            return complete(verified, if (verified) {
                "Verified that the application is no longer installed"
            } else {
                "The uninstall command completed, but removal was not verified"
            })
        }

        private suspend fun clearAppData(): DeviceGoalEvidence {
            val app = goal.app ?: goal.target
            if (app.isNullOrBlank()) return failed("No application was provided for data clearing")
            val packageName = resolveInstalledPackage(app)
                ?: return failed("The application name is missing or ambiguous")
            val result = perform(AgentAction.ClearAppData(packageName))
            return complete(result.success, if (result.success) {
                "The package manager confirmed that application data was cleared"
            } else {
                result.output
            })
        }

        private suspend fun deleteContent(): DeviceGoalEvidence {
            val target = goal.target?.takeIf(String::isNotBlank)
                ?: return failed("No content-removal target was provided")
            val node = uniqueNode(target, requireClickable = true)
                ?: return failed("The destructive UI target is missing or ambiguous")
            val result = perform(
                AgentAction.TapElement(
                    observationId = frame.observation.observationId,
                    elementId = node.elementId,
                    meta = AgentActionMeta(
                        intent = "delete user-requested content",
                        target = target,
                        operationKind = AgentOperationKind.DELETE
                    )
                )
            )
            val targetGone = matchingNodes(target, requireClickable = false).isEmpty()
            return complete(result.success && targetGone, if (targetGone) {
                "Verified that the requested content control is no longer present"
            } else {
                "The delete action completed, but content removal was not verified"
            })
        }

        private suspend fun resolveInstalledPackage(query: String): String? {
            val gateway = deviceGateway as? AgentAppCatalogGateway ?: return null
            val matches = rankInstalledApps(gateway.readInstalledApps(deviceId), query)
            return matches.singleOrNull()?.packageName
        }

        private suspend fun reachChat(recipient: String): Boolean {
            if (chatIsVerified(recipient)) return true
            val existingResult = uniqueResultNode(recipient)
            if (existingResult != null) {
                if (!perform(tap(existingResult)).success) return false
                return waitForUiCondition(CHAT_TRANSITION_TIMEOUT_MS) { chatIsVerified(recipient) }
            }

            var searchField = inputNode(SEARCH_TERMS.first())
            if (searchField == null) {
                val searchControl = uniqueNodeByTerms(SEARCH_TERMS, requireClickable = true) ?: return false
                if (!perform(tap(searchControl)).success) return false
                searchField = waitForUiValue(SEARCH_FIELD_TIMEOUT_MS) {
                    inputNode(SEARCH_TERMS.first()) ?: inputNode(null)
                } ?: return false
            }
            val currentQuery = currentNode(searchField)?.text.orEmpty()
            if (currentQuery != recipient) {
                if (!perform(input(searchField, recipient, replaceExisting = currentQuery.isNotEmpty())).success) return false
                if (!waitForUiCondition(SEARCH_QUERY_TIMEOUT_MS) {
                        val enteredQuery = inputNode(SEARCH_TERMS.first()) ?: inputNode(null)
                        enteredQuery != null && currentNode(enteredQuery)?.text == recipient
                    }
                ) return false
            }
            val result = waitForUiValue(SEARCH_RESULT_TIMEOUT_MS) { uniqueResultNode(recipient) } ?: return false
            if (!perform(tap(result)).success) return false
            return waitForUiCondition(CHAT_TRANSITION_TIMEOUT_MS) { chatIsVerified(recipient) }
        }

        private suspend fun waitForUiCondition(timeoutMs: Long, predicate: () -> Boolean): Boolean =
            waitForUiValue(timeoutMs) { true.takeIf { predicate() } } == true

        /** Polls only fresh observations; it never repeats the preceding device action. */
        private suspend fun <T : Any> waitForUiValue(timeoutMs: Long, value: () -> T?): T? {
            value()?.let { return it }
            val startedAtNanos = System.nanoTime()
            while (true) {
                val elapsedMs = (System.nanoTime() - startedAtNanos) / NANOS_PER_MILLISECOND
                val remainingMs = timeoutMs - elapsedMs
                if (remainingMs <= 0) return null
                delay(minOf(UI_PREDICATE_POLL_INTERVAL_MS, remainingMs))
                val fresh = withTimeoutOrNull(remainingMs) {
                    observe(ObservationFreshness.REQUIRE_FRESH)
                } ?: return null
                frame = fresh
                value()?.let { return it }
            }
        }

        private fun chatIsVerified(recipient: String): Boolean {
            val hasRecipient = matchingNodes(recipient, requireClickable = false).isNotEmpty()
            val hasComposer = frame.observation.uiNodes.any { it.enabled && it.editable && !it.password }
            val hasSend = nodesByTerms(SEND_TERMS, requireClickable = true).isNotEmpty()
            return hasRecipient && hasComposer && hasSend
        }

        private fun visibleMessageCount(text: String): Int = frame.observation.uiNodes.count { node ->
            !node.editable && (node.text == text || node.contentDescription == text)
        }

        private fun inputNode(label: String?): UiNodeSnapshot? {
            val editable = frame.observation.uiNodes.filter { it.enabled && it.editable && !it.password }
            if (!label.isNullOrBlank()) {
                val matching = editable.filter { it.semanticText().contains(label, ignoreCase = true) }
                if (matching.size == 1) return matching.single()
            }
            return editable.singleOrNull()
        }

        private fun uniqueNode(query: String, requireClickable: Boolean): UiNodeSnapshot? {
            val matches = matchingNodes(query, requireClickable)
            val exact = matches.filter { it.semanticText().equals(query, ignoreCase = true) }
            return when {
                exact.size == 1 -> exact.single()
                exact.size > 1 -> null
                matches.size == 1 -> matches.single()
                else -> null
            }
        }

        private fun uniqueResultNode(query: String): UiNodeSnapshot? {
            val textMatches = matchingNodes(query, requireClickable = false)
                .filterNot(UiNodeSnapshot::editable)
            val exact = textMatches.filter { node ->
                node.text.equals(query, ignoreCase = true) ||
                    node.contentDescription.equals(query, ignoreCase = true)
            }
            return exact.distinctBy(UiNodeSnapshot::elementId).singleOrNull()
        }

        private fun uniqueNodeByTerms(terms: List<String>, requireClickable: Boolean): UiNodeSnapshot? =
            nodesByTerms(terms, requireClickable).singleOrNull()

        private fun matchingNodes(query: String, requireClickable: Boolean): List<UiNodeSnapshot> =
            frame.observation.uiNodes.filter { node ->
                node.enabled && (!requireClickable || node.clickable) && node.semanticText().contains(query, ignoreCase = true)
            }

        private fun nodesByTerms(terms: List<String>, requireClickable: Boolean): List<UiNodeSnapshot> =
            frame.observation.uiNodes.filter { node ->
                node.enabled && (!requireClickable || node.clickable) && terms.any { term ->
                    node.semanticText().equals(term, ignoreCase = true) ||
                        node.semanticText().contains(term, ignoreCase = true)
                }
            }.distinctBy(UiNodeSnapshot::elementId)

        private fun currentNode(previous: UiNodeSnapshot): UiNodeSnapshot? = frame.observation.uiNodes.firstOrNull { node ->
            node.resourceId.isNotBlank() && node.resourceId == previous.resourceId ||
                node.role.isNotBlank() && node.role == previous.role && node.bounds == previous.bounds
        }

        private fun tap(node: UiNodeSnapshot): AgentAction.TapElement = AgentAction.TapElement(
            observationId = frame.observation.observationId,
            elementId = node.elementId
        )

        private fun input(
            node: UiNodeSnapshot,
            value: String,
            replaceExisting: Boolean = false
        ): AgentAction.InputText = AgentAction.InputText(
            text = value,
            observationId = frame.observation.observationId,
            elementId = node.elementId,
            replaceExisting = replaceExisting
        )

        private fun visualTap(grounding: VisualGrounding?): AgentAction.Tap? {
            grounding ?: return null
            if (grounding.revision != frame.observation.revision) return null
            val centerXPermille = grounding.leftPermille +
                (grounding.rightPermille - grounding.leftPermille) / 2
            val centerYPermille = grounding.topPermille +
                (grounding.bottomPermille - grounding.topPermille) / 2
            val x = (frame.observation.screenWidth.toLong() * centerXPermille / 1000L)
                .toInt().coerceIn(0, frame.observation.screenWidth - 1)
            val y = (frame.observation.screenHeight.toLong() * centerYPermille / 1000L)
                .toInt().coerceIn(0, frame.observation.screenHeight - 1)
            return AgentAction.Tap(x, y, frame.observation.observationId)
        }

        private suspend fun perform(action: AgentAction): AgentToolResult {
            ensureActionBudget()
            validateAgentAction(action, frame.observation).getOrElse {
                evidence += DeviceActionEvidence(
                    actionName = action.toolName,
                    progress = DeviceActionProgress.FAILED,
                    observationRevision = frame.observation.revision,
                    detail = it.message.orEmpty().take(MAX_EVIDENCE_DETAIL_CHARS)
                )
                return AgentToolResult(false, evidence.last().detail)
            }
            val risk = riskEvaluator.evaluate(
                action,
                frame.observation,
                deviceGateway.confirmationRequirement(deviceId, action, frame.observation)
            )
            if (risk.level == AgentRiskLevel.BLOCKED) {
                terminalReason = DeviceGoalTerminalReason.POLICY_BLOCKED
                val detail = risk.reason
                evidence += DeviceActionEvidence(
                    actionName = action.toolName,
                    progress = DeviceActionProgress.FAILED,
                    observationRevision = frame.observation.revision,
                    detail = detail.take(MAX_EVIDENCE_DETAIL_CHARS)
                )
                return AgentToolResult(false, detail)
            }
            if (risk.level == AgentRiskLevel.CONFIRMATION_REQUIRED) {
                val confirmationStep = AgentStep(
                    id = "v2-confirm-${evidence.size}",
                    action = action,
                    status = AgentStepStatus.AWAITING_CONFIRMATION,
                    riskLevel = risk.level,
                    confirmationReason = risk.reason
                )
                if (!confirmSensitiveAction(confirmationStep)) {
                    terminalReason = DeviceGoalTerminalReason.USER_DENIED
                    evidence += DeviceActionEvidence(
                        actionName = action.toolName,
                        progress = DeviceActionProgress.FAILED,
                        observationRevision = frame.observation.revision,
                        detail = "The user denied the high-risk action"
                    )
                    return AgentToolResult(false, evidence.last().detail)
                }
            }

            val beforeRevision = frame.observation.revision
            val result = deviceGateway.execute(deviceId, action)
            primitiveActions += 1
            observationCoordinator.markMutation(deviceId)
            try {
                frame = observe(
                    freshness = ObservationFreshness.WAIT_FOR_CHANGE,
                    baselineRevision = beforeRevision
                )
            } catch (failure: Exception) {
                evidence += DeviceActionEvidence(
                    actionName = action.toolName,
                    progress = DeviceActionProgress.AMBIGUOUS,
                    observationRevision = beforeRevision,
                    detail = "The action was sent, but fresh post-action evidence was unavailable",
                    executedActionCount = 1
                )
                throw failure
            }
            val progress = when {
                !result.success -> DeviceActionProgress.FAILED
                frame.observation.revision != beforeRevision -> DeviceActionProgress.PROGRESSED
                else -> DeviceActionProgress.NO_CHANGE
            }
            evidence += DeviceActionEvidence(
                actionName = action.toolName,
                progress = progress,
                observationRevision = frame.observation.revision,
                detail = when (progress) {
                    DeviceActionProgress.PROGRESSED -> "Fresh device evidence changed after the action"
                    DeviceActionProgress.NO_CHANGE -> "No device-state change was observed"
                    DeviceActionProgress.AMBIGUOUS -> "The action result is ambiguous"
                    DeviceActionProgress.FAILED -> result.output.take(MAX_EVIDENCE_DETAIL_CHARS)
                },
                executedActionCount = 1
            )
            return result
        }

        private suspend fun observe(
            freshness: ObservationFreshness,
            baselineRevision: Long? = null
        ): AgentObservationFrame = observationCoordinator.observe(
            deviceId,
            AgentObservationRequest(
                includeUiHierarchy = true,
                includeScreenshot = false,
                freshness = freshness,
                baselineRevision = baselineRevision,
                timeoutMs = POST_ACTION_OBSERVATION_TIMEOUT_MS
            )
        )

        private fun ensureActionBudget() {
            check(primitiveActions < budget.maxDeviceActions && primitiveActions < budget.absoluteMaxDeviceActions) {
                "The device-action budget was exhausted"
            }
        }

        private fun complete(verified: Boolean, summary: String): DeviceGoalEvidence = DeviceGoalEvidence(
            goal = goal,
            verified = verified && evidence.none { it.progress in setOf(DeviceActionProgress.FAILED, DeviceActionProgress.AMBIGUOUS) },
            observationRevision = frame.observation.revision,
            actionEvidence = evidence.toList(),
            summary = summary.take(MAX_EVIDENCE_DETAIL_CHARS),
            perceptionSource = when (frame.observation.source) {
                AgentObservationSource.BRIDGE -> AgentPerceptionSource.BRIDGE
                AgentObservationSource.ADB -> AgentPerceptionSource.ADB
            },
            primitiveActionCount = primitiveActions,
            terminalReason = if (!verified && terminalReason == DeviceGoalTerminalReason.NONE) {
                DeviceGoalTerminalReason.FAILED
            } else {
                terminalReason
            }
        )

        private fun failed(reason: String): DeviceGoalEvidence = complete(false, reason)

        fun failureEvidence(reason: String): DeviceGoalEvidence = DeviceGoalEvidence(
            goal = goal,
            verified = false,
            observationRevision = if (this::frame.isInitialized) frame.observation.revision else 0,
            actionEvidence = evidence.toList(),
            summary = reason.take(MAX_EVIDENCE_DETAIL_CHARS),
            perceptionSource = if (this::frame.isInitialized && frame.observation.source == AgentObservationSource.BRIDGE) {
                AgentPerceptionSource.BRIDGE
            } else {
                AgentPerceptionSource.ADB
            },
            primitiveActionCount = primitiveActions,
            terminalReason = if (terminalReason == DeviceGoalTerminalReason.NONE) {
                DeviceGoalTerminalReason.FAILED
            } else {
                terminalReason
            }
        )
    }
}

private fun UiNodeSnapshot.semanticText(): String = listOf(text, contentDescription, role)
    .filter(String::isNotBlank)
    .joinToString(" ")

private fun parseSetting(raw: String?): CuratedDeviceSetting? = when (raw?.trim()?.lowercase()) {
    "wifi", "wi-fi", "无线网络" -> CuratedDeviceSetting.WIFI
    "bluetooth", "蓝牙" -> CuratedDeviceSetting.BLUETOOTH
    "brightness", "亮度" -> CuratedDeviceSetting.BRIGHTNESS
    "rotation", "rotation_auto", "自动旋转" -> CuratedDeviceSetting.ROTATION_AUTO
    else -> null
}

private fun parseSettingValue(
    setting: CuratedDeviceSetting,
    raw: String?
): CuratedSettingValue? = when (setting) {
    CuratedDeviceSetting.BRIGHTNESS -> raw?.trim()?.removeSuffix("%")?.toIntOrNull()
        ?.takeIf { it in 0..100 }
        ?.let(CuratedSettingValue::Level)
    else -> when (raw?.trim()?.lowercase()) {
        "true", "on", "enabled", "1", "开启", "打开", "启用" -> CuratedSettingValue.Toggle(true)
        "false", "off", "disabled", "0", "关闭", "停用" -> CuratedSettingValue.Toggle(false)
        else -> null
    }
}

private fun Throwable.safeDeviceEngineMessage(): String = when (this) {
    is AgentException -> message.orEmpty().take(MAX_EVIDENCE_DETAIL_CHARS)
    else -> "The semantic device operation could not be completed"
}

private val SEARCH_TERMS = listOf("搜索", "search")
private val SEND_TERMS = listOf("发送", "send")
private const val SEARCH_FIELD_TIMEOUT_MS = 4_000L
private const val SEARCH_QUERY_TIMEOUT_MS = 4_000L
private const val SEARCH_RESULT_TIMEOUT_MS = 6_000L
private const val CHAT_TRANSITION_TIMEOUT_MS = 6_000L
private const val UI_PREDICATE_POLL_INTERVAL_MS = 250L
private const val NANOS_PER_MILLISECOND = 1_000_000L
/** Includes a bounded Bridge attempt plus a complete ADB hierarchy fallback. */
private const val POST_ACTION_OBSERVATION_TIMEOUT_MS = 15_000L
private const val MAX_EVIDENCE_DETAIL_CHARS = 300
