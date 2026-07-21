package com.example.chainwayrfidbridge.network

import com.example.chainwayrfidbridge.buildPayload
import com.example.chainwayrfidbridge.data.ScanConfig
import com.example.chainwayrfidbridge.data.TagRecord
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class ApiClient {

    private val client = OkHttpClient.Builder()
        .callTimeout(10, TimeUnit.SECONDS)
        .connectTimeout(5, TimeUnit.SECONDS)
        .build()

    /** Returns null on success, or a human-readable reason on failure. */
    fun sendTags(config: ScanConfig, tags: List<TagRecord>): String? {
        return try {
            val payload = buildPayload(config.mode.key, tags.map { it.epc }, config.toApiFields(), isoNow())
            val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder().url(config.apiUrl).post(body).build()

            client.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) null else "HTTP ${resp.code} ${resp.body?.string().orEmpty().take(200)}"
            }
        } catch (e: Exception) {
            e.message ?: e.javaClass.simpleName
        }
    }

    /** Returns null when the host responds at all (any status code), a reason otherwise. */
    fun testConnection(apiUrl: String): String? {
        return try {
            val request = Request.Builder().url(apiUrl).get().build()
            client.newCall(request).execute().use { null }
        } catch (e: Exception) {
            e.message ?: e.javaClass.simpleName
        }
    }

    private fun isoNow(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        return fmt.format(Date())
    }
}
