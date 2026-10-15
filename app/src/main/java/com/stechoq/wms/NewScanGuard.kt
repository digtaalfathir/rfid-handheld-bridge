package com.stechoq.wms

/** What a New Scan request (on-screen button or physical trigger) should do with the tags still
 * on screen. */
enum class NewScanDecision {
    /** Nothing at risk — clear and start right away (the long-standing behavior). */
    START,
    /** The last send failed, so these tags never reached the server — ask before discarding. */
    CONFIRM_DISCARD,
    /** A send is still in flight; clearing now would lose the tags if it then fails. */
    IGNORE
}

/** The "unsent tags" confirmation. [fromTrigger] decides what "Discard & new scan" does: the
 * physical trigger is hold-to-scan, and it's no longer held by the time the operator taps the
 * dialog — so starting a scan then would run on with nothing to release. Discarding only clears,
 * and the next trigger press scans. From the on-screen New Scan button it behaves like that
 * button (clear, then start; Stop ends it). */
data class UnsentTagsPrompt(val count: Int, val fromTrigger: Boolean)

/** Any failure counts (network, timeout, HTTP error, server-side rejection): the app can't tell
 * whether a resend would succeed, so it asks rather than silently dropping tags. */
fun newScanDecision(sendStatus: SendStatus, tagCount: Int): NewScanDecision = when {
    tagCount == 0 -> NewScanDecision.START
    sendStatus is SendStatus.Sending -> NewScanDecision.IGNORE
    sendStatus is SendStatus.Error -> NewScanDecision.CONFIRM_DISCARD
    else -> NewScanDecision.START
}
