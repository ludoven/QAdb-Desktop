# Sidebar Navigation Grouping Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将左侧导航从 9 个平铺一级入口收敛为 5 个一级入口，并把 `终端 / 按键事件 / 文件浏览 / 日志 / 进程` 下沉到“工具”二级导航。

**Architecture:** 保留现有 `NavHost` route 和 `AppMenuCommand.Navigate(route)` 不变，只调整侧边栏的数据结构与展示层。将“工具分组”的选中态和展开态抽成纯 Kotlin helper，先用桌面单测锁定规则，再把 `App.kt` 和 `Sidebar.kt` 接上。

**Tech Stack:** Kotlin Multiplatform JVM desktop, JetBrains Compose Desktop, `kotlin.test`, Gradle

---

## File Map

- Create: `composeApp/src/desktopMain/kotlin/com/ludoven/adbtool/widget/SidebarNavigation.kt`
  - 负责工具组 route 集合、一级选中路由映射、工具组展开规则
- Create: `composeApp/src/desktopTest/kotlin/com/ludoven/adbtool/widget/SidebarNavigationStateTest.kt`
  - 覆盖工具组 helper 的纯逻辑测试
- Modify: `composeApp/src/desktopMain/kotlin/com/ludoven/adbtool/App.kt`
  - 将侧边栏数据拆成一级项和工具组子项，并把 route 状态传给 `Sidebar`
- Modify: `composeApp/src/desktopMain/kotlin/com/ludoven/adbtool/widget/Sidebar.kt`
  - 渲染“工具”一级分组、展开箭头、二级项缩进与选中态

### Task 1: Lock Sidebar Grouping Rules With Tests

**Files:**
- Create: `composeApp/src/desktopTest/kotlin/com/ludoven/adbtool/widget/SidebarNavigationStateTest.kt`
- Create: `composeApp/src/desktopMain/kotlin/com/ludoven/adbtool/widget/SidebarNavigation.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
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
        assertFalse(SidebarNavigation.isToolRoute("home"))
    }

    @Test
    fun tool_routes_map_to_tools_primary_entry() {
        assertEquals("tools", SidebarNavigation.resolvedPrimaryRoute("terminal"))
        assertEquals("tools", SidebarNavigation.resolvedPrimaryRoute("log"))
        assertEquals("home", SidebarNavigation.resolvedPrimaryRoute("home"))
    }

    @Test
    fun tools_group_is_forced_open_for_tool_routes() {
        assertTrue(SidebarNavigation.shouldExpandTools("terminal", manuallyExpanded = false))
        assertTrue(SidebarNavigation.shouldExpandTools("log", manuallyExpanded = false))
    }

    @Test
    fun tools_group_respects_manual_toggle_for_non_tool_routes() {
        assertTrue(SidebarNavigation.shouldExpandTools("home", manuallyExpanded = true))
        assertFalse(SidebarNavigation.shouldExpandTools("home", manuallyExpanded = false))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
./gradlew :composeApp:desktopTest --tests com.ludoven.adbtool.widget.SidebarNavigationStateTest
```

Expected: FAIL with unresolved reference errors for `SidebarNavigation`

- [ ] **Step 3: Write minimal implementation**

```kotlin
package com.ludoven.adbtool.widget

object SidebarNavigation {
    const val ToolsRoute = "tools"

    private val toolRoutes = setOf(
        "terminal",
        "keyevent",
        "filebrowser",
        "log",
        "process"
    )

    fun isToolRoute(route: String): Boolean = route in toolRoutes

    fun resolvedPrimaryRoute(route: String): String {
        return if (isToolRoute(route)) ToolsRoute else route
    }

    fun shouldExpandTools(selectedRoute: String, manuallyExpanded: Boolean): Boolean {
        return isToolRoute(selectedRoute) || manuallyExpanded
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```bash
./gradlew :composeApp:desktopTest --tests com.ludoven.adbtool.widget.SidebarNavigationStateTest
```

Expected: PASS and `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/desktopMain/kotlin/com/ludoven/adbtool/widget/SidebarNavigation.kt composeApp/src/desktopTest/kotlin/com/ludoven/adbtool/widget/SidebarNavigationStateTest.kt
git commit -m "test: add sidebar navigation state rules"
```

### Task 2: Rewire App Sidebar Data Without Changing Routes

**Files:**
- Modify: `composeApp/src/desktopMain/kotlin/com/ludoven/adbtool/App.kt`
- Create: `composeApp/src/desktopMain/kotlin/com/ludoven/adbtool/widget/SidebarNavigation.kt`

- [ ] **Step 1: Write the failing integration expectation in the existing helper test**

```kotlin
@Test
fun tools_primary_route_is_stable_for_all_group_children() {
    val routes = listOf("terminal", "keyevent", "filebrowser", "log", "process")
    routes.forEach { route ->
        assertEquals(SidebarNavigation.ToolsRoute, SidebarNavigation.resolvedPrimaryRoute(route))
    }
}
```

- [ ] **Step 2: Run test to verify it fails before wiring all child routes**

Run:

```bash
./gradlew :composeApp:desktopTest --tests com.ludoven.adbtool.widget.SidebarNavigationStateTest
```

Expected: FAIL if any tool child route is missing from `toolRoutes`

- [ ] **Step 3: Update `App.kt` to feed primary and secondary items separately**

```kotlin
val primaryTabs = listOf(
    TabItem(stringResource(Res.string.home), Icons.Default.Home, "home"),
    TabItem(stringResource(Res.string.common), Icons.Default.Info, "common"),
    TabItem(stringResource(Res.string.app), Icons.Default.Apps, "app"),
    TabItem(stringResource(Res.string.menu_tools), Icons.Default.Build, SidebarNavigation.ToolsRoute),
    TabItem(stringResource(Res.string.set), Icons.Default.Settings, "setting")
)

val toolTabs = listOf(
    TabItem(stringResource(Res.string.terminal), Icons.Default.Code, "terminal"),
    TabItem(stringResource(Res.string.key_event_page), Icons.Default.VideogameAsset, "keyevent"),
    TabItem(stringResource(Res.string.file_browser), Icons.Default.Folder, "filebrowser"),
    TabItem(stringResource(Res.string.log), Icons.Default.List, "log"),
    TabItem(stringResource(Res.string.process), Icons.Default.Memory, "process")
)

Sidebar(
    items = primaryTabs,
    toolItems = toolTabs,
    selectedRoute = currentRoute,
    connectedDeviceCount = devices.size,
    devices = devices,
    selectedDevice = selectedDevice,
    deviceDisplayNames = deviceDisplayNames,
    onItemClick = { route -> navigateToRoute(route) },
    onDeviceSelected = { deviceId -> devicesViewModel.selectDevice(deviceId) }
)
```

- [ ] **Step 4: Run the helper test again to verify route grouping still passes**

Run:

```bash
./gradlew :composeApp:desktopTest --tests com.ludoven.adbtool.widget.SidebarNavigationStateTest
```

Expected: PASS and `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/desktopMain/kotlin/com/ludoven/adbtool/App.kt composeApp/src/desktopMain/kotlin/com/ludoven/adbtool/widget/SidebarNavigation.kt composeApp/src/desktopTest/kotlin/com/ludoven/adbtool/widget/SidebarNavigationStateTest.kt
git commit -m "feat: group sidebar routes under tools"
```

### Task 3: Render Expandable Tools Group In Sidebar

**Files:**
- Modify: `composeApp/src/desktopMain/kotlin/com/ludoven/adbtool/widget/Sidebar.kt`
- Create: `composeApp/src/desktopMain/kotlin/com/ludoven/adbtool/widget/SidebarNavigation.kt`

- [ ] **Step 1: Write the failing sidebar behavior as a helper-focused test**

```kotlin
@Test
fun tools_group_opens_automatically_for_selected_tool_page() {
    val selectedRoute = "filebrowser"
    val expanded = SidebarNavigation.shouldExpandTools(selectedRoute, manuallyExpanded = false)

    assertTrue(expanded)
    assertEquals(SidebarNavigation.ToolsRoute, SidebarNavigation.resolvedPrimaryRoute(selectedRoute))
}
```

- [ ] **Step 2: Run test to verify it fails if sidebar state helpers regress**

Run:

```bash
./gradlew :composeApp:desktopTest --tests com.ludoven.adbtool.widget.SidebarNavigationStateTest
```

Expected: FAIL only if helper logic no longer keeps tool pages expanded

- [ ] **Step 3: Implement grouped rendering in `Sidebar.kt`**

```kotlin
@Composable
fun Sidebar(
    items: List<TabItem>,
    toolItems: List<TabItem>,
    selectedRoute: String,
    connectedDeviceCount: Int,
    devices: List<String>,
    selectedDevice: String?,
    deviceDisplayNames: Map<String, String>,
    onItemClick: (String) -> Unit,
    onDeviceSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var manuallyExpanded by remember { mutableStateOf(false) }
    val expanded = SidebarNavigation.shouldExpandTools(
        selectedRoute = selectedRoute,
        manuallyExpanded = manuallyExpanded
    )

    items.forEach { item ->
        if (item.route == SidebarNavigation.ToolsRoute) {
            SidebarGroupItem(
                item = item,
                isSelected = SidebarNavigation.resolvedPrimaryRoute(selectedRoute) == item.route,
                isExpanded = expanded,
                onClick = { manuallyExpanded = !expanded }
            )

            if (expanded) {
                toolItems.forEach { toolItem ->
                    SidebarSubItem(
                        item = toolItem,
                        isSelected = selectedRoute == toolItem.route,
                        onClick = { onItemClick(toolItem.route) }
                    )
                }
            }
        } else {
            SidebarItem(
                item = item,
                isSelected = SidebarNavigation.resolvedPrimaryRoute(selectedRoute) == item.route,
                onClick = { onItemClick(item.route) }
            )
        }
    }
}
```

- [ ] **Step 4: Run compile verification**

Run:

```bash
./gradlew :composeApp:compileKotlinDesktop
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/desktopMain/kotlin/com/ludoven/adbtool/widget/Sidebar.kt composeApp/src/desktopMain/kotlin/com/ludoven/adbtool/App.kt composeApp/src/desktopMain/kotlin/com/ludoven/adbtool/widget/SidebarNavigation.kt composeApp/src/desktopTest/kotlin/com/ludoven/adbtool/widget/SidebarNavigationStateTest.kt
git commit -m "feat: render grouped sidebar navigation"
```

### Task 4: Verify Navigation Behavior End-To-End

**Files:**
- Modify: `composeApp/src/desktopMain/kotlin/com/ludoven/adbtool/App.kt`
- Modify: `composeApp/src/desktopMain/kotlin/com/ludoven/adbtool/widget/Sidebar.kt`
- Create: `composeApp/src/desktopMain/kotlin/com/ludoven/adbtool/widget/SidebarNavigation.kt`
- Create: `composeApp/src/desktopTest/kotlin/com/ludoven/adbtool/widget/SidebarNavigationStateTest.kt`

- [ ] **Step 1: Re-run the focused helper test**

Run:

```bash
./gradlew :composeApp:desktopTest --tests com.ludoven.adbtool.widget.SidebarNavigationStateTest
```

Expected: PASS and `BUILD SUCCESSFUL`

- [ ] **Step 2: Re-run the fastest compile check for desktop UI**

Run:

```bash
./gradlew :composeApp:compileKotlinDesktop
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Perform manual QA with this checklist**

```text
1. 首页、常用、应用、设置在左侧仍作为一级入口直接可点
2. 工具作为一级入口出现，点击后可以展开/收起
3. 展开后展示终端、按键事件、文件浏览、日志、进程五个二级入口
4. 从顶部菜单跳到终端/日志/进程时，工具组自动展开，对应二级项高亮
5. 从工具页切回首页或应用时，不出现错误高亮或错误跳转
```

- [ ] **Step 4: Record the final changed files**

```text
composeApp/src/desktopMain/kotlin/com/ludoven/adbtool/App.kt
composeApp/src/desktopMain/kotlin/com/ludoven/adbtool/widget/Sidebar.kt
composeApp/src/desktopMain/kotlin/com/ludoven/adbtool/widget/SidebarNavigation.kt
composeApp/src/desktopTest/kotlin/com/ludoven/adbtool/widget/SidebarNavigationStateTest.kt
```

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/desktopMain/kotlin/com/ludoven/adbtool/App.kt composeApp/src/desktopMain/kotlin/com/ludoven/adbtool/widget/Sidebar.kt composeApp/src/desktopMain/kotlin/com/ludoven/adbtool/widget/SidebarNavigation.kt composeApp/src/desktopTest/kotlin/com/ludoven/adbtool/widget/SidebarNavigationStateTest.kt
git commit -m "feat: simplify sidebar navigation hierarchy"
```
