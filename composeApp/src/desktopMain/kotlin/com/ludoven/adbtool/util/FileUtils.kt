package com.ludoven.adbtool.util


import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import java.awt.Frame // 虽然 JFileChooser 不强制要求 Frame，但为了保持与 AWT 的一致性，你可以选择提供

object FileUtils {
    fun selectApkFile(): String? {
        val fileChooser = JFileChooser().apply {
            // 设置初始目录
            currentDirectory = File(System.getProperty("user.home"))

            // 设置文件过滤器
            // FileNameExtensionFilter 更强大，它会创建一个用户友好的描述，并严格过滤文件
            val apkFilter = FileNameExtensionFilter("Android APK 文件 (*.apk)", "apk")
            addChoosableFileFilter(apkFilter) // 添加过滤器
            fileFilter = apkFilter // 将这个过滤器设置为默认选中的过滤器

            // 禁用“所有文件”选项（可选，但可以更严格地限制用户选择）
            // fileChooser.setAcceptAllFileFilterUsed(false)
        }

        // 显示“打开”文件对话框
        val result = fileChooser.showOpenDialog(null) // null 表示对话框没有父组件，通常居中显示

        if (result == JFileChooser.APPROVE_OPTION) {
            val selectedFile = fileChooser.selectedFile
            // 额外检查确保是 .apk 文件，尽管过滤器应该已经生效
            return if (selectedFile != null && selectedFile.extension.lowercase() == "apk") {
                selectedFile.absolutePath
            } else {
                null
            }
        }
        return null // 用户取消选择
    }
}
