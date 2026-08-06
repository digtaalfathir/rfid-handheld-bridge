package com.example.chainwayrfidbridge.data

import android.content.Context
import android.os.Build
import android.provider.Settings

class ConfigRepository(context: Context) {

    private val appContext = context.applicationContext

    private val prefs = appContext
        .getSharedPreferences("rfid_bridge_config", Context.MODE_PRIVATE)

    // Separate file so "Reset Configuration" (which clears rfid_bridge_config) never
    // wipes out which hardware backend is in use — that's a more fundamental choice.
    private val devicePrefs = appContext
        .getSharedPreferences("rfid_bridge_device", Context.MODE_PRIVATE)

    private val defaults = ScanConfig()

    // Not user-editable — one physical handheld should never need to be renamed by hand.
    // "<model>-<short id>" reads much better than a raw 16-char ANDROID_ID while staying
    // unique per physical unit: 6 hex chars is 16M+ combinations, far more than any real
    // fleet size, and ANDROID_ID itself needs no special permission (unlike the hardware
    // serial, which is locked down on API 26+).
    private val deviceReaderId: String = run {
        val androidId = Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "unknown"
        val model = Build.MODEL.replace(Regex("\\s+"), "")
        "$model-${androidId.takeLast(6)}"
    }

    fun loadDeviceType(): DeviceType? = DeviceType.fromKey(devicePrefs.getString("device_type", null))

    fun saveDeviceType(type: DeviceType) {
        devicePrefs.edit().putString("device_type", type.key).apply()
    }

    // Also device-level, not part of the resettable scan config — same reasoning as device type:
    // resetting your API settings shouldn't silently flip the app back to English.
    fun loadLanguage(): AppLanguage = AppLanguage.fromKey(devicePrefs.getString("language", null))

    fun saveLanguage(language: AppLanguage) {
        devicePrefs.edit().putString("language", language.key).apply()
    }

    // Chainway's 1-30 dBm range as a fallback for a device type that isn't picked yet (shouldn't
    // normally happen — the picker screen runs before this ever loads a config for real use).
    private val powerRange: IntRange get() = loadDeviceType()?.powerRange ?: DeviceType.CHAINWAY.powerRange
    private val defaultPower: Int get() = loadDeviceType()?.defaultPower ?: DeviceType.CHAINWAY.defaultPower

    fun load(): ScanConfig = ScanConfig(
        mode = ScanMode.fromKey(prefs.getString("mode", defaults.mode.key)),
        baseUrl = prefs.getString("base_url", defaults.baseUrl) ?: defaults.baseUrl,
        endpoint = prefs.getString("endpoint", defaults.endpoint) ?: defaults.endpoint,
        readerId = deviceReaderId,
        antenna = prefs.getString("antenna", defaults.antenna) ?: defaults.antenna,
        rrType = prefs.getString("rr_type", defaults.rrType) ?: defaults.rrType,
        makerName = prefs.getString("maker_name", defaults.makerName) ?: defaults.makerName,
        initialYear = prefs.getString("initial_year", defaults.initialYear) ?: defaults.initialYear,
        factoryCode = prefs.getString("factory_code", defaults.factoryCode) ?: defaults.factoryCode,
        opname = prefs.getBoolean("opname", defaults.opname),
        // Clamp defensively: a value saved under a since-changed valid range (or any other
        // stale/corrupt data) should never surface as an out-of-range number in the UI. Range is
        // device-specific (Chainway 1-30, Zebra 0-300), not a single shared scale.
        power = prefs.getInt("power", defaultPower).coerceIn(powerRange.first, powerRange.last),
        soundEnabled = prefs.getBoolean("sound_enabled", defaults.soundEnabled),
        soundVolume = prefs.getInt("sound_volume", defaults.soundVolume)
            .coerceIn(ScanConfig.MIN_VOLUME, ScanConfig.MAX_VOLUME),
        localCsvEnabled = prefs.getBoolean("local_csv_enabled", defaults.localCsvEnabled),
        backgroundScanEnabled = prefs.getBoolean("background_scan_enabled", defaults.backgroundScanEnabled)
    )

    fun save(config: ScanConfig) {
        prefs.edit()
            .putString("mode", config.mode.key)
            .putString("base_url", config.baseUrl)
            .putString("endpoint", config.endpoint)
            // reader_id intentionally not persisted — always derived live from the device
            .putString("antenna", config.antenna)
            .putString("rr_type", config.rrType)
            .putString("maker_name", config.makerName)
            .putString("initial_year", config.initialYear)
            .putString("factory_code", config.factoryCode)
            .putBoolean("opname", config.opname)
            .putInt("power", config.power)
            .putBoolean("sound_enabled", config.soundEnabled)
            .putInt("sound_volume", config.soundVolume)
            .putBoolean("local_csv_enabled", config.localCsvEnabled)
            .putBoolean("background_scan_enabled", config.backgroundScanEnabled)
            .apply()
    }

    fun reset(): ScanConfig {
        prefs.edit().clear().apply()
        return defaults.copy(power = defaultPower)
    }

    fun antennaOptions(): List<String> =
        (ScanConfig.ANTENNA_OPTIONS + customOptions("custom_antenna_options")).distinct()

    fun rrTypeOptions(): List<String> =
        (ScanConfig.RR_TYPE_OPTIONS + customOptions("custom_rr_type_options")).distinct()

    fun baseUrlOptions(): List<String> =
        (ScanConfig.BASE_URL_OPTIONS + customOptions("custom_base_url_options")).distinct()

    fun endpointOptions(): List<String> =
        (ScanConfig.ENDPOINT_OPTIONS + customOptions("custom_endpoint_options")).distinct()

    fun initialYearOptions(): List<String> =
        (ScanConfig.INITIAL_YEAR_OPTIONS + customOptions("custom_initial_year_options")).distinct()

    fun factoryCodeOptions(): List<String> = customOptions("custom_factory_code_options").toList()

    fun rememberCustomAntenna(value: String) = addCustomOption("custom_antenna_options", value)

    fun rememberCustomRrType(value: String) = addCustomOption("custom_rr_type_options", value)

    fun rememberCustomBaseUrl(value: String) = addCustomOption("custom_base_url_options", value)

    fun rememberCustomEndpoint(value: String) = addCustomOption("custom_endpoint_options", value)

    fun rememberCustomInitialYear(value: String) = addCustomOption("custom_initial_year_options", value)

    fun rememberCustomFactoryCode(value: String) = addCustomOption("custom_factory_code_options", value)

    private fun customOptions(key: String): Set<String> = prefs.getStringSet(key, emptySet()) ?: emptySet()

    private fun addCustomOption(key: String, value: String) {
        if (value.isBlank()) return
        val current = customOptions(key)
        if (value !in current) {
            prefs.edit().putStringSet(key, current + value).apply()
        }
    }
}
