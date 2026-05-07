package com.ludoven.adbtool

import com.ludoven.adbtool.ui.mac.*

import androidx.compose.foundation.isSystemInDarkTheme
import com.ludoven.adbtool.ui.mac.MaterialTheme
import com.ludoven.adbtool.ui.mac.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape

private val AppShapes = Shapes(
    small = RoundedCornerShape(UiTokens.RadiusSmall),
    medium = RoundedCornerShape(UiTokens.RadiusMedium),
    large = RoundedCornerShape(UiTokens.RadiusLarge)
)

@Composable
fun AdbToolTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (useDarkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = AppShapes,
        content = content
    )
}
