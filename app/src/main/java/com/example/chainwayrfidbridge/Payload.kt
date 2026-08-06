package com.example.chainwayrfidbridge

import com.example.chainwayrfidbridge.data.ScanConfig
import org.json.JSONArray
import org.json.JSONObject

/** WO and Register now share one endpoint and one payload shape; "mode" tells them apart server-side. */
fun buildPayload(config: ScanConfig, epcs: List<String>, timestamp: String): JSONObject =
    JSONObject().apply {
        put("rr_type", config.rrType)
        put("maker_name", config.makerName)
        put("idHex", JSONArray(epcs))
        put("initial_year", config.initialYear)
        put("reader_id", config.readerId)
        put("antenna", config.antenna)
        put("timestamp", timestamp)
        put("opname", config.opname)
        put("mode", config.mode.key)
        put("factory_code", config.factoryCode)
    }
