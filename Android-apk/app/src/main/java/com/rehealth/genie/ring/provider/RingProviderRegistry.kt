package com.rehealth.genie.ring.provider

import com.rehealth.genie.ring.RingRepository

class RingProviderRegistry(
    factories: Map<WearableVendor, () -> RingRepository>,
) {
    private val factories = factories.toMap()
    private val repositories = mutableMapOf<WearableVendor, RingRepository>()
    private val lock = Any()

    fun supports(vendor: WearableVendor): Boolean = factories.containsKey(vendor)

    fun repositoryOrNull(vendor: WearableVendor): RingRepository? = synchronized(lock) {
        repositories[vendor] ?: factories[vendor]?.invoke()?.also { repositories[vendor] = it }
    }

    fun requireRepository(vendor: WearableVendor): RingRepository =
        repositoryOrNull(vendor) ?: error("No RingRepository provider is registered for $vendor")

    internal fun initializedVendors(): Set<WearableVendor> = synchronized(lock) {
        repositories.keys.toSet()
    }
}
