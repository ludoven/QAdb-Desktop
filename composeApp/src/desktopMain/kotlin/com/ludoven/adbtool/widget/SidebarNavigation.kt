package com.ludoven.adbtool.widget

import androidx.compose.ui.graphics.vector.ImageVector
import com.ludoven.adbtool.TabItem

data class SidebarGroup(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val defaultRoute: String,
    val items: List<TabItem>,
    val collapsible: Boolean = true
)

internal fun selectedSidebarGroupId(groups: List<SidebarGroup>, selectedRoute: String): String? {
    return groups.firstOrNull { group ->
        group.collapsible && group.items.any { it.route == selectedRoute }
    }?.id
}

internal fun visibleSidebarGroupIds(
    groups: List<SidebarGroup>,
    selectedRoute: String,
    expandedGroupId: String?
): Set<String> {
    val selectedGroupId = selectedSidebarGroupId(groups, selectedRoute)
    return setOfNotNull(
        selectedGroupId,
        expandedGroupId?.takeIf { id -> groups.any { it.id == id && it.collapsible } }
    )
}
