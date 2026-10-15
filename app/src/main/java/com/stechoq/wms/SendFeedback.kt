package com.stechoq.wms

import android.content.Context
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** How a send result feels and sounds — distinct enough to tell apart without looking at the
 * screen, since the operator is usually holding the trigger with eyes on the shelf, not the
 * handheld (and with Background Scanning, the app may not be on screen at all).
 *
 * The tone mirrors the vibration pattern (1 = sent, 3 = failed) because the handhelds in use
 * can't vibrate: the Zebra MC33 has no motor at all, and the Chainway C72 reports one that
 * doesn't physically buzz. On those the tone is the cue; on a handheld that can vibrate, both. */
object SendFeedback {
    /** Off/on timings in ms (Vibrator waveform format: starts with an initial delay). */
    val SUCCESS_VIBRATION = longArrayOf(0, 150)
    val FAILURE_VIBRATION = longArrayOf(0, 200, 120, 200, 120, 200)

    /** [tone] played [repeats] times, each [durationMs] long (-1 = the tone's own length), with
     * [gapMs] between the starts of consecutive plays. */
    data class TonePattern(val tone: Int, val durationMs: Int, val repeats: Int, val gapMs: Long)

    // TONE_PROP_ACK is itself a quick double beep; the failure tone is a low beep, three times.
    val SUCCESS_TONE = TonePattern(ToneGenerator.TONE_PROP_ACK, durationMs = -1, repeats = 1, gapMs = 0)
    val FAILURE_TONE = TonePattern(ToneGenerator.TONE_PROP_NACK, durationMs = 250, repeats = 3, gapMs = 400)

    fun vibrationFor(success: Boolean): LongArray = if (success) SUCCESS_VIBRATION else FAILURE_VIBRATION

    fun toneFor(success: Boolean): TonePattern = if (success) SUCCESS_TONE else FAILURE_TONE

    /** Always on — no separate toggle. Silently does nothing on a handheld without a motor. */
    fun vibrate(context: Context, success: Boolean) {
        val vibrator = vibrator(context) ?: return
        if (!vibrator.hasVibrator()) return
        val pattern = vibrationFor(success)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }

    private fun vibrator(context: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
}
