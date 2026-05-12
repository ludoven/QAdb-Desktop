package com.ludoven.adbtool.pages

import androidx.compose.runtime.Composable
import com.ludoven.adbtool.viewmodel.TerminalViewModel
import com.ludoven.adbtool.ui.terminal.TerminalScreen as TerminalConsole

@Composable
fun TerminalScreen(
    viewModel: TerminalViewModel,
    selectedDevice: String?,
    devices: List<String>,
    deviceDisplayNames: Map<String, String>,
    onSelectDevice: (String) -> Unit,
    onRefreshDevices: () -> Unit
) {
    TerminalConsole(
        viewModel = viewModel,
        selectedDevice = selectedDevice,
        devices = devices,
        deviceDisplayNames = deviceDisplayNames,
        onSelectDevice = onSelectDevice,
        onRefreshDevices = onRefreshDevices
    )
}
