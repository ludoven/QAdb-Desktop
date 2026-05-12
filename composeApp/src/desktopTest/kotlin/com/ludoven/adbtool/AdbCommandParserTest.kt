package com.ludoven.adbtool

import com.ludoven.adbtool.domain.adb.AdbCommandParser
import com.ludoven.adbtool.domain.adb.BuiltInCommandType
import com.ludoven.adbtool.domain.adb.ParsedCommand
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AdbCommandParserTest {

    private val parser = AdbCommandParser()

    @Test
    fun `clear without args is built-in clear`() {
        val result = parser.parse("clear", deviceId = "emulator-5554")
        val builtIn = assertIs<ParsedCommand.BuiltIn>(result)
        assertEquals(BuiltInCommandType.CLEAR, builtIn.type)
    }

    @Test
    fun `clear with package should map to pm clear`() {
        val result = parser.parse("clear com.example.app", deviceId = "emulator-5554")
        val external = assertIs<ParsedCommand.External>(result)
        assertEquals(
            listOf("-s", "emulator-5554", "shell", "pm", "clear", "com.example.app"),
            external.command.args
        )
    }

    @Test
    fun `shell without selected device should return invalid`() {
        val result = parser.parse("shell", deviceId = null)
        assertIs<ParsedCommand.Invalid>(result)
    }

    @Test
    fun `raw adb command keeps original args`() {
        val result = parser.parse("adb devices", deviceId = "emulator-5554")
        val external = assertIs<ParsedCommand.External>(result)
        assertEquals(listOf("devices"), external.command.args)
    }
}
