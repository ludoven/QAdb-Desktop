package com.ludoven.adbtool.pages

import adbtool_desktop.composeapp.generated.resources.Res
import adbtool_desktop.composeapp.generated.resources.adb_path_label
import adbtool_desktop.composeapp.generated.resources.adb_path_setting
import adbtool_desktop.composeapp.generated.resources.adb_path_success_message
import adbtool_desktop.composeapp.generated.resources.current_adb_path
import adbtool_desktop.composeapp.generated.resources.language_changed
import adbtool_desktop.composeapp.generated.resources.language_setting
import adbtool_desktop.composeapp.generated.resources.not_set
import adbtool_desktop.composeapp.generated.resources.restart_required
import adbtool_desktop.composeapp.generated.resources.select_adb_icon_desc
import adbtool_desktop.composeapp.generated.resources.select_language
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import com.ludoven.adbtool.UiTokens
import com.ludoven.adbtool.util.AdbPathManager
import com.ludoven.adbtool.util.FileUtils
import com.ludoven.adbtool.util.LanguageManager
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
    val coroutineScope = rememberCoroutineScope()

    val currentLanguage by LanguageManager.currentLanguage.collectAsState()
    val supportedLanguages = LanguageManager.getSupportedLanguages()

    Column(
        modifier = Modifier
            .fillMaxSize()
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
