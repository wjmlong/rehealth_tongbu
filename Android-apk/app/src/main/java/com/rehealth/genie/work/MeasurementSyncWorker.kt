package com.rehealth.genie.work

import android.content.Context
import android.util.Log
import androidx.work.*
import com.rehealth.genie.ReHealthApplication
import com.rehealth.genie.data.sync.MeasurementUploadOutcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * D3 periodic worker that drains durable measurement and intervention feedback queues.
 *
 * Runs every 30 minutes when:
 * - Network is available
 * - Battery is not low
 *
 * Flow:
 * 1. Check if queue can upload (not paused, authorized)
 * 2. Fetch pending feedback items
 * 3. Attempt upload for each item
 * 4. Update item status (done/failed/retry)
 * 5. Prune old completed items
 *
 * On 401 detection:
 * - Queue is paused by InterventionFeedbackRepository
 * - Worker exits early
 * - Resumes after user re-login
 */
class MeasurementSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val app = applicationContext as ReHealthApplication
        val feedbackRepo = app.interventionFeedbackRepository
        val syncRepo = app.syncRepository

        Log.i(TAG, "MeasurementSyncWorker started")

        // Check if we can upload
        if (!syncRepo.canUpload()) {
            Log.w(TAG, "Queue paused or unauthorized, skipping sync")
            return@withContext Result.success()
        }

        try {
            for (item in syncRepo.pending()) {
                when (measurementWorkerAction(syncRepo.uploadMeasurement(item))) {
                    MeasurementWorkerAction.RETRY -> return@withContext Result.retry()
                    MeasurementWorkerAction.STOP_SUCCESS -> return@withContext Result.success()
                    MeasurementWorkerAction.CONTINUE -> Unit
                }
            }
            syncRepo.pruneDone()

            // Fetch pending feedback
            val pendingItems = feedbackRepo.getPendingUploads()
            Log.i(TAG, "Found ${pendingItems.size} pending feedback items")

            if (pendingItems.isEmpty()) {
                // Prune old completed items
                feedbackRepo.pruneDone()
                return@withContext Result.success()
            }

            var uploadedCount = 0
            var failedCount = 0
            var pausedDueToAuth = false

            for (item in pendingItems) {
                Log.d(TAG, "Uploading feedback ${item.id} for intervention ${item.interventionId}")

                val updatedItem = feedbackRepo.uploadFeedback(item)

                if (updatedItem == null) {
                    // 401 detected, queue paused
                    Log.w(TAG, "401 detected, queue paused, stopping worker")
                    pausedDueToAuth = true
                    break
                }

                feedbackRepo.saveFeedback(updatedItem)

                when (updatedItem.uploadStatus) {
                    "done" -> {
                        uploadedCount++
                        Log.i(TAG, "Feedback ${item.id} uploaded successfully")
                    }
                    "failed" -> {
                        if (updatedItem.uploadAttempts >= MAX_ATTEMPTS) {
                            failedCount++
                            Log.e(TAG, "Feedback ${item.id} failed permanently after ${updatedItem.uploadAttempts} attempts")
                        } else {
                            Log.w(TAG, "Feedback ${item.id} failed, will retry (attempt ${updatedItem.uploadAttempts})")
                        }
                    }
                }
            }

            // Prune old completed items
            feedbackRepo.pruneDone()

            Log.i(TAG, "Sync complete: uploaded=$uploadedCount, failed=$failedCount, paused=$pausedDueToAuth")

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Sync worker failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "MeasurementSyncWorker"
        private const val WORK_NAME = "measurement_sync"
        private const val MAX_ATTEMPTS = 10

        /**
         * Schedule periodic sync worker. Called after login or app startup.
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<MeasurementSyncWorker>(
                repeatInterval = 30,
                repeatIntervalTimeUnit = TimeUnit.MINUTES,
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS,
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request,
                )

            Log.i(TAG, "MeasurementSyncWorker scheduled")
        }

        /**
         * Cancel sync worker. Called after logout.
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.i(TAG, "MeasurementSyncWorker cancelled")
        }

        /**
         * Trigger immediate sync. Called after user submits feedback or resumes queue.
         */
        fun triggerImmediate(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<MeasurementSyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(request)
            Log.i(TAG, "Immediate sync triggered")
        }
    }
}

internal enum class MeasurementWorkerAction {
    CONTINUE,
    RETRY,
    STOP_SUCCESS,
}

internal fun measurementWorkerAction(outcome: MeasurementUploadOutcome): MeasurementWorkerAction = when (outcome) {
    MeasurementUploadOutcome.RetryScheduled -> MeasurementWorkerAction.RETRY
    MeasurementUploadOutcome.Paused -> MeasurementWorkerAction.STOP_SUCCESS
    else -> MeasurementWorkerAction.CONTINUE
}
