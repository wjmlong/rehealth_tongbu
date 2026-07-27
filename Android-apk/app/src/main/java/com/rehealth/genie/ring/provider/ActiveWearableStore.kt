package com.rehealth.genie.ring.provider

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.rehealth.genie.features.BaselineHealthProfile
import com.rehealth.genie.ring.RingDevice
import java.security.MessageDigest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ActiveWearableBinding(
    val productCode: String,
    val vendor: WearableVendor,
    val address: String?,
    val deviceName: String?,
    val modelCode: String?,
    val firmwareVersion: String?,
    val capabilityJson: String?,
    val boundAt: Long,
    val lastDeviceChangedAt: Long,
)

interface ActiveWearableBindingStore {
    val activeBinding: StateFlow<ActiveWearableBinding>

    fun activateProduct(profile: WearableProductProfile, changedAt: Long = System.currentTimeMillis())

    fun recordConnectedDevice(
        vendor: WearableVendor,
        device: RingDevice,
        modelCode: String? = null,
        firmwareVersion: String? = null,
        capabilityJson: String? = null,
        changedAt: Long = System.currentTimeMillis(),
    )
}

class ActiveWearableStore(
    context: Context,
    defaultProductCode: String = DEFAULT_MRD_PRODUCT_CODE,
    defaultVendor: WearableVendor = WearableVendor.MRD,
    forceDefaultSelection: Boolean = false,
) : ActiveWearableBindingStore {
    private val defaultBinding = ActiveWearableBinding(
        productCode = defaultProductCode,
        vendor = defaultVendor,
        address = null,
        deviceName = null,
        modelCode = null,
        firmwareVersion = null,
        capabilityJson = null,
        boundAt = 0L,
        lastDeviceChangedAt = 0L,
    )
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    private val preferences = EncryptedSharedPreferences.create(
        context,
        PREFERENCES_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )
    private val mutableActiveBinding = MutableStateFlow(
        resolveInitialWearableBinding(defaultBinding, readBinding(), forceDefaultSelection),
    )

    override val activeBinding: StateFlow<ActiveWearableBinding> = mutableActiveBinding.asStateFlow()

    override fun activateProduct(profile: WearableProductProfile, changedAt: Long) {
        val current = mutableActiveBinding.value
        if (current.productCode == profile.productCode && current.vendor == profile.vendor) return
        update(
            ActiveWearableBinding(
                productCode = profile.productCode,
                vendor = profile.vendor,
                address = null,
                deviceName = null,
                modelCode = null,
                firmwareVersion = null,
                capabilityJson = null,
                boundAt = 0L,
                lastDeviceChangedAt = changedAt,
            ),
        )
    }

    override fun recordConnectedDevice(
        vendor: WearableVendor,
        device: RingDevice,
        modelCode: String?,
        firmwareVersion: String?,
        capabilityJson: String?,
        changedAt: Long,
    ) {
        val current = mutableActiveBinding.value
        if (current.vendor != vendor) return
        val deviceChanged = !current.address.equals(device.address, ignoreCase = true)
        update(
            current.copy(
                address = device.address,
                deviceName = device.name,
                modelCode = modelCode ?: current.modelCode,
                firmwareVersion = firmwareVersion ?: current.firmwareVersion,
                capabilityJson = capabilityJson ?: current.capabilityJson,
                boundAt = if (deviceChanged || current.boundAt <= 0L) changedAt else current.boundAt,
                lastDeviceChangedAt = if (deviceChanged) changedAt else current.lastDeviceChangedAt,
            ),
        )
    }

    fun readUserProfile(userId: String?): BaselineHealthProfile? {
        val prefix = profilePrefix(userId) ?: return null
        val age = preferences.getInt("${prefix}_age", -1).takeIf { it > 0 } ?: return null
        val gender = preferences.getString("${prefix}_gender", null)?.takeIf { it.isNotBlank() } ?: return null
        val height = preferences.getString("${prefix}_height_cm", null)?.toDoubleOrNull() ?: return null
        val weight = preferences.getString("${prefix}_weight_kg", null)?.toDoubleOrNull() ?: return null
        return BaselineHealthProfile(age = age, gender = gender, heightCm = height, weightKg = weight)
    }

    fun saveUserProfile(userId: String?, profile: BaselineHealthProfile?) {
        val prefix = profilePrefix(userId) ?: return
        val editor = preferences.edit()
        if (profile?.age == null || profile.gender.isNullOrBlank() ||
            profile.heightCm == null || profile.weightKg == null
        ) {
            editor
                .remove("${prefix}_age")
                .remove("${prefix}_gender")
                .remove("${prefix}_height_cm")
                .remove("${prefix}_weight_kg")
                .apply()
            return
        }
        editor
            .putInt("${prefix}_age", profile.age)
            .putString("${prefix}_gender", profile.gender)
            .putString("${prefix}_height_cm", profile.heightCm.toString())
            .putString("${prefix}_weight_kg", profile.weightKg.toString())
            .apply()
    }

    private fun profilePrefix(userId: String?): String? {
        val stableUserId = userId?.takeIf { it.isNotBlank() } ?: return null
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(stableUserId.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return "profile_${digest.take(24)}"
    }

    private fun update(binding: ActiveWearableBinding) {
        preferences.edit()
            .putString(KEY_PRODUCT_CODE, binding.productCode)
            .putString(KEY_VENDOR, binding.vendor.name)
            .putString(KEY_ADDRESS, binding.address)
            .putString(KEY_DEVICE_NAME, binding.deviceName)
            .putString(KEY_MODEL_CODE, binding.modelCode)
            .putString(KEY_FIRMWARE_VERSION, binding.firmwareVersion)
            .putString(KEY_CAPABILITY_JSON, binding.capabilityJson)
            .putLong(KEY_BOUND_AT, binding.boundAt)
            .putLong(KEY_LAST_DEVICE_CHANGED_AT, binding.lastDeviceChangedAt)
            .apply()
        mutableActiveBinding.value = binding
    }

    private fun readBinding(): ActiveWearableBinding {
        val vendor = preferences.getString(KEY_VENDOR, null)
            ?.let { stored -> runCatching { WearableVendor.valueOf(stored) }.getOrNull() }
            ?: defaultBinding.vendor
        return ActiveWearableBinding(
            productCode = preferences.getString(KEY_PRODUCT_CODE, null) ?: defaultBinding.productCode,
            vendor = vendor,
            address = preferences.getString(KEY_ADDRESS, null),
            deviceName = preferences.getString(KEY_DEVICE_NAME, null),
            modelCode = preferences.getString(KEY_MODEL_CODE, null),
            firmwareVersion = preferences.getString(KEY_FIRMWARE_VERSION, null),
            capabilityJson = preferences.getString(KEY_CAPABILITY_JSON, null),
            boundAt = preferences.getLong(KEY_BOUND_AT, 0L),
            lastDeviceChangedAt = preferences.getLong(KEY_LAST_DEVICE_CHANGED_AT, 0L),
        )
    }

    private companion object {
        const val PREFERENCES_NAME = "rehealth_active_wearable"
        const val KEY_PRODUCT_CODE = "product_code"
        const val KEY_VENDOR = "vendor"
        const val KEY_ADDRESS = "address"
        const val KEY_DEVICE_NAME = "device_name"
        const val KEY_MODEL_CODE = "model_code"
        const val KEY_FIRMWARE_VERSION = "firmware_version"
        const val KEY_CAPABILITY_JSON = "capability_json"
        const val KEY_BOUND_AT = "bound_at"
        const val KEY_LAST_DEVICE_CHANGED_AT = "last_device_changed_at"
    }
}

internal fun resolveInitialWearableBinding(
    defaultBinding: ActiveWearableBinding,
    storedBinding: ActiveWearableBinding,
    forceDefaultSelection: Boolean,
): ActiveWearableBinding {
    if (!forceDefaultSelection) return storedBinding
    return storedBinding.takeIf { stored ->
        stored.productCode == defaultBinding.productCode && stored.vendor == defaultBinding.vendor
    } ?: defaultBinding
}
