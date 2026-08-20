package com.ludoven.adbtool.pages

import com.ludoven.adbtool.UiTokens
import com.ludoven.adbtool.QadbColors
import com.ludoven.adbtool.ui.icons.IconParkIcons

import adbtool_desktop.composeapp.generated.resources.Res
import adbtool_desktop.composeapp.generated.resources.ai_add_model
import adbtool_desktop.composeapp.generated.resources.ai_api_key
import adbtool_desktop.composeapp.generated.resources.ai_api_key_hint
import adbtool_desktop.composeapp.generated.resources.ai_api_key_required_hint
import adbtool_desktop.composeapp.generated.resources.ai_base_url
import adbtool_desktop.composeapp.generated.resources.ai_base_url_hint
import adbtool_desktop.composeapp.generated.resources.ai_clear_key
import adbtool_desktop.composeapp.generated.resources.ai_config_saved
import adbtool_desktop.composeapp.generated.resources.ai_context_window
import adbtool_desktop.composeapp.generated.resources.ai_context_window_hint
import adbtool_desktop.composeapp.generated.resources.ai_edit_model
import adbtool_desktop.composeapp.generated.resources.ai_agent_beta_disabled_desc
import adbtool_desktop.composeapp.generated.resources.ai_agent_beta_enabled_desc
import adbtool_desktop.composeapp.generated.resources.ai_agent_beta_settings
import adbtool_desktop.composeapp.generated.resources.ai_agent_beta_title
import adbtool_desktop.composeapp.generated.resources.ai_agent_reduce_motion_desc
import adbtool_desktop.composeapp.generated.resources.ai_agent_reduce_motion_title
import adbtool_desktop.composeapp.generated.resources.ai_hide_key
import adbtool_desktop.composeapp.generated.resources.ai_key_cleared
import adbtool_desktop.composeapp.generated.resources.ai_model_configured
import adbtool_desktop.composeapp.generated.resources.ai_model_configured_desc
import adbtool_desktop.composeapp.generated.resources.ai_model_name
import adbtool_desktop.composeapp.generated.resources.ai_model_name_hint
import adbtool_desktop.composeapp.generated.resources.ai_model_not_configured
import adbtool_desktop.composeapp.generated.resources.ai_model_not_configured_desc
import adbtool_desktop.composeapp.generated.resources.ai_model_settings
import adbtool_desktop.composeapp.generated.resources.ai_memory_clear
import adbtool_desktop.composeapp.generated.resources.ai_memory_clear_desc
import adbtool_desktop.composeapp.generated.resources.ai_memory_clear_title
import adbtool_desktop.composeapp.generated.resources.ai_memory_content
import adbtool_desktop.composeapp.generated.resources.ai_memory_delete
import adbtool_desktop.composeapp.generated.resources.ai_memory_disabled_desc
import adbtool_desktop.composeapp.generated.resources.ai_memory_edit
import adbtool_desktop.composeapp.generated.resources.ai_memory_edit_title
import adbtool_desktop.composeapp.generated.resources.ai_memory_empty
import adbtool_desktop.composeapp.generated.resources.ai_memory_enabled_desc
import adbtool_desktop.composeapp.generated.resources.ai_memory_keywords
import adbtool_desktop.composeapp.generated.resources.ai_memory_manage
import adbtool_desktop.composeapp.generated.resources.ai_memory_manage_title
import adbtool_desktop.composeapp.generated.resources.ai_memory_privacy
import adbtool_desktop.composeapp.generated.resources.ai_memory_settings
import adbtool_desktop.composeapp.generated.resources.ai_memory_stats
import adbtool_desktop.composeapp.generated.resources.ai_memory_title
import adbtool_desktop.composeapp.generated.resources.ai_input_helper_install
import adbtool_desktop.composeapp.generated.resources.ai_input_helper_installed
import adbtool_desktop.composeapp.generated.resources.ai_input_helper_missing
import adbtool_desktop.composeapp.generated.resources.ai_input_helper_no_device
import adbtool_desktop.composeapp.generated.resources.ai_input_helper_settings
import adbtool_desktop.composeapp.generated.resources.ai_input_helper_test
import adbtool_desktop.composeapp.generated.resources.ai_input_helper_title
import adbtool_desktop.composeapp.generated.resources.ai_input_helper_uninstall
import adbtool_desktop.composeapp.generated.resources.agent_memory_consent_desc
import adbtool_desktop.composeapp.generated.resources.agent_memory_consent_title
import adbtool_desktop.composeapp.generated.resources.agent_memory_enable
import adbtool_desktop.composeapp.generated.resources.agent_memory_not_now
import adbtool_desktop.composeapp.generated.resources.agent_approval_settings
import adbtool_desktop.composeapp.generated.resources.agent_approval_policy
import adbtool_desktop.composeapp.generated.resources.agent_approval_smart
import adbtool_desktop.composeapp.generated.resources.agent_approval_cautious
import adbtool_desktop.composeapp.generated.resources.agent_approval_smart_desc
import adbtool_desktop.composeapp.generated.resources.agent_approval_cautious_desc
import adbtool_desktop.composeapp.generated.resources.ai_openai_compatible_badge
import adbtool_desktop.composeapp.generated.resources.ai_privacy_notice
import adbtool_desktop.composeapp.generated.resources.ai_save_config
import adbtool_desktop.composeapp.generated.resources.ai_show_key
import adbtool_desktop.composeapp.generated.resources.ai_test_connection
import adbtool_desktop.composeapp.generated.resources.ai_test_success
import adbtool_desktop.composeapp.generated.resources.ai_vision_auto
import adbtool_desktop.composeapp.generated.resources.ai_vision_disabled
import adbtool_desktop.composeapp.generated.resources.ai_vision_enabled
import adbtool_desktop.composeapp.generated.resources.ai_vision_mode
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
import adbtool_desktop.composeapp.generated.resources.cancel
import adbtool_desktop.composeapp.generated.resources.check_update
import adbtool_desktop.composeapp.generated.resources.current_version
import adbtool_desktop.composeapp.generated.resources.download_and_install
import adbtool_desktop.composeapp.generated.resources.downloading_update
import adbtool_desktop.composeapp.generated.resources.language_changed
import adbtool_desktop.composeapp.generated.resources.not_set
import adbtool_desktop.composeapp.generated.resources.open_release_page
import adbtool_desktop.composeapp.generated.resources.preferences_setting
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
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ludoven.adbtool.AppVersion
import com.ludoven.adbtool.entity.MsgContent
import com.ludoven.adbtool.agent.AiConfiguration
import com.ludoven.adbtool.agent.AgentProviderRuntime
import com.ludoven.adbtool.agent.AgentCapabilityProbe
import com.ludoven.adbtool.agent.AgentCapabilityReport
import com.ludoven.adbtool.agent.AgentCapabilityTier
import com.ludoven.adbtool.agent.AgentModelRole
import com.ludoven.adbtool.agent.AiModelConfig
import com.ludoven.adbtool.agent.AgentInputHelper
import com.ludoven.adbtool.agent.AgentInputHelperStatus
import com.ludoven.adbtool.agent.AgentApprovalPolicy
import com.ludoven.adbtool.agent.AgentApprovalRuntime
import com.ludoven.adbtool.agent.AgentFeatureRuntime
import com.ludoven.adbtool.agent.VisionMode
import com.ludoven.adbtool.ui.icons.CompatIconVectors
import com.ludoven.adbtool.util.l10n
import com.ludoven.adbtool.ui.mac.AlertDialog
import com.ludoven.adbtool.ui.mac.Button
import com.ludoven.adbtool.ui.mac.ButtonDefaults
import com.ludoven.adbtool.ui.mac.CircularProgressIndicator
import com.ludoven.adbtool.ui.mac.DropdownMenu
import com.ludoven.adbtool.ui.mac.DropdownMenuItem
import com.ludoven.adbtool.ui.mac.HorizontalDivider
import com.ludoven.adbtool.ui.mac.Icon
import com.ludoven.adbtool.ui.mac.MaterialTheme
import com.ludoven.adbtool.ui.mac.OutlinedButton
import com.ludoven.adbtool.ui.mac.OutlinedTextField
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
import com.ludoven.adbtool.widget.FeedbackToast
import com.ludoven.adbtool.util.LanguageManager
import com.ludoven.adbtool.util.ThemeManager
import com.ludoven.adbtool.widget.PageHeader
import kotlinx.coroutines.delay
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
                containerColor = SettingColors.Surface,
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
fun SettingScreen(selectedDeviceId: String? = null) {
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
                .widthIn(max = 760.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = UiTokens.PagePadding, vertical = UiTokens.PagePaddingCompact),
            verticalArrangement = Arrangement.spacedBy(UiTokens.SectionSpacing)
        ) {
            PageHeader(
                title = stringResource(Res.string.set),
                subtitle = stringResource(Res.string.settings_subtitle)
            )

            val agentFeaturePreferences = remember { AgentFeatureRuntime.preferences }
            val agentFeatureEnabled by agentFeaturePreferences.enabled.collectAsState()
            AgentFeatureSettingsSection(
                enabled = agentFeatureEnabled,
                onEnabledChange = agentFeaturePreferences::setEnabled
            )

            SettingsColumns(
                mainContent = {
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

                },
                sideContent = {
                    val adbSettingsContent: @Composable () -> Unit = {
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
                            trailingIcon = IconParkIcons.FolderOpen,
                            trailingIconDescription = stringResource(Res.string.adb_path_label),
                            onTrailingAction = { openPathLocation(adbEnvironment.path) }
                        )

                        SettingsActionArea {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceSmall)
                            ) {
                                SettingActionButton(
                                    text = stringResource(Res.string.adb_auto_detect),
                                    icon = IconParkIcons.Refresh,
                                    onClick = {
                                        coroutineScope.launch {
                                            AdbPathManager.autoDetect()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    primary = true
                                )

                                SettingActionButton(
                                    text = stringResource(Res.string.adb_select_adb),
                                    icon = IconParkIcons.File,
                                    onClick = {
                                        coroutineScope.launch {
                                            val newPath = FileUtils.selectFile()
                                            if (newPath != null) {
                                                AdbPathManager.setAdbPath(newPath)
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceSmall)
                            ) {
                                SettingActionButton(
                                    text = stringResource(Res.string.adb_restore_bundled),
                                    icon = IconParkIcons.Setting,
                                    onClick = {
                                        coroutineScope.launch {
                                            AdbPathManager.useBundledAdb()
                                        }
                                    },
                                    modifier = Modifier.weight(1.4f)
                                )

                                SettingActionButton(
                                    text = stringResource(Res.string.adb_open_help),
                                    icon = Icons.AutoMirrored.Filled.OpenInNew,
                                    onClick = {
                                        AdbPathManager.openHelp().onFailure { _ -> }
                                    },
                                    modifier = Modifier.weight(0.6f)
                                )
                            }
                        }
                        }
                    }

                    SettingsSection(title = stringResource(Res.string.update_section_title)) {
                        SideSettingValueRow(
                            title = stringResource(Res.string.current_version, "").trim().trimEnd(':', '：'),
                            description = stringResource(Res.string.settings_update_current_version_desc),
                            value = currentVersionLabel
                        )
                        SectionDivider()
                        SideSettingValueRow(
                            title = stringResource(Res.string.settings_update_status),
                            description = stringResource(Res.string.settings_update_status_desc),
                            value = updateStatusText,
                            valueColor = updateStatusColor(updateStatus)
                        )

                        SettingsActionArea {
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
                                                updateStatus = UpdateStatus.UpdateAvailable(
                                                    result.latestVersion,
                                                    hasAutoInstall = true
                                                )
                                            }
                                            is GitHubUpdateManager.CheckResult.UpdateAvailableNoAsset -> {
                                                latestVersion = result.latestVersion
                                                releaseUrl = result.htmlUrl
                                                downloadableAsset = null
                                                updateStatus = UpdateStatus.UpdateAvailable(
                                                    result.latestVersion,
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
                                modifier = Modifier.fillMaxWidth(),
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
                                                    updateStatus = UpdateStatus.DownloadFailed(
                                                        error.message ?: "Unknown error"
                                                    )
                                                }
                                                isDownloadingUpdate = false
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
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
                                                updateStatus = UpdateStatus.CheckFailed(
                                                    error.message ?: "Unknown error"
                                                )
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    adbSettingsContent()
                }
            )

            if (agentFeatureEnabled) {
                AiModelSettingsSection()
            }
        }
    }

    LaunchedEffect(showLanguageDialog) {
        if (showLanguageDialog) {
            delay(2400L)
            showLanguageDialog = false
        }
    }
    FeedbackToast(
        if (showLanguageDialog) MsgContent.Resource(Res.string.language_changed) else null
    )
}

@Composable
private fun SettingsColumns(
    mainContent: @Composable ColumnScope.() -> Unit,
    sideContent: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(UiTokens.SectionSpacing)
    ) {
        sideContent()
        mainContent()
    }
}

@Composable
private fun AgentFeatureSettingsSection(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit
) {
    SettingsSection(title = stringResource(Res.string.ai_agent_beta_settings)) {
        SettingSwitchRow(
            title = stringResource(Res.string.ai_agent_beta_title),
            description = stringResource(
                if (enabled) {
                    Res.string.ai_agent_beta_enabled_desc
                } else {
                    Res.string.ai_agent_beta_disabled_desc
                }
            ),
            checked = enabled,
            onCheckedChange = onEnabledChange
        )
    }
}

@Composable
private fun AgentApprovalSettingsSection() {
    val preferences = remember { AgentApprovalRuntime.preferences }
    val policy by preferences.policy.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    val policyLabel = stringResource(
        when (policy) {
            AgentApprovalPolicy.SMART -> Res.string.agent_approval_smart
            AgentApprovalPolicy.CAUTIOUS -> Res.string.agent_approval_cautious
        }
    )

    SettingsSection(title = stringResource(Res.string.agent_approval_settings)) {
        SettingsDropdownRow(
            title = stringResource(Res.string.agent_approval_policy),
            description = stringResource(
                when (policy) {
                    AgentApprovalPolicy.SMART -> Res.string.agent_approval_smart_desc
                    AgentApprovalPolicy.CAUTIOUS -> Res.string.agent_approval_cautious_desc
                }
            ),
            value = policyLabel,
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            AgentApprovalPolicy.entries.forEach { candidate ->
                val title = stringResource(
                    if (candidate == AgentApprovalPolicy.SMART) {
                        Res.string.agent_approval_smart
                    } else {
                        Res.string.agent_approval_cautious
                    }
                )
                val description = stringResource(
                    if (candidate == AgentApprovalPolicy.SMART) {
                        Res.string.agent_approval_smart_desc
                    } else {
                        Res.string.agent_approval_cautious_desc
                    }
                )
                DropdownMenuItem(
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(title, color = SettingColors.Text, fontSize = UiTokens.TextBody)
                            Text(description, color = SettingColors.Muted, fontSize = UiTokens.TextCaption)
                        }
                    },
                    onClick = {
                        preferences.setPolicy(candidate)
                        expanded = false
                    }
                )
            }
        }
    }
}
@Composable
private fun AgentInputHelperSettingsSection(selectedDeviceId: String?) {
    val helper = remember { AgentInputHelper() }
    val coroutineScope = rememberCoroutineScope()
    var status by remember(selectedDeviceId) { mutableStateOf<AgentInputHelperStatus?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isBusy by remember { mutableStateOf(false) }

    fun refresh() {
        val deviceId = selectedDeviceId ?: return
        coroutineScope.launch {
            status = runCatching { helper.status(deviceId) }
                .onFailure { statusMessage = it.message }
                .getOrNull()
        }
    }

    LaunchedEffect(selectedDeviceId) {
        if (selectedDeviceId != null) refresh()
    }

    val description = when {
        selectedDeviceId == null -> stringResource(Res.string.ai_input_helper_no_device)
        status?.installed == true -> stringResource(Res.string.ai_input_helper_installed)
        else -> stringResource(Res.string.ai_input_helper_missing)
    }
    SettingsSection(title = stringResource(Res.string.ai_input_helper_settings)) {
        SettingValueRow(
            title = stringResource(Res.string.ai_input_helper_title),
            description = description,
            value = if (status?.installed == true) {
                stringResource(Res.string.set)
            } else {
                stringResource(Res.string.not_set)
            },
            valueColor = if (status?.installed == true) SettingColors.Success else SettingColors.Muted
        )
        ActionButtonRow {
            SettingActionButton(
                text = stringResource(Res.string.ai_input_helper_install),
                icon = CompatIconVectors.Keyboard,
                onClick = {
                    val deviceId = selectedDeviceId ?: return@SettingActionButton
                    coroutineScope.launch {
                        isBusy = true
                        val result = helper.install(deviceId)
                        statusMessage = result.output
                        isBusy = false
                        refresh()
                    }
                },
                enabled = selectedDeviceId != null && !isBusy,
                loading = isBusy
            )
            SettingActionButton(
                text = stringResource(Res.string.ai_input_helper_test),
                icon = Icons.AutoMirrored.Filled.OpenInNew,
                onClick = {
                    val deviceId = selectedDeviceId ?: return@SettingActionButton
                    coroutineScope.launch { statusMessage = helper.openTestScreen(deviceId).output }
                },
                enabled = selectedDeviceId != null && status?.installed == true && !isBusy
            )
            SettingActionButton(
                text = stringResource(Res.string.ai_input_helper_uninstall),
                icon = CompatIconVectors.Delete,
                onClick = {
                    val deviceId = selectedDeviceId ?: return@SettingActionButton
                    coroutineScope.launch {
                        isBusy = true
                        statusMessage = helper.uninstall(deviceId).output
                        isBusy = false
                        refresh()
                    }
                },
                enabled = selectedDeviceId != null && status?.installed == true && !isBusy
            )
        }
        statusMessage?.let {
            Text(
                text = it,
                modifier = Modifier.padding(horizontal = UiTokens.SpaceLarge),
                color = SettingColors.SecondaryText,
                fontSize = UiTokens.TextBody
            )
        }
    }
}

@Composable
private fun AiModelSettingsSection() {
    val repository = remember { AiConfiguration.repository }
    val providerRepository = remember { AgentProviderRuntime.repository }
    val providerProfiles by providerRepository.profiles.collectAsState()
    val savedConfig by repository.config.collectAsState()
    val hasSavedKey by repository.hasApiKeyState.collectAsState()
    var showModelDialog by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var statusIsError by remember { mutableStateOf(false) }
    val configSavedText = stringResource(Res.string.ai_config_saved)
    val keyClearedText = stringResource(Res.string.ai_key_cleared)

    LaunchedEffect(Unit) {
        repository.hasApiKey()
    }

    val isConfigured = savedConfig.model.isNotBlank() && hasSavedKey
    val summaryTitle = if (isConfigured) {
        savedConfig.model
    } else {
        stringResource(Res.string.ai_model_not_configured)
    }
    val summaryDescription = if (isConfigured) {
        val configured = stringResource(
            Res.string.ai_model_configured_desc,
            visionModeText(savedConfig.visionMode)
        )
        val brainProfileId = providerRepository.providerFor(AgentModelRole.BRAIN)?.id
        val profile = providerProfiles.firstOrNull { it.id == brainProfileId }
        val attestation = profile?.let(providerRepository::capabilityAttestation)
        if (attestation == null) {
            "$configured · ${l10n("能力未验证", "Capabilities not verified")}"
        } else {
            "$configured · ${l10n("已验证", "Verified")} ${attestation.tier.name}"
        }
    } else {
        stringResource(Res.string.ai_model_not_configured_desc)
    }

    SettingsSection(title = stringResource(Res.string.ai_model_settings)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(UiTokens.RadiusLarge),
            color = SettingColors.SoftSurface,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = UiTokens.SpaceLarge, vertical = UiTokens.SpaceMedium),
                horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceMedium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(38.dp),
                    shape = CircleShape,
                    color = SettingColors.PrimarySoft,
                    border = BorderStroke(1.dp, SettingColors.PrimaryBorder)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = IconParkIcons.Setting,
                            contentDescription = null,
                            modifier = Modifier.size(UiTokens.IconMedium),
                            tint = SettingColors.Primary
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceXSmall)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceSmall),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = summaryTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = SettingColors.Text,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = UiTokens.TextBodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isConfigured) {
                            StatusPill(
                                statusText = stringResource(Res.string.ai_model_configured),
                                tone = StatusTone.Positive
                            )
                        }
                    }
                    Text(
                        text = summaryDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = SettingColors.Muted,
                        fontSize = UiTokens.TextCaption,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                SettingActionButton(
                    text = stringResource(
                        if (isConfigured) Res.string.ai_edit_model else Res.string.ai_add_model
                    ),
                    icon = if (isConfigured) CompatIconVectors.Edit else CompatIconVectors.Add,
                    onClick = {
                        statusMessage = null
                        showModelDialog = true
                    },
                    primary = true
                )
            }
        }

        statusMessage?.let { message ->
            Text(
                text = message,
                modifier = Modifier.padding(horizontal = UiTokens.SpaceLarge),
                color = if (statusIsError) SettingColors.Danger else SettingColors.Success,
                fontSize = UiTokens.TextBody
            )
        }
    }

    if (showModelDialog) {
        AiModelConfigDialog(
            initialConfig = savedConfig,
            hasSavedKey = hasSavedKey,
            onSaved = {
                statusIsError = false
                statusMessage = configSavedText
                showModelDialog = false
            },
            onKeyCleared = {
                statusIsError = false
                statusMessage = keyClearedText
            },
            onDismiss = { showModelDialog = false }
        )
    }
}

@Composable
internal fun AiModelConfigDialog(
    initialConfig: AiModelConfig,
    hasSavedKey: Boolean,
    onSaved: () -> Unit,
    onKeyCleared: () -> Unit,
    onDismiss: () -> Unit
) {
    val repository = remember { AiConfiguration.repository }
    val providerRepository = remember { AgentProviderRuntime.repository }
    val capabilityProbe = remember { AgentCapabilityProbe() }
    val coroutineScope = rememberCoroutineScope()
    var baseUrl by remember(initialConfig.baseUrl) { mutableStateOf(initialConfig.baseUrl) }
    var model by remember(initialConfig.model) { mutableStateOf(initialConfig.model) }
    var apiKey by remember { mutableStateOf("") }
    val visionMode = VisionMode.ENABLED
    var keyAvailable by remember(hasSavedKey) { mutableStateOf(hasSavedKey) }
    var showApiKey by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var statusIsError by remember { mutableStateOf(false) }
    var capabilityReport by remember { mutableStateOf<AgentCapabilityReport?>(null) }
    val testSuccessText = stringResource(Res.string.ai_test_success)
    val keyClearedText = stringResource(Res.string.ai_key_cleared)
    val isBusy = isSaving || isTesting

    fun currentConfig() = AiModelConfig(
        baseUrl = baseUrl,
        model = model,
        visionMode = visionMode,
        contextWindowTokens = initialConfig.contextWindowTokens
    )

    fun invalidateCapabilityResult() {
        capabilityReport = null
        statusMessage = null
    }

    Dialog(onDismissRequest = { if (!isBusy) onDismiss() }) {
        Surface(
            modifier = Modifier.width(560.dp),
            shape = RoundedCornerShape(UiTokens.RadiusLarge),
            color = SettingColors.Surface,
            border = BorderStroke(1.dp, SettingColors.Border),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = UiTokens.SpaceLarge,
                            top = UiTokens.SpaceMedium,
                            end = UiTokens.SpaceMedium,
                            bottom = UiTokens.SpaceMedium
                        ),
                    horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(
                            if (hasSavedKey || initialConfig.model.isNotBlank()) {
                                Res.string.ai_edit_model
                            } else {
                                Res.string.ai_add_model
                            }
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = SettingColors.Text,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = UiTokens.TextSection
                    )
                    Surface(
                        shape = RoundedCornerShape(UiTokens.BadgeRadius),
                        color = SettingColors.SoftSurface
                    ) {
                        Text(
                            text = stringResource(Res.string.ai_openai_compatible_badge),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            color = SettingColors.SecondaryText,
                            fontSize = UiTokens.TextCaption
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable(enabled = !isBusy, onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = IconParkIcons.Close,
                            contentDescription = stringResource(Res.string.cancel),
                            modifier = Modifier.size(UiTokens.IconSmall),
                            tint = SettingColors.Muted
                        )
                    }
                }

                HorizontalDivider(color = SettingColors.Divider)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = UiTokens.SpaceLarge, vertical = UiTokens.SpaceMedium),
                    verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceMedium)
                ) {
                    AiDialogTextField(
                        label = stringResource(Res.string.ai_base_url),
                        value = baseUrl,
                        onValueChange = {
                            baseUrl = it
                            invalidateCapabilityResult()
                        },
                        placeholder = stringResource(Res.string.ai_base_url_hint)
                    )

                    AiDialogTextField(
                        label = stringResource(Res.string.ai_api_key),
                        value = apiKey,
                        onValueChange = {
                            apiKey = it
                            invalidateCapabilityResult()
                        },
                        placeholder = stringResource(
                            if (keyAvailable) {
                                Res.string.ai_api_key_hint
                            } else {
                                Res.string.ai_api_key_required_hint
                            }
                        ),
                        visualTransformation = if (showApiKey) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingContent = {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .clickable { showApiKey = !showApiKey },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (showApiKey) {
                                        CompatIconVectors.EyeOff
                                    } else {
                                        CompatIconVectors.Eye
                                    },
                                    contentDescription = stringResource(
                                        if (showApiKey) Res.string.ai_hide_key else Res.string.ai_show_key
                                    ),
                                    modifier = Modifier.size(UiTokens.IconSmall),
                                    tint = SettingColors.Muted
                                )
                            }
                        }
                    )

                    AiDialogTextField(
                        label = stringResource(Res.string.ai_model_name),
                        value = model,
                        onValueChange = {
                            model = it
                            invalidateCapabilityResult()
                        },
                        placeholder = stringResource(Res.string.ai_model_name_hint)
                    )

                    SideSettingValueRow(
                        title = l10n("视觉能力", "Vision capability"),
                        description = l10n(
                            "Agent 每一步都需要最新截图，不能切换为纯文本模式。",
                            "Every Agent step requires a fresh screenshot; text-only mode is unavailable."
                        ),
                        value = l10n("强制开启", "Required")
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(UiTokens.RadiusMedium),
                        color = SettingColors.SoftSurface,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp
                    ) {
                        Text(
                            text = stringResource(Res.string.ai_privacy_notice),
                            modifier = Modifier.padding(UiTokens.SpaceMedium),
                            color = SettingColors.SecondaryText,
                            fontSize = UiTokens.TextCaption
                        )
                    }

                    statusMessage?.let { message ->
                        Text(
                            text = message,
                            color = if (statusIsError) SettingColors.Danger else SettingColors.Success,
                            fontSize = UiTokens.TextBody
                        )
                    }
                }

                HorizontalDivider(color = SettingColors.Divider)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = UiTokens.SpaceLarge, vertical = UiTokens.SpaceMedium),
                    horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (keyAvailable) {
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    val result = repository.clearApiKey()
                                    statusIsError = result.isFailure
                                    statusMessage = result.exceptionOrNull()?.message ?: keyClearedText
                                    if (result.isSuccess) {
                                        apiKey = ""
                                        keyAvailable = false
                                        onKeyCleared()
                                    }
                                }
                            },
                            modifier = Modifier.height(36.dp),
                            enabled = !isBusy,
                            shape = RoundedCornerShape(UiTokens.RadiusMedium),
                            border = BorderStroke(1.dp, SettingColors.Border),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = SettingColors.Surface,
                                contentColor = SettingColors.Danger
                            ),
                            contentPadding = PaddingValues(horizontal = UiTokens.SpaceMedium, vertical = 0.dp)
                        ) {
                            Text(
                                text = stringResource(Res.string.ai_clear_key),
                                color = SettingColors.Danger,
                                fontSize = UiTokens.TextBody
                            )
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                isTesting = true
                                val key = apiKey.trim().ifBlank { repository.loadApiKey().orEmpty() }
                                val result = runCatching {
                                    require(key.isNotBlank()) { "API Key is required" }
                                    val provider = providerRepository.legacyPreview(currentConfig(), key)
                                    capabilityProbe.probe(provider)
                                }
                                capabilityReport = result.getOrNull()
                                statusIsError = result.isFailure ||
                                    result.getOrNull()?.tier?.let { it < AgentCapabilityTier.L3_VISUAL_AGENT } == true
                                statusMessage = result.exceptionOrNull()?.message ?: result.getOrNull()?.let { report ->
                                    if (report.tier >= AgentCapabilityTier.L3_VISUAL_AGENT) {
                                        "$testSuccessText · ${l10n("能力等级", "Capability tier")} ${report.tier.name}"
                                    } else {
                                        l10n(
                                            "连接成功，但模型未通过视觉与工具调用能力测试（需要 L3，当前 ${report.tier.name}）。",
                                            "Connection succeeded, but the model did not pass the visual tool-use test (L3 required, current ${report.tier.name})."
                                        )
                                    }
                                } ?: testSuccessText
                                isTesting = false
                            }
                        },
                        modifier = Modifier.height(36.dp),
                        enabled = !isBusy && (apiKey.isNotBlank() || keyAvailable),
                        shape = RoundedCornerShape(UiTokens.RadiusMedium),
                        border = BorderStroke(1.dp, SettingColors.Border),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = SettingColors.Surface,
                            contentColor = SettingColors.Text
                        ),
                        contentPadding = PaddingValues(horizontal = UiTokens.SpaceMedium, vertical = 0.dp)
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = SettingColors.Primary
                            )
                            Spacer(Modifier.width(UiTokens.SpaceSmall))
                        }
                        Text(
                            text = stringResource(Res.string.ai_test_connection),
                            color = if (isBusy && !isTesting) SettingColors.Muted else SettingColors.Text,
                            fontSize = UiTokens.TextBody
                        )
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.height(36.dp),
                        enabled = !isBusy,
                        shape = RoundedCornerShape(UiTokens.RadiusMedium),
                        border = BorderStroke(1.dp, SettingColors.Border),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = SettingColors.Surface,
                            contentColor = SettingColors.Text
                        ),
                        contentPadding = PaddingValues(horizontal = UiTokens.SpaceMedium, vertical = 0.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.cancel),
                            color = SettingColors.Text,
                            fontSize = UiTokens.TextBody
                        )
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isSaving = true
                                val result = repository.save(currentConfig(), apiKey)
                                statusIsError = result.isFailure
                                statusMessage = result.exceptionOrNull()?.message
                                if (result.isSuccess) {
                                    providerRepository.syncLegacy(currentConfig(), apiKey)
                                    capabilityReport?.let { report ->
                                        providerRepository.resolve(AgentModelRole.BRAIN)
                                            ?.let { provider ->
                                                providerRepository.attestCapabilities(provider.profile, report.tier)
                                            }
                                    }
                                    onSaved()
                                }
                                isSaving = false
                            }
                        },
                        modifier = Modifier.height(36.dp),
                        enabled = !isBusy,
                        shape = RoundedCornerShape(UiTokens.RadiusMedium),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SettingColors.Primary,
                            contentColor = QadbColors.onPrimary,
                            disabledContainerColor = SettingColors.ControlBackground,
                            disabledContentColor = SettingColors.Muted
                        ),
                        contentPadding = PaddingValues(horizontal = UiTokens.SpaceLarge, vertical = 0.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = QadbColors.onPrimary
                            )
                            Spacer(Modifier.width(UiTokens.SpaceSmall))
                        }
                        Text(
                            text = stringResource(Res.string.ai_save_config),
                            color = if (isBusy && !isSaving) SettingColors.Muted else QadbColors.onPrimary,
                            fontSize = UiTokens.TextBody,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AiDialogTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = SettingColors.Text,
            fontWeight = FontWeight.Medium,
            fontSize = UiTokens.TextBody
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = placeholder,
                    color = SettingColors.Muted,
                    fontSize = UiTokens.TextBody
                )
            },
            trailingIcon = trailingContent,
            visualTransformation = visualTransformation,
            singleLine = true,
            shape = RoundedCornerShape(UiTokens.RadiusMedium)
        )
    }
}

@Composable
private fun AiDialogSelectField(
    label: String,
    value: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    menuContent: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = SettingColors.Text,
            fontWeight = FontWeight.Medium,
            fontSize = UiTokens.TextBody
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            Surface(
                onClick = { onExpandedChange(!expanded) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(UiTokens.RadiusMedium),
                color = SettingColors.Surface,
                border = BorderStroke(1.dp, SettingColors.Border),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(UiTokens.ControlHeight)
                        .padding(horizontal = UiTokens.SpaceMedium),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = value,
                        color = SettingColors.Text,
                        fontSize = UiTokens.TextBody
                    )
                    Icon(
                        imageVector = IconParkIcons.ArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(UiTokens.IconSmall),
                        tint = SettingColors.Muted
                    )
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface),
                content = menuContent
            )
        }
    }
}

@Composable
private fun visionModeText(mode: VisionMode): String = stringResource(
    when (mode) {
        VisionMode.AUTO -> Res.string.ai_vision_auto
        VisionMode.ENABLED -> Res.string.ai_vision_enabled
        VisionMode.DISABLED -> Res.string.ai_vision_disabled
    }
)

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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(16.dp)
                    .clip(RoundedCornerShape(UiTokens.BadgeRadius))
                    .background(SettingColors.Primary)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = SettingColors.Text,
                fontWeight = FontWeight.SemiBold,
                fontSize = UiTokens.TextSection
            )
        }
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
private fun SettingsActionArea(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = UiTokens.SpaceLarge, vertical = UiTokens.SpaceMedium),
        verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceSmall),
        content = content
    )
}

@Composable
private fun SideSettingValueRow(
    title: String,
    description: String,
    value: String,
    valueColor: Color = Color.Unspecified
) {
    val resolvedValueColor = if (valueColor == Color.Unspecified) SettingColors.Text else valueColor
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = UiTokens.SpaceLarge, vertical = UiTokens.SpaceMedium),
        verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceXSmall)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceMedium),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(0.4f),
                style = MaterialTheme.typography.bodyMedium,
                color = SettingColors.Text,
                fontWeight = FontWeight.Medium,
                fontSize = UiTokens.TextBodyLarge
            )
            Text(
                text = value,
                modifier = Modifier.weight(0.6f),
                style = MaterialTheme.typography.bodyMedium,
                color = resolvedValueColor,
                fontSize = UiTokens.TextBodyLarge,
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = SettingColors.Muted,
            fontSize = UiTokens.TextCaption,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
        shape = RoundedCornerShape(0.dp),
        color = Color.Transparent,
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

            HorizontalDivider(color = SettingColors.Divider)

            Column {
                SummaryInfoRow(
                    title = sourceLabel,
                    description = sourceDescription,
                    value = sourceValue
                )
                HorizontalDivider(color = SettingColors.Divider)
                SummaryInfoRow(
                    title = pathLabel,
                    description = pathDescription,
                    value = pathValue,
                    stackedValue = true,
                    trailingIcon = trailingIcon,
                    trailingIconDescription = trailingIconDescription,
                    onTrailingAction = onTrailingAction
                )
                HorizontalDivider(color = SettingColors.Divider)
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
    stackedValue: Boolean = false,
    trailingIcon: ImageVector? = null,
    trailingIconDescription: String? = null,
    onTrailingAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = UiTokens.SpaceMedium),
        verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceSmall)
    ) {
        if (stackedValue) {
            SummaryInfoLabel(
                title = title,
                description = description,
                modifier = Modifier.fillMaxWidth()
            )
            SummaryInfoValue(
                value = value,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2,
                trailingIcon = trailingIcon,
                trailingIconDescription = trailingIconDescription,
                onTrailingAction = onTrailingAction
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceMedium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SummaryInfoLabel(
                    title = title,
                    description = description,
                    modifier = Modifier.weight(0.46f)
                )
                SummaryInfoValue(
                    value = value,
                    modifier = Modifier.weight(0.54f),
                    maxLines = 1,
                    trailingIcon = trailingIcon,
                    trailingIconDescription = trailingIconDescription,
                    onTrailingAction = onTrailingAction
                )
            }
        }
    }
}

@Composable
private fun SummaryInfoLabel(title: String, description: String, modifier: Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceXSmall)
    ) {
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
}

@Composable
private fun SummaryInfoValue(
    value: String,
    modifier: Modifier,
    maxLines: Int,
    trailingIcon: ImageVector?,
    trailingIconDescription: String?,
    onTrailingAction: (() -> Unit)?
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = SettingColors.Text,
            fontWeight = FontWeight.Medium,
            fontSize = UiTokens.TextBodyLarge,
            maxLines = maxLines,
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

@Composable
private fun StatusPill(statusText: String, tone: StatusTone) {
    val color = when (tone) {
        StatusTone.Positive -> SettingColors.Success
        StatusTone.Neutral -> SettingColors.Muted
        StatusTone.Danger -> SettingColors.Danger
    }

    Row(
        modifier = Modifier.widthIn(max = 360.dp),
        horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = statusText,
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
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
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
        Switch(
            checked = checked,
            onCheckedChange = { value -> if (enabled) onCheckedChange(value) }
        )
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
