package com.ludoven.adbtool.pages

import com.ludoven.adbtool.ui.mac.*

import com.ludoven.adbtool.entity.FileInfo
import com.ludoven.adbtool.entity.FileSortBy
import com.ludoven.adbtool.entity.FileSortOrder
import com.ludoven.adbtool.entity.MsgContent
import com.ludoven.adbtool.viewmodel.FileBrowserViewModel
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import kotlin.math.max
import org.jetbrains.compose.resources.stringResource

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

    val filteredFiles = remember(files, showHidden, searchKeyword) {
        val base = if (showHidden) files else files.filter { !it.name.startsWith(".") }
        val keyword = searchKeyword.trim()
        if (keyword.isBlank()) base else base.filter { it.name.contains(keyword, ignoreCase = true) }
    }
    val listState = rememberLazyListState()

    // Dialog states
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<FileInfo?>(null) }
    var renameText by remember { mutableStateOf("") }
    var showNewDirDialog by remember { mutableStateOf(false) }
    var newDirName by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<FileInfo?>(null) }

    // Context menu state
    var contextMenuTarget by remember { mutableStateOf<FileInfo?>(null) }
    var contextMenuExpanded by remember { mutableStateOf(false) }
    var contextMenuOffsetPx by remember { mutableStateOf(Offset.Zero) }
    var fileListRootPx by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current

    LaunchedEffect(selectedDevice) {
        viewModel.loadFiles(deviceId = selectedDevice)
    }

    // Dialogs
    if (showRenameDialog && renameTarget != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("重命名") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("新名称") }
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
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("取消") }
            }
        )
    }

    if (showNewDirDialog) {
        AlertDialog(
            onDismissRequest = { showNewDirDialog = false },
            title = { Text("新建文件夹") },
            text = {
                OutlinedTextField(
                    value = newDirName,
                    onValueChange = { newDirName = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("文件夹名称") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.createDirectory(newDirName, selectedDevice)
                    showNewDirDialog = false
                    newDirName = ""
                }) { Text("创建") }
            },
            dismissButton = {
                TextButton(onClick = { showNewDirDialog = false; newDirName = "" }) { Text("取消") }
            }
        )
    }

    if (showDeleteConfirm && deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除 \"${deleteTarget!!.name}\" 吗？此操作不可撤销。") },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteFile("${currentPath}/${deleteTarget!!.name}", selectedDevice)
                    showDeleteConfirm = false
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
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
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "文件管理",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "浏览和管理设备文件",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (selectedDevice.isNullOrBlank()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "请先选择设备后查看文件。",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return
        }

        // Navigation toolbar
        NavigationToolbar(
            currentPath = currentPath,
            canGoBack = viewModel.canGoBack(),
            canGoForward = viewModel.canGoForward(),
            showHidden = showHidden,
            searchKeyword = searchKeyword,
            hasClipboard = clipboardFiles.isNotEmpty(),
            clipboardMode = clipboardMode,
            onBack = { viewModel.goBack(selectedDevice) },
            onForward = { viewModel.goForward(selectedDevice) },
            onUp = { viewModel.navigateUp(selectedDevice) },
            onRefresh = { viewModel.loadFiles(deviceId = selectedDevice) },
            onToggleHidden = { viewModel.toggleShowHidden() },
            onSearchChange = { viewModel.setSearchKeyword(it) },
            onPushFile = { viewModel.pushFile(selectedDevice) },
            onNewDir = { showNewDirDialog = true },
            onPaste = { viewModel.pasteFiles(selectedDevice) },
            onClearClipboard = { viewModel.clearClipboard() }
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
                    text = "${filteredFiles.size} 项",
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                SortChip("名称", sortBy == FileSortBy.NAME, sortOrder) { viewModel.setSortBy(FileSortBy.NAME) }
                SortChip("大小", sortBy == FileSortBy.SIZE, sortOrder) { viewModel.setSortBy(FileSortBy.SIZE) }
                SortChip("日期", sortBy == FileSortBy.DATE, sortOrder) { viewModel.setSortBy(FileSortBy.DATE) }
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
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = errorText.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            OutlinedButton(onClick = { viewModel.loadFiles(deviceId = selectedDevice) }) {
                                Text("重试")
                            }
                        }
                    }
                }
                filteredFiles.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "此目录为空",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "名称",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "大小",
                                    modifier = Modifier.width(80.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "修改日期",
                                    modifier = Modifier.width(130.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "权限",
                                    modifier = Modifier.width(110.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "所有者",
                                    modifier = Modifier.width(80.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
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
                                            isHighlighted = clipboardFiles.contains("$currentPath/${file.name}"),
                                            onClick = {
                                                if (file.isDirectory) {
                                                    viewModel.navigateTo("$currentPath/${file.name}", selectedDevice)
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
                                        text = { Text("打开") },
                                        onClick = {
                                            viewModel.navigateTo(fullPath, selectedDevice)
                                            contextMenuExpanded = false
                                        },
                                        leadingIcon = { Icon(Icons.Default.FolderOpen, "打开", modifier = Modifier.size(18.dp)) }
                                    )
                                } else {
                                    DropdownMenuItem(
                                        text = { Text("打开") },
                                        onClick = {
                                            viewModel.openFile(fullPath, selectedDevice)
                                            contextMenuExpanded = false
                                        },
                                        leadingIcon = { Icon(Icons.Default.OpenInNew, "打开", modifier = Modifier.size(18.dp)) }
                                    )
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                                DropdownMenuItem(
                                    text = { Text("复制") },
                                    onClick = {
                                        viewModel.copyFiles(listOf(fullPath))
                                        contextMenuExpanded = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.ContentCopy, "复制", modifier = Modifier.size(18.dp)) }
                                )
                                DropdownMenuItem(
                                    text = { Text("剪切") },
                                    onClick = {
                                        viewModel.cutFiles(listOf(fullPath))
                                        contextMenuExpanded = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.ContentCut, "剪切", modifier = Modifier.size(18.dp)) }
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                                if (!file.isDirectory) {
                                    DropdownMenuItem(
                                        text = { Text("拉取到本地") },
                                        onClick = {
                                            viewModel.pullFile(fullPath, selectedDevice)
                                            contextMenuExpanded = false
                                        },
                                        leadingIcon = { Icon(Icons.Default.Download, "拉取", modifier = Modifier.size(18.dp)) }
                                    )
                                }

                                DropdownMenuItem(
                                    text = { Text("重命名") },
                                    onClick = {
                                        renameTarget = file
                                        renameText = file.name
                                        showRenameDialog = true
                                        contextMenuExpanded = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.Edit, "重命名", modifier = Modifier.size(18.dp)) }
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                                DropdownMenuItem(
                                    text = { Text("查看权限") },
                                    onClick = {
                                        viewModel.getFilePermission(fullPath, selectedDevice)
                                        contextMenuExpanded = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.Security, "权限", modifier = Modifier.size(18.dp)) }
                                )

                                DropdownMenuItem(
                                    text = { Text("属性") },
                                    onClick = {
                                        viewModel.getFilePermission(fullPath, selectedDevice)
                                        contextMenuExpanded = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.Info, "属性", modifier = Modifier.size(18.dp)) }
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                                DropdownMenuItem(
                                    text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        deleteTarget = file
                                        showDeleteConfirm = true
                                        contextMenuExpanded = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Delete,
                                            "删除",
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
    }
}

@Composable
private fun NavigationToolbar(
    currentPath: String,
    canGoBack: Boolean,
    canGoForward: Boolean,
    showHidden: Boolean,
    searchKeyword: String,
    hasClipboard: Boolean,
    clipboardMode: FileBrowserViewModel.ClipboardMode?,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onUp: () -> Unit,
    onRefresh: () -> Unit,
    onToggleHidden: () -> Unit,
    onSearchChange: (String) -> Unit,
    onPushFile: () -> Unit,
    onNewDir: () -> Unit,
    onPaste: () -> Unit,
    onClearClipboard: () -> Unit
) {
    var pathInput by remember(currentPath) { mutableStateOf(currentPath) }
    var isEditingPath by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Navigation buttons row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Navigation buttons
            IconButton(onClick = onBack, enabled = canGoBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "后退", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onForward, enabled = canGoForward) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, "前进", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onUp) {
                Icon(Icons.Default.ArrowUpward, "上一级", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, "刷新", modifier = Modifier.size(20.dp))
            }

            // Path input
            OutlinedTextField(
                value = if (isEditingPath) pathInput else currentPath,
                onValueChange = { pathInput = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                placeholder = { Text("输入路径", style = MaterialTheme.typography.bodySmall) },
            )

            // Toggle hidden
            IconButton(onClick = onToggleHidden) {
                Icon(
                    if (showHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    "显示隐藏文件",
                    modifier = Modifier.size(20.dp),
                    tint = if (showHidden) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Action buttons
            IconButton(onClick = onNewDir) {
                Icon(Icons.Default.CreateNewFolder, "新建文件夹", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onPushFile) {
                Icon(Icons.Default.Upload, "推送文件", modifier = Modifier.size(20.dp))
            }

            if (hasClipboard) {
                IconButton(onClick = onPaste) {
                    Icon(
                        Icons.Default.ContentPaste,
                        "粘贴",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onClearClipboard) {
                    Icon(Icons.Default.Clear, "取消", modifier = Modifier.size(18.dp))
                }
            }
        }

        // Search bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchKeyword,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
                placeholder = { Text("搜索文件名...", style = MaterialTheme.typography.bodySmall) },
                leadingIcon = { Icon(Icons.Default.Search, "搜索", modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchKeyword.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }, modifier = Modifier.size(18.dp)) {
                            Icon(Icons.Default.Clear, "清除", modifier = Modifier.size(16.dp))
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
                        label,
                        modifier = Modifier.size(14.dp),
                        tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = label,
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
    onClick: () -> Unit,
    onRightClick: (Offset) -> Unit
) {
    val bgColor = if (isHighlighted) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.01f)
    }
    var rowRootPx by remember(file.name) { mutableStateOf(Offset.Zero) }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor,
        modifier = Modifier
            .fillMaxWidth()
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
                onLongClick = { onRightClick(rowRootPx) }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
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
                        else -> Icons.Default.InsertDriveFile
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
            // Permissions
            Text(
                text = file.permissions,
                modifier = Modifier.width(110.dp),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Owner
            Text(
                text = file.owner,
                modifier = Modifier.width(80.dp),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
