package com.example.chainwayrfidbridge.data

/** Kept separate from ScanConfig so the actual message text lives in AppStrings, not the data layer. */
enum class ValidationErrorType {
    BASE_URL_FORMAT,
    READER_ID_REQUIRED,
    ANTENNA_NUMBER,
    RR_TYPE_REQUIRED,
    MAKER_NAME_REQUIRED,
    INITIAL_YEAR_NUMBER,
    POWER_RANGE
}
