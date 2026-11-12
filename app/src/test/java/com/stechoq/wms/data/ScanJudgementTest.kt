package com.stechoq.wms.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScanJudgementTest {

    // Trimmed version of the spec's example response (Handheld RFID Scan Judgement Response.md).
    private val woResponseBody = """
        {
          "status": true, "code": 200, "message": "Logs created successfully",
          "data": {
            "wo": {
              "wo_number": "WO.DEV-20261102-003", "wo_type": "devanning",
              "qty_planning": 122, "qty_actual": 162
            },
            "tags": [
              { "rfid_number": "A", "note": "Data Match", "rssi": "strong" },
              { "rfid_number": "B", "note": "Data Match", "rssi": "strong" },
              { "rfid_number": "C", "note": "RFID Not Registered", "rssi": "medium" }
            ]
          }
        }
    """.trimIndent()

    @Test
    fun parsesWoProgressAndGroupsNotesByCount() {
        val judgement = parseScanJudgement(woResponseBody)
        assertEquals("WO.DEV-20261102-003", judgement?.wo?.woNumber)
        assertEquals("devanning", judgement?.wo?.woType)
        assertEquals(122, judgement?.wo?.qtyPlanning)
        assertEquals(162, judgement?.wo?.qtyActual)
        assertEquals(listOf("Data Match" to 2, "RFID Not Registered" to 1), judgement?.noteCounts)
    }

    @Test
    fun nullWoNumberStaysNull() {
        val body = woResponseBody.replace("\"WO.DEV-20261102-003\"", "null")
        assertNull(parseScanJudgement(body)?.wo?.woNumber)
    }

    @Test
    fun registerModeLightweightResponseHasNoTagsSoReturnsNull() {
        val body = """{ "status": true, "code": 200, "message": "Logs created successfully" }"""
        assertNull(parseScanJudgement(body))
    }

    @Test
    fun malformedBodyReturnsNull() {
        assertNull(parseScanJudgement("not json"))
    }
}
