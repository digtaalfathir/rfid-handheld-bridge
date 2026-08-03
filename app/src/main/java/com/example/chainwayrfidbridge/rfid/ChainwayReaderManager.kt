package com.example.chainwayrfidbridge.rfid

import android.content.Context
import com.rscja.deviceapi.RFIDWithUHFUART
import com.rscja.deviceapi.interfaces.IUHFInventoryCallback

/**
 * Chainway UHF SDK backend. Uses the SDK's own setInventoryCallback() push API (confirmed
 * present in this aar and is what Chainway's own reference app uses) rather than polling
 * readTagFromBuffer() in a custom thread — the polling approach pegged a CPU core at 100%
 * for the whole scan session and was the real source of the app-wide lag.
 */
class ChainwayReaderManager : RfidReaderManager {

    private var reader: RFIDWithUHFUART? = null
    private var scanning = false

    override fun connect(context: Context): Boolean {
        return try {
            val r = RFIDWithUHFUART.getInstance()
            val ok = r.init(context.applicationContext)
            reader = if (ok) r else null
            ok
        } catch (e: Exception) {
            reader = null
            false
        }
    }

    override fun isConnected(): Boolean = reader != null

    /** This SDK's native power unit is dBm 1-30 — the shared range, so no mapping needed. */
    override fun setPower(level: Int): Boolean = reader?.setPower(level.coerceIn(1, 30)) == true

    override fun startInventory(onTag: (epc: String, rssi: String) -> Unit): String? {
        val r = reader ?: return "Reader not connected"
        if (scanning) return null

        r.setInventoryCallback(IUHFInventoryCallback { info ->
            val epc = info?.epc
            if (!epc.isNullOrEmpty()) onTag(epc, info.rssi.orEmpty())
        })
        val started = r.startInventoryTag()
        scanning = started
        // The SDK only reports success/failure as a boolean here, no reason text available.
        // Kept in English regardless of app language — this is a rare hardware-diagnostic
        // fallback, not part of the localized UI text, same as the raw vendor codes below.
        return if (started) null else "Failed to start scan"
    }

    override fun stopInventory() {
        scanning = false
        reader?.stopInventory()
    }

    override fun release() {
        stopInventory()
        reader?.free()
        reader = null
    }
}
