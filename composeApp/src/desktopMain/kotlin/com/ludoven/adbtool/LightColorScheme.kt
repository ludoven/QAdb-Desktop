package com.ludoven.adbtool

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val LightColorScheme: ColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = PrimaryLight.copy(alpha = 0.18f),
    onPrimaryContainer = PrimaryDark,

    secondary = SecondaryTeal,
    onSecondary = Color.White,
    secondaryContainer = SecondaryCyan.copy(alpha = 0.15f),
    onSecondaryContainer = Color(0xFF004D40),

    tertiary = InfoBlue,
    onTertiary = Color.White,
    tertiaryContainer = InfoBlue.copy(alpha = 0.14f),
    onTertiaryContainer = Color(0xFF00344B),

    background = BackgroundLight,
    onBackground = TextPrimaryLight,

    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,

    error = ErrorRed,
    onError = Color.White,

    outline = OutlineLight,
    outlineVariant = OutlineLight.copy(alpha = 0.65f)
)
