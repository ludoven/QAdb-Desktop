package com.ludoven.adbtool

import com.ludoven.adbtool.ui.mac.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** Semantic colors shared by every QADB page and component. */
object QadbColors {
    val primary: Color
        @Composable get() = MaterialTheme.colorScheme.primary
    val onPrimary: Color
        @Composable get() = MaterialTheme.colorScheme.onPrimary
    val primaryContainer: Color
        @Composable get() = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer: Color
        @Composable get() = MaterialTheme.colorScheme.onPrimaryContainer

    val background: Color
        @Composable get() = MaterialTheme.colorScheme.background
    val surface: Color
        @Composable get() = MaterialTheme.colorScheme.surface
    val surfaceVariant: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceVariant
    val surfaceSelected: Color
        @Composable get() = MaterialTheme.colorScheme.primaryContainer
    val surfaceHover: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f)
    val successSurface: Color
        @Composable get() = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
    val warningSurface: Color
        @Composable get() = warning.copy(alpha = 0.10f)
    val errorSurface: Color
        @Composable get() = MaterialTheme.colorScheme.error.copy(alpha = 0.08f)

    val textPrimary: Color
        @Composable get() = MaterialTheme.colorScheme.onSurface
    val textSecondary: Color
        @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
    val textTertiary: Color
        @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
    val textDisabled: Color
        @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)

    val border: Color
        @Composable get() = MaterialTheme.colorScheme.outlineVariant
    val borderStrong: Color
        @Composable get() = MaterialTheme.colorScheme.outline
    val selectedBorder: Color
        @Composable get() = MaterialTheme.colorScheme.primary.copy(alpha = 0.34f)
    val divider: Color
        @Composable get() = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)

    val success: Color
        @Composable get() = MaterialTheme.colorScheme.secondary
    val info: Color
        @Composable get() = MaterialTheme.colorScheme.tertiary
    val danger: Color
        @Composable get() = MaterialTheme.colorScheme.error
    val warning: Color
        @Composable get() = if (isDarkTheme()) Color(0xFFFF9F0A) else Color(0xFFD97706)
    val purple: Color
        @Composable get() = if (isDarkTheme()) Color(0xFFBF5AF2) else Color(0xFF7C3AED)
    val teal: Color
        @Composable get() = if (isDarkTheme()) Color(0xFF64D2FF) else Color(0xFF0F766E)
    val cyan: Color
        @Composable get() = MaterialTheme.colorScheme.tertiary

    val disabledSurface: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)

    @Composable
    private fun isDarkTheme(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f
}

/** Fixed accents for charts, key groups, and terminal syntax where hue identity is semantic. */
object QadbPalette {
    val Blue = Color(0xFF2563EB)
    val Purple = Color(0xFF5645D4)
    val Orange = Color(0xFFDD5B00)
    val Green = Color(0xFF1AAE39)
    val Slate = Color(0xFF5D5B54)

    val TerminalBackground = Color(0xFF111827)
    val TerminalBorder = Color(0xFF1F2937)
    val TerminalText = Color(0xFFE5E7EB)
    val TerminalBlue = Color(0xFF93C5FD)
    val TerminalSuccess = Color(0xFF86EFAC)
    val TerminalError = Color(0xFFFCA5A5)
    val TerminalWarning = Color(0xFFFDE68A)
}
