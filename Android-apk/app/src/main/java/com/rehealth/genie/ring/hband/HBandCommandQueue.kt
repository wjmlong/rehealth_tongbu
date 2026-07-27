package com.rehealth.genie.ring.hband

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

internal class HBandCommandQueue {
    private val mutex = Mutex()

    suspend fun <T> execute(timeoutMillis: Long, operation: suspend () -> T): T =
        mutex.withLock { withTimeout(timeoutMillis) { operation() } }
}
