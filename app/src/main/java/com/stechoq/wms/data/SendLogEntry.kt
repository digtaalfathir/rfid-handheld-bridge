package com.stechoq.wms.data

import org.json.JSONObject

enum class SendType(val key: String) {
    RFID_BATCH("rfid_batch"),
    BARCODE("barcode"),
    REPLACEMENT("replacement");

    companion object {
        fun fromKey(key: String?): SendType? = entries.find { it.key == key }
    }
}

/** What a send was and the config it went out under — captured at send time, so the log keeps
 * showing what was actually used even after Settings change. */
data class SendContext(
    val type: SendType,
    val mode: ScanMode,
    val opname: Boolean,
    val factoryCode: String,
    val rrType: String,
    // Full request URL (Base URL + endpoint, or the replacement path) — also where a resend goes.
    val url: String,
    val itemCount: Int
) {
    companion object {
        fun of(config: ScanConfig, type: SendType, url: String, itemCount: Int) = SendContext(
            type, config.mode, config.opname, config.factoryCode, config.rrType, url, itemCount
        )
    }
}

/** One send attempt (RFID batch, single barcode, or replacement), kept so the raw/technical
 * detail behind a generalized on-screen error message isn't lost entirely — see Settings' log
 * history. [id] keys the stored request body (see ConfigRepository) that a resend posts again.
 * [id] and [context] are null on entries logged before they were recorded: those still show, but
 * can't be resent. [resendOf] is the original entry's timestamp when this entry is a resend. */
data class SendLogEntry(
    val timestamp: Long,
    val success: Boolean,
    val detail: String,
    val id: String? = null,
    val context: SendContext? = null,
    val resendOf: Long? = null
) {
    /** A successful replacement can't meaningfully be repeated — the old tag is already swapped
     * out server-side, so a resend would only be rejected. */
    val canResend: Boolean
        get() = id != null && context != null && !(context.type == SendType.REPLACEMENT && success)

    fun toJson(): JSONObject = JSONObject().apply {
        put("timestamp", timestamp)
        put("success", success)
        put("detail", detail)
        id?.let { put("id", it) }
        resendOf?.let { put("resend_of", it) }
        context?.let {
            put("type", it.type.key)
            put("mode", it.mode.key)
            put("opname", it.opname)
            put("factory_code", it.factoryCode)
            put("rr_type", it.rrType)
            put("url", it.url)
            put("item_count", it.itemCount)
        }
    }

    companion object {
        fun fromJson(o: JSONObject): SendLogEntry {
            val type = SendType.fromKey(o.optString("type", ""))
            val context = type?.let {
                SendContext(
                    type = it,
                    mode = ScanMode.fromKey(o.optString("mode", "")),
                    opname = o.optBoolean("opname"),
                    factoryCode = o.optString("factory_code"),
                    rrType = o.optString("rr_type"),
                    url = o.optString("url"),
                    itemCount = o.optInt("item_count")
                )
            }
            return SendLogEntry(
                timestamp = o.getLong("timestamp"),
                success = o.getBoolean("success"),
                detail = o.getString("detail"),
                id = if (o.has("id")) o.getString("id") else null,
                context = context,
                resendOf = if (o.has("resend_of")) o.getLong("resend_of") else null
            )
        }
    }
}
