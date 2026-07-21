package com.example.chainwayrfidbridge.data

import android.content.Context

class ConfigRepository(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("rfid_bridge_config", Context.MODE_PRIVATE)

    private val defaults = ScanConfig()

    fun load(): ScanConfig = ScanConfig(
        mode = ScanMode.fromKey(prefs.getString("mode", defaults.mode.key)),
        baseUrl = prefs.getString("base_url", defaults.baseUrl) ?: defaults.baseUrl,
        endpoint = prefs.getString("endpoint", defaults.endpoint) ?: defaults.endpoint,
        readerId = prefs.getString("reader_id", defaults.readerId) ?: defaults.readerId,
        antenna = prefs.getString("antenna", defaults.antenna) ?: defaults.antenna,
        rrType = prefs.getString("rr_type", defaults.rrType) ?: defaults.rrType,
        makerName = prefs.getString("maker_name", defaults.makerName) ?: defaults.makerName,
        initialYear = prefs.getString("initial_year", defaults.initialYear) ?: defaults.initialYear,
        power = prefs.getInt("power", defaults.power)
    )

    fun save(config: ScanConfig) {
        prefs.edit()
            .putString("mode", config.mode.key)
            .putString("base_url", config.baseUrl)
            .putString("endpoint", config.endpoint)
            .putString("reader_id", config.readerId)
            .putString("antenna", config.antenna)
            .putString("rr_type", config.rrType)
            .putString("maker_name", config.makerName)
            .putString("initial_year", config.initialYear)
            .putInt("power", config.power)
            .apply()
    }

    fun reset(): ScanConfig {
        prefs.edit().clear().apply()
        return defaults
    }

    fun antennaOptions(): List<String> =
        (ScanConfig.ANTENNA_OPTIONS + customOptions("custom_antenna_options")).distinct()

    fun rrTypeOptions(): List<String> =
        (ScanConfig.RR_TYPE_OPTIONS + customOptions("custom_rr_type_options")).distinct()

    fun rememberCustomAntenna(value: String) = addCustomOption("custom_antenna_options", value)

    fun rememberCustomRrType(value: String) = addCustomOption("custom_rr_type_options", value)

    private fun customOptions(key: String): Set<String> = prefs.getStringSet(key, emptySet()) ?: emptySet()

    private fun addCustomOption(key: String, value: String) {
        if (value.isBlank()) return
        val current = customOptions(key)
        if (value !in current) {
            prefs.edit().putStringSet(key, current + value).apply()
        }
    }
}
