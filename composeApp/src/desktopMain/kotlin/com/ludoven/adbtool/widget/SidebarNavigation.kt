package com.ludoven.adbtool.widget

object SidebarNavigation {
    const val ToolsRoute = "tools"

    private val toolRoutes = setOf(
        "terminal",
        "log",
        "process"
    )

    fun isToolRoute(route: String): Boolean = route in toolRoutes

    fun resolvedPrimaryRoute(route: String): String {
        return if (isToolRoute(route)) ToolsRoute else route
    }

    fun shouldExpandTools(selectedRoute: String, manuallyExpanded: Boolean): Boolean {
        return manuallyExpanded
    }
}
