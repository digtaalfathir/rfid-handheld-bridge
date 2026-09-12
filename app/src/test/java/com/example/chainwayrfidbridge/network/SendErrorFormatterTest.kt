package com.example.chainwayrfidbridge.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SendErrorFormatterTest {

    private val systemErrorText = "System error, please contact WMS Team"

    @Test
    fun http500IsGeneralizedAsSystemError() {
        val raw = """HTTP 500 {"status":false,"code":500,"message":"NullPointerException at line 42"}"""
        val display = formatSendError(raw, systemErrorText)
        assertEquals(systemErrorText, display.message)
        assertTrue(display.isSystemError)
    }

    @Test
    fun otherHttpCodeShowsOnlyTheMessageField() {
        val raw = """HTTP 422 {"status":false,"code":422,"message":"Factory code is required"}"""
        val display = formatSendError(raw, systemErrorText)
        assertEquals("Factory code is required", display.message)
        assertFalse(display.isSystemError)
    }

    @Test
    fun unparsableBodyFallsBackToRawTextAsSystemError() {
        val raw = "HTTP 400 not json at all"
        val display = formatSendError(raw, systemErrorText)
        assertEquals(raw, display.message)
        assertTrue(display.isSystemError)
    }

    @Test
    fun networkExceptionFallsBackToRawTextAsSystemError() {
        val raw = "Unable to resolve host"
        val display = formatSendError(raw, systemErrorText)
        assertEquals(raw, display.message)
        assertTrue(display.isSystemError)
    }
}
