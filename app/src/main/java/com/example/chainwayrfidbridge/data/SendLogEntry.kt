package com.example.chainwayrfidbridge.data

/** One send attempt (RFID batch or single barcode), kept so the raw/technical detail behind a
 * generalized on-screen error message isn't lost entirely — see Settings' log history. */
data class SendLogEntry(
    val timestamp: Long,
    val success: Boolean,
    val detail: String
)
