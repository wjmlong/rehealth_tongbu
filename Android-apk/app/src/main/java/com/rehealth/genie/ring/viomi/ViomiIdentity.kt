package com.rehealth.genie.ring.viomi

import java.security.MessageDigest

internal fun viomiDeviceId(imei: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
        .digest(imei.trim().toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
    return "viomi-${digest.take(24)}"
}

internal const val VIOMI_SOURCE = "viomi_cloud"
