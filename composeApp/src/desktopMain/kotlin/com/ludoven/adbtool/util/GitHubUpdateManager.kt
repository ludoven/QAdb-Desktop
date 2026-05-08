package com.ludoven.adbtool.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.awt.Desktop
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path

object GitHubUpdateManager {
    private const val OWNER = "ludoven"
    private const val REPO = "QADB"
    private const val LATEST_RELEASE_API = "https://api.github.com/repos/$OWNER/$REPO/releases/latest"
    private val json = Json { ignoreUnknownKeys = true }

    data class GitHubAssetResponse(
        val name: String = "",
        val downloadUrl: String = "",
        val size: Long = 0L
    )

    sealed class CheckResult {
        data class UpToDate(val latestVersion: String) : CheckResult()
        data class UpdateAvailable(
            val latestVersion: String,
            val htmlUrl: String,
            val asset: GitHubAssetResponse
        ) : CheckResult()

        data class UpdateAvailableNoAsset(
            val latestVersion: String,
            val htmlUrl: String
        ) : CheckResult()

        data class Error(val message: String) : CheckResult()
    }

    suspend fun checkForUpdate(currentVersion: String): CheckResult = withContext(Dispatchers.IO) {
        runCatching {
            val release = fetchLatestRelease()
            val latestVersion = release.tagName.ifBlank { release.name }.ifBlank { "unknown" }
            if (!isRemoteVersionNewer(currentVersion, latestVersion)) {
                return@runCatching CheckResult.UpToDate(latestVersion)
            }

            val asset = selectInstallerAsset(release.assets, currentPlatform())
            if (asset != null) {
                CheckResult.UpdateAvailable(
                    latestVersion = latestVersion,
                    htmlUrl = release.htmlUrl,
                    asset = asset
                )
            } else {
                CheckResult.UpdateAvailableNoAsset(
                    latestVersion = latestVersion,
                    htmlUrl = release.htmlUrl
                )
            }
        }.getOrElse { error ->
            CheckResult.Error(error.message ?: "Unknown error")
        }
    }

    suspend fun downloadAndInstall(asset: GitHubAssetResponse): Result<Path> = withContext(Dispatchers.IO) {
        runCatching {
            val tempDir = Files.createTempDirectory("qadb-update-")
            val sanitizedName = asset.name.ifBlank { "QADB-update.bin" }
            val outputPath = tempDir.resolve(sanitizedName)
            downloadFile(asset.downloadUrl, outputPath)
            openInstaller(outputPath.toFile())
            outputPath
        }
    }

    fun openReleasePage(url: String): Result<Unit> = runCatching {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URL(url).toURI())
        } else {
            throw IllegalStateException("Desktop browse is not supported on this system.")
        }
    }

    private fun fetchLatestRelease(): GitHubReleaseResponse {
        val body = sendGetRequest(LATEST_RELEASE_API)
        val root = json.parseToJsonElement(body).jsonObject
        val tagName = root["tag_name"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val name = root["name"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val htmlUrl = root["html_url"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val assets = root["assets"]
            ?.jsonArray
            ?.mapNotNull { element ->
                val obj = element.jsonObject
                val assetName = obj["name"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val downloadUrl = obj["browser_download_url"]?.jsonPrimitive?.contentOrNull.orEmpty()
                if (assetName.isBlank() || downloadUrl.isBlank()) {
                    null
                } else {
                    GitHubAssetResponse(
                        name = assetName,
                        downloadUrl = downloadUrl,
                        size = obj["size"]?.jsonPrimitive?.longOrNull ?: 0L
                    )
                }
            }
            .orEmpty()
        return GitHubReleaseResponse(
            tagName = tagName,
            name = name,
            htmlUrl = htmlUrl,
            assets = assets
        )
    }

    private fun sendGetRequest(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10_000
        connection.readTimeout = 20_000
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty("User-Agent", "QADB-Updater")

        return try {
            val responseCode = connection.responseCode
            val body = if (responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw IllegalStateException("HTTP $responseCode${if (errorBody.isNotBlank()) ": $errorBody" else ""}")
            }
            body
        } finally {
            connection.disconnect()
        }
    }

    private fun downloadFile(downloadUrl: String, outputPath: Path) {
        val connection = URL(downloadUrl).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10_000
        connection.readTimeout = 60_000
        connection.setRequestProperty("User-Agent", "QADB-Updater")

        try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw IllegalStateException("Download failed: HTTP $responseCode${if (errorBody.isNotBlank()) ": $errorBody" else ""}")
            }
            connection.inputStream.use { input ->
                Files.newOutputStream(outputPath).use { output ->
                    input.copyTo(output)
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun openInstaller(file: File) {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            Desktop.getDesktop().open(file)
            return
        }
        throw IllegalStateException("Desktop open is not supported on this system.")
    }

    private fun selectInstallerAsset(
        assets: List<GitHubAssetResponse>,
        platform: Platform
    ): GitHubAssetResponse? {
        if (assets.isEmpty()) return null
        val preferredExtensions = when (platform) {
            Platform.Mac -> listOf(".dmg", ".pkg", ".zip")
            Platform.Windows -> listOf(".msi", ".exe")
            Platform.Linux -> listOf(".AppImage", ".deb", ".rpm", ".tar.gz")
            Platform.Unknown -> emptyList()
        }
        if (preferredExtensions.isEmpty()) return null

        return assets
            .filter { asset -> preferredExtensions.any { ext -> asset.name.endsWith(ext, ignoreCase = true) } }
            .sortedBy { asset ->
                preferredExtensions.indexOfFirst { ext -> asset.name.endsWith(ext, ignoreCase = true) }.let { idx ->
                    if (idx == -1) Int.MAX_VALUE else idx
                }
            }
            .firstOrNull()
    }

    private fun currentPlatform(): Platform {
        val name = System.getProperty("os.name").lowercase()
        return when {
            "mac" in name -> Platform.Mac
            "win" in name -> Platform.Windows
            "nux" in name || "linux" in name -> Platform.Linux
            else -> Platform.Unknown
        }
    }

    private fun isRemoteVersionNewer(currentVersion: String, latestVersion: String): Boolean {
        val currentTokens = extractVersionNumbers(currentVersion)
        val latestTokens = extractVersionNumbers(latestVersion)
        val maxSize = maxOf(currentTokens.size, latestTokens.size)
        for (index in 0 until maxSize) {
            val current = currentTokens.getOrElse(index) { 0 }
            val latest = latestTokens.getOrElse(index) { 0 }
            if (latest > current) return true
            if (latest < current) return false
        }
        return false
    }

    private fun extractVersionNumbers(version: String): List<Int> {
        return Regex("\\d+")
            .findAll(version)
            .mapNotNull { it.value.toIntOrNull() }
            .toList()
            .ifEmpty { listOf(0) }
    }

    private enum class Platform {
        Mac,
        Windows,
        Linux,
        Unknown
    }

    private data class GitHubReleaseResponse(
        val tagName: String = "",
        val name: String = "",
        val htmlUrl: String = "",
        val assets: List<GitHubAssetResponse> = emptyList()
    )
}
