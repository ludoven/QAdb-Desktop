package com.ludoven.adbtool.pages

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
import adbtool_desktop.composeapp.generated.resources.not_set
import adbtool_desktop.composeapp.generated.resources.open_release_page
import adbtool_desktop.composeapp.generated.resources.preferences_setting
import adbtool_desktop.composeapp.generated.resources.restart_required
import adbtool_desktop.composeapp.generated.resources.select_adb_icon_desc
import adbtool_desktop.composeapp.generated.resources.select_language
import adbtool_desktop.composeapp.generated.resources.select_theme
import adbtool_desktop.composeapp.generated.resources.settings_adb_path_desc
import adbtool_desktop.composeapp.generated.resources.settings_adb_source_desc
import adbtool_desktop.composeapp.generated.resources.settings_adb_version_desc
import adbtool_desktop.composeapp.generated.resources.settings_pref_auto_detect_desc
import adbtool_desktop.composeapp.generated.resources.settings_pref_auto_detect_title
import adbtool_desktop.composeapp.generated.resources.settings_pref_language_desc
import adbtool_desktop.composeapp.generated.resources.settings_pref_remember_device_desc
import adbtool_desktop.composeapp.generated.resources.settings_pref_remember_device_title
import adbtool_desktop.composeapp.generated.resources.settings_pref_theme_desc
import adbtool_desktop.composeapp.generated.resources.settings_status_label
import adbtool_desktop.composeapp.generated.resources.settings_subtitle
import adbtool_desktop.composeapp.generated.resources.settings_update_current_version_desc
import adbtool_desktop.composeapp.generated.resources.settings_update_status
import adbtool_desktop.composeapp.generated.resources.settings_update_status_desc
import adbtool_desktop.composeapp.generated.resources.settings_update_status_latest
import adbtool_desktop.composeapp.generated.resources.set
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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdateAlt
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
import com.ludoven.adbtool.AppVersion
import com.ludoven.adbtool.UiTokens
import com.ludoven.adbtool.ui.mac.Button
import com.ludoven.adbtool.ui.mac.ButtonDefaults
import com.ludoven.adbtool.ui.mac.CircularProgressIndicator
import com.ludoven.adbtool.ui.mac.DropdownMenu
import com.ludoven.adbtool.ui.mac.DropdownMenuItem
import com.ludoven.adbtool.ui.mac.HorizontalDivider
import com.ludoven.adbtool.ui.mac.Icon
import com.ludoven.adbtool.ui.mac.MaterialTheme
import com.ludoven.adbtool.ui.mac.OutlinedButton
import com.ludoven.adbtool.ui.mac.Surface
import com.ludoven.adbtool.ui.mac.Switch
import com.ludoven.adbtool.ui.mac.Text
import com.ludoven.adbtool.ui.mac.TextButton
import com.ludoven.adbtool.ui.mac.bodyLarge
import com.ludoven.adbtool.ui.mac.bodyMedium
import com.ludoven.adbtool.ui.mac.bodySmall
import com.ludoven.adbtool.ui.mac.headlineSmall
import com.ludoven.adbtool.ui.mac.titleMedium
import com.ludoven.adbtool.util.AdbPathManager
import com.ludoven.adbtool.util.FileUtils
import com.ludoven.adbtool.util.GitHubUpdateManager
import com.ludoven.adbtool.util.LanguageManager
import com.ludoven.adbtool.util.ThemeManager
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import java.awt.Desktop
import java.io.File
import java.util.prefs.Preferences

@Composable
fun SettingScreen() {
    val notSetText = stringResource(Res.string.not_set)
    val adbEnvironment by AdbPathManager.adbEnvironment.collectAsState()
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

    val userPrefs = remember { Preferences.userNodeForPackage(AdbPathManager::class.java) }
    var autoDetectDeviceOnLaunch by remember {
        mutableStateOf(userPrefs.getBoolean("setting.auto_detect_device_on_launch", true))
    }
    var rememberLastDevice by remember {
        mutableStateOf(userPrefs.getBoolean("setting.remember_last_device", true))
    }

    val updateStatusText = when (val status = updateStatus) {
        UpdateStatus.Idle -> stringResource(Res.string.settings_update_status_latest)
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(Res.string.set),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(Res.string.settings_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SettingsSection(
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

            SettingStatusRow(
                label = stringResource(Res.string.settings_status_label),
                statusText = statusText,
                tone = when {
                    adbEnvironment.isReady -> StatusTone.Positive
                    isCheckingAdb -> StatusTone.Neutral
                    else -> StatusTone.Danger
                }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.58f))
            SettingValueRow(
                title = stringResource(Res.string.adb_current_using),
                description = stringResource(Res.string.settings_adb_source_desc),
                value = sourceText
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.58f))
            SettingValueRow(
                title = stringResource(Res.string.adb_path_label),
                description = stringResource(Res.string.settings_adb_path_desc),
                value = adbEnvironment.path ?: notSetText,
                trailingIcon = Icons.Default.Folder,
                trailingIconDescription = stringResource(Res.string.adb_path_label),
                onTrailingAction = {
                    openPathLocation(adbEnvironment.path)
                }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.58f))
            SettingValueRow(
                title = stringResource(Res.string.adb_version_label),
                description = stringResource(Res.string.settings_adb_version_desc),
                value = adbEnvironment.version ?: notSetText
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            AdbPathManager.autoDetect()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(stringResource(Res.string.adb_auto_detect), color = Color.White)
                }

                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            val newPath = FileUtils.selectFile()
                            if (newPath != null) {
                                AdbPathManager.setAdbPath(newPath)
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = stringResource(Res.string.select_adb_icon_desc))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(stringResource(Res.string.adb_select_adb))
                }

                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            AdbPathManager.useBundledAdb()
                        }
                    }
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(stringResource(Res.string.adb_restore_bundled))
                }

                OutlinedButton(
                    onClick = {
                        AdbPathManager.openHelp().onFailure { _ -> }
                    }
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(stringResource(Res.string.adb_open_help))
                }
            }
        }

        SettingsSection(
            title = stringResource(Res.string.preferences_setting),
            icon = Icons.Default.Settings
        ) {
            val currentThemeText = when (currentThemeMode) {
                ThemeManager.ThemeMode.SYSTEM -> stringResource(Res.string.theme_mode_system)
                ThemeManager.ThemeMode.LIGHT -> stringResource(Res.string.theme_mode_light)
                ThemeManager.ThemeMode.DARK -> stringResource(Res.string.theme_mode_dark)
            }

            SettingsDropdownRow(
                title = stringResource(Res.string.select_language),
                description = stringResource(Res.string.settings_pref_language_desc),
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

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.58f))

            SettingsDropdownRow(
                title = stringResource(Res.string.select_theme),
                description = stringResource(Res.string.settings_pref_theme_desc),
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

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.58f))

            SettingSwitchRow(
                title = stringResource(Res.string.settings_pref_auto_detect_title),
                description = stringResource(Res.string.settings_pref_auto_detect_desc),
                checked = autoDetectDeviceOnLaunch,
                onCheckedChange = {
                    autoDetectDeviceOnLaunch = it
                    userPrefs.putBoolean("setting.auto_detect_device_on_launch", it)
                }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.58f))

            SettingSwitchRow(
                title = stringResource(Res.string.settings_pref_remember_device_title),
                description = stringResource(Res.string.settings_pref_remember_device_desc),
                checked = rememberLastDevice,
                onCheckedChange = {
                    rememberLastDevice = it
                    userPrefs.putBoolean("setting.remember_last_device", it)
                }
            )
        }

        SettingsSection(
            title = stringResource(Res.string.update_section_title),
            icon = Icons.Default.SystemUpdateAlt
        ) {
            SettingValueRow(
                title = stringResource(Res.string.current_version, "").trim().trimEnd(':', '：'),
                description = stringResource(Res.string.settings_update_current_version_desc),
                value = currentVersionLabel
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.58f))
            SettingValueRow(
                title = stringResource(Res.string.settings_update_status),
                description = stringResource(Res.string.settings_update_status_desc),
                value = updateStatusText,
                valueColor = updateStatusColor(updateStatus)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
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
                                    updateStatus = UpdateStatus.UpdateAvailable(result.latestVersion, hasAutoInstall = true)
                                }
                                is GitHubUpdateManager.CheckResult.UpdateAvailableNoAsset -> {
                                    latestVersion = result.latestVersion
                                    releaseUrl = result.htmlUrl
                                    downloadableAsset = null
                                    updateStatus = UpdateStatus.UpdateAvailable(result.latestVersion, hasAutoInstall = false)
                                }
                                is GitHubUpdateManager.CheckResult.Error -> {
                                    updateStatus = UpdateStatus.CheckFailed(result.message)
                                }
                            }
                            isCheckingUpdate = false
                        }
                    },
                    enabled = !isCheckingUpdate && !isDownloadingUpdate,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                ) {
                    if (isCheckingUpdate) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                    }
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(stringResource(Res.string.check_update), color = Color.White)
                }

                if (downloadableAsset != null) {
                    OutlinedButton(
                        onClick = {
                            val asset = downloadableAsset ?: return@OutlinedButton
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
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(stringResource(Res.string.download_and_install))
                    }
                }

                if (!releaseUrl.isNullOrBlank() && latestVersion != null) {
                    OutlinedButton(
                        onClick = {
                            val url = releaseUrl ?: return@OutlinedButton
                            GitHubUpdateManager.openReleasePage(url).onFailure { error ->
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

    if (showLanguageDialog) {
        TipDialog(stringResource(Res.string.language_changed) + "\n" + stringResource(Res.string.restart_required)) {
            showLanguageDialog = false
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            content()
        }
    }
}

@Composable
private fun SettingStatusRow(label: String, statusText: String, tone: StatusTone) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        val color = when (tone) {
            StatusTone.Positive -> Color(0xFF2563EB)
            StatusTone.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
            StatusTone.Danger -> Color(0xFFDC2626)
        }
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = color.copy(alpha = 0.1f),
            border = BorderStroke(1.dp, color.copy(alpha = 0.22f))
        ) {
            Text(
                text = statusText,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = color,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SettingValueRow(
    title: String,
    description: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    trailingIcon: ImageVector? = null,
    trailingIconDescription: String? = null,
    onTrailingAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(0.62f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(
            modifier = Modifier.weight(0.38f),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = valueColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (trailingIcon != null && onTrailingAction != null) {
                OutlinedButton(
                    onClick = onTrailingAction,
                    modifier = Modifier.height(30.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = trailingIcon,
                        contentDescription = trailingIconDescription,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsDropdownRow(
    title: String,
    description: String,
    value: String,
    icon: ImageVector,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    menuContent: @Composable ColumnScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box {
            Surface(
                onClick = { onExpandedChange(!expanded) },
                shape = RoundedCornerShape(9.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
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
private fun SettingSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun updateStatusColor(status: UpdateStatus): Color {
    return when (status) {
        UpdateStatus.Idle -> MaterialTheme.colorScheme.onSurface
        UpdateStatus.Checking,
        UpdateStatus.Downloading -> MaterialTheme.colorScheme.primary
        is UpdateStatus.UpToDate,
        is UpdateStatus.DownloadSuccess -> Color(0xFF15803D)
        is UpdateStatus.UpdateAvailable -> Color(0xFF2563EB)
        is UpdateStatus.CheckFailed,
        is UpdateStatus.DownloadFailed -> Color(0xFFDC2626)
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

private fun openPathLocation(path: String?) {
    if (path.isNullOrBlank()) return
    runCatching {
        if (!Desktop.isDesktopSupported()) return
        val desktop = Desktop.getDesktop()
        if (!desktop.isSupported(Desktop.Action.OPEN)) return
        val file = File(path)
        val target = if (file.isDirectory) file else file.parentFile
        if (target != null && target.exists()) {
            desktop.open(target)
        }
    }
}

private enum class StatusTone {
    Positive,
    Neutral,
    Danger
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
