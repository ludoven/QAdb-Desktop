package com.ludoven.adbtool

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AiAgentNavigationTest {
    @Test
    fun `primary shortcuts start with device home then AI`() {
        assertEquals("home", PRIMARY_SHORTCUT_ROUTES[1])
        assertEquals("ai", PRIMARY_SHORTCUT_ROUTES[2])
        assertEquals("common", PRIMARY_SHORTCUT_ROUTES[3])
        assertEquals("ai", primaryShortcutRoute(2, agentFeatureEnabled = true))
        assertNull(primaryShortcutRoute(2, agentFeatureEnabled = false))
    }

    @Test
    fun `device home and AI routes remain primary destinations`() {
        assertEquals("home", normalizeRouteForPrimaryNavigation("home"))
        assertEquals("ai", normalizeRouteForPrimaryNavigation("ai"))
        assertEquals("device-control", normalizeRouteForPrimaryNavigation("mirror"))
    }
}
