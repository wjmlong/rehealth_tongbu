package com.rehealth.genie.ring

import android.content.Context
import com.rehealth.genie.ring.data.RingDataDao
import com.rehealth.genie.ring.mrd.MrdBleRingRepository
import com.rehealth.genie.ring.mrd.MrdProtocolAdapter
import com.rehealth.genie.ring.provider.ActiveWearableBindingStore
import com.rehealth.genie.ring.provider.DEFAULT_MRD_PRODUCT_CODE
import com.rehealth.genie.ring.provider.RWFIT_PRODUCT_CODE
import com.rehealth.genie.ring.provider.WearableVendor
import com.rehealth.genie.ring.provider.WearableProductCatalog
import com.rehealth.genie.ring.rwfit.RealRwFitSdkGateway
import com.rehealth.genie.ring.rwfit.RwFitRingRepository

/** Release builds register real providers only and contain no mock repository. */
internal fun createRuntimeRingProviderFactories(
    context: Context,
    dao: RingDataDao,
    protocolAdapter: MrdProtocolAdapter,
    activeWearableStore: ActiveWearableBindingStore,
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
)

internal fun runtimeDefaultWearableSelection(): Pair<String, WearableVendor> =
    DEFAULT_MRD_PRODUCT_CODE to WearableVendor.MRD

internal fun shouldForceRuntimeWearableSelection(): Boolean = false
