package com.ludoven.adbtool.pages

import com.ludoven.adbtool.ui.mac.*

import adbtool_desktop.composeapp.generated.resources.Res
import adbtool_desktop.composeapp.generated.resources.adb_auto_detect
import adbtool_desktop.composeapp.generated.resources.adb_current_using
import adbtool_desktop.composeapp.generated.resources.adb_environment_checking
import adbtool_desktop.composeapp.generated.resources.adb_environment_failed
import adbtool_desktop.composeapp.generated.resources.adb_environment_ready
import adbtool_desktop.composeapp.generated.resources.adb_open_help
import adbtool_desktop.composeapp.generated.resources.adb_path_label
import adbtool_desktop.composeapp.generated.resources.adb_path_setting
import adbtool_desktop.composeapp.generated.resources.adb_restore_bundled
import adbtool_desktop.composeapp.generated.resources.adb_select_adb
import adbtool_desktop.composeapp.generated.resources.adb_source_bundled
import adbtool_desktop.composeapp.generated.resources.adb_source_custom
import adbtool_desktop.composeapp.generated.resources.adb_source_none
import adbtool_desktop.composeapp.generated.resources.adb_source_system
import adbtool_desktop.composeapp.generated.resources.adb_version_label
import adbtool_desktop.composeapp.generated.resources.check_update
import adbtool_desktop.composeapp.generated.resources.current_version
import adbtool_desktop.composeapp.generated.resources.download_and_install
import adbtool_desktop.composeapp.generated.resources.downloading_update
import adbtool_desktop.composeapp.generated.resources.language_changed
import adbtool_desktop.composeapp.generated.resources.latest_version
import adbtool_desktop.composeapp.generated.resources.not_set
import adbtool_desktop.composeapp.generated.resources.open_release_page
import adbtool_desktop.composeapp.generated.resources.preferences_setting
import adbtool_desktop.composeapp.generated.resources.restart_required
import adbtool_desktop.composeapp.generated.resources.select_adb_icon_desc
import adbtool_desktop.composeapp.generated.resources.select_language
import adbtool_desktop.composeapp.generated.resources.select_theme
import adbtool_desktop.composeapp.generated.resources.theme_mode_dark
import adbtool_desktop.composeapp.generated.resources.theme_mode_light
import adbtool_desktop.composeapp.generated.resources.theme_mode_system
import adbtool_desktop.composeapp.generated.resources.update_available_no_asset
import adbtool_desktop.composeapp.generated.resources.update_available_with_version
import adbtool_desktop.composeapp.generated.resources.update_check_failed
import adbtool_desktop.composeapp.generated.resources.update_checking
import adbtool_desktop.composeapp.generated.resources.update_download_failed
import adbtool_desktop.composeapp.generated.resources.update_download_success
import adbtool_desktop.composeapp.generated.resources.update_section_title
import adbtool_desktop.composeapp.generated.resources.update_up_to_date
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdateAlt
import com.ludoven.adbtool.ui.mac.DropdownMenu
import com.ludoven.adbtool.ui.mac.DropdownMenuItem
import com.ludoven.adbtool.ui.mac.ExperimentalMaterial3Api
import com.ludoven.adbtool.ui.mac.Button
import com.ludoven.adbtool.ui.mac.CircularProgressIndicator
import com.ludoven.adbtool.ui.mac.Icon
import com.ludoven.adbtool.ui.mac.MaterialTheme
import com.ludoven.adbtool.ui.mac.Text
import com.ludoven.adbtool.ui.mac.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ludoven.adbtool.AppVersion
import com.ludoven.adbtool.UiTokens
import com.ludoven.adbtool.util.AdbPathManager
import com.ludoven.adbtool.util.FileUtils
import com.ludoven.adbtool.util.GitHubUpdateManager
import com.ludoven.adbtool.util.LanguageManager
import com.ludoven.adbtool.util.ThemeManager
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen() {
    val notSetText = stringResource(Res.string.not_set)
    val adbReadyText = stringResource(Res.string.adb_environment_ready)
    val adbEnvironment by AdbPathManager.adbEnvironment.collectAsState()
    var adbDialogMessage by remember { mutableStateOf<String?>(null) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showLanguageDropdown by remember { mutableStateOf(false) }
    var showThemeDropdown by remember { mutableStateOf(false) }
    var updateStatus by remember { mutableStateOf<UpdateStatus>(UpdateStatus.Idle) }
    var latestVersion by remember { mutableStateOf<String?>(null) }
    var releaseUrl by remember { mutableStateOf<String?>(null) }
    var downloadableAsset by remember { mutableStateOf<GitHubUpdateManager.GitHubAssetResponse?>(null) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var isDownloadingUpdate by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val currentLanguage by LanguageManager.currentLanguage.collectAsState()
    val currentThemeMode by ThemeManager.currentThemeMode.collectAsState()
    val supportedLanguages = LanguageManager.getSupportedLanguages()
    val supportedThemeModes = ThemeManager.ThemeMode.entries
    val currentVersionLabel = AppVersion.CURRENT
    val updateStatusText = when (val status = updateStatus) {
        UpdateStatus.Idle -> null
        UpdateStatus.Checking -> stringResource(Res.string.update_checking)
        UpdateStatus.Downloading -> stringResource(Res.string.downloading_update)
        is UpdateStatus.UpToDate -> stringResource(Res.string.update_up_to_date, status.latestVersion)
        is UpdateStatus.UpdateAvailable -> if (status.hasAutoInstall) {
            stringResource(Res.string.update_available_with_version, status.latestVersion)
        } else {
            stringResource(Res.string.update_available_no_asset, status.latestVersion)
        }
        is UpdateStatus.DownloadSuccess -> stringResource(Res.string.update_download_success, status.path)
        is UpdateStatus.CheckFailed -> stringResource(Res.string.update_check_failed, status.reason)
        is UpdateStatus.DownloadFailed -> stringResource(Res.string.update_download_failed, status.reason)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(UiTokens.PagePadding),
        verticalArrangement = Arrangement.spacedBy(UiTokens.SectionSpacing)
    ) {
        SettingsSection(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(Res.string.adb_path_setting),
            icon = Icons.Default.FolderOpen
        ) {
            val sourceText = settingAdbSourceText(adbEnvironment.source)
            val isCheckingAdb = !adbEnvironment.isReady &&
                adbEnvironment.source == AdbPathManager.AdbSource.NONE &&
                adbEnvironment.message?.contains("检测") == true
            val statusText = if (adbEnvironment.isReady) {
                stringResource(Res.string.adb_environment_ready)
            } else if (isCheckingAdb) {
                stringResource(Res.string.adb_environment_checking)
            } else {
                adbEnvironment.message ?: stringResource(Res.string.adb_environment_failed)
            }

            StatusPill(
                text = statusText,
                color = if (adbEnvironment.isReady) {
                    MaterialTheme.colorScheme.primary
                } else if (isCheckingAdb) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.error
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            InfoGroup {
                AdbInfoRow(
                    label = stringResource(Res.string.adb_current_using),
                    value = sourceText
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                AdbInfoRow(
                    label = stringResource(Res.string.adb_path_label),
                    value = adbEnvironment.path ?: notSetText
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                AdbInfoRow(
                    label = stringResource(Res.string.adb_version_label),
                    value = adbEnvironment.version ?: notSetText
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        coroutineScope.launch {
                            val environment = AdbPathManager.autoDetect()
                            adbDialogMessage = environment.message
                                ?: "$adbReadyText\n${environment.path.orEmpty()}"
                        }
                    }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(stringResource(Res.string.adb_auto_detect))
                }
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        coroutineScope.launch {
                            val newPath = FileUtils.selectFile()
                            if (newPath != null) {
                                val success = AdbPathManager.setAdbPath(newPath)
                                adbDialogMessage = if (success) {
                                    "$adbReadyText\n$newPath"
                                } else {
                                    AdbPathManager.adbEnvironment.value.message
                                }
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = stringResource(Res.string.select_adb_icon_desc))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(stringResource(Res.string.adb_select_adb))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        coroutineScope.launch {
                            val environment = AdbPathManager.useBundledAdb()
                            adbDialogMessage = environment.message
                                ?: "$adbReadyText\n${environment.path.orEmpty()}"
                        }
                    }
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(stringResource(Res.string.adb_restore_bundled))
                }
                TextButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        AdbPathManager.openHelp().onFailure { error ->
                            adbDialogMessage = error.message ?: "无法打开说明"
                        }
                    }
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(stringResource(Res.string.adb_open_help))
                }
            }
        }

        SettingsSection(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(Res.string.preferences_setting),
            icon = Icons.Default.Settings
        ) {
            val currentThemeText = when (currentThemeMode) {
                ThemeManager.ThemeMode.SYSTEM -> stringResource(Res.string.theme_mode_system)
                ThemeManager.ThemeMode.LIGHT -> stringResource(Res.string.theme_mode_light)
                ThemeManager.ThemeMode.DARK -> stringResource(Res.string.theme_mode_dark)
            }

            SettingsDropdownRow(
                label = stringResource(Res.string.select_language),
                value = currentLanguage.displayName,
                icon = Icons.Default.Language,
                expanded = showLanguageDropdown,
                onExpandedChange = { showLanguageDropdown = it }
            ) {
                supportedLanguages.forEach { language ->
                    DropdownMenuItem(
                        text = { Text(language.displayName) },
                        onClick = {
                            LanguageManager.setLanguage(language)
                            showLanguageDropdown = false
                            showLanguageDialog = true
                        }
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 6.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
            )

            SettingsDropdownRow(
                label = stringResource(Res.string.select_theme),
                value = currentThemeText,
                icon = Icons.Default.Settings,
                expanded = showThemeDropdown,
                onExpandedChange = { showThemeDropdown = it }
            ) {
                supportedThemeModes.forEach { themeMode ->
                    val text = when (themeMode) {
                        ThemeManager.ThemeMode.SYSTEM -> stringResource(Res.string.theme_mode_system)
                        ThemeManager.ThemeMode.LIGHT -> stringResource(Res.string.theme_mode_light)
                        ThemeManager.ThemeMode.DARK -> stringResource(Res.string.theme_mode_dark)
                    }
                    DropdownMenuItem(
                        text = { Text(text) },
                        onClick = {
                            ThemeManager.setThemeMode(themeMode)
                            showThemeDropdown = false
                        }
                    )
                }
            }
        }

        SettingsSection(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(Res.string.update_section_title),
            icon = Icons.Default.SystemUpdateAlt
        ) {
            val currentVersionTitle = stringResource(Res.string.current_version, "").trim().trimEnd(':', '：')
            val latestVersionTitle = stringResource(Res.string.latest_version, "").trim().trimEnd(':', '：')

            InfoGroup {
                VersionActionRow(
                    label = currentVersionTitle,
                    value = currentVersionLabel
                ) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isCheckingUpdate = true
                                updateStatus = UpdateStatus.Checking
                                val result = GitHubUpdateManager.checkForUpdate(currentVersionLabel)
                                when (result) {
                                    is GitHubUpdateManager.CheckResult.UpToDate -> {
                                        latestVersion = result.latestVersion
                                        releaseUrl = null
                                        downloadableAsset = null
                                        updateStatus = UpdateStatus.UpToDate(result.latestVersion)
                                    }
                                    is GitHubUpdateManager.CheckResult.UpdateAvailable -> {
                                        latestVersion = result.latestVersion
                                        releaseUrl = result.htmlUrl
                                        downloadableAsset = result.asset
                                        updateStatus = UpdateStatus.UpdateAvailable(
                                            latestVersion = result.latestVersion,
                                            hasAutoInstall = true
                                        )
                                    }
                                    is GitHubUpdateManager.CheckResult.UpdateAvailableNoAsset -> {
                                        latestVersion = result.latestVersion
                                        releaseUrl = result.htmlUrl
                                        downloadableAsset = null
                                        updateStatus = UpdateStatus.UpdateAvailable(
                                            latestVersion = result.latestVersion,
                                            hasAutoInstall = false
                                        )
                                    }
                                    is GitHubUpdateManager.CheckResult.Error -> {
                                        updateStatus = UpdateStatus.CheckFailed(result.message)
                                    }
                                }
                                isCheckingUpdate = false
                            }
                        },
                        enabled = !isCheckingUpdate && !isDownloadingUpdate
                    ) {
                        if (isCheckingUpdate) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                        }
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(stringResource(Res.string.check_update))
                    }
                }
                latestVersion?.let { latest ->
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                    AdbInfoRow(
                        label = latestVersionTitle,
                        value = latest
                    )
                }
            }

            if (!updateStatusText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                StatusPill(
                    text = updateStatusText,
                    color = updateStatusColor(updateStatus)
                )
            }

            if (downloadableAsset != null || (!releaseUrl.isNullOrBlank() && latestVersion != null)) {
                Spacer(modifier = Modifier.height(12.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (downloadableAsset != null) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val asset = downloadableAsset ?: return@Button
                            coroutineScope.launch {
                                isDownloadingUpdate = true
                                updateStatus = UpdateStatus.Downloading
                                val result = GitHubUpdateManager.downloadAndInstall(asset)
                                result.onSuccess { path ->
                                    updateStatus = UpdateStatus.DownloadSuccess(path.toString())
                                }.onFailure { error ->
                                    updateStatus = UpdateStatus.DownloadFailed(error.message ?: "Unknown error")
                                }
                                isDownloadingUpdate = false
                            }
                        },
                        enabled = !isCheckingUpdate && !isDownloadingUpdate
                    ) {
                        if (isDownloadingUpdate) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Download, contentDescription = null)
                        }
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(stringResource(Res.string.download_and_install))
                    }
                } else if (!releaseUrl.isNullOrBlank() && latestVersion != null) {
                    TextButton(
                        onClick = {
                            val url = releaseUrl ?: return@TextButton
                            val openResult = GitHubUpdateManager.openReleasePage(url)
                            openResult.onFailure { error ->
                                updateStatus = UpdateStatus.CheckFailed(error.message ?: "Unknown error")
                            }
                        }
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(stringResource(Res.string.open_release_page))
                    }
                }
            }
        }
    }

    adbDialogMessage?.let { message ->
        TipDialog(message) {
            adbDialogMessage = null
        }
    }

    if (showLanguageDialog) {
        TipDialog(stringResource(Res.string.language_changed) + "\n" + stringResource(Res.string.restart_required)) {
            showLanguageDialog = false
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(UiTokens.RadiusMedium),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun StatusPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.12f),
        contentColor = color
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun InfoGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(UiTokens.RadiusSmall),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            content = content
        )
    }
}

@Composable
private fun SettingsDropdownRow(
    label: String,
    value: String,
    icon: ImageVector,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    menuContent: @Composable ColumnScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Box {
            Surface(
                onClick = { onExpandedChange(!expanded) },
                modifier = Modifier.menuAnchor(),
                shape = RoundedCornerShape(UiTokens.RadiusSmall),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                content = menuContent
            )
        }
    }
}

@Composable
private fun VersionActionRow(
    label: String,
    value: String,
    action: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        action()
    }
}

@Composable
private fun updateStatusColor(status: UpdateStatus): Color {
    return when (status) {
        UpdateStatus.Idle -> MaterialTheme.colorScheme.onSurfaceVariant
        UpdateStatus.Checking,
        UpdateStatus.Downloading -> MaterialTheme.colorScheme.primary
        is UpdateStatus.UpToDate,
        is UpdateStatus.UpdateAvailable,
        is UpdateStatus.DownloadSuccess -> Color(0xFF34C759)
        is UpdateStatus.CheckFailed,
        is UpdateStatus.DownloadFailed -> MaterialTheme.colorScheme.error
    }
}

@Composable
private fun AdbInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(0.32f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            modifier = Modifier.weight(0.68f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun settingAdbSourceText(source: AdbPathManager.AdbSource): String {
    return when (source) {
        AdbPathManager.AdbSource.SYSTEM -> stringResource(Res.string.adb_source_system)
        AdbPathManager.AdbSource.BUNDLED -> stringResource(Res.string.adb_source_bundled)
        AdbPathManager.AdbSource.CUSTOM -> stringResource(Res.string.adb_source_custom)
        AdbPathManager.AdbSource.NONE -> stringResource(Res.string.adb_source_none)
    }
}

private sealed interface UpdateStatus {
    data object Idle : UpdateStatus
    data object Checking : UpdateStatus
    data object Downloading : UpdateStatus
    data class UpToDate(val latestVersion: String) : UpdateStatus
    data class UpdateAvailable(
        val latestVersion: String,
        val hasAutoInstall: Boolean
    ) : UpdateStatus
    data class DownloadSuccess(val path: String) : UpdateStatus
    data class CheckFailed(val reason: String) : UpdateStatus
    data class DownloadFailed(val reason: String) : UpdateStatus
}
