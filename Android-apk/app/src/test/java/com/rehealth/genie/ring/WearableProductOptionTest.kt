package com.rehealth.genie.ring

import kotlin.test.Test
import kotlin.test.assertEquals

class WearableProductOptionTest {
    @Test
    fun `pilot exposes only HBand and one Viomi connection option`() {
        val products = listOf(
            WearableProductOption("RH-MRD-S01", "ReHealth 标准戒指"),
            WearableProductOption("RH-RW-P01", "ReHealth 高端戒指"),
            WearableProductOption("RH-HB-E01", "ReHealth 基础手环"),
            WearableProductOption("RH-VM-S8", "云米 S8 云端手表"),
            WearableProductOption("RH-VM-S9", "云米 S9 云端手表"),
        )

        val visible = userSelectableWearableProductOptions(products, activeProductCode = "RH-MRD-S01")

        assertEquals(
            listOf(
                WearableProductOption("RH-HB-E01", "HBand（MT116 蓝牙）"),
                WearableProductOption("RH-VM-S8", "云米（IMEI 云端）"),
            ),
            visible,
        )
    }

    @Test
    fun `active Viomi model remains the cloud option after filtering`() {
        val products = listOf(
            WearableProductOption("RH-HB-E01", "ReHealth 基础手环"),
            WearableProductOption("RH-VM-S8", "云米 S8 云端手表"),
            WearableProductOption("RH-VM-K9L", "云米 K9L 云端手表"),
        )

        val visible = userSelectableWearableProductOptions(products, activeProductCode = "RH-VM-K9L")

        assertEquals(WearableProductOption("RH-VM-K9L", "云米（IMEI 云端）"), visible.last())
    }

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
