package com.rehealth.genie.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RhiManualInputValidationTest {
    @Test
    fun `blank manual fields remain missing rather than becoming normal values`() {
        val parsed = validateRhiManualInput(blankDraft()).getOrThrow()

        assertNull(parsed.sedentaryHoursPerDay)
        assertNull(parsed.waistCircumferenceCm)
        assertNull(parsed.vo2MaxMlKgMin)
        assertNull(parsed.hba1cPercent)
        assertNull(parsed.egfrMlMin173m2)
    }

    @Test
    fun `confirmed manual fields preserve entered values`() {
        val parsed = validateRhiManualInput(
            RhiManualInputDraft("8", "82.5", "36", "5.6", "96"),
        ).getOrThrow()

        assertEquals(8.0, parsed.sedentaryHoursPerDay)
        assertEquals(82.5, parsed.waistCircumferenceCm)
        assertEquals(36.0, parsed.vo2MaxMlKgMin)
        assertEquals(5.6, parsed.hba1cPercent)
        assertEquals(96.0, parsed.egfrMlMin173m2)
    }

    @Test
    fun `out of range clinical input is rejected`() {
        assertTrue(
            validateRhiManualInput(blankDraft().copy(hba1cPercent = "25")).isFailure,
        )
    }

    @Test
    fun `confirmed cuff requires both pressures and valid day count`() {
        assertTrue(
            validateRhiManualInput(
                blankDraft().copy(
                    cuffSbp7dMean = "128",
                    cuffConfirmed = true,
                ),
            ).isFailure,
        )
        val parsed = validateRhiManualInput(
            blankDraft().copy(
                cuffSbp7dMean = "128",
                cuffDbp7dMean = "78",
                cuffValidDays = "7",
                cuffConfirmed = true,
            ),
        ).getOrThrow()

        assertEquals(128.0, parsed.cuffSbp7dMean)
        assertEquals(78.0, parsed.cuffDbp7dMean)
        assertEquals(7, parsed.cuffValidDays)
        assertTrue(parsed.cuffConfirmed)
    }

    @Test
    fun `confirmed hospital lab requires a value and report date`() {
        assertTrue(
            validateRhiManualInput(blankDraft().copy(labConfirmed = true)).isFailure,
        )
        val parsed = validateRhiManualInput(
            blankDraft().copy(
                ldlMmolL = "3.2",
                labRecordedDate = "2026-07-01",
                labConfirmed = true,
            ),
        ).getOrThrow()

        assertEquals(3.2, parsed.ldlMmolL)
        assertTrue(parsed.labConfirmed)
        assertTrue(parsed.labRecordedAt != null)
    }

    private fun blankDraft() = RhiManualInputDraft("", "", "", "", "")
}
