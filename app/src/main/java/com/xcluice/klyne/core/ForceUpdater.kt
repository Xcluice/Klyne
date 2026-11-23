package com.xcluice.klyne.core

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.xcluice.klyne.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class ReleaseInfo(
    val versionTag: String,
    val versionName: String,
    val notes: String,
    val apkUrl: String,
    val releasePageUrl: String
)

object ForceUpdater {
    private const val LATEST_API = "https://api.github.com/repos/Xcluice/Klyne/releases/latest"

    suspend fun checkForUpdate(): ReleaseInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL(LATEST_API).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "KlyneLauncher/${BuildConfig.VERSION_NAME}")
            }
            if (conn.responseCode != 200) return@withContext null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val json = JSONObject(body)
            val tag = json.optString("tag_name", "").trim()
            val remoteVersion = tag.removePrefix("v").removePrefix("V")
            if (remoteVersion.isBlank() || !isNewerVersion(remoteVersion, BuildConfig.VERSION_NAME)) {
                return@withContext null
            }

            val assets = json.optJSONArray("assets")
            var apkUrl = ""
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url", "")
                        break
                    }
                }
            }
            if (apkUrl.isBlank()) return@withContext null

            ReleaseInfo(
                versionTag = tag,
                versionName = remoteVersion,
                notes = json.optString("body", "").trim().ifBlank { "New Klyne release available." },
                apkUrl = apkUrl,
                releasePageUrl = json.optString("html_url", "https://github.com/Xcluice/Klyne/releases/latest")
            )
        }.getOrNull()
    }

    suspend fun downloadAndInstall(
        context: Context,
        release: ReleaseInfo,
        onProgress: (Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            val outFile = File(dir, "Klyne-${release.versionName}.apk")
            val conn = (URL(release.apkUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = 15000
                readTimeout = 30000
                setRequestProperty("User-Agent", "KlyneLauncher/${BuildConfig.VERSION_NAME}")
            }
            val total = conn.contentLengthLong.coerceAtLeast(1L)
            conn.inputStream.use { input ->
                outFile.outputStream().use { output ->
                    val buf = ByteArray(16 * 1024)
                    var readTotal = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n <= 0) break
                        output.write(buf, 0, n)
                        readTotal += n
                        onProgress((readTotal.toFloat() / total.toFloat()).coerceIn(0f, 1f))
                    }
                }
            }
            conn.disconnect()

            withContext(Dispatchers.Main) {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(intent)
            }
            true
        }.getOrDefault(false)
    }

    internal fun isNewerVersion(remote: String, local: String): Boolean {
        val r = remote.split(".").mapNotNull { it.filter(Char::isDigit).toIntOrNull() }
        val l = local.split(".").mapNotNull { it.filter(Char::isDigit).toIntOrNull() }
        val max = maxOf(r.size, l.size)
        for (i in 0 until max) {
            val rv = r.getOrElse(i) { 0 }
            val lv = l.getOrElse(i) { 0 }
            if (rv != lv) return rv > lv
        }
        return false
    }
}
