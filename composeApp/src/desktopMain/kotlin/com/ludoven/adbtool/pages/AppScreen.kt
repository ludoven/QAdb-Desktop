package com.ludoven.adbtool.pages

import adbtool_desktop.composeapp.generated.resources.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ludoven.adbtool.entity.AdbFunctionType
import com.ludoven.adbtool.entity.AppInfoData
import com.ludoven.adbtool.entity.MsgContent
import com.ludoven.adbtool.util.AdbTool
import com.ludoven.adbtool.viewmodel.AppViewModel
import org.jetbrains.compose.resources.stringResource

data class AppInfo(
    val appName: String,
    val packageName: String,
    val apkPath: String = "",
    val isSystemApp: Boolean = false,
    val versionName: String = "-",
    val installTime: String = "-",
    val size: String = "-",
    val isRunning: Boolean = false
)

fun getInstalledApps(): List<AppInfo> {
    val allApps = AdbTool.exec("pm list packages -f") ?: return emptyList()
    val sysApps = AdbTool.exec("pm list packages -s") ?: ""
    val sysPackages = sysApps.lines()
        .mapNotNull { it.substringAfter("package:", "").takeIf { p -> p.isNotEmpty() } }
        .toSet()
    return allApps.lines().mapNotNull { line ->
        val regex = Regex("""package:(.+)=([A-Za-z0-9._]+)""")
        val match = regex.find(line)
        val (apkPath, packageName) = match?.destructured ?: return@mapNotNull null
        AppInfo(
            appName = packageName,
            packageName = packageName,
            apkPath = apkPath,
            isSystemApp = sysPackages.contains(packageName)
        )
    }.sortedBy { it.packageName }
}

private val avatarColors = listOf(
    Color(0xFF5C6BC0), Color(0xFF42A5F5), Color(0xFF26A69A), Color(0xFF66BB6A),
    Color(0xFFFFA726), Color(0xFFEF5350), Color(0xFFAB47BC), Color(0xFF26C6DA),
    Color(0xFFFF7043), Color(0xFFEC407A)
)

private fun avatarColor(name: String): Color {
    val idx = (name.firstOrNull()?.code ?: 0) % avatarColors.size
    return avatarColors[idx]
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
                    .background(avatarColor(app.appName)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = app.appName.take(1).uppercase(),
                    color = Color.White,
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
    val appIcons by viewModel.appIcons.collectAsState()
    val dialogMessage by viewModel.dialogMessage.collectAsState()
    val showDialog by viewModel.showDialog.collectAsState()
    val appInfo by viewModel.appInfo.collectAsState()

    var filterExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.getAppList() }

    val tabs = listOf("全部应用", "用户应用", "系统应用", "可调试应用", "最近使用")

    val filteredList = appList.filter { app ->
        val matchesSearch = app.appName.contains(searchText, ignoreCase = true) ||
            app.packageName.contains(searchText, ignoreCase = true)
        val matchesTab = when (selectedTab) {
            "用户应用" -> !app.isSystemApp
            "系统应用" -> app.isSystemApp
            else -> true
        }
        matchesSearch && matchesTab
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
                verticalAlignment = Alignment.Top
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "应用管理",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.width(6.dp))
                        IconButton(
                            onClick = { viewModel.getAppList() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "刷新",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = "已安装应用 ${appList.size} 个 | 用户应用 ${appList.count { !it.isSystemApp }} 个 | 系统应用 ${appList.count { it.isSystemApp }} 个",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { viewModel.setSearchText(it) },
                        placeholder = { Text("搜索应用名称或包名") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            ) {
                                Text(
                                    text = "⌘F",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.width(290.dp).height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    )

                    Box {
                        OutlinedButton(
                            onClick = { filterExpanded = true },
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            modifier = Modifier.height(52.dp)
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("筛选")
                            Spacer(Modifier.width(2.dp))
                            Icon(Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        DropdownMenu(expanded = filterExpanded, onDismissRequest = { filterExpanded = false }) {
                            listOf("全部应用", "用户应用", "系统应用").forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        viewModel.setSelectedTab(option)
                                        filterExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
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

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tabs.forEach { tab ->
                    val selected = selectedTab == tab
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.clickable { viewModel.setSelectedTab(tab) }
                    ) {
                        Text(
                            text = tab,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
            ) {
                if (isGridView) {
                    val gridState = rememberLazyGridState()
                    LaunchedEffect(gridState, filteredList) {
                        snapshotFlow {
                            gridState.layoutInfo.visibleItemsInfo
                                .mapNotNull { item -> filteredList.getOrNull(item.index)?.packageName }
                        }.collect { visiblePackages ->
                            viewModel.ensureAppAssetsVisible(visiblePackages)
                        }
                    }
                    Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(240.dp),
                            state = gridState,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(filteredList, key = { it.packageName }) { app ->
                                AppGridCard(
                                    app = app,
                                    icon = appIcons[app.packageName],
                                    onAction = { type -> viewModel.executeAdbAction(type, app.packageName) }
                                )
                            }
                        }
                        AppListFooter(filteredList.size)
                    }
                } else {
                    val listState = rememberLazyListState()
                    LaunchedEffect(listState, filteredList) {
                        snapshotFlow {
                            listState.layoutInfo.visibleItemsInfo
                                .mapNotNull { item -> filteredList.getOrNull(item.index)?.packageName }
                        }.collect { visiblePackages ->
                            viewModel.ensureAppAssetsVisible(visiblePackages)
                        }
                    }
                    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp)) {
                        AppTableHeader()
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                                items(filteredList, key = { it.packageName }) { app ->
                                    AppListRow(
                                        app = app,
                                        icon = appIcons[app.packageName],
                                        onAction = { type -> viewModel.executeAdbAction(type, app.packageName) }
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f))
                                }
                            }
                            VerticalScrollbar(
                                adapter = rememberScrollbarAdapter(listState),
                                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                            )
                        }
                        AppListFooter(filteredList.size)
                    }
                }
            }
        }
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
private fun AppTableHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "应用名称",
            modifier = Modifier.weight(2.6f),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "版本",
            modifier = Modifier.weight(1.0f),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "大小",
            modifier = Modifier.weight(0.9f),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "安装时间",
            modifier = Modifier.weight(1.5f),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "状态",
            modifier = Modifier.weight(0.9f),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "操作",
            modifier = Modifier.weight(2.0f),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AppListRow(
    app: AppInfo,
    icon: ImageBitmap?,
    onAction: (AdbFunctionType) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(2.6f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppAvatar(app = app, icon = icon, size = 40)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
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
            }
        }

        Text(
            text = app.versionName,
            modifier = Modifier.weight(1.0f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = app.size,
            modifier = Modifier.weight(0.9f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = app.installTime,
            modifier = Modifier.weight(1.5f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Box(modifier = Modifier.weight(0.9f)) {
            val statusColor = if (app.isRunning) Color(0xFF22A35A) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            Surface(
                color = statusColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(999.dp)
            ) {
                Text(
                    text = if (app.isRunning) "运行中" else "未运行",
                    color = statusColor,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        Row(
            modifier = Modifier.weight(2.0f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RowActionButton(
                icon = Icons.Default.PlayArrow,
                text = "启动",
                onClick = { onAction(AdbFunctionType.LAUNCH) }
            )
            RowActionButton(
                icon = Icons.Default.Stop,
                text = "停止",
                enabled = app.isRunning,
                onClick = { onAction(AdbFunctionType.FORCE_STOP) }
            )
            AppActionMenu(onAction = onAction)
        }
    }
}

@Composable
private fun RowActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 9.dp, vertical = 5.dp),
        modifier = Modifier.height(34.dp)
    ) {
        Icon(icon, contentDescription = text, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun AppActionMenu(
    onAction: (AdbFunctionType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .size(width = 36.dp, height = 34.dp)
                .clickable { expanded = true }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "更多操作",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("应用详情") },
                onClick = {
                    expanded = false
                    onAction(AdbFunctionType.APP_INFO)
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("启动应用") },
                onClick = {
                    expanded = false
                    onAction(AdbFunctionType.LAUNCH)
                },
                leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("停止应用") },
                onClick = {
                    expanded = false
                    onAction(AdbFunctionType.FORCE_STOP)
                },
                leadingIcon = { Icon(Icons.Default.Stop, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("强制停止") },
                onClick = {
                    expanded = false
                    onAction(AdbFunctionType.FORCE_STOP)
                },
                leadingIcon = { Icon(Icons.Default.Cancel, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("清除数据") },
                onClick = {
                    expanded = false
                    onAction(AdbFunctionType.CLEAR_DATA)
                },
                leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("卸载应用") },
                onClick = {
                    expanded = false
                    onAction(AdbFunctionType.UNINSTALL)
                },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFE53935)) }
            )
            DropdownMenuItem(
                text = { Text("导出 APK") },
                onClick = {
                    expanded = false
                    onAction(AdbFunctionType.EXPORT_APK)
                },
                leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) }
            )
        }
    }
}

@Composable
private fun AppGridCard(
    app: AppInfo,
    icon: ImageBitmap?,
    onAction: (AdbFunctionType) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppAvatar(app = app, icon = icon, size = 48)
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            if (app.isRunning) Color(0xFF22A35A) else MaterialTheme.colorScheme.outlineVariant,
                            CircleShape
                        )
                )
            }

            Text(
                text = app.appName,
                style = MaterialTheme.typography.titleMedium,
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

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RowActionButton(
                    icon = Icons.Default.PlayArrow,
                    text = "启动",
                    onClick = { onAction(AdbFunctionType.LAUNCH) }
                )
                RowActionButton(
                    icon = Icons.Default.Stop,
                    text = "停止",
                    enabled = app.isRunning,
                    onClick = { onAction(AdbFunctionType.FORCE_STOP) }
                )
                AppActionMenu(onAction = onAction)
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
            text = "共 $count 个应用",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "应用按名称排序",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
    var selectedTab by remember { mutableStateOf("应用信息") }
    val detailTabs = listOf(
        DetailTab("应用信息", Icons.AutoMirrored.Filled.Assignment),
        DetailTab("权限信息", Icons.Default.PrivacyTip),
        DetailTab("活动（Activity）", Icons.Default.PlayCircle),
        DetailTab("服务（Service）", Icons.Default.Hub),
        DetailTab("广播接收器", Icons.Default.SettingsInputAntenna),
        DetailTab("内容提供者", Icons.Default.Inventory2),
        DetailTab("签名信息", Icons.Default.VerifiedUser)
    )

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(34.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                text = "应用管理",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text("/", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = "应用详情",
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            detailTabs.forEach { tab ->
                DetailTabItem(
                    tab = tab,
                    selected = selectedTab == tab.title,
                    onClick = { selectedTab = tab.title }
                )
            }
        }

        when (selectedTab) {
            "应用信息" -> AppInfoDetailContent(appInfo)
            "权限信息" -> PermissionDetailContent(appInfo)
            else -> DetailPlaceholder(selectedTab)
        }
    }
}

private data class DetailTab(
    val title: String,
    val icon: ImageVector
)

@Composable
private fun DetailHeaderCard(
    appInfo: AppInfoData,
    icon: ImageBitmap?,
    onAction: (AdbFunctionType) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
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
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = appInfo.appName.ifBlank { appInfo.packageName },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        StatusPill(appInfo.isRunning)
                    }
                    DetailHeaderRow("包名", appInfo.packageName)
                    DetailHeaderRow("版本", listOf(appInfo.versionName, appInfo.versionCode.takeIf { it != "-" }?.let { "($it)" }).filterNotNull().joinToString(" "))
                    DetailHeaderRow("安装时间", appInfo.firstInstallTime)
                    DetailHeaderRow("更新于", appInfo.lastUpdateTime)
                    DetailHeaderRow("路径", appInfo.apkPath.ifBlank { "-" })
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DetailActionButton(Icons.Default.PlayArrow, "启动", MaterialTheme.colorScheme.primary) {
                    onAction(AdbFunctionType.LAUNCH)
                }
                DetailActionButton(Icons.Default.Stop, "停止", MaterialTheme.colorScheme.onSurface, enabled = appInfo.isRunning) {
                    onAction(AdbFunctionType.FORCE_STOP)
                }
                DetailActionButton(Icons.Default.DeleteSweep, "清数据", MaterialTheme.colorScheme.primary) {
                    onAction(AdbFunctionType.CLEAR_DATA)
                }
                DetailActionButton(Icons.Default.Delete, "卸载", Color(0xFFE53935)) {
                    onAction(AdbFunctionType.UNINSTALL)
                }
                DetailMoreButton(onAction)
            }
        }
    }
}

@Composable
private fun DetailHeaderRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = label,
            modifier = Modifier.width(92.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value.ifBlank { "-" },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StatusPill(isRunning: Boolean) {
    val color = if (isRunning) Color(0xFF18A957) else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text = if (isRunning) "运行中" else "未运行",
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
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        modifier = Modifier.height(40.dp),
        border = BorderStroke(1.dp, tint.copy(alpha = if (enabled) 0.35f else 0.12f))
    ) {
        Icon(icon, contentDescription = text, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(7.dp))
        Text(text, color = tint, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun DetailMoreButton(onAction: (AdbFunctionType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            modifier = Modifier.height(40.dp)
        ) {
            Icon(Icons.Default.MoreVert, contentDescription = "更多", modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("更多", style = MaterialTheme.typography.labelLarge)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("导出 APK") },
                onClick = {
                    expanded = false
                    onAction(AdbFunctionType.EXPORT_APK)
                },
                leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("刷新详情") },
                onClick = {
                    expanded = false
                    onAction(AdbFunctionType.APP_INFO)
                },
                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) }
            )
        }
    }
}

@Composable
private fun DetailTabItem(
    tab: DetailTab,
    selected: Boolean,
    onClick: () -> Unit
) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .widthIn(min = 116.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(tab.icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(7.dp))
            Text(
                text = tab.title,
                color = color,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1
            )
        }
        Box(
            modifier = Modifier
                .height(2.dp)
                .fillMaxWidth()
                .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
        )
    }
}

@Composable
private fun AppInfoDetailContent(appInfo: AppInfoData) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth < 980.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                BasicInfoCard(appInfo)
                StorageInfoCard(appInfo)
                ProcessInfoCard(appInfo)
                PermissionSummaryCard(appInfo)
                OperationLogCard(appInfo)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    BasicInfoCard(appInfo, Modifier.weight(1.05f))
                    StorageInfoCard(appInfo, Modifier.weight(1f))
                    ProcessInfoCard(appInfo, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
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
        modifier = modifier.heightIn(min = 208.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
    DetailSectionCard(title = "基本信息", modifier = modifier) {
        DetailInfoRow("应用名称", appInfo.appName.ifBlank { appInfo.packageName })
        DetailInfoRow("包名", appInfo.packageName)
        DetailInfoRow("版本名称", appInfo.versionName)
        DetailInfoRow("版本号", appInfo.versionCode)
        DetailInfoRow("最小 SDK", appInfo.minSdk)
        DetailInfoRow("目标 SDK", appInfo.targetSdk)
        DetailInfoRow("安装位置", appInfo.installLocation)
        DetailInfoRow("安装来源", "adb install")
        DetailInfoRow("应用大小", appInfo.appSize)
        DetailInfoRow("数据目录", appInfo.dataDir)
        DetailInfoRow("APK 路径", appInfo.apkPath)
    }
}

@Composable
private fun StorageInfoCard(appInfo: AppInfoData, modifier: Modifier = Modifier) {
    DetailSectionCard(title = "存储占用", modifier = modifier) {
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
                StorageLegend(Color(0xFF1F6BFF), "应用大小", appInfo.appSize)
                StorageLegend(Color(0xFF38C989), "应用数据", appInfo.dataSize)
                StorageLegend(Color(0xFFFF8A1F), "缓存数据", appInfo.cacheSize)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                DetailInfoRow("总计", appInfo.totalSize)
            }
        }
    }
}

@Composable
private fun StorageRing(total: String) {
    Box(
        modifier = Modifier
            .size(126.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(92.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = total.takeIf { it != "-" }?.substringBefore(" ") ?: "-",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = total.takeIf { it.contains(" ") }?.substringAfter(" ") ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
    DetailSectionCard(title = "进程信息", modifier = modifier) {
        DetailInfoRow("主进程", appInfo.packageName)
        DetailInfoRow("进程 ID (PID)", appInfo.processId)
        DetailInfoRow("内存占用", appInfo.memoryUsage)
        DetailInfoRow("状态", if (appInfo.isRunning) "运行中" else "未运行")
        DetailInfoRow("启动时间", appInfo.startTime)
    }
}

@Composable
private fun PermissionSummaryCard(appInfo: AppInfoData, modifier: Modifier = Modifier) {
    DetailSectionCard(title = "权限统计", modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PermissionTile("危险权限", appInfo.dangerousPermissionCount, Icons.Default.GppMaybe, Color(0xFFFF4D4F), Modifier.weight(1f))
            PermissionTile("隐私权限", appInfo.privacyPermissionCount, Icons.Default.Lock, Color(0xFF1F6BFF), Modifier.weight(1f))
            PermissionTile("普通权限", appInfo.normalPermissionCount, Icons.Default.Info, Color(0xFF0EA5E9), Modifier.weight(1f))
            PermissionTile("全部权限", appInfo.totalPermissionCount, Icons.Default.Apps, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.weight(1f))
        }
        OutlinedButton(
            onClick = {},
            enabled = false,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().height(42.dp)
        ) {
            Text("查看权限详情")
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
    DetailSectionCard(title = "操作记录", modifier = modifier) {
        OperationRow("安装应用", appInfo.firstInstallTime)
        OperationRow("更新应用", appInfo.lastUpdateTime)
        OperationRow("读取详情", "当前会话")
        OperationRow("运行状态", if (appInfo.isRunning) "运行中" else "未运行")
        OutlinedButton(
            onClick = {},
            enabled = false,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().height(42.dp)
        ) {
            Text("查看更多记录")
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
        Text(
            text = value.ifBlank { "-" },
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PermissionDetailContent(appInfo: AppInfoData) {
    DetailSectionCard(title = "权限信息", modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "已解析到 ${appInfo.totalPermissionCount} 项权限，其中 ${appInfo.dangerousPermissionCount} 项可能涉及敏感能力。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PermissionTile("危险权限", appInfo.dangerousPermissionCount, Icons.Default.GppMaybe, Color(0xFFFF4D4F), Modifier.width(132.dp))
            PermissionTile("隐私权限", appInfo.privacyPermissionCount, Icons.Default.Lock, Color(0xFF1F6BFF), Modifier.width(132.dp))
            PermissionTile("普通权限", appInfo.normalPermissionCount, Icons.Default.Info, Color(0xFF0EA5E9), Modifier.width(132.dp))
        }
    }
}

@Composable
private fun DetailPlaceholder(title: String) {
    DetailSectionCard(title = title, modifier = Modifier.fillMaxWidth().heightIn(min = 280.dp)) {
        Box(
            modifier = Modifier.fillMaxWidth().height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "当前版本暂未解析该分类数据",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
