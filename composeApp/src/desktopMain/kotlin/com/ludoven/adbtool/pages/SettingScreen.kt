package com.ludoven.adbtool.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ludoven.adbtool.util.AdbPathManager
import com.ludoven.adbtool.util.AdbTool
import com.ludoven.adbtool.util.FileUtils

@Composable
fun SettingScreen() {
    var adbPath by remember { mutableStateOf(AdbPathManager.currentAdbPath ?: "未设置") }
    var showDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(
            text = "当前 ADB 路径",
            style = MaterialTheme.typography.titleMedium,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = adbPath,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = {
                    val newPath = FileUtils.selectFile("选择 ADB 可执行文件")
                    if (newPath != null) {
                        AdbPathManager.setAdbPath(newPath)
                        adbPath = newPath
                        showDialog = true
                    }
                }) {
                    Icon(Icons.Default.FolderOpen, contentDescription = "选择 ADB")
                }
            },
            label = { Text("ADB 路径") }
        )
    }

    if (showDialog) {
        TipDialog("ADB 路径设置成功：\n$adbPath") {
            showDialog = false
        }
    }
}
