@file:OptIn(ExperimentalMaterial3Api::class)

package com.ludoven.adbtool

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    val windowState = rememberWindowState(
        width = 960.dp,
        height = 640.dp,
        position = WindowPosition(Alignment.Center) // 推荐居中启动
    )
    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "QADB",
    ) {
        App()
    }
}