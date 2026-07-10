package com.ludoven.adbtool

import com.ludoven.adbtool.domain.terminal.TerminalController
import com.ludoven.adbtool.domain.terminal.TerminalLineType
import kotlin.test.Test
import kotlin.test.assertEquals

class TerminalControllerPerformanceTest {

    @Test
    fun `terminal session should default to empty search and follow output`() {
        val session = TerminalController().session.value

        assertEquals("", session.searchQuery)
        assertEquals(true, session.followOutput)
    }

    @Test
    fun `terminal should bind the selected available device`() {
        val controller = TerminalController()

        controller.bindDeviceState(
            devices = listOf("device-a", "device-b"),
            displayNames = emptyMap(),
            selectedDevice = "device-b"
        )

        assertEquals("device-b", controller.session.value.deviceId)
    }

    @Test
    fun `terminal should fall back when the bound device disappears`() {
        val controller = TerminalController()
        controller.bindDeviceState(listOf("device-a", "device-b"), emptyMap(), "device-b")

        controller.bindDeviceState(listOf("device-a"), emptyMap(), "device-b")

        assertEquals("device-a", controller.session.value.deviceId)
    }

    @Test
    fun `terminal search updates should preserve the selected device`() {
        val controller = TerminalController()
        controller.bindDeviceState(listOf("device-a"), emptyMap(), "device-a")

        controller.updateSearchQuery("fatal")

        assertEquals("device-a", controller.session.value.deviceId)
        assertEquals("fatal", controller.session.value.searchQuery)
    }

    @Test
    fun `terminal output should publish in batches instead of every line`() {
        val controller = TerminalController()

        repeat(39) { index ->
            controller.appendLineForTest("line-$index")
        }

        assertEquals(0, controller.session.value.lines.size)

        controller.appendLineForTest("line-39")

        assertEquals(40, controller.session.value.lines.size)
        assertEquals("line-0", controller.session.value.lines.first().text)
        assertEquals("line-39", controller.session.value.lines.last().text)
    }

    @Test
    fun `terminal output should keep only latest max lines after batched publishes`() {
        val controller = TerminalController()

        repeat(5_040) { index ->
            controller.appendLineForTest("line-$index")
        }

        val lines = controller.session.value.lines
        assertEquals(5_000, lines.size)
        assertEquals("line-40", lines.first().text)
        assertEquals("line-5039", lines.last().text)
    }

    @Test
    fun `terminal ui state should update without creating another session`() {
        val controller = TerminalController()
        val sessionId = controller.session.value.id

        controller.updateSearchQuery("error")
        controller.setFollowOutput(false)

        assertEquals(sessionId, controller.session.value.id)
        assertEquals("error", controller.session.value.searchQuery)
        assertEquals(false, controller.session.value.followOutput)
    }

    private fun TerminalController.appendLineForTest(text: String) {
        val method = TerminalController::class.java.getDeclaredMethod(
            "appendLine",
            TerminalLineType::class.java,
            String::class.java
        )
        method.isAccessible = true
        method.invoke(this, TerminalLineType.OUTPUT, text)
    }
}
