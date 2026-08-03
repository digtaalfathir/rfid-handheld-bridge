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

    /** [level] is 1-30, matching Chainway's native dBm range; Zebra maps it onto its own power table. */
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
}
