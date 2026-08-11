package com.example.chainwayrfidbridge.network

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

data class UpdateInfo(val version: String, val downloadUrl: String)

/**
 * Checks GitHub Releases for a newer build and downloads it, so handhelds can update themselves
 * from a "vX.Y" release tag + attached .apk asset instead of being reinstalled one by one over
 * USB. Release tags are expected as "vX.Y" (or "X.Y") matching this app's versionName exactly —
 * bump versionName in build.gradle to match the tag whenever cutting a new release.
 */
class UpdateClient {

    // callTimeout covers the whole call, including download()'s APK transfer (~12MB) — 10s was
    // only ever enough for the small latestRelease() JSON check and timed out real downloads on
    // anything but fast WiFi. connectTimeout stays short so an unreachable host still fails fast.
    private val client = OkHttpClient.Builder()
        .callTimeout(120, TimeUnit.SECONDS)
        .connectTimeout(5, TimeUnit.SECONDS)
        .build()

    /** Null if no reachable release or no .apk asset attached to it. */
    fun latestRelease(repo: String): UpdateInfo? {
        val request = Request.Builder().url("https://api.github.com/repos/$repo/releases/latest").get().build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val json = JSONObject(resp.body?.string().orEmpty())
            val version = json.optString("tag_name").removePrefix("v")
            val assets = json.optJSONArray("assets") ?: return null
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.optString("name").endsWith(".apk")) {
                    return UpdateInfo(version, asset.optString("browser_download_url"))
                }
            }
            return null
        }
    }

    /** Streams the APK to [dest]; returns null on success or a human-readable reason on failure. */
    fun download(url: String, dest: File): String? {
        return try {
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return "HTTP ${resp.code}"
                dest.parentFile?.mkdirs()
                resp.body?.byteStream()?.use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                } ?: return "Empty response"
                null
            }
        } catch (e: Exception) {
            e.message ?: e.javaClass.simpleName
        }
    }
}
