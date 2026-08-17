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
    val inputMode: InputMode = InputMode.RFID,
    val baseUrl: String = "",
    val endpoint: String = "/api/v1/warehouse-management/jmp/log-rfids/components/handheld",
    val readerId: String = "C72",
    val antenna: String = "1",
    val rrType: String = "T1B",
    val makerName: String = "",
    val initialYear: String = Calendar.getInstance().get(Calendar.YEAR).toString(),
    val factoryCode: String = "",
    // Register-only: whether this scan result also posts straight to current stock. Hidden in
    // the UI outside Register mode, but still carried in every payload alongside "mode" so the
    // backend can tell WO and Register submissions apart from the one shared endpoint.
    val opname: Boolean = false,
    val power: Int = 20,
    val soundEnabled: Boolean = true,
    val soundVolume: Int = 80,
    val localCsvEnabled: Boolean = true,
    // When on, the physical trigger keeps scanning while the app isn't in the foreground, and a
    // small floating status bubble is shown (requires the "draw over other apps" permission).
    // When off, a trigger press is ignored entirely unless the app is actually visible.
    val backgroundScanEnabled: Boolean = false
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
        val ENDPOINT_OPTIONS = listOf(
            "/api/v1/warehouse-management/jmp/log-rfids/components/handheld"
        )
        val INITIAL_YEAR_OPTIONS = (2000..2026).map { it.toString() }
    }

    /** Combines baseUrl + endpoint into the full request URL, regardless of slashes on either side. */
    fun fullApiUrl(): String {
        val base = baseUrl.trimEnd('/')
        val path = endpoint.trim().trim('/')
        return if (path.isEmpty()) base else "$base/$path"
    }

    /** Field name -> error type, empty when the config is valid. Display text lives in AppStrings.
     * [powerRange] comes from the active RfidReaderManager — it's device-specific (Chainway 1-30
     * dBm, Zebra 0-300), not a fixed constant. */
    fun validate(powerRange: IntRange = MIN_POWER..MAX_POWER): Map<String, ValidationErrorType> {
        val errors = mutableMapOf<String, ValidationErrorType>()
        if (baseUrl.isBlank() || (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://"))) {
            errors["baseUrl"] = ValidationErrorType.BASE_URL_FORMAT
        }
        if (readerId.isBlank()) errors["readerId"] = ValidationErrorType.READER_ID_REQUIRED
        if (antenna.toIntOrNull() == null) errors["antenna"] = ValidationErrorType.ANTENNA_NUMBER
        if (rrType.isBlank()) errors["rrType"] = ValidationErrorType.RR_TYPE_REQUIRED
        if (makerName.isBlank()) errors["makerName"] = ValidationErrorType.MAKER_NAME_REQUIRED
        if (initialYear.toIntOrNull() == null) errors["initialYear"] = ValidationErrorType.INITIAL_YEAR_NUMBER
        if (power !in powerRange) {
            errors["power"] = ValidationErrorType.POWER_RANGE
        }
        return errors
    }
}
