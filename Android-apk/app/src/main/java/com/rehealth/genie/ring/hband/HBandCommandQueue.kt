package com.rehealth.genie.ring.hband

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

/**
 * Serializes all HBand SDK commands on one mutex.
 *
 * The wait for the lock is itself bounded: a caller whose operation cannot
 * start within [LOCK_WAIT_TIMEOUT_MILLIS] fails with a timeout instead of
 * hanging behind a 180s history sync. Callers already treat timeouts as a
 * skipped round, so the foreground service no longer stacks unbounded waiters.
 */
internal class HBandCommandQueue {
    private val mutex = Mutex()

    suspend fun <T> execute(timeoutMillis: Long, operation: suspend () -> T): T =
        withTimeout(timeoutMillis + LOCK_WAIT_TIMEOUT_MILLIS) {
            mutex.withLock { withTimeout(timeoutMillis) { operation() } }
        }

    companion object {
        const val LOCK_WAIT_TIMEOUT_MILLIS = 30_000L
    }
}
