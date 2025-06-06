package com.ludoven.adbtool.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

// 示例页面 Composable (需要你实现具体内容)
@Composable
fun SystemScreen() {
    Column {
        Text(text = "系统页面", style = MaterialTheme.typography.headlineMedium)
        // 这里放置你的设备列表、刷新按钮、断开连接等逻辑
        // 可以参考之前我给你的 AdbTool 示例
    }
}