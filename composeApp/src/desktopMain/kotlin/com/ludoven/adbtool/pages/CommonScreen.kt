package com.ludoven.adbtool.pages

import adbtool_desktop.composeapp.generated.resources.Res
import adbtool_desktop.composeapp.generated.resources.back_action
import adbtool_desktop.composeapp.generated.resources.back_action_desc
import adbtool_desktop.composeapp.generated.resources.cancel
import adbtool_desktop.composeapp.generated.resources.capture_logs
import adbtool_desktop.composeapp.generated.resources.capture_logs_desc
import adbtool_desktop.composeapp.generated.resources.clear_cache_and_restart
import adbtool_desktop.composeapp.generated.resources.clear_cache_and_restart_desc
import adbtool_desktop.composeapp.generated.resources.common_functions
import adbtool_desktop.composeapp.generated.resources.common_functions_subtitle
import adbtool_desktop.composeapp.generated.resources.common_search_placeholder
import adbtool_desktop.composeapp.generated.resources.confirm
import adbtool_desktop.composeapp.generated.resources.debug_tools_section
import adbtool_desktop.composeapp.generated.resources.debug_tools_section_subtitle
import adbtool_desktop.composeapp.generated.resources.device_control_section
import adbtool_desktop.composeapp.generated.resources.device_control_section_subtitle
import adbtool_desktop.composeapp.generated.resources.escape_spaces
import adbtool_desktop.composeapp.generated.resources.execute
import adbtool_desktop.composeapp.generated.resources.file_manager
import adbtool_desktop.composeapp.generated.resources.file_manager_desc
import adbtool_desktop.composeapp.generated.resources.home_action
import adbtool_desktop.composeapp.generated.resources.home_action_desc
import adbtool_desktop.composeapp.generated.resources.input_hint
import adbtool_desktop.composeapp.generated.resources.input_text
import adbtool_desktop.composeapp.generated.resources.input_text_desc
import adbtool_desktop.composeapp.generated.resources.install_and_open
import adbtool_desktop.composeapp.generated.resources.install_and_open_desc
import adbtool_desktop.composeapp.generated.resources.install_app
import adbtool_desktop.composeapp.generated.resources.install_app_desc
import adbtool_desktop.composeapp.generated.resources.launch_app_short
import adbtool_desktop.composeapp.generated.resources.launch_app_short_desc
import adbtool_desktop.composeapp.generated.resources.one_tap_actions_section
import adbtool_desktop.composeapp.generated.resources.one_tap_actions_section_subtitle
import adbtool_desktop.composeapp.generated.resources.package_input_title
import adbtool_desktop.composeapp.generated.resources.package_name_hint
import adbtool_desktop.composeapp.generated.resources.quick_actions_section
import adbtool_desktop.composeapp.generated.resources.quick_actions_section_subtitle
import adbtool_desktop.composeapp.generated.resources.reboot_device
import adbtool_desktop.composeapp.generated.resources.reboot_device_desc
import adbtool_desktop.composeapp.generated.resources.screen_record
import adbtool_desktop.composeapp.generated.resources.screen_record_desc
import adbtool_desktop.composeapp.generated.resources.screenshot
import adbtool_desktop.composeapp.generated.resources.screenshot_desc
import adbtool_desktop.composeapp.generated.resources.shell_command
import adbtool_desktop.composeapp.generated.resources.shell_command_desc
import adbtool_desktop.composeapp.generated.resources.shell_command_hint
import adbtool_desktop.composeapp.generated.resources.shell_input_title
import adbtool_desktop.composeapp.generated.resources.stop_app_short
import adbtool_desktop.composeapp.generated.resources.stop_app_short_desc
import adbtool_desktop.composeapp.generated.resources.tip_title
import adbtool_desktop.composeapp.generated.resources.view_activity
import adbtool_desktop.composeapp.generated.resources.view_activity_desc
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.scale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ScreenSearchDesktop
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ludoven.adbtool.UiTokens
import com.ludoven.adbtool.entity.AdbFunction
import com.ludoven.adbtool.entity.AdbFunctionType
import com.ludoven.adbtool.entity.MsgContent
import com.ludoven.adbtool.iconColors
import com.ludoven.adbtool.util.AdbTool
import com.ludoven.adbtool.viewmodel.CommonModel
import com.ludoven.adbtool.widget.GlassCard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

private data class CommonActionUi(
    val function: AdbFunction,
    val description: String
)

private data class CommonSectionUi(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val items: List<CommonActionUi>
)

@Composable
@Preview
fun CommonScreen(viewModel: CommonModel) {
    val coroutineScope = rememberCoroutineScope()
    val showDialog by viewModel.showDialog.collectAsState()
    val showTextInputDialog by viewModel.showInputDialog.collectAsState()
    val dialogMsg by viewModel.dialogMessage.collectAsState()

    var packageActionType by remember { mutableStateOf<AdbFunctionType?>(null) }
    var showShellInputDialog by remember { mutableStateOf(false) }

    val quickItems = listOf(
        CommonActionUi(
            function = AdbFunction(stringResource(Res.string.install_app), Icons.Default.InstallMobile, AdbFunctionType.INSTALL_APK),
            description = stringResource(Res.string.install_app_desc)
        ),
        CommonActionUi(
            function = AdbFunction(stringResource(Res.string.shell_command), Icons.Default.Code, AdbFunctionType.OPEN_SHELL),
            description = stringResource(Res.string.shell_command_desc)
        ),
        CommonActionUi(
            function = AdbFunction(stringResource(Res.string.file_manager), Icons.Default.Folder, AdbFunctionType.OPEN_FILE_MANAGER),
            description = stringResource(Res.string.file_manager_desc)
        ),
        CommonActionUi(
            function = AdbFunction(stringResource(Res.string.screenshot), Icons.Default.PhotoCamera, AdbFunctionType.SCREENSHOT),
            description = stringResource(Res.string.screenshot_desc)
        ),
        CommonActionUi(
            function = AdbFunction(stringResource(Res.string.screen_record), Icons.Default.ScreenSearchDesktop, AdbFunctionType.SCREEN_RECORD),
            description = stringResource(Res.string.screen_record_desc)
        )
    )

    val oneTapItems = listOf(
        CommonActionUi(
            function = AdbFunction(stringResource(Res.string.install_and_open), Icons.Default.PlayArrow, AdbFunctionType.INSTALL_AND_LAUNCH),
            description = stringResource(Res.string.install_and_open_desc)
        ),
        CommonActionUi(
            function = AdbFunction(stringResource(Res.string.clear_cache_and_restart), Icons.Default.DeleteSweep, AdbFunctionType.CLEAR_CACHE_AND_RESTART),
            description = stringResource(Res.string.clear_cache_and_restart_desc)
        ),
        CommonActionUi(
            function = AdbFunction(stringResource(Res.string.capture_logs), Icons.Default.Memory, AdbFunctionType.CAPTURE_LOGS),
            description = stringResource(Res.string.capture_logs_desc)
        )
    )

    val controlItems = listOf(
        CommonActionUi(
            function = AdbFunction(stringResource(Res.string.reboot_device), Icons.Default.RestartAlt, AdbFunctionType.REBOOT_DEVICE),
            description = stringResource(Res.string.reboot_device_desc)
        ),
        CommonActionUi(
            function = AdbFunction(stringResource(Res.string.input_text), Icons.Default.Edit, AdbFunctionType.INPUT_TEXT),
            description = stringResource(Res.string.input_text_desc)
        ),
        CommonActionUi(
            function = AdbFunction(stringResource(Res.string.back_action), Icons.Default.Close, AdbFunctionType.KEY_BACK),
            description = stringResource(Res.string.back_action_desc)
        ),
        CommonActionUi(
            function = AdbFunction(stringResource(Res.string.home_action), Icons.Default.Home, AdbFunctionType.KEY_HOME),
            description = stringResource(Res.string.home_action_desc)
        )
    )

    val debugItems = listOf(
        CommonActionUi(
            function = AdbFunction(stringResource(Res.string.view_activity), Icons.Default.Visibility, AdbFunctionType.VIEW_CURRENT_ACTIVITY),
            description = stringResource(Res.string.view_activity_desc)
        ),
        CommonActionUi(
            function = AdbFunction(stringResource(Res.string.launch_app_short), Icons.Default.PlayArrow, AdbFunctionType.LAUNCH_APP_BY_PACKAGE),
            description = stringResource(Res.string.launch_app_short_desc)
        ),
        CommonActionUi(
            function = AdbFunction(stringResource(Res.string.stop_app_short), Icons.Default.Stop, AdbFunctionType.STOP_APP_BY_PACKAGE),
            description = stringResource(Res.string.stop_app_short_desc)
        )
    )

    val sections = listOf(
        CommonSectionUi(
            title = stringResource(Res.string.quick_actions_section),
            subtitle = stringResource(Res.string.quick_actions_section_subtitle),
            icon = Icons.Default.InstallMobile,
            items = quickItems
        ),
        CommonSectionUi(
            title = stringResource(Res.string.one_tap_actions_section),
            subtitle = stringResource(Res.string.one_tap_actions_section_subtitle),
            icon = Icons.Default.PlayArrow,
            items = oneTapItems
        ),
        CommonSectionUi(
            title = stringResource(Res.string.device_control_section),
            subtitle = stringResource(Res.string.device_control_section_subtitle),
            icon = Icons.Default.Home,
            items = controlItems
        ),
        CommonSectionUi(
            title = stringResource(Res.string.debug_tools_section),
            subtitle = stringResource(Res.string.debug_tools_section_subtitle),
            icon = Icons.Default.Visibility,
            items = debugItems
        )
    )

    Scaffold(containerColor = androidx.compose.ui.graphics.Color.Transparent) {
        val scrollState = rememberLazyListState()

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = scrollState,
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    CommonHeaderSection()
                }

                items(sections.size) { sectionIndex ->
                    val section = sections[sectionIndex]
                    CommonSectionCard(
                        section = section,
                        onActionClick = { type ->
                            when (type) {
                                AdbFunctionType.OPEN_SHELL -> showShellInputDialog = true
                                AdbFunctionType.INSTALL_AND_LAUNCH,
                                AdbFunctionType.CLEAR_CACHE_AND_RESTART,
                                AdbFunctionType.LAUNCH_APP_BY_PACKAGE,
                                AdbFunctionType.STOP_APP_BY_PACKAGE -> packageActionType = type
                                else -> viewModel.executeAdbAction(type)
                            }
                        }
                    )
                }
            }

            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(scrollState),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
            )
        }
    }

    if (showDialog) {
        dialogMsg?.let {
            TipDialog(
                dialogText = when (it) {
                    is MsgContent.Resource -> stringResource(it.stringResource, *it.args.toTypedArray())
                    is MsgContent.Text -> it.text
                }
            ) {
                viewModel.dismissTipDialog()
            }
        }
    }

    if (showTextInputDialog) {
        TextInputDialog(coroutineScope) {
            viewModel.showInputDialog(false)
        }
    }

    if (showShellInputDialog) {
        ShellCommandDialog(
            onConfirm = { command -> viewModel.executeShellCommand(command) },
            onDismiss = { showShellInputDialog = false }
        )
    }

    val actionType = packageActionType
    if (actionType != null) {
        PackageInputDialog(
            onConfirm = { packageName -> viewModel.executePackageAction(actionType, packageName) },
            onDismiss = { packageActionType = null }
        )
    }
}

@Composable
private fun CommonHeaderSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(Res.string.common_functions),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(Res.string.common_functions_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier
                .padding(start = 16.dp)
                .widthIn(min = 260.dp, max = 320.dp)
                .height(44.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), 
                    RoundedCornerShape(12.dp)
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(Res.string.common_search_placeholder),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "⌘ K",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun CommonSectionCard(
    section: CommonSectionUi,
    onActionClick: (AdbFunctionType) -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(12.dp)
                        )
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = section.icon,
                        contentDescription = section.title,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = section.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val itemRows = section.items.chunked(3)
                itemRows.forEachIndexed { rowIndex, rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowItems.forEachIndexed { columnIndex, item ->
                            val colorIndex = (rowIndex * 3 + columnIndex) % iconColors.size
                            CommonActionCard(
                                title = item.function.title,
                                description = item.description,
                                icon = item.function.icon,
                                iconColor = iconColors[colorIndex],
                                modifier = Modifier.weight(1f),
                                onClick = { onActionClick(item.function.type) }
                            )
                        }
                        repeat(3 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommonActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    val backgroundColor by animateColorAsState(
        if (isHovered) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
        }
    )
    val scale by animateFloatAsState(if (isHovered) 1.02f else 1f)

    Card(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(
            width = 1.dp,
            color = if (isHovered) {
                iconColor.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun TextInputDialog(coroutineScope: CoroutineScope, onDismiss: () -> Unit) {
    var inputText by remember { mutableStateOf("") }
    var escapeSpaces by remember { mutableStateOf(false) }

    val inputHint = stringResource(Res.string.input_hint)
    val escapeText = stringResource(Res.string.escape_spaces)
    val cancelText = stringResource(Res.string.cancel)
    val confirmText = stringResource(Res.string.confirm)

    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Column {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text(inputHint) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    shape = RoundedCornerShape(UiTokens.RadiusMedium)
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = escapeSpaces,
                        onCheckedChange = { escapeSpaces = it }
                    )
                    Text(
                        text = escapeText,
                        modifier = Modifier.clickable { escapeSpaces = !escapeSpaces }
                    )
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(cancelText)
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        onDismiss()
                        coroutineScope.launch {
                            AdbTool.inputText(text = inputText)
                        }
                    },
                    shape = RoundedCornerShape(UiTokens.RadiusSmall)
                ) {
                    Text(confirmText)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
private fun PackageInputDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var packageName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.package_input_title)) },
        text = {
            OutlinedTextField(
                value = packageName,
                onValueChange = { packageName = it },
                label = { Text(stringResource(Res.string.package_name_hint)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(UiTokens.RadiusMedium)
            )
        },
        confirmButton = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.cancel))
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        onDismiss()
                        onConfirm(packageName)
                    },
                    shape = RoundedCornerShape(UiTokens.RadiusSmall)
                ) {
                    Text(stringResource(Res.string.execute))
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
private fun ShellCommandDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var command by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.shell_input_title)) },
        text = {
            OutlinedTextField(
                value = command,
                onValueChange = { command = it },
                label = { Text(stringResource(Res.string.shell_command_hint)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(UiTokens.RadiusMedium)
            )
        },
        confirmButton = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.cancel))
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        onDismiss()
                        onConfirm(command)
                    },
                    shape = RoundedCornerShape(UiTokens.RadiusSmall)
                ) {
                    Text(stringResource(Res.string.execute))
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun TipDialog(dialogText: String, onDismiss: () -> Unit) {
    val tipTitle = stringResource(Res.string.tip_title)
    val confirmText = stringResource(Res.string.confirm)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tipTitle) },
        text = { Text(dialogText) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(confirmText)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}
