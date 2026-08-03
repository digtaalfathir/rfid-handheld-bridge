package com.example.chainwayrfidbridge.data

import java.util.Calendar

enum class ScanMode(val key: String, val label: String, val endpoint: String) {
    WO("wo", "WO", "/api/v1/warehouse-management/jmp/log-rfids"),
    REGISTER("register", "Register", "/api/v1/warehouse-management/jmp/log-rfids/components/handheld");

    companion object {
        fun fromKey(key: String?): ScanMode = entries.find { it.key == key } ?: WO
    }
}

data class ScanConfig(
    val mode: ScanMode = ScanMode.WO,
    val baseUrl: String = "",
    val readerId: String = "C72",
    val antenna: String = "1",
    val rrType: String = "T1B",
    val makerName: String = "",
    val initialYear: String = Calendar.getInstance().get(Calendar.YEAR).toString(),
    val power: Int = 20,
    val soundEnabled: Boolean = true,
    val soundVolume: Int = 80
) {
    companion object {
        const val MIN_POWER = 1
        const val MAX_POWER = 30
        const val MIN_VOLUME = 1
        const val MAX_VOLUME = 100
        val ANTENNA_OPTIONS = (1..8).map { it.toString() }
        val RR_TYPE_OPTIONS = listOf(
            "T1B", "T1R", "T1F", "T5B", "SP3", "T3N", "T1X", "T2A", "SP2 UC", "T3P", "T3M", "T3A"
        )
        val BASE_URL_OPTIONS = listOf(
            "https://wms.suite.stechoq-j.com", "https://product.suite.stechoq-j.com"
        )
        val INITIAL_YEAR_OPTIONS = (2000..2026).map { it.toString() }
    }

    /** Combines baseUrl + the endpoint fixed for the current mode into the full request URL. */
    fun fullApiUrl(): String {
        val base = baseUrl.trimEnd('/')
        val path = mode.endpoint.trim('/')
        return if (path.isEmpty()) base else "$base/$path"
    }

    /** Field name -> error type, empty when the config is valid. Display text lives in AppStrings. */
    fun validate(): Map<String, ValidationErrorType> {
        val errors = mutableMapOf<String, ValidationErrorType>()
        if (baseUrl.isBlank() || (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://"))) {
            errors["baseUrl"] = ValidationErrorType.BASE_URL_FORMAT
        }
        if (mode == ScanMode.WO) {
            if (readerId.isBlank()) errors["readerId"] = ValidationErrorType.READER_ID_REQUIRED
            if (antenna.toIntOrNull() == null) errors["antenna"] = ValidationErrorType.ANTENNA_NUMBER
        } else {
            if (rrType.isBlank()) errors["rrType"] = ValidationErrorType.RR_TYPE_REQUIRED
            if (makerName.isBlank()) errors["makerName"] = ValidationErrorType.MAKER_NAME_REQUIRED
            if (initialYear.toIntOrNull() == null) errors["initialYear"] = ValidationErrorType.INITIAL_YEAR_NUMBER
        }
        if (power !in MIN_POWER..MAX_POWER) {
            errors["power"] = ValidationErrorType.POWER_RANGE
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
