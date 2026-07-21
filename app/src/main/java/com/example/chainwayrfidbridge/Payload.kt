package com.example.chainwayrfidbridge

import org.json.JSONArray
import org.json.JSONObject

fun buildPayload(mode: String, epcs: List<String>, cfg: Map<String, String>, timestamp: String): JSONObject {
    return if (mode == "register") {
        JSONObject().apply {
            put("rr_type", cfg["rr_type"])
            put("maker_name", cfg["maker_name"])
            put("rfid_numbers", JSONArray(epcs))
            put("initial_year", cfg["initial_year"])
        }
    } else {
        JSONObject().apply {
            put("reader_id", cfg["reader_id"])
            put("antenna", cfg["antenna"])
            put("idHex", JSONArray(epcs))
            put("timestamp", timestamp)
        }
    }
}
