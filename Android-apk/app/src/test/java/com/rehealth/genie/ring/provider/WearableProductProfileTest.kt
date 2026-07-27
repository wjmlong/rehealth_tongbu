package com.rehealth.genie.ring.provider

import com.rehealth.genie.ring.RingMetricType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WearableProductProfileTest {
    @Test
    fun parsesDomainValuesWithoutVendorSdkTypes() {
        val profiles = parseWearableProductProfiles(
            """
            [
              {
                "productCode": "RH-MRD-S01",
                "vendor": "MRD",
                "displayName": "MRD Ring",
                "modelNameHints": ["MR11"],
                "expectedMetrics": ["HEART_RATE", "SLEEP"]
              }
            ]
            """.trimIndent(),
        )

        assertEquals(WearableVendor.MRD, profiles.single().vendor)
        assertEquals(setOf("MR11"), profiles.single().modelNameHints)
        assertEquals(setOf(RingMetricType.HEART_RATE, RingMetricType.SLEEP), profiles.single().expectedMetrics)
    }

    @Test
    fun rejectsDuplicateProductCodes() {
        val json = """
            [
              {"productCode":"DUP","vendor":"MRD","displayName":"A","modelNameHints":[],"expectedMetrics":[]},
              {"productCode":"DUP","vendor":"MRD","displayName":"B","modelNameHints":[],"expectedMetrics":[]}
            ]
        """.trimIndent()

        assertFailsWith<IllegalArgumentException> { parseWearableProductProfiles(json) }
    }
}
