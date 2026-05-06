package com.ludoven.adbtool.viewmodel

import adbtool_desktop.composeapp.generated.resources.Res
import adbtool_desktop.composeapp.generated.resources.dialog_clear_and_restart_success
import adbtool_desktop.composeapp.generated.resources.dialog_export_failed
import adbtool_desktop.composeapp.generated.resources.dialog_export_success
import adbtool_desktop.composeapp.generated.resources.dialog_get_install_path_failed
import adbtool_desktop.composeapp.generated.resources.dialog_no_export_path
import adbtool_desktop.composeapp.generated.resources.dialog_operation_failed
import adbtool_desktop.composeapp.generated.resources.dialog_reset_permissions_and_restart_success
import adbtool_desktop.composeapp.generated.resources.dialog_restart_success
import adbtool_desktop.composeapp.generated.resources.dialog_uninstall_failed
import adbtool_desktop.composeapp.generated.resources.dialog_uninstall_success
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.lifecycle.viewModelScope
import com.ludoven.adbtool.entity.AdbFunctionType
import com.ludoven.adbtool.entity.MsgContent
import com.ludoven.adbtool.entity.AppInfoData
import com.ludoven.adbtool.pages.AppInfo
import com.ludoven.adbtool.pages.getInstalledApps
import com.ludoven.adbtool.util.AdbPathManager
import com.ludoven.adbtool.util.AdbTool
import com.ludoven.adbtool.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.ZipFile


class AppViewModel : BaseViewModel() {

    private val _appInfo = MutableStateFlow<AppInfoData?>(null)
    val appInfo: StateFlow<AppInfoData?> = _appInfo.asStateFlow()

    private val _appList = MutableStateFlow<List<AppInfo>>(emptyList())
    val appList: StateFlow<List<AppInfo>> = _appList

    private val _searchText = MutableStateFlow("")
    val searchText = _searchText.asStateFlow()

    private val _selectedTab = MutableStateFlow("全部应用")
    val selectedTab = _selectedTab.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // View mode: true = grid, false = list
    private val _isGridView = MutableStateFlow(false)
    val isGridView = _isGridView.asStateFlow()

    // Icon cache: packageName -> ImageBitmap
    private val _appIcons = MutableStateFlow<Map<String, ImageBitmap>>(emptyMap())
    val appIcons = _appIcons.asStateFlow()

    // Label cache: packageName -> app label
    private val _appLabels = MutableStateFlow<Map<String, String>>(emptyMap())
    private val loadingPackages = mutableSetOf<String>()
    private val loadingDetailPackages = mutableSetOf<String>()
    private var activeCacheDeviceKey: String = ""

    // Local persistent cache root, split by device id.
    private val iconCacheRootDir = File(System.getProperty("user.home"), ".qadb/icons").also { it.mkdirs() }

    fun clearAppInfo() {
        _appInfo.value = null
    }

    fun toggleViewMode() {
        _isGridView.value = !_isGridView.value
    }

    fun setGridView(enabled: Boolean) {
        _isGridView.value = enabled
    }

    fun getAppList() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                refreshCacheContextIfNeeded()
                val list = getInstalledApps()
                _appList.value = list
                hydrateCachedLabels(list)
                refreshRunningStatus()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun ensureAppAssetsVisible(packageNames: List<String>) {
        val distinctPackages = packageNames.distinct()
        if (distinctPackages.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            refreshCacheContextIfNeeded()
            for (packageName in distinctPackages) {
                if (_appIcons.value.containsKey(packageName) && _appLabels.value.containsKey(packageName)) continue
                if (!claimLoading(packageName)) continue
                try {
                    val app = _appList.value.firstOrNull { it.packageName == packageName } ?: continue
                    ensureAppAssets(app)
                } catch (_: Exception) {
                    // Silently skip failed icons
                } finally {
                    releaseLoading(packageName)
                }
            }
        }
    }

    fun ensureAppDetailsVisible(packageNames: List<String>) {
        val distinctPackages = packageNames.distinct()
        if (distinctPackages.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            refreshCacheContextIfNeeded()
            for (packageName in distinctPackages) {
                val app = _appList.value.firstOrNull { it.packageName == packageName } ?: continue
                val hasDetails = app.versionName != "-" && app.installTime != "-" && app.size != "-"
                if (hasDetails) continue
                if (!claimDetailLoading(packageName)) continue
                try {
                    loadSingleAppDetail(packageName)
                } catch (_: Exception) {
                    // Skip single package failures to avoid blocking list interaction.
                } finally {
                    releaseDetailLoading(packageName)
                }
            }
        }
    }

    private fun refreshCacheContextIfNeeded() {
        val currentKey = currentDeviceCacheKey()
        if (activeCacheDeviceKey == currentKey) return

        activeCacheDeviceKey = currentKey
        _appIcons.value = emptyMap()
        _appLabels.value = emptyMap()
        synchronized(loadingPackages) {
            loadingPackages.clear()
        }
        synchronized(loadingDetailPackages) {
            loadingDetailPackages.clear()
        }
    }

    private fun currentDeviceCacheDir(): File {
        val dir = File(iconCacheRootDir, currentDeviceCacheKey())
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun currentDeviceCacheKey(): String {
        val raw = AdbTool.selectDeviceId?.trim().orEmpty().ifBlank { "default" }
        return raw.replace(Regex("[^A-Za-z0-9._-]"), "_")
    }

    private fun claimLoading(packageName: String): Boolean = synchronized(loadingPackages) {
        if (loadingPackages.contains(packageName)) return@synchronized false
        loadingPackages.add(packageName)
        true
    }

    private fun releaseLoading(packageName: String) = synchronized(loadingPackages) {
        loadingPackages.remove(packageName)
    }

    private fun claimDetailLoading(packageName: String): Boolean = synchronized(loadingDetailPackages) {
        if (loadingDetailPackages.contains(packageName)) return@synchronized false
        loadingDetailPackages.add(packageName)
        true
    }

    private fun releaseDetailLoading(packageName: String) = synchronized(loadingDetailPackages) {
        loadingDetailPackages.remove(packageName)
    }

    private fun hydrateCachedLabels(list: List<AppInfo>) {
        val labels = mutableMapOf<String, String>()
        for (app in list) {
            val label = readLabelCache(app.packageName) ?: continue
            labels[app.packageName] = label
        }
        if (labels.isEmpty()) return

        _appLabels.value = _appLabels.value.toMutableMap().apply { putAll(labels) }
        _appList.value = _appList.value.map { app ->
            labels[app.packageName]?.let { label -> app.copy(appName = label) } ?: app
        }
    }

    private fun ensureAppAssets(app: AppInfo) {
        val packageName = app.packageName
        val cacheDir = currentDeviceCacheDir()
        val iconCacheFile = File(cacheDir, "$packageName.png")
        val labelCacheFile = File(cacheDir, "$packageName.label.txt")

        if (!_appLabels.value.containsKey(packageName)) {
            readLabelCache(packageName)?.let { updateAppLabel(packageName, it) }
        }
        if (!_appIcons.value.containsKey(packageName)) {
            loadIconFromCache(packageName, iconCacheFile)
        }
        if (_appLabels.value.containsKey(packageName) && _appIcons.value.containsKey(packageName)) return

        if (app.apkPath.isBlank()) return
        val tmpApk = File(cacheDir, "$packageName.apk")
        val pulled = AdbTool.pullFile(app.apkPath, tmpApk.absolutePath)
        if (!pulled) {
            tmpApk.delete()
            return
        }

        try {
            if (!_appLabels.value.containsKey(packageName)) {
                extractLabelFromApk(tmpApk)?.let { label ->
                    updateAppLabel(packageName, label)
                    runCatching { labelCacheFile.writeText(label) }
                }
            }

            if (!_appIcons.value.containsKey(packageName)) {
                extractIconFromApk(tmpApk)?.let { iconBytes ->
                    runCatching { iconCacheFile.writeBytes(iconBytes) }
                    loadIconFromCache(packageName, iconCacheFile)
                }
            }
        } finally {
            tmpApk.delete()
        }
    }

    private fun readLabelCache(packageName: String): String? {
        val labelCacheFile = File(currentDeviceCacheDir(), "$packageName.label.txt")
        if (!labelCacheFile.exists()) return null
        val text = runCatching { labelCacheFile.readText().trim() }.getOrNull().orEmpty()
        return text.takeIf { it.isNotEmpty() }
    }

    private fun loadIconFromCache(packageName: String, iconFile: File) {
        if (!iconFile.exists() || iconFile.length() <= 0L) return
        runCatching {
            val skiaImage = Image.makeFromEncoded(iconFile.readBytes())
            val bitmap = skiaImage.toComposeImageBitmap()
            _appIcons.value = _appIcons.value.toMutableMap().apply { put(packageName, bitmap) }
        }
    }

    private fun updateAppLabel(packageName: String, label: String) {
        val cleanLabel = label.trim()
        if (cleanLabel.isEmpty()) return
        val old = _appLabels.value[packageName]
        if (old == cleanLabel) return

        _appLabels.value = _appLabels.value.toMutableMap().apply { put(packageName, cleanLabel) }
        _appList.value = _appList.value.map { app ->
            if (app.packageName == packageName) app.copy(appName = cleanLabel) else app
        }
    }

    private fun extractLabelFromApk(apkFile: File): String? {
        val output = runAaptBadging(apkFile) ?: return null
        val labelPatterns = listOf(
            Regex("""application-label-zh-CN:'([^']+)'"""),
            Regex("""application-label-zh_CN:'([^']+)'"""),
            Regex("""application-label-zh:'([^']+)'"""),
            Regex("""application-label:'([^']+)'"""),
            Regex("""application-label-[^:]+:'([^']+)'""")
        )

        return labelPatterns.asSequence()
            .mapNotNull { it.find(output)?.groupValues?.getOrNull(1)?.trim() }
            .firstOrNull { it.isNotEmpty() }
    }

    private fun runAaptBadging(apkFile: File): String? {
        val osName = System.getProperty("os.name").lowercase()
        val aaptName = if (osName.contains("windows")) "aapt.exe" else "aapt"
        val candidates = buildList {
            add(aaptName)
            val adbPath = AdbPathManager.currentAdbPath
            if (!adbPath.isNullOrBlank()) {
                val sdkRoot = File(adbPath).parentFile?.parentFile
                val buildToolsDir = sdkRoot?.resolve("build-tools")
                buildToolsDir?.listFiles()
                    ?.filter { it.isDirectory }
                    ?.sortedByDescending { it.name }
                    ?.forEach { dir ->
                        val file = dir.resolve(aaptName)
                        if (file.exists() && file.canExecute()) add(file.absolutePath)
                    }
            }
        }.distinct()

        for (candidate in candidates) {
            val output = runCatching {
                val process = ProcessBuilder(candidate, "dump", "badging", apkFile.absolutePath)
                    .redirectErrorStream(true)
                    .start()
                val text = process.inputStream.bufferedReader().readText()
                process.waitFor()
                if (process.exitValue() == 0) text else null
            }.getOrNull()
            if (!output.isNullOrBlank()) return output
        }
        return null
    }

    private fun extractIconFromApk(apkFile: File): ByteArray? {
        return try {
            ZipFile(apkFile).use { zip ->
                val entries = zip.entries().toList()

                val rasterEntry = entries
                    .filter { entry ->
                    val name = entry.name.lowercase()
                    (name.startsWith("res/mipmap") || name.startsWith("res/drawable")) &&
                    (name.endsWith(".png") || name.endsWith(".webp")) &&
                    (name.contains("ic_launcher") || name.contains("launcher_foreground") || name.contains("app_icon") || name.contains("icon") || name.contains("logo"))
                    }
                    .minByOrNull { entry ->
                    val name = entry.name.lowercase()
                    when {
                        name.contains("ic_launcher.png") -> 0
                        name.contains("launcher_foreground") -> 1
                        name.contains("xxxhdpi") -> 0
                        name.contains("xxhdpi") -> 2
                        name.contains("xhdpi") -> 3
                        name.contains("hdpi") -> 4
                        name.contains("mdpi") -> 5
                        else -> 6
                    }
                }

                if (rasterEntry != null) {
                    return@use zip.getInputStream(rasterEntry).readBytes()
                }

                val adaptiveXml = entries.firstOrNull { entry ->
                    val name = entry.name.lowercase()
                    (name.startsWith("res/mipmap") || name.startsWith("res/drawable")) &&
                    name.endsWith(".xml") &&
                    name.contains("ic_launcher")
                } ?: return@use null

                val xmlContent = zip.getInputStream(adaptiveXml).bufferedReader().use { it.readText() }
                val refRegex = Regex("""android:drawable=\"@((?:mipmap|drawable)/[A-Za-z0-9_]+)\"""")
                val resourceRefs = refRegex.findAll(xmlContent).map { it.groupValues[1] }.toList()
                val densityOrder = listOf("xxxhdpi", "xxhdpi", "xhdpi", "hdpi", "mdpi", "anydpi")

                for (ref in resourceRefs) {
                    val type = ref.substringBefore("/")
                    val name = ref.substringAfter("/")
                    val candidate = densityOrder
                        .asSequence()
                        .mapNotNull { density ->
                            entries.firstOrNull { entry ->
                                val entryName = entry.name.lowercase()
                                entryName.startsWith("res/${type.lowercase()}-$density/$name".lowercase()) &&
                                    (entryName.endsWith(".png") || entryName.endsWith(".webp"))
                            }
                        }
                        .firstOrNull()
                    if (candidate != null) {
                        return@use zip.getInputStream(candidate).readBytes()
                    }
                }
                null
            }
        } catch (_: Exception) { null }
    }

    private fun refreshRunningStatus() {
        val runningProcesses = AdbTool.exec("dumpsys activity processes")
        val runningPackages = runningProcesses.lines().mapNotNull { line ->
            val match = Regex("ProcessRecord\\{.+?:(.+?)/").find(line)
            match?.groupValues?.get(1)?.substringBefore(":")
        }.toSet()

        if (runningPackages.isEmpty()) return

        _appList.value = _appList.value.map { app ->
            app.copy(isRunning = runningPackages.contains(app.packageName))
        }
    }

    private fun loadSingleAppDetail(packageName: String) {
        val app = _appList.value.firstOrNull { it.packageName == packageName } ?: return
        val dumpsys = AdbTool.exec("dumpsys package $packageName")
        val versionName = Regex("versionName=([^\\s]+)").find(dumpsys)?.groupValues?.get(1) ?: app.versionName
        val firstInstallTime = Regex("firstInstallTime=([^\n]+)").find(dumpsys)?.groupValues?.get(1)?.trim() ?: app.installTime
        val installTimestamp = parseInstallTimestamp(firstInstallTime)
        val packagePath = app.apkPath.takeIf { it.isNotBlank() }
            ?: AdbTool.exec("pm path $packageName").lineSequence()
                .firstOrNull { it.startsWith("package:") }
                ?.removePrefix("package:")
                ?.trim()
                .orEmpty()

        val sizeBytes = queryPackageSizeBytes(packagePath, packageName) ?: app.sizeBytes
        val sizeText = sizeBytes?.let { formatSize(it) } ?: app.size

        _appList.value = _appList.value.map { current ->
            if (current.packageName == packageName) {
                current.copy(
                    versionName = versionName,
                    installTime = firstInstallTime,
                    size = sizeText,
                    sizeBytes = sizeBytes,
                    installTimestamp = installTimestamp
                )
            } else {
                current
            }
        }
    }

    fun setSearchText(text: String) {
        _searchText.value = text
    }

    fun setSelectedTab(tab: String) {
        _selectedTab.value = tab
    }

    fun executeAdbAction(type: AdbFunctionType, packageName: String) {
        val appName = _appList.value.find { it.packageName == packageName }?.appName ?: packageName
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    when (type) {
                        AdbFunctionType.UNINSTALL -> {
                            val result = AdbTool.exec("pm uninstall $packageName")
                            if (result.contains("Success")) {
                                withContext(Dispatchers.Main) {
                                    getAppList()
                                    showTipDialog(MsgContent.Resource(Res.string.dialog_uninstall_success))
                                }
                            } else {
                                showTipDialog(MsgContent.Resource(Res.string.dialog_uninstall_failed))
                            }
                        }
                        AdbFunctionType.LAUNCH -> {
                            AdbTool.exec("monkey -p $packageName -c android.intent.category.LAUNCHER 1")
                            _appList.value = _appList.value.map { app ->
                                if (app.packageName == packageName) app.copy(isRunning = true) else app
                            }
                        }
                        AdbFunctionType.FORCE_STOP -> {
                            AdbTool.exec("am force-stop $packageName")
                            _appList.value = _appList.value.map { app ->
                                if (app.packageName == packageName) app.copy(isRunning = false) else app
                            }
                        }
                        AdbFunctionType.CLEAR_DATA -> AdbTool.exec("pm clear $packageName")
                        AdbFunctionType.APP_INFO -> {
                            val app = _appList.value.firstOrNull { it.packageName == packageName }
                            val dumpsys = AdbTool.exec("dumpsys package $packageName")
                            val versionName = Regex("versionName=([^\\s]+)").find(dumpsys)?.groupValues?.get(1) ?: "-"
                            val versionCode = Regex("versionCode=([^\\s]+)").find(dumpsys)?.groupValues?.get(1) ?: "-"
                            val minSdk = Regex("minSdk=([^\\s]+)").find(dumpsys)?.groupValues?.get(1) ?: "-"
                            val targetSdk = Regex("targetSdk=([^\\s]+)").find(dumpsys)?.groupValues?.get(1) ?: "-"
                            val uid = Regex("userId=([^\\s]+)").find(dumpsys)?.groupValues?.get(1) ?: "-"
                            val firstInstallTime = Regex("firstInstallTime=([^\n]+)").find(dumpsys)?.groupValues?.get(1)?.trim() ?: "-"
                            val lastUpdateTime = Regex("lastUpdateTime=([^\n]+)").find(dumpsys)?.groupValues?.get(1)?.trim() ?: "-"
                            val supportedAbi = Regex("primaryCpuAbi=([^\n]+)").find(dumpsys)?.groupValues?.get(1)?.trim() ?: "-"
                            val isSystemApp = dumpsys.contains("pkgFlags=[") && dumpsys.substringAfter("pkgFlags=[").substringBefore("]").contains("SYSTEM")
                            val packagePath = app?.apkPath?.takeIf { it.isNotBlank() }
                                ?: AdbTool.exec("pm path $packageName").lineSequence()
                                    .firstOrNull { it.startsWith("package:") }
                                    ?.removePrefix("package:")
                                    ?.trim()
                                    .orEmpty()
                            val sizeBytes = queryPackageSizeBytes(packagePath, packageName) ?: app?.sizeBytes
                            val sizeText = app?.size?.takeIf { it != "-" } ?: sizeBytes?.let { formatSize(it) } ?: "-"
                            val processId = if (app?.isRunning == true) {
                                AdbTool.exec("pidof $packageName").trim().takeIf { it.isNotEmpty() } ?: "-"
                            } else {
                                "-"
                            }
                            val permissionStats = parsePermissionStats(dumpsys)
                            val permissionDetails = parsePermissionDetails(dumpsys)
                            val activityDetails = parseActivityDetails(dumpsys, packageName)
                            val serviceDetails = parseServiceDetails(dumpsys, packageName)
                            val receiverDetails = parseReceiverDetails(dumpsys, packageName)
                            val providerDetails = parseProviderDetails(dumpsys, packageName)
                            val signatureDetails = parseSignatureDetails(dumpsys)

                            val info = AppInfoData(
                                appName = app?.appName ?: packageName,
                                packageName = packageName,
                                versionName = versionName,
                                versionCode = versionCode,
                                minSdk = minSdk,
                                targetSdk = targetSdk,
                                uid = uid,
                                firstInstallTime = firstInstallTime,
                                lastUpdateTime = lastUpdateTime,
                                supportedAbi = supportedAbi,
                                isSystemApp = isSystemApp,
                                isRunning = app?.isRunning == true,
                                apkPath = packagePath,
                                dataDir = "/data/user/0/$packageName",
                                installLocation = if (isSystemApp) "系统分区" else "内部存储",
                                appSize = sizeText,
                                totalSize = sizeText,
                                processId = processId,
                                memoryUsage = if (app?.isRunning == true) "运行中" else "-",
                                startTime = if (app?.isRunning == true) "已启动" else "-",
                                dangerousPermissionCount = permissionStats.dangerous,
                                privacyPermissionCount = permissionStats.privacy,
                                normalPermissionCount = permissionStats.normal,
                                totalPermissionCount = permissionStats.total,
                                permissionDetails = permissionDetails,
                                activityDetails = activityDetails,
                                serviceDetails = serviceDetails,
                                receiverDetails = receiverDetails,
                                providerDetails = providerDetails,
                                signatureDetails = signatureDetails
                            )
                            withContext(Dispatchers.Main) {
                                if (app != null) {
                                    _appList.value = _appList.value.map { current ->
                                        if (current.packageName == packageName) {
                                            current.copy(
                                                versionName = versionName,
                                                installTime = firstInstallTime,
                                                size = sizeText,
                                                sizeBytes = sizeBytes,
                                                installTimestamp = parseInstallTimestamp(firstInstallTime)
                                            )
                                        } else {
                                            current
                                        }
                                    }
                                }
                                _appInfo.value = info
                            }
                        }
                        AdbFunctionType.EXPORT_APK -> {
                            val path = AdbTool.exec("pm path $packageName").split(":").getOrNull(1)?.trim()
                            if (path != null) {
                                val folderPath = withContext(Dispatchers.Main) { FileUtils.selectFolder() }
                                if (folderPath != null) {
                                    val savePath = "$folderPath/${appName}_${System.currentTimeMillis()}.apk"
                                    val success = AdbTool.pullFile(path, savePath)
                                    showTipDialog(MsgContent.Resource(if (success) Res.string.dialog_export_success else Res.string.dialog_export_failed))
                                } else {
                                    showTipDialog(MsgContent.Resource(Res.string.dialog_no_export_path))
                                }
                            } else {
                                showTipDialog(MsgContent.Resource(Res.string.dialog_get_install_path_failed))
                            }
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                showTipDialog(MsgContent.Resource(Res.string.dialog_operation_failed))
            }
        }
    }

    private data class PermissionStats(
        val dangerous: Int,
        val privacy: Int,
        val normal: Int,
        val total: Int
    )

    private fun parsePermissionStats(dumpsys: String): PermissionStats {
        val permissions = parsePermissionDetails(dumpsys).toSet()
        val dangerousKeywords = listOf(
            "CAMERA",
            "LOCATION",
            "RECORD_AUDIO",
            "CONTACTS",
            "SMS",
            "PHONE",
            "CALENDAR",
            "BODY_SENSORS",
            "STORAGE",
            "BLUETOOTH"
        )
        val dangerous = permissions.count { permission ->
            dangerousKeywords.any { keyword -> permission.contains(keyword) }
        }
        val privacy = permissions.count { permission ->
            listOf("CAMERA", "LOCATION", "RECORD_AUDIO", "CONTACTS", "SMS", "PHONE").any { keyword ->
                permission.contains(keyword)
            }
        }
        val normal = (permissions.size - dangerous).coerceAtLeast(0)
        return PermissionStats(
            dangerous = dangerous,
            privacy = privacy,
            normal = normal,
            total = permissions.size
        )
    }

    private fun parsePermissionDetails(dumpsys: String): List<String> {
        val permissionPattern = Regex("""\b(?:[a-zA-Z0-9_]+\.)*permission\.[A-Za-z0-9_\.]+\b""")
        return permissionPattern.findAll(dumpsys)
            .map { it.value.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
            .sorted()
    }

    private fun parseActivityDetails(dumpsys: String, packageName: String): List<String> {
        return parseComponentDetails(dumpsys, packageName, listOf("activity", "launcheractivity", "resumedactivity"))
    }

    private fun parseServiceDetails(dumpsys: String, packageName: String): List<String> {
        return parseComponentDetails(dumpsys, packageName, listOf("service"))
    }

    private fun parseReceiverDetails(dumpsys: String, packageName: String): List<String> {
        return parseComponentDetails(dumpsys, packageName, listOf("receiver"))
    }

    private fun parseProviderDetails(dumpsys: String, packageName: String): List<String> {
        return parseComponentDetails(dumpsys, packageName, listOf("provider", "contentprovider"))
    }

    private fun parseComponentDetails(
        dumpsys: String,
        packageName: String,
        keywords: List<String>
    ): List<String> {
        val pkgEscaped = Regex.escape(packageName)
        val slashPattern = Regex("""\b$pkgEscaped/[A-Za-z0-9_.$]+\b""")
        val classPattern = Regex("""\b$pkgEscaped\.[A-Za-z0-9_.$]+\b""")
        val candidates = linkedSetOf<String>()

        dumpsys.lineSequence().forEach { line ->
            val lower = line.lowercase()
            if (keywords.none { lower.contains(it) }) return@forEach
            slashPattern.findAll(line).forEach { match ->
                normalizeComponentName(packageName, match.value)?.let { candidates.add(it) }
            }
            classPattern.findAll(line).forEach { match ->
                normalizeComponentName(packageName, match.value)?.let { candidates.add(it) }
            }
        }

        return candidates.toList().sorted()
    }

    private fun normalizeComponentName(packageName: String, raw: String): String? {
        val value = raw.trim().trim(',', ';', ')', '(', '[', ']')
        if (value.isEmpty()) return null
        if ('/' !in value) return value

        val classPart = value.substringAfter("/")
        if (classPart.isBlank()) return null
        return if (classPart.startsWith(".")) {
            "$packageName$classPart"
        } else {
            classPart
        }
    }

    private fun parseSignatureDetails(dumpsys: String): List<String> {
        val signatureKeywords = listOf("signature", "signing", "cert", "certificate", "sha-256", "sha1", "md5")
        val lines = dumpsys.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .filter { line ->
                val lower = line.lowercase()
                signatureKeywords.any { lower.contains(it) }
            }
            .toList()

        return if (lines.isEmpty()) emptyList() else lines.distinct().take(50)
    }

    private fun queryPackageSizeBytes(packagePath: String, packageName: String): Long? {
        val resolvedPath = packagePath.ifBlank {
            AdbTool.exec("pm path $packageName").lineSequence()
                .firstOrNull { it.startsWith("package:") }
                ?.removePrefix("package:")
                ?.trim()
                .orEmpty()
        }
        if (resolvedPath.isBlank()) return null

        val escapedPath = resolvedPath.replace("'", "'\\''")
        val byStat = AdbTool.exec("stat -c %s '$escapedPath'")
            .lineSequence()
            .mapNotNull { it.trim().toLongOrNull() }
            .firstOrNull()
        if (byStat != null && byStat > 0L) return byStat

        val duOutput = AdbTool.exec("du -k '$escapedPath'")
        val kb = Regex("""^\s*(\d+)""").find(duOutput)?.groupValues?.getOrNull(1)?.toLongOrNull()
        if (kb != null && kb > 0L) return kb * 1024L

        return null
    }

    private fun formatSize(bytes: Long): String {
        if (bytes <= 0L) return "-"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format("%.2f GB", gb)
            mb >= 1.0 -> String.format("%.1f MB", mb)
            kb >= 1.0 -> String.format("%.0f KB", kb)
            else -> "$bytes B"
        }
    }

    private fun parseInstallTimestamp(raw: String): Long? {
        if (raw.isBlank() || raw == "-") return null
        val normalized = raw.trim()
        val patterns = listOf(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss.SSS"
        )
        for (pattern in patterns) {
            val parsed = runCatching {
                LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern(pattern))
            }.getOrNull() ?: continue
            return parsed.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
        return null
    }
}
