package com.rehealth.genie.ring.provider

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

internal object WearableCloudIdentity {
    fun addressHash(address: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(address.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    fun deviceId(address: String, vendor: WearableVendor): String =
        "${vendor.name.lowercase()}-${addressHash(address).take(24)}"

    fun deviceId(binding: ActiveWearableBinding?): String? = binding
        ?.address
        ?.takeIf(String::isNotBlank)
        ?.let { address -> deviceId(address, binding.vendor) }
}
