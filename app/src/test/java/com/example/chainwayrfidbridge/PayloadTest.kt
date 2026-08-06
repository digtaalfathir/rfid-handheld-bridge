package com.example.chainwayrfidbridge

import com.example.chainwayrfidbridge.data.ScanConfig
import com.example.chainwayrfidbridge.data.ScanMode
import org.junit.Assert.assertEquals
import org.junit.Test

class PayloadTest {

    @Test
    fun buildsUnifiedPayloadForWoMode() {
        val config = ScanConfig(
            mode = ScanMode.WO,
            readerId = "C72",
            antenna = "1",
            rrType = "T1B",
            makerName = "Acme",
            initialYear = "2026",
            factoryCode = "FAC1",
            opname = false
        )
        val json = buildPayload(config, listOf("E200001122"), "2026-07-20T00:00:00Z")
        assertEquals("C72", json.getString("reader_id"))
        assertEquals("1", json.getString("antenna"))
        assertEquals(1, json.getJSONArray("idHex").length())
        assertEquals("2026-07-20T00:00:00Z", json.getString("timestamp"))
        assertEquals("wo", json.getString("mode"))
        assertEquals(false, json.getBoolean("opname"))
        assertEquals("FAC1", json.getString("factory_code"))
    }

    @Test
    fun buildsUnifiedPayloadForRegisterModeWithOpname() {
        val config = ScanConfig(
            mode = ScanMode.REGISTER,
            rrType = "TRIAL",
            makerName = "Acme",
            initialYear = "2026",
            readerId = "MC333R",
            antenna = "2",
            factoryCode = "FAC2",
            opname = true
        )
        val json = buildPayload(config, listOf("E1", "E2"), "2026-07-20T00:00:00Z")
        assertEquals("TRIAL", json.getString("rr_type"))
        assertEquals("Acme", json.getString("maker_name"))
        assertEquals(2, json.getJSONArray("idHex").length())
        assertEquals("2026", json.getString("initial_year"))
        assertEquals("MC333R", json.getString("reader_id"))
        assertEquals("2", json.getString("antenna"))
        assertEquals("register", json.getString("mode"))
        assertEquals(true, json.getBoolean("opname"))
        assertEquals("FAC2", json.getString("factory_code"))
    }
}
