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
import com.rehealth.genie.ring.RingRepository
import com.rehealth.genie.work.RingBackgroundRecoveryWorker
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
            val delayMs = RingBackgroundCollectionPolicy.nextDelayMillis(now, lastAttempt)
            if (delayMs > 0L) {
                updateNotification("Next local ring collection is scheduled")
                delay(delayMs)
                continue
            }
            updateNotification("Collecting ring data locally")
            RingBackgroundCollectionSettings.markAttempt(this, now)
            val message = runLocalCollectionCycle()
            updateNotification(message)
            delay(RingBackgroundCollectionPolicy.COLLECTION_INTERVAL_MS)
        }
    }

    private suspend fun runLocalCollectionCycle(): String {
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
        return runCatching { repository.syncAll() }
            .fold(
                onSuccess = { result ->
                    if (result.recordsWritten > 0) {
                        RingBackgroundCollectionSettings.markSuccess(this, result.completedAt)
                        "Saved ${result.recordsWritten} local ring records"
                    } else {
                        "Ring collection finished: no new local records"
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
            context.startService(intent(context, ACTION_STOP, runImmediately = false))
        }
    }
}
