package com.ludoven.adbtool.entity

import androidx.compose.ui.graphics.vector.ImageVector

data class AdbFunction(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)
