package com.rehealth.genie.ring.miwi

import com.rehealth.genie.ring.RingConnectionState
import com.rehealth.genie.ring.RingDevice
import com.rehealth.genie.ring.RingMetricType
import com.rehealth.genie.ring.provider.ActiveWearableBinding
import com.rehealth.genie.ring.provider.ActiveWearableBindingStore
import com.rehealth.genie.ring.provider.MIWI4G_PRODUCT_CODE
import com.rehealth.genie.ring.provider.WearableProductProfile
import com.rehealth.genie.ring.provider.WearableVendor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest

class Miwi4gCloudRingRepositoryTest {

    private fun miwiProfile() = WearableProductProfile(
        productCode = MIWI4G_PRODUCT_CODE,
        vendor = WearableVendor.MIWI4G,
        displayName = "ReHealth 4G 健康手表 S8",
        modelNameHints = setOf("S8"),
        expectedMetrics = setOf(RingMetricType.HEART_RATE),
    )

    @Test
    fun connectWithImeiRecordsBindingAndBecomesConnected() = runTest {
        val store = FakeStore(miwiProfile())
        val repository = Miwi4gCloudRingRepository(store)

        repository.connect(RingDevice(address = "358273001234567", name = null, rssi = null))

        assertEquals(RingConnectionState.CONNECTED, repository.connectionState.value)
        assertEquals("358273001234567", store.activeBinding.value.address)
        assertEquals("S8", store.activeBinding.value.modelCode)
        assertEquals(
            Miwi4gCloudRingRepository.DEFAULT_DEVICE_NAME,
            repository.connectedDevice.value?.name,
        )
    }

    @Test
    fun connectRejectsInvalidImei() = runTest {
        val repository = Miwi4gCloudRingRepository(FakeStore(miwiProfile()))

        assertFailsWith<IllegalArgumentException> {
            repository.connect(RingDevice(address = "AA:BB:CC", name = null, rssi = null))
        }
    }

    @Test
    fun scanReturnsBoundDeviceAndAutoConnectRestoresSession() = runTest {
        val store = FakeStore(miwiProfile())
        val repository = Miwi4gCloudRingRepository(store)
        repository.connect(RingDevice(address = "3582730012345", name = null, rssi = null))

        val restored = Miwi4gCloudRingRepository(store)
        assertEquals("3582730012345", restored.scan().single().address)
        assertTrue(restored.autoConnect())
        assertEquals(RingConnectionState.CONNECTED, restored.connectionState.value)
    }

    @Test
    fun syncProducesNoLocalRecordsBecauseDataFlowsThroughVendorCloud() = runTest {
        val store = FakeStore(miwiProfile())
        val repository = Miwi4gCloudRingRepository(store)
        repository.connect(RingDevice(address = "3582730012345", name = null, rssi = null))

        val result = repository.syncAll()
        assertEquals(0, result.recordsWritten)
        assertEquals(emptySet(), result.collectedTypes)
    }

    private class FakeStore(profile: WearableProductProfile) : ActiveWearableBindingStore {
        private val mutableBinding = MutableStateFlow(profile.toBinding(0L))
        override val activeBinding: StateFlow<ActiveWearableBinding> = mutableBinding.asStateFlow()

        override fun activateProduct(profile: WearableProductProfile, changedAt: Long) {
            mutableBinding.value = profile.toBinding(changedAt)
        }

        override fun recordConnectedDevice(
            vendor: WearableVendor,
            device: RingDevice,
            modelCode: String?,
            firmwareVersion: String?,
            capabilityJson: String?,
            changedAt: Long,
        ) {
            if (mutableBinding.value.vendor != vendor) return
            mutableBinding.value = mutableBinding.value.copy(
                address = device.address,
                deviceName = device.name,
                modelCode = modelCode,
                boundAt = changedAt,
            )
        }
    }
}

private fun WearableProductProfile.toBinding(changedAt: Long = 0L) = ActiveWearableBinding(
    productCode = productCode,
    vendor = vendor,
    address = null,
    deviceName = null,
    modelCode = null,
    firmwareVersion = null,
    capabilityJson = null,
    boundAt = 0L,
    lastDeviceChangedAt = changedAt,
)
