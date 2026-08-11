package com.rehealth.genie.ui

import com.rehealth.genie.BuildConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RuntimeAttributionPiasPreviewTest {
    @Test
    fun `pias preview follows explicit debug seed switch`() {
        val preview = runtimeAttributionPiasPreview(historyDays = 0)
        if (!BuildConfig.SEED_FAKE_HEALTH_DATA) {
            assertNull(preview)
            return
        }

        val seededPreview = assertNotNull(preview)

        assertEquals("ready", seededPreview.status)
        assertEquals(90, seededPreview.historyDays)
        assertEquals(31, seededPreview.forecastNoAction.size)
        assertEquals(31, seededPreview.forecastWithPlan.size)
        assertTrue(seededPreview.headline.orEmpty().contains("Debug 模拟"))
    }
}
