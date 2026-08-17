package com.example.chainwayrfidbridge.data

/**
 * Signal-quality classification shown on the tag list in place of a raw RSSI number, which
 * operators found meaningless on its own. The exact thresholds are deliberately not surfaced
 * anywhere in the UI or Settings.
 *
 * Grounded in commonly-cited UHF RFID (860-960MHz) RSSI bands: strong/near-field reads typically
 * sit above -50dBm, usable-but-marginal reads bottom out around -65dBm before reliability drops
 * off sharply toward a reader's noise floor. Read count acts as a confidence modifier on top of
 * that: a tag caught only once hasn't demonstrated a stable link the way repeated reads have, so
 * it's treated as one tier weaker than its RSSI alone would suggest.
 */
// Declared weakest-first so sortedByDescending (used for the "Quality" sort option) ranks
// STRONG's higher ordinal first without needing a separate comparator.
enum class TagQuality {
    WEAK, MEDIUM, STRONG;

    companion object {
        fun from(rssi: String, readCount: Int): TagQuality {
            val value = rssi.toDoubleOrNull()
            val rssiTier = when {
                value == null -> 0
                value >= -50.0 -> 2
                value >= -65.0 -> 1
                else -> 0
            }
            val tier = if (readCount <= 1 && rssiTier > 0) rssiTier - 1 else rssiTier
            return when (tier) {
                2 -> STRONG
                1 -> MEDIUM
                else -> WEAK
            }
        }
    }
}
