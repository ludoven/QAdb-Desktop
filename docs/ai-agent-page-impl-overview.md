# QADB AI Agent 页面 UI 落地实现 · 交付概述

> 实现团队:软件开发团队(software-team-lead · 快速模式)
> 日期:2026-08-20
> 状态:✅ 已完成 · 工程师实现 + QA 独立验证双编译通过 · 可交付

---

## 一、任务

按 `canvas/qadb-aiagent.html`(高保真原型)+ `canvas/design-tokens.md`(QADB Workbench 双主题令牌规范)将现有 `AiAgentScreen.kt` 从旧 Material 风格重构为**三栏工作台**。纯 UI 重构,未改动 Agent 引擎 / ViewModel / 路由 / 对外签名。

## 二、变更文件清单

| 文件 | 变更 | 说明 |
|------|------|------|
| `composeApp/src/desktopMain/kotlin/com/ludoven/adbtool/QadbTokens.kt` | **新增** | QADB Workbench 双主题设计令牌,逐条映射 design-tokens.md §2.2;间距/圆角/字号复用 UiTokens 防漂移 |
| `composeApp/src/desktopMain/kotlin/com/ludoven/adbtool/pages/AiAgentScreen.kt` | 重写 | 对外签名不变,三栏工作台全量改造 |
| `composeApp/src/desktopMain/composeResources/values/strings.xml` | +41 条 | zh 文案 |
| `composeApp/src/desktopMain/composeResources/values-en/strings.xml` | +41 条 | en 文案(与 zh 完全对齐,均为 843 条) |
| `composeApp/src/desktopMain/kotlin/com/ludoven/adbtool/ui/icons/IconParkIcons.kt` | +5 别名 | Star / ChevronUp / ChevronLeft / Eye / ShieldAlert(仅追加) |

## 三、实现要点(对照原型)

1. **顶部 44dp 设备状态栏**:设备名(等宽)+ Android X · API Y + 连接方式 + 电量;右侧 ADB 绿/灰点 + 运行中蓝徽标
2. **三栏布局**:中央对话区文本列 max 720dp 居中;右设备面板 400dp(≥1280)/ 320dp 浮层(1080–1279)/ <1080 隐藏;可收合 + 右下角 ‹ 重新展开
3. **消息**:用户气泡品牌蓝浅底 + 深蓝字 + 小圆角在右下(12/12/4/12);助手卡片式 + 24dp AI 紫「AI」头像
4. **思考链卡**:AI 紫边框 + 紫星图标,默认折叠,展开显示阶段说明(不编造推理文本)
5. **工具调用卡**:等宽工具名(observe / am start / input tap / input swipe / input text / input keyevent / sleep / pm list packages / am force-stop / pm clear / pm uninstall / reboot)+ 状态点(已执行绿 / 待确认琥珀 / 执行中蓝呼吸 / 失败红)+ HH:mm:ss + 展开后 bg-3 命令代码块 + 结果行
6. **任务进度卡**:标题 + 状态徽标 + 圆点竖线步骤时间线(完成绿 / 当前琥珀 / 进行中蓝)+ N 步 · 已完成 M
7. **敏感操作内联确认卡**:danger 容器,对话流底部内联,接线 `viewModel.respondToConfirmation(true/false)`(替换旧 AlertDialog)
8. **底部输入区**:8dp 圆角输入框 + focus 品牌蓝焦点环;ghost 圆形快捷按钮(设置 / 截图识别);primary「发送+箭头」/ 运行中 danger「取消」;视觉观察小开关 + 「AI 正在看你的屏幕 · 时间」+ Enter 提示
9. **右侧设备面板**:Page id 深底 pill + changed 徽标;视觉观察模式卡(BETA 徽标 + 大开关 + 状态点 + 最近观察 / 每 2s 一帧);Token 计量条(4dp,≥0.75 琥珀 / ≥0.9 红)+ 压缩次数徽标 + 等宽数字(当前 / 上限 / 剩余)

## 四、验证结果

| 项 | 结果 |
|----|------|
| 工程师编译 `compileKotlinDesktop` | EXIT=0 ✅ |
| QA 独立重编译 `--rerun-tasks`(9 tasks 全量重跑) | BUILD SUCCESSFUL · EXIT=0 ✅ |
| 设计令牌 vs design-tokens.md §2.2 | 逐值一致 PASS |
| 页面组件 vs 原型 | PASS(1 项 P2 圆角方向已修复) |
| 功能回归(startTask/newTask/cancelTask/respondToConfirmation 等) | PASS |
| i18n zh/en 对齐 | 843=843 · only_zh=[] · only_en=[] PASS |
| 硬约束(agent/、App.kt、ViewModel、签名零改动) | PASS(git status 佐证) |

## 五、已知限制 / 后续建议

- **观察模式开关为 UI 展示态**,未接引擎;后续建议在 ViewModel 增加观察模式接口
- **工具卡展开命令块为骨架 + "…"**:公开活动模型刻意不携带坐标/文本/包名(隐私设计),如需完整命令需扩展公开活动模型
- **思考链卡为阶段标签**(UNDERSTANDING / PLANNING),无真实推理文本
- 工具/思考卡按 run 分组渲染在用户消息之后(受公开活动模型限制,无法在任意消息间精确插值)
- 建议 QA 后续在 1280 / 1080 两个断点窗口尺寸做人工视觉回归,并验证面板收合交互
