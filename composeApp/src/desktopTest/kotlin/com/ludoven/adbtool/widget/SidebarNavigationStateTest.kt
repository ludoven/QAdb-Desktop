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
            id = "device-control",
            label = "设备控制",
            icon = Icons.Default.Home,
            defaultRoute = "device-control",
            items = listOf(TabItem("设备控制", Icons.Default.Home, "device-control")),
            collapsible = false
        ),
        SidebarGroup(
            id = "app",
            label = "应用",
            icon = Icons.Default.Home,
            defaultRoute = "app",
            items = listOf(TabItem("应用", Icons.Default.Home, "app")),
            collapsible = false
        ),
        SidebarGroup(
            id = "filebrowser",
            label = "文件",
            icon = Icons.Default.Home,
            defaultRoute = "filebrowser",
            items = listOf(TabItem("文件", Icons.Default.Home, "filebrowser")),
            collapsible = false
        ),
        SidebarGroup(
            id = "diagnostics",
            label = "诊断",
            icon = Icons.Default.Home,
            defaultRoute = "diagnostics",
            items = listOf(TabItem("诊断", Icons.Default.Home, "diagnostics")),
            collapsible = false
        ),
        SidebarGroup(
            id = "terminal",
            label = "终端",
            icon = Icons.Default.Home,
            defaultRoute = "terminal",
            items = listOf(TabItem("终端", Icons.Default.Home, "terminal")),
            collapsible = false
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
    fun `app route should stay standalone`() {
        assertEquals(
            emptySet(),
            visibleSidebarGroupIds(groups, selectedRoute = "app", expandedGroupId = null)
        )
    }

    @Test
    fun `user expanded collapsible group should be visible from standalone route`() {
        assertEquals(
            emptySet(),
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
    fun `diagnostics route should stay standalone`() {
        assertEquals(
            emptySet(),
            visibleSidebarGroupIds(groups, selectedRoute = "diagnostics", expandedGroupId = null)
        )
    }

    @Test
    fun `device control route should stay standalone`() {
        assertEquals(
            emptySet(),
            visibleSidebarGroupIds(groups, selectedRoute = "device-control", expandedGroupId = null)
        )
    }
}
