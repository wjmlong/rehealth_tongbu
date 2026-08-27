package com.rehealth.genie.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ScanCodeParserTest {
    @Test
    fun parsesTheSchemePayloadWithCodeAndTenant() {
        assertEquals(
            "ABCD2345",
            ScanCodeParser.parseEmployeeCode("rehealth://insurance/scan?c=ABCD2345&t=9101"),
        )
    }

    @Test
    fun parsesALowerCaseSchemePayloadIntoUpperCase() {
        assertEquals(
            "ABCD2345",
            ScanCodeParser.parseEmployeeCode("rehealth://insurance/scan?c=abcd2345&t=9101"),
        )
    }

    @Test
    fun acceptsAPlainEmployeeCode() {
        assertEquals("ABCD2345", ScanCodeParser.parseEmployeeCode(" ABCD2345 "))
    }

    @Test
    fun rejectsEmptyOrMalformedContent() {
        assertNull(ScanCodeParser.parseEmployeeCode(null))
        assertNull(ScanCodeParser.parseEmployeeCode("   "))
        assertNull(ScanCodeParser.parseEmployeeCode("rehealth://insurance/scan?c=&t=9101"))
        assertNull(ScanCodeParser.parseEmployeeCode("https://example.com/other"))
        assertNull(ScanCodeParser.parseEmployeeCode("AB"))
    }
}
