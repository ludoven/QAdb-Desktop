package com.ludoven.adbtool.pages

import com.ludoven.adbtool.ui.mac.*

import adbtool_desktop.composeapp.generated.resources.Res
import adbtool_desktop.composeapp.generated.resources.apk_not_selected
import adbtool_desktop.composeapp.generated.resources.confirm
import adbtool_desktop.composeapp.generated.resources.tip_title
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ScreenSearchDesktop
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Wifi
import com.ludoven.adbtool.ui.mac.AlertDialog
import com.ludoven.adbtool.ui.mac.Card
import com.ludoven.adbtool.ui.mac.CardDefaults
import com.ludoven.adbtool.ui.mac.Icon
import com.ludoven.adbtool.ui.mac.MaterialTheme
import com.ludoven.adbtool.ui.mac.OutlinedTextField
import com.ludoven.adbtool.ui.mac.OutlinedTextFieldDefaults
import com.ludoven.adbtool.ui.mac.Scaffold
import com.ludoven.adbtool.ui.mac.Text
import com.ludoven.adbtool.ui.mac.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ludoven.adbtool.entity.AdbFunctionType
import com.ludoven.adbtool.entity.MsgContent
import com.ludoven.adbtool.util.AdbTool
import com.ludoven.adbtool.util.l10n
import com.ludoven.adbtool.viewmodel.CommonModel
import com.ludoven.adbtool.widget.GlassCard
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

private enum class CommandTrigger {
    DIRECT,
    PACKAGE_INPUT,
    SHELL_INPUT
}

private data class CommandCategoryUi(
    val key: String,
    val label: String,
    val icon: ImageVector
)

private data class CommandItemUi(
    val id: String,
    val title: String,
    val description: String,
    val commandPreview: String,
    val categoryKey: String,
    val icon: ImageVector,
    val iconTint: Color,
    val trigger: CommandTrigger,
    val actionType: AdbFunctionType?,
    val shellCommand: String? = null,
    val hints: List<String> = emptyList()
)

private fun displayCommandTitle(command: CommandItemUi): String {
    return if (command.categoryKey == "tv" && command.title.startsWith("TV ")) {
        command.title.removePrefix("TV ").trim()
    } else {
        command.title
    }
}

@Composable
@Preview
fun CommonScreen(viewModel: CommonModel) {
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val showDialog by viewModel.showDialog.collectAsState()
    val dialogMsg by viewModel.dialogMessage.collectAsState()

    val categories = remember {
        listOf(
            CommandCategoryUi("all", l10n("全部", "All"), Icons.Default.Tune),
            CommandCategoryUi("device", l10n("设备", "Device"), Icons.Default.Home),
            CommandCategoryUi("app", l10n("应用管理", "Apps"), Icons.Default.InstallMobile),
            CommandCategoryUi("file", l10n("文件操作", "Files"), Icons.Default.Folder),
            CommandCategoryUi("input", l10n("输入控制", "Input"), Icons.Default.Edit),
            CommandCategoryUi("screen", l10n("截图录屏", "Screen"), Icons.Default.PhotoCamera),
            CommandCategoryUi("network", l10n("网络调试", "Network"), Icons.Default.Wifi),
            CommandCategoryUi("log", l10n("日志", "Logs"), Icons.Default.Memory),
            CommandCategoryUi("tv", l10n("TV盒子", "TV"), Icons.Default.ScreenSearchDesktop)
        )
    }

    val allCommands = rememberCommandLibrary()

    var selectedCategory by remember { mutableStateOf("all") }
    var searchKeyword by remember { mutableStateOf("") }
    var selectedCommandId by remember { mutableStateOf(allCommands.firstOrNull()?.id.orEmpty()) }
    var packageNameInput by remember { mutableStateOf("com.example.app") }
    var shellCommandInput by remember { mutableStateOf("getprop") }
    var textInput by remember { mutableStateOf("hello") }
    var executionResult by remember { mutableStateOf(l10n("等待执行", "Ready")) }
    var showRebootConfirm by remember { mutableStateOf(false) }
    var pendingRebootCommand by remember { mutableStateOf<CommandItemUi?>(null) }

    val filteredCommands by remember(allCommands, selectedCategory, searchKeyword) {
        derivedStateOf {
            val keyword = searchKeyword.trim().lowercase()
            allCommands.filter { command ->
                val matchCategory = selectedCategory == "all" || command.categoryKey == selectedCategory
                val matchKeyword = keyword.isBlank() ||
                    command.title.lowercase().contains(keyword) ||
                    command.description.lowercase().contains(keyword) ||
                    command.commandPreview.lowercase().contains(keyword)
                matchCategory && matchKeyword
            }
        }
    }

    LaunchedEffect(filteredCommands, selectedCommandId) {
        if (filteredCommands.isEmpty()) {
            selectedCommandId = ""
        } else if (filteredCommands.none { it.id == selectedCommandId }) {
            selectedCommandId = filteredCommands.first().id
        }
    }

    val selectedCommand = filteredCommands.firstOrNull { it.id == selectedCommandId }
        ?: allCommands.firstOrNull { it.id == selectedCommandId }
        ?: filteredCommands.firstOrNull()
        ?: allCommands.first()

    val resolvedCommandPreview by remember(selectedCommand, packageNameInput, shellCommandInput, textInput) {
        derivedStateOf {
            when (selectedCommand.trigger) {
                CommandTrigger.PACKAGE_INPUT -> {
                    val pkg = packageNameInput.trim().ifEmpty { "com.example.app" }
                    when (selectedCommand.actionType) {
                        AdbFunctionType.LAUNCH_APP_BY_PACKAGE ->
                            "adb shell monkey -p $pkg -c android.intent.category.LAUNCHER 1"
                        AdbFunctionType.STOP_APP_BY_PACKAGE ->
                            "adb shell am force-stop $pkg"
                        AdbFunctionType.CLEAR_CACHE_AND_RESTART ->
                            "adb shell pm clear $pkg && adb shell monkey -p $pkg -c android.intent.category.LAUNCHER 1"
                        else -> selectedCommand.commandPreview
                    }
                }

                CommandTrigger.SHELL_INPUT -> {
                    val shell = shellCommandInput.trim().ifEmpty { "getprop" }
                    "adb shell $shell"
                }

                CommandTrigger.DIRECT -> {
                    if (selectedCommand.actionType == AdbFunctionType.INPUT_TEXT) {
                        val text = textInput.ifEmpty { "hello" }
                        "adb shell input text \"$text\""
                    } else {
                        selectedCommand.commandPreview
                    }
                }
            }
        }
    }

    fun copyCommand(command: String) {
        clipboardManager.setText(AnnotatedString(command))
    }

    fun executeCommand(command: CommandItemUi) {
        if (AdbTool.selectDeviceId.isNullOrBlank()) {
            executionResult = l10n("执行失败：未选择设备", "Failed: no device selected")
            return
        }
        executionResult = l10n("执行中...", "Running...")

        coroutineScope.launch {
            runCatching {
                when (command.trigger) {
                    CommandTrigger.PACKAGE_INPUT -> {
                        val pkg = packageNameInput.trim()
                        if (pkg.isBlank()) {
                            executionResult = l10n("执行失败：包名不能为空", "Failed: package name cannot be empty")
                            return@launch
                        }
                        val success = withContext(Dispatchers.IO) {
                            when (command.actionType) {
                                AdbFunctionType.LAUNCH_APP_BY_PACKAGE -> AdbTool.startApp(pkg)
                                AdbFunctionType.STOP_APP_BY_PACKAGE -> AdbTool.stopApp(pkg)
                                AdbFunctionType.CLEAR_CACHE_AND_RESTART -> {
                                    val cleared = AdbTool.clearAppData(pkg)
                                    cleared && AdbTool.startApp(pkg)
                                }
                                else -> false
                            }
                        }
                        executionResult = if (success) {
                            "${l10n("执行成功", "Success")}：$resolvedCommandPreview"
                        } else {
                            "${l10n("执行失败", "Failed")}：$resolvedCommandPreview"
                        }
                    }

                    CommandTrigger.SHELL_INPUT -> {
                        val shell = shellCommandInput.trim()
                        if (shell.isBlank()) {
                            executionResult = l10n("执行失败：Shell 命令不能为空", "Failed: shell command cannot be empty")
                            return@launch
                        }
                        val output = withContext(Dispatchers.IO) {
                            AdbTool.execShell(shell)
                        }
                        executionResult = output.ifBlank { l10n("执行完成，无输出", "Done, no output") }
                    }

                    CommandTrigger.DIRECT -> {
                        if (!command.shellCommand.isNullOrBlank()) {
                            val output = withContext(Dispatchers.IO) {
                                AdbTool.execShell(command.shellCommand)
                            }
                            executionResult = output.ifBlank { l10n("执行完成，无输出", "Done, no output") }
                            return@launch
                        }

                        when (command.actionType) {
                            AdbFunctionType.INPUT_TEXT -> {
                                val input = textInput
                                if (input.isBlank()) {
                                    executionResult = l10n("执行失败：输入文本不能为空", "Failed: input text cannot be empty")
                                    return@launch
                                }
                                val success = withContext(Dispatchers.IO) {
                                    AdbTool.inputText(input)
                                }
                                executionResult = if (success) {
                                    "${l10n("执行成功", "Success")}：$resolvedCommandPreview"
                                } else {
                                    "${l10n("执行失败", "Failed")}：$resolvedCommandPreview"
                                }
                            }

                            AdbFunctionType.VIEW_CURRENT_ACTIVITY -> {
                                val output = withContext(Dispatchers.IO) {
                                    AdbTool.execShell("dumpsys window | grep mCurrentFocus")
                                }
                                executionResult = output.ifBlank { l10n("执行完成，无输出", "Done, no output") }
                            }

                            AdbFunctionType.KEY_BACK -> {
                                withContext(Dispatchers.IO) { AdbTool.execShell("input keyevent 4") }
                                executionResult = "${l10n("执行成功", "Success")}：$resolvedCommandPreview"
                            }

                            AdbFunctionType.KEY_HOME -> {
                                withContext(Dispatchers.IO) { AdbTool.execShell("input keyevent 3") }
                                executionResult = "${l10n("执行成功", "Success")}：$resolvedCommandPreview"
                            }

                            AdbFunctionType.NETWORK_STATUS -> {
                                val output = withContext(Dispatchers.IO) { AdbTool.execShell("dumpsys connectivity") }
                                executionResult = output.ifBlank { l10n("执行完成，无输出", "Done, no output") }
                            }

                            AdbFunctionType.REBOOT_DEVICE -> {
                                showRebootConfirm = true
                                pendingRebootCommand = command
                                executionResult = l10n("等待确认...", "Awaiting confirmation...")
                                return@launch
                            }

                            AdbFunctionType.DEVELOPER_OPTIONS -> {
                                withContext(Dispatchers.IO) {
                                    AdbTool.execShell("am start -a android.settings.APPLICATION_DEVELOPMENT_SETTINGS")
                                }
                                executionResult = "${l10n("执行成功", "Success")}：$resolvedCommandPreview"
                            }

                            else -> {
                                command.actionType?.let(viewModel::executeAdbAction)
                                executionResult = l10n(
                                    "已触发执行：$resolvedCommandPreview\n具体结果请查看提示弹窗",
                                    "Triggered: $resolvedCommandPreview\nSee toast/tip for details"
                                )
                            }
                        }
                    }
                }
            }.onFailure { error ->
                executionResult = l10n("执行异常", "Error") + "：${error.message ?: l10n("未知错误", "Unknown error")}"
            }
        }
    }

    Scaffold(containerColor = Color.Transparent) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CommandCenterHeader(
                searchKeyword = searchKeyword,
                onSearchKeywordChange = { searchKeyword = it }
            )

            CategoryTabs(
                categories = categories,
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CommandLibraryPanel(
                    commands = filteredCommands,
                    selectedCommandId = selectedCommandId,
                    onSelectCommand = { selectedCommandId = it.id },
                    modifier = Modifier.weight(1f)
                )

                CommandDetailPanel(
                    selectedCommand = selectedCommand,
                    resolvedCommandPreview = resolvedCommandPreview,
                    packageNameInput = packageNameInput,
                    onPackageNameInputChange = { packageNameInput = it },
                    shellCommandInput = shellCommandInput,
                    onShellCommandInputChange = { shellCommandInput = it },
                    textInput = textInput,
                    onTextInputChange = { textInput = it },
                    executionResult = executionResult,
                    onCopyCommand = ::copyCommand,
                    onExecuteCommand = ::executeCommand,
                    modifier = Modifier.weight(2f)
                )
            }
        }
    }

    if (showRebootConfirm) {
        AlertDialog(
            onDismissRequest = {
                showRebootConfirm = false
                pendingRebootCommand = null
            },
            title = { Text(l10n("确认重启设备", "Confirm Device Reboot")) },
            text = {
                Text(
                    l10n(
                        "即将重启连接的设备，设备上的未保存数据可能丢失。是否继续？",
                        "The connected device will reboot. Unsaved data may be lost. Continue?"
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val rebootShell = pendingRebootCommand?.shellCommand ?: "reboot"
                    val rebootPreview = pendingRebootCommand?.commandPreview ?: "adb shell reboot"
                    showRebootConfirm = false
                    executionResult = l10n("执行中...", "Running...")
                    coroutineScope.launch {
                        runCatching {
                            withContext(Dispatchers.IO) { AdbTool.execShell(rebootShell) }
                        }.onSuccess {
                            executionResult = "${l10n("执行成功", "Success")}：$rebootPreview"
                        }.onFailure {
                            executionResult = "${l10n("执行失败", "Failed")}：${it.message}"
                        }
                        pendingRebootCommand = null
                    }
                }) { Text(l10n("确认重启", "Reboot")) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRebootConfirm = false
                    pendingRebootCommand = null
                    executionResult = l10n("已取消", "Cancelled")
                }) {
                    Text(l10n("取消", "Cancel"))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    val latestDialogText = dialogMsg?.let {
        when (it) {
            is MsgContent.Resource -> stringResource(it.stringResource, *it.args.toTypedArray())
            is MsgContent.Text -> it.text
        }
    }
    LaunchedEffect(showDialog, latestDialogText) {
        if (showDialog && !latestDialogText.isNullOrBlank()) {
            executionResult = latestDialogText
        }
    }

    if (showDialog) {
        dialogMsg?.let {
            val dialogText = when (it) {
                is MsgContent.Resource -> stringResource(it.stringResource, *it.args.toTypedArray())
                is MsgContent.Text -> it.text
            }
            val showAsToast = it is MsgContent.Resource && it.stringResource == Res.string.apk_not_selected

            if (showAsToast) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.inverseSurface,
                        shadowElevation = 6.dp,
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        Text(
                            text = dialogText,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                TipDialog(dialogText = dialogText) {
                    viewModel.dismissTipDialog()
                }
            }
        }
    }

}

@Composable
private fun rememberCommandLibrary(): List<CommandItemUi> {
    return remember {
        loadCommandLibraryFromConfig().ifEmpty { fallbackCommands() }
    }
}

private fun loadCommandLibraryFromConfig(): List<CommandItemUi> {
    val fileName = if (l10n("zh", "en") == "en") "adb_commands_en.json" else "adb_commands.json"
    val stream = Thread.currentThread().contextClassLoader.getResourceAsStream(fileName)
        ?: return emptyList()

    val jsonText = InputStreamReader(stream).use { it.readText() }
    val root = runCatching { Json.parseToJsonElement(jsonText).jsonArray }.getOrElse { return emptyList() }

    return root.mapNotNull { entry ->
        val obj = entry.jsonObject
        val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
        val title = obj["title"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
        val description = obj["description"]?.jsonPrimitive?.contentOrNull ?: ""
        val commandPreview = obj["commandPreview"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
        val categoryKey = obj["category"]?.jsonPrimitive?.contentOrNull ?: "device"
        val iconKey = obj["icon"]?.jsonPrimitive?.contentOrNull ?: "info"
        val color = parseColor(obj["color"]?.jsonPrimitive?.contentOrNull)
        val trigger = obj["trigger"]?.jsonPrimitive?.contentOrNull
            ?.let { runCatching { CommandTrigger.valueOf(it) }.getOrNull() }
            ?: CommandTrigger.DIRECT
        val actionType = obj["actionType"]?.jsonPrimitive?.contentOrNull
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { AdbFunctionType.valueOf(it) }.getOrNull() }
        val shellCommand = obj["shellCommand"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
        val hints = obj["hints"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()

        CommandItemUi(
            id = id,
            title = title,
            description = description,
            commandPreview = commandPreview,
            categoryKey = categoryKey,
            icon = resolveIcon(iconKey),
            iconTint = color,
            trigger = trigger,
            actionType = actionType,
            shellCommand = shellCommand,
            hints = hints
        )
    }
}

private fun parseColor(raw: String?): Color {
    if (raw.isNullOrBlank()) return Color(0xFF3D73FF)
    return runCatching {
        val hex = raw.removePrefix("#")
        val value = when (hex.length) {
            6 -> 0xFF000000 or hex.toLong(16)
            8 -> hex.toLong(16)
            else -> 0xFF3D73FF
        }
        Color(value)
    }.getOrDefault(Color(0xFF3D73FF))
}

private fun resolveIcon(iconKey: String): ImageVector = when (iconKey) {
    "visibility" -> Icons.Default.Visibility
    "install" -> Icons.Default.InstallMobile
    "play" -> Icons.Default.PlayArrow
    "stop" -> Icons.Default.Stop
    "delete" -> Icons.Default.DeleteSweep
    "code" -> Icons.Default.Code
    "edit" -> Icons.Default.Edit
    "back" -> Icons.Default.Close
    "home" -> Icons.Default.Home
    "camera" -> Icons.Default.PhotoCamera
    "screen" -> Icons.Default.ScreenSearchDesktop
    "log" -> Icons.Default.Memory
    "folder" -> Icons.Default.Folder
    "wifi" -> Icons.Default.Wifi
    "reboot" -> Icons.Default.RestartAlt
    else -> Icons.Default.Info
}

private fun categoryIconFor(categoryKey: String): ImageVector = when (categoryKey) {
    "device" -> Icons.Default.Home
    "app" -> Icons.Default.InstallMobile
    "file" -> Icons.Default.Folder
    "input" -> Icons.Default.Edit
    "screen" -> Icons.Default.PhotoCamera
    "network" -> Icons.Default.Wifi
    "log" -> Icons.Default.Memory
    "tv" -> Icons.Default.ScreenSearchDesktop
    else -> Icons.Default.Info
}

private fun commandItemAccentColor(index: Int): Color {
    val palette = listOf(
        Color(0xFF3D73FF),
        Color(0xFF22B573),
        Color(0xFFFF9F43),
        Color(0xFF9166FF),
        Color(0xFF25AFC8),
        Color(0xFFFF6B6B)
    )
    return palette[index % palette.size]
}

private fun fallbackCommands(): List<CommandItemUi> = listOf(
    CommandItemUi(
        id = "fallback_shell",
        title = l10n("执行 Shell 命令", "Run Shell Command"),
        description = l10n("配置文件读取失败时的回退命令", "Fallback command when config loading fails"),
        commandPreview = "adb shell getprop",
        categoryKey = "device",
        icon = Icons.Default.Code,
        iconTint = Color(0xFF3D73FF),
        trigger = CommandTrigger.SHELL_INPUT,
        actionType = AdbFunctionType.OPEN_SHELL
    )
)

@Composable
private fun CommandCenterHeader(
    searchKeyword: String,
    onSearchKeywordChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = l10n("命令中心", "Command Center"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = l10n("浏览常用 ADB 命令，快速复制并执行", "Browse common ADB commands and run quickly"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        OutlinedTextField(
            value = searchKeyword,
            onValueChange = onSearchKeywordChange,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            singleLine = true,
            placeholder = { Text(l10n("搜索命令", "Search commands")) },
            textStyle = MaterialTheme.typography.bodySmall,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
            ),
            modifier = Modifier
                .widthIn(min = 420.dp, max = 520.dp)
                .heightIn(min = 52.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryTabs(
    categories: List<CommandCategoryUi>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(0.dp, Color.Transparent),
        modifier = Modifier.fillMaxWidth()
    ) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = Int.MAX_VALUE
        ) {
            categories.forEach { category ->
                val selected = category.key == selectedCategory
                val chipBg = if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                } else {
                    Color.Transparent
                }
                val chipColor = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }

                Row(
                    modifier = Modifier
                        .background(chipBg, RoundedCornerShape(10.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onCategorySelected(category.key) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = category.label,
                        tint = chipColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = category.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = chipColor,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun CommandLibraryPanel(
    commands: List<CommandItemUi>,
    selectedCommandId: String,
    onSelectCommand: (CommandItemUi) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = l10n("命令库（共 ${commands.size} 条）", "Command library (${commands.size})"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            val listState = rememberLazyListState()
            Box(modifier = Modifier.fillMaxSize()) {
                if (commands.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = l10n("未找到匹配命令", "No matching commands"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(bottom = 4.dp),
                        modifier = Modifier.fillMaxSize().padding(end = 8.dp)
                    ) {
                        itemsIndexed(commands, key = { _, item -> item.id }) { index, command ->
                            CommandListItem(
                                command = command,
                                itemIndex = index,
                                selected = selectedCommandId == command.id,
                                onSelect = { onSelectCommand(command) }
                            )
                        }
                    }
                }

                VerticalScrollbar(
                    adapter = rememberScrollbarAdapter(listState),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                )
            }
        }
    }
}

@Composable
private fun CommandListItem(
    command: CommandItemUi,
    itemIndex: Int,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val accent = commandItemAccentColor(itemIndex)
    val categoryIcon = categoryIconFor(command.categoryKey)
    val borderColor = if (selected) {
        accent.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    }
    val containerColor = if (selected) {
        accent.copy(alpha = 0.09f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(containerColor, RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onSelect() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    color = accent.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = categoryIcon,
                contentDescription = command.title,
                tint = accent,
                modifier = Modifier.size(17.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = displayCommandTitle(command),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = command.description,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CommandDetailPanel(
    selectedCommand: CommandItemUi,
    resolvedCommandPreview: String,
    packageNameInput: String,
    onPackageNameInputChange: (String) -> Unit,
    shellCommandInput: String,
    onShellCommandInputChange: (String) -> Unit,
    textInput: String,
    onTextInputChange: (String) -> Unit,
    executionResult: String,
    onCopyCommand: (String) -> Unit,
    onExecuteCommand: (CommandItemUi) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = l10n("当前命令", "Current command"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        RoundedCornerShape(12.dp)
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(selectedCommand.iconTint.copy(alpha = 0.14f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = selectedCommand.icon,
                        contentDescription = selectedCommand.title,
                        tint = selectedCommand.iconTint
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = displayCommandTitle(selectedCommand),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = selectedCommand.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(0.dp, Color.Transparent)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = l10n("命令预览", "Command preview"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CommandActionButton(
                                text = l10n("复制", "Copy"),
                                icon = Icons.Default.ContentCopy,
                                onClick = { onCopyCommand(resolvedCommandPreview) }
                            )
                            CommandActionButton(
                                text = l10n("运行", "Run"),
                                icon = Icons.Default.PlayArrow,
                                onClick = { onExecuteCommand(selectedCommand) },
                                primary = true
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onCopyCommand(resolvedCommandPreview) }
                            .padding(10.dp)
                    ) {
                        Text(
                            text = resolvedCommandPreview,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            when (selectedCommand.trigger) {
                CommandTrigger.PACKAGE_INPUT -> {
                    OutlinedTextField(
                        value = packageNameInput,
                        onValueChange = onPackageNameInputChange,
                        singleLine = true,
                        label = { Text(l10n("应用包名", "Package name")) },
                        placeholder = { Text("com.example.app") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                CommandTrigger.SHELL_INPUT -> {
                    OutlinedTextField(
                        value = shellCommandInput,
                        onValueChange = onShellCommandInputChange,
                        singleLine = true,
                        label = { Text(l10n("Shell 命令", "Shell command")) },
                        placeholder = { Text("getprop") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                CommandTrigger.DIRECT -> {
                    if (selectedCommand.actionType == AdbFunctionType.INPUT_TEXT) {
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = onTextInputChange,
                            singleLine = true,
                            label = { Text(l10n("输入文本", "Input text")) },
                            placeholder = { Text("hello") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(0.dp, Color.Transparent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = l10n("执行结果", "Execution result"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        CommandActionButton(
                            text = l10n("复制结果", "Copy result"),
                            icon = Icons.Default.ContentCopy,
                            onClick = { onCopyCommand(executionResult) }
                        )
                    }
                    val resultScroll = rememberScrollState()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 80.dp, max = 300.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .verticalScroll(resultScroll)
                    ) {
                        SelectionContainer {
                            Text(
                                text = executionResult,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommandActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false
) {
    val contentColor = if (primary) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val backgroundColor = if (primary) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.68f)
    }
    val borderColor = if (primary) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)
    }

    TextButton(
        onClick = onClick,
        modifier = modifier
            .height(32.dp)
            .background(backgroundColor, RoundedCornerShape(10.dp))
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 0.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = text,
                color = contentColor,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun TipDialog(dialogText: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.tip_title)) },
        text = { Text(dialogText) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.confirm))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}
