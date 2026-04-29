package com.ludoven.adbtool

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
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
