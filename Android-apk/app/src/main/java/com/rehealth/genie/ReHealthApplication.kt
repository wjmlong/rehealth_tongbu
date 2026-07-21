package com.rehealth.genie

import android.app.Application
import android.os.Build
import com.rehealth.genie.data.AppDatabase
import com.rehealth.genie.data.sync.InterventionFeedbackRepository
import com.rehealth.genie.data.sync.SyncRepository
import com.rehealth.genie.network.AuthenticatedApiClient
import com.rehealth.genie.network.BackendConfig
import com.rehealth.genie.network.PiasApiClient
import com.rehealth.genie.network.ReHealthBackendClient
import com.rehealth.genie.network.ReHealthMobileApi
import com.rehealth.genie.network.SessionStore
import com.rehealth.genie.notification.RingNotificationChannels
import com.rehealth.genie.phm.MockPhmService
import com.rehealth.genie.phm.PhmService
import com.rehealth.genie.phm.RemotePhmService
import com.rehealth.genie.ring.RingBackgroundCollectionSettings
import com.rehealth.genie.ring.MockRingRepository
import com.rehealth.genie.ring.RingRepository
import com.rehealth.genie.ring.mrd.MrdBleRingRepository
import com.rehealth.genie.ring.mrd.MrdProtocolAdapter
import com.rehealth.genie.work.MeasurementSyncWorker
import com.rehealth.genie.work.RingBackgroundRecoveryWorker

class ReHealthApplication : Application() {
    val database by lazy { AppDatabase.create(this) }

    // D3: Auth and session management
    val sessionStore by lazy { SessionStore(this) }

    val backendClient by lazy {
        ReHealthBackendClient(
            baseUrl = BuildConfig.REHEALTH_API_BASE_URL,
            apiToken = BuildConfig.REHEALTH_API_TOKEN.takeIf { it.isNotBlank() },
        )
    }
    /**
     * Typed E1 mobile API client for the D1-safe endpoints (feature evaluate, risk/intervention
     * retrieval, intervention feedback, health, config). Built on Retrofit/Moshi with the
     * shared OkHttp configuration. Does NOT implement the durable `/measurements/batch`
     * telemetry upload path; that is E2-pending.
     */
    val reHealthMobileApi: ReHealthMobileApi by lazy {
        ReHealthMobileApi(
            baseUrl = BuildConfig.REHEALTH_API_BASE_URL,
            apiToken = BuildConfig.REHEALTH_API_TOKEN.takeIf { it.isNotBlank() },
            httpClient = BackendConfig.buildHttpClient(signSecret = BuildConfig.JEECG_SIGN_SECRET),
        )
    }
    val piasApiClient: PiasApiClient by lazy {
        PiasApiClient(
            baseUrl = BuildConfig.MODEL_SERVICE_BASE_URL,
            httpClient = BackendConfig.buildHttpClient(),
        )
    }
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

    /**
     * Remote-capable PHM service. Falls back to [MockPhmService] when the backend is
     * unavailable, misconfigured, or not yet wired. The local mock remains the snapshot
     * the legacy UI already consumes via [MockPhmService].
     */
    val remotePhmService: RemotePhmService by lazy {
        RemotePhmService(
            api = reHealthMobileApi,
            piasApi = piasApiClient,
            mockFallback = MockPhmService(),
        )
    }

    /** Exposes the offline mock PHM service for UI screens that need local demo data. */
    val phmService: PhmService by lazy { remotePhmService.mock() }
    val mrdProtocolAdapter by lazy { MrdProtocolAdapter(this) }
    val ringRepository: RingRepository by lazy {
        if (BuildConfig.USE_FAKE_RING || (BuildConfig.SEED_FAKE_HEALTH_DATA && isProbablyEmulator())) {
            MockRingRepository(database.ringDataDao())
        } else {
            MrdBleRingRepository(this, database.ringDataDao(), mrdProtocolAdapter)
        }
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

    private fun isProbablyEmulator(): Boolean {
        val fingerprint = Build.FINGERPRINT.lowercase()
        val model = Build.MODEL.lowercase()
        val product = Build.PRODUCT.lowercase()
        val brand = Build.BRAND.lowercase()
        return fingerprint.contains("generic") ||
            fingerprint.contains("emulator") ||
            model.contains("emulator") ||
            model.contains("android sdk built for") ||
            product.contains("sdk") ||
            product.contains("emulator") ||
            brand.contains("generic")
    }
}
