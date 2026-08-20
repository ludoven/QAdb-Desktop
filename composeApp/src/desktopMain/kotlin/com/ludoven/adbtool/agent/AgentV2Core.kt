package com.ludoven.adbtool.agent

import java.security.MessageDigest

enum class AgentEngineVersion { V1, V2, SCREENSHOT }

enum class AgentBrainPhase { INITIAL, STEP, EXCEPTION, FINAL }

enum class AgentEvidenceKind { DEVICE_STATUS, APP_CATALOG, SCREEN, SCREENSHOT }

enum class AgentPerceptionSource { BRIDGE, ADB, VISION }

enum class SemanticScreenKind { UNKNOWN, LAUNCHER, APP_LIST, SEARCH, CHAT_LIST, CHAT, SETTINGS, DIALOG }

data class GroundedElement(
    val candidateId: String,
    val revision: Long,
    val role: String,
    val label: String,
    val enabled: Boolean,
    val clickable: Boolean,
    val editable: Boolean,
    val selected: Boolean,
    val checked: Boolean,
    val screenOrder: Int,
    val resourceId: String? = null,
    val parentResourceId: String? = null,
    val parentRole: String? = null,
    val boundsPermille: UiBounds
)

data class SemanticContentBlock(
    val contentId: String,
    val revision: Long,
    val text: String,
    val role: String,
    val screenOrder: Int,
    val boundsPermille: UiBounds
)

data class SemanticScreen(
    val revision: Long,
    val appPackage: String?,
    val kind: SemanticScreenKind,
    val signature: String,
    val candidates: List<GroundedElement>,
    val source: AgentPerceptionSource,
    val contentBlocks: List<SemanticContentBlock> = emptyList()
)

/** Exact PackageManager-backed application choice exposed to V2 Brain. */
data class AgentAppReference(
    val appRef: String,
    val label: String
)

/** Converts raw device observations into a bounded, non-coordinate model surface. */
object SemanticScreenMapper {
    fun frameFrom(observation: AgentObservation, maxCandidates: Int = DEFAULT_MAX_CANDIDATES): GroundedScreenFrame {
        val screen = from(observation, maxCandidates)
        val eligibleNodes = observation.uiNodes.filter { it.enabled && !it.password }
        val nodesByCandidateId = eligibleNodes.asSequence()
            .filter { it.enabled && !it.password }
            .associateBy { semanticCandidateId(observation.revision, it) }
        val tapTargetsByCandidateId = eligibleNodes.associate { node ->
            semanticCandidateId(observation.revision, node) to node.resolveSemanticTapTarget(eligibleNodes)
        }.filterValues { it != null }.mapValues { (_, node) -> requireNotNull(node) }
        return GroundedScreenFrame(screen, observation, nodesByCandidateId, tapTargetsByCandidateId)
    }

    fun from(observation: AgentObservation, maxCandidates: Int = DEFAULT_MAX_CANDIDATES): SemanticScreen {
        require(maxCandidates in 1..ABSOLUTE_MAX_CANDIDATES)
        val appPackage = observation.currentActivity.foregroundComponentPackage()
            ?: observation.uiNodes.map(UiNodeSnapshot::packageName).filter(String::isNotBlank).toSet().singleOrNull()
        val candidates = observation.uiNodes.withIndex().asSequence()
            .filter { it.value.enabled && !it.value.password }
            .filter { (_, node) -> node.clickable || node.editable || node.role.isNotBlank() || node.text.isNotBlank() || node.contentDescription.isNotBlank() }
            .map { indexed ->
                SemanticCandidateSource(
                    index = indexed.index,
                    node = indexed.value,
                    hasTapTarget = indexed.value.resolveSemanticTapTarget(observation.uiNodes) != null
                )
            }
            // Preserve the bounded prompt budget for elements the model can actually operate.
            // Large Android trees otherwise fill the first page with non-interactive containers.
            .sortedWith(
                compareByDescending<SemanticCandidateSource> { it.node.editable }
                    .thenByDescending { it.hasTapTarget }
                    .thenByDescending { it.node.text.isNotBlank() || it.node.contentDescription.isNotBlank() }
                    .thenBy { it.index }
            )
            .map { source -> source.node.toGroundedElement(observation, source.index, observation.uiNodes) }
            .distinctBy(GroundedElement::candidateId)
            .take(maxCandidates)
            .toList()
        val contentBlocks = contentBlocksFrom(observation)
        return SemanticScreen(
            revision = observation.revision,
            appPackage = appPackage,
            kind = inferScreenKind(candidates, appPackage),
            signature = semanticSignature(observation, candidates),
            candidates = candidates,
            source = when (observation.source) {
                AgentObservationSource.BRIDGE -> AgentPerceptionSource.BRIDGE
                AgentObservationSource.ADB -> AgentPerceptionSource.ADB
            },
            contentBlocks = contentBlocks
        )
    }

    private fun contentBlocksFrom(observation: AgentObservation): List<SemanticContentBlock> {
        var remainingChars = MAX_CONTENT_BLOCK_TOTAL_CHARS
        val seen = linkedSetOf<String>()
        return observation.uiNodes.withIndex().asSequence()
            .filter { (_, node) -> !node.password && !node.editable }
            .sortedWith(
                compareBy<IndexedValue<UiNodeSnapshot>> { it.value.bounds.top }
                    .thenBy { it.value.bounds.left }
                    .thenBy { it.index }
            )
            .mapNotNull { indexed ->
                val node = indexed.value
                val rawText = node.text.ifBlank { node.contentDescription }.trim()
                if (rawText.isBlank() || remainingChars <= 0) return@mapNotNull null
                val text = rawText.take(minOf(MAX_CONTENT_BLOCK_CHARS, remainingChars))
                val role = node.role.ifBlank { node.className.substringAfterLast('.') }
                    .take(MAX_CANDIDATE_ROLE_CHARS)
                val dedupeKey = "${text.lowercase().replace(CONTENT_WHITESPACE, " ")}|${role.lowercase()}"
                if (!seen.add(dedupeKey)) return@mapNotNull null
                val identity = listOf(
                    text,
                    role,
                    node.resourceId,
                    node.bounds.left,
                    node.bounds.top,
                    node.bounds.right,
                    node.bounds.bottom
                ).joinToString("|")
                remainingChars -= text.length
                SemanticContentBlock(
                    contentId = "t-${(observation.revision.toString() + "|" + identity).v2Sha256().take(16)}",
                    revision = observation.revision,
                    text = text,
                    role = role,
                    screenOrder = indexed.index,
                    boundsPermille = UiBounds(
                        left = node.bounds.left.toPermille(observation.screenWidth),
                        top = node.bounds.top.toPermille(observation.screenHeight),
                        right = node.bounds.right.toPermille(observation.screenWidth),
                        bottom = node.bounds.bottom.toPermille(observation.screenHeight)
                    )
                )
            }
            .take(MAX_CONTENT_BLOCKS)
            .toList()
    }

    private fun UiNodeSnapshot.toGroundedElement(
        observation: AgentObservation,
        screenOrder: Int,
        allNodes: List<UiNodeSnapshot>
    ): GroundedElement {
        val displayLabel = text.ifBlank { contentDescription }.trim().take(MAX_CANDIDATE_LABEL_CHARS)
        return GroundedElement(
            candidateId = semanticCandidateId(observation.revision, this),
            revision = observation.revision,
            role = role.ifBlank { className.substringAfterLast('.') }.take(MAX_CANDIDATE_ROLE_CHARS),
            label = displayLabel,
            enabled = enabled,
            clickable = resolveSemanticTapTarget(allNodes) != null,
            editable = editable,
            selected = selected,
            checked = checked,
            screenOrder = screenOrder,
            resourceId = resourceId.takeIf(String::isNotBlank)?.take(MAX_CANDIDATE_RESOURCE_CHARS),
            parentResourceId = ancestorResourceIds.lastOrNull()?.take(MAX_CANDIDATE_RESOURCE_CHARS),
            parentRole = ancestorRoles.lastOrNull()?.take(MAX_CANDIDATE_ROLE_CHARS),
            boundsPermille = UiBounds(
                left = bounds.left.toPermille(observation.screenWidth),
                top = bounds.top.toPermille(observation.screenHeight),
                right = bounds.right.toPermille(observation.screenWidth),
                bottom = bounds.bottom.toPermille(observation.screenHeight)
            )
        )
    }

    private fun inferScreenKind(
        candidates: List<GroundedElement>,
        appPackage: String?
    ): SemanticScreenKind {
        val searchable = candidates.any { it.editable && it.label.containsAny("搜索", "search") }
        val sendable = candidates.any { it.clickable && it.label.containsAny("发送", "send") }
        val hasEditable = candidates.any(GroundedElement::editable)
        val settings = candidates.count { it.role.contains("switch", true) || it.label.containsAny("设置", "settings") } >= 2
        return when {
            appPackage.isLikelyLauncherPackage() -> SemanticScreenKind.LAUNCHER
            sendable && hasEditable -> SemanticScreenKind.CHAT
            searchable -> SemanticScreenKind.SEARCH
            settings -> SemanticScreenKind.SETTINGS
            else -> SemanticScreenKind.UNKNOWN
        }
    }

    private fun semanticSignature(observation: AgentObservation, candidates: List<GroundedElement>): String =
        (listOf(observation.currentActivity, observation.screenWidth, observation.screenHeight) +
            candidates.map { "${it.role}:${it.label}:${it.clickable}:${it.editable}" })
            .joinToString("|")
            .v2Sha256()
}

private data class SemanticCandidateSource(
    val index: Int,
    val node: UiNodeSnapshot,
    val hasTapTarget: Boolean
)

private fun Int.toPermille(total: Int): Int = if (total <= 0) 0 else ((toLong() * 1_000L) / total)
    .toInt()
    .coerceIn(0, 1_000)

private val CONTENT_WHITESPACE = Regex("\\s+")

/** Runtime-only grounding table. Only [screen] is sent to the model. */
class GroundedScreenFrame internal constructor(
    val screen: SemanticScreen,
    val observation: AgentObservation,
    internal val nodesByCandidateId: Map<String, UiNodeSnapshot>,
    private val tapTargetsByCandidateId: Map<String, UiNodeSnapshot>
) {
    fun resolveCandidate(candidateId: String, revision: Long): UiNodeSnapshot? =
        nodesByCandidateId[candidateId].takeIf { revision == screen.revision }

    fun resolveTapTarget(candidateId: String, revision: Long): UiNodeSnapshot? =
        tapTargetsByCandidateId[candidateId].takeIf { revision == screen.revision }
}

/**
 * Android frequently exposes a visible TextView as a non-clickable child of a clickable row.
 * Keep the model bound to the labelled child candidate, but execute only against one exact,
 * enabled semantic ancestor. Geometry is used only to disambiguate nodes already declared in
 * the hierarchy; it never creates a free-form coordinate action.
 */
private fun UiNodeSnapshot.resolveSemanticTapTarget(allNodes: List<UiNodeSnapshot>): UiNodeSnapshot? {
    if (!enabled || password || editable) return null
    if (clickable) return this
    val containers = allNodes.asSequence()
        .filter { candidate ->
            candidate !== this &&
                candidate.enabled &&
                candidate.clickable &&
                !candidate.password &&
                !candidate.editable &&
                candidate.packageName == packageName &&
                candidate.bounds.contains(bounds) &&
                candidate.isDeclaredAncestorOf(this)
        }
        .sortedBy { it.bounds.area() }
        .toList()
    val smallestArea = containers.firstOrNull()?.bounds?.area() ?: return null
    return containers.takeWhile { it.bounds.area() == smallestArea }.singleOrNull()
}

private fun UiNodeSnapshot.isDeclaredAncestorOf(child: UiNodeSnapshot): Boolean =
    (resourceId.isNotBlank() && resourceId in child.ancestorResourceIds) ||
        (role.isNotBlank() && role in child.ancestorRoles)

private fun UiBounds.contains(other: UiBounds): Boolean =
    left <= other.left && top <= other.top && right >= other.right && bottom >= other.bottom

private fun UiBounds.area(): Long =
    (right - left).coerceAtLeast(0).toLong() * (bottom - top).coerceAtLeast(0).toLong()

internal fun semanticCandidateId(revision: Long, node: UiNodeSnapshot): String {
    val identity = listOf(
        revision.toString(), node.resourceId, node.role, node.className, node.packageName,
        node.bounds.left.toString(), node.bounds.top.toString(), node.bounds.right.toString(), node.bounds.bottom.toString()
    ).joinToString("|")
    return "c-${identity.v2Sha256().take(16)}"
}

sealed interface AgentBrainDecision {
    data class Answer(val text: String) : AgentBrainDecision
    data class RequestEvidence(
        val kinds: Set<AgentEvidenceKind>,
        val appQueries: List<String> = emptyList(),
        val listAllApps: Boolean = false
    ) : AgentBrainDecision {
        init {
            val requestsCatalog = AgentEvidenceKind.APP_CATALOG in kinds
            require(appQueries.size <= MAX_V2_APP_EVIDENCE_QUERIES)
            require(appQueries.all { it.isNotBlank() })
            require(!requestsCatalog || appQueries.isNotEmpty() || listAllApps) {
                "APP_CATALOG evidence requires app queries or an explicit full-list request"
            }
            require(requestsCatalog || (appQueries.isEmpty() && !listAllApps)) {
                "Application evidence scope is only valid for APP_CATALOG"
            }
            require(!(appQueries.isNotEmpty() && listAllApps)) {
                "APP_CATALOG cannot request both targeted queries and a full listing"
            }
        }
    }
    data class BeginOperation(
        val contract: SemanticGoal,
        val action: V2ActionIntent
    ) : AgentBrainDecision
    data class BeginPlan(val plan: AgentOperationPlan) : AgentBrainDecision
    data class ExecuteAction(val action: V2ActionIntent) : AgentBrainDecision
    data class Complete(
        val summary: String,
        val evidenceRefs: List<String> = emptyList(),
        val visualRevision: Long? = null
    ) : AgentBrainDecision {
        init {
            require(summary.isNotBlank()) { "A completion summary cannot be blank" }
            require(evidenceRefs.size <= MAX_READ_EVIDENCE_REFS) { "Too many completion evidence references" }
            require(evidenceRefs.all(String::isNotBlank)) { "Completion evidence references cannot be blank" }
            require(visualRevision == null || visualRevision >= 0) { "Visual revision cannot be negative" }
        }
    }
    /** Legacy compatibility for saved fake providers. Production prompts no longer request this decision. */
    data class ExecuteGoal(val goal: SemanticGoal) : AgentBrainDecision
    data class Clarify(val question: String) : AgentBrainDecision
    data class Stop(
        val reason: String,
        val code: AgentBrainStopCode = AgentBrainStopCode.OTHER
    ) : AgentBrainDecision
}

enum class AgentBrainStopCode {
    APP_NOT_FOUND,
    DEVICE_UNAVAILABLE,
    EVIDENCE_UNAVAILABLE,
    SAFETY_BLOCKED,
    UNSUPPORTED,
    OTHER
}

data class AgentConversationTurn(
    val role: AgentMessageRole,
    val text: String
) {
    init {
        require(role != AgentMessageRole.SYSTEM) { "System messages cannot enter conversation history" }
        require(text.isNotBlank()) { "Conversation history cannot contain blank messages" }
    }
}

data class AgentBrainRequest(
    val task: String,
    val phase: AgentBrainPhase,
    val conversationContext: List<AgentConversationTurn> = emptyList(),
    val memoryContext: String = "",
    val trustedEvidence: AgentTrustedEvidence? = null,
    val screen: SemanticScreen? = null,
    val executionEvidence: DeviceGoalEvidence? = null,
    val operationContract: SemanticGoal? = null,
    val skill: AgentSkillSnapshot? = null,
    val appKnowledge: AgentAppKnowledgeSnapshot? = null,
    val completedOperations: List<AgentCompletedOperation> = emptyList(),
    val actionHistory: List<V2ActionEvidence> = emptyList(),
    val failure: String? = null,
    val availableApps: List<AgentAppReference> = emptyList(),
    /** Ephemeral visual fallback input. It must never be persisted in task logs. */
    val screenshotPng: ByteArray? = null,
    val screenshotMimeType: String = "image/png",
    val remainingModelCalls: Int = V2_DEFAULT_MODEL_CALLS,
    val remainingVisionCalls: Int = V2_DEFAULT_VISION_CALLS,
    val remainingDeviceActions: Int = V2_DEFAULT_DEVICE_ACTIONS
) {
    override fun equals(other: Any?): Boolean = other is AgentBrainRequest &&
        task == other.task && phase == other.phase && conversationContext == other.conversationContext && memoryContext == other.memoryContext && trustedEvidence == other.trustedEvidence &&
        screen == other.screen && executionEvidence == other.executionEvidence &&
        operationContract == other.operationContract && skill == other.skill && appKnowledge == other.appKnowledge &&
        completedOperations == other.completedOperations &&
        actionHistory == other.actionHistory && failure == other.failure &&
        availableApps == other.availableApps &&
        screenshotPng.contentEqualsNullable(other.screenshotPng) && screenshotMimeType == other.screenshotMimeType &&
        remainingModelCalls == other.remainingModelCalls && remainingVisionCalls == other.remainingVisionCalls &&
        remainingDeviceActions == other.remainingDeviceActions

    override fun hashCode(): Int = arrayOf(
        task, phase, conversationContext, memoryContext, trustedEvidence, screen, executionEvidence, operationContract, skill, appKnowledge,
        completedOperations, actionHistory, failure, availableApps,
        screenshotPng?.contentHashCode(), screenshotMimeType, remainingModelCalls,
        remainingVisionCalls, remainingDeviceActions
    ).contentHashCode()
}

enum class V2TextSource { TARGET, VALUE, LITERAL }
enum class V2SwipeDirection { UP, DOWN, LEFT, RIGHT }
enum class V2VisualActionPurpose { NAVIGATION, SEND }
enum class V2FinalNavigation { NONE, BACK, HOME }

sealed interface V2ActionIntent {
    data class OpenApp(val appRef: String) : V2ActionIntent
    data class TapCandidate(
        val candidateId: String,
        val revision: Long,
        /** Visual evidence may label an existing semantic node, but never supplies tap coordinates. */
        val visualLabel: String? = null,
        val visualRevision: Long? = null
    ) : V2ActionIntent {
        init {
            require((visualLabel == null) == (visualRevision == null)) {
                "Visual candidate label and revision must be supplied together"
            }
            require(visualLabel == null || visualLabel.isNotBlank()) {
                "Visual candidate label cannot be blank"
            }
        }
    }
    data class InputText(
        val candidateId: String,
        val revision: Long,
        val source: V2TextSource,
        val literalText: String? = null,
        val replaceExisting: Boolean = false
    ) : V2ActionIntent
    data class Swipe(
        val direction: V2SwipeDirection,
        val distancePercent: Int = 60,
        val durationMs: Int = 350
    ) : V2ActionIntent
    data class Key(val key: AgentKey) : V2ActionIntent
    data class Wait(val durationMs: Int) : V2ActionIntent
    data class SystemCommand(val goal: SemanticGoal) : V2ActionIntent
    data class VisualTap(
        val grounding: VisualGrounding,
        val purpose: V2VisualActionPurpose = V2VisualActionPurpose.NAVIGATION
    ) : V2ActionIntent
}

data class V2ActionEvidence(
    val actionName: String,
    val beforeRevision: Long,
    val afterRevision: Long,
    val progressed: Boolean,
    val executed: Boolean,
    val detail: String = "",
    /** Ephemeral candidate identity only; never a contact name, input value, selector, or coordinate. */
    val targetReference: String? = null,
    val targetRole: String? = null,
    val inputSource: V2TextSource? = null,
    val operationKind: AgentOperationKind? = null,
    val effect: AgentActionEffect? = null
)

data class AgentBrainResult(
    val decision: AgentBrainDecision,
    val usage: AgentUsage = AgentUsage(),
    val usedVision: Boolean = false,
    val providerSnapshot: AgentModelProviderSnapshot? = null,
    val billing: AgentModelBilling? = null
)

interface AgentBrainGateway {
    suspend fun decide(request: AgentBrainRequest): AgentBrainResult
}

enum class SemanticGoalKind {
    OPEN_APP,
    READ_APP_CONTENT,
    FIND_AND_CLICK,
    INPUT_TEXT,
    OPEN_CHAT,
    SEND_MESSAGE,
    SYSTEM_SETTING,
    UNINSTALL_APP,
    CLEAR_APP_DATA,
    DELETE_CONTENT
}

enum class ContentReadMode { PAGE_SUMMARY, FIRST_VISIBLE_ITEM, VISIBLE_ITEM_MATCH, FIELD_VALUE }

data class ReadContentSpec(
    val surface: String,
    val mode: ContentReadMode,
    val query: String? = null
) {
    init {
        require(surface.isNotBlank()) { "A content read requires a target surface" }
        val needsQuery = mode in setOf(ContentReadMode.VISIBLE_ITEM_MATCH, ContentReadMode.FIELD_VALUE)
        require(!needsQuery || !query.isNullOrBlank()) { "$mode requires a non-blank query" }
        require(needsQuery || query == null) { "$mode does not accept a query" }
    }
}

enum class AgentSemanticActivityStatus { RUNNING, SUCCEEDED, FAILED }

data class AgentSemanticActivity(
    val goalKind: SemanticGoalKind,
    val status: AgentSemanticActivityStatus
)

data class AgentV2RunMetrics(
    val semanticCommands: Int = 0,
    val primitiveActions: Int = 0,
    val visualGroundings: Int = 0,
    val localRecoveries: Int = 0,
    val groundingFailures: Int = 0
)

data class SemanticGoal(
    val kind: SemanticGoalKind,
    /** Selected only from live [AgentBrainRequest.availableApps], by BRAIN or a bounded local read bootstrap. */
    val appRef: String? = null,
    /** Exact package resolved locally from [appRef] immediately before execution. */
    val app: String? = null,
    val target: String? = null,
    val value: String? = null,
    val successDescription: String,
    val finalNavigation: V2FinalNavigation = V2FinalNavigation.NONE,
    val visualGrounding: VisualGrounding? = null,
    val readContentSpec: ReadContentSpec? = null
) {
    init {
        require(successDescription.isNotBlank()) { "A semantic goal requires a success condition" }
        if (kind == SemanticGoalKind.SEND_MESSAGE) {
            require(!target.isNullOrBlank()) { "A message goal requires a recipient" }
            require(!value.isNullOrBlank()) { "A message goal requires non-empty content" }
        }
        require((kind == SemanticGoalKind.READ_APP_CONTENT) == (readContentSpec != null)) {
            "READ_APP_CONTENT requires exactly one read-content specification"
        }
    }
}

/**
 * Bounded local bootstrap for high-confidence app-content reads. It only chooses an application
 * from the live catalog and freezes a read-only contract; it never operates the device itself.
 * Routes are data so additional applications can be added without changing the V2 engine.
 */
data class LocalAppContentReadBootstrap(
    val appQueries: List<String>,
    val readSpec: ReadContentSpec,
    val navigationWaypoints: List<String> = emptyList(),
    val packageAliases: Map<String, String> = emptyMap()
) {
    init {
        require(appQueries.isNotEmpty() && appQueries.all(String::isNotBlank))
        require(navigationWaypoints.all(String::isNotBlank))
        require(packageAliases.all { (packageName, label) -> packageName.isNotBlank() && label.isNotBlank() })
    }

    fun beginOperation(apps: List<AgentAppReference>): AgentBrainDecision.BeginOperation? {
        val normalizedQueries = appQueries.map { it.trim().lowercase() }.toSet()
        val exact = apps.filter { it.label.trim().lowercase() in normalizedQueries }
        val selected = exact.singleOrNull() ?: apps.singleOrNull() ?: return null
        val goal = SemanticGoal(
            kind = SemanticGoalKind.READ_APP_CONTENT,
            appRef = selected.appRef,
            successDescription = "the requested application content is bound to fresh current-revision evidence",
            readContentSpec = readSpec
        )
        return AgentBrainDecision.BeginOperation(goal, V2ActionIntent.OpenApp(selected.appRef))
    }

    /**
     * Uses only exact, current-revision semantic labels. Ambiguous or missing waypoints deliberately
     * fall back to the Brain; this adapter never invents coordinates or bypasses read policy.
     */
    fun navigationAction(screen: SemanticScreen, contract: SemanticGoal): V2ActionIntent.TapCandidate? {
        if (
            contract.kind != SemanticGoalKind.READ_APP_CONTENT ||
            contract.readContentSpec != readSpec ||
            contract.app.isNullOrBlank() ||
            screen.appPackage != contract.app
        ) return null
        val surfaceIsVisible = screen.candidates.any { candidate ->
            !candidate.clickable &&
                candidate.boundsPermille.top <= SURFACE_TITLE_MAX_TOP_PERMILLE &&
                candidate.label.trim().equals(readSpec.surface, ignoreCase = true)
        }
        if (surfaceIsVisible) return null
        val target = navigationWaypoints.asReversed().firstNotNullOfOrNull { waypoint ->
            screen.candidates.filter { candidate ->
                candidate.clickable && candidate.label.trim().equals(waypoint, ignoreCase = true)
            }.singleOrNull()
        } ?: return null
        return V2ActionIntent.TapCandidate(target.candidateId, screen.revision)
    }

    companion object {
        fun resolve(task: String, intent: AgentTaskIntent): LocalAppContentReadBootstrap? {
            if (intent.kind != AgentTaskIntentKind.APP_CONTENT_READ) return null
            val normalized = task.lowercase().replace(Regex("\\s+"), "")
            val route = LOCAL_APP_CONTENT_ROUTES.firstOrNull { it.surfacePattern.containsMatchIn(normalized) }
                ?: return null
            val mode = when {
                FIRST_VISIBLE_READ_PATTERN.containsMatchIn(normalized) -> ContentReadMode.FIRST_VISIBLE_ITEM
                PAGE_SUMMARY_READ_PATTERN.containsMatchIn(normalized) -> ContentReadMode.PAGE_SUMMARY
                else -> return null
            }
            return LocalAppContentReadBootstrap(
                appQueries = route.appQueries,
                readSpec = ReadContentSpec(route.surface, mode),
                navigationWaypoints = route.navigationWaypoints,
                packageAliases = route.packageAliases
            )
        }
    }
}

private data class LocalAppContentRoute(
    val surfacePattern: Regex,
    val surface: String,
    val appQueries: List<String>,
    val navigationWaypoints: List<String>,
    val packageAliases: Map<String, String> = emptyMap()
)

private val LOCAL_APP_CONTENT_ROUTES = listOf(
    LocalAppContentRoute(
        surfacePattern = Regex("朋友圈"),
        surface = "朋友圈",
        appQueries = listOf("微信"),
        navigationWaypoints = listOf("发现", "朋友圈"),
        packageAliases = mapOf("com.tencent.mm" to "微信")
    )
)
private val FIRST_VISIBLE_READ_PATTERN = Regex("第一|首条|最新")
private val PAGE_SUMMARY_READ_PATTERN = Regex("概括|总结|摘要|页面内容|有什么内容")
private const val SURFACE_TITLE_MAX_TOP_PERMILLE = 200

data class AgentPlannedOperation(
    val contract: SemanticGoal,
    val initialAction: V2ActionIntent
) {
    init {
        require(contract.kind in V2_PLANNABLE_GOAL_KINDS) {
            "Only low-risk navigation goals are supported in a multi-operation plan"
        }
        val openApp = requireNotNull(initialAction as? V2ActionIntent.OpenApp) {
            "Every planned operation must start from one frozen application reference"
        }
        require(openApp.appRef == contract.appRef && !openApp.appRef.isBlank()) {
            "The planned initial action must match the frozen application reference"
        }
    }
}

data class AgentOperationPlan(val operations: List<AgentPlannedOperation>) {
    init {
        require(operations.size in MIN_V2_PLAN_OPERATIONS..MAX_V2_PLAN_OPERATIONS) {
            "A multi-operation plan must contain $MIN_V2_PLAN_OPERATIONS..$MAX_V2_PLAN_OPERATIONS operations"
        }
        require(operations.dropLast(1).none { it.contract.finalNavigation != V2FinalNavigation.NONE }) {
            "Only the final planned operation may request final navigation"
        }
        require(
            operations.map { it.contract.copy(app = null, finalNavigation = V2FinalNavigation.NONE) }.distinct().size ==
                operations.size
        ) { "A multi-operation plan cannot repeat the same operation" }
    }
}

data class AgentCompletedOperation(
    val kind: SemanticGoalKind,
    val appRef: String?,
    val successDescription: String
)

internal fun SemanticGoal.completedOperation(): AgentCompletedOperation = AgentCompletedOperation(
    kind = kind,
    appRef = appRef,
    successDescription = successDescription
)

internal val V2_PLANNABLE_GOAL_KINDS = setOf(
    SemanticGoalKind.OPEN_APP,
    SemanticGoalKind.OPEN_CHAT
)

internal const val MIN_V2_PLAN_OPERATIONS = 2
internal const val MAX_V2_PLAN_OPERATIONS = 3

internal fun buildAgentAppReferences(installedApps: List<InstalledAgentApp>): List<AgentAppReference> =
    installedApps.asSequence()
        .filter { it.enabled && it.launchable && it.packageName.isNotBlank() }
        .distinctBy(InstalledAgentApp::packageName)
        .take(MAX_BRAIN_APP_REFERENCES)
        .map { app -> AgentAppReference(appRef = app.packageName, label = app.label.ifBlank { app.packageName }) }
        .toList()

/** Exact lookup only: no user-text parsing, aliases, fuzzy matching, or silent target replacement. */
internal fun resolveSemanticGoalAppReference(
    goal: SemanticGoal,
    offeredApps: List<AgentAppReference>,
    freshInstalledApps: List<InstalledAgentApp>
): Result<SemanticGoal> = runCatching {
    if (goal.kind !in APP_SCOPED_SEMANTIC_GOALS) return@runCatching goal
    val selectedRef = goal.appRef?.trim().orEmpty()
    require(selectedRef.isNotEmpty()) { "BRAIN did not select an app_ref" }
    require(offeredApps.any { it.appRef == selectedRef }) { "BRAIN selected an unavailable app_ref" }
    val installed = freshInstalledApps.singleOrNull {
        it.enabled && it.launchable && it.packageName == selectedRef
    }
    requireNotNull(installed) { "The selected app_ref is stale or no longer launchable" }
    goal.copy(app = installed.packageName)
}

private val APP_SCOPED_SEMANTIC_GOALS = setOf(
    SemanticGoalKind.OPEN_APP,
    SemanticGoalKind.READ_APP_CONTENT,
    SemanticGoalKind.OPEN_CHAT,
    SemanticGoalKind.SEND_MESSAGE,
    SemanticGoalKind.UNINSTALL_APP,
    SemanticGoalKind.CLEAR_APP_DATA
)

internal fun SemanticGoalKind.requiresAppReference(): Boolean = this in APP_SCOPED_SEMANTIC_GOALS

private const val MAX_BRAIN_APP_REFERENCES = 300

/** One-use visual location bound to the screenshot revision that produced it. */
data class VisualGrounding(
    val revision: Long,
    val leftPermille: Int,
    val topPermille: Int,
    val rightPermille: Int,
    val bottomPermille: Int
) {
    init {
        require(leftPermille in 0..999 && topPermille in 0..999)
        require(rightPermille in 1..1000 && bottomPermille in 1..1000)
        require(leftPermille < rightPermille && topPermille < bottomPermille)
    }
}

enum class DeviceActionProgress { PROGRESSED, NO_CHANGE, AMBIGUOUS, FAILED }

enum class DeviceGoalTerminalReason { NONE, FAILED, USER_DENIED, POLICY_BLOCKED }

data class DeviceActionEvidence(
    val actionName: String,
    val progress: DeviceActionProgress,
    val observationRevision: Long,
    val detail: String = "",
    val executedActionCount: Int = 0
)

data class DeviceGoalEvidence(
    val goal: SemanticGoal,
    val verified: Boolean,
    val observationRevision: Long,
    val actionEvidence: List<DeviceActionEvidence>,
    val summary: String,
    val perceptionSource: AgentPerceptionSource,
    val primitiveActionCount: Int,
    val terminalReason: DeviceGoalTerminalReason = DeviceGoalTerminalReason.NONE
) {
    init {
        require(primitiveActionCount >= 0)
        require(actionEvidence.none { it.executedActionCount < 0 })
    }
}

data class V2ExecutionBudget(
    val maxModelCalls: Int = V2_DEFAULT_MODEL_CALLS,
    val maxVisionCalls: Int = V2_DEFAULT_VISION_CALLS,
    val maxReplans: Int = 1,
    val maxDeviceActions: Int = V2_DEFAULT_DEVICE_ACTIONS,
    val absoluteMaxDeviceActions: Int = V2_ABSOLUTE_MAX_DEVICE_ACTIONS,
    val operationTimeoutMs: Long = 60_000,
    val maxModelCallTimeoutMs: Long = 30_000,
    val maxModelWallClockMs: Long = 45_000,
    /** Controls call/token/vision/replan quotas. The per-call timeout is always enforced. */
    val enforceModelLimits: Boolean = true
) {
    init {
        require(maxModelCalls in 1..12)
        require(maxVisionCalls in 0..2)
        require(maxReplans in 0..1)
        require(maxDeviceActions in 1..absoluteMaxDeviceActions)
        require(absoluteMaxDeviceActions in 1..20)
        require(operationTimeoutMs in 1_000..120_000)
        require(maxModelCallTimeoutMs in 1_000..120_000)
        require(maxModelWallClockMs in maxModelCallTimeoutMs..120_000)
    }
}

interface DeviceEngine {
    suspend fun execute(
        deviceId: String,
        goal: SemanticGoal,
        budget: V2ExecutionBudget,
        confirmSensitiveAction: suspend (AgentStep) -> Boolean = { false }
    ): DeviceGoalEvidence
}

enum class CuratedDeviceSetting { WIFI, BLUETOOTH, BRIGHTNESS, ROTATION_AUTO }

sealed interface CuratedSettingValue {
    data class Toggle(val enabled: Boolean) : CuratedSettingValue
    data class Level(val value: Int) : CuratedSettingValue {
        init {
            require(value in 0..100) { "Setting level must be between 0 and 100" }
        }
    }
}

interface CuratedDeviceCommandGateway {
    suspend fun readSetting(deviceId: String, setting: CuratedDeviceSetting): CuratedSettingValue?
    suspend fun writeSetting(
        deviceId: String,
        setting: CuratedDeviceSetting,
        value: CuratedSettingValue
    ): AgentToolResult
}

private fun String.containsAny(vararg terms: String): Boolean = terms.any { contains(it, ignoreCase = true) }

private fun String?.isLikelyLauncherPackage(): Boolean {
    val value = this?.trim()?.lowercase().orEmpty()
    return value == "com.miui.home" || value.endsWith(".home") || "launcher" in value
}

private fun String.v2Sha256(): String = MessageDigest.getInstance("SHA-256")
    .digest(toByteArray(Charsets.UTF_8))
    .joinToString("") { "%02x".format(it) }

private const val DEFAULT_MAX_CANDIDATES = 48
private const val ABSOLUTE_MAX_CANDIDATES = 160
private const val MAX_CANDIDATE_LABEL_CHARS = 120
private const val MAX_CANDIDATE_ROLE_CHARS = 80
private const val MAX_CANDIDATE_RESOURCE_CHARS = 120
private const val MAX_CONTENT_BLOCKS = 64
private const val MAX_CONTENT_BLOCK_CHARS = 300
private const val MAX_CONTENT_BLOCK_TOTAL_CHARS = 8_000
internal const val MAX_READ_EVIDENCE_REFS = 16
private const val V2_DEFAULT_MODEL_CALLS = 12
private const val V2_DEFAULT_VISION_CALLS = 2
private const val V2_DEFAULT_DEVICE_ACTIONS = 12
private const val V2_ABSOLUTE_MAX_DEVICE_ACTIONS = 20
internal const val MAX_V2_APP_EVIDENCE_QUERIES = 8

private fun ByteArray?.contentEqualsNullable(other: ByteArray?): Boolean = when {
    this == null -> other == null
    other == null -> false
    else -> contentEquals(other)
}
