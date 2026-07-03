package com.ludoven.adbtool

import com.ludoven.adbtool.domain.terminal.TerminalController
import com.ludoven.adbtool.domain.terminal.TerminalLineType
import kotlin.test.Test
import kotlin.test.assertEquals

class TerminalControllerPerformanceTest {

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
