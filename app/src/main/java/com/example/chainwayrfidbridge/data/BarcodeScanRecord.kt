package com.example.chainwayrfidbridge.data

/** One decoded barcode, sent to the server immediately rather than batched like RFID tags. */
data class BarcodeScanRecord(
    val code: String,
    val timestamp: Long,
    val status: BarcodeSendStatus
)

enum class BarcodeSendStatus { SENDING, SENT, FAILED }
