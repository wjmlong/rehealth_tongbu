package com.rehealth.genie.work

import com.rehealth.genie.data.sync.MeasurementUploadOutcome
import kotlin.test.Test
import kotlin.test.assertEquals

class MeasurementSyncWorkerPolicyTest {
    @Test
    fun `requests WorkManager retry after transient measurement failure`() {
        assertEquals(
            MeasurementWorkerAction.RETRY,
            measurementWorkerAction(MeasurementUploadOutcome.RetryScheduled),
        )
    }

    @Test
    fun `stops successfully while measurement queue waits for login`() {
        assertEquals(
            MeasurementWorkerAction.STOP_SUCCESS,
            measurementWorkerAction(MeasurementUploadOutcome.Paused),
        )
    }
}
