package com.stechoq.wms.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class InitialYearOptionsTest {

    @Test
    fun runFromFirstYearToGivenYear() {
        val options = ScanConfig.initialYearOptions(currentYear = 2027)
        assertEquals("2000", options.first())
        assertEquals("2027", options.last())
        assertEquals(28, options.size)
    }

    @Test
    fun includeDefaultInitialYear() {
        // The default initialYear is the device's current year — it must always be pickable.
        assertTrue(ScanConfig().initialYear in ScanConfig.initialYearOptions())
        assertEquals(
            Calendar.getInstance().get(Calendar.YEAR).toString(),
            ScanConfig.initialYearOptions().last()
        )
    }
}
