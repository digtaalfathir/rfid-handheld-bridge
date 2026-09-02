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

    /** Tells the backend whether Barcode mode is the one currently selected. Meaningful only
     * where activating barcode capture has a side effect on something else sharing the trigger
     * (Zebra, via a DataWedge profile switch) — a no-op default covers Chainway, whose barcode
     * and RFID hardware paths are already fully independent. Called once at connect time and
     * again whenever the operator changes Scan Mode, mirroring RfidReaderManager.setInputMode —
     * connect() itself must stay free of that side effect so an RFID-mode operator never has
     * DataWedge's barcode profile activated underneath them. */
    fun setActive(active: Boolean) {}

    /** [onResult] fires once per decoded barcode (thread depends on the implementation). */
    fun setResultListener(onResult: (String) -> Unit)

    fun startScan() {}

    fun stopScan() {}

    fun release()
}
