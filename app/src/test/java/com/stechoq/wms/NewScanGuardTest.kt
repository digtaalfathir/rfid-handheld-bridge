package com.stechoq.wms

import org.junit.Assert.assertEquals
import org.junit.Test

class NewScanGuardTest {

    @Test
    fun startsRightAwayWhenNothingIsOnScreen() {
        assertEquals(NewScanDecision.START, newScanDecision(SendStatus.Error("HTTP 500"), tagCount = 0))
        assertEquals(NewScanDecision.START, newScanDecision(SendStatus.Idle, tagCount = 0))
    }

    @Test
    fun startsRightAwayAfterSuccessfulSend() {
        assertEquals(NewScanDecision.START, newScanDecision(SendStatus.Success(12), tagCount = 12))
    }

    @Test
    fun asksBeforeDiscardingAfterAnyFailure() {
        listOf("timeout", "Unable to resolve host", "HTTP 500 {}", "HTTP 422 {\"message\":\"x\"}").forEach {
            assertEquals(NewScanDecision.CONFIRM_DISCARD, newScanDecision(SendStatus.Error(it), tagCount = 5))
        }
    }

    @Test
    fun ignoredWhileSendIsInFlight() {
        assertEquals(NewScanDecision.IGNORE, newScanDecision(SendStatus.Sending, tagCount = 5))
    }
}
