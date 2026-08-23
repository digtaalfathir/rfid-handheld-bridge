package com.example.chainwayrfidbridge.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.chainwayrfidbridge.SortOption
import com.example.chainwayrfidbridge.data.AppLanguage
import com.example.chainwayrfidbridge.data.DeviceType
import com.example.chainwayrfidbridge.data.InputMode
import com.example.chainwayrfidbridge.data.TagQuality
import com.example.chainwayrfidbridge.data.ValidationErrorType

/**
 * Every user-facing string in the app, in one place, so switching language in Settings updates
 * everything consistently instead of screens drifting out of sync with each other.
 */
data class AppStrings(
    // Device picker
    val pickerTitle: String,
    val pickerSubtitle: String,
    val deviceChainwayLabel: String,
    val deviceChainwayDescription: String,
    val deviceZebraLabel: String,
    val deviceZebraDescription: String,

    // Scan screen
    val appTitle: String,
    val settingsDescription: String,
    val totalDetected: String,
    val totalUnique: String,
    val lastScanPrefix: String,
    val stopScan: String,
    val startScan: String,
    val scanNew: String,
    val continueScan: String,
    val scanStartErrorPrefix: String,
    val sending: String,
    val sentPrefix: String,
    val tagWord: String,
    val sendErrorPrefix: String,
    val retry: String,
    val emptyTagList: String,
    val searchPlaceholder: String,
    val tagNew: String,
    val tagExisting: String,
    val copyEpcDescription: String,
    val epcCopiedToast: String,
    val sortDescription: String,
    val sortLastSeen: String,
    val sortEpcAsc: String,
    val sortReadCount: String,
    val sortQuality: String,
    val qualityLabel: String,
    val qualityStrong: String,
    val qualityMedium: String,
    val qualityWeak: String,
    val updateAvailablePrefix: String,
    val updateNowButton: String,
    val downloadingUpdate: String,
    val updateReadyToInstall: String,
    val installUpdateButton: String,
    val updateErrorPrefix: String,

    // Barcode mode
    val barcodeEmptyList: String,
    val barcodeHint: String,
    val barcodeSendingLabel: String,
    val barcodeSentLabel: String,
    val barcodeFailedLabel: String,
    val totalScanned: String,

    // Settings screen
    val settingsTitle: String,
    val backDescription: String,
    val scanModeTitle: String,
    val scanModeRfidLabel: String,
    val scanModeBarcodeLabel: String,
    val modeTitle: String,
    val apiConfigTitle: String,
    val baseUrlLabel: String,
    val endpointLabel: String,
    val readerIdLabel: String,
    val antennaLabel: String,
    val testConnection: String,
    val testing: String,
    val serverReachable: String,
    val testFailedPrefix: String,
    val dropdownFetchFailed: String,
    val registerConfigTitle: String,
    val rrTypeLabel: String,
    val makerNameLabel: String,
    val initialYearLabel: String,
    val factoryCodeLabel: String,
    val opnameLabel: String,
    val powerTitle: String,
    val powerLevelPrefix: String,
    val soundTitle: String,
    val soundToggleLabel: String,
    val soundVolumeLabel: String,
    val localCsvTitle: String,
    val localCsvToggleLabel: String,
    val backgroundScanTitle: String,
    val backgroundScanToggleLabel: String,
    val backgroundScanActive: String,
    val overlayPermissionToast: String,
    val languageTitle: String,
    val languageEnglish: String,
    val languageIndonesian: String,
    val reset: String,
    val configResetToast: String,
    val save: String,
    val configSavedToast: String,
    val configInvalidToast: String,
    val versionPrefix: String,

    // Validation errors
    val errorBaseUrlFormat: String,
    val errorReaderIdRequired: String,
    val errorAntennaNumber: String,
    val errorRrTypeRequired: String,
    val errorMakerNameRequired: String,
    val errorInitialYearNumber: String,
    val errorPowerRange: String
) {
    fun sentSuccess(count: Int): String = "$sentPrefix ($count $tagWord)"

    fun deviceLabel(type: DeviceType): String = when (type) {
        DeviceType.CHAINWAY -> deviceChainwayLabel
        DeviceType.ZEBRA -> deviceZebraLabel
    }

    fun deviceDescription(type: DeviceType): String = when (type) {
        DeviceType.CHAINWAY -> deviceChainwayDescription
        DeviceType.ZEBRA -> deviceZebraDescription
    }

    fun sortLabel(option: SortOption): String = when (option) {
        SortOption.LAST_SEEN_DESC -> sortLastSeen
        SortOption.EPC_ASC -> sortEpcAsc
        SortOption.READ_COUNT_DESC -> sortReadCount
        SortOption.QUALITY_DESC -> sortQuality
    }

    fun qualityText(quality: TagQuality): String = when (quality) {
        TagQuality.STRONG -> qualityStrong
        TagQuality.MEDIUM -> qualityMedium
        TagQuality.WEAK -> qualityWeak
    }

    fun scanModeLabel(mode: InputMode): String = when (mode) {
        InputMode.RFID -> scanModeRfidLabel
        InputMode.BARCODE -> scanModeBarcodeLabel
    }

    fun validationMessage(type: ValidationErrorType): String = when (type) {
        ValidationErrorType.BASE_URL_FORMAT -> errorBaseUrlFormat
        ValidationErrorType.READER_ID_REQUIRED -> errorReaderIdRequired
        ValidationErrorType.ANTENNA_NUMBER -> errorAntennaNumber
        ValidationErrorType.RR_TYPE_REQUIRED -> errorRrTypeRequired
        ValidationErrorType.MAKER_NAME_REQUIRED -> errorMakerNameRequired
        ValidationErrorType.INITIAL_YEAR_NUMBER -> errorInitialYearNumber
        ValidationErrorType.POWER_RANGE -> errorPowerRange
    }
}

private const val POWER_RANGE_TEXT = "1-30"

val EnglishStrings = AppStrings(
    pickerTitle = "Select Device Type",
    pickerSubtitle = "This choice determines how the app connects to the RFID reader.",
    deviceChainwayLabel = "Chainway C72",
    deviceChainwayDescription = "Chainway C72 built-in UHF reader",
    deviceZebraLabel = "Zebra",
    deviceZebraDescription = "Zebra handheld built-in UHF reader",

    appTitle = "Stechoq RFID Suite",
    settingsDescription = "Settings",
    totalDetected = "Total Detected",
    totalUnique = "Total Unique",
    lastScanPrefix = "Last scan: ",
    stopScan = "Stop Scan",
    startScan = "Start Scan",
    scanNew = "New Scan",
    continueScan = "Continue",
    scanStartErrorPrefix = "Failed to start scan: ",
    sending = "Sending...",
    sentPrefix = "Sent",
    tagWord = "tags",
    sendErrorPrefix = "Failed to send: ",
    retry = "Retry",
    emptyTagList = "No tags read yet",
    searchPlaceholder = "Search EPC...",
    tagNew = "NEW",
    tagExisting = "EXISTING",
    copyEpcDescription = "Copy EPC",
    epcCopiedToast = "EPC copied",
    sortDescription = "Sort",
    sortLastSeen = "Most Recent",
    sortEpcAsc = "EPC A-Z",
    sortReadCount = "Read Count",
    sortQuality = "Quality",
    qualityLabel = "Quality: ",
    qualityStrong = "Strong",
    qualityMedium = "Medium",
    qualityWeak = "Weak",
    updateAvailablePrefix = "Update available: v",
    updateNowButton = "Update Now",
    downloadingUpdate = "Downloading update...",
    updateReadyToInstall = "Update ready to install",
    installUpdateButton = "Install",
    updateErrorPrefix = "Update failed: ",

    barcodeEmptyList = "No barcodes scanned yet",
    barcodeHint = "Hold the trigger to scan a barcode",
    barcodeSendingLabel = "Sending...",
    barcodeSentLabel = "Sent",
    barcodeFailedLabel = "Failed",
    totalScanned = "Total Scanned",

    settingsTitle = "Settings",
    backDescription = "Back",
    scanModeTitle = "Scan Mode",
    scanModeRfidLabel = "RFID",
    scanModeBarcodeLabel = "Barcode",
    modeTitle = "Mode",
    apiConfigTitle = "API Configuration",
    baseUrlLabel = "Base URL",
    endpointLabel = "Endpoint",
    readerIdLabel = "Reader ID",
    antennaLabel = "Antenna",
    testConnection = "Test & Get Data",
    testing = "Testing...",
    serverReachable = "Server is reachable",
    testFailedPrefix = "Failed: ",
    dropdownFetchFailed = "Could not load RR Type / Factory Code from the server",
    registerConfigTitle = "Register Configuration",
    rrTypeLabel = "RR Type",
    makerNameLabel = "Maker Name",
    initialYearLabel = "Initial Year",
    factoryCodeLabel = "Factory Code",
    opnameLabel = "Scan result is sent to current stock",
    powerTitle = "Power",
    powerLevelPrefix = "Level ",
    soundTitle = "Sound",
    soundToggleLabel = "Beep on tag read",
    soundVolumeLabel = "Volume",
    localCsvTitle = "Local Backup",
    localCsvToggleLabel = "Save each scan to a CSV file on the device",
    backgroundScanTitle = "Background Scanning",
    backgroundScanToggleLabel = "Keep scanning when the app is in the background (shows a floating status bubble)",
    backgroundScanActive = "Background scanning active",
    overlayPermissionToast = "Please allow \"Display over other apps\" for this feature",
    languageTitle = "Language",
    languageEnglish = "English",
    languageIndonesian = "Indonesia",
    reset = "Reset",
    configResetToast = "Configuration reset to default",
    save = "Save",
    configSavedToast = "Configuration saved",
    configInvalidToast = "Please check the invalid fields",
    versionPrefix = "Version ",

    errorBaseUrlFormat = "Base URL must start with http:// or https://",
    errorReaderIdRequired = "Reader ID is required",
    errorAntennaNumber = "Antenna must be a number",
    errorRrTypeRequired = "RR Type is required",
    errorMakerNameRequired = "Maker Name is required",
    errorInitialYearNumber = "Initial Year must be a number",
    errorPowerRange = "Power must be between $POWER_RANGE_TEXT"
)

val IndonesianStrings = AppStrings(
    pickerTitle = "Pilih Tipe Device",
    pickerSubtitle = "Pilihan ini menentukan cara aplikasi terhubung ke reader RFID.",
    deviceChainwayLabel = "Chainway C72",
    deviceChainwayDescription = "Reader UHF bawaan Chainway C72",
    deviceZebraLabel = "Zebra",
    deviceZebraDescription = "Reader UHF bawaan handheld Zebra",

    appTitle = "Stechoq RFID Suite",
    settingsDescription = "Pengaturan",
    totalDetected = "Total Terdeteksi",
    totalUnique = "Total Unik",
    lastScanPrefix = "Scan terakhir: ",
    stopScan = "Stop Scan",
    startScan = "Mulai Scan",
    scanNew = "Scan Baru",
    continueScan = "Lanjutkan",
    scanStartErrorPrefix = "Gagal memulai scan: ",
    sending = "Mengirim...",
    sentPrefix = "Terkirim",
    tagWord = "tag",
    sendErrorPrefix = "Gagal kirim: ",
    retry = "Retry",
    emptyTagList = "Belum ada tag terbaca",
    searchPlaceholder = "Cari EPC...",
    tagNew = "BARU",
    tagExisting = "LAMA",
    copyEpcDescription = "Copy EPC",
    epcCopiedToast = "EPC disalin",
    sortDescription = "Urutkan",
    sortLastSeen = "Terbaru",
    sortEpcAsc = "EPC A-Z",
    sortReadCount = "Read Count",
    sortQuality = "Kualitas",
    qualityLabel = "Kualitas: ",
    qualityStrong = "Kuat",
    qualityMedium = "Sedang",
    qualityWeak = "Lemah",
    updateAvailablePrefix = "Update tersedia: v",
    updateNowButton = "Update Sekarang",
    downloadingUpdate = "Mengunduh update...",
    updateReadyToInstall = "Update siap dipasang",
    installUpdateButton = "Pasang",
    updateErrorPrefix = "Update gagal: ",

    barcodeEmptyList = "Belum ada barcode discan",
    barcodeHint = "Tahan trigger untuk scan barcode",
    barcodeSendingLabel = "Mengirim...",
    barcodeSentLabel = "Terkirim",
    barcodeFailedLabel = "Gagal",
    totalScanned = "Total Discan",

    settingsTitle = "Pengaturan",
    backDescription = "Kembali",
    scanModeTitle = "Mode Scan",
    scanModeRfidLabel = "RFID",
    scanModeBarcodeLabel = "Barcode",
    modeTitle = "Mode",
    apiConfigTitle = "Konfigurasi API",
    baseUrlLabel = "Base URL",
    endpointLabel = "Endpoint",
    readerIdLabel = "Reader ID",
    antennaLabel = "Antenna",
    testConnection = "Tes & Ambil Data",
    testing = "Menguji...",
    serverReachable = "Server dapat dijangkau",
    testFailedPrefix = "Gagal: ",
    dropdownFetchFailed = "Gagal memuat RR Type / Factory Code dari server",
    registerConfigTitle = "Register Configuration",
    rrTypeLabel = "RR Type",
    makerNameLabel = "Maker Name",
    initialYearLabel = "Initial Year",
    factoryCodeLabel = "Factory Code",
    opnameLabel = "Hasil scan langsung dikirim ke stok saat ini",
    powerTitle = "Power",
    powerLevelPrefix = "Level ",
    soundTitle = "Suara",
    soundToggleLabel = "Bunyi saat tag terbaca",
    soundVolumeLabel = "Volume",
    localCsvTitle = "Backup Lokal",
    localCsvToggleLabel = "Simpan setiap hasil scan ke file CSV di perangkat",
    backgroundScanTitle = "Scan Latar Belakang",
    backgroundScanToggleLabel = "Tetap scan walau aplikasi di latar belakang (muncul bulatan status mengambang)",
    backgroundScanActive = "Scan latar belakang aktif",
    overlayPermissionToast = "Mohon izinkan \"Tampil di atas aplikasi lain\" untuk fitur ini",
    languageTitle = "Bahasa",
    languageEnglish = "English",
    languageIndonesian = "Indonesia",
    reset = "Reset",
    configResetToast = "Konfigurasi direset ke default",
    save = "Simpan",
    configSavedToast = "Konfigurasi tersimpan",
    configInvalidToast = "Periksa kembali input yang belum valid",
    versionPrefix = "Versi ",

    errorBaseUrlFormat = "Base URL harus diawali http:// atau https://",
    errorReaderIdRequired = "Reader ID wajib diisi",
    errorAntennaNumber = "Antenna harus berupa angka",
    errorRrTypeRequired = "RR Type wajib diisi",
    errorMakerNameRequired = "Maker Name wajib diisi",
    errorInitialYearNumber = "Initial Year harus berupa angka",
    errorPowerRange = "Power harus antara $POWER_RANGE_TEXT"
)

fun stringsFor(language: AppLanguage): AppStrings = when (language) {
    AppLanguage.EN -> EnglishStrings
    AppLanguage.ID -> IndonesianStrings
}

val LocalStrings = staticCompositionLocalOf { EnglishStrings }
