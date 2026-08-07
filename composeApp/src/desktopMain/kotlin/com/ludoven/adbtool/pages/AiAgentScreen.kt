package com.ludoven.adbtool.pages

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
import adbtool_desktop.composeapp.generated.resources.agent_quick_camera
import adbtool_desktop.composeapp.generated.resources.agent_quick_clear_cache
import adbtool_desktop.composeapp.generated.resources.agent_quick_recognize
import adbtool_desktop.composeapp.generated.resources.agent_quick_settings
import adbtool_desktop.composeapp.generated.resources.agent_quick_wechat
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import com.ludoven.adbtool.QadbColors
import com.ludoven.adbtool.UiTokens
import com.ludoven.adbtool.agent.AgentAction
import com.ludoven.adbtool.agent.AgentBudgetMode
import com.ludoven.adbtool.agent.AgentBudgetStatus
import com.ludoven.adbtool.agent.AgentMessage
import com.ludoven.adbtool.agent.AgentMessageRole
import com.ludoven.adbtool.agent.AgentObservationMode
import com.ludoven.adbtool.agent.AgentStep
import com.ludoven.adbtool.agent.AgentAppKnowledgeCard
import com.ludoven.adbtool.agent.AgentWorkflow
import com.ludoven.adbtool.agent.WorkflowStatus
import com.ludoven.adbtool.entity.DeviceCenterInfoData
import com.ludoven.adbtool.entity.DeviceInfoData
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
    val memoryEnabled by viewModel.memoryEnabled.collectAsState()
    val memoryNeedsConsent by viewModel.memoryNeedsConsent.collectAsState()
    val workflows by viewModel.workflows.collectAsState()
    val knowledgeCards by viewModel.knowledgeCards.collectAsState()
    val selectedDevice by devicesViewModel.selectedDevice.collectAsState()
    val devices by devicesViewModel.devices.collectAsState()
    val deviceNames by devicesViewModel.deviceDisplayNames.collectAsState()
    val deviceInfo by devicesViewModel.deviceInfo.collectAsState()
    val centerInfo by devicesViewModel.centerInfo.collectAsState()
    var prompt by remember { mutableStateOf("") }
    var showModelDialog by remember { mutableStateOf(false) }
    var showAutomationDialog by remember { mutableStateOf(false) }
    val isConnected = selectedDevice != null && selectedDevice in devices
    val canSend = configurationReady && isConnected && !taskState.isRunning

    LaunchedEffect(Unit) {
        viewModel.refreshConfigurationStatus()
        devicesViewModel.refreshDevices()
    }

    fun submit(task: String) {
        if (!canSend || task.isBlank()) return
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
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = UiTokens.SpaceXLarge),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = { showAutomationDialog = true }) { Text("自动化知识") }
        }

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val showPreview = maxWidth >= 900.dp
            Row(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = UiTokens.SpaceXLarge)
                ) {
                    AgentConversation(
                        messages = taskState.messages,
                        configurationReady = configurationReady,
                        connected = isConnected,
                        errorMessage = taskState.errorMessage,
                        budgetStatus = taskState.budgetStatus,
                        onOpenSettings = { showModelDialog = true },
                        onOpenDevices = onOpenDevices,
                        modifier = Modifier.weight(1f)
                    )

                    AgentComposer(
                        prompt = prompt,
                        onPromptChange = { prompt = it },
                        canSend = canSend && prompt.isNotBlank(),
                        running = taskState.isRunning,
                        configurationReady = configurationReady,
                        connected = isConnected,
                        observationMode = taskState.observationMode,
                        memoryEnabled = memoryEnabled,
                        memoryHitCount = taskState.memoryHitCount,
                        totalTokens = taskState.usage.totalTokens,
                        compactionCount = taskState.compactionCount,
                        savedMemoryCount = taskState.savedMemoryCount,
                        onSend = { submit(prompt) },
                        onQuickPrompt = ::submit,
                        onNewTask = viewModel::newTask,
                        onCancel = viewModel::cancelTask,
                        onOpenMemory = onOpenSettings
                    )
                }

                if (showPreview) {
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
        }
    }

    taskState.pendingConfirmation?.let { step ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(Res.string.agent_sensitive_title)) },
            text = {
                Text(
                    if (step.confirmationReason.isNotBlank()) {
                        stringResource(
                            Res.string.agent_sensitive_reason,
                            actionDisplayName(step.action),
                            step.confirmationReason
                        )
                    } else {
                        stringResource(
                            Res.string.agent_sensitive_desc,
                            actionDisplayName(step.action)
                        )
                    }
                )
            },
            confirmButton = {
                Button(onClick = { viewModel.respondToConfirmation(true) }) {
                    Text(stringResource(Res.string.agent_approve))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.respondToConfirmation(false) }) {
                    Text(stringResource(Res.string.agent_reject))
                }
            },
            containerColor = QadbColors.surface
        )
    }

    if (memoryNeedsConsent) {
        AlertDialog(
            onDismissRequest = viewModel::declineMemoryConsent,
            title = { Text(stringResource(Res.string.agent_memory_consent_title)) },
            text = { Text(stringResource(Res.string.agent_memory_consent_desc)) },
            confirmButton = {
                Button(onClick = viewModel::acceptMemoryConsent) {
                    Text(stringResource(Res.string.agent_memory_enable))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::declineMemoryConsent) {
                    Text(stringResource(Res.string.agent_memory_not_now))
                }
            }
        )
    }

    if (showModelDialog) {
        AiModelConfigDialog(
            initialConfig = modelConfig,
            hasSavedKey = apiKeyAvailable,
            onSaved = { showModelDialog = false },
            onKeyCleared = viewModel::refreshConfigurationStatus,
            onDismiss = { showModelDialog = false }
        )
    }

    if (showAutomationDialog) {
        AutomationKnowledgeDialog(
            workflows = workflows,
            knowledgeCards = knowledgeCards,
            onEnableWorkflow = { viewModel.setWorkflowEnabled(it, true) },
            onDisableWorkflow = { viewModel.setWorkflowEnabled(it, false) },
            onDeleteWorkflow = viewModel::deleteWorkflow,
            onSaveKnowledge = viewModel::saveKnowledgeCard,
            onSetKnowledgeEnabled = viewModel::setKnowledgeCardEnabled,
            onDeleteKnowledge = viewModel::deleteKnowledgeCard,
            onDismiss = { showAutomationDialog = false }
        )
    }
}

@Composable
private fun AutomationKnowledgeDialog(
    workflows: List<AgentWorkflow>,
    knowledgeCards: List<AgentAppKnowledgeCard>,
    onEnableWorkflow: (AgentWorkflow) -> Unit,
    onDisableWorkflow: (AgentWorkflow) -> Unit,
    onDeleteWorkflow: (AgentWorkflow) -> Unit,
    onSaveKnowledge: (AgentAppKnowledgeCard) -> Unit,
    onSetKnowledgeEnabled: (AgentAppKnowledgeCard, Boolean) -> Unit,
    onDeleteKnowledge: (AgentAppKnowledgeCard) -> Unit,
    onDismiss: () -> Unit
) {
    var tab by remember { mutableStateOf(0) }
    var editing by remember { mutableStateOf<AgentAppKnowledgeCard?>(null) }
    var pendingWorkflowDelete by remember { mutableStateOf<AgentWorkflow?>(null) }
    var pendingKnowledgeDelete by remember { mutableStateOf<AgentAppKnowledgeCard?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自动化知识") },
        text = {
            Column(modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceSmall)) {
                Row(horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceSmall)) {
                    TextButton(onClick = { tab = 0 }) { Text(if (tab == 0) "Workflow ·" else "Workflow") }
                    TextButton(onClick = { tab = 1 }) { Text(if (tab == 1) "应用知识 ·" else "应用知识") }
                    if (tab == 1) TextButton(onClick = { editing = AgentAppKnowledgeCard(packageName = "", title = "", guide = "") }) { Text("新建") }
                }
                if (tab == 0) {
                    workflows.forEach { workflow ->
                        Surface(color = QadbColors.surfaceVariant, shape = RoundedCornerShape(UiTokens.RadiusMedium)) {
                            Column(modifier = Modifier.padding(UiTokens.SpaceMedium)) {
                                Text(workflow.name, fontWeight = FontWeight.SemiBold)
                                Text("${workflow.packageName} · ${workflow.replaySteps.size} 步 · 成功率 ${"%.0f".format(workflow.statistics.successRate * 100)}%", color = QadbColors.textSecondary, fontSize = UiTokens.TextCaption)
                                Text(if (workflow.canEnable) workflow.status.name else "旧版或无效草稿，需重新生成", color = QadbColors.textTertiary, fontSize = UiTokens.TextCaption)
                                Row {
                                    if (workflow.status == WorkflowStatus.ENABLED) TextButton(onClick = { onDisableWorkflow(workflow) }) { Text("停用") }
                                    else TextButton(onClick = { onEnableWorkflow(workflow) }, enabled = workflow.canEnable) { Text("启用") }
                                    TextButton(onClick = { pendingWorkflowDelete = workflow }) { Text("删除") }
                                }
                            }
                        }
                    }
                    if (workflows.isEmpty()) Text("暂无 Workflow 草稿", color = QadbColors.textSecondary)
                } else {
                    knowledgeCards.forEach { card ->
                        Surface(color = QadbColors.surfaceVariant, shape = RoundedCornerShape(UiTokens.RadiusMedium)) {
                            Column(modifier = Modifier.padding(UiTokens.SpaceMedium)) {
                                Text(card.title, fontWeight = FontWeight.SemiBold)
                                Text(card.packageName, color = QadbColors.textSecondary, fontSize = UiTokens.TextCaption)
                                Text(card.guide, color = QadbColors.textSecondary, fontSize = UiTokens.TextCaption, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                Row {
                                    TextButton(onClick = { editing = card }) { Text("编辑") }
                                    TextButton(onClick = { onSetKnowledgeEnabled(card, !card.enabled) }) { Text(if (card.enabled) "停用" else "启用") }
                                    TextButton(onClick = { pendingKnowledgeDelete = card }) { Text("删除") }
                                }
                            }
                        }
                    }
                    if (knowledgeCards.isEmpty()) Text("暂无应用知识卡", color = QadbColors.textSecondary)
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("完成") } }
    )
    editing?.let { card -> KnowledgeCardEditor(card, onSave = { onSaveKnowledge(it); editing = null }, onDismiss = { editing = null }) }
    pendingWorkflowDelete?.let { workflow -> ConfirmDeleteDialog("删除该 Workflow？", { onDeleteWorkflow(workflow); pendingWorkflowDelete = null }) { pendingWorkflowDelete = null } }
    pendingKnowledgeDelete?.let { card -> ConfirmDeleteDialog("删除该应用知识卡？", { onDeleteKnowledge(card); pendingKnowledgeDelete = null }) { pendingKnowledgeDelete = null } }
}

@Composable
private fun KnowledgeCardEditor(card: AgentAppKnowledgeCard, onSave: (AgentAppKnowledgeCard) -> Unit, onDismiss: () -> Unit) {
    var packageName by remember(card.id) { mutableStateOf(card.packageName) }
    var title by remember(card.id) { mutableStateOf(card.title) }
    var guide by remember(card.id) { mutableStateOf(card.guide) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("应用知识卡") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceSmall)) {
            KnowledgeField("包名", packageName) { packageName = it }
            KnowledgeField("标题", title) { title = it }
            KnowledgeField("指南（最多 4000 字符）", guide, minLines = 5) { guide = it }
        }
    }, confirmButton = { Button(onClick = { onSave(card.copy(packageName = packageName.trim(), title = title.trim(), guide = guide.trim())) }, enabled = packageName.isNotBlank() && title.isNotBlank() && guide.isNotBlank()) { Text("保存") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}

@Composable
private fun KnowledgeField(label: String, value: String, minLines: Int = 1, onValueChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = UiTokens.TextCaption, color = QadbColors.textSecondary)
        BasicTextField(value = value, onValueChange = onValueChange, minLines = minLines, modifier = Modifier.fillMaxWidth().border(BorderStroke(1.dp, QadbColors.border), RoundedCornerShape(UiTokens.RadiusSmall)).padding(UiTokens.SpaceSmall), textStyle = MaterialTheme.typography.body1.copy(color = QadbColors.textPrimary))
    }
}

@Composable
private fun ConfirmDeleteDialog(message: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("确认删除") }, text = { Text(message) }, confirmButton = { Button(onClick = onConfirm) { Text("删除") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
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
    configurationReady: Boolean,
    connected: Boolean,
    errorMessage: String?,
    budgetStatus: AgentBudgetStatus,
    onOpenSettings: () -> Unit,
    onOpenDevices: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    LaunchedEffect(messages.size, errorMessage) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(vertical = UiTokens.SpaceXLarge),
        verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceMedium)
    ) {
        if (!configurationReady) {
            AgentGuideCard(
                icon = IconParkIcons.Setting,
                title = stringResource(Res.string.agent_config_required),
                description = stringResource(Res.string.agent_config_required_desc),
                action = stringResource(Res.string.agent_open_settings),
                onAction = onOpenSettings
            )
        } else if (!connected) {
            AgentGuideCard(
                icon = IconParkIcons.Phone,
                title = stringResource(Res.string.agent_no_device),
                description = stringResource(Res.string.agent_no_device_desc),
                action = stringResource(Res.string.agent_open_devices),
                onAction = onOpenDevices
            )
        } else if (messages.isEmpty()) {
            AgentGuideCard(
                icon = IconParkIcons.Command,
                title = stringResource(Res.string.agent_ready_title),
                description = stringResource(Res.string.agent_ready_desc)
            )
        }

        messages.forEach { message ->
            AgentMessageBubble(message)
        }

        AgentBudgetCard(budgetStatus)

        errorMessage?.let {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(UiTokens.RadiusLarge),
                color = QadbColors.errorSurface
            ) {
                Text(
                    text = it,
                    modifier = Modifier.padding(UiTokens.SpaceLarge),
                    color = QadbColors.danger,
                    fontSize = UiTokens.TextBody
                )
            }
        }
    }
}

@Composable
private fun AgentBudgetCard(status: AgentBudgetStatus) {
    if (status.modelCalls == 0 && status.stopReason == null) return
    AgentRuntimeDetail(
        title = "任务预算",
        titleColor = if (status.stopReason == null) QadbColors.textSecondary else QadbColors.danger
    ) {
        Text(
            "模型 ${status.modelCalls}/${status.modelCallLimit} · 视觉 ${status.visionCalls}/${status.visionCallLimit} · 重规划 ${status.replans}/${status.replanLimit}",
            color = QadbColors.textSecondary,
            fontSize = UiTokens.TextCaption
        )
        Text(
            "输入 ${status.usage.promptTokens} · 输出 ${status.usage.completionTokens} · 缓存 ${status.usage.cachedTokens} · 预计 ${"%.4f".format(status.estimatedCost)} CNY",
            color = QadbColors.textSecondary,
            fontSize = UiTokens.TextCaption
        )
        Text(
            budgetModeLabel(status.mode),
            color = if (status.stopReason == null) QadbColors.textTertiary else QadbColors.danger,
            fontSize = UiTokens.TextCaption
        )
        status.stopReason?.let { reason ->
            Text(reason, color = QadbColors.danger, fontSize = UiTokens.TextCaption)
        }
    }
}

@Composable
private fun AgentRuntimeDetail(
    title: String,
    titleColor: androidx.compose.ui.graphics.Color = QadbColors.textTertiary,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .widthIn(max = 680.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceXSmall)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(QadbColors.divider)
        )
        Text(
            title,
            color = titleColor,
            fontSize = UiTokens.TextCaption,
            fontWeight = FontWeight.Medium
        )
        content()
    }
}

private fun budgetModeLabel(mode: AgentBudgetMode): String = when (mode) {
    AgentBudgetMode.NORMAL -> "正常预算模式"
    AgentBudgetMode.DIFF_ONLY -> "已切换为页面差异优先"
    AgentBudgetMode.NO_OPTIONAL_VISION -> "已关闭非必要视觉请求"
    AgentBudgetMode.FINAL_RECOVERY_ONLY -> "仅保留最终结果调用"
    AgentBudgetMode.EXHAUSTED -> "预算已耗尽"
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
            Text(
                text = message.text,
                modifier = Modifier.widthIn(max = 680.dp),
                color = QadbColors.textPrimary,
                fontSize = UiTokens.TextBodyLarge
            )
        }
    }
}

@Composable
private fun QuickPromptRow(
    enabled: Boolean,
    onPrompt: (String) -> Unit
) {
    val quickPrompts = listOf(
        Triple(Res.string.agent_quick_wechat, Res.string.agent_prompt_wechat, IconParkIcons.Send),
        Triple(Res.string.agent_quick_settings, Res.string.agent_prompt_settings, IconParkIcons.Setting),
        Triple(Res.string.agent_quick_recognize, Res.string.agent_prompt_recognize, IconParkIcons.Search),
        Triple(Res.string.agent_quick_camera, Res.string.agent_prompt_camera, IconParkIcons.Camera),
        Triple(Res.string.agent_quick_clear_cache, Res.string.agent_prompt_clear_cache, IconParkIcons.StorageCard)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = UiTokens.SpaceSmall),
        horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceSmall)
    ) {
        quickPrompts.forEach { (label, prompt, icon) ->
            val promptText = stringResource(prompt)
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
    observationMode: AgentObservationMode,
    memoryEnabled: Boolean,
    memoryHitCount: Int,
    totalTokens: Int,
    compactionCount: Int,
    savedMemoryCount: Int,
    onSend: () -> Unit,
    onQuickPrompt: (String) -> Unit,
    onNewTask: () -> Unit,
    onCancel: () -> Unit,
    onOpenMemory: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val composerShape = RoundedCornerShape(20.dp)
    val borderColor = if (focused) {
        QadbColors.primary.copy(alpha = 0.62f)
    } else {
        QadbColors.border
    }
    val statusText = when {
        !configurationReady -> stringResource(Res.string.agent_composer_waiting_model)
        !connected -> stringResource(Res.string.agent_composer_waiting_device)
        running -> stringResource(Res.string.agent_running)
        observationMode == AgentObservationMode.TEXT_ONLY -> stringResource(Res.string.agent_text_mode)
        else -> stringResource(Res.string.agent_vision_mode)
    }
    val statusColor = when {
        running -> QadbColors.primary
        configurationReady && connected -> QadbColors.success
        else -> QadbColors.textTertiary
    }

    Surface(
        modifier = Modifier
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
                    .heightIn(min = 68.dp, max = 138.dp)
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

            QuickPromptRow(
                enabled = !running && configurationReady && connected,
                onPrompt = onQuickPrompt
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

                    Surface(
                        modifier = Modifier.clickable(onClick = onOpenMemory),
                        shape = RoundedCornerShape(UiTokens.BadgeRadius),
                        color = if (memoryEnabled) {
                            QadbColors.primaryContainer
                        } else {
                            QadbColors.surfaceVariant
                        }
                    ) {
                        Text(
                            text = when {
                                savedMemoryCount > 0 -> stringResource(
                                    Res.string.agent_saved_memory,
                                    savedMemoryCount
                                )
                                memoryEnabled -> stringResource(
                                    Res.string.agent_memory_status,
                                    memoryHitCount
                                )
                                else -> stringResource(Res.string.agent_memory_off)
                            },
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            color = if (memoryEnabled) QadbColors.primary else QadbColors.textTertiary,
                            fontSize = 11.sp,
                            maxLines = 1
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
