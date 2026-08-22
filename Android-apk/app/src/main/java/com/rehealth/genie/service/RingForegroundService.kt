package com.rehealth.genie.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import com.rehealth.genie.ReHealthApplication
import com.rehealth.genie.notification.RingNotificationChannels
import com.rehealth.genie.ring.RingBackgroundCollectionPolicy
import com.rehealth.genie.ring.RingBackgroundCollectionSettings
import com.rehealth.genie.ring.RingBleGuards
import com.rehealth.genie.ring.RingConnectionState
import com.rehealth.genie.ring.RingMetricType
import com.rehealth.genie.ring.RingRepository
import com.rehealth.genie.ring.provider.WearableVendor
import com.rehealth.genie.work.RingBackgroundRecoveryWorker
import com.rehealth.genie.work.TelemetryUploadWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class RingForegroundService : Service() {
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.Main.immediate)
    private var collectionJob: Job? = null
    private lateinit var repository: RingRepository

    override fun onCreate() {
        super.onCreate()
        repository = (application as ReHealthApplication).ringRepository
        RingNotificationChannels.ensure(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action ?: ACTION_START) {
            ACTION_STOP -> {
                stopCollection()
                return START_NOT_STICKY
            }
            ACTION_START -> startCollection(
                runImmediately = intent?.getBooleanExtra(EXTRA_RUN_IMMEDIATELY, true) ?: true,
            )
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        collectionJob?.cancel()
        serviceJob.cancel()
        super.onDestroy()
    }

    private fun startCollection(runImmediately: Boolean) {
        RingBackgroundCollectionSettings.setActive(this, true)
        RingBackgroundRecoveryWorker.schedule(this)
        TelemetryUploadWorker.schedule(this)
        if (!moveToForeground("Preparing local ring collection")) {
            return
        }
        if (collectionJob?.isActive == true) {
            return
        }
        collectionJob = serviceScope.launch {
            runCollectionLoop(runImmediately)
        }
    }

    private suspend fun runCollectionLoop(runImmediately: Boolean) {
        if (runImmediately) {
            RingBackgroundCollectionSettings.markAttempt(this, 0L)
        }
        while (currentCoroutineContext().isActive && RingBackgroundCollectionSettings.isActive(this)) {
            val now = System.currentTimeMillis()
            val lastAttempt = RingBackgroundCollectionSettings.lastAttemptAt(this)
            val intervalMs = RingBackgroundCollectionSettings.measurementIntervalMinutes(this) * 60_000L
            val delayMs = RingBackgroundCollectionPolicy.nextDelayMillis(now, lastAttempt, intervalMs)
            if (delayMs > 0L) {
                updateNotification("Next local ring collection is scheduled")
                delay(delayMs)
                continue
            }
            updateNotification("Collecting ring data locally")
            val message = runLocalCollectionCycle()
            RingBackgroundCollectionSettings.markAttempt(this, System.currentTimeMillis())
            updateNotification(message)
        }
    }

    private suspend fun runLocalCollectionCycle(): String {
        val app = application as ReHealthApplication
        val binding = app.activeWearableStore.activeBinding.value
        if (binding.vendor != WearableVendor.HBAND) {
            return "Active measurement paused: HBand binding required"
        }
        if (!RingBleGuards.hasCollectionPermission(this)) {
            return "Ring collection paused: Bluetooth permission required"
        }
        if (!RingBleGuards.isBluetoothAvailable(this)) {
            return "Ring collection paused: Bluetooth is unsupported"
        }
        if (!RingBleGuards.isBluetoothEnabled(this)) {
            return "Ring collection paused: Bluetooth is off"
        }
        if (repository.connectionState.value == RingConnectionState.SYNCING) {
            return "Ring collection skipped: foreground collection in progress"
        }
        if (repository.connectionState.value != RingConnectionState.CONNECTED && !repository.autoConnect()) {
            return "Ring collection paused: bound device is unavailable"
        }
        val now = System.currentTimeMillis()
        val scheduledMetrics = buildList {
            add(RingMetricType.HEART_RATE)
            if (RingBackgroundCollectionPolicy.shouldMeasureBloodOxygen(
                    now,
                    RingBackgroundCollectionSettings.lastBloodOxygenAt(this@RingForegroundService),
                )
            ) {
                add(RingMetricType.BLOOD_OXYGEN)
            }
            if (RingBackgroundCollectionPolicy.shouldMeasureBloodPressure(
                    now,
                    RingBackgroundCollectionSettings.lastBloodPressureAt(this@RingForegroundService),
                )
            ) {
                add(RingMetricType.BLOOD_PRESSURE)
            }
        }.filter { it in repository.manuallyMeasurableMetrics }
        return runCatching {
            val failures = mutableListOf<Throwable>()
            val results = scheduledMetrics.mapNotNull { metric ->
                runCatching { repository.measure(metric) }
                    .fold(
                        onSuccess = { metric to it },
                        onFailure = { error ->
                            failures += error
                            Log.w(TAG, "scheduled metric failed: ${metric.name}", error)
                            null
                        },
                    )
            }
            if (results.isEmpty() && failures.isNotEmpty()) throw failures.first()
            val recordsWritten = results.sumOf { it.second.recordsWritten }
            val completedAt = results.maxOfOrNull { it.second.completedAt } ?: System.currentTimeMillis()
            if (results.any { (metric, result) ->
                    metric == RingMetricType.BLOOD_PRESSURE && result.recordsWritten > 0
                }
            ) {
                RingBackgroundCollectionSettings.markBloodPressureSuccess(this, completedAt)
            }
            if (results.any { (metric, result) ->
                    metric == RingMetricType.BLOOD_OXYGEN && result.recordsWritten > 0
                }
            ) {
                RingBackgroundCollectionSettings.markBloodOxygenSuccess(this, completedAt)
            }
            ScheduledCollectionOutcome(recordsWritten, completedAt, scheduledMetrics.size, failures.size)
        }
            .fold(
                onSuccess = { outcome ->
                    val recordsWritten = outcome.recordsWritten
                    val completedAt = outcome.completedAt
                    if (recordsWritten > 0) {
                        RingBackgroundCollectionSettings.markSuccess(this, completedAt)
                        val device = repository.connectedDevice.value
                            ?: binding.address?.let { address ->
                                com.rehealth.genie.ring.RingDevice(address, binding.deviceName, null)
                            }
                        if (device != null) {
                            app.ringCloudRepository.enqueueLatestTelemetry(
                                device = device,
                                collectedAt = completedAt,
                                trigger = "scheduled_active_measurement",
                                triggerUpload = false,
                            ).onFailure { error ->
                                Log.w(TAG, "unable to enqueue scheduled telemetry", error)
                            }
                        }
                        "Saved $recordsWritten records (${outcome.attemptedMetrics - outcome.failedMetrics}/${outcome.attemptedMetrics} measurements)"
                    } else {
                        "Active measurement finished: no new local records"
                    }
                },
                onFailure = { error ->
                    Log.w(TAG, "background ring collection failed", error)
                    "Ring collection will retry later"
                },
            )
    }

    private fun stopCollection() {
        RingBackgroundCollectionSettings.setActive(this, false)
        RingBackgroundRecoveryWorker.cancel(this)
        collectionJob?.cancel()
        collectionJob = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun moveToForeground(contentText: String): Boolean {
        return runCatching {
            startForeground(
                RingNotificationChannels.COLLECTION_NOTIFICATION_ID,
                RingNotificationChannels.collectionNotification(this, contentText),
            )
        }.fold(
            onSuccess = { true },
            onFailure = { error ->
                Log.w(TAG, "unable to start ring foreground service", error)
                RingBackgroundCollectionSettings.setActive(this, false)
                stopSelf()
                false
            },
        )
    }

    private fun updateNotification(contentText: String) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(
            RingNotificationChannels.COLLECTION_NOTIFICATION_ID,
            RingNotificationChannels.collectionNotification(this, contentText),
        )
    }

    companion object {
        const val ACTION_START = "com.rehealth.genie.ring.action.START_COLLECTION"
        const val ACTION_STOP = "com.rehealth.genie.ring.action.STOP_COLLECTION"
        private const val EXTRA_RUN_IMMEDIATELY = "run_immediately"
        private const val TAG = "RingForegroundService"

        fun intent(context: Context, action: String, runImmediately: Boolean = true): Intent =
            Intent(context, RingForegroundService::class.java)
                .setAction(action)
                .putExtra(EXTRA_RUN_IMMEDIATELY, runImmediately)

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                intent(context, ACTION_START, runImmediately = true),
            )
        }

        fun recover(context: Context) {
            ContextCompat.startForegroundService(
                context,
                intent(context, ACTION_START, runImmediately = false),
            )
        }

        fun stop(context: Context) {
            RingBackgroundCollectionSettings.setActive(context, false)
            RingBackgroundRecoveryWorker.cancel(context)
            context.startService(intent(context, ACTION_STOP, runImmediately = false))
        }
    }
}

private data class ScheduledCollectionOutcome(
    val recordsWritten: Int,
    val completedAt: Long,
    val attemptedMetrics: Int,
    val failedMetrics: Int,
)
