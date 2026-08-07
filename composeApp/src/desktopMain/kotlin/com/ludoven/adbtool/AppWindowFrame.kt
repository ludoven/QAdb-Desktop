package com.ludoven.adbtool

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import javax.swing.JFrame

private val MacTitleBarHeight = 24.dp
private val WindowsTitleBarHeight = 40.dp
private val WindowsControlWidth = 46.dp

@Composable
fun FrameWindowScope.AppWindowFrame(
    isMacOs: Boolean,
    isWindows: Boolean,
    useDarkTheme: Boolean,
    windowState: WindowState,
    onClose: () -> Unit,
    content: @Composable () -> Unit
) {
    if (!isMacOs && !isWindows) {
        content()
        return
    }

    val background = if (useDarkTheme) DarkColorScheme.background else LightColorScheme.background
    val foreground = if (useDarkTheme) DarkColorScheme.onBackground else LightColorScheme.onBackground

    if (isMacOs) {
        ConfigureMacUnifiedTitleBar()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        if (isMacOs) {
            WindowDraggableArea(
                modifier = Modifier
                    .height(MacTitleBarHeight)
                    .fillMaxWidth()
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .width(UiTokens.SidebarWidth)
                            .fillMaxHeight()
                            .background(if (useDarkTheme) background else Color(0xFFF9F9FA))
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(background)
                    )
                }
            }
        } else {
            WindowsTitleBar(
                background = background,
                foreground = foreground,
                windowState = windowState,
                onClose = onClose
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            content()
        }
    }
}

@Composable
private fun FrameWindowScope.ConfigureMacUnifiedTitleBar() {
    DisposableEffect(window) {
        val rootPane = (window as? JFrame)?.rootPane
        rootPane?.putClientProperty("apple.awt.fullWindowContent", true)
        rootPane?.putClientProperty("apple.awt.transparentTitleBar", true)
        rootPane?.putClientProperty("apple.awt.windowTitleVisible", false)

        onDispose {
            rootPane?.putClientProperty("apple.awt.fullWindowContent", null)
            rootPane?.putClientProperty("apple.awt.transparentTitleBar", null)
            rootPane?.putClientProperty("apple.awt.windowTitleVisible", null)
        }
    }
}

@Composable
private fun FrameWindowScope.WindowsTitleBar(
    background: Color,
    foreground: Color,
    windowState: WindowState,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .height(WindowsTitleBarHeight)
            .background(background),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WindowDraggableArea(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                    onDoubleClick = {
                        windowState.placement =
                            if (windowState.placement == WindowPlacement.Maximized) {
                                WindowPlacement.Floating
                            } else {
                                WindowPlacement.Maximized
                            }
                    }
                )
        )

        WindowControlButton(
            type = WindowControlType.Minimize,
            background = background,
            foreground = foreground,
            contentDescription = "最小化窗口",
            onClick = { windowState.isMinimized = true }
        )
        WindowControlButton(
            type = if (windowState.placement == WindowPlacement.Maximized) {
                WindowControlType.Restore
            } else {
                WindowControlType.Maximize
            },
            background = background,
            foreground = foreground,
            contentDescription = if (windowState.placement == WindowPlacement.Maximized) {
                "还原窗口"
            } else {
                "最大化窗口"
            },
            onClick = {
                windowState.placement =
                    if (windowState.placement == WindowPlacement.Maximized) {
                        WindowPlacement.Floating
                    } else {
                        WindowPlacement.Maximized
                    }
            }
        )
        WindowControlButton(
            type = WindowControlType.Close,
            background = background,
            foreground = foreground,
            contentDescription = "关闭窗口",
            onClick = onClose
        )
    }
}

@Composable
private fun WindowControlButton(
    type: WindowControlType,
    background: Color,
    foreground: Color,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(WindowsControlWidth)
            .fillMaxHeight()
            .semantics {
                this.contentDescription = contentDescription
                role = Role.Button
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .padding(12.dp)
                .size(16.dp)
        ) {
            val strokeWidth = 1.25.dp.toPx()
            when (type) {
                WindowControlType.Minimize -> {
                    drawLine(
                        color = foreground,
                        start = Offset(size.width * 0.2f, size.height * 0.5f),
                        end = Offset(size.width * 0.8f, size.height * 0.5f),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Square
                    )
                }

                WindowControlType.Maximize -> {
                    drawRect(
                        color = foreground,
                        topLeft = Offset(size.width * 0.23f, size.height * 0.23f),
                        size = size.copy(width = size.width * 0.54f, height = size.height * 0.54f),
                        style = Stroke(width = strokeWidth)
                    )
                }

                WindowControlType.Restore -> {
                    drawRect(
                        color = foreground,
                        topLeft = Offset(size.width * 0.32f, size.height * 0.2f),
                        size = size.copy(width = size.width * 0.48f, height = size.height * 0.48f),
                        style = Stroke(width = strokeWidth)
                    )
                    drawRect(
                        color = background,
                        topLeft = Offset(size.width * 0.18f, size.height * 0.34f),
                        size = size.copy(width = size.width * 0.48f, height = size.height * 0.48f)
                    )
                    drawRect(
                        color = foreground,
                        topLeft = Offset(size.width * 0.18f, size.height * 0.34f),
                        size = size.copy(width = size.width * 0.48f, height = size.height * 0.48f),
                        style = Stroke(width = strokeWidth)
                    )
                }

                WindowControlType.Close -> {
                    drawLine(
                        color = foreground,
                        start = Offset(size.width * 0.25f, size.height * 0.25f),
                        end = Offset(size.width * 0.75f, size.height * 0.75f),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Square
                    )
                    drawLine(
                        color = foreground,
                        start = Offset(size.width * 0.75f, size.height * 0.25f),
                        end = Offset(size.width * 0.25f, size.height * 0.75f),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Square
                    )
                }
            }
        }
    }
}

private enum class WindowControlType {
    Minimize,
    Maximize,
    Restore,
    Close
}
