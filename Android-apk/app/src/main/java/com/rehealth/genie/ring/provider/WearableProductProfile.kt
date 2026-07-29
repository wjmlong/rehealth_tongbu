package com.rehealth.genie.ring.provider

import android.content.Context
import com.google.gson.JsonParser
import com.rehealth.genie.ring.RingMetricType

data class WearableProductProfile(
    val productCode: String,
    val vendor: WearableVendor,
    val displayName: String,
    val modelNameHints: Set<String>,
    val expectedMetrics: Set<RingMetricType>,
)

class WearableProductCatalog(context: Context) {
    private val appContext = context.applicationContext

    val products: List<WearableProductProfile> by lazy {
        val json = appContext.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        parseWearableProductProfiles(json)
    }

    fun find(productCode: String): WearableProductProfile? =
        products.firstOrNull { it.productCode == productCode }

    private companion object {
        const val ASSET_NAME = "wearable_products.json"
    }
}

internal fun parseWearableProductProfiles(json: String): List<WearableProductProfile> {
    val profiles = JsonParser.parseString(json).asJsonArray.map { element ->
        val item = element.asJsonObject
        WearableProductProfile(
            productCode = item.requireString("productCode"),
            vendor = WearableVendor.valueOf(item.requireString("vendor")),
            displayName = item.requireString("displayName"),
            modelNameHints = item.requireStringSet("modelNameHints"),
            expectedMetrics = item.requireStringSet("expectedMetrics")
                .mapTo(linkedSetOf(), RingMetricType::valueOf),
        )
    }
    require(profiles.map { it.productCode }.distinct().size == profiles.size) {
        "wearable productCode values must be unique"
    }
    return profiles
}

private fun com.google.gson.JsonObject.requireString(name: String): String =
    get(name)?.takeUnless { it.isJsonNull }?.asString?.takeIf { it.isNotBlank() }
        ?: error("wearable product field '$name' is required")

private fun com.google.gson.JsonObject.requireStringSet(name: String): Set<String> =
    getAsJsonArray(name)?.mapTo(linkedSetOf()) { value -> value.asString }
        ?: error("wearable product field '$name' is required")

const val DEFAULT_MRD_PRODUCT_CODE = "RH-MRD-S01"
const val RWFIT_PRODUCT_CODE = "RH-RW-P01"
const val HBAND_PRODUCT_CODE = "RH-HB-E01"
const val MIWI4G_PRODUCT_CODE = "RH-S8-4G01"
const val DEBUG_MOCK_PRODUCT_CODE = "RH-MOCK-DEBUG"
