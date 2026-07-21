package com.example.chainwayrfidbridge

import org.junit.Assert.assertEquals
import org.junit.Test

class PayloadTest {

    @Test
    fun woMode_sendsReaderAntennaAndIdHex() {
        val json = buildPayload(
            "wo",
            listOf("E200001122"),
            mapOf("reader_id" to "C72", "antenna" to "1"),
            "2026-07-20T00:00:00Z"
        )
        assertEquals("C72", json.getString("reader_id"))
        assertEquals("1", json.getString("antenna"))
        assertEquals(1, json.getJSONArray("idHex").length())
        assertEquals("2026-07-20T00:00:00Z", json.getString("timestamp"))
    }

    @Test
    fun registerMode_sendsMakerRrTypeAndYear() {
        val json = buildPayload(
            "register",
            listOf("E1", "E2"),
            mapOf("rr_type" to "TRIAL", "maker_name" to "Acme", "initial_year" to "2026"),
            "ignored"
        )
        assertEquals("TRIAL", json.getString("rr_type"))
        assertEquals("Acme", json.getString("maker_name"))
        assertEquals(2, json.getJSONArray("rfid_numbers").length())
        assertEquals("2026", json.getString("initial_year"))
    }
}
