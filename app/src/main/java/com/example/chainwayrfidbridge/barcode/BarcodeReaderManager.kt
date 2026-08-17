package com.example.chainwayrfidbridge.barcode

import android.content.Context

/**
 * Vendor-agnostic contract for barcode capture, mirroring RfidReaderManager's shape. The two
 * backends actually work quite differently under the hood:
 *
 * - Chainway: a fully independent hardware/SDK path from the UHF antenna (com.rscja.barcode).
 *   We explicitly drive it — [startScan]/[stopScan] map directly onto the trigger press/release
 *   ScanViewModel already receives, same as RFID inventory does.
 * - Zebra: barcode capture IS the device's DataWedge engine, which owns the physical trigger
 *   itself once RfidReaderManager.setBarcodeTriggerActive(true) hands it over — [startScan] and
 *   [stopScan] are no-ops there, results just arrive via [setResultListener] whenever the
 *   operator uses the trigger, with no explicit start/stop call from us at all.
 */
interface BarcodeReaderManager {

    fun connect(context: Context): Boolean

    /** [onResult] fires once per decoded barcode (thread depends on the implementation). */
    fun setResultListener(onResult: (String) -> Unit)

    fun startScan() {}

    fun stopScan() {}

    fun release()
}
