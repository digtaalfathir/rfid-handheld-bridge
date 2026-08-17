package com.example.chainwayrfidbridge.barcode

import android.content.Context
import com.rscja.barcode.BarcodeDecoder
import com.rscja.barcode.BarcodeFactory

/**
 * Chainway barcode SDK backend. Verified method-by-method against the real class bytecode in
 * DeviceAPI_ver20251103_release.aar (com.rscja.barcode.*) — not guessed. Uses the SDK's own
 * push-callback API (setDecodeCallback), the same pattern already proven for the UHF side —
 * polling would reintroduce the exact CPU-spin lag this app fixed once before.
 */
class ChainwayBarcodeManager : BarcodeReaderManager {

    private var decoder: BarcodeDecoder? = null
    private var onResult: ((String) -> Unit)? = null

    override fun connect(context: Context): Boolean {
        return try {
            val d = BarcodeFactory.getInstance().getBarcodeDecoder()
            val ok = d.open(context)
            if (ok) {
                d.setDecodeCallback { entity ->
                    if (entity.resultCode == BarcodeDecoder.DECODE_SUCCESS) {
                        entity.barcodeData?.takeIf { it.isNotEmpty() }?.let { data -> onResult?.invoke(data) }
                    }
                }
            }
            decoder = if (ok) d else null
            ok
        } catch (e: Exception) {
            decoder = null
            false
        }
    }

    override fun setResultListener(onResult: (String) -> Unit) {
        this.onResult = onResult
    }

    override fun startScan() {
        try {
            decoder?.startScan()
        } catch (e: Exception) {
            // best-effort — a stray trigger press while disconnected shouldn't crash the app
        }
    }

    override fun stopScan() {
        try {
            decoder?.stopScan()
        } catch (e: Exception) {
            // ignore
        }
    }

    override fun release() {
        try {
            decoder?.close()
        } catch (e: Exception) {
            // ignore
        }
        decoder = null
        onResult = null
    }
}
