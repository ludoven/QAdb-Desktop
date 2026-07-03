package com.ludoven.adbtool.util

import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection

internal fun plainTextSelection(text: String): StringSelection =
    StringSelection(text)

internal fun clipboardPlainText(selection: StringSelection): String =
    selection.getTransferData(DataFlavor.stringFlavor) as String

fun copyPlainTextToClipboard(text: String) {
    runCatching {
        Toolkit.getDefaultToolkit()
            .systemClipboard
            .setContents(plainTextSelection(text), null)
    }
}
