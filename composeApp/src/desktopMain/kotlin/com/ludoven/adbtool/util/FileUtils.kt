package com.ludoven.adbtool.util

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openDirectoryPicker
import io.github.vinceglb.filekit.dialogs.openFilePicker
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

object FileUtils {
    suspend fun selectApkFile(): String? {
        val file = FileKit.openFilePicker(type = FileKitType.File(listOf("apk")))
        return file?.absolutePath()
    }


    suspend fun selectFolder(): String? {
        val openDirectoryPicker = FileKit.openDirectoryPicker()
        return openDirectoryPicker?.absolutePath()
    }


    suspend fun selectFile(): String? {
        val dialog = FileDialog(null as Frame?, "Select ADB", FileDialog.LOAD)
        dialog.isVisible = true
        val dir = dialog.directory
        val filename = dialog.file
        return if (dir != null && filename != null) File(dir, filename).absolutePath else null
    }
}
