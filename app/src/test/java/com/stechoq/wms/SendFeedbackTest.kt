package com.stechoq.wms

import android.media.ToneGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SendFeedbackTest {

    // Vibrator waveform: [initial delay, on, off, on, ...] — buzzes are the odd indices.
    private fun buzzes(pattern: LongArray) = pattern.indices.count { it % 2 == 1 }

    @Test
    fun successIsOneShortBuzz() {
        val pattern = SendFeedback.vibrationFor(success = true)
        assertEquals(1, buzzes(pattern))
        assertTrue(pattern[1] <= 200)
    }

    @Test
    fun failureIsThreeBuzzes() {
        assertEquals(3, buzzes(SendFeedback.vibrationFor(success = false)))
    }

    @Test
    fun toneMirrorsVibrationCount() {
        // Handhelds without a working motor rely on the tone alone, so it carries the same 1-vs-3.
        assertEquals(1, SendFeedback.toneFor(success = true).repeats)
        assertEquals(3, SendFeedback.toneFor(success = false).repeats)
    }

    @Test
    fun successAndFailureUseDifferentTones() {
        assertEquals(ToneGenerator.TONE_PROP_ACK, SendFeedback.toneFor(success = true).tone)
        assertEquals(ToneGenerator.TONE_PROP_NACK, SendFeedback.toneFor(success = false).tone)
        assertNotEquals(SendFeedback.toneFor(true).tone, SendFeedback.toneFor(false).tone)
    }

    @Test
    fun repeatedFailureBeepsDontOverlap() {
        val failure = SendFeedback.toneFor(success = false)
        assertTrue(failure.gapMs > failure.durationMs)
    }
}
