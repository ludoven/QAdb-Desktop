# 快速开始

## 1. 安装 ADB

QADB 依赖系统中的 `adb` 命令。你可以通过 Android Studio 安装 Android SDK Platform Tools，也可以单独安装 Platform Tools。

安装完成后，在终端执行：

```bash
adb version
```

如果能看到 ADB 版本信息，说明本机环境正常。

## 2. 开启设备调试

在 Android 设备上完成这些步骤：

1. 打开「开发者选项」。
2. 开启「USB 调试」。
3. 使用 USB 连接电脑。
4. 在设备上允许 USB 调试授权。

然后执行：

```bash
adb devices
```

如果设备列表中出现目标设备，说明连接成功。

## 3. 启动 QADB

打开应用后：

1. 在设备列表中选择当前设备。
2. 查看首页设备状态。
3. 使用常用操作、应用管理、日志和终端等功能。

## 网络 ADB

调试 Android TV、电视盒子或局域网设备时，可以使用网络 ADB：

```bash
adb connect 192.168.1.100:5555
```

连接成功后，设备会出现在 QADB 的设备列表中。不同设备开启网络 ADB 的方式可能不同，部分设备需要先通过 USB 或系统设置开启无线调试。

## 常见问题

### 为什么检测不到设备？

请确认：

- 已安装 Android Platform Tools。
- `adb devices` 可以在终端正常执行。
- 手机或设备已开启 USB 调试。
- 设备已允许 USB 调试授权。
- USB 数据线支持数据传输。
- Windows 用户已安装对应设备驱动。
