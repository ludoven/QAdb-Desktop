package com.ludoven.adbtool

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.ColorScheme

// 定义浅色主题的配色方案
val LightColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF1976D2),       // 主色，稍深的蓝色，科技感强
    onPrimary = Color.White,           // 主色上的文字颜色，白色对比强

    secondary = Color(0xFF00BCD4),     // 次主色，清爽的青绿色，用于强调按钮等
    onSecondary = Color.White,         // 次主色上的文字颜色

    background = Color(0xFFF5F5F5),    // 整体背景，浅灰色，看起来更现代
    onBackground = Color(0xFF212121),  // 背景上的文字颜色，深灰

    surface = Color(0xFFFFFFFF),       // 卡片、弹窗等组件的背景色，纯白
    onSurface = Color(0xFF121212),     // 表面上的文字颜色

    error = Color(0xFFD32F2F),         // 错误色，醒目的红色
    onError = Color.White,             // 错误提示上的文字颜色

    outline = Color(0xFFBDBDBD)        // 边框、分割线颜色，淡灰色
)
