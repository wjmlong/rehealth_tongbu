package com.rehealth.genie.work

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import com.rehealth.genie.ReHealthApplication
import com.rehealth.genie.ring.RingBackgroundCollectionSettings
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Uploads only durable wearable telemetry; business queues retain their own cadence. */
class TelemetryUploadWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val repository = (applicationContext as ReHealthApplication).syncRepository
        if (!repository.canUpload()) return@withContext Result.success()
        runCatching {
            while (true) {
                val item = repository.claimNextByKind(TELEMETRY_BATCH_KIND) ?: break
                when (measurementWorkerAction(repository.uploadQueuedItem(item))) {
                    MeasurementWorkerAction.RETRY -> {
                        repository.releaseClaim(item.id)
                        return@withContext Result.retry()
                    }
                    MeasurementWorkerAction.STOP_SUCCESS -> {
                        repository.releaseClaim(item.id)
                        return@withContext Result.success()
                    }
                    MeasurementWorkerAction.CONTINUE -> Unit
                }
            }
            repository.pruneDone()
            Result.success()
        }.getOrElse { error ->
            Log.w(TAG, "telemetry upload failed", error)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "TelemetryUploadWorker"
        private const val WORK_NAME = "telemetry_upload"
        private const val TELEMETRY_BATCH_KIND = "telemetry_batch"

        fun schedule(context: Context, ownerUserId: String? = null) {
            val minutes = RingBackgroundCollectionSettings.uploadIntervalMinutes(
                context,
                ownerUserId,
            ).toLong()
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()
            val request = PeriodicWorkRequestBuilder<TelemetryUploadWorker>(minutes, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS,
                )
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
            Log.i(TAG, "telemetry upload scheduled every $minutes minutes")
        }

        fun triggerImmediate(context: Context) {
            val request = OneTimeWorkRequestBuilder<TelemetryUploadWorker>()
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
                )
                .build()
            WorkManager.getInstance(context.applicationContext).enqueue(request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context.applicationContext).cancelUniqueWork(WORK_NAME)
        }
    }
}
