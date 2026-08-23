package com.example.chainwayrfidbridge

import com.example.chainwayrfidbridge.data.ScanConfig
import org.json.JSONObject

/** WO and Register now share one endpoint and one payload shape; "mode" tells them apart server-side.
 * [codes] maps each EPC/barcode to its quality wire value ("low"/"medium"/"strong" — see
 * [com.example.chainwayrfidbridge.data.TagQuality]), becoming the "idHex" object. */
fun buildPayload(config: ScanConfig, codes: Map<String, String>, timestamp: String): JSONObject =
    JSONObject().apply {
        put("rr_type", config.rrType)
        put("maker_name", config.makerName)
        put("idHex", JSONObject(codes))
        put("initial_year", config.initialYear)
        put("reader_id", config.readerId)
        put("antenna", config.antenna)
        put("timestamp", timestamp)
        put("opname", config.opname)
        put("mode", config.mode.key)
        put("factory_code", config.factoryCode)
    }
