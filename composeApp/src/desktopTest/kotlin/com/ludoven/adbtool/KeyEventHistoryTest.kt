package com.ludoven.adbtool

import com.ludoven.adbtool.viewmodel.KeyEventRecord
import com.ludoven.adbtool.viewmodel.updatedRecentKeyEvents
import kotlin.test.Test
import kotlin.test.assertEquals

class KeyEventHistoryTest {

    @Test
    fun keyEventRecordFormatsAdbCommand() {
        val record = KeyEventRecord(code = 3, name = "KEYCODE_HOME", sentAt = "10:24:31")

        assertEquals("adb shell input keyevent 3", record.adbCommand)
        assertEquals("KEYCODE_HOME (3)", record.displayText)
    }

    @Test
    fun updatedRecentKeyEventsKeepsNewestRecordsFirstAndCapsSize() {
        val existing = listOf(
            KeyEventRecord(code = 4, name = "KEYCODE_BACK", sentAt = "10:24:25"),
            KeyEventRecord(code = 26, name = "KEYCODE_POWER", sentAt = "10:24:19")
        )

        val updated = updatedRecentKeyEvents(
            current = existing,
            next = KeyEventRecord(code = 3, name = "KEYCODE_HOME", sentAt = "10:24:31"),
            maxSize = 2
        )

        assertEquals(
            listOf(
                KeyEventRecord(code = 3, name = "KEYCODE_HOME", sentAt = "10:24:31"),
                KeyEventRecord(code = 4, name = "KEYCODE_BACK", sentAt = "10:24:25")
            ),
            updated
        )
    }
}
