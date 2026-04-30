package com.ludoven.adbtool

import com.ludoven.adbtool.viewmodel.KeyEventRecord
import kotlin.test.Test
import kotlin.test.assertEquals

class KeyEventHistoryTest {

    @Test
    fun keyEventRecordFormatsAdbCommand() {
        val record = KeyEventRecord(code = 3, name = "KEYCODE_HOME", sentAt = "10:24:31")

        assertEquals("adb shell input keyevent 3", record.adbCommand)
        assertEquals("KEYCODE_HOME (3)", record.displayText)
    }
}
