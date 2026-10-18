package com.stechoq.wms.network

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResendTest {

    private val original = """{"rr_type":"T1X","idHex":{"E200":"strong"},"timestamp":"2026-10-14T02:12:00Z","mode":"wo"}"""

    @Test
    fun keepsOriginalPayloadAndAddsMarkers() {
        val json = JSONObject(markAsResend(original, originalSentAtMs = 1_760_407_920_000))
        assertEquals("T1X", json.getString("rr_type"))
        assertEquals("strong", json.getJSONObject("idHex").getString("E200"))
        // The payload's own timestamp (scan time) is left as it was.
        assertEquals("2026-10-14T02:12:00Z", json.getString("timestamp"))
        assertTrue(json.getBoolean("is_resend"))
        assertEquals(isoUtc(1_760_407_920_000), json.getString("original_timestamp"))
    }

    @Test
    fun resendingAResendKeepsTheFirstOriginalTimestamp() {
        val first = markAsResend(original, originalSentAtMs = 1_000)
        val second = JSONObject(markAsResend(first, originalSentAtMs = 2_000))
        assertEquals(isoUtc(1_000), second.getString("original_timestamp"))
    }

    @Test
    fun isoUtcUsesPayloadTimestampFormat() {
        assertEquals("1970-01-01T00:00:01Z", isoUtc(1_000))
    }
}
