package com.stechoq.wms.data

/** Hardcoded per the WMS team — only these two reasons exist for replacing a physical tag.
 * [wireValue] is the fixed, language-independent text sent as "replacement_remark"; the UI label
 * (EN/ID) lives in AppStrings, same split as TagQuality's wireValue vs its localized label. */
enum class ReplacementReason {
    DAMAGED, UNREADABLE;

    val wireValue: String
        get() = when (this) {
            DAMAGED -> "Damaged"
            UNREADABLE -> "Unreadable"
        }
}
