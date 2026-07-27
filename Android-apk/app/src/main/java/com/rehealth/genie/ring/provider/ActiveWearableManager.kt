package com.rehealth.genie.ring.provider

import kotlinx.coroutines.flow.StateFlow

class ActiveWearableManager(
    private val store: ActiveWearableBindingStore,
    products: List<WearableProductProfile>,
    private val registry: RingProviderRegistry,
    private val repository: ActiveRingRepository,
) {
    private val availableProducts = products.toList()

    val activeBinding: StateFlow<ActiveWearableBinding> = store.activeBinding
    val products: List<WearableProductProfile>
        get() = availableProducts

    suspend fun switchProduct(productCode: String) {
        val profile = availableProducts.firstOrNull { it.productCode == productCode }
            ?: throw IllegalArgumentException("Unknown wearable productCode")
        require(registry.supports(profile.vendor)) {
            "Wearable provider is not available for ${profile.vendor}"
        }
        repository.switchProduct(profile)
    }
}
