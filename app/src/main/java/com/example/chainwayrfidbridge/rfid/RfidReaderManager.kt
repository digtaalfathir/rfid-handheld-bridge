package com.example.chainwayrfidbridge.rfid

import android.content.Context

/**
 * Vendor-agnostic contract for a UHF reader backend. ScanViewModel talks only to this
 * interface — it doesn't know or care whether the concrete implementation is Chainway's
 * SDK, Zebra's, or anything else added later.
 */
interface RfidReaderManager {

    fun connect(context: Context): Boolean

    fun isConnected(): Boolean

    /** Native valid range for [setPower]'s [level] — each backend exposes its own hardware's
     * actual scale directly (no shared abstraction) rather than remapping onto a common range. */
    val powerRange: IntRange get() = 1..30

    /** [level] must be within [powerRange] for this backend. */
    fun setPower(level: Int): Boolean

    /**
     * Starts continuous inventory; [onTag] is invoked for every read (thread depends on the
     * implementation). Returns null on success, or a human-readable reason on failure (e.g.
     * the reader refusing to scan while it thinks it's charging).
     */
    fun startInventory(onTag: (epc: String, rssi: String) -> Unit): String?

    fun stopInventory()

    fun release()

    /**
     * Wires the device's physical scan trigger, for backends where trigger press/release
     * comes through the SDK itself rather than a raw Android key event (e.g. Zebra).
     * No-op by default — Chainway's trigger is handled at the Activity level instead.
     */
    fun setTriggerListener(onPressed: () -> Unit, onReleased: () -> Unit) {}

    /**
     * Called every time the app returns to the foreground. Re-asserts hardware ownership
     * where the OS/vendor service can reclaim it while backgrounded — e.g. Zebra's barcode
     * (DataWedge) engine takes back the physical trigger button whenever another app is in
     * front, so the RFID trigger mode needs to be reapplied on resume or the trigger fires
     * both scanners at once. No-op by default.
     */
    fun onForeground() {}

    /**
     * Called every time the app leaves the foreground (Home pressed, another app opened, etc).
     * DataWedge reclaims the trigger the moment focus is lost, not just while some other app
     * stays in front — reasserting only on return leaves a window where a background trigger
     * press (with background scanning enabled) fires the barcode laser too. No-op by default.
     */
    fun onBackground() {}
}
