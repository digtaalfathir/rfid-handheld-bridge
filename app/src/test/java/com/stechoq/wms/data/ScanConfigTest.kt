package com.stechoq.wms.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanConfigTest {

    private val factories = listOf(
        FactoryCodeOption(382, "FAC1", "Factory One"),
        FactoryCodeOption(390, "FAC2", "Factory Two")
    )

    @Test
    fun keepsSelectionsPresentOnNewServer() {
        val config = ScanConfig(rrType = "T1B", factoryCode = "FAC2", factoryId = 390)
        val result = config.keepingOnlyAvailable(listOf("T1B", "T2A"), factories)
        assertEquals("T1B", result.rrType)
        assertEquals("FAC2", result.factoryCode)
        assertEquals(390, result.factoryId)
    }

    @Test
    fun clearsSelectionsMissingFromNewServer() {
        val config = ScanConfig(rrType = "OLD", factoryCode = "GONE", factoryId = 7)
        val result = config.keepingOnlyAvailable(listOf("T1B"), factories)
        assertEquals("", result.rrType)
        assertEquals("", result.factoryCode)
        assertEquals(0, result.factoryId)
    }

    @Test
    fun resyncsFactoryIdWhenSameCodeHasDifferentIdOnNewServer() {
        val config = ScanConfig(factoryCode = "FAC1", factoryId = 999)
        val result = config.keepingOnlyAvailable(emptyList(), factories)
        assertEquals("FAC1", result.factoryCode)
        assertEquals(382, result.factoryId)
    }

    private val validConfig = ScanConfig(
        baseUrl = "https://wms.suite.stechoq-j.com",
        makerName = "Acme",
        factoryCode = "FAC1",
        factoryId = 382
    )

    @Test
    fun validConfigPasses() {
        assertTrue(validConfig.validate().isEmpty())
    }

    @Test
    fun blankFactoryCodeIsRejected() {
        val errors = validConfig.copy(factoryCode = "", factoryId = 0).validate()
        assertEquals(ValidationErrorType.FACTORY_CODE_REQUIRED, errors["factoryCode"])
    }

    @Test
    fun missingFactoryIdIsRejectedOnlyWhenReplacementIsActive() {
        val noId = validConfig.copy(factoryId = 0)
        assertTrue(noId.validate().isEmpty())
        val replacement = noId.copy(mode = ScanMode.REGISTER, replacementModeEnabled = true)
        assertEquals(ValidationErrorType.FACTORY_ID_MISSING, replacement.validate()["factoryCode"])
        // Replacement left ticked but switched to WO isn't in effect, so it can't block Save.
        assertTrue(replacement.copy(mode = ScanMode.WO).validate().isEmpty())
    }

    @Test
    fun fetchableBaseUrlNeedsSchemeAndHost() {
        assertTrue(ScanConfig.isFetchableBaseUrl("https://wms.suite.stechoq-j.com"))
        assertTrue(ScanConfig.isFetchableBaseUrl("http://192.168.1.10:8000"))
        assertFalse(ScanConfig.isFetchableBaseUrl(""))
        assertFalse(ScanConfig.isFetchableBaseUrl("http://"))
        assertFalse(ScanConfig.isFetchableBaseUrl("https://"))
        assertFalse(ScanConfig.isFetchableBaseUrl("wms.suite.stechoq-j.com"))
    }
}
