# 下载

请前往 GitHub Releases 下载最新版本：

[打开 Releases](https://github.com/ludoven/QAdb-Desktop/releases)

## 推荐安装包

| 平台 | 推荐安装包 | 说明 |
| --- | --- | --- |
| Windows | `.msi` | 推荐普通用户使用 |
| Windows | `.exe` | 免安装 / 便携版本 |
| macOS | `.dmg` | 适用于 macOS 用户 |

## macOS 打开提示

如果 macOS 提示“无法验证开发者”或“无法打开”，可以在「系统设置」->「隐私与安全性」中允许打开。

## 从源码构建

如果你想从源码运行桌面应用，可以在项目根目录执行：

```bash
./gradlew :composeApp:run
```

如果只是预览官网，可以执行：

```bash
npm run docs:dev
```
