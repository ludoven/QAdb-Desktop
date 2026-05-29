package com.ludoven.adbtool.widget

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import com.ludoven.adbtool.TabItem
import kotlin.test.Test
import kotlin.test.assertEquals

class SidebarNavigationStateTest {
    private val groups = listOf(
        SidebarGroup(
            id = "home",
            label = "首页",
            icon = Icons.Default.Home,
            defaultRoute = "home",
            items = listOf(TabItem("首页", Icons.Default.Home, "home")),
            collapsible = false
        ),
        SidebarGroup(
            id = "terminal",
            label = "终端",
            icon = Icons.Default.Home,
            defaultRoute = "terminal",
            items = listOf(TabItem("终端", Icons.Default.Home, "terminal")),
            collapsible = false
        ),
        SidebarGroup(
            id = "diagnostics",
            label = "诊断分析",
            icon = Icons.Default.Home,
            defaultRoute = "log",
            items = listOf(
                TabItem("日志", Icons.Default.Home, "log"),
                TabItem("性能", Icons.Default.Home, "performance"),
                TabItem("进程", Icons.Default.Home, "process")
            )
        )
    )

    @Test
    fun `standalone route should not expand a secondary group`() {
        assertEquals(
            emptySet(),
            visibleSidebarGroupIds(groups, selectedRoute = "terminal", expandedGroupId = null)
        )
    }

    @Test
    fun `user expanded collapsible group should be visible from standalone route`() {
        assertEquals(
            setOf("diagnostics"),
            visibleSidebarGroupIds(groups, selectedRoute = "home", expandedGroupId = "diagnostics")
        )
    }

    @Test
    fun `unknown expanded group should be ignored`() {
        assertEquals(
            emptySet(),
            visibleSidebarGroupIds(groups, selectedRoute = "home", expandedGroupId = "missing")
        )
    }

    @Test
    fun `current collapsible route group should remain visible`() {
        assertEquals(
            setOf("diagnostics"),
            visibleSidebarGroupIds(groups, selectedRoute = "performance", expandedGroupId = null)
        )
    }

    @Test
    fun `process route should keep diagnostics group visible`() {
        assertEquals(
            setOf("diagnostics"),
            visibleSidebarGroupIds(groups, selectedRoute = "process", expandedGroupId = null)
        )
    }
}
