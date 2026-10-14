package com.stechoq.wms.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SendLogEntryTest {

    private val context = SendContext(
        type = SendType.RFID_BATCH,
        mode = ScanMode.REGISTER,
        opname = true,
        factoryCode = "WR01",
        rrType = "T1X",
        url = "https://product.suite.stechoq-j.com/api/v1/warehouse-management/jmp/log-rfids/components/handheld",
        itemCount = 25
    )

    @Test
    fun roundTripsThroughJson() {
        val entry = SendLogEntry(1_760_000_000_000, false, "HTTP 500 {}", "abc", context, resendOf = 1_759_000_000_000)
        assertEquals(entry, SendLogEntry.fromJson(JSONObject(entry.toJson().toString())))
    }

    @Test
    fun readsOldFormatEntryWithoutCrashingOrResend() {
        val old = JSONObject("""{"timestamp":1760000000000,"success":true,"detail":"OK (3 tag)"}""")
        val entry = SendLogEntry.fromJson(old)
        assertEquals("OK (3 tag)", entry.detail)
        assertNull(entry.context)
        assertNull(entry.id)
        assertFalse(entry.canResend)
    }

    @Test
    fun resendAvailableForBatchAndBarcodeWhetherOrNotTheyWorked() {
        listOf(SendType.RFID_BATCH, SendType.BARCODE).forEach { type ->
            listOf(true, false).forEach { success ->
                assertTrue(SendLogEntry(1, success, "", "id", context.copy(type = type)).canResend)
            }
        }
    }

    @Test
    fun successfulReplacementCannotBeResent() {
        val replacement = context.copy(type = SendType.REPLACEMENT)
        assertFalse(SendLogEntry(1, true, "", "id", replacement).canResend)
        assertTrue(SendLogEntry(1, false, "", "id", replacement).canResend)
    }
}
