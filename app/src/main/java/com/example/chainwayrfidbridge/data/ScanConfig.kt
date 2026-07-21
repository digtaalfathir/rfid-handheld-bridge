package com.example.chainwayrfidbridge.data

import java.util.Calendar

enum class ScanMode(val key: String, val label: String) {
    WO("wo", "WO"),
    REGISTER("register", "Register");

    companion object {
        fun fromKey(key: String?): ScanMode = entries.find { it.key == key } ?: WO
    }
}

data class ScanConfig(
    val mode: ScanMode = ScanMode.WO,
    val baseUrl: String = "http://192.168.1.31:3030",
    val endpoint: String = "/rfid",
    val readerId: String = "C72",
    val antenna: String = "1",
    val rrType: String = "T1B",
    val makerName: String = "",
    val initialYear: String = Calendar.getInstance().get(Calendar.YEAR).toString(),
    val power: Int = 20
) {
    companion object {
        const val MIN_POWER = 1
        const val MAX_POWER = 30
        val ANTENNA_OPTIONS = (1..8).map { it.toString() }
        val RR_TYPE_OPTIONS = listOf(
            "T1B", "T1R", "T1F", "T5B", "SP3", "T3N", "T1X", "T2A", "SP2 UC", "T3P", "T3M", "T3A"
        )
    }

    /** Combines baseUrl + endpoint into the full request URL, regardless of slashes on either side. */
    fun fullApiUrl(): String {
        val base = baseUrl.trimEnd('/')
        val path = endpoint.trim().trim('/')
        return if (path.isEmpty()) base else "$base/$path"
    }

    /** Field name -> error message, empty when the config is valid. */
    fun validate(): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        if (baseUrl.isBlank() || (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://"))) {
            errors["baseUrl"] = "Base URL harus diawali http:// atau https://"
        }
        if (mode == ScanMode.WO) {
            if (readerId.isBlank()) errors["readerId"] = "Reader ID wajib diisi"
            if (antenna.toIntOrNull() == null) errors["antenna"] = "Antenna harus berupa angka"
        } else {
            if (rrType.isBlank()) errors["rrType"] = "RR Type wajib diisi"
            if (makerName.isBlank()) errors["makerName"] = "Maker Name wajib diisi"
            if (initialYear.toIntOrNull() == null) errors["initialYear"] = "Initial Year harus berupa angka"
        }
        if (power !in MIN_POWER..MAX_POWER) {
            errors["power"] = "Power harus antara $MIN_POWER-$MAX_POWER dBm"
        }
        return errors
    }

    fun toApiFields(): Map<String, String> = mapOf(
        "reader_id" to readerId,
        "antenna" to antenna,
        "rr_type" to rrType,
        "maker_name" to makerName,
        "initial_year" to initialYear
    )
}
