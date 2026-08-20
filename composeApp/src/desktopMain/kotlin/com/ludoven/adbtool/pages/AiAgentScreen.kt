package com.ludoven.adbtool.pages

import adbtool_desktop.composeapp.generated.resources.*
import adbtool_desktop.composeapp.generated.resources.Res
import adbtool_desktop.composeapp.generated.resources.agent_approve
import adbtool_desktop.composeapp.generated.resources.agent_action_clear_data
import adbtool_desktop.composeapp.generated.resources.agent_action_finish
import adbtool_desktop.composeapp.generated.resources.agent_action_force_stop
import adbtool_desktop.composeapp.generated.resources.agent_action_input
import adbtool_desktop.composeapp.generated.resources.agent_action_key
import adbtool_desktop.composeapp.generated.resources.agent_action_launch
import adbtool_desktop.composeapp.generated.resources.agent_action_observe
import adbtool_desktop.composeapp.generated.resources.agent_action_reboot
import adbtool_desktop.composeapp.generated.resources.agent_action_swipe
import adbtool_desktop.composeapp.generated.resources.agent_action_tap
import adbtool_desktop.composeapp.generated.resources.agent_action_tap_element
import adbtool_desktop.composeapp.generated.resources.agent_action_find_app
import adbtool_desktop.composeapp.generated.resources.agent_action_uninstall
import adbtool_desktop.composeapp.generated.resources.agent_action_wait
import adbtool_desktop.composeapp.generated.resources.agent_cancel_task
import adbtool_desktop.composeapp.generated.resources.agent_config_required
import adbtool_desktop.composeapp.generated.resources.agent_config_required_desc
import adbtool_desktop.composeapp.generated.resources.agent_connected
import adbtool_desktop.composeapp.generated.resources.agent_composer_shortcut
import adbtool_desktop.composeapp.generated.resources.agent_composer_waiting_device
import adbtool_desktop.composeapp.generated.resources.agent_composer_waiting_model
import adbtool_desktop.composeapp.generated.resources.agent_disconnected
import adbtool_desktop.composeapp.generated.resources.agent_input_hint
import adbtool_desktop.composeapp.generated.resources.agent_memory_consent_desc
import adbtool_desktop.composeapp.generated.resources.agent_memory_consent_title
import adbtool_desktop.composeapp.generated.resources.agent_memory_enable
import adbtool_desktop.composeapp.generated.resources.agent_memory_not_now
import adbtool_desktop.composeapp.generated.resources.agent_memory_off
import adbtool_desktop.composeapp.generated.resources.agent_memory_status
import adbtool_desktop.composeapp.generated.resources.agent_saved_memory
import adbtool_desktop.composeapp.generated.resources.agent_usage_status
import adbtool_desktop.composeapp.generated.resources.agent_new_task
import adbtool_desktop.composeapp.generated.resources.agent_no_device
import adbtool_desktop.composeapp.generated.resources.agent_no_device_desc
import adbtool_desktop.composeapp.generated.resources.agent_open_devices
import adbtool_desktop.composeapp.generated.resources.agent_open_settings
import adbtool_desktop.composeapp.generated.resources.agent_preview_desc
import adbtool_desktop.composeapp.generated.resources.agent_preview_title
import adbtool_desktop.composeapp.generated.resources.agent_preview_unavailable
import adbtool_desktop.composeapp.generated.resources.agent_prompt_camera
import adbtool_desktop.composeapp.generated.resources.agent_prompt_clear_cache
import adbtool_desktop.composeapp.generated.resources.agent_prompt_recognize
import adbtool_desktop.composeapp.generated.resources.agent_prompt_settings
import adbtool_desktop.composeapp.generated.resources.agent_prompt_wechat
import adbtool_desktop.composeapp.generated.resources.agent_quick_recognize
import adbtool_desktop.composeapp.generated.resources.agent_quick_settings
import adbtool_desktop.composeapp.generated.resources.agent_ready_desc
import adbtool_desktop.composeapp.generated.resources.agent_ready_title
import adbtool_desktop.composeapp.generated.resources.agent_reject
import adbtool_desktop.composeapp.generated.resources.agent_running
import adbtool_desktop.composeapp.generated.resources.agent_send
import adbtool_desktop.composeapp.generated.resources.agent_sensitive_desc
import adbtool_desktop.composeapp.generated.resources.agent_sensitive_reason
import adbtool_desktop.composeapp.generated.resources.agent_sensitive_title
import adbtool_desktop.composeapp.generated.resources.agent_text_mode
import adbtool_desktop.composeapp.generated.resources.agent_vision_mode
import adbtool_desktop.composeapp.generated.resources.ai_agent_subtitle
import adbtool_desktop.composeapp.generated.resources.ai_agent_title
import adbtool_desktop.composeapp.generated.resources.agent_device_placeholder
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import com.ludoven.adbtool.QadbColors
import com.ludoven.adbtool.UiTokens
import com.ludoven.adbtool.agent.AgentAction
import com.ludoven.adbtool.agent.AgentFailureAction
import com.ludoven.adbtool.agent.AgentFailureCategory
import com.ludoven.adbtool.agent.AgentFailureCode
import com.ludoven.adbtool.agent.AgentFailure
import com.ludoven.adbtool.agent.AgentFailureStage
import com.ludoven.adbtool.agent.AgentFailureSubsystem
import com.ludoven.adbtool.agent.AgentFeatureRuntime
import com.ludoven.adbtool.agent.AgentMessage
import com.ludoven.adbtool.agent.AgentMessageRole
import com.ludoven.adbtool.agent.AgentObservationMode
import com.ludoven.adbtool.agent.AgentPublicActivityItem
import com.ludoven.adbtool.agent.AgentPublicActivityState
import com.ludoven.adbtool.agent.AgentPublicRunStatus
import com.ludoven.adbtool.agent.AgentPublicStage
import com.ludoven.adbtool.agent.AgentPublicToolKind
import com.ludoven.adbtool.agent.AgentPublicToolResult
import com.ludoven.adbtool.agent.AgentPublicToolSummary
import com.ludoven.adbtool.agent.AgentRunPresentation
import com.ludoven.adbtool.agent.AgentTaskExecutionGate
import com.ludoven.adbtool.agent.executionGate
import com.ludoven.adbtool.agent.AgentStep
import com.ludoven.adbtool.entity.DeviceCenterInfoData
import com.ludoven.adbtool.entity.DeviceInfoData
import com.ludoven.adbtool.util.l10n
import com.ludoven.adbtool.ui.icons.IconParkIcons
import com.ludoven.adbtool.ui.mac.AlertDialog
import com.ludoven.adbtool.ui.mac.Button
import com.ludoven.adbtool.ui.mac.Icon
import com.ludoven.adbtool.ui.mac.MaterialTheme
import com.ludoven.adbtool.ui.mac.OutlinedButton
import com.ludoven.adbtool.ui.mac.Surface
import com.ludoven.adbtool.ui.mac.Text
import com.ludoven.adbtool.ui.mac.TextButton
import com.ludoven.adbtool.viewmodel.AiAgentViewModel
import com.ludoven.adbtool.viewmodel.DevicesViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.skia.Image as SkiaImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect

internal enum class AgentScreenLayout {
    SINGLE_COLUMN,
    OVERLAY_DEVICE_PANEL,
    PERMANENT_DEVICE_PANEL
}

internal fun agentScreenLayout(widthDp: Float): AgentScreenLayout = when {
    widthDp >= 960f -> AgentScreenLayout.PERMANENT_DEVICE_PANEL
    widthDp >= 720f -> AgentScreenLayout.OVERLAY_DEVICE_PANEL
    else -> AgentScreenLayout.SINGLE_COLUMN
}

internal fun agentLatestItemScrollOffset(): Int = 0

internal fun shouldShowAgentGuide(messages: List<AgentMessage>): Boolean =
    messages.none { it.role != AgentMessageRole.SYSTEM }

@Composable
fun AiAgentScreen(
    viewModel: AiAgentViewModel,
    devicesViewModel: DevicesViewModel,
    onOpenDevices: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val taskState by viewModel.state.collectAsState()
    val modelConfig by viewModel.modelConfig.collectAsState()
    val apiKeyAvailable by viewModel.apiKeyAvailable.collectAsState()
    val configurationReady by viewModel.configurationReady.collectAsState()
    val selectedDevice by devicesViewModel.selectedDevice.collectAsState()
    val devices by devicesViewModel.devices.collectAsState()
    val deviceNames by devicesViewModel.deviceDisplayNames.collectAsState()
    val deviceInfo by devicesViewModel.deviceInfo.collectAsState()
    val centerInfo by devicesViewModel.centerInfo.collectAsState()
    val agentFeaturePreferences = remember { AgentFeatureRuntime.preferences }
    val reduceMotion by agentFeaturePreferences.reduceMotion.collectAsState()
    var prompt by remember { mutableStateOf("") }
    var showModelDialog by remember { mutableStateOf(false) }
    var showDeviceOverlay by remember { mutableStateOf(false) }
    val isConnected = selectedDevice != null && selectedDevice in devices

    fun canStart(task: String): Boolean = task.isNotBlank() &&
        !taskState.isRunning &&
        isConnected &&
        configurationReady

    val canSend = canStart(prompt)

    LaunchedEffect(Unit) {
        viewModel.refreshConfigurationStatus()
        devicesViewModel.refreshDevices()
    }

    fun submit(task: String) {
        if (!canStart(task)) return
        viewModel.startTask(task, selectedDevice)
        prompt = ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(QadbColors.background)
    ) {
        AgentDeviceHeader(
            selectedDevice = selectedDevice,
            displayName = selectedDevice?.let(deviceNames::get),
            deviceInfo = deviceInfo,
            centerInfo = centerInfo,
            connected = isConnected,
            running = taskState.isRunning
        )
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val layout = agentScreenLayout(maxWidth.value)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = UiTokens.SpaceXLarge),
                horizontalArrangement = Arrangement.End
            ) {
                if (layout == AgentScreenLayout.OVERLAY_DEVICE_PANEL) {
                    TextButton(onClick = { showDeviceOverlay = !showDeviceOverlay }) {
                        Text(
                            stringResource(
                                if (showDeviceOverlay) {
                                    Res.string.agent_preview_hide
                                } else {
                                    Res.string.agent_preview_show
                                }
                            )
                        )
                    }
                }
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val layout = agentScreenLayout(maxWidth.value)
            LaunchedEffect(layout) {
                if (layout != AgentScreenLayout.OVERLAY_DEVICE_PANEL) {
                    showDeviceOverlay = false
                }
            }
            Row(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = UiTokens.SpaceXLarge),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AgentConversation(
                        messages = taskState.messages,
                        errorMessage = taskState.errorMessage,
                        publicActivity = taskState.publicActivity,
                        reduceMotion = reduceMotion,
                        onRetry = ::submit,
                        onOpenSettings = { showModelDialog = true },
                        onOpenDevices = onOpenDevices,
                        canSubmitQuickPrompt = ::canStart,
                        onQuickPrompt = ::submit,
                        modifier = Modifier.weight(1f)
                    )

                    AgentComposer(
                        prompt = prompt,
                        onPromptChange = { prompt = it },
                        canSend = canSend,
                        running = taskState.isRunning,
                        configurationReady = configurationReady,
                        connected = isConnected,
                        requiresProvider = true,
                        requiresDevice = true,
                        observationMode = taskState.observationMode,
                        totalTokens = taskState.usage.totalTokens,
                        compactionCount = 0,
                        onSend = { submit(prompt) },
                        onNewTask = viewModel::newTask,
                        onCancel = viewModel::cancelTask
                    )
                }

                if (layout == AgentScreenLayout.PERMANENT_DEVICE_PANEL) {
                    DevicePreviewPanel(
                        observationMode = taskState.observationMode,
                        screenshot = taskState.latestScreenshot,
                        pageSignature = taskState.deviceState?.pageSignature?.value,
                        pageChanged = taskState.pageDiff?.changed,
                        modifier = Modifier
                            .width(316.dp)
                            .fillMaxHeight()
                    )
                }
            }

            if (layout == AgentScreenLayout.OVERLAY_DEVICE_PANEL && showDeviceOverlay) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(316.dp)
                        .fillMaxHeight()
                        .shadow(12.dp, RoundedCornerShape(UiTokens.RadiusLarge)),
                    shape = RoundedCornerShape(UiTokens.RadiusLarge),
                    color = QadbColors.surface
                ) {
                    DevicePreviewPanel(
                        observationMode = taskState.observationMode,
                        screenshot = taskState.latestScreenshot,
                        pageSignature = taskState.deviceState?.pageSignature?.value,
                        pageChanged = taskState.pageDiff?.changed,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    taskState.pendingConfirmation?.let { step ->
        val rejectFocusRequester = remember(step.id) { FocusRequester() }
        LaunchedEffect(step.id) { rejectFocusRequester.requestFocus() }
        AlertDialog(
            onDismissRequest = { viewModel.respondToConfirmation(false) },
            title = { Text(stringResource(Res.string.agent_sensitive_title)) },
            text = {
                AgentApprovalCardBody(step)
            },
            confirmButton = {
                Button(onClick = { viewModel.respondToConfirmation(true) }) {
                    Text(stringResource(Res.string.agent_approve))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.respondToConfirmation(false) },
                    modifier = Modifier.focusRequester(rejectFocusRequester)
                ) {
                    Text(stringResource(Res.string.agent_reject))
                }
            },
            containerColor = QadbColors.surface
        )
    }

    if (showModelDialog) {
        AiModelConfigDialog(
            initialConfig = modelConfig,
            hasSavedKey = apiKeyAvailable,
            onSaved = {
                viewModel.refreshConfigurationStatus()
                showModelDialog = false
            },
            onKeyCleared = viewModel::refreshConfigurationStatus,
            onDismiss = { showModelDialog = false }
        )
    }

}

@Composable
private fun AgentApprovalCardBody(step: AgentStep) {
    val actionName = actionDisplayName(step.action)
    val description = if (step.confirmationReason.isNotBlank()) {
        stringResource(
            Res.string.agent_sensitive_reason,
            actionName,
            step.confirmationReason
        )
    } else {
        stringResource(Res.string.agent_sensitive_desc, actionName)
    }

    Surface(
        shape = RoundedCornerShape(UiTokens.RadiusLarge),
        color = QadbColors.warningSurface,
        border = BorderStroke(1.dp, QadbColors.warning.copy(alpha = 0.24f))
    ) {
        Column(
            modifier = Modifier.padding(UiTokens.SpaceLarge),
            verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceMedium)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = QadbColors.warning.copy(alpha = 0.13f)
                ) {
                    Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = IconParkIcons.Info,
                            contentDescription = null,
                            tint = QadbColors.warning,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = actionName,
                        color = QadbColors.textPrimary,
                        fontSize = UiTokens.TextBodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(Res.string.agent_stage_waiting_confirmation),
                        color = QadbColors.warning,
                        fontSize = UiTokens.TextCaption,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(QadbColors.warning.copy(alpha = 0.18f)))
            Text(
                text = description,
                color = QadbColors.textSecondary,
                style = MaterialTheme.typography.body1.copy(
                    fontSize = UiTokens.TextBody,
                    lineHeight = 19.sp
                )
            )
        }
    }
}

@Composable
private fun AgentDeviceHeader(
    selectedDevice: String?,
    displayName: String?,
    deviceInfo: DeviceInfoData?,
    centerInfo: DeviceCenterInfoData?,
    connected: Boolean,
    running: Boolean
) {
    val deviceName = displayName.orEmpty().ifBlank {
        deviceInfo?.deviceModel.orEmpty().ifBlank { selectedDevice ?: stringResource(Res.string.agent_no_device) }
    }
    val androidVersion = deviceInfo?.androidVersion.orEmpty()
    val apiLevel = deviceInfo?.sdkVersion.orEmpty()
    val android = if (androidVersion.isBlank()) {
        "Android --"
    } else {
        "Android $androidVersion" + if (apiLevel.isBlank()) "" else " (API $apiLevel)"
    }
    val battery = centerInfo?.batteryLevel.orEmpty().ifBlank { "--" }
    val connection = if (
        selectedDevice?.contains(":") == true ||
        selectedDevice?.contains("_adb-tls") == true ||
        deviceInfo?.ipAddress?.isNotBlank() == true
    ) {
        "Wi‑Fi"
    } else {
        "USB"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .padding(horizontal = UiTokens.SpaceXLarge),
        horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceLarge),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = IconParkIcons.Phone,
            contentDescription = null,
            tint = QadbColors.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = deviceName,
            color = QadbColors.textPrimary,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        HeaderDivider()
        Text(android, color = QadbColors.textSecondary, fontSize = UiTokens.TextBody)
        HeaderDivider()
        Text(connection, color = QadbColors.textSecondary, fontSize = UiTokens.TextBody)
        HeaderDivider()
        Text(battery, color = QadbColors.textSecondary, fontSize = UiTokens.TextBody)
        Spacer(Modifier.weight(1f))
        StatusDot(connected)
        Text(
            text = stringResource(if (connected) Res.string.agent_connected else Res.string.agent_disconnected),
            color = if (connected) QadbColors.success else QadbColors.textTertiary,
            fontSize = UiTokens.TextBody
        )
        if (running) {
            Surface(
                shape = RoundedCornerShape(UiTokens.BadgeRadius),
                color = QadbColors.primaryContainer
            ) {
                Text(
                    stringResource(Res.string.agent_running),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    color = QadbColors.primary,
                    fontSize = UiTokens.TextCaption,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun HeaderDivider() {
    Box(
        Modifier
            .width(1.dp)
            .height(16.dp)
            .background(QadbColors.divider)
    )
}

@Composable
private fun StatusDot(connected: Boolean) {
    Box(
        Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(if (connected) QadbColors.success else QadbColors.textDisabled)
    )
}

@Composable
private fun AgentConversation(
    messages: List<AgentMessage>,
    errorMessage: String?,
    publicActivity: AgentPublicActivityState,
    reduceMotion: Boolean,
    onRetry: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDevices: () -> Unit,
    canSubmitQuickPrompt: (String) -> Boolean,
    onQuickPrompt: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val activeRun = publicActivity.activeRun
    val followThresholdPx = with(LocalDensity.current) { 64.dp.roundToPx() }
    val conversationItems = remember(messages, publicActivity, errorMessage) {
        val visibleMessages = messages.filter { it.role != AgentMessageRole.SYSTEM }
        val assistantRunIds = visibleMessages.asSequence()
            .filter { it.role == AgentMessageRole.ASSISTANT }
            .mapNotNull { it.runId }
            .toSet()
        buildList<AgentConversationItem> {
            if (shouldShowAgentGuide(messages)) add(AgentConversationItem.Guide)
            visibleMessages.forEach { message ->
                add(AgentConversationItem.Message(message))
                if (message.role == AgentMessageRole.USER) {
                    message.runId?.let(publicActivity.runs::get)?.let { run ->
                        add(AgentConversationItem.Activity(message, run))
                        if (run.responseText.isNotBlank() && run.runId !in assistantRunIds) {
                            add(AgentConversationItem.Streaming(run.runId, run.responseText))
                        }
                    }
                }
            }
            errorMessage?.takeIf { activeRun?.failure == null }?.let {
                add(AgentConversationItem.Error(it))
            }
        }
    }
    val isNearLatest by remember(listState, followThresholdPx) {
        derivedStateOf {
            val layout = listState.layoutInfo
            if (layout.totalItemsCount == 0) {
                true
            } else {
                val lastVisible = layout.visibleItemsInfo.lastOrNull()
                lastVisible != null &&
                    lastVisible.index == layout.totalItemsCount - 1 &&
                    lastVisible.offset + lastVisible.size - layout.viewportEndOffset <= followThresholdPx
            }
        }
    }
    var followsLatest by remember { mutableStateOf(true) }
    var programmaticScroll by remember { mutableStateOf(false) }

    LaunchedEffect(listState, followThresholdPx) {
        snapshotFlow { listState.isScrollInProgress to isNearLatest }
            .collect { (isScrolling, nearLatest) ->
                if (!programmaticScroll && isScrolling) {
                    followsLatest = nearLatest
                } else if (!isScrolling && nearLatest) {
                    followsLatest = true
                }
            }
    }
    LaunchedEffect(conversationItems, followsLatest) {
        if (followsLatest && conversationItems.isNotEmpty()) {
            programmaticScroll = true
            try {
                listState.scrollToItem(
                    conversationItems.lastIndex,
                    agentLatestItemScrollOffset()
                )
            } finally {
                programmaticScroll = false
            }
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(vertical = UiTokens.SpaceXLarge),
            verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceMedium)
        ) {
            items(
                items = conversationItems,
                key = AgentConversationItem::stableKey,
                contentType = AgentConversationItem::contentType
            ) { item ->
                when (item) {
                    AgentConversationItem.Guide -> Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceMedium)
                    ) {
                        AgentGuideCard(
                            icon = IconParkIcons.Command,
                            title = stringResource(Res.string.agent_ready_title),
                            description = stringResource(Res.string.agent_ready_desc)
                        )
                        QuickPromptRow(
                            canSubmit = canSubmitQuickPrompt,
                            onPrompt = onQuickPrompt,
                            modifier = Modifier.widthIn(max = 620.dp)
                        )
                    }
                    is AgentConversationItem.Message -> Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        AgentMessageBubble(item.message)
                    }
                    is AgentConversationItem.Activity -> {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            AgentActivityCard(
                                run = item.run,
                                reduceMotion = reduceMotion,
                                onRetry = { onRetry(item.userMessage.text) },
                                onOpenSettings = onOpenSettings,
                                onOpenDevices = onOpenDevices
                            )
                        }
                    }
                    is AgentConversationItem.Streaming -> Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        AgentStreamingResponse(item.text)
                    }
                    is AgentConversationItem.Error -> {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(UiTokens.RadiusLarge),
                            color = QadbColors.errorSurface
                        ) {
                            Text(
                                text = item.text,
                                modifier = Modifier.padding(UiTokens.SpaceLarge),
                                color = QadbColors.danger,
                                fontSize = UiTokens.TextBody
                            )
                        }
                    }
                }
            }
        }

        if (!followsLatest && !isNearLatest) {
            OutlinedButton(
                onClick = { followsLatest = true },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = UiTokens.SpaceSmall)
            ) {
                Text(stringResource(Res.string.agent_back_to_latest))
            }
        }
    }
}

private sealed interface AgentConversationItem {
    val stableKey: String
    val contentType: String

    data object Guide : AgentConversationItem {
        override val stableKey = "agent-guide"
        override val contentType = "guide"
    }

    data class Message(val message: AgentMessage) : AgentConversationItem {
        override val stableKey = "message:${message.id}"
        override val contentType = "message"
    }

    data class Activity(
        val userMessage: AgentMessage,
        val run: AgentRunPresentation
    ) : AgentConversationItem {
        override val stableKey = "activity:${run.runId}"
        override val contentType = "activity"
    }

    data class Streaming(val runId: String, val text: String) : AgentConversationItem {
        override val stableKey = "streaming:$runId"
        override val contentType = "streaming"
    }

    data class Error(val text: String) : AgentConversationItem {
        override val stableKey = "agent-error"
        override val contentType = "error"
    }
}

@Composable
private fun AgentActivityCard(
    run: AgentRunPresentation,
    reduceMotion: Boolean,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDevices: () -> Unit
) {
    var expanded by remember(run.runId) { mutableStateOf(run.preferredExpanded) }
    var nowMs by remember(run.runId) { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(run.status) {
        when (run.status) {
            AgentPublicRunStatus.WAITING_CONFIRMATION,
            AgentPublicRunStatus.FAILED -> expanded = true
            AgentPublicRunStatus.COMPLETED,
            AgentPublicRunStatus.CANCELLED -> expanded = false
            AgentPublicRunStatus.RUNNING,
            AgentPublicRunStatus.CANCELLING -> Unit
        }
    }
    LaunchedEffect(run.runId, run.finishedAtMs) {
        val finishedAt = run.finishedAtMs
        if (finishedAt != null) {
            nowMs = finishedAt
        } else {
            while (true) {
                nowMs = System.currentTimeMillis()
                delay(1_000)
            }
        }
    }

    val elapsedMs = ((run.finishedAtMs ?: nowMs) - run.startedAtMs).coerceAtLeast(0)
    val stageColor = when (run.status) {
        AgentPublicRunStatus.COMPLETED -> QadbColors.success
        AgentPublicRunStatus.FAILED -> QadbColors.danger
        AgentPublicRunStatus.WAITING_CONFIRMATION -> QadbColors.warning
        AgentPublicRunStatus.CANCELLED -> QadbColors.textTertiary
        AgentPublicRunStatus.CANCELLING,
        AgentPublicRunStatus.RUNNING -> QadbColors.primary
    }
    val stageLiveRegion = if (
        run.status == AgentPublicRunStatus.FAILED ||
        run.status == AgentPublicRunStatus.WAITING_CONFIRMATION
    ) {
        LiveRegionMode.Assertive
    } else {
        LiveRegionMode.Polite
    }

    Surface(
        modifier = Modifier
            .widthIn(max = 680.dp)
            .fillMaxWidth()
            .border(1.dp, QadbColors.border, RoundedCornerShape(UiTokens.RadiusLarge)),
        shape = RoundedCornerShape(UiTokens.RadiusLarge),
        color = when (run.status) {
            AgentPublicRunStatus.FAILED -> QadbColors.errorSurface
            AgentPublicRunStatus.WAITING_CONFIRMATION -> QadbColors.warningSurface
            else -> QadbColors.surfaceVariant.copy(alpha = 0.58f)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = UiTokens.SpaceLarge, vertical = UiTokens.SpaceMedium),
            verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceSmall)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().semantics { liveRegion = stageLiveRegion },
                horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(stageColor))
                Text(
                    text = publicStageLabel(run.stage),
                    color = QadbColors.textPrimary,
                    fontSize = UiTokens.TextBody,
                    fontWeight = FontWeight.SemiBold
                )
                AgentThinkingDots(
                    visible = run.status == AgentPublicRunStatus.RUNNING &&
                        run.stage in setOf(
                            AgentPublicStage.UNDERSTANDING,
                            AgentPublicStage.PLANNING,
                            AgentPublicStage.RECOVERING,
                            AgentPublicStage.RESPONDING
                        ),
                    reduceMotion = reduceMotion
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = stringResource(
                        Res.string.agent_activity_elapsed,
                        publicDurationLabel(elapsedMs)
                    ),
                    color = QadbColors.textTertiary,
                    fontSize = UiTokens.TextCaption
                )
                Text(
                    text = stringResource(
                        if (expanded) {
                            Res.string.agent_activity_hide_details
                        } else {
                            Res.string.agent_activity_show_details
                        }
                    ),
                    color = QadbColors.primary,
                    fontSize = UiTokens.TextCaption,
                    fontWeight = FontWeight.Medium
                )
            }

            run.latestTool?.let { tool ->
                AgentToolChip(tool)
            }

            if (expanded) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(QadbColors.divider))
                run.failure?.let { failure ->
                    Text(
                        text = publicFailureLabel(failure),
                        color = QadbColors.danger,
                        fontSize = UiTokens.TextBody
                    )
                    Text(
                        text = l10n(
                            "阶段 ${publicFailureStageLabel(failure)} · 子系统 ${publicFailureSubsystemLabel(failure)} · 代码 ${failure.code.name.lowercase()}",
                            "Stage ${publicFailureStageLabel(failure)} · subsystem ${publicFailureSubsystemLabel(failure)} · code ${failure.code.name.lowercase()}"
                        ),
                        color = QadbColors.textTertiary,
                        fontSize = UiTokens.TextCaption
                    )
                    val actionLabel = when (failure.suggestedAction) {
                        AgentFailureAction.RETRY -> stringResource(Res.string.file_browser_retry)
                        AgentFailureAction.OPEN_MODEL_SETTINGS -> stringResource(Res.string.agent_open_settings)
                        AgentFailureAction.OPEN_DEVICES -> stringResource(Res.string.agent_open_devices)
                        AgentFailureAction.NONE -> null
                    }
                    if (actionLabel != null) {
                        OutlinedButton(
                            onClick = when (failure.suggestedAction) {
                                AgentFailureAction.RETRY -> onRetry
                                AgentFailureAction.OPEN_MODEL_SETTINGS -> onOpenSettings
                                AgentFailureAction.OPEN_DEVICES -> onOpenDevices
                                AgentFailureAction.NONE -> ({})
                            }
                        ) {
                            Text(actionLabel)
                        }
                    }
                }
                run.activities.takeLast(12).forEach { activity ->
                    AgentActivityTimelineRow(
                        activity = activity,
                        elapsedMs = (activity.occurredAtMs - run.startedAtMs).coerceAtLeast(0)
                    )
                }
                val metrics = run.metrics
                if (metrics.modelCalls > 0 || metrics.totalTokens > 0) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(QadbColors.divider))
                    Text(
                        text = l10n(
                            "运行引擎：${metrics.engineVersion.name}",
                            "Engine: ${metrics.engineVersion.name}"
                        ),
                        color = QadbColors.textTertiary,
                        fontSize = UiTokens.TextCaption
                    )
                    Text(
                        text = stringResource(
                            if (metrics.modelCallLimit > 0) {
                                Res.string.agent_activity_budget
                            } else {
                                Res.string.agent_activity_usage
                            }
                        ),
                        color = QadbColors.textSecondary,
                        fontSize = UiTokens.TextCaption,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (metrics.modelCallLimit > 0) {
                            stringResource(
                                Res.string.agent_activity_budget_calls,
                                metrics.modelCalls,
                                metrics.modelCallLimit,
                                metrics.visionCalls,
                                metrics.visionCallLimit,
                                metrics.replans,
                                metrics.replanLimit
                            )
                        } else {
                            stringResource(
                                Res.string.agent_activity_usage_calls,
                                metrics.modelCalls,
                                metrics.visionCalls,
                                metrics.replans
                            )
                        },
                        color = QadbColors.textSecondary,
                        fontSize = UiTokens.TextCaption
                    )
                    Text(
                        text = stringResource(
                            Res.string.agent_activity_budget_tokens,
                            metrics.promptTokens,
                            metrics.completionTokens,
                            metrics.cachedTokens,
                            metrics.totalTokens
                        ),
                        color = QadbColors.textTertiary,
                        fontSize = UiTokens.TextCaption
                    )
                }
            }
        }
    }
}

@Composable
private fun AgentActivityTimelineRow(activity: AgentPublicActivityItem, elapsedMs: Long) {
    val activityLabel = activity.tool?.let { publicToolLabel(it) } ?: publicStageLabel(activity.stage)
    val resultLabel = activity.result?.let { publicToolResultLabel(it) }
    val stateColor = when (activity.result) {
        AgentPublicToolResult.SUCCEEDED,
        AgentPublicToolResult.RECOVERED -> QadbColors.success
        AgentPublicToolResult.FAILED,
        AgentPublicToolResult.DENIED -> QadbColors.danger
        AgentPublicToolResult.UNVERIFIED -> QadbColors.warning
        null -> QadbColors.primary
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = publicDurationLabel(elapsedMs),
            color = QadbColors.textTertiary,
            fontSize = 11.sp,
            modifier = Modifier.width(56.dp)
        )
        Box(
            modifier = Modifier.size(18.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.width(1.dp).height(18.dp).background(QadbColors.divider))
            Box(Modifier.size(7.dp).clip(CircleShape).background(stateColor))
        }
        Text(
            text = activityLabel,
            color = QadbColors.textSecondary,
            fontSize = UiTokens.TextCaption,
            modifier = Modifier.weight(1f)
        )
        resultLabel?.let {
            Surface(
                shape = RoundedCornerShape(UiTokens.BadgeRadius),
                color = stateColor.copy(alpha = 0.11f),
                border = BorderStroke(1.dp, stateColor.copy(alpha = 0.22f))
            ) {
                Text(
                    text = it,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    color = stateColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun AgentToolChip(tool: AgentPublicToolSummary) {
    Surface(
        modifier = Modifier.widthIn(max = 360.dp),
        shape = RoundedCornerShape(UiTokens.BadgeRadius),
        color = QadbColors.surface,
        border = BorderStroke(1.dp, QadbColors.border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = IconParkIcons.Command,
                contentDescription = null,
                tint = QadbColors.primary,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = publicToolLabel(tool),
                color = QadbColors.textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AgentThinkingDots(visible: Boolean, reduceMotion: Boolean) {
    if (!visible) return
    val phase = if (reduceMotion) {
        4
    } else {
        val transition = rememberInfiniteTransition()
        val animatedPhase by transition.animateFloat(
            initialValue = 0f,
            targetValue = 9f,
            animationSpec = infiniteRepeatable(tween(durationMillis = 900))
        )
        animatedPhase.toInt().coerceIn(0, 8)
    }
    Column(verticalArrangement = Arrangement.spacedBy(1.5.dp)) {
        repeat(3) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(1.5.dp)) {
                repeat(3) { column ->
                    val index = row * 3 + column
                    val distance = (index - phase + 9) % 9
                    Box(
                        Modifier
                            .size(3.5.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(
                                QadbColors.primary.copy(
                                    alpha = when (distance) {
                                        0 -> 1f
                                        1, 8 -> 0.58f
                                        else -> 0.18f
                                    }
                                )
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun publicDurationLabel(durationMs: Long): String {
    val seconds = durationMs / 1_000
    return if (seconds < 60) {
        stringResource(Res.string.agent_activity_duration_seconds, seconds)
    } else {
        stringResource(
            Res.string.agent_activity_duration_minutes,
            seconds / 60,
            seconds % 60
        )
    }
}

@Composable
private fun publicStageLabel(stage: AgentPublicStage): String = stringResource(
    when (stage) {
        AgentPublicStage.UNDERSTANDING -> Res.string.agent_stage_understanding
        AgentPublicStage.READING -> Res.string.agent_stage_reading
        AgentPublicStage.PLANNING -> Res.string.agent_stage_planning
        AgentPublicStage.EXECUTING -> Res.string.agent_stage_executing
        AgentPublicStage.VERIFYING -> Res.string.agent_stage_verifying
        AgentPublicStage.RECOVERING -> Res.string.agent_stage_recovering
        AgentPublicStage.WAITING_CONFIRMATION -> Res.string.agent_stage_waiting_confirmation
        AgentPublicStage.RESPONDING -> Res.string.agent_stage_responding
        AgentPublicStage.CANCELLING -> Res.string.agent_stage_cancelling
        AgentPublicStage.COMPLETED -> Res.string.agent_stage_completed
        AgentPublicStage.FAILED -> Res.string.agent_stage_failed
        AgentPublicStage.CANCELLED -> Res.string.agent_stage_cancelled
    }
)

@Composable
private fun publicToolLabel(tool: AgentPublicToolSummary): String = when (tool.kind) {
    AgentPublicToolKind.SEMANTIC_GOAL -> l10n("执行语义设备任务", "Execute semantic device task")
    AgentPublicToolKind.OBSERVE_DEVICE -> stringResource(Res.string.agent_public_tool_observe)
    AgentPublicToolKind.FIND_APP -> stringResource(Res.string.agent_public_tool_find_app)
    AgentPublicToolKind.OPEN_APP -> stringResource(Res.string.agent_public_tool_open_app)
    AgentPublicToolKind.TAP -> stringResource(Res.string.agent_public_tool_tap)
    AgentPublicToolKind.SWIPE -> stringResource(Res.string.agent_public_tool_swipe)
    AgentPublicToolKind.INPUT_TEXT -> stringResource(
        Res.string.agent_public_tool_input,
        tool.inputCharacterCount ?: 0
    )
    AgentPublicToolKind.KEY_EVENT -> stringResource(Res.string.agent_public_tool_key)
    AgentPublicToolKind.WAIT -> stringResource(Res.string.agent_public_tool_wait)
    AgentPublicToolKind.FINISH -> stringResource(Res.string.agent_public_tool_finish)
    AgentPublicToolKind.FORCE_STOP_APP -> stringResource(Res.string.agent_public_tool_force_stop)
    AgentPublicToolKind.CLEAR_APP_DATA -> stringResource(Res.string.agent_public_tool_clear_data)
    AgentPublicToolKind.UNINSTALL_APP -> stringResource(Res.string.agent_public_tool_uninstall)
    AgentPublicToolKind.REBOOT_DEVICE -> stringResource(Res.string.agent_public_tool_reboot)
}

@Composable
private fun publicToolResultLabel(result: AgentPublicToolResult): String = stringResource(
    when (result) {
        AgentPublicToolResult.SUCCEEDED -> Res.string.agent_public_result_succeeded
        AgentPublicToolResult.RECOVERED -> Res.string.agent_public_result_recovered
        AgentPublicToolResult.UNVERIFIED -> Res.string.agent_public_result_unverified
        AgentPublicToolResult.FAILED -> Res.string.agent_public_result_failed
        AgentPublicToolResult.DENIED -> Res.string.agent_public_result_denied
    }
)

@Composable
private fun publicFailureLabel(failure: AgentFailure): String = stringResource(
    if (failure.code == AgentFailureCode.OUTCOME_UNCERTAIN) {
        Res.string.agent_failure_outcome_uncertain
    } else if (failure.code == AgentFailureCode.MODEL_CALL_TIMED_OUT) {
        Res.string.agent_failure_model_timeout
    } else when (failure.category) {
        AgentFailureCategory.PROTOCOL -> Res.string.agent_failure_protocol
        AgentFailureCategory.NETWORK -> Res.string.agent_failure_network
        AgentFailureCategory.RATE_LIMIT -> Res.string.agent_failure_rate_limit
        AgentFailureCategory.AUTHENTICATION -> Res.string.agent_failure_authentication
        AgentFailureCategory.DEVICE_DISCONNECTED -> Res.string.agent_failure_device
        AgentFailureCategory.DEVICE_EXECUTION -> Res.string.agent_failure_device_execution
        AgentFailureCategory.OBSERVATION -> Res.string.agent_failure_observation
        AgentFailureCategory.VERIFICATION -> Res.string.agent_failure_verification
        AgentFailureCategory.SAFETY_BLOCKED -> Res.string.agent_failure_safety
        AgentFailureCategory.BUDGET_EXHAUSTED -> Res.string.agent_failure_budget
        AgentFailureCategory.UNKNOWN -> Res.string.agent_failure_unknown
    }
)

private fun publicFailureStageLabel(failure: AgentFailure): String = when (failure.stage) {
    AgentFailureStage.CONFIGURATION -> l10n("配置", "configuration")
    AgentFailureStage.UNDERSTANDING -> l10n("理解", "understanding")
    AgentFailureStage.OBSERVING -> l10n("观察", "observing")
    AgentFailureStage.PLANNING -> l10n("规划", "planning")
    AgentFailureStage.EXECUTING -> l10n("执行", "executing")
    AgentFailureStage.VERIFYING -> l10n("验证", "verifying")
    AgentFailureStage.RECOVERING -> l10n("恢复", "recovering")
    AgentFailureStage.UNKNOWN -> l10n("未知", "unknown")
}

private fun publicFailureSubsystemLabel(failure: AgentFailure): String = when (failure.subsystem) {
    AgentFailureSubsystem.PROVIDER -> "Provider"
    AgentFailureSubsystem.DEVICE_TRANSPORT -> l10n("设备连接", "device transport")
    AgentFailureSubsystem.OBSERVATION -> l10n("设备观察", "observation")
    AgentFailureSubsystem.DEVICE_ACTION -> l10n("设备动作", "device action")
    AgentFailureSubsystem.VERIFICATION -> l10n("完成验证", "verification")
    AgentFailureSubsystem.SAFETY_POLICY -> l10n("安全策略", "safety policy")
    AgentFailureSubsystem.ORCHESTRATOR -> l10n("任务编排", "orchestrator")
    AgentFailureSubsystem.UNKNOWN -> l10n("未知", "unknown")
}

@Composable
private fun AgentStreamingResponse(text: String) {
    AgentMarkdownText(
        text = text,
        modifier = Modifier.widthIn(max = 680.dp)
    )
}

@Composable
private fun AgentGuideCard(
    icon: ImageVector,
    title: String,
    description: String,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .widthIn(max = 620.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(UiTokens.RadiusXLarge),
        color = QadbColors.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(UiTokens.SpaceXLarge),
            horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceLarge),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = QadbColors.primaryContainer
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = QadbColors.primary,
                    modifier = Modifier.padding(10.dp).size(20.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceXSmall)
            ) {
                Text(title, color = QadbColors.textPrimary, fontWeight = FontWeight.SemiBold)
                Text(description, color = QadbColors.textSecondary, fontSize = UiTokens.TextBody)
            }
            if (action != null && onAction != null) {
                OutlinedButton(onClick = onAction) {
                    Text(action)
                }
            }
        }
    }
}

@Composable
private fun AgentMessageBubble(message: AgentMessage) {
    val isUser = message.role == AgentMessageRole.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (isUser) {
            Surface(
                modifier = Modifier.widthIn(max = 500.dp),
                shape = RoundedCornerShape(18.dp),
                color = QadbColors.surfaceVariant
            ) {
                Text(
                    text = message.text,
                    modifier = Modifier.padding(horizontal = UiTokens.SpaceLarge, vertical = 10.dp),
                    color = QadbColors.textPrimary,
                    fontSize = UiTokens.TextBodyLarge
                )
            }
        } else {
            AgentMarkdownText(
                text = message.text,
                modifier = Modifier.widthIn(max = 680.dp)
            )
        }
    }
}

@Composable
private fun QuickPromptRow(
    canSubmit: (String) -> Boolean,
    onPrompt: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val quickPrompts = listOf(
        Triple(Res.string.agent_quick_settings, Res.string.agent_prompt_settings, IconParkIcons.Setting),
        Triple(Res.string.agent_quick_recognize, Res.string.agent_prompt_recognize, IconParkIcons.Search)
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = UiTokens.SpaceSmall),
        horizontalArrangement = Arrangement.Center
    ) {
        quickPrompts.forEach { (label, prompt, icon) ->
            val promptText = stringResource(prompt)
            val enabled = canSubmit(promptText)
            Surface(
                modifier = Modifier
                    .height(30.dp)
                    .clickable(enabled = enabled) { onPrompt(promptText) },
                shape = RoundedCornerShape(UiTokens.BadgeRadius),
                color = if (enabled) QadbColors.surfaceVariant.copy(alpha = 0.72f) else QadbColors.disabledSurface
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = UiTokens.SpaceSmall),
                    horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceXSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = QadbColors.textSecondary)
                    Text(stringResource(label), color = QadbColors.textPrimary, fontSize = UiTokens.TextBody)
                }
            }
        }
    }
}

@Composable
private fun AgentComposer(
    prompt: String,
    onPromptChange: (String) -> Unit,
    canSend: Boolean,
    running: Boolean,
    configurationReady: Boolean,
    connected: Boolean,
    requiresProvider: Boolean,
    requiresDevice: Boolean,
    observationMode: AgentObservationMode,
    totalTokens: Int,
    compactionCount: Int,
    onSend: () -> Unit,
    onNewTask: () -> Unit,
    onCancel: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val composerShape = RoundedCornerShape(20.dp)
    val borderColor = if (focused) {
        QadbColors.primary.copy(alpha = 0.62f)
    } else {
        QadbColors.border
    }
    val statusText = when {
        running -> stringResource(Res.string.agent_running)
        requiresProvider && !configurationReady -> stringResource(Res.string.agent_composer_waiting_model)
        requiresDevice && !connected -> stringResource(Res.string.agent_composer_waiting_device)
        observationMode == AgentObservationMode.TEXT_ONLY -> stringResource(Res.string.agent_text_mode)
        else -> stringResource(Res.string.agent_vision_mode)
    }
    val statusColor = when {
        running -> QadbColors.primary
        (!requiresProvider || configurationReady) && (!requiresDevice || connected) -> QadbColors.success
        else -> QadbColors.textTertiary
    }

    Surface(
        modifier = Modifier
            .widthIn(max = 760.dp)
            .fillMaxWidth()
            .padding(bottom = UiTokens.SpaceMedium)
            .shadow(
                elevation = if (focused) 7.dp else 3.dp,
                shape = composerShape,
                clip = false
            ),
        shape = composerShape,
        color = QadbColors.surface,
        border = BorderStroke(if (focused) 1.5.dp else 1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(
                start = UiTokens.SpaceLarge,
                top = 14.dp,
                end = UiTokens.SpaceMedium,
                bottom = UiTokens.SpaceSmall
            )
        ) {
            BasicTextField(
                value = prompt,
                onValueChange = onPromptChange,
                enabled = !running,
                textStyle = MaterialTheme.typography.body1.copy(
                    color = QadbColors.textPrimary,
                    fontSize = UiTokens.TextBodyLarge,
                    lineHeight = 21.sp
                ),
                cursorBrush = SolidColor(QadbColors.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp, max = 138.dp)
                    .onFocusChanged { focused = it.isFocused }
                    .onPreviewKeyEvent { event ->
                        if (
                            event.type == KeyEventType.KeyDown &&
                            event.key == Key.Enter &&
                            !event.isShiftPressed
                        ) {
                            if (canSend) onSend()
                            true
                        } else {
                            false
                        }
                    },
                maxLines = 6,
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (prompt.isEmpty()) {
                            Text(
                                text = stringResource(Res.string.agent_input_hint),
                                color = QadbColors.textTertiary,
                                style = MaterialTheme.typography.body1.copy(
                                    fontSize = UiTokens.TextBodyLarge,
                                    lineHeight = 21.sp
                                )
                            )
                        }
                        innerTextField()
                    }
                }
            )

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .padding(top = UiTokens.SpaceXSmall)
            ) {
                val showKeyboardHint = this.maxWidth >= 520.dp
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onNewTask,
                        enabled = !running,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (running) QadbColors.textDisabled else QadbColors.textPrimary
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = stringResource(Res.string.agent_new_task),
                            color = if (running) QadbColors.textDisabled else QadbColors.textPrimary,
                            fontSize = UiTokens.TextBody
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    Surface(
                        shape = RoundedCornerShape(UiTokens.BadgeRadius),
                        color = QadbColors.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Text(
                                text = statusText,
                                color = QadbColors.textSecondary,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }

                    if (totalTokens > 0 && showKeyboardHint) {
                        Text(
                            text = stringResource(
                                Res.string.agent_usage_status,
                                totalTokens,
                                compactionCount
                            ),
                            color = QadbColors.textTertiary,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    } else if (showKeyboardHint) {
                        Text(
                            text = stringResource(Res.string.agent_composer_shortcut),
                            color = QadbColors.textTertiary,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .size(40.dp)
                            .clickable(
                                enabled = running || canSend,
                                onClick = if (running) onCancel else onSend
                            ),
                        shape = CircleShape,
                        color = when {
                            running || canSend -> QadbColors.textPrimary
                            else -> QadbColors.disabledSurface
                        }
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (running) IconParkIcons.Close else IconParkIcons.Send,
                                contentDescription = stringResource(
                                    if (running) Res.string.agent_cancel_task else Res.string.agent_send
                                ),
                                tint = when {
                                    running || canSend -> QadbColors.surface
                                    else -> QadbColors.textDisabled
                                },
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DevicePreviewPanel(
    observationMode: AgentObservationMode,
    screenshot: ByteArray?,
    pageSignature: String?,
    pageChanged: Boolean?,
    modifier: Modifier = Modifier
) {
    val deviceImage = remember(screenshot) {
        screenshot?.let { bytes -> runCatching { SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap() }.getOrNull() }
    }
    Column(
        modifier = modifier
            .border(width = 1.dp, color = QadbColors.divider)
            .padding(UiTokens.SpaceLarge),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(Res.string.agent_preview_title),
                color = QadbColors.textPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Surface(
                shape = RoundedCornerShape(UiTokens.BadgeRadius),
                color = QadbColors.primaryContainer
            ) {
                Text(
                    stringResource(
                        if (observationMode == AgentObservationMode.TEXT_ONLY) {
                            Res.string.agent_text_mode
                        } else {
                            Res.string.agent_vision_mode
                        }
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = QadbColors.primary,
                    fontSize = 11.sp
                )
            }
        }
        if (deviceImage != null) {
            Image(
                bitmap = deviceImage,
                contentDescription = stringResource(Res.string.agent_preview_title),
                modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = UiTokens.SpaceLarge),
                contentScale = ContentScale.Fit
            )
        } else {
            Image(
                painter = painterResource(Res.drawable.agent_device_placeholder),
                contentDescription = stringResource(Res.string.agent_preview_unavailable),
                modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = UiTokens.SpaceLarge),
                contentScale = ContentScale.Fit
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(UiTokens.RadiusLarge),
            color = QadbColors.surfaceVariant
        ) {
            Column(
                modifier = Modifier.padding(UiTokens.SpaceMedium),
                verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceXSmall)
            ) {
                Text(
                    if (deviceImage == null) stringResource(Res.string.agent_preview_unavailable) else "Live device observation",
                    color = QadbColors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = UiTokens.TextBody
                )
                Text(
                    pageSignature?.let { "Page ${it.take(10)} · ${if (pageChanged == true) "changed" else "stable"}" }
                        ?: stringResource(Res.string.agent_preview_desc),
                    color = QadbColors.textSecondary,
                    fontSize = UiTokens.TextCaption
                )
            }
        }
    }
}

@Composable
private fun actionDisplayName(action: AgentAction): String = when (action) {
    AgentAction.Observe -> stringResource(Res.string.agent_action_observe)
    is AgentAction.FindApp -> stringResource(Res.string.agent_action_find_app, action.query)
    is AgentAction.OpenApp -> stringResource(Res.string.agent_action_launch, action.query)
    is AgentAction.Tap -> stringResource(Res.string.agent_action_tap, action.x, action.y)
    is AgentAction.TapElement -> stringResource(Res.string.agent_action_tap_element, action.elementId)
    is AgentAction.Swipe -> stringResource(Res.string.agent_action_swipe)
    is AgentAction.InputText -> stringResource(Res.string.agent_action_input)
    is AgentAction.KeyEvent -> stringResource(Res.string.agent_action_key, action.key.name)
    is AgentAction.LaunchPackage -> stringResource(Res.string.agent_action_launch, action.packageName)
    is AgentAction.Wait -> stringResource(Res.string.agent_action_wait, action.durationMs)
    is AgentAction.Finish -> stringResource(Res.string.agent_action_finish)
    is AgentAction.ForceStopPackage -> stringResource(Res.string.agent_action_force_stop, action.packageName)
    is AgentAction.ClearAppData -> stringResource(Res.string.agent_action_clear_data, action.packageName)
    is AgentAction.UninstallPackage -> stringResource(Res.string.agent_action_uninstall, action.packageName)
    AgentAction.RebootDevice -> stringResource(Res.string.agent_action_reboot)
}
