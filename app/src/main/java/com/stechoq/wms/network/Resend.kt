package com.stechoq.wms.network

import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Marks a logged request body as a resend: the original payload is kept exactly as it was
 * (including its own "timestamp", i.e. when it was scanned), plus `is_resend: true` and
 * `original_timestamp` — when that original send happened — so the backend can tell a resend
 * apart from a first send. Resending a resend keeps the very first `original_timestamp`.
 * Documented for the backend in README → API payload. */
fun markAsResend(body: String, originalSentAtMs: Long): String {
    val json = JSONObject(body)
    json.put("is_resend", true)
    if (!json.has("original_timestamp")) json.put("original_timestamp", isoUtc(originalSentAtMs))
    return json.toString()
}

/** Same format as the payload's own "timestamp" field. */
fun isoUtc(millis: Long): String {
    val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
    fmt.timeZone = TimeZone.getTimeZone("UTC")
    return fmt.format(Date(millis))
}
