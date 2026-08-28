# QADB AI Agent 页面 · 设计令牌（Design Tokens）

> 设计系统专家：彩格调（Cai）
> 输入基准：`canvas/discovery-summary.md`（需求发现分析师许明需整理）
> 用途：作为 AI Agent 页面原型与视觉执行的唯一色彩/排版/组件基准，可直接映射到 Compose Multiplatform 代码。
> 主题：Light / Dark 双主题（默认跟随系统，符合桌面工具惯例）

---

## 0. 决策摘要

- **推荐候选**：Linear（★★★★★）· Warp（★★★★☆）· Raycast（★★★★☆）
- **选定方案**：以 **Linear** 的工作台基底 + **Warp** 的 AI 过程可视化范式 + **Raycast** 的输入/导航范式融合，注入 **QADB 蓝 #2196F3** 作为品牌强调色。
- **视觉方向**：Modern Minimal（现代极简）× Tech Utility（技术工具感）——低饱和中性背景、清晰层级、数据密度、克制高亮。
- **双色语义**：🔵 **QADB 蓝** = 设备 / 品牌 / 主操作；🟣 **AI 紫（辅助强调）** = Agent 活动遥测（思考链、工具调用、Token、观察模式）。双色分离，互不抢戏。

---

## 1. 候选方案对比

| 方案 | 设计系统 | 匹配度 | 特征 | 适合原因 |
|------|---------|--------|------|---------|
| **A（选定基底）** | **Linear** | ★★★★★ | 深/浅双主题工作台、克制低饱和、数据密度、精确排版、任务流状态机 | 需求摘要点名参考系之首；「三栏工作台 + 任务状态流 + 开发工具」的现成范式；对专业用户信任感的建立方式与 QADB 诉求完全一致 |
| **B** | **Warp** | ★★★★☆ | 终端 × AI 融合、命令面板、工具调用/输出内联展示、AI 提示带 | Agent 工具调用卡、思考链、Token 计数等「AI 活动可视化」的最佳现成范式；但整体偏终端块状，需弱化 |
| **C** | **Raycast** | ★★★★☆ | 键盘优先、极简、命令面板、清晰的输入框/选中态/快捷操作 | 底部输入区、快捷按钮、导航选中态、空状态的最佳参考；但缺少「多面板工作台」范式，需 Linear 补足 |

> 备选说明：曾考虑 **Claude**（AI 对话调性亲和，但奶油色基底与「工具感」冲突）、**Supabase / Vercel**（同为开发者工具，但品牌色抢夺 QADB 蓝的识别度）。最终以上三套差异化最明显、与「专业·克制·工具感」最契合。

---

## 2. 选定方案：QADB Workbench（Linear-Class）

### 2.1 Visual Theme（视觉主题）

```
Philosophy : 让 Agent 的工作透明可见，让设备的状态一目了然。
Direction  : minimal, utilitarian, data-dense（现代极简 × 技术工具感）
Personality: precise, trustworthy, calm-confident（精确、可信、从容自信）
Reference  : Linear / Warp / Raycast / VS Code
```

**设计原则**
1. **中性打底，蓝为点睛**：页面基底为中性深/浅灰阶，QADB 蓝只出现在「操作、焦点、活动状态」上，绝不铺满。
2. **层级靠「面」而非「色」**：用 bg-0/1/2/3 的明度差与 1px 边框建立结构，不用彩色块分区。
3. **数据自己说话**：Token 数字、命令、设备状态用等宽字体 + 语义色点，减少装饰性元素。
4. **AI 透明化**：Agent 的每一步（思考、调用工具、执行结果）都以可折叠卡片内联呈现，默认折叠、可展开。

---

### 2.2 Color Palette（色彩令牌）

#### 2.2.1 品牌蓝（QADB Blue Ramp）

以品牌基准 **#2196F3** 为中点展开的完整色阶，供按钮/文字/容器/边框/焦点环分级使用。

| Token | HEX | 用途 |
|-------|-----|------|
| `brand-50`  | `#E3F2FD` | 极浅蓝底（选中态容器背景，Light） |
| `brand-100` | `#BBDEFB` | 品牌容器背景（Light）/ 品牌容器边框（Light） |
| `brand-200` | `#90CAF9` | 品牌边框 / 进度条轨道填充（Light） |
| `brand-300` | `#64B5F6` | 品牌边框（Dark）/ hover 提亮 |
| `brand-400` | `#42A5F5` | **品牌文字（Dark 主题）**、链接（Dark） |
| `brand-500` | `#2196F3` | **品牌基准色**：焦点环、图标、活动态、AI 状态点、进度条 |
| `brand-600` | `#1E88E5` | 主按钮 hover（Dark）/ 次级强调（Light） |
| `brand-700` | `#1976D2` | **主按钮底色（Light，白字 AA ≈4.6:1）**、链接文字（Light） |
| `brand-800` | `#1565C0` | 主按钮 hover（Light） |
| `brand-900` | `#0D47A1` | 品牌容器内文字（Light） |

**对比度提醒（WCAG AA）**：`#2196F3` 在白底上的对比度 ≈3.1:1，**仅可用于图标/大元素/非文本（≥3:1）**；小字号链接/强调文字在 Light 下请用 `brand-700 #1976D2`（≈4.6:1）。

#### 2.2.2 中性背景层级（bg-0 → bg-3）

| Token | Light | Dark | 用途 |
|-------|-------|------|------|
| `bg-0` | `#F6F7F9` | `#0F0F12` | **窗口底色**（最底层，留白/页边/空区） |
| `bg-1` | `#FFFFFF` | `#141418` | **主表面**（对话区、卡片、输入区） |
| `bg-2` | `#FBFCFD` | `#1A1A1F` | **次级表面**（左侧导航、顶部状态栏、设备面板） |
| `bg-3` | `#EFF1F4` | `#212127` | **内嵌表面**（代码块、工具调用卡体、Token 轨道、hover 填充） |

> 用法：导航栏/设备面板用 `bg-2`，对话主区用 `bg-1`，两者用 1px `divider` 分隔而非色块对撞；代码块/命令/折叠内容用 `bg-3`。

#### 2.2.3 文字层级

| Token | Light | Dark | 用途 |
|-------|-------|------|------|
| `text-primary`   | `#16181D` | `#ECEDEF` | 标题、正文、高优先信息（对比度 ≥12:1） |
| `text-secondary` | `#4B505A` | `#A9ADB5` | 副标题、元信息、消息正文次级（≥7:1） |
| `text-tertiary`  | `#717684` | `#80858F` | 时间戳、注释、次要标签（≥4.5:1，仅限非关键文字） |
| `text-muted`     | `#9AA0AC` | `#565B64` | 禁用态、占位符、装饰性文字（不可承载关键信息） |

#### 2.2.4 边框 / 分隔线

| Token | Light | Dark | 用途 |
|-------|-------|------|------|
| `border`        | `#E4E6EA` | `#26262C` | 卡片/输入框/组件默认描边 |
| `border-strong` | `#D3D6DC` | `#35353D` | 强调描边、focus 未着色前、分隔较重的区块 |
| `divider`       | `#EDEFF2` | `#1E1E23` | 面板分隔、列表分隔、行内分隔（比 border 更轻） |

#### 2.2.5 状态色（success / warning / danger / info）

| Token | Light | Dark | 用途 |
|-------|-------|------|------|
| `success` | `#1E8E3E` | `#3FB950` | 已执行/成功/ADB 在线（绿） |
| `warning` | `#B45309` | `#F0A020` | 需确认/待观察/压缩提示（琥珀） |
| `danger`  | `#D93025` | `#F2555A` | 失败/错误/风险操作（红） |
| `info`    | `#1976D2` | `#42A5F5` | 进行中/信息（蓝，与品牌同族） |

**状态容器色（浅底 + 深字，用于状态徽标/卡片底）**：

| Token | Light | Dark | 用途 |
|-------|-------|------|------|
| `success-container` | `#E7F4EC` / text `#135C28` | `#12301F` / text `#7EE2A0` | 成功徽标底 |
| `warning-container` | `#FDF0D9` / text `#7C4A03` | `#3A2B0C` / text `#FFD28A` | 警告徽标底 |
| `danger-container`  | `#FCEBEA` / text `#A11C12` | `#3A1A1E` / text `#FF9C96` | 失败徽标底 |
| `info-container`   | `#E8F1FC` / text `#0B4FA0` | `#0E2A47` / text `#9ECBEF` | 信息徽标底 |

#### 2.2.6 AI 强调色（Agent 活动专属，可配置）

| Token | Light | Dark | 用途 |
|-------|-------|------|------|
| `ai`            | `#7C3AED` | `#A78BFA` | 思考链图标、工具调用图标、Token 计量条、观察模式开关（紫） |
| `ai-container`  | `#EFEAFE` / text `#4C1D95` | `#241A3D` / text `#C4B5FD` | 思考链/工具调用卡片底色 |
| `ai-border`     | `#DCD1F8` | `#3A2F5E` | AI 卡片描边 |

> **为什么用紫色做 AI**：QADB 蓝代表「设备与品牌」，紫色代表「Agent 的活动」，一眼可辨「这是 AI 在做什么」；业界已有成熟先例（GitHub Copilot、Claude）。若团队坚持单强调色，可退化为 `brand-200`/`brand-300` 蓝系，但会削弱「Agent 遥测」与「设备状态」的语义区分。

#### 2.2.7 快速引用（双主题速查）

```
Light: bg-0 #F6F7F9 · bg-1 #FFFFFF · bg-2 #FBFCFD · bg-3 #EFF1F4
       text #16181D / #4B505A / #717684 / #9AA0AC
       border #E4E6EA · divider #EDEFF2
       brand #2196F3 · brand-action #1976D2 · ai #7C3AED
Dark : bg-0 #0F0F12 · bg-1 #141418 · bg-2 #1A1A1F · bg-3 #212127
       text #ECEDEF / #A9ADB5 / #80858F / #565B64
       border #26262C · divider #1E1E23
       brand #2196F3 · brand-action #42A5F5 · ai #A78BFA
```

---

### 2.3 Typography（排版）

#### 2.3.1 字体族（跨平台 Fallback 栈）

```css
--font-ui:  "Inter", "SF Pro Text", "Segoe UI", "Roboto",
            "Noto Sans SC", "PingFang SC", "Microsoft YaHei", sans-serif;
--font-mono: "JetBrains Mono", "SF Mono", "Cascadia Code",
             "Consolas", "Roboto Mono", "Menlo", monospace;
```

- **UI/正文**：Inter（随包内置，优先）→ 平台系统字体（macOS SF Pro / Windows Segoe UI / Linux Roboto）→ 中文字体（Noto Sans SC / PingFang SC / 微软雅黑）。
- **Mono（等宽）**：JetBrains Mono（随包内置）→ 平台等宽（SF Mono / Cascadia Code / Consolas）→ 通用 monospace。用于命令、工具调用参数、Token 数字、设备信息。
- 中文混排注意：中文不参与等宽行，命令/参数用等宽、说明文字用 UI 字体，避免中文挤在 mono 里。

#### 2.3.2 字号阶梯（桌面优先，14px 为正文基准）

| Level | Size | Weight | Line-height | 用途 |
|-------|------|--------|-------------|------|
| `display`   | 28px | 700 | 1.2 | 窗口大标题、空状态主文案 |
| `heading`   | 17px | 600 | 1.3 | 面板标题、会话标题、任务状态条 |
| `subheading`| 15px | 600 | 1.4 | 卡片标题、分组标题 |
| `body`      | 14px | 400 | 1.55 | 正文、消息、列表（默认） |
| `caption`   | 12px | 400 | 1.5 | 时间戳、元信息、面板标签 |
| `micro`     | 11px | 500 | 1.4 | 徽标文字、状态标签、工具调用图标旁注 |
| `mono`      | 13px | 400 | 1.5 | 命令、参数、Token 数字 |
| `mono-micro`| 12px | 500 | 1.4 | 设备型号、版本号、压缩次数 |

> 字重只用 400 / 500 / 600 / 700 四档；正文禁加粗（强调用颜色而非字重）。

---

### 2.4 Spacing（间距阶梯）

| Token | Value | 用途 |
|-------|-------|------|
| `space-1` | 4px | 图标与文字内距、状态点 |
| `space-2` | 8px | 紧凑元素间距、按钮内距、徽标内距 |
| `space-3` | 12px | 默认组件内距、卡片内距（紧凑） |
| `space-4` | 16px | 默认面板内距、列表间距 |
| `space-6` | 24px | 区块间距、卡片内距（宽松）、输入区外距 |
| `space-8` | 32px | 面板间主分隔、对话框内距 |
| `space-12` | 48px | 大区块分隔、对话流首尾留白 |
| `space-16` | 64px | 空状态/欢迎区留白 |

**与现有 `UiTokens` 映射**（渐进迁移用）：
`space-1→SpaceXSmall(4)` · `space-2→SpaceSmall(8)` · `space-3→SpaceMedium(12)` · `space-4→SpaceLarge(16)` · `space-6→SpaceXXLarge(24)` · `space-8→SpaceXXXLarge(32)`。

---

### 2.5 Radius（圆角阶梯）

| Token | Value | 用途 |
|-------|-------|------|
| `radius-sm`   | 6px | 按钮、输入框、列表行、Tab、工具调用行 |
| `radius-md`   | 8px | 卡片、消息气泡（助手）、下拉 |
| `radius-lg`   | 12px | 大卡片、代码块、设备画面容器 |
| `radius-xl`   | 16px | 对话框、弹层（上限，不再加大） |
| `radius-full` | 999px | 徽标、开关、状态点、Tab 药丸 |

> 工具感红线：**组件圆角 ≤16px**；只有「状态性小元素」允许 full 圆角。避免 Material 风格的 28px 大圆角按钮。

---

### 2.6 Shadows / Elevation（阴影与层级）

工具感的关键：**浅阴影 + 边框为主**，深阴影会让桌面工具显「浮」。

| Level | Shadow | 用途 |
|-------|--------|------|
| `flat`    | `none` | 默认表面（卡片、面板） |
| `raised`  | `0 1px 2px rgba(15,18,25,.05), 0 1px 3px rgba(15,18,25,.04)` | 卡片 hover、可点击行 |
| `floating`| `0 4px 8px rgba(15,18,25,.08), 0 2px 4px rgba(15,18,25,.06)` | 下拉、命令面板、气泡弹层 |
| `overlay` | `0 12px 24px rgba(15,18,25,.12), 0 4px 8px rgba(15,18,25,.08)` | 对话框、全屏遮罩层 |

**Z-index 阶梯**：base `0` · dropdown `100` · sticky `200` · modal `300` · toast `400` · tooltip `500`

> Dark 主题下阴影几乎不可见，弹层/浮层请以 `border-strong` 描边 + 弱阴影区分，而非依赖投影。

---

### 2.7 Layout（布局）

| 项 | 值 | 说明 |
|----|----|------|
| 窗口最小 | **1280×800** | 低于此宽度进入紧凑模式 |
| 窗口基准 | **1440×900**（推荐默认） | 三栏完整展开的最佳比例 |
| 顶部状态栏 | **44px** 通栏 | 设备上下文常驻 |
| 左侧导航 | **220px** 固定 | <1080 折叠为 64px 图标栏 |
| 中央对话区 | 弹性，**≥480px** | 对话文本列居中，最大宽 **720px** |
| 右侧设备面板 | **400px**，可折叠 | <1280 压缩至 320px；<1080 隐藏/浮层 |
| 对话文本最大宽 | **720px** | 保持可读行长（55–75 字符） |

**断点**：
| 断点 | 宽度 | 行为 |
|------|------|------|
| XL | ≥1440 | 三栏完整（220 + 弹性 + 400） |
| L  | 1280–1439 | 设备面板 320px，其余弹性 |
| M  | 1080–1279 | 设备面板收为浮层/抽屉，导航折叠为图标栏 |
| S  | <1080 | 单对话区 + 图标导航，设备面板抽屉式 |

---

### 2.8 Components（组件规格）

> 单位为 dp/sp，可直接对照 Compose；括号内为对应现有 `UiTokens` 命名（如有）。

#### 2.8.1 Button 按钮

| 变体 | 背景 | 文字 | 边框 | 用途 |
|------|------|------|------|------|
| `primary` | Light `brand-700 #1976D2` / Dark `brand-500 #2196F3` | `#FFFFFF` | 无 | 主操作：发送、开始任务、确认执行 |
| `secondary` | `bg-1` | `text-primary` | `border` 1px | 次级操作：打开设置、新任务 |
| `ghost` | 透明 | `text-secondary`（hover `text-primary`） | 无 | 快捷按钮、图标按钮、折叠/展开 |
| `danger` | `danger #D93025`（Light）/ `#F2555A`（Dark） | `#FFFFFF` | 无 | 风险操作确认、终止任务 |

- 高度：**32px**（紧凑）/ **36px**（默认，`ControlHeight 40` 可沿用）
- 圆角：`radius-sm 6px`；内距：水平 12–16px
- 状态：hover 加深 8%（同色阶下一档）、pressed 再加深、disabled 用 `text-muted` + `bg-3`、focus 用 2px `brand` 焦点环（offset 2px）

#### 2.8.2 Input 输入框

- 高度：**40px**（对话输入区可 44px）；圆角 `radius-sm 6px`
- 背景 `bg-1`，描边 `border`，focus 描边 `brand` + 2px 焦点环（`brand` 30% 透明度）
- Placeholder：`text-muted`；文字：`text-primary`
- 附注/错误：下方 12px `caption`，错误色 `danger`

#### 2.8.3 Card 卡片

- 背景 `bg-1`，描边 `border` 1px，圆角 `radius-md 8px`（大卡 `radius-lg`）
- 内距 `space-4 16px`；默认无阴影（`flat`），hover 可 `raised`
- 卡片头：`subheading 15px/600` + 右侧 `caption` 元信息

#### 2.8.4 Badge 徽标

- 药丸形 `radius-full`，高度 20–22px，内距 6–10px
- 背景用「状态容器色」，文字 `micro 11px/500` 用对应深色字
- 示例：`已执行`（success-container）、`需确认`（warning-container）、`失败`（danger-container）、`changed`（info/brand 容器）、`BETA`（ai-container）

#### 2.8.5 Switch 开关（视觉观察模式）

- 轨道 36×20px，圆角 full；thumb 16px 圆
- 开启：轨道 `brand-500`，thumb `#FFFFFF`；关闭：轨道 `bg-3` + `border`
- 文字说明用 `caption` `text-tertiary`；开关旁可放 3px 状态点（观察中=蓝/绿）

#### 2.8.6 Tab 标签

- 紧凑型：高度 32px，`radius-sm`，激活态背景 `bg-3` + `text-primary`，未激活 `text-tertiary`
- 可选中底边 2px `brand`（Linear 风格）替代色块，二者选一，勿混用

#### 2.8.7 Message Bubble 消息气泡

| 角色 | 布局 | 背景 | 圆角 |
|------|------|------|------|
| 用户 | 右对齐，气泡式 | `brand-100`（Light）/ `brand-300` 30% 或 `bg-3`（Dark） | `radius-md`，右下角 `radius-sm` |
| 助手 | 左对齐，卡片式（无气泡感） | `bg-1` + `border` | `radius-lg` |

- 消息宽度上限 720px；头像 24px 圆，助手用 AI 紫底 + 白色「AI」标识，用户用品牌蓝底
- 时间戳 `caption` `text-tertiary`，置于气泡内右下或消息下方

#### 2.8.8 Tool Call Card 工具调用卡

- 结构：左侧 16px 图标（AI 紫）→ 工具名（`mono 13px`，如 `input tap`）→ 参数摘要（`mono-micro` `text-secondary`）→ 时间戳（`caption`）
- 状态点：待执行（`text-muted` 灰）/ 执行中（`brand` 蓝，可加脉冲）/ 成功（`success` 绿）/ 失败（`danger` 红）
- 默认**折叠**只显示一行；展开显示完整命令（`mono` 块 `bg-3`，`radius-sm`）、参数、返回摘要
- 背景 `ai-container` 8% 透明度 + `ai-border`，圆角 `radius-md`

#### 2.8.9 Thinking Chain Card 思考链卡

- 默认折叠为一行：「🧠 思考中 / 推理过程」+ 展开箭头；展开后显示推理文本（`body` `text-secondary`）
- 背景 `ai-container` 8% + `ai-border`；图标 AI 紫；动画仅在展开箭头/状态点，不做整卡动画
- 折叠态高度 ≤28px，避免淹没对话流

#### 2.8.10 Status Dot 状态点

- 尺寸：8px（标准）/ 6px（紧凑）；圆角 full
- 颜色映射：在线 `success` · 离线 `text-muted` · 执行中 `brand`（可 1.5s 呼吸动画）· 告警 `warning` · 错误 `danger`
- 可带 4px 同色 30% 光晕表示「活跃中」

#### 2.8.11 Token 计量条

- 轨道：高 4px，`bg-3`，圆角 full；填充：`brand-500`（或接近上限时 `warning`→`danger` 渐变）
- 上方一行：`caption` 左「45,449 / 128,000 tokens」右「压缩 0 次」；数字用 `mono-micro`
- 压缩次数 ≥1 时以 `warning-container` 徽标呈现

---

## 3. 在 QADB AI Agent 页面的应用建议

> 用上述令牌重塑 5 个核心区域（对齐 discovery-summary 三栏骨架）。

### 3.1 左侧导航（220px）
- 底色 `bg-2`，与对话区用 1px `divider` 分隔；Logo 区 44px，下方 8 个菜单项高度 36px、`radius-sm`
- 选中项：`bg-3` 底 + `text-primary` + 左侧 3px `brand-500` 指示条（`IndicatorWidth 4` 可沿用）；未选中 `text-tertiary`
- 底部「已连接设备卡」：`bg-1` + `border` 卡片，设备名 `body/600`，状态点 `success`
- 收起态（<1080）：仅图标，选中项背景 `brand-100` 30%

### 3.2 顶部状态栏（44px 通栏）
- 底色 `bg-2`，底边 1px `divider`；内容高度 44px
- 左：设备型号（`mono-micro`）+ Android 16 · API 36（`caption text-tertiary`）
- 右：Wi-Fi / 电量（`caption text-secondary`）+ ADB 状态（`success` 绿点 + `micro` 文字「ADB 在线」/ 灰点「离线」）

### 3.3 中央对话区
- 底色 `bg-1`；会话标题行：`heading 17px/600` + 右侧任务状态徽标（进行中 `brand` 容器 / 待确认 `warning-container`）
- 对话流按消息类型分层：普通消息 `Message Bubble`、工具调用 `Tool Call Card`、推理 `Thinking Chain Card`、任务进度 `Activity Card`（`bg-1`+`border`，内含步骤时间线）
- **信息密度**：思考链/工具调用默认折叠，仅保留一行；对话区底部预留 ≥80px 安全间距，保证输入区钉住后不被遮挡
- 空状态：`display 28px` 主文案 + `text-secondary` 说明 + 3 个「能力示例」快捷卡片

### 3.4 底部输入区（钉住视口底部）
- 容器：`bg-1` + 顶部 1px `divider`，内距 `space-4`
- 输入框：`radius-md`、`bg-1` + `border`，focus 品牌蓝焦点环；左侧快捷按钮（ghost：打开设置 / 截图识别），右侧发送按钮 `primary`
- 快捷按钮用 32px ghost 圆形图标按钮；「新任务」为 `secondary` 32px
- 危险指令输入时（如检测到「卸载/发送消息」关键词）：输入框上方浮出内联确认条（`danger-container` + `danger` 文字 + 确认/取消按钮）

### 3.5 右侧设备面板（400px）
- 底色 `bg-2`，左缘 1px `divider`；面板头：`subheading 15px/600`「设备画面」+ 收起按钮（ghost）
- **设备画面镜像**：容器 `bg-0` + `radius-lg`，顶部悬浮 `Page id`（`mono-micro` 深底白字 pill）+ `changed` 徽标（`warning-container`）
- **视觉观察模式**（核心 Beta）：大开关（`Switch`）+ 一行说明「AI 正在看你的屏幕」+ 状态点 + 最近观察时间 `caption`；开启时容器描边 `brand`
- **Token 消耗**：`Token 计量条` + 压缩次数徽标；底部收合按钮（ghost）腾出空间
- 画面帧率高、避免右侧重动效（实时流场景只做状态点呼吸，不做整区动画）

---

## 4. 与现有代码映射（Migration Map）

| 现有代码 | 当前值 | 建议令牌 | 备注 |
|---------|--------|---------|------|
| `Color.kt PrimaryBlue` | `#2962FF` | `brand-500 #2196F3`（或保留为品牌强调） | 统一到品牌基准，注意对比度规则 |
| `Color.kt InfoBlue` | `#2196F3` | `brand-500` | 已一致，作为品牌基准 |
| `Color.kt BackgroundLight` | `#F0F2F5` | `bg-0 #F6F7F9` | 略提亮，接近 Linear 底 |
| `Color.kt BackgroundDark` | `#121212` | `bg-0 #0F0F12` | 微调 |
| `QadbColors.textTertiary` | onSurfaceVariant 72% | `text-tertiary` | 语义对齐 |
| `QadbColors.success` | colorScheme.secondary | `success #1E8E3E / #3FB950` | 建议从 secondary 解耦为专用绿 |
| `QadbColors.danger` | colorScheme.error | `danger #D93025 / #F2555A` | 语义对齐 |
| `QadbColors.warning` | `#FF9F0A / #D97706` | `warning #B45309 / #F0A020` | 统一为令牌 |
| `QadbColors.purple` | `#BF5AF2 / #7C3AED` | `ai #7C3AED / #A78BFA` | 统一为 AI 强调色 |
| `UiTokens.TextBody` | 13sp | `body 14px`（可选） | 新页面用 14px，存量可保持 13sp 过渡 |
| `UiTokens.RadiusSmall/Medium/Large` | 6/8/12 | `radius-sm/md/lg` | 一致 |
| `UiTokens.SpaceXLarge` 20dp | — | `space-4 16px / space-6 24px` | 新阶梯引入 24/48/64 |

> 迁移策略：**新 AI Agent 页优先直接使用本令牌**（新增 `QadbTokens` 对象或扩展 `QadbColors`）；存量页面按需渐进替换，不一次性全量重构。

---

## 5. Agent Prompt Guide（生成指南）

### Key Instructions
- 始终使用「中性打底 + 蓝为点睛」：背景只用 bg-0/1/2/3 灰阶，QADB 蓝只用于操作/焦点/活动状态，禁止大面积蓝色块或蓝色渐变。
- 双色语义：蓝色=设备/品牌/操作，AI 紫=Agent 活动遥测（思考链、工具调用、Token、观察模式）。不要把 AI 紫用于普通按钮。
- 阴影克制：默认 `flat`，弹层最高 `overlay`；Dark 主题用描边而非投影分层。
- 组件默认折叠信息：思考链/工具调用永远先折叠成一行，可展开，避免淹没对话流。
- 状态语义必须明确：设备操作结果用「成功绿 / 失败红 / 进行中蓝」，不得含糊；ADB 在线/离线用状态点表达。
- 圆角 ≤16px；只有徽标/开关/状态点用 full 圆角。
- 正文 14px，数据（命令/Token/设备型号）用等宽 13px；正文不靠加粗强调，用颜色层级。

### Quick CSS Snippet（Web 原型速查）

```css
:root {
  --bg-0:#F6F7F9; --bg-1:#FFFFFF; --bg-2:#FBFCFD; --bg-3:#EFF1F4;
  --text-primary:#16181D; --text-secondary:#4B505A; --text-tertiary:#717684; --text-muted:#9AA0AC;
  --border:#E4E6EA; --border-strong:#D3D6DC; --divider:#EDEFF2;
  --brand:#2196F3; --brand-action:#1976D2; --brand-hover:#1565C0; --brand-soft:#E3F2FD;
  --success:#1E8E3E; --warning:#B45309; --danger:#D93025; --info:#1976D2;
  --ai:#7C3AED; --ai-soft:#EFEAFE;
  --font-ui:"Inter","SF Pro Text","Segoe UI","Roboto","Noto Sans SC","PingFang SC","Microsoft YaHei",sans-serif;
  --font-mono:"JetBrains Mono","SF Mono","Cascadia Code","Consolas","Roboto Mono",monospace;
  --space-1:4px; --space-2:8px; --space-3:12px; --space-4:16px; --space-6:24px; --space-8:32px; --space-12:48px; --space-16:64px;
  --radius-sm:6px; --radius-md:8px; --radius-lg:12px; --radius-xl:16px; --radius-full:999px;
  --shadow-raised:0 1px 2px rgba(15,18,25,.05),0 1px 3px rgba(15,18,25,.04);
  --shadow-floating:0 4px 8px rgba(15,18,25,.08),0 2px 4px rgba(15,18,25,.06);
  --shadow-overlay:0 12px 24px rgba(15,18,25,.12),0 4px 8px rgba(15,18,25,.08);
}
[data-theme="dark"] {
  --bg-0:#0F0F12; --bg-1:#141418; --bg-2:#1A1A1F; --bg-3:#212127;
  --text-primary:#ECEDEF; --text-secondary:#A9ADB5; --text-tertiary:#80858F; --text-muted:#565B64;
  --border:#26262C; --border-strong:#35353D; --divider:#1E1E23;
  --brand-action:#42A5F5; --brand-hover:#64B5F6;
  --success:#3FB950; --warning:#F0A020; --danger:#F2555A; --info:#42A5F5;
  --ai:#A78BFA; --ai-soft:#241A3D;
}
```

### Compose 速查（Kotlin）

```kotlin
// QadbTokens（建议新增，双主题切换时取对应组）
object QadbTokens {
    // Colors
    val Bg0 = Color(0xFFF6F7F9)      // Dark: 0xFF0F0F12
    val Bg1 = Color(0xFFFFFFFF)      // Dark: 0xFF141418
    val Bg2 = Color(0xFFFBFCFD)      // Dark: 0xFF1A1A1F
    val Bg3 = Color(0xFFEFF1F4)      // Dark: 0xFF212127
    val TextPrimary = Color(0xFF16181D)   // Dark: 0xFFECEDEF
    val TextSecondary = Color(0xFF4B505A) // Dark: 0xFFA9ADB5
    val TextTertiary = Color(0xFF717684)  // Dark: 0xFF80858F
    val TextMuted = Color(0xFF9AA0AC)     // Dark: 0xFF565B64
    val Border = Color(0xFFE4E6EA)   // Dark: 0xFF26262C
    val Divider = Color(0xFFEDEFF2)  // Dark: 0xFF1E1E23
    val Brand = Color(0xFF2196F3)
    val BrandAction = Color(0xFF1976D2)   // Dark: 0xFF42A5F5
    val Success = Color(0xFF1E8E3E)  // Dark: 0xFF3FB950
    val Warning = Color(0xFFB45309)  // Dark: 0xFFF0A020
    val Danger = Color(0xFFD93025)   // Dark: 0xFFF2555A
    val Ai = Color(0xFF7C3AED)       // Dark: 0xFFA78BFA
    // Spacing / Radius / FontSize：对齐 UiTokens 既有对象即可
}
```

---

*本设计令牌由设计系统专家彩格调生成，供原型/视觉/开发执行阶段使用。若后续补充品牌手册或交互稿，可在此文档上迭代。*
