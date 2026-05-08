<h1 align="center">QADB</h1>

<p align="center">
  <b>An open-source, cross-platform, modern ADB desktop debugging tool</b>
</p>

<p align="center">
  Make common ADB operations more intuitive and efficient, without repeatedly typing commands.
</p>

<p align="center">
  <a href="https://ludoven.github.io/QADB/">Website</a> |
  <a href="./README.md">中文</a> |
  <a href="./README_EN.md">English</a>
</p>

<p align="center">
  <img src="https://img.shields.io/github/stars/ludoven/QADB?style=flat-square" alt="GitHub Stars" />
  <img src="https://img.shields.io/github/forks/ludoven/QADB?style=flat-square" alt="GitHub Forks" />
  <img src="https://img.shields.io/github/v/release/ludoven/QADB?style=flat-square" alt="GitHub Release" />
  <img src="https://img.shields.io/github/license/ludoven/QADB?style=flat-square" alt="License" />
</p>

---

## 🚀 Project Overview

**QADB** is a cross-platform ADB GUI tool built with **Jetpack Compose Multiplatform**, supporting **Windows** and **macOS**.

It is designed for Android developers, testers, Android TV / TV box debugging users, and advanced users who frequently use ADB commands. It turns common ADB workflows into a visual interface so you can debug devices, manage apps, inspect logs, take screenshots and recordings, run terminal commands, and simulate key events more efficiently.

If you often run commands like:

```bash
adb devices
adb install app.apk
adb shell pm clear package.name
adb shell am force-stop package.name
adb logcat
adb shell screencap
adb shell input keyevent 3
```

QADB can make these tasks much easier.

---

## 📸 Interface Preview

![Home](./screenshots/home.png)
![Apps](./screenshots/applist.png)
![Logcat](./screenshots/logcat.png)
![Terminal](./screenshots/terminal.png)

---

## ✨ Key Features

- **Device Management**: Detect USB / network ADB devices and quickly switch the current target device
- **Device Info**: View model, Android version, screen information, connection status, and more
- **App Management**: Install APKs, uninstall apps, clear data, force stop apps, and export APKs
- **Common Operations**: Reboot, shutdown, screenshot, screen recording, open settings, inspect current Activity, etc.
- **Key Event Simulation**: Quickly trigger common key events such as Back, Home, Menu, Volume, and D-pad keys
- **Built-in Terminal**: Run adb / shell commands directly inside the app
- **Log Viewer**: View and filter Logcat logs to troubleshoot runtime issues
- **Command Center**: Organize frequently used ADB commands into visual actions
- **TV / Box Debugging**: Suitable for Android TV, set-top boxes, and system app debugging scenarios
- **Cross-Platform**: Built on Compose Multiplatform, supports Windows and macOS

---

## 🎯 Target Users

QADB is suitable for:

- Android developers
- Android testers
- Android TV / TV box debugging engineers
- System app / preinstalled app debugging engineers
- Advanced users who frequently use ADB commands
- Development teams that want to improve debugging efficiency

---

## 🧩 Functional Modules

| Module | Description |
|---|---|
| Home | Device status, quick entries, and common actions |
| Common | Quick execution for frequently used ADB commands |
| Terminal | Built-in command terminal for adb / shell commands |
| Key Events | Simulate Android device key operations |
| Apps | App list, install, uninstall, clear data, force stop, etc. |
| Logs | View and filter Logcat logs |
| Settings | Configure tool behavior, ADB path, and related options |
| Performance | Planned: CPU, memory, network, and related metrics |
| Processes | Planned: View and manage device processes |

---

## 📦 Download

Please go to [GitHub Releases](https://github.com/ludoven/QADB/releases) to download the latest version.

| Platform | Recommended Package | Notes |
|---|---|---|
| Windows | `.msi` | Recommended for regular users |
| Windows | `.exe` | Portable / no installation |
| Windows | `.zip` | Extract and run (portable package) |
| macOS | `.dmg` | For macOS users |

> If macOS shows "cannot verify developer" or "cannot open", allow it in "System Settings" -> "Privacy & Security".

---

## ⚡ Quick Start

### 1. Install ADB

QADB depends on the `adb` command available in your system.

Install Android Platform Tools first:

- Android Studio users usually already have ADB
- You can also download Android SDK Platform Tools separately

After installation, make sure this command works:

```bash
adb version
```

If you can see the ADB version output, your environment is ready.

---

### 2. Enable Device Debugging

On your Android device:

1. Open "Developer options"
2. Enable "USB debugging"
3. Connect the device via USB
4. Allow USB debugging authorization on the device

Then run:

```bash
adb devices
```

If the device list appears, the connection is successful.

---

### 3. Launch QADB

After opening QADB:

1. Select the current device
2. Check device status
3. Use common actions, app management, logs, terminal, and other features

---

## 🔌 Network ADB

If you need to connect Android TV, TV boxes, or LAN devices, you can use network ADB.

Common connection command:

```bash
adb connect 192.168.1.100:5555
```

After a successful connection, the device will appear in the QADB device list.

> The way to enable network ADB varies by device. Some devices require USB or system settings to enable wireless debugging first.

---

## 🛠️ Common Scenarios

### Android App Development

- Quickly install APKs
- Clear app data
- Force stop apps
- Inspect app logs
- View current Activity
- Take screenshots and recordings

### Android TV / TV Box Debugging

- Connect via network ADB
- Simulate remote control key events
- Install or uninstall APKs
- View system app information
- Debug settings pages or launcher/startup pages

### QA / Testing

- Quickly switch devices
- Execute common commands in batches
- View logs and screenshots
- Collect information quickly during issue reproduction

---

## ❓ FAQ

### 1. Why is my device not detected?

Please confirm:

- Android Platform Tools are installed
- `adb devices` works in your terminal
- USB debugging is enabled on the device
- USB debugging authorization is accepted on the device
- Your USB cable supports data transfer
- Windows users have installed the correct device drivers

---

### 2. Why does it say adb is not found?

Please ensure `adb` is added to your system `PATH`.

You can run:

```bash
adb version
```

If the command is not found, install Android Platform Tools first and add its directory to your environment variables.

---

### 3. What should I do if macOS says the app cannot be opened?

If macOS cannot verify the developer, try:

1. Open "System Settings"
2. Go to "Privacy & Security"
3. Find the blocked prompt for QADB
4. Click "Open Anyway"

---

### 4. Does QADB include ADB?

Currently, QADB relies on ADB installed in your system.

Possible future support:

- Auto-detect ADB path
- Manually configure ADB path
- Built-in Platform Tools
- Multi-version ADB management

---

### 5. Is Linux supported?

Currently the main supported platforms are Windows and macOS.

Linux support can be a future plan. If you have experience with Linux packaging or adaptation, contributions are welcome.

---

## 🗺️ Roadmap

- [x] Device management
- [x] Device information display
- [x] Common ADB operations
- [x] App management
- [x] Key event simulation
- [x] Screenshot / screen recording
- [x] Built-in terminal
- [x] Log viewer
- [ ] Performance monitoring
- [ ] Process management
- [ ] File management
- [ ] Command favorites
- [ ] Custom command grouping
- [ ] Multi-device batch operations
- [ ] Visual ADB path configuration
- [ ] Linux support
- [ ] Plugin-based command extensions

---

## 🤝 Contributing

Issues, feature suggestions, and Pull Requests are welcome.

You can contribute by:

- Reporting bugs
- Submitting useful ADB commands
- Improving UI / interaction design
- Enhancing Windows / macOS compatibility
- Improving English documentation
- Adding usage tutorials
- Helping with Linux packaging and adaptation

If you have frequently used ADB commands, feel free to suggest them and help improve the QADB command center.

---

## 🧪 Local Development

### Requirements

- JDK 17 or above
- Android Studio / IntelliJ IDEA
- Gradle
- Android SDK Platform Tools

### Clone Project

```bash
git clone https://github.com/ludoven/QADB.git
cd QADB
```

### Run Project

Open the project with Android Studio or IntelliJ IDEA and run the Desktop configuration.

> Specific Gradle tasks may change with project structure updates. Please refer to the actual project configuration.

---

## 📄 License

This project is released under an open-source license. See [LICENSE](./LICENSE) for details.

---

## ⭐ Star Support

If QADB helps you, please consider giving this project a Star.

Your Star is an important motivation for continuous updates.

<p align="center">
  <b>QADB: Make Android debugging easier.</b>
</p>
