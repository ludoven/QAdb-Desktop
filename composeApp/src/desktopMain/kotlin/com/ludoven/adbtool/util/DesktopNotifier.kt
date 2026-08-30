package com.ludoven.adbtool.util

import java.awt.EventQueue
import java.awt.SystemTray
import java.awt.TrayIcon

/**
 * Shows a system tray balloon notification. It reuses the tray icon registered by the
 * application window, so it silently no-ops when the tray is unavailable (for example
 * when minimize-to-tray is disabled and no icon was installed).
 */
object DesktopNotifier {
    fun notify(caption: String, message: String, isError: Boolean = false) {
        runCatching {
            if (!SystemTray.isSupported()) return
            val icon = SystemTray.getSystemTray().trayIcons.firstOrNull() ?: return
            val messageType = if (isError) TrayIcon.MessageType.ERROR else TrayIcon.MessageType.INFO
            EventQueue.invokeLater {
                runCatching { icon.displayMessage(caption, message, messageType) }
            }
        }
    }
}
