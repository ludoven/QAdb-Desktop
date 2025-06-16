@file:OptIn(ExperimentalMaterial3Api::class)

package com.ludoven.adbtool

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "QADB",
    ) {
        App()
    }
}