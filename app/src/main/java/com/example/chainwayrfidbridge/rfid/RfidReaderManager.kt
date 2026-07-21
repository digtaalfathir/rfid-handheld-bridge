package com.example.chainwayrfidbridge.rfid

import android.content.Context
import com.rscja.deviceapi.RFIDWithUHFUART
import com.rscja.deviceapi.interfaces.IUHFInventoryCallback

/**
 * Thin wrapper around the Chainway UHF SDK so hardware calls live in one place,
 * reusable by future features (write tag, lock/kill) without touching UI code.
 *
 * Uses the SDK's own setInventoryCallback() push API (confirmed present in this aar
 * and is what Chainway's own reference app uses) rather than polling
 * readTagFromBuffer() in a custom thread — the polling approach pegged a CPU core
 * at 100% for the whole scan session and was the real source of the app-wide lag.
 */
class RfidReaderManager {

    private var reader: RFIDWithUHFUART? = null
    private var scanning = false

    fun connect(context: Context): Boolean {
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

    fun isConnected(): Boolean = reader != null

    fun setPower(dbm: Int): Boolean = reader?.setPower(dbm) == true

    /** Starts continuous inventory; [onTag] is invoked by the SDK's own callback thread for every read. */
    fun startInventory(onTag: (epc: String, rssi: String) -> Unit): Boolean {
        val r = reader ?: return false
        if (scanning) return true

        r.setInventoryCallback(IUHFInventoryCallback { info ->
            val epc = info?.epc
            if (!epc.isNullOrEmpty()) onTag(epc, info.rssi.orEmpty())
        })
        val started = r.startInventoryTag()
        scanning = started
        return started
    }

    fun stopInventory() {
        scanning = false
        reader?.stopInventory()
    }

    fun release() {
        stopInventory()
        reader?.free()
        reader = null
    }
}
