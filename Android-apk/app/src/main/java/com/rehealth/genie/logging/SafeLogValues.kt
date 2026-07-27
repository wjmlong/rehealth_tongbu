package com.rehealth.genie.logging

internal object SafeLogValues {
    fun byteCount(value: ByteArray): String = "bytes=${value.size}"

    fun exceptionType(error: Throwable): String =
        error::class.java.simpleName.ifBlank { "Unknown" }
}
