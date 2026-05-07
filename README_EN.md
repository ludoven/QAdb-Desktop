# QADB 2.0.1

<p align="center">
  <img src="composeApp/src/desktopMain/composeResources/drawable/ic_logo.png" width="120" alt="QADB Logo"/>
</p>

<p align="center">
  A cross-platform ADB GUI tool based on <b>Jetpack Compose Multiplatform</b>, with support for <b>Windows</b> and <b>macOS</b>.
</p>

<p align="center">
  <a href="README.md">中文</a> | <a href="README_EN.md">English</a>
</p>

---

## Overview

QADB wraps common ADB workflows in a desktop GUI for daily debugging, device management, app management, log inspection, and command execution. After connecting an Android device, you can view device details, run common commands, manage installed apps, filter logs, and execute terminal commands from one window.

## Features

- **Device Home**: Detect online devices and show basic device information, connection status, and runtime status.
- **Common Operations**: Run frequent ADB actions such as reboot, shutdown, screenshot, screen recording, file push, and file pull.
- **App Management**: View installed apps, install or uninstall apps, clear app data, export APK files, and inspect app details.
- **Key Events**: Send common Android key events quickly without repeatedly typing commands.
- **Logcat Viewer**: View and filter Logcat output to help diagnose runtime issues.
- **Terminal Commands**: Execute ADB commands inside the app while keeping the flexibility of direct command usage.

## Interface Preview

| Home | Common Operations |
|------|-------------------|
| ![Home](home.png) | ![Common Operations](common.png) |

| App List | App Details |
|----------|-------------|
| ![App List](applist.png) | ![App Details](appinfo.png) |

| Key Events | Logcat |
|------------|--------|
| ![Key Events](keyevent.png) | ![Logcat](logcat.png) |

| Terminal |
|----------|
| ![Terminal](terminal.png) |

## Quick Start

1. Install [ADB](https://developer.android.com/tools/adb) and make sure `adb` is available from the command line.
2. Connect an Android device, enable USB debugging, and authorize the current computer.
3. Launch QADB, select a device, and run the operation you need.

## Download

Go to [GitHub Releases](https://github.com/ludoven/QAdb-Desktop/releases) to get the latest package.

- Windows: `QAdb.exe` / `QAdb.msi`
- macOS: `QAdb.dmg`

## Local Development

Run the desktop app:

```bash
./gradlew :composeApp:run
```

Run desktop tests:

```bash
./gradlew :composeApp:desktopTest
```

Package a distribution for the current OS:

```bash
./gradlew :composeApp:packageDistributionForCurrentOS
```

## Feedback

Please submit bug reports or feature requests through [Issues](https://github.com/ludoven/QAdb-Desktop/issues).

## Disclaimer

This project only provides a GUI wrapper for ADB. It does not include or modify ADB itself. Runtime usage depends on the `adb` installed in the system environment.
