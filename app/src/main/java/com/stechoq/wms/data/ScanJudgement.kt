package com.stechoq.wms.data

import org.json.JSONObject

/** The active Work Order's progress snapshot (see Handheld RFID Scan Judgement Response.md,
 * object `data.wo`). [woNumber] is null only if the WO record has no number populated;
 * [woType], [qtyPlanning] and [qtyActual] are always populated per the spec. */
data class WoProgress(val woNumber: String?, val woType: String, val qtyPlanning: Int, val qtyActual: Int)

/** [wo] is null in Register mode (its response has no "wo" object at all). [noteCounts] groups the
 * per-tag "note" judgement into counts, in the order each note was first seen — not sorted, so
 * "Data Match" (almost always the biggest group) reliably lands first rather than jumping around. */
data class ScanJudgement(val wo: WoProgress?, val noteCounts: List<Pair<String, Int>>)

/** Parses the WO-mode-only judgement enrichment from a successful send's response body. Returns
 * null for anything that doesn't match that shape — Register mode's lightweight response has no
 * "tags" array, so it naturally no-ops here too without needing a separate mode check. */
fun parseScanJudgement(body: String): ScanJudgement? {
    return try {
        val data = JSONObject(body).optJSONObject("data") ?: return null
        val tags = data.optJSONArray("tags") ?: return null
        val wo = data.optJSONObject("wo")?.let {
            WoProgress(
                woNumber = if (it.isNull("wo_number")) null else it.optString("wo_number"),
                woType = it.optString("wo_type"),
                qtyPlanning = it.optInt("qty_planning"),
                qtyActual = it.optInt("qty_actual")
            )
        }
        val counts = LinkedHashMap<String, Int>()
        for (i in 0 until tags.length()) {
            val tag = tags.getJSONObject(i)
            val note = if (tag.isNull("note")) "" else tag.optString("note")
            val key = note.ifBlank { "Tidak Diketahui" }
            counts[key] = (counts[key] ?: 0) + 1
        }
        ScanJudgement(wo, counts.toList())
    } catch (e: Exception) {
        null
    }
}
