package com.ludoven.adbtool.pages

import com.ludoven.adbtool.ui.mac.*

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ludoven.adbtool.UiTokens
import com.ludoven.adbtool.viewmodel.DeviceMirrorViewModel
import com.ludoven.adbtool.viewmodel.KeyEventViewModel

enum class DeviceControlTab {
    Mirror,
    Keys
}

@Composable
fun DeviceControlScreen(
    mirrorViewModel: DeviceMirrorViewModel,
    keyEventViewModel: KeyEventViewModel,
    selectedDevice: String?,
    deviceDisplayNames: Map<String, String>,
    @Suppress("UNUSED_PARAMETER")
    initialTab: DeviceControlTab = DeviceControlTab.Mirror
) {
    val pageScrollState = rememberScrollState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(pageScrollState)
                .padding(horizontal = UiTokens.PagePaddingCompact, vertical = UiTokens.ItemSpacing)
        ) {
            val stacked = maxWidth < 1180.dp
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (stacked) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DeviceMirrorScreen(
                            viewModel = mirrorViewModel,
                            selectedDevice = selectedDevice,
                            modifier = Modifier.fillMaxWidth(),
                            embedded = true,
                            showHeader = false,
                            collapseSettings = true,
                            showRuntimeControls = false,
                            useInternalScroll = false
                        )
                        KeyEventScreen(
                            viewModel = keyEventViewModel,
                            selectedDevice = selectedDevice,
                            deviceDisplayNames = deviceDisplayNames,
                            modifier = Modifier.fillMaxWidth(),
                            embedded = true,
                            showHeader = false,
                            useInternalScroll = false
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DeviceMirrorScreen(
                            viewModel = mirrorViewModel,
                            selectedDevice = selectedDevice,
                            modifier = Modifier.weight(0.9f),
                            embedded = true,
                            showHeader = false,
                            collapseSettings = true,
                            showRuntimeControls = false,
                            useInternalScroll = false
                        )
                        KeyEventScreen(
                            viewModel = keyEventViewModel,
                            selectedDevice = selectedDevice,
                            deviceDisplayNames = deviceDisplayNames,
                            modifier = Modifier.weight(1.25f),
                            embedded = true,
                            showHeader = false,
                            useInternalScroll = false
                        )
                    }
                }
            }
        }
    }
}
