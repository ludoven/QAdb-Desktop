package com.ludoven.adbtool.agent

import com.ludoven.adbtool.util.AdbProcessTimeoutContext
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AgentOperationDeadlineTest {
    @Test
    fun `operation deadline is propagated to adb process waits`() = runBlocking {
        val requestedProcessTimeoutMs = 30_000L
        val deadline = AgentOperationDeadline(durationMillis = 5_000L)

        deadline.runWithin {
            val effectiveTimeoutMs = AdbProcessTimeoutContext.clampTimeoutMillis(
                requestedProcessTimeoutMs
            )
            assertTrue(effectiveTimeoutMs in 1L..5_000L)
        }

        assertEquals(
            requestedProcessTimeoutMs,
            AdbProcessTimeoutContext.clampTimeoutMillis(requestedProcessTimeoutMs)
        )
    }
}
