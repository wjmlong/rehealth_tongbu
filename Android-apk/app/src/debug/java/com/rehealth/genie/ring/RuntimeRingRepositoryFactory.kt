package com.rehealth.genie.ring

import android.content.Context
import android.os.Build
import com.rehealth.genie.BuildConfig
import com.rehealth.genie.ring.data.RingDataDao
import com.rehealth.genie.ring.hband.HBandRingRepository
import com.rehealth.genie.ring.hband.RealHBandSdkGateway
import com.rehealth.genie.ring.miwi.Miwi4gCloudRingRepository
import com.rehealth.genie.ring.mrd.MrdBleRingRepository
import com.rehealth.genie.ring.mrd.MrdProtocolAdapter
import com.rehealth.genie.ring.provider.ActiveWearableBindingStore
import com.rehealth.genie.ring.provider.DEBUG_MOCK_PRODUCT_CODE
import com.rehealth.genie.ring.provider.DEFAULT_MRD_PRODUCT_CODE
import com.rehealth.genie.ring.provider.HBAND_PRODUCT_CODE
import com.rehealth.genie.ring.provider.RWFIT_PRODUCT_CODE
import com.rehealth.genie.ring.provider.WearableVendor
import com.rehealth.genie.ring.provider.WearableProductCatalog
import com.rehealth.genie.ring.rwfit.RealRwFitSdkGateway
import com.rehealth.genie.ring.rwfit.RwFitRingRepository

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
    WearableVendor.RWFIT to {
        RwFitRingRepository(
            dao = dao,
            activeWearableStore = activeWearableStore,
            gateway = RealRwFitSdkGateway(context),
            modelNameHints = WearableProductCatalog(context).find(RWFIT_PRODUCT_CODE)?.modelNameHints.orEmpty(),
        )
    },
    WearableVendor.HBAND to {
        val product = WearableProductCatalog(context).find(HBAND_PRODUCT_CODE)
            ?: error("Missing HBand product profile")
        HBandRingRepository(
            dao = dao,
            activeWearableStore = activeWearableStore,
            gateway = RealHBandSdkGateway(context),
            modelNameHints = product.modelNameHints,
            expectedMetrics = product.expectedMetrics,
        )
    },
    WearableVendor.MIWI4G to {
        Miwi4gCloudRingRepository(activeWearableStore = activeWearableStore)
    },
)

internal fun runtimeDefaultWearableSelection(): Pair<String, WearableVendor> =
    if (shouldUseDebugMock()) {
        DEBUG_MOCK_PRODUCT_CODE to WearableVendor.MOCK
    } else if (BuildConfig.DEBUG_WEARABLE_PRODUCT_CODE == RWFIT_PRODUCT_CODE) {
        RWFIT_PRODUCT_CODE to WearableVendor.RWFIT
    } else if (BuildConfig.DEBUG_WEARABLE_PRODUCT_CODE == HBAND_PRODUCT_CODE) {
        HBAND_PRODUCT_CODE to WearableVendor.HBAND
    } else {
        DEFAULT_MRD_PRODUCT_CODE to WearableVendor.MRD
    }

internal fun shouldForceRuntimeWearableSelection(): Boolean =
    shouldUseDebugMock() || BuildConfig.DEBUG_WEARABLE_PRODUCT_CODE in setOf(RWFIT_PRODUCT_CODE, HBAND_PRODUCT_CODE)

private fun shouldUseDebugMock(): Boolean =
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
