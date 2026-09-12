package com.example.chainwayrfidbridge.network

import org.json.JSONObject

/** Result of interpreting a raw [ApiClient] failure string for on-screen display. */
data class SendErrorDisplay(val message: String, val isSystemError: Boolean)

private val HTTP_ERROR_REGEX = Regex("^HTTP (\\d+) (.*)$", RegexOption.DOT_MATCHES_ALL)

/** Turns ApiClient's raw failure string ("HTTP 500 {...}" or a bare exception message) into a
 * display-ready (message, isSystemError) pair, per the operator-facing rule: HTTP 500 is
 * generalized — the raw body's actual cause is rarely something an operator can act on — and
 * shown as a system error; any other HTTP status instead shows just the body's "message" field,
 * which is normally a real, specific, worth-reading validation message. The full raw [raw] string
 * is never lost — callers log it separately (Settings' log history) regardless of what's shown
 * here. Anything that isn't a parsed HTTP response (a network exception, or an unparsable body)
 * falls back to showing the raw text as a system error, same as before this formatting existed. */
fun formatSendError(raw: String, systemErrorText: String): SendErrorDisplay {
    val match = HTTP_ERROR_REGEX.matchEntire(raw) ?: return SendErrorDisplay(raw, isSystemError = true)
    val code = match.groupValues[1].toIntOrNull()
    if (code == 500) return SendErrorDisplay(systemErrorText, isSystemError = true)
    val message = try {
        JSONObject(match.groupValues[2]).optString("message").takeIf { it.isNotBlank() }
    } catch (e: Exception) {
        null
    }
    return if (message != null) SendErrorDisplay(message, isSystemError = false) else SendErrorDisplay(raw, isSystemError = true)
}
