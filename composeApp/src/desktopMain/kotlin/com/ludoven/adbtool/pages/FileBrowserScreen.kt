package com.ludoven.adbtool.pages

import com.ludoven.adbtool.ui.mac.*

import com.ludoven.adbtool.entity.FileInfo
import com.ludoven.adbtool.entity.FileSortBy
import com.ludoven.adbtool.entity.FileSortOrder
import com.ludoven.adbtool.entity.MsgContent
import com.ludoven.adbtool.util.AdbTool
import com.ludoven.adbtool.util.l10n
import com.ludoven.adbtool.viewmodel.FileBrowserViewModel
import com.ludoven.adbtool.widget.EmptyStatePanel
import com.ludoven.adbtool.widget.PageHeader
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import kotlin.math.max
import org.jetbrains.compose.resources.stringResource

internal fun fileBrowserDeviceActionsEnabled(selectedDevice: String?): Boolean =
    !selectedDevice.isNullOrBlank()

internal fun fileBrowserAvailableSpaceCommand(path: String): String =
    AdbTool.buildShellCommand("df", "-h", path)

@Composable
fun FileBrowserScreen(
    viewModel: FileBrowserViewModel,
    selectedDevice: String?
) {
    val currentPath by viewModel.currentPath.collectAsState()
    val files by viewModel.files.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorText by viewModel.errorText.collectAsState()
    val sortBy by viewModel.sortBy.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val showHidden by viewModel.showHidden.collectAsState()
    val searchKeyword by viewModel.searchKeyword.collectAsState()
    val clipboardMode by viewModel.clipboardMode.collectAsState()
    val clipboardFiles by viewModel.clipboardFiles.collectAsState()
    val showDialog by viewModel.showDialog.collectAsState()
    val dialogMessage by viewModel.dialogMessage.collectAsState()
    val deviceActionsEnabled = fileBrowserDeviceActionsEnabled(selectedDevice)

    val filteredFiles by remember {
        derivedStateOf {
            val base = if (showHidden) files else files.filter { !it.name.startsWith(".") }
            val keyword = searchKeyword.trim()
            if (keyword.isBlank()) base else base.filter { it.name.contains(keyword, ignoreCase = true) }
        }
    }
    val listState = rememberLazyListState()

    // Dialog states
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<FileInfo?>(null) }
    var renameText by remember { mutableStateOf("") }
    var showNewDirDialog by remember { mutableStateOf(false) }
    var newDirName by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deleteTargets by remember { mutableStateOf<List<String>>(emptyList()) }

    // Context menu state
    var contextMenuTarget by remember { mutableStateOf<FileInfo?>(null) }
    var contextMenuExpanded by remember { mutableStateOf(false) }
    var contextMenuOffsetPx by remember { mutableStateOf(Offset.Zero) }
    var fileListRootPx by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    var showAdvancedFields by remember { mutableStateOf(false) }
    var selectedPaths by remember { mutableStateOf(setOf<String>()) }
    var toolbarMenuExpanded by remember { mutableStateOf(false) }

    val availableSpace by produceState(initialValue = "--", currentPath, selectedDevice) {
        value = "--"
        if (!selectedDevice.isNullOrBlank()) {
            runCatching {
                val output = AdbTool.execShellAsync(fileBrowserAvailableSpaceCommand(currentPath), selectedDevice)
                if (output.success) {
                    output.output.lines().lastOrNull { it.trim().isNotEmpty() && !it.contains("Filesystem", true) }
                        ?.split(Regex("\\s+"))
                        ?.let { if (it.size >= 4) it[it.size - 3] else "--" }
                        ?: "--"
                } else "--"
            }.onSuccess { value = it }
        }
    }

    LaunchedEffect(selectedDevice) {
        viewModel.loadFiles(deviceId = selectedDevice)
    }
    DisposableEffect(Unit) {
        onDispose {
            viewModel.cancelActiveLoad()
        }
    }

    // Dialogs
    if (showRenameDialog && renameTarget != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text(l10n("重命名", "Rename")) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(l10n("新名称", "New name")) }
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.renameFile(
                        "${currentPath}/${renameTarget!!.name}",
                        renameText,
                        selectedDevice
                    )
                    showRenameDialog = false
                }) { Text(l10n("确定", "Confirm")) }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text(l10n("取消", "Cancel")) }
            }
        )
    }

    if (showNewDirDialog) {
        AlertDialog(
            onDismissRequest = { showNewDirDialog = false },
            title = { Text(l10n("新建文件夹", "New folder")) },
            text = {
                OutlinedTextField(
                    value = newDirName,
                    onValueChange = { newDirName = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(l10n("文件夹名称", "Folder name")) }
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.createDirectory(newDirName, selectedDevice)
                    showNewDirDialog = false
                    newDirName = ""
                }) { Text(l10n("创建", "Create")) }
            },
            dismissButton = {
                TextButton(onClick = { showNewDirDialog = false; newDirName = "" }) { Text(l10n("取消", "Cancel")) }
            }
        )
    }

    if (showDeleteConfirm && deleteTargets.isNotEmpty()) {
        val count = deleteTargets.size
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(l10n("确认删除", "Confirm delete")) },
            text = {
                if (count == 1) {
                    val name = deleteTargets.first().substringAfterLast('/')
                    Text(l10n("确定要删除 \"$name\" 吗？此操作不可撤销。", "Delete \"$name\"? This action cannot be undone."))
                } else {
                    Text(l10n("确定要删除选中的 $count 个项目吗？此操作不可撤销。", "Delete $count selected items? This action cannot be undone."))
                }
            },
            confirmButton = {
                Button(onClick = {
                    val targets = deleteTargets
                    viewModel.deleteFiles(targets, selectedDevice)
                    selectedPaths = selectedPaths - targets.toSet()
                    deleteTargets = emptyList()
                    showDeleteConfirm = false
                }) { Text(l10n("删除", "Delete"), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(l10n("取消", "Cancel")) }
            }
        )
    }

    if (showDialog) {
        dialogMessage?.let { message ->
            TipDialog(
                dialogText = when (message) {
                    is MsgContent.Resource -> stringResource(message.stringResource, *message.args.toTypedArray())
                    is MsgContent.Text -> message.text
                }
            ) { viewModel.dismissTipDialog() }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PageHeader(
            title = l10n("文件管理", "File Manager"),
            subtitle = l10n("浏览和管理设备文件", "Browse and manage device files")
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = { viewModel.pushFile(selectedDevice) },
                    enabled = deviceActionsEnabled,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Upload, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(l10n("上传", "Upload"))
                }
                OutlinedButton(
                    onClick = { showNewDirDialog = true },
                    enabled = deviceActionsEnabled,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.CreateNewFolder, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(l10n("新建文件夹", "New folder"))
                }
                Box {
                    OutlinedButton(
                        onClick = { toolbarMenuExpanded = true },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.MoreHoriz, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(l10n("更多", "More"))
                    }
                    DropdownMenu(
                        expanded = toolbarMenuExpanded,
                        onDismissRequest = { toolbarMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (showAdvancedFields) l10n("隐藏高级字段", "Hide advanced columns") else l10n("显示高级字段", "Show advanced columns")) },
                            onClick = {
                                showAdvancedFields = !showAdvancedFields
                                toolbarMenuExpanded = false
                            },
                            leadingIcon = { Icon(Icons.Default.Tune, null, modifier = Modifier.size(16.dp)) }
                        )
                        DropdownMenuItem(
                            text = { Text(if (showHidden) l10n("隐藏隐藏文件", "Hide hidden files") else l10n("显示隐藏文件", "Show hidden files")) },
                            onClick = {
                                viewModel.toggleShowHidden()
                                toolbarMenuExpanded = false
                            },
                            leadingIcon = { Icon(if (showHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
            }
        }

        if (selectedDevice.isNullOrBlank()) {
            EmptyStatePanel(
                title = l10n("未选择设备", "No device selected"),
                description = l10n("请先连接并选择设备后查看文件。", "Connect and select a device before browsing files."),
                icon = Icons.Default.Folder,
                modifier = Modifier.fillMaxSize()
            )
            return
        }

        // Navigation toolbar
        NavigationToolbar(
            currentPath = currentPath,
            canGoBack = viewModel.canGoBack(),
            canGoForward = viewModel.canGoForward(),
            searchKeyword = searchKeyword,
            onBack = { viewModel.goBack(selectedDevice) },
            onForward = { viewModel.goForward(selectedDevice) },
            onUp = { viewModel.navigateUp(selectedDevice) },
            onRefresh = { viewModel.loadFiles(deviceId = selectedDevice, forceRefresh = true) },
            onNavigateToPath = { viewModel.navigateTo(it, selectedDevice) },
            onSearchChange = { viewModel.setSearchKeyword(it) },
        )

        // Quick paths
        QuickPathBar(
            currentPath = currentPath,
            onNavigate = { viewModel.navigateTo(it, selectedDevice) }
        )

        // File count and status bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = l10n("${filteredFiles.size} 项", "${filteredFiles.size} items"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = currentPath,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SortChip(l10n("名称", "Name"), sortBy == FileSortBy.NAME, sortOrder) { viewModel.setSortBy(FileSortBy.NAME) }
                SortChip(l10n("大小", "Size"), sortBy == FileSortBy.SIZE, sortOrder) { viewModel.setSortBy(FileSortBy.SIZE) }
                SortChip(l10n("日期", "Date"), sortBy == FileSortBy.DATE, sortOrder) { viewModel.setSortBy(FileSortBy.DATE) }
            }
        }

        // File list
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(8.dp)
        ) {
            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                errorText != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyStatePanel(
                            title = l10n("目录读取失败", "Failed to load folder"),
                            description = errorText.orEmpty(),
                            actionLabel = l10n("重试", "Retry"),
                            onAction = { viewModel.loadFiles(deviceId = selectedDevice, forceRefresh = true) }
                        )
                    }
                }
                filteredFiles.isEmpty() -> {
                    EmptyStatePanel(
                        title = if (searchKeyword.isBlank()) l10n("此目录为空", "This folder is empty") else l10n("没有匹配文件", "No matching files"),
                        description = if (searchKeyword.isBlank()) {
                            l10n("当前路径没有可显示的文件或文件夹。", "This path has no visible files or folders.")
                        } else {
                            l10n("调整搜索词或清除筛选后再试。", "Adjust the search term or clear the filter.")
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .onGloballyPositioned { coordinates ->
                                fileListRootPx = coordinates.positionInRoot()
                            }
                    ) {
                        // Column header
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Header row
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp)) {
                                Text(
                                    text = l10n("名称", "Name"),
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = l10n("大小", "Size"),
                                    modifier = Modifier.width(80.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = l10n("修改日期", "Modified"),
                                    modifier = Modifier.width(130.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (showAdvancedFields) {
                                    Text(
                                        text = l10n("权限", "Permission"),
                                        modifier = Modifier.width(110.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = l10n("所有者", "Owner"),
                                        modifier = Modifier.width(80.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                } else {
                                    Text(
                                        text = l10n("类型", "Type"),
                                        modifier = Modifier.width(90.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))

                            // File rows
                            Box(modifier = Modifier.weight(1f)) {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(end = 12.dp),
                                    state = listState,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    items(filteredFiles, key = { it.name }) { file ->
                                        FileRow(
                                            file = file,
                                            isHighlighted = selectedPaths.contains("$currentPath/${file.name}") || clipboardFiles.contains("$currentPath/${file.name}"),
                                            showAdvancedFields = showAdvancedFields,
                                            onClick = {
                                                val full = "$currentPath/${file.name}"
                                                selectedPaths = if (selectedPaths.contains(full)) selectedPaths - full else selectedPaths + full
                                            },
                                            onOpen = {
                                                if (file.isDirectory) {
                                                    viewModel.navigateTo("$currentPath/${file.name}", selectedDevice)
                                                } else {
                                                    viewModel.openFile("$currentPath/${file.name}", selectedDevice)
                                                }
                                            },
                                            onRightClick = { clickInRoot ->
                                                contextMenuTarget = file
                                                val clickInList = clickInRoot - fileListRootPx
                                                contextMenuOffsetPx = Offset(
                                                    x = max(0f, clickInList.x),
                                                    y = max(0f, clickInList.y)
                                                )
                                                contextMenuExpanded = true
                                            }
                                        )
                                    }
                                }

                                VerticalScrollbar(
                                    adapter = rememberScrollbarAdapter(listState),
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .fillMaxHeight()
                                        .padding(end = 2.dp)
                                )
                            }
                        }

                        // Context menu
                        if (contextMenuExpanded && contextMenuTarget != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) { contextMenuExpanded = false }
                            )
                            androidx.compose.material.DropdownMenu(
                                expanded = contextMenuExpanded,
                                onDismissRequest = { contextMenuExpanded = false },
                                offset = with(density) {
                                    DpOffset(contextMenuOffsetPx.x.toDp(), contextMenuOffsetPx.y.toDp())
                                },
                                modifier = Modifier.width(200.dp)
                            ) {
                                val file = contextMenuTarget!!
                                val fullPath = "$currentPath/${file.name}"

                                if (file.isDirectory) {
                                    DropdownMenuItem(
                                        text = { Text(l10n("打开", "Open")) },
                                        onClick = {
                                            viewModel.navigateTo(fullPath, selectedDevice)
                                            contextMenuExpanded = false
                                        },
                                        leadingIcon = { Icon(Icons.Default.FolderOpen, l10n("打开", "Open"), modifier = Modifier.size(18.dp)) }
                                    )
                                } else {
                                    DropdownMenuItem(
                                        text = { Text(l10n("打开", "Open")) },
                                        onClick = {
                                            viewModel.openFile(fullPath, selectedDevice)
                                            contextMenuExpanded = false
                                        },
                                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, l10n("打开", "Open"), modifier = Modifier.size(18.dp)) }
                                    )
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                                DropdownMenuItem(
                                    text = { Text(l10n("复制路径", "Copy path")) },
                                    onClick = {
                                        val copied = copyToClipboardText(fullPath)
                                        viewModel.showTipDialog(
                                            MsgContent.Text(if (copied) l10n("已复制路径", "Path copied") else l10n("复制失败", "Copy failed")),
                                            autoDismiss = copied
                                        )
                                        contextMenuExpanded = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.ContentCopy, l10n("复制路径", "Copy path"), modifier = Modifier.size(18.dp)) }
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                                if (!file.isDirectory) {
                                    DropdownMenuItem(
                                        text = { Text(l10n("拉取到本地", "Pull to local")) },
                                        onClick = {
                                            viewModel.pullFile(fullPath, selectedDevice)
                                            contextMenuExpanded = false
                                        },
                                        leadingIcon = { Icon(Icons.Default.Download, l10n("拉取", "Pull"), modifier = Modifier.size(18.dp)) }
                                    )
                                }

                                DropdownMenuItem(
                                    text = { Text(l10n("重命名", "Rename")) },
                                    onClick = {
                                        renameTarget = file
                                        renameText = file.name
                                        showRenameDialog = true
                                        contextMenuExpanded = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.Edit, l10n("重命名", "Rename"), modifier = Modifier.size(18.dp)) }
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                                DropdownMenuItem(
                                    text = { Text(l10n("查看权限", "View permission")) },
                                    onClick = {
                                        viewModel.getFilePermission(fullPath, selectedDevice)
                                        contextMenuExpanded = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.Security, l10n("权限", "Permission"), modifier = Modifier.size(18.dp)) }
                                )

                                DropdownMenuItem(
                                    text = { Text(l10n("属性", "Properties")) },
                                    onClick = {
                                        viewModel.getFilePermission(fullPath, selectedDevice)
                                        contextMenuExpanded = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.Info, l10n("属性", "Properties"), modifier = Modifier.size(18.dp)) }
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                                DropdownMenuItem(
                                    text = { Text(l10n("删除", "Delete"), color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        deleteTargets = listOf("${currentPath}/${file.name}")
                                        showDeleteConfirm = true
                                        contextMenuExpanded = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Delete,
                                            l10n("删除", "Delete"),
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (selectedPaths.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        selectedPaths.forEach { viewModel.pullFile(it, selectedDevice) }
                    },
                    shape = RoundedCornerShape(10.dp)
                ) { Text(l10n("下载", "Download")) }
                OutlinedButton(
                    onClick = {
                        selectedPaths.firstOrNull()?.let {
                            val f = filteredFiles.firstOrNull { file -> "$currentPath/${file.name}" == it }
                            if (f != null) {
                                renameTarget = f
                                renameText = f.name
                                showRenameDialog = true
                            }
                        }
                    },
                    enabled = selectedPaths.size == 1,
                    shape = RoundedCornerShape(10.dp)
                ) { Text(l10n("重命名", "Rename")) }
                OutlinedButton(
                    onClick = {
                        val copied = copyToClipboardText(selectedPaths.joinToString("\n"))
                        viewModel.showTipDialog(
                            MsgContent.Text(if (copied) l10n("已复制 ${selectedPaths.size} 个路径", "Copied ${selectedPaths.size} paths") else l10n("复制失败", "Copy failed")),
                            autoDismiss = copied
                        )
                    },
                    shape = RoundedCornerShape(10.dp)
                ) { Text(l10n("复制路径", "Copy path")) }
                OutlinedButton(
                    onClick = {
                        if (selectedPaths.isNotEmpty()) {
                            deleteTargets = selectedPaths.toList()
                            showDeleteConfirm = true
                        }
                    },
                    shape = RoundedCornerShape(10.dp)
                ) { Text(l10n("删除", "Delete")) }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f), RoundedCornerShape(10.dp))
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = l10n("${filteredFiles.size} 项 · 已选择 ${selectedPaths.size} 项", "${filteredFiles.size} items · selected ${selectedPaths.size}"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = l10n("路径: $currentPath · 可用空间: $availableSpace", "Path: $currentPath · Free: $availableSpace"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun NavigationToolbar(
    currentPath: String,
    canGoBack: Boolean,
    canGoForward: Boolean,
    searchKeyword: String,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onUp: () -> Unit,
    onRefresh: () -> Unit,
    onNavigateToPath: (String) -> Unit,
    onSearchChange: (String) -> Unit,
) {
    var pathInput by remember(currentPath) { mutableStateOf(currentPath) }
    var isEditingPath by remember(currentPath) { mutableStateOf(false) }
    val crumbs = remember(currentPath) {
        currentPath.trim('/').split("/")
            .filter { it.isNotBlank() }
    }
    val crumbPaths = remember(currentPath, crumbs) {
        crumbs.mapIndexed { index, _ ->
            "/" + crumbs.take(index + 1).joinToString("/")
        }
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            IconButton(onClick = onBack, enabled = canGoBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, l10n("后退", "Back"), modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onForward, enabled = canGoForward) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, l10n("前进", "Forward"), modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onUp) {
                Icon(Icons.Default.ArrowUpward, l10n("上一级", "Up"), modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, l10n("刷新", "Refresh"), modifier = Modifier.size(18.dp))
            }

            if (isEditingPath) {
                OutlinedTextField(
                    value = pathInput,
                    onValueChange = { pathInput = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    placeholder = { Text(l10n("输入路径", "Input path"), style = MaterialTheme.typography.bodySmall) },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (pathInput.isNotBlank()) onNavigateToPath(pathInput.trim())
                                isEditingPath = false
                            }
                        ) { Icon(Icons.Default.Check, l10n("确认", "Confirm"), modifier = Modifier.size(16.dp)) }
                    }
                )
            } else {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f), RoundedCornerShape(8.dp))
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = l10n("内部存储", "Internal storage"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onNavigateToPath("/sdcard") }
                    )
                    crumbs.forEachIndexed { index, crumb ->
                        Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = crumb,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable { onNavigateToPath(crumbPaths.getOrNull(index) ?: "/sdcard") }
                        )
                    }
                }
            }

            IconButton(onClick = { isEditingPath = !isEditingPath }) {
                Icon(
                    if (isEditingPath) Icons.Default.ViewModule else Icons.Default.Edit,
                    l10n("切换路径输入", "Toggle path input"),
                    modifier = Modifier.size(18.dp)
                )
            }
            OutlinedTextField(
                value = searchKeyword,
                onValueChange = onSearchChange,
                modifier = Modifier.widthIn(min = 220.dp, max = 320.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
                placeholder = { Text(l10n("搜索文件名...", "Search file name..."), style = MaterialTheme.typography.bodySmall) },
                leadingIcon = { Icon(Icons.Default.Search, l10n("搜索", "Search"), modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchKeyword.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }, modifier = Modifier.size(18.dp)) {
                            Icon(Icons.Default.Clear, l10n("清除", "Clear"), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun QuickPathBar(
    currentPath: String,
    onNavigate: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FileBrowserViewModel.QUICK_PATHS.forEach { (path, label) ->
            val isActive = currentPath == path || currentPath.startsWith("$path/")
            val localizedLabel = when (path) {
                "/" -> l10n("根目录", "Root")
                "/sdcard" -> l10n("内部存储", "Internal")
                "/sdcard/Download" -> l10n("下载", "Download")
                "/sdcard/DCIM" -> l10n("相册", "DCIM")
                "/sdcard/Documents" -> l10n("文档", "Documents")
                "/data/data" -> l10n("应用数据", "App Data")
                "/system" -> l10n("系统", "System")
                "/tmp" -> l10n("临时文件", "Temp")
                else -> label
            }
            Surface(
                onClick = { onNavigate(path) },
                shape = RoundedCornerShape(6.dp),
                color = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(
                    alpha = 0.5f
                ),
                border = if (isActive) androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                ) else null
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        when (path) {
                            "/" -> Icons.Default.Storage
                            "/sdcard" -> Icons.Default.PhoneAndroid
                            "/sdcard/Download" -> Icons.Default.Download
                            "/sdcard/DCIM" -> Icons.Default.CameraAlt
                            "/sdcard/Documents" -> Icons.Default.Description
                            "/data/data" -> Icons.Default.Apps
                            "/system" -> Icons.Default.Settings
                            else -> Icons.Default.Folder
                        },
                        localizedLabel,
                        modifier = Modifier.size(14.dp),
                        tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = localizedLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun SortChip(
    label: String,
    isActive: Boolean,
    order: FileSortOrder,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(4.dp),
        color = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant.copy(
            alpha = 0.3f
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
            )
            if (isActive) {
                Text(
                    text = if (order == FileSortOrder.ASC) "↑" else "↓",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun FileRow(
    file: FileInfo,
    isHighlighted: Boolean,
    showAdvancedFields: Boolean,
    onClick: () -> Unit,
    onOpen: () -> Unit,
    onRightClick: (Offset) -> Unit
) {
    val rowInteraction = remember { MutableInteractionSource() }
    val isHovered by rowInteraction.collectIsHoveredAsState()
    val bgColor = when {
        isHighlighted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        isHovered -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)
        else -> Color.Transparent
    }
    var rowRootPx by remember(file.name) { mutableStateOf(Offset.Zero) }
    val fileType = when {
        file.isDirectory -> l10n("文件夹", "Folder")
        file.name.endsWith(".apk", true) -> "APK"
        file.name.endsWith(".zip", true) || file.name.endsWith(".tar", true) || file.name.endsWith(".gz", true) -> l10n("压缩包", "Archive")
        file.name.endsWith(".jpg", true) || file.name.endsWith(".png", true) || file.name.endsWith(".gif", true) -> l10n("图片", "Image")
        file.name.endsWith(".mp4", true) || file.name.endsWith(".avi", true) || file.name.endsWith(".mkv", true) -> l10n("视频", "Video")
        else -> l10n("文件", "File")
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        modifier = Modifier
            .fillMaxWidth()
            .hoverable(rowInteraction)
            .onGloballyPositioned { coordinates ->
                rowRootPx = coordinates.positionInRoot()
            }
            .pointerInput(file.name) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.button == PointerButton.Secondary && event.type == PointerEventType.Press) {
                            val clickInRoot = rowRootPx + event.changes.first().position
                            onRightClick(clickInRoot)
                        }
                    }
                }
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = { onRightClick(rowRootPx) },
                onDoubleClick = onOpen
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 36.dp, max = 42.dp)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Name column with icon
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = when {
                        file.isDirectory -> Icons.Default.Folder
                        file.isSymlink -> Icons.Default.Link
                        file.name.endsWith(".apk") -> Icons.Default.Android
                        file.name.endsWith(".zip") || file.name.endsWith(".tar") || file.name.endsWith(".gz") -> Icons.Default.Archive
                        file.name.endsWith(".jpg") || file.name.endsWith(".png") || file.name.endsWith(".gif") -> Icons.Default.Image
                        file.name.endsWith(".mp3") || file.name.endsWith(".wav") || file.name.endsWith(".ogg") -> Icons.Default.MusicNote
                        file.name.endsWith(".mp4") || file.name.endsWith(".avi") || file.name.endsWith(".mkv") -> Icons.Default.Videocam
                        file.name.endsWith(".txt") || file.name.endsWith(".log") || file.name.endsWith(".json") || file.name.endsWith(".xml") -> Icons.Default.Description
                        file.name.endsWith(".sh") || file.name.endsWith(".py") || file.name.endsWith(".java") || file.name.endsWith(".kt") -> Icons.Default.Code
                        else -> Icons.AutoMirrored.Filled.InsertDriveFile
                    },
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = when {
                        file.isDirectory -> MaterialTheme.colorScheme.primary
                        file.isSymlink -> MaterialTheme.colorScheme.tertiary
                        file.name.endsWith(".apk") -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Column {
                    Text(
                        text = file.name,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = if (file.isDirectory) FontWeight.SemiBold else FontWeight.Normal
                    )
                    if (file.isSymlink && file.symlinkTarget.isNotEmpty()) {
                        Text(
                            text = "-> ${file.symlinkTarget}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            // Size
            Text(
                text = if (file.isDirectory) "-" else file.displaySize,
                modifier = Modifier.width(80.dp),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Date
            Text(
                text = "${file.date} ${file.time}",
                modifier = Modifier.width(130.dp),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (showAdvancedFields) {
                Text(
                    text = file.permissions,
                    modifier = Modifier.width(110.dp),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = file.owner,
                    modifier = Modifier.width(80.dp),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = fileType,
                    modifier = Modifier.width(90.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun copyToClipboardText(text: String): Boolean {
    return runCatching {
        val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(java.awt.datatransfer.StringSelection(text), null)
    }.isSuccess
}
