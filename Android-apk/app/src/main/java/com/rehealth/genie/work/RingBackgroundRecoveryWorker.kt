package com.rehealth.genie.work

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.rehealth.genie.ring.RingBackgroundCollectionPolicy
import com.rehealth.genie.ring.RingBackgroundCollectionSettings
import com.rehealth.genie.service.RingForegroundService
import java.util.concurrent.TimeUnit

class RingBackgroundRecoveryWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        if (!RingBackgroundCollectionSettings.isActive(applicationContext)) {
            Log.i(TAG, "ring background recovery inactive")
            return Result.success()
        }
        return runCatching {
            RingForegroundService.recover(applicationContext)
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { error ->
                Log.w(TAG, "ring background recovery failed", error)
                Result.retry()
            },
        )
    }

    companion object {
        private const val TAG = "RingRecoveryWorker"
        private const val UNIQUE_WORK_NAME = "ring_background_recovery"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<RingBackgroundRecoveryWorker>(
                RingBackgroundCollectionPolicy.COLLECTION_INTERVAL_MS,
                TimeUnit.MILLISECONDS,
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .setRequiresBatteryNotLow(true)
                        .build(),
                )
                .build()
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniquePeriodicWork(
                    UNIQUE_WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request,
                )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context.applicationContext)
                .cancelUniqueWork(UNIQUE_WORK_NAME)
        }
    }
}
