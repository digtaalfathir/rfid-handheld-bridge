package com.example.chainwayrfidbridge.data

/** Which physical scanner the trigger drives — the built-in UHF antenna or the barcode engine. */
enum class InputMode(val key: String) {
    RFID("rfid"),
    BARCODE("barcode");

    companion object {
        fun fromKey(key: String?): InputMode = entries.find { it.key == key } ?: RFID
    }
}
