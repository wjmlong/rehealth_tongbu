package com.rehealth.genie.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class DataPeriodPresentationTest {
    @Test
    fun `data screen defaults to today`() {
        val defaultPeriod = DATA_PERIOD_OPTIONS[DEFAULT_DATA_PERIOD_INDEX]

        assertEquals("今日", defaultPeriod.first)
        assertEquals(0, defaultPeriod.second)
    }
}
