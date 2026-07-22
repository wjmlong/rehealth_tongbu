package com.rehealth.genie.logging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class SafeLogValuesTest {
    @Test
    fun byteCountDoesNotExposePayload() {
        val payload = "heart-rate-json-with-health-data".toByteArray()

        val summary = SafeLogValues.byteCount(payload)

        assertEquals("bytes=${payload.size}", summary)
        assertFalse(summary.contains("heart-rate"))
        assertFalse(summary.contains("health-data"))
    }

    @Test
    fun exceptionTypeDoesNotExposeErrorMessage() {
        val error = IllegalStateException("vendor response contains token=secret")

        val summary = SafeLogValues.exceptionType(error)

        assertEquals("IllegalStateException", summary)
        assertFalse(summary.contains("token"))
        assertFalse(summary.contains("secret"))
    }
}
