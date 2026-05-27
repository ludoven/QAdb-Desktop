# v2.0.7 更新日志

## 性能优化

- **终端输出性能提升**：TerminalController 的行输出从 `UUID.randomUUID()`（基于 SecureRandom）改为 `AtomicLong` 自增 ID，列表追加从双次拷贝优化为单次预分配 ArrayList，在高频命令输出下大幅降低 GC 压力
- **Logcat 实时过滤优化**：日志过滤从主线程 `remember` 内每次全量遍历改为 ViewModel 层 `combine` + `debounce(100)` 响应式派生 StateFlow，消除高频日志捕获时的 UI 卡顿
- **Logcat 正则匹配去重**：每行日志的正则匹配从两次（`.matches()` + `.find()`）合并为单次 `.find()` 调用，去除不安全的 `!!` 操作符
- **应用列表加载提速**：`pm list packages -f/-s/-d` 三个 ADB 命令从串行执行改为 `async` 并行执行；正则从循环内重复编译提取为文件级常量
- **设备信息正则缓存**：DevicesViewModel 中 `getprop` 输出解析的正则从循环内编译提取为类级常量
- **PackageNameCache 线程安全**：底层存储从 `mutableMapOf()` 改为 `ConcurrentHashMap`，消除多协程并发访问时的潜在数据竞争
- **ADB 进程流清理**：`runCommand` 方法添加 `try/finally` 确保进程的 InputStream、OutputStream 关闭和 `destroyForcibly()` 调用，防止流泄漏和线程阻塞
- **FileBrowser 过滤计算优化**：`filteredFiles` 从 `remember` 改为 `derivedStateOf`，避免输入变化但过滤结果相同时的不必要重组

## 交互优化

- **危险操作确认弹窗**：重启设备（菜单栏 + 命令中心）、应用详情页的卸载和清除数据操作均添加了确认弹窗，防止误操作
- **HomeScreen 空状态引导**：未连接设备时展示居中引导页面（设备图标 + 提示文案），替代原先满屏 "--" 占位的仪表盘
- **连接时长持久化**：设备连接时长不再因页面切换而重置为 0，改为通过设备 ID 变化触发更新
- **断开设备反馈**：断开设备连接的操作结果从 `println` 改为 TipDialog 弹窗提示
- **ProcessScreen 列排序**：所有表头（名称、CPU、CPU 时间、内存、PID、用户）均可点击排序，支持升降序切换
- **ProcessScreen User 列修复**：最后一列从错误显示进程名修正为正确显示用户字段
- **FileBrowser 批量删除**：多选文件后的删除操作支持批量执行，确认弹窗显示删除数量
- **FileBrowser 面包屑修复**：路径导航不再硬编码 `/sdcard` 为基路径，支持 `/data/data`、`/system` 等任意目录的正确面包屑显示
- **CommonScreen 输出区自适应**：命令执行结果区域从固定 160dp 改为 80dp-300dp 自适应高度
- **KeyEventScreen 移除误导图标**：设备指示器旁的无功能下拉箭头图标已移除
- **Sidebar 设备名溢出处理**：长设备名称添加 `TextOverflow.Ellipsis` 省略显示
- **AppScreen 移除死按钮**：永久禁用的"查看权限详情"和"查看更多日志"按钮已移除

## 暗色模式修复

- **DeviceMirrorScreen**：`MirrorColors` 从硬编码亮色改为从 `MaterialTheme.colorScheme` 语义化派生，所有 `Color.White` 背景替换为主题色
- **SettingScreen**：卡片、下拉菜单的 `Color.White` 和 `Color(0xFFF8F9FB)` 替换为 `surface` / `surfaceVariant` 主题色
- **LogScreen**：过滤器输入框、表头、快速过滤标签的硬编码颜色替换为 `outlineVariant` / `surfaceVariant` 主题色

## 国际化修复

- **AppScreen 详情页 Tab 标题**：从硬编码中文（"概览"、"权限"）改为 `l10n()` 国际化调用
- **AppScreen Section 标题**："Activity 列表"、"Service 列表"、"Receiver 列表"、"Provider 列表" 改为 `l10n()` 调用
