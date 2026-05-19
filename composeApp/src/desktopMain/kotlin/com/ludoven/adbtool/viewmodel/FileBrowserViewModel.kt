package com.ludoven.adbtool.viewmodel

import androidx.lifecycle.viewModelScope
import com.ludoven.adbtool.entity.FileInfo
import com.ludoven.adbtool.entity.FileSortBy
import com.ludoven.adbtool.entity.FileSortOrder
import com.ludoven.adbtool.entity.MsgContent
import com.ludoven.adbtool.util.AdbTool
import com.ludoven.adbtool.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FileBrowserViewModel : BaseViewModel() {

    companion object {
        val QUICK_PATHS = listOf(
            "/" to "根目录",
            "/sdcard" to "内部存储",
            "/sdcard/Download" to "下载",
            "/sdcard/DCIM" to "相册",
            "/sdcard/Documents" to "文档",
            "/data/data" to "应用数据",
            "/system" to "系统",
            "/tmp" to "临时文件"
        )
    }

    private val _currentPath = MutableStateFlow("/sdcard")
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    private val _files = MutableStateFlow<List<FileInfo>>(emptyList())
    val files: StateFlow<List<FileInfo>> = _files.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorText = MutableStateFlow<String?>(null)
    val errorText: StateFlow<String?> = _errorText.asStateFlow()

    private val _sortBy = MutableStateFlow(FileSortBy.NAME)
    val sortBy: StateFlow<FileSortBy> = _sortBy.asStateFlow()

    private val _sortOrder = MutableStateFlow(FileSortOrder.ASC)
    val sortOrder: StateFlow<FileSortOrder> = _sortOrder.asStateFlow()

    private val _showHidden = MutableStateFlow(false)
    val showHidden: StateFlow<Boolean> = _showHidden.asStateFlow()

    private val _searchKeyword = MutableStateFlow("")
    val searchKeyword: StateFlow<String> = _searchKeyword.asStateFlow()

    private val _history = MutableStateFlow<List<String>>(listOf("/sdcard"))
    private val _historyIndex = MutableStateFlow(0)

    private val _clipboardFiles = MutableStateFlow<List<String>>(emptyList())
    val clipboardFiles: StateFlow<List<String>> = _clipboardFiles.asStateFlow()

    private val _clipboardMode = MutableStateFlow<ClipboardMode?>(null)
    val clipboardMode: StateFlow<ClipboardMode?> = _clipboardMode.asStateFlow()

    enum class ClipboardMode { COPY, CUT }

    fun loadFiles(path: String = _currentPath.value, deviceId: String?) {
        if (deviceId.isNullOrBlank()) {
            _files.value = emptyList()
            _errorText.value = null
            return
        }
        _isLoading.value = true
        _errorText.value = null
        viewModelScope.launch {
            val result = listDirectory(path, deviceId)
            result.onSuccess { fileList ->
                _currentPath.value = path
                _files.value = sortFiles(fileList)
                // Push to history
                val hist = _history.value.toMutableList()
                val idx = _historyIndex.value
                // Truncate forward history and append
                val newHist = hist.subList(0, idx + 1) + path
                _history.value = newHist
                _historyIndex.value = newHist.size - 1
            }.onFailure {
                _errorText.value = it.message ?: "Failed to load directory"
            }
            _isLoading.value = false
        }
    }

    fun navigateTo(path: String, deviceId: String?) {
        loadFiles(path, deviceId)
    }

    fun navigateUp(deviceId: String?) {
        val parent = _currentPath.value.let { p ->
            if (p == "/") "/" else p.substringBeforeLast("/").ifEmpty { "/" }
        }
        navigateTo(parent, deviceId)
    }

    fun canGoBack(): Boolean = _historyIndex.value > 0
    fun canGoForward(): Boolean = _historyIndex.value < _history.value.size - 1

    fun goBack(deviceId: String?) {
        if (!canGoBack()) return
        _historyIndex.value -= 1
        loadFiles(_history.value[_historyIndex.value], deviceId)
    }

    fun goForward(deviceId: String?) {
        if (!canGoForward()) return
        _historyIndex.value += 1
        loadFiles(_history.value[_historyIndex.value], deviceId)
    }

    fun setSortBy(sort: FileSortBy) {
        if (_sortBy.value == sort) {
            _sortOrder.value = if (_sortOrder.value == FileSortOrder.ASC) FileSortOrder.DESC else FileSortOrder.ASC
        } else {
            _sortBy.value = sort
            _sortOrder.value = if (sort == FileSortBy.SIZE) FileSortOrder.DESC else FileSortOrder.ASC
        }
        _files.value = sortFiles(_files.value)
    }

    fun toggleShowHidden() {
        _showHidden.value = !_showHidden.value
    }

    fun setSearchKeyword(keyword: String) {
        _searchKeyword.value = keyword
    }

    fun getFilteredFiles(): List<FileInfo> {
        val base = if (_showHidden.value) _files.value else _files.value.filter { !it.name.startsWith(".") }
        val kw = _searchKeyword.value.trim()
        return if (kw.isBlank()) base else base.filter { it.name.contains(kw, ignoreCase = true) }
    }

    // --- Clipboard operations ---

    fun copyFiles(fileNames: List<String>) {
        _clipboardFiles.value = fileNames
        _clipboardMode.value = ClipboardMode.COPY
    }

    fun cutFiles(fileNames: List<String>) {
        _clipboardFiles.value = fileNames
        _clipboardMode.value = ClipboardMode.CUT
    }

    fun clearClipboard() {
        _clipboardFiles.value = emptyList()
        _clipboardMode.value = null
    }

    fun pasteFiles(deviceId: String?) {
        if (deviceId.isNullOrBlank() || _clipboardFiles.value.isEmpty() || _clipboardMode.value == null) return
        val sources = _clipboardFiles.value
        val dest = _currentPath.value
        val mode = _clipboardMode.value
        viewModelScope.launch {
            var successCount = 0
            var failCount = 0
            for (src in sources) {
                val destPath = "$dest/${src.substringAfterLast("/")}"
                val command = when (mode) {
                    ClipboardMode.COPY -> AdbTool.buildShellCommand("cp", "-r", src, destPath)
                    ClipboardMode.CUT -> AdbTool.buildShellCommand("mv", src, destPath)
                    else -> continue
                }
                val result = withContext(Dispatchers.IO) {
                    AdbTool.execShellAsync(command, deviceId)
                }
                if (result.success || result.output.isBlank()) successCount++ else failCount++
            }
            clearClipboard()
            loadFiles(deviceId = deviceId)
            if (failCount > 0) {
                showTipDialog(MsgContent.Text("完成：成功 $successCount 个，失败 $failCount 个"), autoDismiss = true)
            } else {
                showTipDialog(MsgContent.Text("操作成功：$successCount 个文件"), autoDismiss = true)
            }
        }
    }

    // --- File operations ---

    fun deleteFile(path: String, deviceId: String?) {
        if (deviceId.isNullOrBlank()) return
        viewModelScope.launch {
            val command = AdbTool.buildShellCommand("rm", "-rf", "--", path)
            val result = withContext(Dispatchers.IO) {
                AdbTool.execShellAsync(command, deviceId)
            }
            if (result.success || result.output.isBlank()) {
                showTipDialog(MsgContent.Text("已删除: ${path.substringAfterLast("/")}"), autoDismiss = true)
                loadFiles(deviceId = deviceId)
            } else {
                showTipDialog(MsgContent.Text("删除失败: ${result.errorMessage ?: result.output}"))
            }
        }
    }

    fun renameFile(oldPath: String, newName: String, deviceId: String?) {
        if (deviceId.isNullOrBlank() || newName.isBlank()) return
        val newPath = "${oldPath.substringBeforeLast("/")}/$newName"
        viewModelScope.launch {
            val command = AdbTool.buildShellCommand("mv", oldPath, newPath)
            val result = withContext(Dispatchers.IO) {
                AdbTool.execShellAsync(command, deviceId)
            }
            if (result.success || result.output.isBlank()) {
                showTipDialog(MsgContent.Text("重命名成功"), autoDismiss = true)
                loadFiles(deviceId = deviceId)
            } else {
                showTipDialog(MsgContent.Text("重命名失败: ${result.errorMessage ?: result.output}"))
            }
        }
    }

    fun createDirectory(dirName: String, deviceId: String?) {
        if (deviceId.isNullOrBlank() || dirName.isBlank()) return
        val path = "${_currentPath.value}/$dirName"
        viewModelScope.launch {
            val command = AdbTool.buildShellCommand("mkdir", "--", path)
            val result = withContext(Dispatchers.IO) {
                AdbTool.execShellAsync(command, deviceId)
            }
            if (result.success || result.output.isBlank()) {
                showTipDialog(MsgContent.Text("已创建目录: $dirName"), autoDismiss = true)
                loadFiles(deviceId = deviceId)
            } else {
                showTipDialog(MsgContent.Text("创建失败: ${result.errorMessage ?: result.output}"))
            }
        }
    }

    fun pullFile(devicePath: String, deviceId: String?) {
        if (deviceId.isNullOrBlank()) return
        viewModelScope.launch {
            val localPath = withContext(Dispatchers.IO) {
                val name = devicePath.substringAfterLast("/")
                val tmpDir = java.io.File(System.getProperty("java.io.tmpdir"), "adbtool_pull")
                tmpDir.mkdirs()
                java.io.File(tmpDir, name).absolutePath
            }
            val result = withContext(Dispatchers.IO) {
                AdbTool.pullFileAsync(devicePath, localPath, deviceId)
            }
            if (result.success) {
                showTipDialog(MsgContent.Text("已拉取到: $localPath"), autoDismiss = true)
                // Open the folder
                runCatching {
                    val dir = java.io.File(localPath).parentFile
                    if (dir != null && java.awt.Desktop.isDesktopSupported()) {
                        java.awt.Desktop.getDesktop().open(dir)
                    }
                }
            } else {
                showTipDialog(MsgContent.Text("拉取失败: ${result.errorMessage ?: result.output}"))
            }
        }
    }

    fun pushFile(deviceId: String?) {
        if (deviceId.isNullOrBlank()) return
        viewModelScope.launch {
            val localPath = withContext(Dispatchers.IO) {
                FileUtils.selectFile()
            }
            if (localPath.isNullOrBlank()) return@launch
            val fileName = localPath.substringAfterLast("/").substringAfterLast("\\")
            val destPath = "${_currentPath.value}/$fileName"
            val result = withContext(Dispatchers.IO) {
                AdbTool.execAdbAsync("-s", deviceId, "push", localPath, destPath)
            }
            if (result.success) {
                showTipDialog(MsgContent.Text("推送成功: $fileName"), autoDismiss = true)
                loadFiles(deviceId = deviceId)
            } else {
                showTipDialog(MsgContent.Text("推送失败: ${result.errorMessage ?: result.output}"))
            }
        }
    }

    fun getFilePermission(path: String, deviceId: String?) {
        if (deviceId.isNullOrBlank()) return
        viewModelScope.launch {
            val command = AdbTool.buildShellCommand("stat", path)
            val result = withContext(Dispatchers.IO) {
                AdbTool.execShellAsync(command, deviceId)
            }
            if (result.success) {
                showTipDialog(MsgContent.Text(result.output))
            } else {
                showTipDialog(MsgContent.Text("获取信息失败: ${result.errorMessage ?: result.output}"))
            }
        }
    }

    fun openFile(devicePath: String, deviceId: String?) {
        if (deviceId.isNullOrBlank()) return
        viewModelScope.launch {
            val command = AdbTool.buildShellCommand(
                "am",
                "start",
                "-a",
                "android.intent.action.VIEW",
                "-d",
                "file://$devicePath"
            )
            val result = withContext(Dispatchers.IO) {
                AdbTool.execShellAsync(command, deviceId)
            }
            if (!result.success) {
                showTipDialog(MsgContent.Text("无法打开文件: ${result.errorMessage ?: result.output}"))
            }
        }
    }

    // --- Internal helpers ---

    private fun sortFiles(files: List<FileInfo>): List<FileInfo> {
        // Always put directories first
        val dirs = files.filter { it.isDirectory }
        val regularFiles = files.filter { !it.isDirectory }
        val comparator: Comparator<FileInfo> = when (_sortBy.value) {
            FileSortBy.NAME -> compareBy { it.name.lowercase() }
            FileSortBy.SIZE -> compareBy { it.size.toLongOrNull() ?: 0L }
            FileSortBy.DATE -> compareBy { "${it.date} ${it.time}" }
        }
        val sortedComparator = if (_sortOrder.value == FileSortOrder.DESC) comparator.reversed() else comparator
        return dirs.sortedWith(compareBy<FileInfo> { it.name.lowercase() }.let {
            if (_sortOrder.value == FileSortOrder.DESC) it.reversed() else it
        }) + regularFiles.sortedWith(sortedComparator)
    }

    private suspend fun listDirectory(path: String, deviceId: String): Result<List<FileInfo>> {
        return withContext(Dispatchers.IO) {
            // Try `ls -la` first
            val result = AdbTool.execShellAsync("ls -la \"$path\"", deviceId)
            if (result.success && result.output.isNotBlank()) {
                val parsed = parseLsOutput(result.output, path)
                if (parsed.isNotEmpty()) {
                    return@withContext Result.success(parsed)
                }
            }
            // Fallback to `ls -l`
            val fallback = AdbTool.execShellAsync("ls -l \"$path\"", deviceId)
            if (fallback.success && fallback.output.isNotBlank()) {
                val parsed = parseLsOutput(fallback.output, path)
                return@withContext Result.success(parsed)
            }
            if (fallback.success) {
                Result.success(emptyList())
            } else {
                Result.failure(IllegalStateException(fallback.errorMessage ?: fallback.output.ifBlank { "Permission denied or path not found" }))
            }
        }
    }

    private fun parseLsOutput(raw: String, parentPath: String): List<FileInfo> {
        val files = mutableListOf<FileInfo>()
        for (line in raw.lines()) {
            val trimmed = line.trim()
            // Skip total line and empty lines
            if (trimmed.isBlank() || trimmed.startsWith("total")) continue

            val parts = trimmed.split(Regex("\\s+"))
            if (parts.size < 7) continue

            val perms = parts[0]
            val isDir = perms.startsWith("d")
            val isLink = perms.startsWith("l")

            // Handle different ls formats:
            // Standard: perms links owner group size date time name
            // Some Android: perms owner group size date time name (no links column)
            val owner: String
            val group: String
            val sizeStr: String
            val dateIdx: Int
            val nameIdx: Int

            // Check if second part is a number (link count) or a string (owner name)
            if (parts[1].toLongOrNull() != null) {
                // Format: perms links owner group size date time name
                if (parts.size < 8) continue
                owner = parts[2]
                group = parts[3]
                sizeStr = parts[4]
                dateIdx = 5
                nameIdx = 7
            } else {
                // Format: perms owner group size date time name
                owner = parts[1]
                group = parts[2]
                sizeStr = parts[3]
                dateIdx = 4
                nameIdx = 6
            }

            val date = parts.getOrNull(dateIdx) ?: ""
            val time = parts.getOrNull(dateIdx + 1) ?: ""
            val rawName = parts.drop(nameIdx).joinToString(" ")

            if (rawName == "." || rawName == "..") continue

            // Handle symlinks: name -> target
            val name: String
            val symlinkTarget: String
            if (isLink && rawName.contains(" -> ")) {
                name = rawName.substringBefore(" -> ")
                symlinkTarget = rawName.substringAfter(" -> ")
            } else {
                name = rawName
                symlinkTarget = ""
            }

            files.add(
                FileInfo(
                    permissions = perms,
                    owner = owner,
                    group = group,
                    size = sizeStr,
                    date = date,
                    time = time,
                    name = name,
                    isDirectory = isDir,
                    isSymlink = isLink,
                    symlinkTarget = symlinkTarget
                )
            )
        }
        return files
    }
}
