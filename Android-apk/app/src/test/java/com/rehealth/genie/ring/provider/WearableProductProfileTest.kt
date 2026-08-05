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

    @Test
    fun mergesDebugProductsWithoutReplacingRealProducts() {
        val real = profile("RH-VM-S8", WearableVendor.VIOMI_CLOUD)
        val debug = profile("RH-MOCK-DEBUG", WearableVendor.MOCK)

        val merged = mergeWearableProductProfiles(listOf(real), listOf(debug))

        assertEquals(listOf("RH-VM-S8", "RH-MOCK-DEBUG"), merged.map { it.productCode })
    }

    @Test
    fun rejectsDuplicateCodesAcrossCatalogs() {
        val profile = profile("DUP", WearableVendor.MRD)

        assertFailsWith<IllegalArgumentException> {
            mergeWearableProductProfiles(listOf(profile), listOf(profile))
        }
    }

    private fun profile(productCode: String, vendor: WearableVendor) = WearableProductProfile(
        productCode = productCode,
        vendor = vendor,
        displayName = productCode,
        modelNameHints = emptySet(),
        expectedMetrics = emptySet(),
    )
}
