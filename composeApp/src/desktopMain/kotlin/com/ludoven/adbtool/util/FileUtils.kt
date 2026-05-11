package com.ludoven.adbtool.util

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openDirectoryPicker
import io.github.vinceglb.filekit.dialogs.openFilePicker
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileNameExtensionFilter

object FileUtils {
    private val isWindows: Boolean
        get() = System.getProperty("os.name").lowercase().contains("windows")

    suspend fun selectApkFile(): String? {
        if (isWindows) {
            return runOnEdt {
                val chooser = JFileChooser().apply {
                    fileSelectionMode = JFileChooser.FILES_ONLY
                    isMultiSelectionEnabled = false
                    fileFilter = FileNameExtensionFilter("APK Files (*.apk)", "apk")
                }
                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    chooser.selectedFile?.absolutePath
                } else {
                    null
                }
            }
        }

        val file = FileKit.openFilePicker(type = FileKitType.File(listOf("apk")))
        return file?.absolutePath()
    }


    suspend fun selectFolder(): String? {
        if (isWindows) {
            return runOnEdt {
                val chooser = JFileChooser().apply {
                    fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                    isMultiSelectionEnabled = false
                    isAcceptAllFileFilterUsed = false
                }
                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    chooser.selectedFile?.absolutePath
                } else {
                    null
                }
            }
        }

        val openDirectoryPicker = FileKit.openDirectoryPicker()
        return openDirectoryPicker?.absolutePath()
    }


    suspend fun selectFile(): String? {
        if (isWindows) {
            return runOnEdt {
                val chooser = JFileChooser().apply {
                    fileSelectionMode = JFileChooser.FILES_ONLY
                    isMultiSelectionEnabled = false
                }
                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    chooser.selectedFile?.absolutePath
                } else {
                    null
                }
            }
        }

        val openFilePicker = FileKit.openFilePicker()
        return openFilePicker?.absolutePath()
    }

    private fun <T> runOnEdt(action: () -> T): T {
        if (SwingUtilities.isEventDispatchThread()) {
            return action()
        }

        var result: Result<T>? = null
        SwingUtilities.invokeAndWait {
            result = runCatching(action)
        }
        return result!!.getOrThrow()
    }
}
