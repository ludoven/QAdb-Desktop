# 功能亮点

QADB 将高频 ADB 工作流拆成清晰模块，覆盖设备连接、应用管理、日志查看、按键模拟和命令执行。

<div class="module-grid">
  <div class="module-card">
    <h3>首页</h3>
    <p>展示设备状态、快捷入口和常用操作，进入应用后先确认当前设备是否可用。</p>
  </div>
  <div class="module-card">
    <h3>常用命令</h3>
    <p>把截图、录屏、重启、打开设置、查看 Activity 等命令整理成可点击动作。</p>
  </div>
  <div class="module-card">
    <h3>终端</h3>
    <p>内置命令行终端，可直接执行 adb 或 shell 命令。</p>
  </div>
  <div class="module-card">
    <h3>按键</h3>
    <p>模拟 Android 返回、主页、菜单、音量、方向键等 KeyEvent。</p>
  </div>
  <div class="module-card">
    <h3>应用</h3>
    <p>查看应用列表，支持安装、卸载、清除数据、强制停止和导出 APK。</p>
  </div>
  <div class="module-card">
    <h3>日志</h3>
    <p>查看和筛选 Logcat 日志，辅助定位应用运行问题。</p>
  </div>
  <div class="module-card">
    <h3>诊断</h3>
    <p>在统一工作区切换日志与进程，支持进程搜索、排序和资源占用查看。</p>
  </div>
  <div class="module-card">
    <h3>AI Agent Beta</h3>
    <p>默认关闭；使用单一视觉 Agent 根据每一步最新截图执行一个受控动作，UI 结构只作可选提示。</p>
  </div>
</div>

## AI Agent 执行边界

- 生产入口只使用截图主导的单引擎，不在 V1、V2、Bridge 或 Workflow 之间选择和回退。
- 每个模型决策都必须包含最新设备截图；模型需通过 L3 视觉与工具调用能力测试。
- 模型只能选择打开应用、点击、输入、滑动、按键、等待、完成、询问用户或阻塞，不接收任意 shell/ADB 指令。
- 点击和滑动使用 `0..1000` 归一化坐标，由本地执行层映射到真实设备分辨率并校验截图 revision。
- 执行20步后进入软限制，在第20、25、30、35步检查实际页面变化和目标进展；第40步后强制交还用户。
- 危险操作由本地策略确认；结果不明确的发送、删除或购买动作不会自动重放。

## 界面预览

<div class="screenshot-strip">
  <img src="/screenshots/home.png" alt="首页截图">
  <img src="/screenshots/common.png" alt="常用命令截图">
  <img src="/screenshots/keyevent.png" alt="按键模拟截图">
  <img src="/screenshots/terminal.png" alt="终端截图">
</div>

## 规划中

- 性能面板：查看 CPU、内存、网络等设备指标。
- 更丰富的命令中心：沉淀更多高频调试动作。
