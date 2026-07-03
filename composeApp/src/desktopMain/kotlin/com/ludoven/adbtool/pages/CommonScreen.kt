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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ScreenSearchDesktop
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ludoven.adbtool.entity.AdbFunctionType
import com.ludoven.adbtool.entity.MsgContent
import com.ludoven.adbtool.util.AdbTool
import com.ludoven.adbtool.util.CommandFavoritesManager
import com.ludoven.adbtool.util.CustomCommand
import com.ludoven.adbtool.util.CustomCommandManager
import com.ludoven.adbtool.util.copyPlainTextToClipboard
import com.ludoven.adbtool.util.l10n
import com.ludoven.adbtool.viewmodel.CommonModel
import com.ludoven.adbtool.widget.InlineStatusBanner
import com.ludoven.adbtool.widget.InlineStatusTone
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

internal fun commonCommandRunEnabled(selectedDevice: String?): Boolean =
    !selectedDevice.isNullOrBlank()

@Composable
@Preview
fun CommonScreen(
    viewModel: CommonModel,
    selectedDevice: String? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val showDialog by viewModel.showDialog.collectAsState()
    val dialogMsg by viewModel.dialogMessage.collectAsState()

    var favoriteIds by remember { mutableStateOf(CommandFavoritesManager.load()) }
    var customCommands by remember { mutableStateOf(CustomCommandManager.load()) }
    var showAddCommandDialog by remember { mutableStateOf(false) }
    var editingCommand by remember { mutableStateOf<CustomCommand?>(null) }

    val categories = remember(favoriteIds) {
        listOf(
            CommandCategoryUi("all", l10n("全部", "All"), Icons.Default.Tune),
            CommandCategoryUi("favorites", l10n("收藏", "Favorites"), Icons.Default.Bookmark),
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

    val mergedCommands = remember(allCommands, customCommands) {
        val builtin = allCommands
        val custom = customCommands.map { cmd ->
            CommandItemUi(
                id = cmd.id,
                title = cmd.title,
                description = cmd.description,
                commandPreview = cmd.commandPreview,
                categoryKey = cmd.categoryKey,
                icon = Icons.Default.Code,
                iconTint = Color(0xFF3D73FF),
                trigger = if (cmd.shellCommand.isNotBlank()) CommandTrigger.SHELL_INPUT else CommandTrigger.DIRECT,
                actionType = null,
                shellCommand = cmd.shellCommand.takeIf { it.isNotBlank() },
                hints = emptyList()
            )
        }
        builtin + custom
    }

    var selectedCategory by remember { mutableStateOf("all") }
    var searchKeyword by remember { mutableStateOf("") }
    var selectedCommandId by remember { mutableStateOf(mergedCommands.firstOrNull()?.id.orEmpty()) }
    var packageNameInput by remember { mutableStateOf("com.example.app") }
    var shellCommandInput by remember { mutableStateOf("getprop") }
    var textInput by remember { mutableStateOf("hello") }
    var executionResult by remember { mutableStateOf(l10n("等待执行", "Ready")) }
    var showRebootConfirm by remember { mutableStateOf(false) }
    var pendingRebootCommand by remember { mutableStateOf<CommandItemUi?>(null) }

    val filteredCommands by remember(mergedCommands, selectedCategory, searchKeyword, favoriteIds) {
        derivedStateOf {
            val keyword = searchKeyword.trim().lowercase()
            mergedCommands.filter { command ->
                val matchCategory = when (selectedCategory) {
                    "all" -> true
                    "favorites" -> command.id in favoriteIds
                    else -> command.categoryKey == selectedCategory
                }
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
        ?: mergedCommands.firstOrNull { it.id == selectedCommandId }
        ?: filteredCommands.firstOrNull()
        ?: mergedCommands.first()

    val resolvedCommandPreview by remember(selectedCommand, selectedDevice, packageNameInput, shellCommandInput, textInput) {
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
                            .replace("{package}", pkg)
                            .replace("{text}", textInput.trim())
                    }
                }

                CommandTrigger.SHELL_INPUT -> {
                    if (!selectedCommand.shellCommand.isNullOrBlank()) {
                        "adb shell ${selectedCommand.shellCommand
                            .replace("{package}", packageNameInput.trim())
                            .replace("{text}", textInput.trim())}"
                    } else {
                        val shell = shellCommandInput.trim().ifEmpty { "getprop" }
                        "adb shell $shell"
                    }
                }

                CommandTrigger.DIRECT -> {
                    if (selectedCommand.actionType == AdbFunctionType.INPUT_TEXT) {
                        val text = textInput.ifEmpty { "hello" }
                        "adb shell input text \"$text\""
                    } else if (selectedCommand.id.startsWith("custom_")) {
                        resolvedCustomAdbPreview(
                            rawCommand = selectedCommand.commandPreview,
                            deviceId = selectedDevice,
                            packageName = packageNameInput,
                            text = textInput
                        )
                    } else {
                        selectedCommand.commandPreview
                            .replace("{package}", packageNameInput.trim())
                            .replace("{text}", textInput.trim())
                    }
                }
            }
        }
    }

    fun copyCommand(command: String) {
        copyPlainTextToClipboard(command)
    }

    fun executeCommand(command: CommandItemUi) {
        if (!commonCommandRunEnabled(selectedDevice)) {
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
                        val shell = (command.shellCommand ?: shellCommandInput.trim())
                            .replace("{package}", packageNameInput.trim())
                            .replace("{text}", textInput.trim())
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
                            val resolvedShell = command.shellCommand
                                .replace("{package}", packageNameInput.trim())
                                .replace("{text}", textInput.trim())
                            val output = withContext(Dispatchers.IO) {
                                AdbTool.execShell(resolvedShell)
                            }
                            executionResult = output.ifBlank { l10n("执行完成，无输出", "Done, no output") }
                            return@launch
                        }

                        when (command.actionType) {
                            null -> {
                                val args = customAdbArgs(
                                    rawCommand = command.commandPreview,
                                    deviceId = selectedDevice,
                                    packageName = packageNameInput,
                                    text = textInput
                                )
                                if (args.isEmpty()) {
                                    executionResult = l10n("执行失败：ADB 命令不能为空", "Failed: ADB command cannot be empty")
                                    return@launch
                                }
                                val result = withContext(Dispatchers.IO) {
                                    AdbTool.execAdbAsync(*args.toTypedArray())
                                }
                                executionResult = AdbTool.outputText(result).ifBlank {
                                    l10n("执行完成，无输出", "Done, no output")
                                }
                            }

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
                onSearchKeywordChange = { searchKeyword = it },
                categories = categories,
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it },
                onAddCommand = { showAddCommandDialog = true }
            )

            if (!commonCommandRunEnabled(selectedDevice)) {
                InlineStatusBanner(
                    text = l10n("当前没有选择设备。命令可以浏览和复制，但执行前需要先连接并选择设备。", "No device is selected. Commands can be browsed and copied, but running them needs a connected selected device."),
                    tone = InlineStatusTone.Warning,
                    icon = Icons.Default.Info
                )
            }

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CommandLibraryPanel(
                    commands = filteredCommands,
                    selectedCommandId = selectedCommandId,
                    favoriteIds = favoriteIds,
                    onSelectCommand = { selectedCommandId = it.id },
                    onToggleFavorite = { cmd ->
                        favoriteIds = CommandFavoritesManager.toggle(cmd.id)
                    },
                    onEditCustomCommand = { cmd ->
                        val cc = customCommands.firstOrNull { it.id == cmd.id }
                        if (cc != null) editingCommand = cc
                    },
                    onDeleteCustomCommand = { cmd ->
                        customCommands = CustomCommandManager.remove(cmd.id)
                        if (selectedCommandId == cmd.id) {
                            selectedCommandId = mergedCommands.firstOrNull { it.id != cmd.id }?.id.orEmpty()
                        }
                    },
                    modifier = Modifier.width(330.dp)
                )

                CommandDetailPanel(
                    selectedCommand = selectedCommand,
                    resolvedCommandPreview = resolvedCommandPreview,
                    selectedDevice = selectedDevice,
                    packageNameInput = packageNameInput,
                    onPackageNameInputChange = { packageNameInput = it },
                    shellCommandInput = shellCommandInput,
                    onShellCommandInputChange = { shellCommandInput = it },
                    textInput = textInput,
                    onTextInputChange = { textInput = it },
                    executionResult = executionResult,
                    onCopyCommand = ::copyCommand,
                    onClearResult = { executionResult = l10n("等待执行", "Ready") },
                    onExecuteCommand = ::executeCommand,
                    canRunCommand = commonCommandRunEnabled(selectedDevice),
                    modifier = Modifier.weight(1f)
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

    if (showAddCommandDialog || editingCommand != null) {
        CustomCommandDialog(
            initialCommand = editingCommand,
            onDismiss = {
                showAddCommandDialog = false
                editingCommand = null
            },
            onConfirm = { cmd ->
                val savedCommands = if (editingCommand != null) {
                    CustomCommandManager.update(cmd)
                } else {
                    CustomCommandManager.add(cmd)
                }
                if (editingCommand != null) {
                    customCommands = savedCommands
                    selectedCommandId = cmd.id
                } else {
                    customCommands = savedCommands
                    selectedCommandId = cmd.id
                    selectedCategory = "all"
                }
                showAddCommandDialog = false
                editingCommand = null
            }
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

private fun deviceDisplayLabel(selectedDevice: String?): String {
    return selectedDevice?.takeIf { it.isNotBlank() }
        ?: l10n("未选择设备", "No device selected")
}

internal fun customAdbArgs(
    rawCommand: String,
    deviceId: String?,
    packageName: String,
    text: String
): List<String> {
    val resolved = rawCommand
        .replace("{package}", packageName.trim())
        .replace("{text}", text.trim())
        .trim()
    if (resolved.isBlank()) return emptyList()

    val tokens = tokenizeCommandLine(resolved).let { parts ->
        if (parts.firstOrNull() == "adb") parts.drop(1) else parts
    }
    if (tokens.isEmpty()) return emptyList()

    val hasExplicitDevice = tokens.withIndex().any { (index, token) ->
        token == "-d" ||
            token == "-e" ||
            token == "--one-device" ||
            token == "--serial" ||
            token.startsWith("--serial=") ||
            (token == "-s" && index + 1 < tokens.size)
    }

    return if (!hasExplicitDevice && customAdbSubcommandUsesSelectedDevice(tokens) && !deviceId.isNullOrBlank()) {
        listOf("-s", deviceId.trim()) + tokens
    } else {
        tokens
    }
}

internal fun resolvedCustomAdbPreview(
    rawCommand: String,
    deviceId: String?,
    packageName: String,
    text: String
): String {
    val args = customAdbArgs(rawCommand, deviceId, packageName, text)
    return if (args.isEmpty()) {
        "adb"
    } else {
        "adb ${args.joinToString(" ")}"
    }
}

private fun customAdbSubcommandUsesSelectedDevice(args: List<String>): Boolean {
    val firstCommand = args.firstOrNull { !it.startsWith("-") } ?: return true
    return firstCommand !in setOf(
        "connect",
        "disconnect",
        "devices",
        "help",
        "kill-server",
        "reconnect",
        "start-server",
        "version"
    )
}

private fun tokenizeCommandLine(input: String): List<String> {
    val regex = Regex("\"([^\"]*)\"|'([^']*)'|([^\\s]+)")
    return regex.findAll(input)
        .mapNotNull { match ->
            match.groups[1]?.value
                ?: match.groups[2]?.value
                ?: match.groups[3]?.value
        }
        .toList()
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
    onSearchKeywordChange: (String) -> Unit,
    categories: List<CommandCategoryUi>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    onAddCommand: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = l10n("命令中心", "Command Center"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = l10n("浏览、收藏、编辑并执行常用 ADB 命令", "Browse, favorite, edit and run common ADB commands"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedTextField(
                value = searchKeyword,
                onValueChange = onSearchKeywordChange,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "⌘K",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                singleLine = true,
                placeholder = { Text(l10n("搜索命令、分类、包名或参数", "Search commands, categories, packages or args")) },
                textStyle = MaterialTheme.typography.bodyMedium,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .widthIn(min = 380.dp, max = 520.dp)
                    .heightIn(min = 44.dp)
            )

            CommandActionButton(
                text = l10n("新建命令", "New Command"),
                icon = Icons.Default.Add,
                onClick = onAddCommand,
                primary = true,
                modifier = Modifier.height(38.dp)
            )
        }

        CategoryTabs(
            categories = categories,
            selectedCategory = selectedCategory,
            onCategorySelected = onCategorySelected
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
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
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = Int.MAX_VALUE
    ) {
        categories.forEach { category ->
            val selected = category.key == selectedCategory
            val chipBg = if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            } else {
                Color.Transparent
            }
            val chipColor = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }

            Row(
                modifier = Modifier
                    .background(chipBg, RoundedCornerShape(9.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onCategorySelected(category.key) }
                    .padding(horizontal = 13.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
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

@Composable
private fun CommandLibraryPanel(
    commands: List<CommandItemUi>,
    selectedCommandId: String,
    favoriteIds: Set<String>,
    onSelectCommand: (CommandItemUi) -> Unit,
    onToggleFavorite: (CommandItemUi) -> Unit,
    onEditCustomCommand: (CommandItemUi) -> Unit,
    onDeleteCustomCommand: (CommandItemUi) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxHeight(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 2.dp, top = 4.dp, end = 14.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                val listState = rememberLazyListState()
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
                                isFavorite = command.id in favoriteIds,
                                onSelect = { onSelectCommand(command) },
                                onToggleFavorite = { onToggleFavorite(command) },
                                onEditCustom = if (command.id.startsWith("custom_")) {
                                    { onEditCustomCommand(command) }
                                } else null,
                                onDeleteCustom = if (command.id.startsWith("custom_")) {
                                    { onDeleteCustomCommand(command) }
                                } else null
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = l10n("更多命令...", "More commands..."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = l10n("共 ${commands.size} 条", "${commands.size} total"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
    }
}

@Composable
private fun CommandListItem(
    command: CommandItemUi,
    itemIndex: Int,
    selected: Boolean,
    isFavorite: Boolean,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEditCustom: (() -> Unit)? = null,
    onDeleteCustom: (() -> Unit)? = null
) {
    val accent = commandItemAccentColor(itemIndex)
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    }
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    var showContextMenu by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(containerColor, RoundedCornerShape(8.dp))
                .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onSelect() }
                .padding(horizontal = 9.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.82f))), RoundedCornerShape(7.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = command.icon,
                    contentDescription = command.title,
                    tint = Color.White,
                    modifier = Modifier.size(17.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
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

            com.ludoven.adbtool.ui.mac.IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(26.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = if (isFavorite) l10n("取消收藏", "Unfavorite") else l10n("收藏", "Favorite"),
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(17.dp)
                )
            }

            if (onEditCustom != null || onDeleteCustom != null) {
                com.ludoven.adbtool.ui.mac.IconButton(
                    onClick = { showContextMenu = true },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = l10n("更多操作", "More actions"),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        com.ludoven.adbtool.ui.mac.DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false }
        ) {
            if (onEditCustom != null) {
                com.ludoven.adbtool.ui.mac.DropdownMenuItem(
                    text = { Text(l10n("编辑命令", "Edit command")) },
                    onClick = {
                        showContextMenu = false
                        onEditCustom()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
            if (onDeleteCustom != null) {
                com.ludoven.adbtool.ui.mac.DropdownMenuItem(
                    text = { Text(l10n("删除命令", "Delete command")) },
                    onClick = {
                        showContextMenu = false
                        onDeleteCustom()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun CommandDetailPanel(
    selectedCommand: CommandItemUi,
    resolvedCommandPreview: String,
    selectedDevice: String?,
    packageNameInput: String,
    onPackageNameInputChange: (String) -> Unit,
    shellCommandInput: String,
    onShellCommandInputChange: (String) -> Unit,
    textInput: String,
    onTextInputChange: (String) -> Unit,
    executionResult: String,
    onCopyCommand: (String) -> Unit,
    onClearResult: () -> Unit,
    onExecuteCommand: (CommandItemUi) -> Unit,
    canRunCommand: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 4.dp, top = 4.dp, end = 2.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = l10n("命令详情", "Command Details"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CommandActionButton(
                        text = l10n("复制", "Copy"),
                        icon = Icons.Default.ContentCopy,
                        onClick = { onCopyCommand(resolvedCommandPreview) }
                    )
                    CommandActionButton(
                        text = l10n("运行命令", "Run"),
                        icon = Icons.Default.PlayArrow,
                        onClick = { onExecuteCommand(selectedCommand) },
                        enabled = canRunCommand,
                        primary = true
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(selectedCommand.iconTint, selectedCommand.iconTint.copy(alpha = 0.8f))
                                ),
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = selectedCommand.icon,
                            contentDescription = selectedCommand.title,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = displayCommandTitle(selectedCommand),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = selectedCommand.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = l10n("完整命令预览", "Full command preview"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f), RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onCopyCommand(resolvedCommandPreview) }
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "adb",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = resolvedCommandPreview.removePrefix("adb").trimStart(),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = l10n("复制命令", "Copy command"),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = when (selectedCommand.trigger) {
                            CommandTrigger.PACKAGE_INPUT -> l10n("应用包名（可编辑）", "Package name")
                            CommandTrigger.SHELL_INPUT -> l10n("Shell 参数（可编辑）", "Shell args")
                            CommandTrigger.DIRECT -> if (selectedCommand.actionType == AdbFunctionType.INPUT_TEXT) {
                                l10n("输入文本（可编辑）", "Input text")
                            } else {
                                l10n("命令参数", "Command args")
                            }
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    when (selectedCommand.trigger) {
                        CommandTrigger.PACKAGE_INPUT -> {
                            OutlinedTextField(
                                value = packageNameInput,
                                onValueChange = onPackageNameInputChange,
                                singleLine = true,
                                placeholder = { Text("com.example.app") },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 44.dp)
                            )
                        }

                        CommandTrigger.SHELL_INPUT -> {
                            OutlinedTextField(
                                value = shellCommandInput,
                                onValueChange = onShellCommandInputChange,
                                singleLine = true,
                                placeholder = { Text("getprop") },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 44.dp)
                            )
                        }

                        CommandTrigger.DIRECT -> {
                            if (selectedCommand.actionType == AdbFunctionType.INPUT_TEXT) {
                                OutlinedTextField(
                                    value = textInput,
                                    onValueChange = onTextInputChange,
                                    singleLine = true,
                                    placeholder = { Text("hello") },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 44.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = l10n("无需额外参数", "No extra args"),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = l10n("目标设备", "Target device"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .background(
                                        if (canRunCommand) Color(0xFF34C759) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                                        RoundedCornerShape(99.dp)
                                    )
                            )
                            Text(
                                text = deviceDisplayLabel(selectedDevice),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = l10n("执行结果", "Execution result"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = BorderStroke(1.dp, Color(0xFF1F2937)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .heightIn(min = 240.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Color(0xFF34C759), RoundedCornerShape(99.dp))
                                )
                                Text(
                                    text = l10n("执行状态", "Status"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF86EFAC),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TerminalActionButton(l10n("清空", "Clear"), Icons.Default.Delete, onClearResult)
                                TerminalActionButton(l10n("复制", "Copy"), Icons.Default.ContentCopy) {
                                    onCopyCommand(executionResult)
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color.White.copy(alpha = 0.08f))
                        )
                        val resultScroll = rememberScrollState()
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(end = 10.dp)
                                    .verticalScroll(resultScroll)
                            ) {
                                SelectionContainer {
                                    Text(
                                        text = terminalResultText(resolvedCommandPreview, executionResult),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFE5E7EB),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            VerticalScrollbar(
                                adapter = rememberScrollbarAdapter(resultScroll),
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .fillMaxHeight()
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun terminalResultText(
    commandPreview: String,
    executionResult: String
): AnnotatedString = buildAnnotatedString {
    appendStyledLine("${'$'} $commandPreview", Color(0xFF93C5FD), FontWeight.SemiBold)
    executionResult.lines().forEachIndexed { index, line ->
        if (index > 0) append('\n')
        val color = when {
            line.contains("执行成功") || line.contains("Success", ignoreCase = true) -> Color(0xFF86EFAC)
            line.contains("执行失败") ||
                line.contains("执行异常") ||
                line.contains("Failed", ignoreCase = true) ||
                line.contains("Error", ignoreCase = true) -> Color(0xFFFCA5A5)
            line.contains("执行中") || line.contains("Running", ignoreCase = true) -> Color(0xFFFDE68A)
            else -> Color(0xFFE5E7EB)
        }
        appendStyled(line, color)
    }
}

private fun AnnotatedString.Builder.appendStyledLine(
    text: String,
    color: Color,
    fontWeight: FontWeight? = null
) {
    appendStyled(text, color, fontWeight)
    append('\n')
}

private fun AnnotatedString.Builder.appendStyled(
    text: String,
    color: Color,
    fontWeight: FontWeight? = null
) {
    pushStyle(SpanStyle(color = color, fontWeight = fontWeight))
    append(text)
    pop()
}

@Composable
private fun TerminalActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .height(28.dp)
            .widthIn(min = 58.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = text,
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun CommandActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = false
) {
    val contentColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
        primary -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val backgroundColor = when {
        !enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
        primary -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.68f)
    }
    val borderColor = when {
        !enabled -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        primary -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)
    }

    TextButton(
        onClick = onClick,
        enabled = enabled,
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

@Composable
private fun CustomCommandDialog(
    initialCommand: CustomCommand?,
    onDismiss: () -> Unit,
    onConfirm: (CustomCommand) -> Unit
) {
    val isEditing = initialCommand != null
    var title by remember { mutableStateOf(initialCommand?.title.orEmpty()) }
    var commandPreview by remember { mutableStateOf(initialCommand?.commandPreview.orEmpty()) }
    var shellCommand by remember { mutableStateOf(initialCommand?.shellCommand.orEmpty()) }
    var description by remember { mutableStateOf(initialCommand?.description.orEmpty()) }
    var categoryKey by remember { mutableStateOf(initialCommand?.categoryKey ?: "device") }
    var categoryExpanded by remember { mutableStateOf(false) }

    val categoryOptions = listOf(
        "device" to l10n("设备", "Device"),
        "app" to l10n("应用管理", "Apps"),
        "file" to l10n("文件操作", "Files"),
        "input" to l10n("输入控制", "Input"),
        "screen" to l10n("截图录屏", "Screen"),
        "network" to l10n("网络调试", "Network"),
        "log" to l10n("日志", "Logs"),
        "tv" to l10n("TV盒子", "TV")
    )
    val selectedCategoryLabel = categoryOptions.firstOrNull { it.first == categoryKey }?.second ?: categoryOptions[0].second

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.widthIn(min = 520.dp, max = 560.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = if (isEditing) l10n("编辑自定义命令", "Edit Custom Command")
                            else l10n("添加自定义命令", "Add Custom Command"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = l10n("配置命令标题、执行内容和分类", "Configure title, command body and category"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .widthIn(min = 520.dp, max = 560.dp)
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .widthIn(min = 520.dp, max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CustomCommandDialogField(
                    label = l10n("命令标题", "Command title"),
                    value = title,
                    onValueChange = { title = it },
                    placeholder = l10n("例：查看设备信息", "e.g.: Show device info"),
                    required = true
                )

                CustomCommandDialogField(
                    label = l10n("ADB 命令", "ADB command"),
                    value = commandPreview,
                    onValueChange = { commandPreview = it },
                    placeholder = l10n("例：adb shell getprop ro.build.display.id", "e.g.: adb shell getprop ro.build.display.id"),
                    required = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CustomCommandDialogField(
                        label = l10n("Shell 命令", "Shell command"),
                        value = shellCommand,
                        onValueChange = { shellCommand = it },
                        placeholder = l10n("可选，留空则直接执行 ADB 命令", "Optional, leave empty to run ADB command"),
                        modifier = Modifier.weight(1f)
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = l10n("分类", "Category"),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Box {
                            OutlinedTextField(
                                value = selectedCategoryLabel,
                                onValueChange = {},
                                readOnly = true,
                                singleLine = true,
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                                },
                                shape = RoundedCornerShape(8.dp),
                                textStyle = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 42.dp)
                                    .clickable { categoryExpanded = !categoryExpanded }
                            )
                            com.ludoven.adbtool.ui.mac.DropdownMenu(
                                expanded = categoryExpanded,
                                onDismissRequest = { categoryExpanded = false }
                            ) {
                                categoryOptions.forEach { (key, label) ->
                                    com.ludoven.adbtool.ui.mac.DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            categoryKey = key
                                            categoryExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                CustomCommandDialogField(
                    label = l10n("描述", "Description"),
                    value = description,
                    onValueChange = { description = it },
                    placeholder = l10n("可选，命令的简要说明", "Optional, brief description of the command")
                )

                Text(
                    text = l10n("可使用 {package} 和 {text} 作为占位符。", "You can use {package} and {text} as placeholders."),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            CommandActionButton(
                text = if (isEditing) l10n("保存", "Save") else l10n("添加", "Add"),
                icon = Icons.Default.Add,
                onClick = {
                    val finalTitle = title.trim()
                    val finalCommand = commandPreview.trim()
                    if (finalTitle.isNotBlank() && finalCommand.isNotBlank()) {
                        val cmd = CustomCommand(
                            id = initialCommand?.id ?: "custom_${java.util.UUID.randomUUID()}",
                            title = finalTitle,
                            description = description.trim(),
                            commandPreview = finalCommand,
                            categoryKey = categoryKey,
                            shellCommand = shellCommand.trim()
                        )
                        onConfirm(cmd)
                    }
                },
                enabled = title.trim().isNotBlank() && commandPreview.trim().isNotBlank(),
                primary = true,
                modifier = Modifier.height(32.dp)
            )
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            ) {
                Text(l10n("取消", "Cancel"))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
private fun CustomCommandDialogField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    required: Boolean = false
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            if (required) {
                Text(
                    text = "*",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            placeholder = {
                Text(
                    text = placeholder,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodySmall
                )
            },
            textStyle = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 42.dp)
        )
    }
}
