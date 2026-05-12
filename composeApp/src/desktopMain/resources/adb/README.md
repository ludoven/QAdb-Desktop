Bundled ADB files should be placed here before packaging QADB.

Expected layout:

```text
adb/
├── macos/
│   ├── arm64/adb
│   └── x64/adb
├── windows/
│   ├── adb.exe
│   ├── AdbWinApi.dll
│   └── AdbWinUsbApi.dll
└── linux/
    └── adb
```

Use matching files from Android SDK Platform-Tools. On macOS and Linux,
the `adb` file must be executable.
