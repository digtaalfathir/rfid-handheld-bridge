package com.example.chainwayrfidbridge.data

enum class AppLanguage(val key: String) {
    EN("en"),
    ID("id");

    companion object {
        fun fromKey(key: String?): AppLanguage = entries.find { it.key == key } ?: EN
    }
}
