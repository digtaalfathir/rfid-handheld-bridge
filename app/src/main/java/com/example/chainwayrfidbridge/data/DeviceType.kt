package com.example.chainwayrfidbridge.data

/** Display label/description live in AppStrings, not here — this enum is persistence-only. */
enum class DeviceType(val key: String) {
    CHAINWAY("chainway"),
    ZEBRA("zebra");

    companion object {
        fun fromKey(key: String?): DeviceType? = entries.find { it.key == key }
    }
}
