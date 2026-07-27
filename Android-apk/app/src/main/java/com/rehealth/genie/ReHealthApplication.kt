package com.rehealth.genie

import android.app.Application
import com.rehealth.genie.data.AppDatabase
import com.rehealth.genie.data.RiskHistoryRepository
import com.rehealth.genie.data.sync.InterventionFeedbackRepository
import com.rehealth.genie.data.sync.RingCloudRepository
import com.rehealth.genie.data.sync.SyncRepository
import com.rehealth.genie.network.AuthenticatedApiClient
import com.rehealth.genie.network.BackendConfig
import com.rehealth.genie.network.SessionStore
import com.rehealth.genie.notification.RingNotificationChannels
import com.rehealth.genie.phm.RemotePhmService
import com.rehealth.genie.ring.RingBackgroundCollectionSettings
import com.rehealth.genie.ring.RingRepository
import com.rehealth.genie.ring.createRuntimeRingRepository
import com.rehealth.genie.ring.mrd.MrdProtocolAdapter
import com.rehealth.genie.work.MeasurementSyncWorker
import com.rehealth.genie.work.RingBackgroundRecoveryWorker

class ReHealthApplication : Application() {
    val database by lazy { AppDatabase.create(this) }

    // D3: Auth and session management
    val sessionStore by lazy { SessionStore(this) }

    /**
     * D3: Auth-aware API client with 401 detection and queue pause.
     */
    val authenticatedApiClient by lazy {
        AuthenticatedApiClient(
            baseUrl = BuildConfig.REHEALTH_API_BASE_URL,
            httpClient = BackendConfig.buildHttpClient(signSecret = BuildConfig.JEECG_SIGN_SECRET),
            sessionStore = sessionStore,
        )
    }

    /**
     * D3: Upload queue repository with auth-aware pause/resume.
     */
    val syncRepository by lazy {
        SyncRepository(
            dao = database.uploadQueueDao(),
            apiClient = authenticatedApiClient,
        )
    }

    /**
     * D3: Typed intervention feedback repository.
     */
    val interventionFeedbackRepository by lazy {
        InterventionFeedbackRepository(
            dao = database.interventionFeedbackDao(),
            apiClient = authenticatedApiClient,
        )
    }

    val ringCloudRepository by lazy {
        RingCloudRepository(
            dao = database.ringDataDao(),
            syncRepository = syncRepository,
            apiClient = authenticatedApiClient,
            sessionStore = sessionStore,
            triggerSync = { MeasurementSyncWorker.triggerImmediate(this) },
        )
    }

    val riskHistoryRepository by lazy {
        RiskHistoryRepository(
            riskHistoryDao = database.riskHistoryDao(),
            feedbackDao = database.interventionFeedbackDao(),
            userIdProvider = { sessionStore.userId },
        )
    }

    /** Remote-only PHM service. Network failures are surfaced and never synthesize risk output. */
    val remotePhmService: RemotePhmService by lazy {
        RemotePhmService(
            api = null,
            authenticatedApi = authenticatedApiClient,
        )
    }

    val mrdProtocolAdapter by lazy { MrdProtocolAdapter(this) }
    val ringRepository: RingRepository by lazy {
        createRuntimeRingRepository(this, database.ringDataDao(), mrdProtocolAdapter)
    }

    override fun onCreate() {
        super.onCreate()
        RingNotificationChannels.ensure(this)

        if (RingBackgroundCollectionSettings.isActive(this)) {
            RingBackgroundRecoveryWorker.schedule(this)
        }
        // D3: if a session was restored, schedule the feedback sync worker
        if (sessionStore.isLoggedIn) {
            MeasurementSyncWorker.schedule(this)
        }
    }
}
