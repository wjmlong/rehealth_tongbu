package com.rehealth.genie.ring

import android.content.Context
import com.rehealth.genie.ring.data.RingDataDao
import com.rehealth.genie.network.AuthenticatedApiClient
import com.rehealth.genie.ring.hband.HBandRingRepository
import com.rehealth.genie.ring.hband.RealHBandSdkGateway
import com.rehealth.genie.ring.mrd.MrdBleRingRepository
import com.rehealth.genie.ring.mrd.MrdProtocolAdapter
import com.rehealth.genie.ring.provider.ActiveWearableBindingStore
import com.rehealth.genie.ring.provider.DEFAULT_MRD_PRODUCT_CODE
import com.rehealth.genie.ring.provider.HBAND_PRODUCT_CODE
import com.rehealth.genie.ring.provider.RWFIT_PRODUCT_CODE
import com.rehealth.genie.ring.provider.WearableVendor
import com.rehealth.genie.ring.provider.WearableProductCatalog
import com.rehealth.genie.ring.rwfit.RealRwFitSdkGateway
import com.rehealth.genie.ring.rwfit.RwFitRingRepository
import com.rehealth.genie.ring.viomi.ViomiCloudRingRepository

/** Release builds register real providers only and contain no mock repository. */
internal fun createRuntimeRingProviderFactories(
    context: Context,
    dao: RingDataDao,
    protocolAdapter: MrdProtocolAdapter,
    activeWearableStore: ActiveWearableBindingStore,
    apiClient: AuthenticatedApiClient,
): Map<WearableVendor, () -> RingRepository> = mapOf(
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
    WearableVendor.VIOMI_CLOUD to { ViomiCloudRingRepository(dao, apiClient, activeWearableStore) },
)

internal fun runtimeDefaultWearableSelection(): Pair<String, WearableVendor> =
    DEFAULT_MRD_PRODUCT_CODE to WearableVendor.MRD

internal fun shouldForceRuntimeWearableSelection(): Boolean = false
