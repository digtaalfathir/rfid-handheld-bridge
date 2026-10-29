package com.stechoq.wms.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class SendErrorMessageTest {

    @Test
    fun indonesianMessageNamesUnsentCount() {
        assertEquals(
            "16 tag belum terkirim — Terjadi kesalahan sistem, silakan hubungi Tim WMS",
            IndonesianStrings.unsentSendError(16, IndonesianStrings.systemErrorGeneric)
        )
    }

    @Test
    fun englishMessageNamesUnsentCount() {
        assertEquals(
            "3 tags not sent — System error, please contact WMS Team",
            EnglishStrings.unsentSendError(3, EnglishStrings.systemErrorGeneric)
        )
    }

    @Test
    fun updateConfirmNamesVersionWhenKnown() {
        assertEquals(
            "Versi v1.5.15. Update akan diunduh sekarang; setelah selesai, tekan Pasang untuk memasangnya.",
            IndonesianStrings.updateConfirmMessage("1.5.15")
        )
        assertEquals(IndonesianStrings.updateConfirmBody, IndonesianStrings.updateConfirmMessage(null))
    }

    @Test
    fun sendRetryButtonIsTranslated() {
        assertEquals("Kirim Ulang", IndonesianStrings.retrySend)
        assertEquals("Retry", EnglishStrings.retrySend)
    }
}
