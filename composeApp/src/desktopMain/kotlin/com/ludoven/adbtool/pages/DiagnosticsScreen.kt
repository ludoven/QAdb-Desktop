package com.ludoven.adbtool.pages

import com.ludoven.adbtool.ui.mac.*

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ludoven.adbtool.UiTokens
import com.ludoven.adbtool.ui.icons.IconParkIcons
import com.ludoven.adbtool.util.l10n
import com.ludoven.adbtool.viewmodel.LogViewModel
import com.ludoven.adbtool.widget.ProductTab
import com.ludoven.adbtool.widget.ProductTabBar

enum class DiagnosticsTab {
    Logs,
    Processes
}

@Composable
fun DiagnosticsScreen(
    logViewModel: LogViewModel,
    selectedDevice: String?,
    initialTab: DiagnosticsTab = DiagnosticsTab.Logs
) {
    var selectedTab by rememberSaveable(initialTab) { mutableStateOf(initialTab) }
    val tabs = listOf(
        ProductTab(DiagnosticsTab.Logs, l10n("日志", "Logs"), IconParkIcons.List),
        ProductTab(DiagnosticsTab.Processes, l10n("进程", "Processes"), IconParkIcons.ChartLine)
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = 1200.dp)
                    .fillMaxSize()
            ) {
                ProductTabBar(
                    tabs = tabs,
                    selected = selectedTab,
                    onSelected = { selectedTab = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = UiTokens.PagePadding,
                            vertical = UiTokens.SpaceLarge
                        )
                )
                Box(modifier = Modifier.fillMaxSize()) {
                    when (selectedTab) {
                        DiagnosticsTab.Logs -> LogScreen(
                            viewModel = logViewModel,
                            selectedDevice = selectedDevice
                        )
                        DiagnosticsTab.Processes -> ProcessScreen(selectedDevice = selectedDevice)
                    }
                }
            }
        }
    }
}
