package com.ludoven.adbtool

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = PrimaryLight,
    onPrimary = Color.Black,
    primaryContainer = PrimaryDark.copy(alpha = 0.60f),
    onPrimaryContainer = Color.White,

    secondary = SecondaryTeal,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF004D40),
    onSecondaryContainer = SecondaryCyan,

    tertiary = InfoBlue,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF103548),
    onTertiaryContainer = Color(0xFFBEE9FF),

    background = BackgroundDark,
    onBackground = TextPrimaryDark,

    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceDarkVariant,
    onSurfaceVariant = TextSecondaryDark,

    error = ErrorRed,
    onError = Color.Black,

    outline = OutlineDark,
    outlineVariant = OutlineDark.copy(alpha = 0.75f)
)
