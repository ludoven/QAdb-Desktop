package com.ludoven.adbtool.agent

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AgentModelCatalogTest {

    @Test
    fun `openai style data envelope is parsed`() {
        val body = """
            {
              "object": "list",
              "data": [
                {"id": "gpt-4o", "object": "model"},
                {"id": "gpt-4o-mini", "object": "model"},
                {"id": null}
              ]
            }
        """.trimIndent()

        assertEquals(listOf("gpt-4o", "gpt-4o-mini"), AgentModelCatalog.parseModelIds(body))
    }

    @Test
    fun `bare json array is parsed and deduplicated`() {
        val body = """[{"id":"b-model"},{"id":"a-model"},{"id":"a-model"}]"""

        assertEquals(listOf("a-model", "b-model"), AgentModelCatalog.parseModelIds(body))
    }

    @Test
    fun `malformed payload throws a descriptive error`() {
        var thrown: Throwable? = null
        try {
            AgentModelCatalog.parseModelIds("<html>502 Bad Gateway</html>")
        } catch (error: Throwable) {
            thrown = error
        }
        assertTrue(thrown?.message?.contains("Malformed model catalog response") == true)
    }
}
