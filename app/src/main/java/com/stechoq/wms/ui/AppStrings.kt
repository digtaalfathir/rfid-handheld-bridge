package com.stechoq.wms.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.stechoq.wms.SortOption
import com.stechoq.wms.data.AppLanguage
import com.stechoq.wms.data.DeviceType
import com.stechoq.wms.data.InputMode
import com.stechoq.wms.data.ReplacementReason
import com.stechoq.wms.data.SendContext
import com.stechoq.wms.data.SendType
import com.stechoq.wms.data.TagQuality
import com.stechoq.wms.data.ValidationErrorType

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
    val systemErrorGeneric: String,
    val retry: String,
    val retrySend: String,
    val notSentLabel: String,
    val unsentTagsTitle: String,
    val unsentTagsMessagePrefix: String,
    val unsentTagsMessageSuffix: String,
    val unsentTagsSendFirst: String,
    val unsentTagsDiscard: String,
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
    val dropdownFetchLoading: String,
    val dropdownFetchConnectedPrefix: String,
    val dropdownFetchFailed: String,
    val dropdownFetchStaleHint: String,
    val tryAgain: String,
    val registerConfigTitle: String,
    val rrTypeLabel: String,
    val makerNameLabel: String,
    val initialYearLabel: String,
    val factoryCodeLabel: String,
    val opnameLabel: String,
    val replacementModeLabel: String,
    val replacementModeDescription: String,
    val replacementDialogTitle: String,
    val replacementNewRfidLabel: String,
    val replacementOldRfidLabel: String,
    val replacementOldRfidHint: String,
    val replacementReasonLabel: String,
    val replacementReasonDamaged: String,
    val replacementReasonUnreadable: String,
    val replacementSuccessToast: String,
    val replacementFactoryMissing: String,
    val submit: String,
    val cancel: String,
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

    // Log history (Settings)
    val logHistoryTitle: String,
    val logHistoryEmpty: String,
    val logHistorySuccessLabel: String,
    val logHistoryFailedLabel: String,
    val logTypeRfid: String,
    val logTypeBarcode: String,
    val logTypeReplacement: String,
    val logOpname: String,
    val logCodeWord: String,
    val logShowDetails: String,
    val logHideDetails: String,
    val logUrlLabel: String,
    val logPayloadLabel: String,
    val logResponseLabel: String,
    val logResentFromPrefix: String,
    val logResend: String,
    val logResendConfirmTitle: String,
    val logResendConfirmMessage: String,
    val logResendDuplicateWarning: String,
    val logResendSuccessToast: String,
    val logResendFailedToast: String,
    val close: String,

    // Validation errors
    val errorBaseUrlFormat: String,
    val errorReaderIdRequired: String,
    val errorAntennaNumber: String,
    val errorRrTypeRequired: String,
    val errorMakerNameRequired: String,
    val errorInitialYearNumber: String,
    val errorFactoryCodeRequired: String,
    val errorFactoryIdMissing: String,
    val errorPowerRange: String
) {
    fun sentSuccess(count: Int): String = "$sentPrefix ($count $tagWord)"

    /** e.g. "16 tag belum terkirim — Terjadi kesalahan sistem, silakan hubungi Tim WMS". */
    fun unsentSendError(count: Int, reason: String): String = "$count $tagWord $notSentLabel — $reason"

    fun dropdownFetchConnected(rrTypeCount: Int, factoryCount: Int): String =
        "$dropdownFetchConnectedPrefix · $rrTypeCount RR Type, $factoryCount Factory"

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

    fun unsentTagsMessage(count: Int): String = "$unsentTagsMessagePrefix$count$unsentTagsMessageSuffix"

    fun reasonLabel(reason: ReplacementReason): String = when (reason) {
        ReplacementReason.DAMAGED -> replacementReasonDamaged
        ReplacementReason.UNREADABLE -> replacementReasonUnreadable
    }

    fun qualityText(quality: TagQuality): String = when (quality) {
        TagQuality.STRONG -> qualityStrong
        TagQuality.MEDIUM -> qualityMedium
        TagQuality.WEAK -> qualityWeak
    }

    /** Log History badge, e.g. "RFID · Register · Opname", "Barcode · WO", "Replacement". */
    fun logBadge(context: SendContext): String = when (context.type) {
        SendType.REPLACEMENT -> logTypeReplacement
        SendType.RFID_BATCH, SendType.BARCODE -> listOfNotNull(
            if (context.type == SendType.RFID_BATCH) logTypeRfid else logTypeBarcode,
            context.mode.label,
            logOpname.takeIf { context.opname }
        ).joinToString(" · ")
    }

    fun logItemCount(context: SendContext): String =
        "${context.itemCount} ${if (context.type == SendType.BARCODE) logCodeWord else tagWord}"

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
        ValidationErrorType.FACTORY_CODE_REQUIRED -> errorFactoryCodeRequired
        ValidationErrorType.FACTORY_ID_MISSING -> errorFactoryIdMissing
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

    appTitle = "WMS RFID Suite",
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
    systemErrorGeneric = "System error, please contact WMS Team",
    retry = "Retry",
    retrySend = "Retry",
    notSentLabel = "not sent",
    unsentTagsTitle = "Unsent tags",
    unsentTagsMessagePrefix = "There are ",
    unsentTagsMessageSuffix = " tags that haven't been sent to the server yet. Send them first, or discard them and start a new scan?",
    unsentTagsSendFirst = "Send first",
    unsentTagsDiscard = "Discard & new scan",
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
    dropdownFetchLoading = "Fetching data…",
    dropdownFetchConnectedPrefix = "Connected",
    dropdownFetchFailed = "Failed to fetch data from the server",
    dropdownFetchStaleHint = "RR Type / Factory Code lists may be from the previous server",
    tryAgain = "Try again",
    registerConfigTitle = "Register Configuration",
    rrTypeLabel = "RR Type",
    makerNameLabel = "Maker Name",
    initialYearLabel = "Initial Year",
    factoryCodeLabel = "Factory Code",
    opnameLabel = "Scan result is sent to current stock",
    replacementModeLabel = "RFID Replacement",
    replacementModeDescription = "Stop after the first tag detected and prompt to replace it, instead of scanning a batch",
    replacementDialogTitle = "Replace RFID Tag",
    replacementNewRfidLabel = "New RFID (scanned)",
    replacementOldRfidLabel = "Old RFID",
    replacementOldRfidHint = "Type the old tag's ID — it's assumed damaged/unreadable",
    replacementReasonLabel = "Reason",
    replacementReasonDamaged = "Damaged",
    replacementReasonUnreadable = "Unreadable",
    replacementSuccessToast = "Tag replaced successfully",
    replacementFactoryMissing = "Factory Code is not selected. Open Settings, check the Base URL, and pick a Factory Code.",
    submit = "Submit",
    cancel = "Cancel",
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

    logHistoryTitle = "Log History",
    logHistoryEmpty = "No send attempts logged yet",
    logHistorySuccessLabel = "Sent",
    logHistoryFailedLabel = "Failed",
    logTypeRfid = "RFID",
    logTypeBarcode = "Barcode",
    logTypeReplacement = "Replacement",
    logOpname = "Opname",
    logCodeWord = "codes",
    logShowDetails = "Details",
    logHideDetails = "Hide details",
    logUrlLabel = "URL",
    logPayloadLabel = "Payload",
    logResponseLabel = "Result",
    logResentFromPrefix = "Resent from ",
    logResend = "Resend",
    logResendConfirmTitle = "Resend this data?",
    logResendConfirmMessage = "The original data will be sent again, unchanged, to:",
    logResendDuplicateWarning = "This data was already sent successfully. Sending it again may record it twice on the server.",
    logResendSuccessToast = "Resent successfully",
    logResendFailedToast = "Resend failed — see the newest log entry",
    close = "Close",

    errorBaseUrlFormat = "Base URL must start with http:// or https://",
    errorReaderIdRequired = "Reader ID is required",
    errorAntennaNumber = "Antenna must be a number",
    errorRrTypeRequired = "RR Type is required",
    errorMakerNameRequired = "Maker Name is required",
    errorInitialYearNumber = "Initial Year must be a number",
    errorFactoryCodeRequired = "Factory Code is not selected, check the Base URL",
    errorFactoryIdMissing = "Re-select the Factory Code (its ID is missing)",
    errorPowerRange = "Power must be between $POWER_RANGE_TEXT"
)

val IndonesianStrings = AppStrings(
    pickerTitle = "Pilih Tipe Device",
    pickerSubtitle = "Pilihan ini menentukan cara aplikasi terhubung ke reader RFID.",
    deviceChainwayLabel = "Chainway C72",
    deviceChainwayDescription = "Reader UHF bawaan Chainway C72",
    deviceZebraLabel = "Zebra",
    deviceZebraDescription = "Reader UHF bawaan handheld Zebra",

    appTitle = "WMS RFID Suite",
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
    systemErrorGeneric = "Terjadi kesalahan sistem, silakan hubungi Tim WMS",
    // Update-download retry (banner) — the send banner's button uses retrySend below.
    retry = "Coba lagi",
    retrySend = "Kirim Ulang",
    notSentLabel = "belum terkirim",
    unsentTagsTitle = "Tag belum terkirim",
    unsentTagsMessagePrefix = "Ada ",
    unsentTagsMessageSuffix = " tag yang belum terkirim ke server. Kirim dulu, atau buang dan mulai scan baru?",
    unsentTagsSendFirst = "Kirim dulu",
    unsentTagsDiscard = "Buang & scan baru",
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
    dropdownFetchLoading = "Mengambil data…",
    dropdownFetchConnectedPrefix = "Terhubung",
    dropdownFetchFailed = "Gagal mengambil data dari server",
    dropdownFetchStaleHint = "Daftar RR Type / Factory Code mungkin dari server sebelumnya",
    tryAgain = "Coba lagi",
    registerConfigTitle = "Register Configuration",
    rrTypeLabel = "RR Type",
    makerNameLabel = "Maker Name",
    initialYearLabel = "Initial Year",
    factoryCodeLabel = "Factory Code",
    opnameLabel = "Hasil scan langsung dikirim ke stok saat ini",
    replacementModeLabel = "Penggantian RFID",
    replacementModeDescription = "Berhenti setelah tag pertama terdeteksi dan tampilkan form penggantian, bukan scan sekaligus banyak",
    replacementDialogTitle = "Ganti Tag RFID",
    replacementNewRfidLabel = "RFID Baru (hasil scan)",
    replacementOldRfidLabel = "RFID Lama",
    replacementOldRfidHint = "Ketik ID tag lama — dianggap rusak/tidak terbaca",
    replacementReasonLabel = "Alasan",
    replacementReasonDamaged = "Rusak",
    replacementReasonUnreadable = "Tidak Terbaca",
    replacementSuccessToast = "Tag berhasil diganti",
    replacementFactoryMissing = "Factory Code belum dipilih. Buka Settings, periksa Base URL, lalu pilih Factory Code.",
    submit = "Kirim",
    cancel = "Batal",
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

    logHistoryTitle = "Riwayat Log",
    logHistoryEmpty = "Belum ada riwayat pengiriman",
    logHistorySuccessLabel = "Terkirim",
    logHistoryFailedLabel = "Gagal",
    logTypeRfid = "RFID",
    logTypeBarcode = "Barcode",
    logTypeReplacement = "Penggantian",
    logOpname = "Opname",
    logCodeWord = "kode",
    logShowDetails = "Detail",
    logHideDetails = "Tutup detail",
    logUrlLabel = "URL",
    logPayloadLabel = "Payload",
    logResponseLabel = "Hasil",
    logResentFromPrefix = "Kirim ulang dari ",
    logResend = "Kirim ulang",
    logResendConfirmTitle = "Kirim ulang data ini?",
    logResendConfirmMessage = "Data asli akan dikirim lagi, tanpa diubah, ke:",
    logResendDuplicateWarning = "Data ini sudah pernah berhasil terkirim. Mengirim lagi bisa membuat data tercatat dua kali di server.",
    logResendSuccessToast = "Kirim ulang berhasil",
    logResendFailedToast = "Kirim ulang gagal — lihat entri log paling atas",
    close = "Tutup",

    errorBaseUrlFormat = "Base URL harus diawali http:// atau https://",
    errorReaderIdRequired = "Reader ID wajib diisi",
    errorAntennaNumber = "Antenna harus berupa angka",
    errorRrTypeRequired = "RR Type wajib diisi",
    errorMakerNameRequired = "Maker Name wajib diisi",
    errorInitialYearNumber = "Initial Year harus berupa angka",
    errorFactoryCodeRequired = "Factory Code belum dipilih, periksa Base URL",
    errorFactoryIdMissing = "Pilih ulang Factory Code (ID-nya belum tersimpan)",
    errorPowerRange = "Power harus antara $POWER_RANGE_TEXT"
)

fun stringsFor(language: AppLanguage): AppStrings = when (language) {
    AppLanguage.EN -> EnglishStrings
    AppLanguage.ID -> IndonesianStrings
}

val LocalStrings = staticCompositionLocalOf { EnglishStrings }
