package com.ludoven.adbtool.ui.mac

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.AlertDialog as M2AlertDialog
import androidx.compose.material.Button as M2Button
import androidx.compose.material.ButtonColors
import androidx.compose.material.ButtonDefaults as M2ButtonDefaults
import androidx.compose.material.Card as M2Card
import androidx.compose.material.Checkbox as M2Checkbox
import androidx.compose.material.CircularProgressIndicator as M2CircularProgressIndicator
import androidx.compose.material.Colors
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider as M2Divider
import androidx.compose.material.DropdownMenu as M2DropdownMenu
import androidx.compose.material.DropdownMenuItem as M2DropdownMenuItem
import androidx.compose.material.Icon as M2Icon
import androidx.compose.material.IconButton as M2IconButton
import androidx.compose.material.MaterialTheme as M2MaterialTheme
import androidx.compose.material.OutlinedButton as M2OutlinedButton
import androidx.compose.material.OutlinedTextField as M2OutlinedTextField
import androidx.compose.material.Scaffold as M2Scaffold
import androidx.compose.material.Snackbar as M2Snackbar
import androidx.compose.material.Surface as M2Surface
import androidx.compose.material.Switch as M2Switch
import androidx.compose.material.Text as M2Text
import androidx.compose.material.TextButton as M2TextButton
import androidx.compose.material.TextFieldColors
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.Typography
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
annotation class ExperimentalMaterial3Api

typealias Shapes = androidx.compose.material.Shapes

@Immutable
data class ColorScheme(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val error: Color,
    val onError: Color,
    val outline: Color,
    val outlineVariant: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val inverseSurface: Color,
    val inverseOnSurface: Color
)

fun lightColorScheme(
    primary: Color = Color(0xFF3A7AFE),
    onPrimary: Color = Color.White,
    primaryContainer: Color = primary.copy(alpha = 0.16f),
    onPrimaryContainer: Color = primary,
    secondary: Color = Color(0xFF34AADC),
    onSecondary: Color = Color.White,
    secondaryContainer: Color = secondary.copy(alpha = 0.18f),
    onSecondaryContainer: Color = Color(0xFF0D3E52),
    tertiary: Color = Color(0xFF5AC8FA),
    onTertiary: Color = Color.White,
    tertiaryContainer: Color = tertiary.copy(alpha = 0.16f),
    onTertiaryContainer: Color = Color(0xFF0D3E52),
    background: Color = Color(0xFFF5F5F7),
    onBackground: Color = Color(0xFF1D1D1F),
    surface: Color = Color.White,
    onSurface: Color = Color(0xFF1D1D1F),
    surfaceVariant: Color = Color(0xFFF2F2F7),
    onSurfaceVariant: Color = Color(0xFF636366),
    error: Color = Color(0xFFFF3B30),
    onError: Color = Color.White,
    outline: Color = Color(0xFFD1D1D6),
    outlineVariant: Color = Color(0xFFE5E5EA),
    errorContainer: Color = Color(0xFFFFE6E3),
    onErrorContainer: Color = Color(0xFF7D130F),
    inverseSurface: Color = Color(0xFF1D1D1F),
    inverseOnSurface: Color = Color(0xFFF5F5F7)
): ColorScheme = ColorScheme(
    primary,
    onPrimary,
    primaryContainer,
    onPrimaryContainer,
    secondary,
    onSecondary,
    secondaryContainer,
    onSecondaryContainer,
    tertiary,
    onTertiary,
    tertiaryContainer,
    onTertiaryContainer,
    background,
    onBackground,
    surface,
    onSurface,
    surfaceVariant,
    onSurfaceVariant,
    error,
    onError,
    outline,
    outlineVariant,
    errorContainer,
    onErrorContainer,
    inverseSurface,
    inverseOnSurface
)

fun darkColorScheme(
    primary: Color = Color(0xFF7EA6FF),
    onPrimary: Color = Color(0xFF0B1C4A),
    primaryContainer: Color = Color(0xFF18306F),
    onPrimaryContainer: Color = Color(0xFFDCE6FF),
    secondary: Color = Color(0xFF69D1F2),
    onSecondary: Color = Color(0xFF003747),
    secondaryContainer: Color = Color(0xFF12485D),
    onSecondaryContainer: Color = Color(0xFFCAF2FF),
    tertiary: Color = Color(0xFF82D8FF),
    onTertiary: Color = Color(0xFF003546),
    tertiaryContainer: Color = Color(0xFF12485D),
    onTertiaryContainer: Color = Color(0xFFD7F4FF),
    background: Color = Color(0xFF111113),
    onBackground: Color = Color(0xFFE6E6EA),
    surface: Color = Color(0xFF1B1B1F),
    onSurface: Color = Color(0xFFE6E6EA),
    surfaceVariant: Color = Color(0xFF2C2C31),
    onSurfaceVariant: Color = Color(0xFFB7B7BE),
    error: Color = Color(0xFFFF6A60),
    onError: Color = Color(0xFF5F0B08),
    outline: Color = Color(0xFF4D4D52),
    outlineVariant: Color = Color(0xFF3A3A3F),
    errorContainer: Color = Color(0xFF5F0B08),
    onErrorContainer: Color = Color(0xFFFFDAD6),
    inverseSurface: Color = Color(0xFFE6E6EA),
    inverseOnSurface: Color = Color(0xFF1B1B1F)
): ColorScheme = ColorScheme(
    primary,
    onPrimary,
    primaryContainer,
    onPrimaryContainer,
    secondary,
    onSecondary,
    secondaryContainer,
    onSecondaryContainer,
    tertiary,
    onTertiary,
    tertiaryContainer,
    onTertiaryContainer,
    background,
    onBackground,
    surface,
    onSurface,
    surfaceVariant,
    onSurfaceVariant,
    error,
    onError,
    outline,
    outlineVariant,
    errorContainer,
    onErrorContainer,
    inverseSurface,
    inverseOnSurface
)

private fun ColorScheme.isLightMode(): Boolean = background.red > 0.5f

private fun ColorScheme.asM2Colors(): Colors = if (isLightMode()) {
    lightColors(
        primary = primary,
        primaryVariant = primaryContainer,
        secondary = secondary,
        secondaryVariant = secondaryContainer,
        background = background,
        surface = surface,
        error = error,
        onPrimary = onPrimary,
        onSecondary = onSecondary,
        onBackground = onBackground,
        onSurface = onSurface,
        onError = onError
    )
} else {
    darkColors(
        primary = primary,
        primaryVariant = primaryContainer,
        secondary = secondary,
        secondaryVariant = secondaryContainer,
        background = background,
        surface = surface,
        error = error,
        onPrimary = onPrimary,
        onSecondary = onSecondary,
        onBackground = onBackground,
        onSurface = onSurface,
        onError = onError
    )
}

private val LocalColorScheme: ProvidableCompositionLocal<ColorScheme> = compositionLocalOf { lightColorScheme() }

object MaterialTheme {
    @Composable
    operator fun invoke(
        colorScheme: ColorScheme = lightColorScheme(),
        shapes: Shapes = Shapes(),
        typography: Typography = Typography(),
        content: @Composable () -> Unit
    ) {
        CompositionLocalProvider(LocalColorScheme provides colorScheme) {
            M2MaterialTheme(
                colors = colorScheme.asM2Colors(),
                shapes = shapes,
                typography = typography,
                content = content
            )
        }
    }

    val colorScheme: ColorScheme
        @Composable
        get() = LocalColorScheme.current

    val typography: Typography
        @Composable
        get() = M2MaterialTheme.typography

    val shapes: Shapes
        @Composable
        get() = M2MaterialTheme.shapes
}

val Typography.headlineMedium: TextStyle
    get() = h4
val Typography.headlineSmall: TextStyle
    get() = h5
val Typography.titleLarge: TextStyle
    get() = h6
val Typography.titleMedium: TextStyle
    get() = subtitle1
val Typography.titleSmall: TextStyle
    get() = subtitle2
val Typography.bodyLarge: TextStyle
    get() = body1
val Typography.bodyMedium: TextStyle
    get() = body2
val Typography.bodySmall: TextStyle
    get() = caption
val Typography.labelLarge: TextStyle
    get() = button
val Typography.labelMedium: TextStyle
    get() = caption.copy(fontWeight = FontWeight.Medium)
val Typography.labelSmall: TextStyle
    get() = overline

@Immutable
class CardColors(val containerColor: Color, val contentColor: Color)

@Immutable
class CardElevation(val defaultElevation: Dp)

object CardDefaults {
    @Composable
    fun cardColors(
        containerColor: Color = MaterialTheme.colorScheme.surface,
        contentColor: Color = MaterialTheme.colorScheme.onSurface
    ): CardColors = CardColors(containerColor, contentColor)

    fun cardElevation(defaultElevation: Dp = 0.5.dp): CardElevation = CardElevation(defaultElevation)
}

object OutlinedTextFieldDefaults {
    @Composable
    fun colors(
        focusedBorderColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
        unfocusedBorderColor: Color = MaterialTheme.colorScheme.outlineVariant,
        disabledBorderColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
        focusedContainerColor: Color = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        disabledContainerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ): TextFieldColors = TextFieldDefaults.outlinedTextFieldColors(
        focusedBorderColor = focusedBorderColor,
        unfocusedBorderColor = unfocusedBorderColor,
        disabledBorderColor = disabledBorderColor,
        backgroundColor = unfocusedContainerColor,
        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        cursorColor = focusedBorderColor,
        focusedLabelColor = focusedBorderColor,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

object ExposedDropdownMenuDefaults {
    @Composable
    fun TrailingIcon(expanded: Boolean) {
        Text(
            text = if (expanded) "▲" else "▼",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ExposedDropdownMenuBox(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier.clickable { onExpandedChange(!expanded) }) {
        content()
    }
}

fun Modifier.menuAnchor(): Modifier = this

@Composable
fun Scaffold(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.background,
    content: @Composable (PaddingValues) -> Unit
) {
    M2Scaffold(
        modifier = modifier,
        backgroundColor = containerColor,
        content = content
    )
}

@Composable
fun Surface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(0.dp),
    color: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    border: androidx.compose.foundation.BorderStroke? = null,
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    M2Surface(
        modifier = modifier,
        shape = shape,
        color = color,
        contentColor = contentColor,
        border = border,
        elevation = maxOf(tonalElevation, shadowElevation),
        content = content
    )
}

@Composable
fun Surface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(0.dp),
    color: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    border: androidx.compose.foundation.BorderStroke? = null,
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.clickable(enabled = enabled, onClick = onClick),
        shape = shape,
        color = color,
        contentColor = contentColor,
        border = border,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
        content = content
    )
}

@Composable
fun Card(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: androidx.compose.foundation.BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val resolvedBorder = border ?: androidx.compose.foundation.BorderStroke(
        width = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f)
    )
    M2Card(
        modifier = modifier,
        shape = shape,
        backgroundColor = colors.containerColor,
        contentColor = colors.contentColor,
        border = resolvedBorder,
        elevation = elevation.defaultElevation
    ) {
        Column(content = content)
    }
}

@Composable
fun AlertDialog(
    onDismissRequest: () -> Unit,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface
) {
    M2AlertDialog(
        onDismissRequest = onDismissRequest,
        title = title,
        text = text,
        shape = RoundedCornerShape(14.dp),
        backgroundColor = containerColor,
        buttons = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(containerColor)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f))
                dismissButton?.invoke()
                confirmButton()
            }
        }
    )
}

@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(10.dp),
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    interactionSource: MutableInteractionSource? = null,
    contentPadding: PaddingValues = M2ButtonDefaults.ContentPadding,
    content: @Composable () -> Unit
) {
    M2Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        interactionSource = interactionSource,
        contentPadding = contentPadding,
        elevation = M2ButtonDefaults.elevation(defaultElevation = 0.dp, pressedElevation = 0.dp, hoveredElevation = 0.dp)
    ) {
        content()
    }
}

@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(10.dp),
    border: androidx.compose.foundation.BorderStroke? = null,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    contentPadding: PaddingValues = M2ButtonDefaults.ContentPadding,
    content: @Composable () -> Unit
) {
    val resolvedBorder = border ?: androidx.compose.foundation.BorderStroke(
        width = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.9f)
    )
    M2OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        border = resolvedBorder,
        colors = colors,
        interactionSource = interactionSource,
        contentPadding = contentPadding
    ) {
        content()
    }
}

@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = M2ButtonDefaults.TextButtonContentPadding,
    content: @Composable () -> Unit
) {
    M2TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        contentPadding = contentPadding
    ) {
        content()
    }
}

@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    M2IconButton(onClick = onClick, modifier = modifier, enabled = enabled, content = content)
}

@Composable
fun OutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
    label: (@Composable () -> Unit)? = null,
    placeholder: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    shape: Shape = RoundedCornerShape(10.dp),
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors()
) {
    M2OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        singleLine = singleLine,
        maxLines = maxLines,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        shape = shape,
        colors = colors
    )
}

object ButtonDefaults {
    @Composable
    fun buttonColors(
        containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
        contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
        disabledContainerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        disabledContentColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlpha.disabled)
    ): ButtonColors = M2ButtonDefaults.buttonColors(
        backgroundColor = containerColor,
        contentColor = contentColor,
        disabledBackgroundColor = disabledContainerColor,
        disabledContentColor = disabledContentColor
    )

    @Composable
    fun outlinedButtonColors(
        containerColor: Color = Color.Transparent,
        contentColor: Color = MaterialTheme.colorScheme.onSurface,
        disabledContentColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlpha.disabled)
    ): ButtonColors = M2ButtonDefaults.outlinedButtonColors(
        backgroundColor = containerColor,
        contentColor = contentColor,
        disabledContentColor = disabledContentColor
    )
}

@Composable
fun DropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val decoratedModifier = modifier
        .background(MaterialTheme.colorScheme.surface)
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f), RoundedCornerShape(10.dp))
    M2DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = decoratedModifier,
        content = content
    )
}

@Composable
fun DropdownMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
) {
    M2DropdownMenuItem(
        onClick = onClick,
        enabled = enabled,
        contentPadding = contentPadding
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            leadingIcon?.invoke()
            if (leadingIcon != null) {
                Box(modifier = Modifier.padding(end = 8.dp))
            }
            Box(modifier = Modifier.weight(1f)) { text() }
            trailingIcon?.invoke()
        }
    }
}

@Composable
fun HorizontalDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = 1.dp,
    color: Color = MaterialTheme.colorScheme.outlineVariant
) {
    M2Divider(modifier = modifier, thickness = thickness, color = color)
}

@Composable
fun CircularProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = 4.dp
) {
    M2CircularProgressIndicator(modifier = modifier, color = color, strokeWidth = strokeWidth)
}

@Composable
fun Snackbar(
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    M2Snackbar(
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f), RoundedCornerShape(10.dp)),
        backgroundColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        action = action,
        content = { content() }
    )
}

@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    fontFamily: FontFamily? = null,
    fontSize: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    fontWeight: FontWeight? = null
) {
    M2Text(
        text = text,
        modifier = modifier,
        color = color,
        style = style,
        fontFamily = fontFamily,
        fontSize = fontSize,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = overflow,
        fontWeight = fontWeight
    )
}

@Composable
fun Text(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    fontFamily: FontFamily? = null,
    fontSize: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    fontWeight: FontWeight? = null
) {
    M2Text(
        text = text,
        modifier = modifier,
        color = color,
        style = style,
        fontFamily = fontFamily,
        fontSize = fontSize,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = overflow,
        fontWeight = fontWeight
    )
}

@Composable
fun Icon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    M2Icon(imageVector = imageVector, contentDescription = contentDescription, modifier = modifier, tint = tint)
}

@Composable
fun Checkbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier
) {
    M2Checkbox(checked = checked, onCheckedChange = onCheckedChange, modifier = modifier)
}

@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier
) {
    M2Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = modifier)
}
