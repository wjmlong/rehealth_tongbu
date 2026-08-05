package com.rehealth.genie.ring

import kotlin.test.Test
import kotlin.test.assertEquals

class WearableProductOptionTest {
    @Test
    fun `six Viomi models are displayed as one Viomi option`() {
        val products = listOf(
            WearableProductOption("RH-MRD-S01", "ReHealth 标准戒指"),
            WearableProductOption("RH-VM-S8", "云米 S8 云端手表"),
            WearableProductOption("RH-VM-S9", "云米 S9 云端手表"),
            WearableProductOption("RH-VM-GS20", "云米 GS20 云端手表"),
            WearableProductOption("RH-VM-GS17", "云米 GS17 云端手表"),
            WearableProductOption("RH-VM-A67", "云米 A67 云端手表"),
            WearableProductOption("RH-VM-K9L", "云米 K9L 云端手表"),
        )

        val visible = collapseViomiProductOptions(products, activeProductCode = "RH-MRD-S01")

        assertEquals(
            listOf(
                WearableProductOption("RH-MRD-S01", "ReHealth 标准戒指"),
                WearableProductOption("RH-VM-S8", "云米"),
            ),
            visible,
        )
    }

    @Test
    fun `active legacy Viomi product code remains selected behind vendor label`() {
        val products = listOf(
            WearableProductOption("RH-MRD-S01", "ReHealth 标准戒指"),
            WearableProductOption("RH-VM-S8", "云米 S8 云端手表"),
            WearableProductOption("RH-VM-K9L", "云米 K9L 云端手表"),
        )

        val visible = collapseViomiProductOptions(products, activeProductCode = "RH-VM-K9L")

        assertEquals(WearableProductOption("RH-VM-K9L", "云米"), visible.last())
    }
}
