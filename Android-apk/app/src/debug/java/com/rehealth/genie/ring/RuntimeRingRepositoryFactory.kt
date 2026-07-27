package com.rehealth.genie.ring

import android.content.Context
import android.os.Build
import com.rehealth.genie.BuildConfig
import com.rehealth.genie.ring.data.RingDataDao
import com.rehealth.genie.ring.mrd.MrdBleRingRepository
import com.rehealth.genie.ring.mrd.MrdProtocolAdapter
import com.rehealth.genie.ring.provider.ActiveWearableBindingStore
import com.rehealth.genie.ring.provider.DEBUG_MOCK_PRODUCT_CODE
import com.rehealth.genie.ring.provider.DEFAULT_MRD_PRODUCT_CODE
import com.rehealth.genie.ring.provider.WearableVendor

internal fun createRuntimeRingProviderFactories(
    context: Context,
    dao: RingDataDao,
    protocolAdapter: MrdProtocolAdapter,
    activeWearableStore: ActiveWearableBindingStore,
): Map<WearableVendor, () -> RingRepository> = mapOf(
    WearableVendor.MOCK to { MockRingRepository(dao) },
    WearableVendor.MRD to {
        MrdBleRingRepository(context, dao, protocolAdapter, activeWearableStore)
    },
)

internal fun runtimeDefaultWearableSelection(): Pair<String, WearableVendor> =
    if (shouldForceRuntimeWearableSelection()) {
        DEBUG_MOCK_PRODUCT_CODE to WearableVendor.MOCK
    } else {
        DEFAULT_MRD_PRODUCT_CODE to WearableVendor.MRD
    }

internal fun shouldForceRuntimeWearableSelection(): Boolean =
    BuildConfig.USE_FAKE_RING || (BuildConfig.SEED_FAKE_HEALTH_DATA && isProbablyEmulator())

private fun isProbablyEmulator(): Boolean {
    val fingerprint = Build.FINGERPRINT.lowercase()
    val model = Build.MODEL.lowercase()
    val product = Build.PRODUCT.lowercase()
    val brand = Build.BRAND.lowercase()
    return fingerprint.contains("generic") ||
        fingerprint.contains("emulator") ||
        model.contains("emulator") ||
        model.contains("android sdk built for") ||
        product.contains("sdk") ||
        product.contains("emulator") ||
        brand.contains("generic")
}
