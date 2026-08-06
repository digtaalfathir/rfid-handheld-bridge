package com.example.chainwayrfidbridge.data

/**
 * Display label/description live in AppStrings, not here — this enum is persistence-only.
 *
 * [powerRange]/[defaultPower] mirror the corresponding RfidReaderManager implementation's own
 * powerRange (rfid.ChainwayReaderManager / rfid.ZebraReaderManager) — duplicated here rather than
 * referenced directly so this data-layer enum doesn't need to depend on the rfid package. Used
 * by ConfigRepository for persistence-time defaulting/clamping, before any reader is connected;
 * ScanViewModel.powerRange (the live reader's own value) is authoritative once connected.
 */
enum class DeviceType(val key: String, val powerRange: IntRange, val defaultPower: Int) {
    CHAINWAY("chainway", 1..30, 20),
    ZEBRA("zebra", 0..300, 200);

    companion object {
        fun fromKey(key: String?): DeviceType? = entries.find { it.key == key }
    }
}
