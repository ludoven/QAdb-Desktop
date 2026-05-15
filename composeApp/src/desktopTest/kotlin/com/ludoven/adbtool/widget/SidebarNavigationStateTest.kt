package com.ludoven.adbtool.widget

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SidebarNavigationStateTest {

    @Test
    fun tool_routes_are_recognized() {
        assertTrue(SidebarNavigation.isToolRoute("terminal"))
        assertTrue(SidebarNavigation.isToolRoute("process"))
        assertFalse(SidebarNavigation.isToolRoute("keyevent"))
        assertFalse(SidebarNavigation.isToolRoute("filebrowser"))
        assertFalse(SidebarNavigation.isToolRoute("home"))
    }

    @Test
    fun tool_routes_map_to_tools_primary_entry() {
        assertEquals("tools", SidebarNavigation.resolvedPrimaryRoute("terminal"))
        assertEquals("tools", SidebarNavigation.resolvedPrimaryRoute("log"))
        assertEquals("home", SidebarNavigation.resolvedPrimaryRoute("home"))
    }

    @Test
    fun tools_group_stays_collapsed_for_tool_routes_until_user_expands_it() {
        assertFalse(SidebarNavigation.shouldExpandTools("terminal", manuallyExpanded = false))
        assertFalse(SidebarNavigation.shouldExpandTools("log", manuallyExpanded = false))
        assertTrue(SidebarNavigation.shouldExpandTools("terminal", manuallyExpanded = true))
    }

    @Test
    fun tools_group_respects_manual_toggle_for_non_tool_routes() {
        assertTrue(SidebarNavigation.shouldExpandTools("home", manuallyExpanded = true))
        assertFalse(SidebarNavigation.shouldExpandTools("home", manuallyExpanded = false))
    }

    @Test
    fun tools_primary_route_is_stable_for_group_children_only() {
        val routes = listOf("terminal", "log", "process")
        routes.forEach { route ->
            assertEquals(SidebarNavigation.ToolsRoute, SidebarNavigation.resolvedPrimaryRoute(route))
        }
    }

    @Test
    fun promoted_primary_pages_keep_their_own_primary_route() {
        assertEquals("keyevent", SidebarNavigation.resolvedPrimaryRoute("keyevent"))
        assertEquals("filebrowser", SidebarNavigation.resolvedPrimaryRoute("filebrowser"))
    }

    @Test
    fun selected_tool_page_keeps_tools_primary_selected_without_auto_expand() {
        val selectedRoute = "terminal"
        val expanded = SidebarNavigation.shouldExpandTools(selectedRoute, manuallyExpanded = false)

        assertFalse(expanded)
        assertEquals(SidebarNavigation.ToolsRoute, SidebarNavigation.resolvedPrimaryRoute(selectedRoute))
    }
}
