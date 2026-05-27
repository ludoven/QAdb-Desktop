# QADB 项目优化分析报告

---

## 一、性能优化

### P0 - 严重性能问题

**1. `materialIconsExtended` 导致启动缓慢**

`build.gradle.kts` 中引入了完整的 Material Icons 库（约 2000+ 图标，25+ MB 字节码），但项目实际只使用了约 30 个图标。JVM Desktop 端启动时会加载所有图标类，显著拖慢冷启动速度。

修复：替换为仅导入项目实际使用的图标子集，或自定义图标集合。这是单项最大的启动优化。

**2. Logcat 过滤在每次批量到达时全量重算**

`LogScreen.kt` 的 `filteredLogs` 和 `detectLikelyPackage` 在每 40 行新日志到达时触发，分别遍历全部 10,000 条日志做正则匹配和包名统计，且在主线程的 `remember` 中执行。高频输出时造成明显 UI 卡顿。

修复：将过滤逻辑移入 ViewModel，用 `combine` + `debounce` 替代；`detectLikelyPackage` 至多每 2-3 秒重算一次。

**3. TerminalController 每行输出执行 UUID 分配 + 全列表拷贝**

`TerminalController.kt` 的 `appendLine` 每行输出调用 `UUID.randomUUID()`（使用 SecureRandom，性能极差），并拼接 + 复制最多 5000 元素的列表。高频命令输出时造成大量 GC 压力和 UI 无响应。

修复：使用 `AtomicLong` 自增 ID 替代 UUID；采用 `ArrayDeque` + 批量发布策略（类似 Logcat 的缓冲机制）。

**4. 设备信息加载串行执行 12 个 ADB 命令**

`DevicesViewModel.loadDeviceInfo` 顺序执行 `getprop`、`wm size`、`wm density`、`cat /proc/meminfo`、`dumpsys battery` 等约 12 个命令，每个耗时 50-200ms，总计 1-3 秒。

修复：用 `async {}` 将独立命令并行执行，预计可将加载时间缩短约 70%。

**5. AppViewModel 每次加载应用列表都执行重量级 `dumpsys activity processes`**

这是最耗时的 ADB 命令之一（1-3 秒），在每次加载应用列表时执行，而非仅在用户主动请求时触发。

修复：改为按需加载或缓存结果，不在初次加载时自动执行。

### P1 - 高优先级性能问题

**6. AppViewModel 频繁全量拷贝应用列表和图标 Map**

每次加载单个图标、更新标签或修改状态时，都对包含 200+ 个应用的列表做 `.map {}` 全量拷贝，对整个图标 Map 做 `.toMutableMap()` 拷贝。每个图标加载都触发整个列表的重组。

修复：使用 `StateFlow.update {}` 做局部更新；考虑引入 `kotlinx-collections-immutable` 的 `PersistentMap`。

**7. `getInstalledApps()` 循环内重复编译正则 + 串行三次 ADB 调用**

`AppScreen.kt` 中该函数在 200+ 行循环内每次 `Regex(...)` 编译正则；三次 `pm list` 命令串行执行。且该函数位于页面层而非 ViewModel。

修复：正则提取为伴生对象的 `val`；三次 pm 调用用 `async` 并行；逻辑迁移到 ViewModel。

**8. `DevicesViewModel.loadDeviceInfo` 中正则在循环内重复编译**

`getprop` 输出约 200+ 行，每行都重新编译 `Regex("\\[(.*?)]\\s*:\\s*\\[(.*?)]")`。

修复：将正则编译移到循环外部作为变量。

**9. LogViewModel 正则匹配执行两次**

每行日志先 `.matches()` 判断，再 `.find()!!.destructured` 提取，同一正则被匹配执行两遍。

修复：直接用 `find()` 返回值判空即可，省去一次完整匹配。

**10. AdbTool.runCommand 无超时、无流关闭**

同步阻塞调用无超时机制，ADB 命令挂起时线程永久阻塞；`inputStream` 和 `bufferedReader` 未在 `finally` 中关闭，存在流泄漏。

修复：添加超时参数；在 `finally` 中关闭流；优先使用 suspend 版本的 `executeCommand`。

### P2 - 中等性能问题

**11. App.kt 根组件收集过多 StateFlow 触发大范围重组**

4 个 `collectAsState()` 在根组件中，任一变化导致整个 NavHost（10 个页面路由）重组；`tabs` 列表每次重组都重新创建。

修复：将 `collectAsState` 下沉到具体子组件；`tabs` 用 `remember` 包裹。

**12. PackageNameCache 使用非线程安全的 `mutableMapOf()`**

多协程并发访问可能导致 `ConcurrentModificationException`。

修复：改用 `ConcurrentHashMap`。

**13. AppScreen DetailItemList 对 50+ 权限/100+ Activity 全量渲染**

`FlowRow` 在 `Column` 中一次性渲染所有条目，无懒加载。

修复：改用 `LazyVerticalGrid` 或"加载更多"机制。

---

## 二、交互优化

### P0 - 严重交互问题

**14. 重启设备无确认对话框（App.kt + CommonScreen.kt）**

菜单栏和命令中心的"重启设备"操作均直接执行，无确认弹窗。误触会导致测试中断、设备数据丢失。

修复：在执行前显示 AlertDialog 确认。

**15. App 详情页的卸载和清除数据操作无确认**

`AppScreen.kt` 的 `DetailMoreActionMenu` 中，"清除数据"和"卸载"直接调用 `onAction`，绕过了列表视图中已有的确认弹窗机制。

修复：接入已有的确认流程（`onRequestDangerAction`）。

**16. ProcessScreen 最后一列显示错误：显示了 name 而非 user**

表头标注"User"列，但实际渲染的是 `item.name`（重复显示了进程名），`item.user` 从未被展示。

修复：将 line 305 的 `item.name` 改为 `item.user`。

**17. 暗色模式大面积失效：Mirror、Settings、Log 页面硬编码白色**

`DeviceMirrorScreen`（`MirrorColors` 全局硬编码）、`SettingScreen`（`Color.White`）、`LogScreen`（过滤器和表头）均使用硬编码亮色，暗色模式下视觉严重不协调。

修复：全部替换为 `MaterialTheme.colorScheme` 对应的语义化颜色。

### P1 - 高优先级交互问题

**18. HomeScreen 未连接设备时显示满屏 "--" 占位**

所有指标面板、设备信息、连接信息均显示 `--`，无引导性空状态。新用户不知道该做什么。

修复：未连接时展示居中的空状态页面，包含设备图标、引导文案和"连接设备"按钮。

**19. 无键盘快捷键**

无任何导航快捷键（如 Cmd+1~0 切换页面）、刷新快捷键（Cmd+R）、终端快捷键（Cmd+T）。菜单栏也无加速键标注。

修复：在根组件添加 `onPreviewKeyEvent` 处理器；为菜单项添加 `shortcut` 参数。

**20. 操作结果静默失败，无用户反馈**

`disconnectSelectedDevice()` 结果仅 `println` 输出；复制到剪贴板操作也无任何提示。

修复：通过 `showTipDialog` 或 Snackbar 反馈操作结果；复制操作后显示"已复制"提示。

**21. 连接时长每次导航回 Home 都重置**

`connectedSince` 用 `remember(selectedDevice)` 存储在 composable 中，切换页面后返回时重新计算为 `LocalDateTime.now()`。

修复：将连接时间戳存储在 `DevicesViewModel` 中，而非 composable 局部状态。

**22. HomeScreen 延迟和传输速度永远显示 "--"**

`ConnectionInfoPanel` 中的延迟和传输速度行从未被填充数据，永久显示占位符，误导用户认为功能存在但未工作。

修复：实现实际测量，或移除这些占位行。

**23. KeyEventScreen 头部下拉箭头无实际功能**

设备指示器旁显示 `KeyboardArrowDown` 图标暗示有下拉选择，但点击无反应。

修复：实现设备切换下拉，或移除误导性箭头。

### P2 - 中等交互问题

**24. 多处国际化不完整**

`AppScreen` 详情页 Tab 标题混用中英文（"Activity 列表"始终中文）；多个 `DetailSectionCard` 标题硬编码中文。非中文用户看到中英混杂界面。

修复：所有用户可见文本使用 `l10n(...)` 国际化。

**25. FileBrowser 多选删除只删除第一个文件**

选中多个文件后点击删除，仅对 `selectedPaths.firstOrNull()` 生效。

修复：改为批量删除，确认弹窗显示删除数量。

**26. FileBrowser 面包屑导航假设基路径为 `/sdcard`**

导航到 `/data/data` 或 `/system` 时面包屑计算错误，可能导致路径导航异常。

修复：基于实际当前路径前缀计算面包屑，而非硬编码 `/sdcard`。

**27. Sidebar 设备名称过长时文本溢出**

`ConnectedStatusCard` 的 `selectedDisplay` 设置了 `maxLines = 1` 但未设置 `TextOverflow.Ellipsis`。

修复：添加 `overflow = TextOverflow.Ellipsis`，悬浮时显示完整名称 Tooltip。

**28. CommonScreen 命令输出区域固定高度 160dp**

`dumpsys` 等长输出命令受限于 160dp 高度的小窗口内，需大量滚动。

修复：改为 `heightIn(min = 80.dp, max = 300.dp)` 或支持拖拽调整高度。

**29. ProcessScreen 表头不可排序**

用户无法按 CPU、内存、PID 等列排序，只能手动扫描列表。

修复：参照 `AppScreen.SortableHeaderLabel` 模式实现可点击排序。

**30. AppScreen 存在永久禁用的死按钮**

"查看权限详情"和"查看更多日志"按钮 `enabled = false`，无任何说明何时可用。

修复：实现功能或移除按钮，避免用户困惑。

**31. SystemScreen 是空壳占位文件**

仅包含硬编码中文文本 "系统页面" 和 TODO 注释，属死代码。

修复：完整实现或移除文件。

---

## 优化优先级建议

| 优先级 | 编号 | 预期收益 |
|--------|------|----------|
| 立即修复 | #16 ProcessScreen 列错误 | 数据正确性 |
| 立即修复 | #14/#15 危险操作无确认 | 防止误操作 |
| 高收益 | #1 materialIconsExtended | 启动速度提升 2-5 秒 |
| 高收益 | #3 TerminalController 优化 | 终端高输出时不再卡顿 |
| 高收益 | #2 Logcat 过滤优化 | 日志实时捕获流畅度 |
| 高收益 | #4/#5 ADB 命令并行化 | 设备信息加载提速 70% |
| 体验提升 | #17 暗色模式修复 | 视觉一致性 |
| 体验提升 | #18 空状态引导 | 新用户引导 |
| 体验提升 | #24 国际化补全 | 英文用户可用性 |
