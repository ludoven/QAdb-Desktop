package com.ludoven.adbtool.pages

import com.ludoven.adbtool.UiTokens
import com.ludoven.adbtool.QadbColors
import com.ludoven.adbtool.ui.icons.IconParkIcons

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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.unit.sp
import com.ludoven.adbtool.AppVersion
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
import com.ludoven.adbtool.widget.PageHeader
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import java.awt.Desktop
import java.io.File
import java.util.prefs.Preferences

private object SettingColors {
    val PageBackground: Color @Composable get() = QadbColors.background
    val Surface: Color @Composable get() = QadbColors.surface
    val SoftSurface: Color @Composable get() = QadbColors.surfaceVariant
    val Text: Color @Composable get() = QadbColors.textPrimary
    val SecondaryText: Color @Composable get() = QadbColors.textSecondary
    val Muted: Color @Composable get() = QadbColors.textTertiary
    val Border: Color @Composable get() = QadbColors.border
    val Divider: Color @Composable get() = QadbColors.divider
    val Primary: Color @Composable get() = QadbColors.primary
    val PrimarySoft: Color @Composable get() = QadbColors.primaryContainer
    val PrimaryBorder: Color @Composable get() = QadbColors.primary.copy(alpha = 0.34f)
    val ButtonBorder: Color @Composable get() = QadbColors.border
    val NeutralButton: Color @Composable get() = QadbColors.surfaceVariant
    val ControlBackground: Color @Composable get() = QadbColors.disabledSurface
    val Danger: Color @Composable get() = QadbColors.danger
    val Success: Color @Composable get() = QadbColors.success
}

@Composable
private fun SettingActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    val contentColor = when {
        !enabled -> SettingColors.Muted
        primary -> QadbColors.onPrimary
        else -> SettingColors.Text
    }
    val iconColor = when {
        !enabled -> SettingColors.Muted
        primary -> QadbColors.onPrimary
        else -> SettingColors.Muted
    }

    val content: @Composable () -> Unit = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceSmall)
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = if (primary && enabled) QadbColors.onPrimary else SettingColors.Primary
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(UiTokens.IconSmall)
                )
            }
            Text(
                text = text,
                color = contentColor,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                fontSize = UiTokens.TextBody,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    if (primary) {
        Button(
            onClick = onClick,
            modifier = modifier.height(36.dp),
            enabled = enabled,
            shape = RoundedCornerShape(UiTokens.RadiusMedium),
            colors = ButtonDefaults.buttonColors(
                containerColor = SettingColors.Primary,
                contentColor = QadbColors.onPrimary,
                disabledContainerColor = SettingColors.ControlBackground,
                disabledContentColor = SettingColors.Muted
            ),
            contentPadding = PaddingValues(horizontal = UiTokens.SpaceMedium, vertical = 0.dp)
        ) {
            content()
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(36.dp),
            enabled = enabled,
            shape = RoundedCornerShape(UiTokens.RadiusMedium),
            border = BorderStroke(1.dp, SettingColors.ButtonBorder.copy(alpha = 0.72f)),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = SettingColors.NeutralButton,
                contentColor = SettingColors.Text,
                disabledContentColor = SettingColors.Muted
            ),
            contentPadding = PaddingValues(horizontal = UiTokens.SpaceMedium, vertical = 0.dp)
        ) {
            content()
        }
    }
}

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SettingColors.PageBackground)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 880.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = UiTokens.PagePadding, vertical = UiTokens.PagePaddingCompact),
            verticalArrangement = Arrangement.spacedBy(UiTokens.SectionSpacing)
        ) {
            PageHeader(
                title = stringResource(Res.string.set),
                subtitle = stringResource(Res.string.settings_subtitle)
            )

            SettingsSection(title = stringResource(Res.string.adb_path_setting)) {
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

                AdbEnvironmentSummary(
                    statusLabel = stringResource(Res.string.settings_status_label),
                    statusText = statusText,
                    tone = when {
                        adbEnvironment.isReady -> StatusTone.Positive
                        isCheckingAdb -> StatusTone.Neutral
                        else -> StatusTone.Danger
                    },
                    sourceLabel = stringResource(Res.string.adb_current_using),
                    sourceDescription = stringResource(Res.string.settings_adb_source_desc),
                    sourceValue = sourceText,
                    pathLabel = stringResource(Res.string.adb_path_label),
                    pathDescription = stringResource(Res.string.settings_adb_path_desc),
                    pathValue = adbEnvironment.path ?: notSetText,
                    versionLabel = stringResource(Res.string.adb_version_label),
                    versionDescription = stringResource(Res.string.settings_adb_version_desc),
                    versionValue = adbEnvironment.version ?: notSetText,
                    trailingIcon = IconParkIcons.Folder,
                    trailingIconDescription = stringResource(Res.string.adb_path_label),
                    onTrailingAction = {
                        openPathLocation(adbEnvironment.path)
                    }
                )

                ActionButtonRow {
                    SettingActionButton(
                        text = stringResource(Res.string.adb_auto_detect),
                        icon = IconParkIcons.Refresh,
                        onClick = {
                            coroutineScope.launch {
                                AdbPathManager.autoDetect()
                            }
                        },
                        primary = true
                    )

                    SettingActionButton(
                        text = stringResource(Res.string.adb_select_adb),
                        icon = IconParkIcons.Folder,
                        onClick = {
                            coroutineScope.launch {
                                val newPath = FileUtils.selectFile()
                                if (newPath != null) {
                                    AdbPathManager.setAdbPath(newPath)
                                }
                            }
                        }
                    )

                    SettingActionButton(
                        text = stringResource(Res.string.adb_restore_bundled),
                        icon = IconParkIcons.Setting,
                        onClick = {
                            coroutineScope.launch {
                                AdbPathManager.useBundledAdb()
                            }
                        }
                    )

                    SettingActionButton(
                        text = stringResource(Res.string.adb_open_help),
                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                        onClick = {
                            AdbPathManager.openHelp().onFailure { _ -> }
                        }
                    )
                }
            }

            SettingsSection(title = stringResource(Res.string.preferences_setting)) {
                val currentThemeText = when (currentThemeMode) {
                    ThemeManager.ThemeMode.SYSTEM -> stringResource(Res.string.theme_mode_system)
                    ThemeManager.ThemeMode.LIGHT -> stringResource(Res.string.theme_mode_light)
                    ThemeManager.ThemeMode.DARK -> stringResource(Res.string.theme_mode_dark)
                }

                SettingsDropdownRow(
                    title = stringResource(Res.string.select_language),
                    description = stringResource(Res.string.settings_pref_language_desc),
                    value = currentLanguage.displayName,
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

                SectionDivider()

                SettingsDropdownRow(
                    title = stringResource(Res.string.select_theme),
                    description = stringResource(Res.string.settings_pref_theme_desc),
                    value = currentThemeText,
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

                SectionDivider()

                SettingSwitchRow(
                    title = stringResource(Res.string.settings_pref_auto_detect_title),
                    description = stringResource(Res.string.settings_pref_auto_detect_desc),
                    checked = autoDetectDeviceOnLaunch,
                    onCheckedChange = {
                        autoDetectDeviceOnLaunch = it
                        userPrefs.putBoolean("setting.auto_detect_device_on_launch", it)
                    }
                )

                SectionDivider()

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

            SettingsSection(title = stringResource(Res.string.update_section_title)) {
                SettingValueRow(
                    title = stringResource(Res.string.current_version, "").trim().trimEnd(':', '：'),
                    description = stringResource(Res.string.settings_update_current_version_desc),
                    value = currentVersionLabel
                )
                SectionDivider()
                SettingValueRow(
                    title = stringResource(Res.string.settings_update_status),
                    description = stringResource(Res.string.settings_update_status_desc),
                    value = updateStatusText,
                    valueColor = updateStatusColor(updateStatus)
                )

                ActionButtonRow {
                    SettingActionButton(
                        text = stringResource(Res.string.check_update),
                        icon = IconParkIcons.Refresh,
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
                        loading = isCheckingUpdate,
                        primary = true
                    )

                    if (downloadableAsset != null) {
                        SettingActionButton(
                            text = stringResource(Res.string.download_and_install),
                            icon = IconParkIcons.Download,
                            onClick = {
                                val asset = downloadableAsset
                                if (asset != null) {
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
                                }
                            },
                            enabled = !isCheckingUpdate && !isDownloadingUpdate,
                            loading = isDownloadingUpdate
                        )
                    }

                    if (!releaseUrl.isNullOrBlank() && latestVersion != null) {
                        SettingActionButton(
                            text = stringResource(Res.string.open_release_page),
                            icon = Icons.AutoMirrored.Filled.OpenInNew,
                            onClick = {
                                val url = releaseUrl
                                if (!url.isNullOrBlank()) {
                                    GitHubUpdateManager.openReleasePage(url).onFailure { error ->
                                        updateStatus = UpdateStatus.CheckFailed(error.message ?: "Unknown error")
                                    }
                                }
                            }
                        )
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
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceSmall)
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = UiTokens.SpaceLarge),
            style = MaterialTheme.typography.titleMedium,
            color = SettingColors.Text,
            fontWeight = FontWeight.Medium,
            fontSize = UiTokens.TextSection
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = UiTokens.SpaceLarge),
        color = SettingColors.Divider
    )
}

@Composable
private fun ActionButtonRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = UiTokens.SpaceLarge, vertical = UiTokens.SpaceMedium),
        horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        content()
    }
}

@Composable
private fun AdbEnvironmentSummary(
    statusLabel: String,
    statusText: String,
    tone: StatusTone,
    sourceLabel: String,
    sourceDescription: String,
    sourceValue: String,
    pathLabel: String,
    pathDescription: String,
    pathValue: String,
    versionLabel: String,
    versionDescription: String,
    versionValue: String,
    trailingIcon: ImageVector,
    trailingIconDescription: String,
    onTrailingAction: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(UiTokens.RadiusLarge),
        color = SettingColors.SoftSurface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = UiTokens.SpaceLarge, vertical = UiTokens.SpaceMedium),
            verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceMedium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceMedium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceXSmall)) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SettingColors.Text,
                        fontWeight = FontWeight.Medium,
                        fontSize = UiTokens.TextBodyLarge
                    )
                }
                StatusPill(statusText = statusText, tone = tone)
            }

            Column(verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceSmall)) {
                SummaryInfoRow(
                    title = sourceLabel,
                    description = sourceDescription,
                    value = sourceValue
                )
                SummaryInfoRow(
                    title = pathLabel,
                    description = pathDescription,
                    value = pathValue,
                    trailingIcon = trailingIcon,
                    trailingIconDescription = trailingIconDescription,
                    onTrailingAction = onTrailingAction
                )
                SummaryInfoRow(
                    title = versionLabel,
                    description = versionDescription,
                    value = versionValue
                )
            }
        }
    }
}

@Composable
private fun SummaryInfoRow(
    title: String,
    description: String,
    value: String,
    trailingIcon: ImageVector? = null,
    trailingIconDescription: String? = null,
    onTrailingAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(0.44f), verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceXSmall)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = SettingColors.Text,
                fontWeight = FontWeight.Medium,
                fontSize = UiTokens.TextBodyLarge
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = SettingColors.Muted,
                fontSize = UiTokens.TextCaption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(
            modifier = Modifier.weight(0.56f),
            horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceSmall, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = SettingColors.Text,
                fontWeight = FontWeight.Medium,
                fontSize = UiTokens.TextBodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (trailingIcon != null && onTrailingAction != null) {
                OutlinedButton(
                    onClick = onTrailingAction,
                    modifier = Modifier.height(30.dp),
                    shape = RoundedCornerShape(UiTokens.RadiusMedium),
                    border = BorderStroke(1.dp, SettingColors.ButtonBorder.copy(alpha = 0.72f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = SettingColors.Surface,
                        contentColor = SettingColors.Muted
                    ),
                    contentPadding = PaddingValues(horizontal = UiTokens.SpaceSmall, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = trailingIcon,
                        contentDescription = trailingIconDescription,
                        tint = SettingColors.Muted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusPill(statusText: String, tone: StatusTone) {
    val color = when (tone) {
        StatusTone.Positive -> SettingColors.Primary
        StatusTone.Neutral -> SettingColors.Muted
        StatusTone.Danger -> SettingColors.Danger
    }
    val background = if (tone == StatusTone.Positive) SettingColors.PrimarySoft else color.copy(alpha = 0.08f)
    val border = if (tone == StatusTone.Positive) SettingColors.PrimaryBorder.copy(alpha = 0.72f) else color.copy(alpha = 0.14f)

    Surface(
        modifier = Modifier.widthIn(max = 360.dp),
        shape = RoundedCornerShape(UiTokens.BadgeRadius),
        color = background,
        border = BorderStroke(1.dp, border)
    ) {
        Text(
            text = statusText,
            modifier = Modifier.padding(horizontal = UiTokens.SpaceSmall, vertical = UiTokens.SpaceXSmall),
            style = MaterialTheme.typography.bodySmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
            fontSize = UiTokens.TextCaption,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SettingValueRow(
    title: String,
    description: String,
    value: String,
    valueColor: Color = Color.Unspecified,
    trailingIcon: ImageVector? = null,
    trailingIconDescription: String? = null,
    onTrailingAction: (() -> Unit)? = null
) {
    val resolvedValueColor = if (valueColor == Color.Unspecified) SettingColors.Text else valueColor
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = UiTokens.SpaceLarge),
        horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(0.62f), verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceXSmall)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = SettingColors.Text,
                fontWeight = FontWeight.Medium,
                fontSize = UiTokens.TextBodyLarge
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = SettingColors.Muted,
                fontSize = UiTokens.TextCaption
            )
        }
        Column(
            modifier = Modifier.weight(0.38f),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceXSmall)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = resolvedValueColor,
                fontSize = UiTokens.TextBodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (trailingIcon != null && onTrailingAction != null) {
                OutlinedButton(
                    onClick = onTrailingAction,
                    modifier = Modifier.height(30.dp),
                    shape = RoundedCornerShape(UiTokens.RadiusMedium),
                    border = BorderStroke(1.dp, SettingColors.ButtonBorder.copy(alpha = 0.72f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = SettingColors.Surface,
                        contentColor = SettingColors.Muted
                    ),
                    contentPadding = PaddingValues(horizontal = UiTokens.SpaceSmall, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = trailingIcon,
                        contentDescription = trailingIconDescription,
                        tint = SettingColors.Muted,
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
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    menuContent: @Composable ColumnScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = UiTokens.SpaceLarge),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceMedium)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceXSmall)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = SettingColors.Text,
                fontWeight = FontWeight.Medium,
                fontSize = UiTokens.TextBodyLarge
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = SettingColors.Muted,
                fontSize = UiTokens.TextCaption
            )
        }

        Box {
            Surface(
                onClick = { onExpandedChange(!expanded) },
                shape = RoundedCornerShape(UiTokens.RadiusMedium),
                color = SettingColors.Surface,
                border = BorderStroke(1.dp, SettingColors.ButtonBorder.copy(alpha = 0.72f)),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .height(32.dp)
                        .padding(horizontal = UiTokens.SpaceSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SettingColors.Text,
                        fontSize = UiTokens.TextBodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = IconParkIcons.ArrowDown,
                        contentDescription = null,
                        tint = SettingColors.Muted,
                        modifier = Modifier.size(UiTokens.IconSmall)
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
            .height(56.dp)
            .padding(horizontal = UiTokens.SpaceLarge),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceXSmall)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = SettingColors.Text,
                fontWeight = FontWeight.Medium,
                fontSize = UiTokens.TextBodyLarge
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = SettingColors.Muted,
                fontSize = UiTokens.TextCaption
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun updateStatusColor(status: UpdateStatus): Color {
    return when (status) {
        UpdateStatus.Idle -> SettingColors.Text
        UpdateStatus.Checking,
        UpdateStatus.Downloading -> SettingColors.Primary
        is UpdateStatus.UpToDate,
        is UpdateStatus.DownloadSuccess -> SettingColors.Success
        is UpdateStatus.UpdateAvailable -> SettingColors.Primary
        is UpdateStatus.CheckFailed,
        is UpdateStatus.DownloadFailed -> SettingColors.Danger
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
