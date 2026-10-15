package com.stechoq.wms.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ServerShortNameTest {

    @Test
    fun domainKeepsFirstTwoLabels() {
        assertEquals("wms.suite", ScanConfig.serverShortName("https://wms.suite.stechoq-j.com"))
        assertEquals("product.suite", ScanConfig.serverShortName("https://product.suite.stechoq-j.com/"))
    }

    @Test
    fun ipAddressIsKeptWholeWithPort() {
        assertEquals("192.168.1.3:8080", ScanConfig.serverShortName("http://192.168.1.3:8080"))
    }

    @Test
    fun shortHostIsKeptWhole() {
        assertEquals("localhost:8080", ScanConfig.serverShortName("http://localhost:8080"))
        assertEquals("example.com", ScanConfig.serverShortName("https://example.com/api"))
    }

    @Test
    fun blankUrlGivesBlank() {
        assertEquals("", ScanConfig.serverShortName(""))
    }
}
