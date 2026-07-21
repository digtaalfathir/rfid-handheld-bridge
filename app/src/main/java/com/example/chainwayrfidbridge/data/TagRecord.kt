package com.example.chainwayrfidbridge.data

data class TagRecord(
    val epc: String,
    val firstSeen: Long,
    val lastSeen: Long,
    val readCount: Int,
    val antenna: String,
    val rssi: String,
    val isNew: Boolean
)
