package com.stechoq.wms.data

/** Kept separate from ScanConfig so the actual message text lives in AppStrings, not the data layer. */
enum class ValidationErrorType {
    BASE_URL_FORMAT,
    READER_ID_REQUIRED,
    ANTENNA_NUMBER,
    RR_TYPE_REQUIRED,
    MAKER_NAME_REQUIRED,
    INITIAL_YEAR_NUMBER,
    FACTORY_CODE_REQUIRED,
    // A Factory Code is set but its numeric id isn't (e.g. saved before factoryId existed) —
    // only matters for Replacement, the one endpoint that sends the id instead of the code.
    FACTORY_ID_MISSING,
    POWER_RANGE
}
