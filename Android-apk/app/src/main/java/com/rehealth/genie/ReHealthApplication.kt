package com.rehealth.genie

import android.app.Application
import com.rehealth.genie.data.AppDatabase
import com.rehealth.genie.data.BehaviorRecordRepository
import com.rehealth.genie.data.RiskHistoryRepository
import com.rehealth.genie.data.HealthChatRepository
import com.rehealth.genie.data.sync.InterventionFeedbackRepository
import com.rehealth.genie.data.sync.RingCloudRepository
import com.rehealth.genie.data.sync.SyncRepository
import com.rehealth.genie.diet.DietRecordRepository
import com.rehealth.genie.network.AuthenticatedApiClient
import com.rehealth.genie.network.ApiResult
import com.rehealth.genie.network.BackendConfig
import com.rehealth.genie.network.OnboardingStore
import com.rehealth.genie.network.SessionStore
import com.rehealth.genie.notification.RingNotificationChannels
import com.rehealth.genie.phm.RemotePhmService
import com.rehealth.genie.qa.createRuntimeFullChainSimulationRunner
import com.rehealth.genie.rdi.RdiRepository
import com.rehealth.genie.rhi.RhiRepository
import com.rehealth.genie.rhi.RhiManualHealthInputRepository
import com.rehealth.genie.ring.RingBackgroundCollectionSettings
import com.rehealth.genie.ring.RingRepository
import com.rehealth.genie.ring.createRuntimeRingProviderFactories
import com.rehealth.genie.ring.mrd.MrdProtocolAdapter
import com.rehealth.genie.ring.provider.ActiveRingRepository
import com.rehealth.genie.ring.provider.ActiveWearableManager
import com.rehealth.genie.ring.provider.ActiveWearableStore
import com.rehealth.genie.ring.provider.RingProviderRegistry
import com.rehealth.genie.ring.provider.WearableProductCatalog
import com.rehealth.genie.ring.runtimeDefaultWearableSelection
import com.rehealth.genie.ring.shouldForceRuntimeWearableSelection
import com.rehealth.genie.work.MeasurementSyncWorker
import com.rehealth.genie.work.RingBackgroundRecoveryWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ReHealthApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val database by lazy { AppDatabase.create(this) }

    // D3: Auth and session management
    val sessionStore by lazy { SessionStore(this) }
    val onboardingStore by lazy { OnboardingStore(this) }

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
            wearableBindingProvider = { activeWearableStore.activeBinding.value },
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

    val dietRecordRepository by lazy {
        DietRecordRepository(
            dao = database.dietRecordDao(),
            syncRepository = syncRepository,
            userIdProvider = { sessionStore.userId },
            wearableBindingProvider = { activeWearableStore.activeBinding.value },
            triggerSync = { MeasurementSyncWorker.triggerImmediate(this) },
        )
    }

    val rdiRepository by lazy {
        RdiRepository(
            rdiDao = database.rdiDao(),
            rdiBaselineDao = database.rdiBaselineDao(),
            ringDataDao = database.ringDataDao(),
            rhiManualHealthInputDao = database.rhiManualHealthInputDao(),
            rdiLabMealDao = database.rdiLabMealDao(),
            dietRecordDao = database.dietRecordDao(),
            userIdProvider = { sessionStore.userId },
        )
    }

    val rhiRepository by lazy {
        RhiRepository(
            ringDataDao = database.ringDataDao(),
            manualInputDao = database.rhiManualHealthInputDao(),
            interventionFeedbackDao = database.interventionFeedbackDao(),
            snapshotDao = database.rhiSnapshotDao(),
            syncRepository = syncRepository,
            userIdProvider = { sessionStore.userId },
            remoteSeriesEvaluator = { request ->
                when (val result = authenticatedApiClient.evaluateRhiSeries(request)) {
                    is ApiResult.Success -> result.data
                    is ApiResult.Unauthorized -> error(result.message)
                    is ApiResult.Forbidden -> error(result.message)
                    is ApiResult.InvalidRequest -> error(result.message)
                    is ApiResult.InvalidResponse -> error(result.message)
                    is ApiResult.ServiceUnavailable -> error(result.message)
                    is ApiResult.NetworkError -> error(result.message)
                }
            },
        )
    }

    val rhiManualHealthInputRepository by lazy {
        RhiManualHealthInputRepository(
            dao = database.rhiManualHealthInputDao(),
            syncRepository = syncRepository,
            apiClient = authenticatedApiClient,
            triggerSync = { MeasurementSyncWorker.triggerImmediate(this) },
        )
    }

    val fullChainSimulationRunner by lazy {
        createRuntimeFullChainSimulationRunner(this)
    }

    val healthChatRepository by lazy {
        HealthChatRepository(
            dao = database.healthChatDao(),
            apiClient = authenticatedApiClient,
            userIdProvider = { sessionStore.userId },
        )
    }

    val behaviorRecordRepository by lazy {
        BehaviorRecordRepository(authenticatedApiClient)
    }

    val mrdProtocolAdapter by lazy { MrdProtocolAdapter(this) }
    val activeWearableStore by lazy {
        val (productCode, vendor) = runtimeDefaultWearableSelection()
        ActiveWearableStore(
            context = this,
            defaultProductCode = productCode,
            defaultVendor = vendor,
            forceDefaultSelection = shouldForceRuntimeWearableSelection(),
        )
    }
    val ringProviderRegistry by lazy {
        RingProviderRegistry(
            createRuntimeRingProviderFactories(
                context = this,
                dao = database.ringDataDao(),
                protocolAdapter = mrdProtocolAdapter,
                activeWearableStore = activeWearableStore,
                apiClient = authenticatedApiClient,
            ),
        )
    }
    private val activeRingRepository by lazy {
        ActiveRingRepository(
            appScope = applicationScope,
            store = activeWearableStore,
            registry = ringProviderRegistry,
            initialUserProfile = activeWearableStore.readUserProfile(sessionStore.userId),
            persistUserProfile = { profile ->
                activeWearableStore.saveUserProfile(sessionStore.userId, profile)
            },
        )
    }
    val ringRepository: RingRepository
        get() = activeRingRepository
    val activeWearableManager by lazy {
        ActiveWearableManager(
            store = activeWearableStore,
            products = WearableProductCatalog(this).products,
            registry = ringProviderRegistry,
            repository = activeRingRepository,
        )
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
        if (BuildConfig.DEBUG && BuildConfig.SEED_FAKE_HEALTH_DATA) {
            applicationScope.launch {
                runCatching {
                    ringRepository.autoConnect()
                    ringRepository.syncAll()
                }
            }
        }
    }
}
