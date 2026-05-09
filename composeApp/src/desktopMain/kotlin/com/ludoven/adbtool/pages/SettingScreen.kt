package com.ludoven.adbtool.pages

import com.ludoven.adbtool.ui.mac.*

import adbtool_desktop.composeapp.generated.resources.Res
import adbtool_desktop.composeapp.generated.resources.adb_path_label
import adbtool_desktop.composeapp.generated.resources.adb_path_setting
import adbtool_desktop.composeapp.generated.resources.adb_path_success_message
import adbtool_desktop.composeapp.generated.resources.check_update
import adbtool_desktop.composeapp.generated.resources.current_adb_path
import adbtool_desktop.composeapp.generated.resources.current_version
import adbtool_desktop.composeapp.generated.resources.download_and_install
import adbtool_desktop.composeapp.generated.resources.downloading_update
import adbtool_desktop.composeapp.generated.resources.language_changed
import adbtool_desktop.composeapp.generated.resources.language_setting
import adbtool_desktop.composeapp.generated.resources.latest_version
import adbtool_desktop.composeapp.generated.resources.not_set
import adbtool_desktop.composeapp.generated.resources.open_release_page
import adbtool_desktop.composeapp.generated.resources.restart_required
import adbtool_desktop.composeapp.generated.resources.select_adb_icon_desc
import adbtool_desktop.composeapp.generated.resources.select_language
import adbtool_desktop.composeapp.generated.resources.select_theme
import adbtool_desktop.composeapp.generated.resources.theme_mode_dark
import adbtool_desktop.composeapp.generated.resources.theme_mode_light
import adbtool_desktop.composeapp.generated.resources.theme_mode_system
import adbtool_desktop.composeapp.generated.resources.theme_setting
import adbtool_desktop.composeapp.generated.resources.update_available_no_asset
import adbtool_desktop.composeapp.generated.resources.update_available_with_version
import adbtool_desktop.composeapp.generated.resources.update_check_failed
import adbtool_desktop.composeapp.generated.resources.update_checking
import adbtool_desktop.composeapp.generated.resources.update_download_failed
import adbtool_desktop.composeapp.generated.resources.update_download_success
import adbtool_desktop.composeapp.generated.resources.update_section_title
import adbtool_desktop.composeapp.generated.resources.update_up_to_date
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.ludoven.adbtool.ui.mac.ExposedDropdownMenuBox
import com.ludoven.adbtool.ui.mac.ExposedDropdownMenuDefaults
import com.ludoven.adbtool.ui.mac.Button
import com.ludoven.adbtool.ui.mac.CircularProgressIndicator
import com.ludoven.adbtool.ui.mac.Icon
import com.ludoven.adbtool.ui.mac.IconButton
import com.ludoven.adbtool.ui.mac.MaterialTheme
import com.ludoven.adbtool.ui.mac.OutlinedTextField
import com.ludoven.adbtool.ui.mac.OutlinedTextFieldDefaults
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.ludoven.adbtool.AppVersion
import com.ludoven.adbtool.UiTokens
import com.ludoven.adbtool.util.AdbPathManager
import com.ludoven.adbtool.util.FileUtils
import com.ludoven.adbtool.util.GitHubUpdateManager
import com.ludoven.adbtool.util.LanguageManager
import com.ludoven.adbtool.util.ThemeManager
import com.ludoven.adbtool.widget.SectionCard
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen() {
    val notSetText = stringResource(Res.string.not_set)
    var adbPath by remember { mutableStateOf(AdbPathManager.currentAdbPath ?: notSetText) }
    var showDialog by remember { mutableStateOf(false) }
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
        SectionCard(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(Res.string.adb_path_setting),
            icon = Icons.Default.FolderOpen
        ) {
            Text(
                text = stringResource(Res.string.current_adb_path),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = adbPath,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = {
                        coroutineScope.launch {
                            val newPath = FileUtils.selectFile()
                            if (newPath != null) {
                                AdbPathManager.setAdbPath(newPath)
                                adbPath = newPath
                                showDialog = true
                            }
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = stringResource(Res.string.select_adb_icon_desc)
                        )
                    }
                },
                label = { Text(stringResource(Res.string.adb_path_label)) },
                shape = RoundedCornerShape(UiTokens.RadiusMedium),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }

        SectionCard(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(Res.string.language_setting),
            icon = Icons.Default.Language
        ) {
            ExposedDropdownMenuBox(
                expanded = showLanguageDropdown,
                onExpandedChange = { showLanguageDropdown = !showLanguageDropdown }
            ) {
                OutlinedTextField(
                    value = currentLanguage.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(Res.string.select_language)) },
                    leadingIcon = {
                        Icon(Icons.Default.Language, contentDescription = null)
                    },
                    trailingIcon = {
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.clickable { showLanguageDropdown = !showLanguageDropdown }
                        )
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(UiTokens.RadiusMedium),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                DropdownMenu(
                    expanded = showLanguageDropdown,
                    onDismissRequest = { showLanguageDropdown = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
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
            }
        }

        SectionCard(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(Res.string.theme_setting),
            icon = Icons.Default.Settings
        ) {
            val currentThemeText = when (currentThemeMode) {
                ThemeManager.ThemeMode.SYSTEM -> stringResource(Res.string.theme_mode_system)
                ThemeManager.ThemeMode.LIGHT -> stringResource(Res.string.theme_mode_light)
                ThemeManager.ThemeMode.DARK -> stringResource(Res.string.theme_mode_dark)
            }

            ExposedDropdownMenuBox(
                expanded = showThemeDropdown,
                onExpandedChange = { showThemeDropdown = !showThemeDropdown }
            ) {
                OutlinedTextField(
                    value = currentThemeText,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(Res.string.select_theme)) },
                    leadingIcon = {
                        Icon(Icons.Default.Settings, contentDescription = null)
                    },
                    trailingIcon = {
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.clickable { showThemeDropdown = !showThemeDropdown }
                        )
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(UiTokens.RadiusMedium),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                DropdownMenu(
                    expanded = showThemeDropdown,
                    onDismissRequest = { showThemeDropdown = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
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
        }

        SectionCard(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(Res.string.update_section_title),
            icon = Icons.Default.SystemUpdateAlt
        ) {
            Text(
                text = stringResource(Res.string.current_version, currentVersionLabel),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            latestVersion?.let { latest ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.latest_version, latest),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!updateStatusText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = updateStatusText,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    modifier = Modifier.weight(1f),
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

    if (showDialog) {
        TipDialog(stringResource(Res.string.adb_path_success_message, adbPath)) {
            showDialog = false
        }
    }

    if (showLanguageDialog) {
        TipDialog(stringResource(Res.string.language_changed) + "\n" + stringResource(Res.string.restart_required)) {
            showLanguageDialog = false
        }
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
