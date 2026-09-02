package com.example.chainwayrfidbridge.network

import com.example.chainwayrfidbridge.buildPayload
import com.example.chainwayrfidbridge.data.FactoryCodeOption
import com.example.chainwayrfidbridge.data.ScanConfig
import com.example.chainwayrfidbridge.data.TagQuality
import com.example.chainwayrfidbridge.data.TagRecord
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class ApiClient {

    // Bumped from 10s/5s after a real incident: slow warehouse WiFi meant a send actually
    // reached the server and was recorded there, but the handheld had already given up and
    // shown a timeout/failure to the operator — a false negative worse than just waiting longer.
    private val client = OkHttpClient.Builder()
        .callTimeout(60, TimeUnit.SECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .build()

    /** Returns null on success, or a human-readable reason on failure. */
    fun sendTags(config: ScanConfig, tags: List<TagRecord>): String? =
        sendCodes(config, tags.associate { it.epc to TagQuality.from(it.rssi, it.readCount).wireValue })

    /** Same payload shape as [sendTags]. [codes] maps each code to its quality wire value — for
     * callers without a real RSSI-based quality (e.g. a single barcode scan, sent immediately
     * rather than batched like RFID's tag list), STRONG is the reasonable default: a successful
     * decode is a certain read, not a marginal one the way a weak RF signal is. */
    fun sendCodes(config: ScanConfig, codes: Map<String, String>): String? {
        return try {
            val payload = buildPayload(config, codes, isoNow())
            val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder().url(config.fullApiUrl()).post(body).build()

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

    // Path is fixed and not user-facing — a settings-screen convenience, not something an
    // operator would ever need to point elsewhere the way baseUrl/endpoint are.
    private val RR_TYPE_PATH = "/api/v1/master/dropdown/rr-type/components?factory_id=&category_id="
    private val FACTORY_PATH = "/api/v1/master/warehouse-factory/"

    /** Returns the rr_type list from every entry in "data", or null on failure. */
    fun fetchRrTypes(baseUrl: String): List<String>? {
        return try {
            val request = Request.Builder().url(baseUrl.trimEnd('/') + RR_TYPE_PATH).get().build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val data = JSONObject(resp.body?.string().orEmpty()).optJSONArray("data") ?: return null
                (0 until data.length()).mapNotNull {
                    data.getJSONObject(it).optString("rr_type").takeIf { s -> s.isNotBlank() }
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Walks every page per the response's "meta.total_page", or null on failure. Capped at 50
     * pages as a sanity limit — real factory lists are nowhere near that size. */
    fun fetchFactoryCodes(baseUrl: String): List<FactoryCodeOption>? {
        return try {
            val results = mutableListOf<FactoryCodeOption>()
            var page = 1
            var totalPages = 1
            while (page <= totalPages && page <= 50) {
                val request = Request.Builder().url("${baseUrl.trimEnd('/')}$FACTORY_PATH?page=$page").get().build()
                client.newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) return null
                    val json = JSONObject(resp.body?.string().orEmpty())
                    totalPages = json.optJSONObject("meta")?.optInt("total_page", 1) ?: 1
                    val data = json.optJSONArray("data") ?: JSONArray()
                    for (i in 0 until data.length()) {
                        val item = data.getJSONObject(i)
                        val code = item.optString("code")
                        if (code.isNotBlank()) results.add(FactoryCodeOption(code, item.optString("name")))
                    }
                }
                page++
            }
            results
        } catch (e: Exception) {
            null
        }
    }

    private fun isoNow(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        return fmt.format(Date())
    }
}
