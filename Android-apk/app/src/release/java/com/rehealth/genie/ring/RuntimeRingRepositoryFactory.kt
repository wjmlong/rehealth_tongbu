package com.rehealth.genie.ring

import android.content.Context
import com.rehealth.genie.ring.data.RingDataDao
import com.rehealth.genie.network.AuthenticatedApiClient
import com.rehealth.genie.ring.hband.HBandRingRepository
import com.rehealth.genie.ring.hband.RealHBandSdkGateway
import com.rehealth.genie.ring.mrd.MrdProtocolAdapter
import com.rehealth.genie.ring.provider.ActiveWearableBindingStore
import com.rehealth.genie.ring.provider.HBAND_PRODUCT_CODE
import com.rehealth.genie.ring.provider.WearableVendor
import com.rehealth.genie.ring.provider.WearableProductCatalog
import com.rehealth.genie.ring.viomi.ViomiCloudRingRepository

/** Release builds expose only the current pilot providers and contain no mock repository. */
@Suppress("UNUSED_PARAMETER")
internal fun createRuntimeRingProviderFactories(
    context: Context,
    dao: RingDataDao,
    protocolAdapter: MrdProtocolAdapter,
    activeWearableStore: ActiveWearableBindingStore,
    apiClient: AuthenticatedApiClient,
    userIdProvider: () -> String?,
): Map<WearableVendor, () -> RingRepository> = mapOf(
    WearableVendor.HBAND to {
        val product = WearableProductCatalog(context).find(HBAND_PRODUCT_CODE)
            ?: error("Missing HBand product profile")
        HBandRingRepository(
            dao = dao,
            activeWearableStore = activeWearableStore,
            gateway = RealHBandSdkGateway(context),
            modelNameHints = product.modelNameHints,
            expectedMetrics = product.expectedMetrics,
            userIdProvider = userIdProvider,
        )
    },
    WearableVendor.VIOMI_CLOUD to {
        ViomiCloudRingRepository(dao, apiClient, activeWearableStore, userIdProvider)
    },
)

internal fun runtimeDefaultWearableSelection(): Pair<String, WearableVendor> =
    HBAND_PRODUCT_CODE to WearableVendor.HBAND

internal fun runtimeAllowedWearableVendors(): Set<WearableVendor> =
    setOf(WearableVendor.HBAND, WearableVendor.VIOMI_CLOUD)

internal fun shouldForceRuntimeWearableSelection(): Boolean = false
