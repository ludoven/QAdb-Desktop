package com.ludoven.adbtool.pages

import adbtool_desktop.composeapp.generated.resources.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import com.ludoven.adbtool.ui.mac.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ludoven.adbtool.entity.AdbFunctionType
import com.ludoven.adbtool.entity.AppInfoData
import com.ludoven.adbtool.entity.MsgContent
import com.ludoven.adbtool.util.AdbTool
import com.ludoven.adbtool.util.l10n
import com.ludoven.adbtool.viewmodel.AppViewModel
import org.jetbrains.compose.resources.stringResource

data class AppInfo(
    val appName: String,
    val packageName: String,
    val apkPath: String = "",
    val isSystemApp: Boolean = false,
    val isDebuggable: Boolean = false,
    val isDisabled: Boolean = false,
    val versionName: String = "-",
    val installTime: String = "-",
    val size: String = "-",
    val sizeBytes: Long? = null,
    val installTimestamp: Long? = null,
    val lastUsedTimestamp: Long? = null,
    val isRunning: Boolean = false
)

private data class AppFilterTab(
    val key: String,
    val label: String
)

private enum class AppSortMode {
    Name,
    Size,
    Version,
    InstallTime,
    Recent
}

fun getInstalledApps(): List<AppInfo> {
    val allApps = AdbTool.exec("pm list packages -f") ?: return emptyList()
    val sysApps = AdbTool.exec("pm list packages -s") ?: ""
    val disabledApps = AdbTool.exec("pm list packages -d") ?: ""
    val sysPackages = sysApps.lines()
        .mapNotNull { it.substringAfter("package:", "").takeIf { p -> p.isNotEmpty() } }
        .toSet()
    val disabledPackages = disabledApps.lines()
        .mapNotNull { it.substringAfter("package:", "").takeIf { p -> p.isNotEmpty() } }
        .toSet()
    return allApps.lines().mapNotNull { line ->
        val regex = Regex("""package:(.+)=([A-Za-z0-9._]+)""")
        val match = regex.find(line)
        val (apkPath, packageName) = match?.destructured ?: return@mapNotNull null
        val debugHint = packageName.contains("debug", ignoreCase = true) || packageName.endsWith(".dev")
        AppInfo(
            appName = packageName,
            packageName = packageName,
            apkPath = apkPath,
            isSystemApp = sysPackages.contains(packageName),
            isDebuggable = debugHint,
            isDisabled = disabledPackages.contains(packageName)
        )
    }.sortedBy { it.packageName }
}

@Composable
private fun AppAvatar(app: AppInfo, icon: ImageBitmap?, size: Int = 44) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size * 0.24f).dp)),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = app.appName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF2F4F8)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = app.appName.take(1).uppercase(),
                    color = Color(0xFF4B5F80),
                    fontWeight = FontWeight.Bold,
                    fontSize = (size * 0.4).sp
                )
            }
        }
    }
}

@Composable
fun AppScreen(viewModel: AppViewModel) {
    val appList by viewModel.appList.collectAsState()
    val searchText by viewModel.searchText.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val appIcons by viewModel.appIcons.collectAsState()
    val dialogMessage by viewModel.dialogMessage.collectAsState()
    val showDialog by viewModel.showDialog.collectAsState()
    val appInfo by viewModel.appInfo.collectAsState()

    var sortMenuExpanded by remember { mutableStateOf(false) }
    var lastRefreshMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var pendingDangerAction by remember { mutableStateOf<Pair<AdbFunctionType, AppInfo>?>(null) }
    var confirmActionLabel by remember { mutableStateOf("") }
    var confirmActionMessage by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf(AppSortMode.Name) }

    LaunchedEffect(Unit) { viewModel.getAppList() }

    val tabs = listOf(
        AppFilterTab("all", "全部应用"),
        AppFilterTab("user", "用户应用"),
        AppFilterTab("system", "系统应用"),
        AppFilterTab("debug", "可调试应用"),
        AppFilterTab("recent", "最近使用"),
        AppFilterTab("running", "运行中")
    )

    val filteredList = appList.filter { app ->
        val matchesSearch = app.appName.contains(searchText, ignoreCase = true) ||
            app.packageName.contains(searchText, ignoreCase = true)
        val matchesTab = when (selectedTab) {
            "用户应用" -> !app.isSystemApp
            "系统应用" -> app.isSystemApp
            "可调试应用" -> app.isDebuggable
            "最近使用" -> (app.lastUsedTimestamp ?: app.installTimestamp ?: 0L) > 0L
            "运行中" -> app.isRunning
            else -> true
        }
        matchesSearch && matchesTab
    }
    val displayedList = remember(filteredList, sortMode) {
        when (sortMode) {
            AppSortMode.Name -> filteredList.sortedBy { it.appName.lowercase() }
            AppSortMode.Size -> filteredList.sortedByDescending { it.sizeBytes ?: -1L }
            AppSortMode.Version -> filteredList.sortedByDescending { it.versionName }
            AppSortMode.InstallTime -> filteredList.sortedByDescending { it.installTimestamp ?: 0L }
            AppSortMode.Recent -> filteredList.sortedByDescending { it.lastUsedTimestamp ?: it.installTimestamp ?: 0L }
        }
    }
    val tabCountMap = remember(appList) {
        mapOf(
            "全部应用" to appList.size,
            "用户应用" to appList.count { !it.isSystemApp },
            "系统应用" to appList.count { it.isSystemApp },
            "可调试应用" to appList.count { it.isDebuggable },
            "最近使用" to appList.count { (it.lastUsedTimestamp ?: it.installTimestamp ?: 0L) > 0L },
            "运行中" to appList.count { it.isRunning }
        )
    }

    Scaffold(containerColor = Color.Transparent) { paddingValues ->
        if (appInfo != null) {
            AppDetailPage(
                appInfo = appInfo!!,
                icon = appIcons[appInfo!!.packageName],
                onBack = { viewModel.clearAppInfo() },
                onAction = { type -> viewModel.executeAdbAction(type, appInfo!!.packageName) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(20.dp)
            )
            if (showDialog) {
                dialogMessage?.let {
                    TipDialog(
                        dialogText = when (it) {
                            is MsgContent.Resource -> stringResource(it.stringResource, *it.args.toTypedArray())
                            is MsgContent.Text -> it.text
                        }
                    ) { viewModel.dismissTipDialog() }
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            lastRefreshMillis = System.currentTimeMillis()
                            viewModel.getAppList()
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(l10n("刷新", "Refresh"))
                    }
                    Text(
                        text = l10n("最近刷新", "Last refresh") + "：${formatRelativeRefresh(lastRefreshMillis)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(
                    value = searchText,
                    onValueChange = { viewModel.setSearchText(it) },
                    placeholder = { Text(l10n("搜索应用名称或包名", "Search app name or package")) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    textStyle = MaterialTheme.typography.bodyMedium,
                    singleLine = true,
                    modifier = Modifier.widthIn(min = 360.dp, max = 480.dp).height(52.dp),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(tabs, key = { it.key }) { tab ->
                        val selected = selectedTab == tab.label
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.dp,
                                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                                else Color(0xFFE5E7EB)
                            ),
                            modifier = Modifier.clickable { viewModel.setSelectedTab(tab.label) }
                        ) {
                            Text(
                                text = "${appTabLabel(tab.label)} ${tabCountMap[tab.label] ?: 0}",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        OutlinedButton(
                            onClick = { sortMenuExpanded = true },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(appSortModeLabel(sortMode))
                            Spacer(Modifier.width(2.dp))
                            Icon(Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            AppSortMode.entries.forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(appSortModeLabel(mode)) },
                                    onClick = {
                                        sortMode = mode
                                        sortMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(modifier = Modifier.padding(4.dp)) {
                            ViewToggleButton(
                                icon = Icons.Default.GridView,
                                selected = isGridView,
                                onClick = { viewModel.setGridView(true) }
                            )
                            ViewToggleButton(
                                icon = Icons.AutoMirrored.Filled.ViewList,
                                selected = !isGridView,
                                onClick = { viewModel.setGridView(false) }
                            )
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
            ) {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = l10n("应用加载中...", "Loading apps..."),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (displayedList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Apps,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(42.dp)
                            )
                            Text(
                                text = l10n("当前筛选条件下暂无应用", "No apps under current filters"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (isGridView) {
                    val gridState = rememberLazyGridState()
                    LaunchedEffect(gridState, displayedList) {
                        snapshotFlow {
                            gridState.layoutInfo.visibleItemsInfo
                                .mapNotNull { item -> displayedList.getOrNull(item.index)?.packageName }
                        }.collect { visiblePackages ->
                            viewModel.ensureAppAssetsVisible(visiblePackages)
                            viewModel.ensureAppDetailsVisible(visiblePackages)
                        }
                    }
                    Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(160.dp),
                            state = gridState,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(displayedList, key = { it.packageName }) { app ->
                                AppGridCard(
                                    app = app,
                                    icon = appIcons[app.packageName],
                                    onAction = { type -> viewModel.executeAdbAction(type, app.packageName) },
                                    onCopyPackageName = { viewModel.copyToClipboard(app.packageName) },
                                    onRequestDangerAction = { type, label, message ->
                                        pendingDangerAction = type to app
                                        confirmActionLabel = label
                                        confirmActionMessage = message
                                    }
                                )
                            }
                        }
                        AppListFooter(displayedList.size)
                    }
                } else {
                    val listState = rememberLazyListState()
                    LaunchedEffect(listState, displayedList) {
                        snapshotFlow {
                            listState.layoutInfo.visibleItemsInfo
                                .mapNotNull { item -> displayedList.getOrNull(item.index)?.packageName }
                        }.collect { visiblePackages ->
                            viewModel.ensureAppAssetsVisible(visiblePackages)
                            viewModel.ensureAppDetailsVisible(visiblePackages)
                        }
                    }
                    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                                items(displayedList, key = { it.packageName }) { app ->
                                    AppListRow(
                                        app = app,
                                        icon = appIcons[app.packageName],
                                        onAction = { type -> viewModel.executeAdbAction(type, app.packageName) },
                                        onCopyPackageName = { viewModel.copyToClipboard(app.packageName) },
                                        onRequestDangerAction = { type, label, message ->
                                            pendingDangerAction = type to app
                                            confirmActionLabel = label
                                            confirmActionMessage = message
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                            VerticalScrollbar(
                                adapter = rememberScrollbarAdapter(listState),
                                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                            )
                        }
                        AppListFooter(displayedList.size)
                    }
                }
            }
        }
    }

    pendingDangerAction?.let { (type, app) ->
        AlertDialog(
            onDismissRequest = { pendingDangerAction = null },
            title = { Text(confirmActionLabel) },
            text = { Text(confirmActionMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.executeAdbAction(type, app.packageName)
                        pendingDangerAction = null
                    }
                ) { Text(l10n("确认", "Confirm")) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDangerAction = null }) {
                    Text(l10n("取消", "Cancel"))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showDialog) {
        dialogMessage?.let {
            TipDialog(
                dialogText = when (it) {
                    is MsgContent.Resource -> stringResource(it.stringResource, *it.args.toTypedArray())
                    is MsgContent.Text -> it.text
                }
            ) { viewModel.dismissTipDialog() }
        }
    }
}

private fun formatRelativeRefresh(timestamp: Long): String {
    val delta = (System.currentTimeMillis() - timestamp).coerceAtLeast(0L) / 1000L
    return when {
        delta < 5L -> l10n("刚刚", "just now")
        delta < 60L -> if (l10n("zh", "en") == "en") "${delta}s ago" else "${delta}s 前"
        delta < 3600L -> if (l10n("zh", "en") == "en") "${delta / 60}m ago" else "${delta / 60}m 前"
        else -> if (l10n("zh", "en") == "en") "${delta / 3600}h ago" else "${delta / 3600}h 前"
    }
}

private fun appSortModeLabel(mode: AppSortMode): String {
    return when (mode) {
        AppSortMode.Name -> l10n("按名称", "By name")
        AppSortMode.Size -> l10n("按大小", "By size")
        AppSortMode.Version -> l10n("按版本", "By version")
        AppSortMode.InstallTime -> l10n("按安装时间", "By install time")
        AppSortMode.Recent -> l10n("按最近使用", "By recent")
    }
}

private fun appTabLabel(label: String): String {
    return when (label) {
        "全部应用" -> l10n("全部应用", "All apps")
        "用户应用" -> l10n("用户应用", "User apps")
        "系统应用" -> l10n("系统应用", "System apps")
        "可调试应用" -> l10n("可调试应用", "Debuggable")
        "最近使用" -> l10n("最近使用", "Recent")
        "运行中" -> l10n("运行中", "Running")
        else -> label
    }
}

@Composable
private fun AppStatBadge(
    label: String,
    value: String,
    emphasize: Boolean = false
) {
    val borderColor = if (emphasize) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else Color(0xFFE5E7EB)
    val bgColor = if (emphasize) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
    val valueColor = if (emphasize) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = valueColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun AppTypeTag(
    text: String,
    accent: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = accent.copy(alpha = 0.1f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ViewToggleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun AppTableHeader(
    sortBySizeEnabled: Boolean,
    sortAscending: Boolean,
    onSizeSortToggle: () -> Unit,
    showPackageName: Boolean,
    onToggleNameDisplay: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(2.6f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (showPackageName) l10n("包名", "Package") else l10n("应用名称", "App name"),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(
                imageVector = Icons.Default.SwapHoriz,
                contentDescription = l10n("切换显示", "Toggle display"),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .size(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(onClick = onToggleNameDisplay)
                    .padding(1.dp)
            )
        }
        Text(
            text = l10n("版本", "Version"),
            modifier = Modifier.weight(1.0f),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SortableHeaderLabel(
            text = l10n("大小", "Size"),
            modifier = Modifier.weight(1.0f),
            active = sortBySizeEnabled,
            ascending = sortAscending,
            onClick = onSizeSortToggle
        )
        Text(
            text = l10n("状态", "Status"),
            modifier = Modifier.weight(0.95f),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = l10n("操作", "Actions"),
            modifier = Modifier.weight(1.7f),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SortableHeaderLabel(
    text: String,
    modifier: Modifier = Modifier,
    active: Boolean,
    ascending: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .hoverable(interactionSource)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = if (ascending) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = if (active) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isHovered) 0.9f else 0.5f),
            modifier = Modifier.size(14.dp)
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun AppListRow(
    app: AppInfo,
    icon: ImageBitmap?,
    onAction: (AdbFunctionType) -> Unit,
    onCopyPackageName: () -> Unit,
    onRequestDangerAction: (AdbFunctionType, String, String) -> Unit
) {
    val rowInteraction = remember { MutableInteractionSource() }
    val isRowHovered by rowInteraction.collectIsHoveredAsState()
    var contextMenuExpanded by remember { mutableStateOf(false) }
    var contextMenuOffset by remember { mutableStateOf(DpOffset.Zero) }
    val density = LocalDensity.current
    val appName = app.appName.takeIf { it.isNotBlank() && it != app.packageName } ?: app.packageName

    Box(
        modifier = Modifier
            .hoverable(rowInteraction)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isRowHovered) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f) else Color.Transparent)
            .pointerInput(app.packageName) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.button == PointerButton.Secondary && event.type == PointerEventType.Press) {
                            val point = event.changes.firstOrNull()?.position
                            if (point != null) {
                                contextMenuOffset = with(density) { DpOffset(point.x.toDp(), point.y.toDp()) }
                            }
                            contextMenuExpanded = true
                        }
                    }
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(2.25f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppAvatar(app = app, icon = icon, size = 34)
                Spacer(Modifier.width(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = appName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                modifier = Modifier.weight(1.25f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppTypeTag(text = if (app.isSystemApp) l10n("系统应用", "System") else l10n("用户应用", "User"))
                if (app.isDebuggable) {
                    AppTypeTag(text = l10n("可调试", "Debuggable"), accent = MaterialTheme.colorScheme.primary)
                }
            }

            Text(
                text = app.versionName,
                modifier = Modifier
                    .weight(0.85f)
                    .padding(end = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = app.size,
                modifier = Modifier.weight(0.75f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Box(modifier = Modifier.weight(0.9f)) {
                val statusText = when {
                    app.isDisabled -> l10n("已禁用", "Disabled")
                    app.isRunning -> l10n("运行中", "Running")
                    else -> l10n("未运行", "Stopped")
                }
                val statusColor = when {
                    app.isDisabled -> Color(0xFF9CA3AF)
                    app.isRunning -> Color(0xFF2DBE60)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Surface(
                    color = statusColor.copy(alpha = if (app.isRunning) 0.14f else 0.1f),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.weight(1.2f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val actionAlpha = if (isRowHovered) 1f else 0.38f
                RowActionButton(
                    icon = Icons.Default.ChevronRight,
                    contentDescription = l10n("应用详情", "App details"),
                    tint = MaterialTheme.colorScheme.primary,
                    alpha = actionAlpha,
                    onClick = { onAction(AdbFunctionType.APP_INFO) }
                )
                if (app.isRunning) {
                    RowActionButton(
                        icon = Icons.Default.Stop,
                        contentDescription = l10n("停止应用", "Stop app"),
                        tint = Color(0xFFEF6C00),
                        alpha = actionAlpha,
                        onClick = { onAction(AdbFunctionType.FORCE_STOP) }
                    )
                } else {
                    RowActionButton(
                        icon = Icons.Default.PlayArrow,
                        contentDescription = l10n("启动应用", "Launch app"),
                        tint = Color(0xFF22A35A),
                        alpha = actionAlpha,
                        onClick = { onAction(AdbFunctionType.LAUNCH) }
                    )
                }
                AppActionMenu(
                    app = app,
                    alpha = actionAlpha,
                    onAction = onAction,
                    onCopyPackageName = onCopyPackageName,
                    onRequestDangerAction = onRequestDangerAction
                )
            }
        }

        Box(modifier = Modifier.offset(x = contextMenuOffset.x, y = contextMenuOffset.y)) {
            DropdownMenu(
                expanded = contextMenuExpanded,
                onDismissRequest = { contextMenuExpanded = false },
                modifier = Modifier
                    .width(180.dp)
                    .background(
                        MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(12.dp)
                    )
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(vertical = 4.dp)
            ) {
            AppActionMenuItem(
                icon = Icons.Default.ContentCopy,
                text = l10n("复制包名", "Copy package"),
                iconTint = MaterialTheme.colorScheme.primary
            ) {
                contextMenuExpanded = false
                onCopyPackageName()
            }
            AppActionMenuItem(
                icon = Icons.Default.ChevronRight,
                text = l10n("打开详情", "Open details"),
                iconTint = MaterialTheme.colorScheme.primary
            ) {
                contextMenuExpanded = false
                onAction(AdbFunctionType.APP_INFO)
            }
            AppActionMenuItem(
                icon = Icons.Default.DeleteSweep,
                text = l10n("清除数据", "Clear data"),
                iconTint = Color(0xFF1565C0)
            ) {
                contextMenuExpanded = false
                onRequestDangerAction(
                    AdbFunctionType.CLEAR_DATA,
                    l10n("确认清除应用数据", "Confirm clear app data"),
                    l10n("将清除 ${appName} 的本地数据，该操作不可撤销。", "This will clear local data of ${appName}. This action cannot be undone.")
                )
            }
            AppActionMenuItem(
                icon = Icons.Default.Download,
                text = l10n("导出 APK", "Export APK"),
                iconTint = Color(0xFF00897B)
            ) {
                contextMenuExpanded = false
                onAction(AdbFunctionType.EXPORT_APK)
            }
            AppActionMenuItem(
                icon = Icons.Default.PrivacyTip,
                text = l10n("查看权限", "View permissions"),
                iconTint = Color(0xFF5C6BC0)
            ) {
                contextMenuExpanded = false
                onAction(AdbFunctionType.APP_INFO)
            }
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
            )
            AppActionMenuItem(
                icon = Icons.Default.Delete,
                text = l10n("卸载应用", "Uninstall app"),
                iconTint = Color(0xFFE53935),
                textColor = Color(0xFFE53935)
            ) {
                contextMenuExpanded = false
                onRequestDangerAction(
                    AdbFunctionType.UNINSTALL,
                    l10n("确认卸载应用", "Confirm uninstall"),
                    l10n("将从设备卸载 ${appName}，请确认继续。", "App ${appName} will be uninstalled from device. Continue?")
                )
            }
            }
        }
    }
}

@Composable
private fun RowActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.primary,
    alpha: Float = 1f,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.size(28.dp).graphicsLayer { this.alpha = alpha },
        border = BorderStroke(1.dp, tint.copy(alpha = if (enabled) 0.3f else 0.1f))
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(13.dp), tint = tint)
    }
}

@Composable
private fun AppActionMenu(
    app: AppInfo,
    onAction: (AdbFunctionType) -> Unit,
    onCopyPackageName: () -> Unit,
    onRequestDangerAction: (AdbFunctionType, String, String) -> Unit,
    alpha: Float = 1f
) {
    var expanded by remember { mutableStateOf(false) }
    val appName = app.appName.takeIf { it.isNotBlank() && it != app.packageName } ?: app.packageName

    Box {
        Surface(
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .size(width = 30.dp, height = 28.dp)
                .graphicsLayer { this.alpha = alpha }
                .clickable { expanded = true }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = l10n("更多操作", "More actions"),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(180.dp)
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(12.dp)
                )
                .border(
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    RoundedCornerShape(12.dp)
                )
                .padding(vertical = 4.dp)
        ) {
            AppActionMenuItem(
                icon = Icons.Default.ContentCopy,
                text = l10n("复制包名", "Copy package"),
                iconTint = MaterialTheme.colorScheme.primary
            ) {
                expanded = false
                onCopyPackageName()
            }
            AppActionMenuItem(
                icon = Icons.Default.ChevronRight,
                text = l10n("打开详情", "Open details"),
                iconTint = MaterialTheme.colorScheme.primary
            ) {
                expanded = false
                onAction(AdbFunctionType.APP_INFO)
            }
            AppActionMenuItem(
                icon = if (app.isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                text = if (app.isRunning) l10n("强行停止", "Force stop") else l10n("启动应用", "Launch app"),
                iconTint = if (app.isRunning) Color(0xFFEF6C00) else Color(0xFF22A35A)
            ) {
                expanded = false
                if (app.isRunning) {
                    onRequestDangerAction(
                        AdbFunctionType.FORCE_STOP,
                        l10n("确认强行停止应用", "Confirm force stop"),
                        l10n("将强行停止 $appName。", "App $appName will be force stopped.")
                    )
                } else {
                    onAction(AdbFunctionType.LAUNCH)
                }
            }
            AppActionMenuItem(
                icon = Icons.Default.DeleteSweep,
                text = l10n("清除数据", "Clear data"),
                iconTint = Color(0xFF1565C0)
            ) {
                expanded = false
                onRequestDangerAction(
                    AdbFunctionType.CLEAR_DATA,
                    l10n("确认清除应用数据", "Confirm clear app data"),
                    l10n("将清除 $appName 的本地数据，该操作不可撤销。", "This will clear local data of $appName. This action cannot be undone.")
                )
            }
            AppActionMenuItem(
                icon = Icons.Default.Download,
                text = l10n("导出 APK", "Export APK"),
                iconTint = Color(0xFF00897B)
            ) {
                expanded = false
                onAction(AdbFunctionType.EXPORT_APK)
            }
            AppActionMenuItem(
                icon = Icons.Default.PrivacyTip,
                text = l10n("查看权限", "View permissions"),
                iconTint = Color(0xFF5C6BC0)
            ) {
                expanded = false
                onAction(AdbFunctionType.APP_INFO)
            }
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
            )
            AppActionMenuItem(
                icon = Icons.Default.Delete,
                text = l10n("卸载应用", "Uninstall app"),
                iconTint = Color(0xFFE53935),
                textColor = Color(0xFFE53935)
            ) {
                expanded = false
                onRequestDangerAction(
                    AdbFunctionType.UNINSTALL,
                    l10n("确认卸载应用", "Confirm uninstall"),
                    l10n("将从设备卸载 $appName，请确认继续。", "App $appName will be uninstalled from device. Continue?")
                )
            }
        }
    }
}

@Composable
private fun AppActionMenuItem(
    icon: ImageVector,
    text: String,
    iconTint: Color,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                fontWeight = FontWeight.Medium
            )
        },
        onClick = onClick,
        leadingIcon = {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = iconTint.copy(alpha = 0.1f),
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
                }
            }
        },
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun AppGridCard(
    app: AppInfo,
    icon: ImageBitmap?,
    onAction: (AdbFunctionType) -> Unit,
    onCopyPackageName: () -> Unit,
    onRequestDangerAction: (AdbFunctionType, String, String) -> Unit
) {
    var contextMenuExpanded by remember { mutableStateOf(false) }
    var contextMenuOffset by remember { mutableStateOf(DpOffset.Zero) }
    val density = LocalDensity.current

    Box(
        modifier = Modifier.pointerInput(app.packageName) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.button == PointerButton.Secondary && event.type == androidx.compose.ui.input.pointer.PointerEventType.Press) {
                        val point = event.changes.firstOrNull()?.position
                        if (point != null) {
                            contextMenuOffset = with(density) { DpOffset(point.x.toDp(), point.y.toDp()) }
                        }
                        contextMenuExpanded = true
                    }
                }
            }
        }
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
        ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(11.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppAvatar(app = app, icon = icon, size = 34)
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            if (app.isRunning) Color(0xFF2DBE60) else MaterialTheme.colorScheme.outlineVariant,
                            CircleShape
                        )
                )
            }

            Text(
                text = app.appName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (app.appName != app.packageName) {
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                RowActionButton(
                    icon = Icons.Default.Info,
                    contentDescription = l10n("应用详情", "App details"),
                    tint = Color(0xFF5C6BC0),
                    onClick = { onAction(AdbFunctionType.APP_INFO) }
                )
                if (app.isRunning) {
                    RowActionButton(
                        icon = Icons.Default.Stop,
                        contentDescription = l10n("停止应用", "Stop app"),
                        tint = Color(0xFFEF6C00),
                        onClick = { onAction(AdbFunctionType.FORCE_STOP) }
                    )
                } else {
                    RowActionButton(
                        icon = Icons.Default.PlayArrow,
                        contentDescription = l10n("启动应用", "Launch app"),
                        tint = Color(0xFF22A35A),
                        onClick = { onAction(AdbFunctionType.LAUNCH) }
                    )
                }
                AppActionMenu(
                    app = app,
                    onAction = onAction,
                    onCopyPackageName = onCopyPackageName,
                    onRequestDangerAction = onRequestDangerAction
                )
            }
        }
        }

        Box(modifier = Modifier.offset(x = contextMenuOffset.x, y = contextMenuOffset.y)) {
            DropdownMenu(
                expanded = contextMenuExpanded,
                onDismissRequest = { contextMenuExpanded = false },
                modifier = Modifier
                    .width(180.dp)
                    .background(
                        MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(12.dp)
                    )
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(vertical = 4.dp)
            ) {
            AppActionMenuItem(
                icon = Icons.Default.ContentCopy,
                text = l10n("复制包名", "Copy package"),
                iconTint = Color(0xFF5C6BC0)
            ) {
                contextMenuExpanded = false
                onCopyPackageName()
            }
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
            )
            AppActionMenuItem(
                icon = Icons.Default.Info,
                text = l10n("应用详情", "App details"),
                iconTint = Color(0xFF5C6BC0)
            ) {
                contextMenuExpanded = false
                onAction(AdbFunctionType.APP_INFO)
            }
            AppActionMenuItem(
                icon = Icons.Default.PlayArrow,
                text = l10n("启动应用", "Launch app"),
                iconTint = Color(0xFF22A35A)
            ) {
                contextMenuExpanded = false
                onAction(AdbFunctionType.LAUNCH)
            }
            AppActionMenuItem(
                icon = Icons.Default.Stop,
                text = l10n("停止应用", "Stop app"),
                iconTint = Color(0xFFEF6C00)
            ) {
                contextMenuExpanded = false
                onRequestDangerAction(
                    AdbFunctionType.FORCE_STOP,
                    l10n("确认强行停止应用", "Confirm force stop"),
                    l10n("将强行停止 ${app.appName}。", "App ${app.appName} will be force stopped.")
                )
            }
            AppActionMenuItem(
                icon = Icons.Default.DeleteSweep,
                text = l10n("清除数据", "Clear data"),
                iconTint = Color(0xFF1565C0)
            ) {
                contextMenuExpanded = false
                onRequestDangerAction(
                    AdbFunctionType.CLEAR_DATA,
                    l10n("确认清除应用数据", "Confirm clear app data"),
                    l10n("将清除 ${app.appName} 的本地数据，该操作不可撤销。", "This will clear local data of ${app.appName}. This action cannot be undone.")
                )
            }
            AppActionMenuItem(
                icon = Icons.Default.Download,
                text = l10n("导出 APK", "Export APK"),
                iconTint = Color(0xFF00897B)
            ) {
                contextMenuExpanded = false
                onAction(AdbFunctionType.EXPORT_APK)
            }
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
            )
            AppActionMenuItem(
                icon = Icons.Default.Delete,
                text = l10n("卸载应用", "Uninstall app"),
                iconTint = Color(0xFFE53935),
                textColor = Color(0xFFE53935)
            ) {
                contextMenuExpanded = false
                onRequestDangerAction(
                    AdbFunctionType.UNINSTALL,
                    l10n("确认卸载应用", "Confirm uninstall"),
                    l10n("将从设备卸载 ${app.appName}，请确认继续。", "App ${app.appName} will be uninstalled from device. Continue?")
                )
            }
            }
        }
    }
}

@Composable
private fun AppListFooter(count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp, end = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = l10n("共 $count 个应用", "$count apps"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = l10n("右键点击可快速操作", "Right click for quick actions"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun AppDetailPage(
    appInfo: AppInfoData,
    icon: ImageBitmap?,
    onBack: () -> Unit,
    onAction: (AdbFunctionType) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf("概览") }
    val detailTabs = listOf(
        DetailTab("概览"),
        DetailTab("权限"),
        DetailTab("Activity"),
        DetailTab("Service"),
        DetailTab("Receiver"),
        DetailTab("Provider"),
        DetailTab("签名")
    )

    val scrollState = rememberScrollState()

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = l10n("返回", "Back"), tint = MaterialTheme.colorScheme.onSurface)
                }
                Text(
                    text = l10n("应用详情", "App details"),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
            }

            DetailHeaderCard(
                appInfo = appInfo,
                icon = icon,
                onAction = onAction
            )

            val tabListState = rememberLazyListState()
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                LazyRow(
                    state = tabListState,
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(detailTabs) { tab ->
                        DetailTabItem(
                            tab = tab,
                            selected = selectedTab == tab.title,
                            onClick = { selectedTab = tab.title }
                        )
                    }
                }
                HorizontalDivider(color = Color(0xFFE5E7EB))
            }

            when (selectedTab) {
                "概览" -> AppInfoDetailContent(appInfo)
                "权限" -> PermissionDetailContent(appInfo)
                "Activity" -> ActivityDetailContent(appInfo)
                "Service" -> ServiceDetailContent(appInfo)
                "Receiver" -> ReceiverDetailContent(appInfo)
                "Provider" -> ProviderDetailContent(appInfo)
                "签名" -> SignatureDetailContent(appInfo)
                else -> DetailPlaceholder(selectedTab)
            }
        }

        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(scrollState),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(8.dp)
        )
    }
}

private data class DetailTab(
    val title: String
)

@Composable
private fun DetailHeaderCard(
    appInfo: AppInfoData,
    icon: ImageBitmap?,
    onAction: (AdbFunctionType) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    AppAvatar(
                        app = AppInfo(appName = appInfo.appName.ifBlank { appInfo.packageName }, packageName = appInfo.packageName),
                        icon = icon,
                        size = 76
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = appInfo.appName.ifBlank { appInfo.packageName },
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            StatusPill(appInfo.isRunning)
                        }
                        DetailHeaderRow(l10n("包名", "Package"), appInfo.packageName)
                        DetailHeaderRow(l10n("版本", "Version"), listOf(appInfo.versionName, appInfo.versionCode.takeIf { it != "-" }?.let { "($it)" }).filterNotNull().joinToString(" "))
                        DetailHeaderRow(l10n("类型", "Type"), if (appInfo.isSystemApp) l10n("系统应用", "System app") else l10n("用户应用", "User app"))
                        DetailHeaderRow(l10n("更新于", "Updated"), appInfo.lastUpdateTime)
                        DetailHeaderRow(l10n("安装路径", "Install path"), appInfo.apkPath.ifBlank { "-" }, maxLines = 2)
                    }
                }

                HeaderStorageUsageCard(
                    appInfo = appInfo,
                    modifier = Modifier.widthIn(min = 230.dp, max = 300.dp)
                )
            }

            DetailActionsPanel(
                appInfo = appInfo,
                onAction = onAction
            )
        }
    }
}

@Composable
private fun DetailHeaderRow(
    label: String,
    value: String,
    maxLines: Int = 1
) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = label,
            modifier = Modifier.width(92.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SelectionContainer {
            SelectableValueText(
                text = value.ifBlank { "-" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HeaderStorageUsageCard(
    appInfo: AppInfoData,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF8F9FB),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = l10n("存储占用", "Storage"),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StorageRing(total = appInfo.totalSize, size = 88.dp, innerSize = 62.dp)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StorageLegend(Color(0xFF1F6BFF), l10n("应用大小", "App size"), appInfo.appSize)
                    StorageLegend(MaterialTheme.colorScheme.onSurfaceVariant, l10n("应用数据", "App data"), appInfo.dataSize)
                    StorageLegend(MaterialTheme.colorScheme.onSurfaceVariant, l10n("缓存数据", "Cache"), appInfo.cacheSize)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = l10n("总计", "Total"),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = appInfo.totalSize,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailActionsPanel(
    appInfo: AppInfoData,
    onAction: (AdbFunctionType) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        DetailActionButton(
            icon = if (appInfo.isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
            text = if (appInfo.isRunning) l10n("停止应用", "Stop app") else l10n("启动应用", "Launch app"),
            tint = Color.White,
            bgColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(140.dp)
        ) {
            onAction(if (appInfo.isRunning) AdbFunctionType.FORCE_STOP else AdbFunctionType.LAUNCH)
        }
        DetailActionButton(
            icon = Icons.Default.Refresh,
            text = l10n("刷新", "Refresh"),
            tint = MaterialTheme.colorScheme.onSurface,
            bgColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.width(110.dp)
        ) { onAction(AdbFunctionType.APP_INFO) }
        DetailMoreActionMenu(onAction = onAction)
    }
}

@Composable
private fun StatusPill(isRunning: Boolean) {
    val color = if (isRunning) Color(0xFF2DBE60) else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        color = if (isRunning) Color(0xFFE9F9EF) else color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text = if (isRunning) l10n("运行中", "Running") else l10n("未运行", "Stopped"),
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun DetailActionButton(
    icon: ImageVector,
    text: String,
    tint: Color,
    bgColor: Color = MaterialTheme.colorScheme.surface,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(42.dp),
        shape = RoundedCornerShape(10.dp),
        color = if (enabled) bgColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(
            1.dp,
            if (bgColor == MaterialTheme.colorScheme.surface) Color(0xFFE5E7EB)
            else bgColor
        )
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = text,
                tint = if (enabled) tint else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(17.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text,
                color = if (enabled) tint else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun DetailMoreActionMenu(onAction: (AdbFunctionType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        DetailActionButton(
            icon = Icons.Default.MoreHoriz,
            text = l10n("更多操作", "More"),
            tint = MaterialTheme.colorScheme.onSurface,
            bgColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.width(124.dp)
        ) { expanded = true }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(190.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                .border(BorderStroke(1.dp, Color(0xFFE5E7EB)), RoundedCornerShape(12.dp))
        ) {
            AppActionMenuItem(
                icon = Icons.Default.DeleteSweep,
                text = l10n("清除数据", "Clear data"),
                iconTint = MaterialTheme.colorScheme.onSurfaceVariant
            ) { expanded = false; onAction(AdbFunctionType.CLEAR_DATA) }
            AppActionMenuItem(
                icon = Icons.Default.Download,
                text = l10n("导出 APK", "Export APK"),
                iconTint = MaterialTheme.colorScheme.onSurfaceVariant
            ) { expanded = false; onAction(AdbFunctionType.EXPORT_APK) }
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
            )
            AppActionMenuItem(
                icon = Icons.Default.Delete,
                text = l10n("卸载应用", "Uninstall app"),
                iconTint = Color(0xFFD93025),
                textColor = Color(0xFFD93025)
            ) { expanded = false; onAction(AdbFunctionType.UNINSTALL) }
        }
    }
}

@Composable
private fun DetailTabItem(
    tab: DetailTab,
    selected: Boolean,
    onClick: () -> Unit
) {
    val textColor = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .widthIn(min = 92.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = when (tab.title) {
                "概览" -> l10n("概览", "Overview")
                "权限" -> l10n("权限", "Permissions")
                "签名" -> l10n("签名", "Signature")
                else -> tab.title
            },
            color = textColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
        )
        Box(
            modifier = Modifier
                .height(2.5.dp)
                .fillMaxWidth()
                .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
        )
    }
}

@Composable
private fun AppInfoDetailContent(appInfo: AppInfoData) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth < 980.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                BasicInfoCard(appInfo)
                ProcessInfoCard(appInfo)
                PermissionSummaryCard(appInfo)
                OperationLogCard(appInfo)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    BasicInfoCard(appInfo, Modifier.weight(1.05f))
                    ProcessInfoCard(appInfo, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    PermissionSummaryCard(appInfo, Modifier.weight(1f))
                    OperationLogCard(appInfo, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DetailSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            content()
        }
    }
}

@Composable
private fun BasicInfoCard(appInfo: AppInfoData, modifier: Modifier = Modifier) {
    val infoItems = listOf(
        l10n("应用名称", "App name") to appInfo.appName.ifBlank { appInfo.packageName },
        l10n("包名", "Package") to appInfo.packageName,
        l10n("版本名称", "Version name") to appInfo.versionName,
        l10n("版本号", "Version code") to appInfo.versionCode,
        l10n("最小 SDK", "Min SDK") to appInfo.minSdk,
        l10n("目标 SDK", "Target SDK") to appInfo.targetSdk,
        l10n("安装位置", "Install location") to appInfo.installLocation,
        l10n("应用类型", "App type") to if (appInfo.isSystemApp) l10n("系统应用", "System app") else l10n("用户应用", "User app"),
        l10n("应用大小", "App size") to appInfo.appSize,
        l10n("数据目录", "Data dir") to appInfo.dataDir,
        l10n("APK 路径", "APK path") to appInfo.apkPath
    )
    DetailSectionCard(title = l10n("基本信息", "Basic info"), modifier = modifier) {
        DetailInfoGrid(items = infoItems)
    }
}

@Composable
private fun DetailInfoGrid(items: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { (label, value) ->
                    DetailInfoGridCell(
                        label = label,
                        value = value,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DetailInfoGridCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFF8F9FB),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SelectionContainer {
                SelectableValueText(
                    text = value.ifBlank { "-" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun StorageInfoCard(appInfo: AppInfoData, modifier: Modifier = Modifier) {
    DetailSectionCard(title = l10n("存储占用", "Storage"), modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StorageRing(total = appInfo.totalSize)
            Column(
                modifier = Modifier.weight(1f).padding(start = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StorageLegend(Color(0xFF1F6BFF), l10n("应用大小", "App size"), appInfo.appSize)
                StorageLegend(Color(0xFF38C989), l10n("应用数据", "App data"), appInfo.dataSize)
                StorageLegend(Color(0xFFFF8A1F), l10n("缓存数据", "Cache"), appInfo.cacheSize)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                DetailInfoRow(l10n("总计", "Total"), appInfo.totalSize)
            }
        }
    }
}

@Composable
private fun StorageRing(
    total: String,
    size: Dp = 126.dp,
    innerSize: Dp = 92.dp
) {
    val compact = size <= 92.dp
    val normalized = total.takeIf { it.isNotBlank() && it != "-" } ?: "-"
    val valueText = normalized.substringBefore(" ").ifBlank { normalized }
    val unitText = normalized.substringAfter(" ", "")

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(innerSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (compact) 3.dp else 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = valueText,
                    style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (unitText.isNotBlank()) {
                    Text(
                        text = unitText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun StorageLegend(color: Color, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ProcessInfoCard(appInfo: AppInfoData, modifier: Modifier = Modifier) {
    DetailSectionCard(title = l10n("进程信息", "Process info"), modifier = modifier) {
        DetailInfoRow(l10n("主进程", "Main process"), appInfo.packageName)
        DetailInfoRow(l10n("进程 ID (PID)", "Process ID (PID)"), appInfo.processId)
        DetailInfoRow(l10n("内存占用", "Memory usage"), appInfo.memoryUsage)
        DetailInfoRow(l10n("状态", "Status"), if (appInfo.isRunning) l10n("运行中", "Running") else l10n("未运行", "Stopped"))
        DetailInfoRow(l10n("启动时间", "Start time"), appInfo.startTime)
    }
}

@Composable
private fun PermissionSummaryCard(appInfo: AppInfoData, modifier: Modifier = Modifier) {
    DetailSectionCard(title = l10n("权限统计", "Permission summary"), modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PermissionTile(l10n("危险权限", "Dangerous"), appInfo.dangerousPermissionCount, Icons.Default.GppMaybe, Color(0xFFDF4C4C), Modifier.weight(1f))
            PermissionTile(l10n("隐私权限", "Privacy"), appInfo.privacyPermissionCount, Icons.Default.Lock, Color(0xFF1F6BFF), Modifier.weight(1f))
            PermissionTile(l10n("普通权限", "Normal"), appInfo.normalPermissionCount, Icons.Default.Info, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.weight(1f))
            PermissionTile(l10n("全部权限", "All"), appInfo.totalPermissionCount, Icons.Default.Apps, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.weight(1f))
        }
        OutlinedButton(
            onClick = {},
            enabled = false,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().height(42.dp)
        ) {
            Text(l10n("查看权限详情", "View permission details"))
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun PermissionTile(
    label: String,
    value: Int,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(112.dp),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier
                    .size(30.dp)
                    .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(6.dp)
            )
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun OperationLogCard(appInfo: AppInfoData, modifier: Modifier = Modifier) {
    DetailSectionCard(title = l10n("操作记录", "Operation log"), modifier = modifier) {
        OperationRow(l10n("安装应用", "Install app"), appInfo.firstInstallTime)
        OperationRow(l10n("更新应用", "Update app"), appInfo.lastUpdateTime)
        OperationRow(l10n("读取详情", "Read details"), l10n("当前会话", "Current session"))
        OperationRow(l10n("运行状态", "Run status"), if (appInfo.isRunning) l10n("运行中", "Running") else l10n("未运行", "Stopped"))
        OutlinedButton(
            onClick = {},
            enabled = false,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().height(42.dp)
        ) {
            Text(l10n("查看更多记录", "View more logs"))
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun OperationRow(action: String, time: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(action, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(time, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DetailInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            modifier = Modifier.width(96.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SelectionContainer(modifier = Modifier.weight(1f)) {
            SelectableValueText(
                text = value.ifBlank { "-" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PermissionDetailContent(appInfo: AppInfoData) {
    var selectedFilter by remember { mutableStateOf("全部") }

    val dangerousKeywords = listOf(
        "CAMERA", "LOCATION", "RECORD_AUDIO", "CONTACTS", "SMS", "PHONE",
        "CALENDAR", "BODY_SENSORS", "STORAGE", "BLUETOOTH"
    )
    val privacyKeywords = listOf("CAMERA", "LOCATION", "RECORD_AUDIO", "CONTACTS", "SMS", "PHONE")

    val filteredPermissions = remember(appInfo.permissionDetails, selectedFilter) {
        when (selectedFilter) {
            "危险权限" -> appInfo.permissionDetails.filter { permission ->
                dangerousKeywords.any { keyword -> permission.contains(keyword) }
            }
            "隐私权限" -> appInfo.permissionDetails.filter { permission ->
                privacyKeywords.any { keyword -> permission.contains(keyword) }
            }
            "普通权限" -> appInfo.permissionDetails.filter { permission ->
                dangerousKeywords.none { keyword -> permission.contains(keyword) }
            }
            else -> appInfo.permissionDetails
        }
    }

    DetailSectionCard(title = l10n("权限信息", "Permission info"), modifier = Modifier.fillMaxWidth()) {
        Text(
            text = l10n(
                "已解析到 ${appInfo.totalPermissionCount} 项权限，其中 ${appInfo.dangerousPermissionCount} 项可能涉及敏感能力。",
                "Detected ${appInfo.totalPermissionCount} permissions, ${appInfo.dangerousPermissionCount} of them may be sensitive."
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PermissionTile(l10n("危险权限", "Dangerous"), appInfo.dangerousPermissionCount, Icons.Default.GppMaybe, Color(0xFFFF4D4F), Modifier.width(132.dp))
            PermissionTile(l10n("隐私权限", "Privacy"), appInfo.privacyPermissionCount, Icons.Default.Lock, Color(0xFF1F6BFF), Modifier.width(132.dp))
            PermissionTile(l10n("普通权限", "Normal"), appInfo.normalPermissionCount, Icons.Default.Info, Color(0xFF0EA5E9), Modifier.width(132.dp))
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf("全部", "危险权限", "隐私权限", "普通权限").forEach { filter ->
                val selected = selectedFilter == filter
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = BorderStroke(
                        1.dp,
                        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.clickable { selectedFilter = filter }
                ) {
                    Text(
                        text = when (filter) {
                            "全部" -> l10n("全部", "All")
                            "危险权限" -> l10n("危险权限", "Dangerous")
                            "隐私权限" -> l10n("隐私权限", "Privacy")
                            "普通权限" -> l10n("普通权限", "Normal")
                            else -> filter
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }
        }

        DetailItemList(
            title = l10n("权限明细 (${filteredPermissions.size})", "Permissions (${filteredPermissions.size})"),
            items = filteredPermissions,
            emptyText = l10n("当前筛选条件下无权限", "No permissions under current filter")
        )
    }
}

@Composable
private fun ActivityDetailContent(appInfo: AppInfoData) {
    DetailSectionCard(title = l10n("活动（Activity）", "Activities"), modifier = Modifier.fillMaxWidth()) {
        DetailItemList(
            title = "Activity 列表",
            items = appInfo.activityDetails,
            emptyText = l10n("当前未解析到 Activity 信息", "No Activity information parsed")
        )
    }
}

@Composable
private fun ServiceDetailContent(appInfo: AppInfoData) {
    DetailSectionCard(title = l10n("服务（Service）", "Services"), modifier = Modifier.fillMaxWidth()) {
        DetailItemList(
            title = "Service 列表",
            items = appInfo.serviceDetails,
            emptyText = l10n("当前未解析到 Service 信息", "No Service information parsed")
        )
    }
}

@Composable
private fun ReceiverDetailContent(appInfo: AppInfoData) {
    DetailSectionCard(title = l10n("广播接收器", "Receivers"), modifier = Modifier.fillMaxWidth()) {
        DetailItemList(
            title = "Receiver 列表",
            items = appInfo.receiverDetails,
            emptyText = l10n("当前未解析到 Receiver 信息", "No Receiver information parsed")
        )
    }
}

@Composable
private fun ProviderDetailContent(appInfo: AppInfoData) {
    DetailSectionCard(title = l10n("内容提供者", "Providers"), modifier = Modifier.fillMaxWidth()) {
        DetailItemList(
            title = "Provider 列表",
            items = appInfo.providerDetails,
            emptyText = l10n("当前未解析到 Provider 信息", "No Provider information parsed")
        )
    }
}

@Composable
private fun SignatureDetailContent(appInfo: AppInfoData) {
    DetailSectionCard(title = l10n("签名信息", "Signature"), modifier = Modifier.fillMaxWidth()) {
        DetailItemList(
            title = l10n("签名相关", "Signature details"),
            items = appInfo.signatureDetails,
            emptyText = l10n("当前未解析到签名信息", "No signature information parsed")
        )
    }
}

@Composable
private fun DetailItemList(
    title: String,
    items: List<String>,
    emptyText: String
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
    )
    if (items.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = emptyText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items.forEach { item ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                        ) {
                            SelectionContainer {
                                SelectableValueText(
                                    text = item,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
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
private fun SelectableValueText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
    maxLines: Int,
    overflow: TextOverflow,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier,
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = overflow
    )
}

@Composable
private fun DetailPlaceholder(title: String) {
    DetailSectionCard(title = title, modifier = Modifier.fillMaxWidth().heightIn(min = 280.dp)) {
        Box(
            modifier = Modifier.fillMaxWidth().height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = l10n("当前版本暂未解析该分类数据", "This category is not parsed in current version"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
