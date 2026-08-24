package com.rehealth.genie.ring

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rehealth.genie.data.sync.RingCloudRepository
import com.rehealth.genie.data.RiskHistoryRepository
import com.rehealth.genie.network.PatientMvpPayload
import com.rehealth.genie.network.PatientProfilePayload
import com.rehealth.genie.features.BaselineHealthProfile
import com.rehealth.genie.features.HealthMemorySnapshot
import com.rehealth.genie.ring.SupportedHardwareHealthMetrics
import com.rehealth.genie.ring.data.RingActivityEntity
import com.rehealth.genie.ring.data.RingDataDao
import com.rehealth.genie.ring.data.RingMeasurementEntity
import com.rehealth.genie.ring.data.RingSignalChunkEntity
import com.rehealth.genie.ring.data.RingSleepSessionEntity
import com.rehealth.genie.ring.provider.ActiveWearableManager
import com.rehealth.genie.ring.provider.HBAND_PRODUCT_CODE
import com.rehealth.genie.ring.provider.WearableVendor
import com.rehealth.genie.service.RingForegroundService
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RingUiState(
    val acquisitionMode: RingAcquisitionMode = RingAcquisitionMode.BLUETOOTH,
    val connectionState: RingConnectionState = RingConnectionState.DISCONNECTED,
    val devices: List<RingDevice> = emptyList(),
    val connectedDevice: RingDevice? = null,
    val isScanning: Boolean = false,
    val isSyncing: Boolean = false,
    val measuringMetric: RingMetricType? = null,
    val configuringFeature: RingFeatureType? = null,
    val syncProgress: Int = 0,
    val lastSyncAt: Long? = null,
    val message: String? = null,
    val cloudSnapshotId: String? = null,
    val cloudRiskLevel: String? = null,
    val cloudRiskScore: Double? = null,
    val cloudRiskMode: String? = null,
    val cloudRiskSummary: String? = null,
    val patientMvp: PatientMvpPayload? = null,
    val isPatientMvpLoading: Boolean = false,
    val isInterventionGenerating: Boolean = false,
    val interventionGenerationError: String? = null,
    val measurements: Map<RingMetricType, RingMeasurementEntity> = emptyMap(),
    val sleep: RingSleepSessionEntity? = null,
    val activity: RingActivityEntity? = null,
    val todayActivitySteps: Long? = null,
    val signals: Map<RingMetricType, RingSignalChunkEntity> = emptyMap(),
    val ecgHistory: List<RingSignalChunkEntity> = emptyList(),
    val liveEcg: RingEcgLiveState = RingEcgLiveState(),
    val supportedMetrics: Set<RingMetricType> = emptySet(),
    val manuallyMeasurableMetrics: Set<RingMetricType> = emptySet(),
    val supportedFeatures: Set<RingFeatureType> = emptySet(),
    val wearableProducts: List<WearableProductOption> = emptyList(),
    val activeProductCode: String? = null,
    val hasBoundBluetoothDevice: Boolean = false,
    val backgroundCollectionEnabled: Boolean = false,
    val measurementIntervalMinutes: Int = RingBackgroundCollectionSettings.DEFAULT_MEASUREMENT_INTERVAL_MINUTES,
    val uploadIntervalMinutes: Int = RingBackgroundCollectionSettings.DEFAULT_UPLOAD_INTERVAL_MINUTES,
) {
    val collectedMetricCount: Int
        get() = measurements.keys.count { it in SupportedHardwareHealthMetrics && it != RingMetricType.SLEEP } +
            if (sleep != null) 1 else 0
}

internal fun RingUiState.clearedForPatientSession(): RingUiState = copy(
    devices = emptyList(),
    isScanning = false,
    isSyncing = false,
    measuringMetric = null,
    configuringFeature = null,
    syncProgress = 0,
    lastSyncAt = null,
    message = null,
    cloudSnapshotId = null,
    cloudRiskLevel = null,
    cloudRiskScore = null,
    cloudRiskMode = null,
    cloudRiskSummary = null,
    patientMvp = null,
    isPatientMvpLoading = false,
    isInterventionGenerating = false,
    interventionGenerationError = null,
    hasBoundBluetoothDevice = false,
    backgroundCollectionEnabled = false,
    measurements = emptyMap(),
    sleep = null,
    activity = null,
    todayActivitySteps = null,
    signals = emptyMap(),
    ecgHistory = emptyList(),
    liveEcg = RingEcgLiveState(),
)

/**
 * Real aggregated health stats for a rolling window, computed from local Room history.
 * Used by the Data screen period selector so switching 今日/7天/30天/90天 changes the data.
 */
data class PeriodAggregate(
    val windowDays: Int,
    val avgHeartRate: Double? = null,
    val avgSpo2: Double? = null,
    val minSpo2: Double? = null,
    val avgSbp: Double? = null,
    val avgDbp: Double? = null,
    val avgTemp: Double? = null,
    val totalSteps: Long = 0,
    val avgDailySteps: Double? = null,
    val avgSleepMinutes: Double? = null,
    val daysWithData: Int = 0,
    val avgRiskScore: Double? = null,
    val healthIndex: Int? = null,
    val daysWithRiskScore: Int = 0,
)

internal fun aggregateLocalDayActivitySteps(
    activities: List<RingActivityEntity>,
    now: Long = System.currentTimeMillis(),
): Long? {
    val today = localDateAt(now)
    return dailyActivityStepTotals(activities)[today]
}

/**
 * Daily sport records contain cumulative watch totals. Local device collection and cloud
 * restore can legitimately represent the same day more than once, so summing rows inflates
 * the watch value. Keep the highest cumulative total for each local calendar day.
 */
internal fun dailyActivityStepTotals(
    activities: List<RingActivityEntity>,
): Map<LocalDate, Long> = activities
    .filter { it.startedAt > 0L }
    .groupBy { localDateAt(it.startedAt) }
    .mapValues { (_, dailyRecords) ->
        dailyRecords.maxOf { it.steps.coerceAtLeast(0).toLong() }
    }

@OptIn(ExperimentalCoroutinesApi::class)
class RingViewModel(
    private val repository: RingRepository,
    private val dao: RingDataDao,
    private val cloudRepository: RingCloudRepository? = null,
    private val wearableManager: ActiveWearableManager? = null,
    private val allowWearableProductSwitch: Boolean = false,
    private val riskHistoryRepository: RiskHistoryRepository? = null,
    private val currentUserIdProvider: () -> String? = { null },
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(
        RingUiState(acquisitionMode = repository.acquisitionMode, supportedMetrics = repository.supportedMetrics),
    )
    val uiState: StateFlow<RingUiState> = mutableUiState.asStateFlow()
    private var restoreConnectionJob: Job? = null
    private var patientRefreshJob: Job? = null
    private val activePatientUserId = MutableStateFlow(
        currentUserIdProvider()?.takeIf(String::isNotBlank),
    )

    init {
        wearableManager?.let { manager ->
            mutableUiState.update {
                it.copy(
                    wearableProducts = if (allowWearableProductSwitch) {
                        userSelectableWearableProductOptions(
                            products = manager.products.map { product ->
                                WearableProductOption(product.productCode, product.displayName)
                            },
                            activeProductCode = manager.activeBinding.value.productCode,
                        )
                    } else {
                        emptyList()
                    },
                    activeProductCode = manager.activeBinding.value.productCode,
                    hasBoundBluetoothDevice = manager.activeBinding.value.let { binding ->
                        binding.vendor != WearableVendor.VIOMI_CLOUD && !binding.address.isNullOrBlank() &&
                            manager.boundToCurrentUser
                    },
                )
            }
            viewModelScope.launch {
                manager.activeBinding.collect { binding ->
                    mutableUiState.update {
                        it.copy(
                            activeProductCode = binding.productCode,
                            acquisitionMode = repository.acquisitionMode,
                            hasBoundBluetoothDevice =
                                binding.vendor != WearableVendor.VIOMI_CLOUD && !binding.address.isNullOrBlank() &&
                                    manager.boundToCurrentUser,
                            measurements = if (binding.vendor == WearableVendor.VIOMI_CLOUD) {
                                emptyMap()
                            } else {
                                it.measurements
                            },
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            repository.connectionState.collect { connectionState ->
                mutableUiState.update { it.copy(connectionState = connectionState) }
            }
        }
        viewModelScope.launch {
            repository.connectedDevice.collect { device ->
                mutableUiState.update {
                    it.copy(
                        connectedDevice = device,
                        supportedMetrics = if (device == null) emptySet() else repository.supportedMetrics,
                        manuallyMeasurableMetrics = if (device == null) {
                            emptySet()
                        } else {
                            repository.manuallyMeasurableMetrics
                        },
                        supportedFeatures = if (device == null) {
                            emptySet()
                        } else {
                            (repository as? RingFeatureRepository)?.supportedFeatures.orEmpty()
                        },
                    )
                }
            }
        }
        viewModelScope.launch {
            activePatientUserId.flatMapLatest { ownerUserId ->
                if (ownerUserId == null) {
                    flowOf(RingDatabaseSnapshot(emptyList(), null, emptyList(), emptyList()))
                } else {
                    combine(
                        dao.observeLatestMeasurementsForOwner(ownerUserId),
                        dao.observeLatestSleepSessionForOwner(ownerUserId),
                        dao.observeActivitiesForOwner(ownerUserId),
                        dao.observeLatestSignalChunksForOwner(ownerUserId),
                    ) { measurements, sleep, activities, signals ->
                        RingDatabaseSnapshot(measurements, sleep, activities, signals)
                    }
                }
            }.collect { snapshot ->
                mutableUiState.update { state ->
                    val cloudMode = state.acquisitionMode == RingAcquisitionMode.CLOUD
                    state.copy(
                        measurements = snapshot.measurements.mapNotNull { record ->
                            runCatching { RingMetricType.valueOf(record.metricType) }
                                .getOrNull()
                                ?.let { it to record }
                        }.toMap(),
                        sleep = if (cloudMode) null else snapshot.sleep,
                        activity = if (cloudMode) null else snapshot.activities.firstOrNull(),
                        todayActivitySteps = if (cloudMode) null else aggregateLocalDayActivitySteps(snapshot.activities),
                        signals = if (cloudMode) emptyMap() else snapshot.signals.mapNotNull { record ->
                            runCatching { RingMetricType.valueOf(record.signalType) }
                                .getOrNull()
                                ?.let { it to record }
                        }.toMap(),
                    )
                }
            }
        }
        viewModelScope.launch {
            activePatientUserId.flatMapLatest { ownerUserId ->
                if (ownerUserId == null) flowOf(emptyList())
                else dao.observeSignalChunksForOwner(ownerUserId, RingMetricType.ECG.name, ECG_HISTORY_LIMIT)
            }.collect { records -> mutableUiState.update { it.copy(ecgHistory = records) } }
        }
        (repository as? RingEcgRepository)?.let { ecgRepository ->
            viewModelScope.launch {
                combine(activePatientUserId, ecgRepository.liveEcg) { ownerUserId, live ->
                    if (ownerUserId == null) RingEcgLiveState() else live
                }.collect { live ->
                    mutableUiState.update { it.copy(liveEcg = live) }
                }
            }
        }
    }

    /**
     * Page-level auto collection was intentionally removed: the Foreground
     * Service is the single scheduler for unattended measurement (D2 §5). A
     * second in-page loop would measure the same device twice and bypass the
     * configured upload cadence.
     */
    fun stopAutoCollection() {
        // Retained as a lifecycle hook so stage transitions can stop any legacy
        // collection job; the page loop itself no longer exists.
    }

    /** Explicitly opts into continuous local collection for the encrypted Bluetooth binding. */
    fun startBackgroundCollection(context: Context) {
        val appContext = context.applicationContext
        val binding = wearableManager?.activeBinding?.value
        if (repository.acquisitionMode == RingAcquisitionMode.CLOUD) {
            val planRepository = repository as? RingActiveMeasurementPlanRepository
            if (planRepository == null || repository.connectedDevice.value == null) {
                mutableUiState.update { it.copy(message = "请先绑定云米设备，再启用主动测量") }
                return
            }
            viewModelScope.launch {
                mutableUiState.update { it.copy(message = "正在保存云米主动测量计划") }
                runCatching {
                    planRepository.configureActiveMeasurement(currentMeasurementInterval(), enabled = true)
                }.onSuccess {
                    RingBackgroundCollectionSettings.setCloudPlanActive(
                        appContext,
                        currentUserIdProvider(),
                        true,
                    )
                    mutableUiState.update {
                        it.copy(backgroundCollectionEnabled = true, message = "云米云端主动测量已启用")
                    }
                }.onFailure { error ->
                    mutableUiState.update { it.copy(message = error.message ?: "云米主动测量启用失败") }
                }
            }
            return
        }
        if (binding?.address.isNullOrBlank()) {
            mutableUiState.update { it.copy(message = "请先绑定 HBand 设备，再启用后台采集") }
            return
        }
        RingForegroundService.start(appContext)
        mutableUiState.update {
            it.copy(
                backgroundCollectionEnabled = true,
                message = "后台采集已启用，每 ${it.measurementIntervalMinutes} 分钟主动测量一次",
            )
        }
    }

    /**
     * Stops collection. [disableServerPlan] controls whether a Viomi cloud plan
     * is also disabled server-side. Logout/401 cleanup passes false: the cloud
     * plan is account-level state and must only change when the user explicitly
     * turns it off or unbinds the device (D2 §10), never silently on logout.
     */
    fun stopBackgroundCollection(context: Context, disableServerPlan: Boolean = true) {
        if (repository.acquisitionMode == RingAcquisitionMode.CLOUD) {
            if (!disableServerPlan) return
            val planRepository = repository as? RingActiveMeasurementPlanRepository ?: return
            viewModelScope.launch {
                runCatching { planRepository.configureActiveMeasurement(currentMeasurementInterval(), enabled = false) }
                    .onSuccess {
                        RingBackgroundCollectionSettings.setCloudPlanActive(
                            context.applicationContext,
                            currentUserIdProvider(),
                            false,
                        )
                        mutableUiState.update {
                            it.copy(backgroundCollectionEnabled = false, message = "云米云端主动测量已关闭")
                        }
                    }
                    .onFailure { error ->
                        mutableUiState.update { it.copy(message = error.message ?: "云米主动测量关闭失败") }
                    }
            }
            return
        }
        RingForegroundService.stop(context.applicationContext)
        mutableUiState.update {
            it.copy(
                backgroundCollectionEnabled = false,
                message = "后台采集已关闭",
            )
        }
    }

    fun refreshBackgroundCollectionState(context: Context) {
        val binding = wearableManager?.activeBinding?.value
        mutableUiState.update {
            it.copy(
                hasBoundBluetoothDevice = binding != null &&
                    binding.vendor != WearableVendor.VIOMI_CLOUD &&
                    !binding.address.isNullOrBlank() &&
                    (wearableManager?.boundToCurrentUser ?: false),
                backgroundCollectionEnabled = if (repository.acquisitionMode == RingAcquisitionMode.CLOUD) {
                    RingBackgroundCollectionSettings.isCloudPlanActive(
                        context.applicationContext,
                        currentUserIdProvider(),
                    )
                } else {
                    RingBackgroundCollectionSettings.isActive(
                        context.applicationContext,
                        currentUserIdProvider(),
                    )
                },
                measurementIntervalMinutes = RingBackgroundCollectionSettings.measurementIntervalMinutes(
                    context.applicationContext,
                    currentUserIdProvider(),
                ),
                uploadIntervalMinutes = RingBackgroundCollectionSettings.uploadIntervalMinutes(
                    context.applicationContext,
                    currentUserIdProvider(),
                ),
            )
        }
    }

    fun setMeasurementInterval(context: Context, minutes: Int) {
        val appContext = context.applicationContext
        runCatching {
            RingBackgroundCollectionSettings.setMeasurementIntervalMinutes(
                appContext,
                currentUserIdProvider(),
                minutes,
            )
        }
            .onSuccess {
                if (repository.acquisitionMode == RingAcquisitionMode.CLOUD) {
                    val planRepository = repository as? RingActiveMeasurementPlanRepository
                    mutableUiState.update { it.copy(measurementIntervalMinutes = minutes) }
                    if (planRepository != null && mutableUiState.value.backgroundCollectionEnabled) {
                        viewModelScope.launch {
                            runCatching { planRepository.configureActiveMeasurement(minutes, enabled = true) }
                                .onSuccess {
                                    mutableUiState.update { it.copy(message = "云米主动测量间隔已设为 $minutes 分钟") }
                                }
                                .onFailure { error -> mutableUiState.update { it.copy(message = error.message) } }
                        }
                    }
                    return@onSuccess
                }
                // The running collection loop re-reads the measurement interval on
                // every round (RingForegroundService.runCollectionLoop), so the new
                // value takes effect on the next cycle without touching the service.
                // Restarting the foreground service here caused a stop/start race on
                // rapid taps: a startForegroundService() request stayed pending while
                // stopSelf() tore the service down, and the system killed the app 5s
                // later with RemoteServiceException$ForegroundServiceDidNotStartInTimeException.
                mutableUiState.update {
                    it.copy(
                        measurementIntervalMinutes = minutes,
                        message = "主动测量间隔已设为 $minutes 分钟，下一轮采集生效",
                    )
                }
            }
            .onFailure { error -> mutableUiState.update { it.copy(message = error.message) } }
    }

    private fun currentMeasurementInterval(): Int = mutableUiState.value.measurementIntervalMinutes

    fun setUploadInterval(context: Context, minutes: Int) {
        val appContext = context.applicationContext
        runCatching {
            RingBackgroundCollectionSettings.setUploadIntervalMinutes(
                appContext,
                currentUserIdProvider(),
                minutes,
            )
        }
            .onSuccess {
                com.rehealth.genie.work.TelemetryUploadWorker.schedule(
                    appContext,
                    currentUserIdProvider(),
                )
                mutableUiState.update {
                    it.copy(uploadIntervalMinutes = minutes, message = "数据上传间隔已更新")
                }
            }
            .onFailure { error -> mutableUiState.update { it.copy(message = error.message) } }
    }

    /**
     * Restores only the previously persisted device connection after the app process is recreated.
     * Providers must use their encrypted active binding and must not scan or collect data here.
     */
    fun restoreLastConnection() {
        if (restoreConnectionJob?.isActive == true) return
        val binding = wearableManager?.activeBinding?.value ?: return
        if (binding.address.isNullOrBlank()) return
        if (repository.connectionState.value == RingConnectionState.CONNECTED) return
        restoreConnectionJob = viewModelScope.launch {
            mutableUiState.update { it.copy(message = "正在恢复上次设备连接") }
            val connected = runCatching { repository.autoConnect() }
                .onFailure { error -> Log.w(TAG, "last device reconnect failed", error) }
                .getOrDefault(false)
            mutableUiState.update {
                it.copy(
                    connectionState = repository.connectionState.value,
                    message = if (connected) {
                        "已恢复上次设备连接"
                    } else {
                        "未能恢复上次设备连接，请检查设备电量和蓝牙"
                    },
                )
            }
        }
    }

    fun switchWearableProduct(context: Context, productCode: String) {
        val manager = wearableManager ?: return
        if (!allowWearableProductSwitch || productCode == mutableUiState.value.activeProductCode) return
        if (mutableUiState.value.isSyncing || mutableUiState.value.isScanning) return
        val appContext = context.applicationContext
        val resumeBackground = RingBackgroundCollectionSettings.isActive(
            appContext,
            currentUserIdProvider(),
        )
        if (resumeBackground) RingForegroundService.stop(appContext)
        stopAutoCollection()
        viewModelScope.launch {
            mutableUiState.update { it.copy(message = "正在切换设备套餐", devices = emptyList()) }
            runCatching { manager.switchProduct(productCode) }
                .onSuccess {
                    mutableUiState.update {
                        it.copy(
                            devices = emptyList(),
                            message = "套餐已切换，请搜索并绑定新设备；历史健康数据已保留",
                        )
                    }
                }
                .onFailure { error ->
                    mutableUiState.update { it.copy(message = error.message ?: "设备套餐切换失败") }
                }
            if (resumeBackground && repository.acquisitionMode != RingAcquisitionMode.CLOUD) {
                RingForegroundService.start(appContext)
            }
            mutableUiState.update {
                it.copy(backgroundCollectionEnabled = resumeBackground && repository.acquisitionMode != RingAcquisitionMode.CLOUD)
            }
        }
    }

    fun scan() {
        viewModelScope.launch {
            mutableUiState.update { it.copy(isScanning = true, message = "正在搜索附近设备") }
            runCatching { repository.scan() }
                .onSuccess { devices ->
                    mutableUiState.update {
                        it.copy(
                            devices = devices,
                            isScanning = false,
                            message = if (devices.isEmpty()) "没有发现设备" else "发现 ${devices.size} 台设备",
                        )
                    }
                }
                .onFailure { error ->
                    mutableUiState.update {
                        it.copy(isScanning = false, message = error.message ?: "扫描失败")
                    }
                }
        }
    }

    fun connect(device: RingDevice) {
        viewModelScope.launch {
            mutableUiState.update { it.copy(message = "正在连接 ${device.name ?: "智能戒指"}") }
            runCatching { repository.connect(device) }
                .onSuccess {
                    val cloudMode = repository.acquisitionMode == RingAcquisitionMode.CLOUD
                    val binding = if (repository.acquisitionMode == RingAcquisitionMode.CLOUD) {
                        null
                    } else {
                        cloudRepository?.bindDevice(device)
                    }
                    mutableUiState.update {
                        it.copy(
                            connectionState = repository.connectionState.value,
                            message = if (binding == null || binding.isSuccess) {
                                if (cloudMode) "云米设备已绑定，正在同步健康数据" else "设备已连接"
                            } else {
                                "设备已连接，云端绑定失败，可稍后重新连接重试"
                            },
                        )
                    }
                    if (cloudMode) syncAll()
                }
                .onFailure { error ->
                    mutableUiState.update { it.copy(message = error.message ?: "连接失败") }
                }
        }
    }

    fun disconnect() {
        restoreConnectionJob?.cancel()
        restoreConnectionJob = null
        viewModelScope.launch {
            runCatching { repository.disconnect() }
                .onSuccess {
                    mutableUiState.update {
                        it.copy(isSyncing = false, measuringMetric = null, syncProgress = 0, message = "设备已断开")
                    }
                }
                .onFailure { error ->
                    mutableUiState.update {
                        it.copy(
                            isSyncing = false,
                            measuringMetric = null,
                            syncProgress = 0,
                            message = error.message ?: "设备断开失败，请稍后重试",
                        )
                    }
                }
        }
    }

    fun syncAll() {
        viewModelScope.launch {
            val cloudMode = repository.acquisitionMode == RingAcquisitionMode.CLOUD
            val connected = if (cloudMode) {
                repository.connectionState.value == RingConnectionState.CONNECTED
            } else {
                runCatching { repository.autoConnect() }.getOrDefault(false)
            }
            if (!connected) {
                mutableUiState.update {
                    it.copy(
                        isSyncing = false,
                        syncProgress = 0,
                        message = if (cloudMode) "请先绑定云米设备" else "请先连接设备，再同步睡眠、步数与活动",
                    )
                }
                return@launch
            }
            mutableUiState.update {
                it.copy(
                    isSyncing = true,
                    syncProgress = 5,
                    message = if (cloudMode) "正在同步云米心率、血氧和血压" else "正在同步睡眠、步数与活动",
                )
            }
            val targetProgress = MutableStateFlow(5)
            val progressJob = launch {
                while (true) {
                    delay(SYNC_PROGRESS_TICK_MILLIS)
                    mutableUiState.update { state ->
                        state.copy(
                            syncProgress = (state.syncProgress + 1)
                                .coerceAtMost(targetProgress.value.coerceAtMost(95)),
                        )
                    }
                }
            }
            runCatching {
                repository.sync(if (cloudMode) CLOUD_SYNC_METRICS else DAILY_SYNC_METRICS) { progress ->
                    targetProgress.update { current -> maxOf(current, progress.coerceIn(5, 95)) }
                }
            }
                .onSuccess { result ->
                    progressJob.cancel()
                    val uploadMessage = if (result.recordsWritten > 0 && result.requiresUpload) {
                        uploadLatestSnapshot(result.completedAt, "manual_sync")
                    } else {
                        null
                    }
                    mutableUiState.update {
                        it.copy(
                            isSyncing = false,
                            syncProgress = 100,
                            lastSyncAt = result.completedAt,
                            cloudSnapshotId = uploadMessage?.batchId ?: it.cloudSnapshotId,
                            message = if (result.recordsWritten > 0) {
                                if (result.requiresUpload) {
                                    "${result.recordsWritten} 条设备数据已保存到本机，${uploadMessage?.message ?: "云端未上传"}"
                                } else {
                                    "云米云端已入库，${result.recordsWritten} 条数据已同步到本机"
                                }
                            } else {
                                if (repository.acquisitionMode == RingAcquisitionMode.CLOUD) {
                                    "云米云端暂无新数据"
                                } else {
                                    "未读取到戒指数据，请确认戒指仍保持连接"
                                }
                            },
                        )
                    }
                }
                .onFailure { error ->
                    progressJob.cancel()
                    mutableUiState.update {
                        it.copy(
                            isSyncing = false,
                            syncProgress = 0,
                            message = error.message ?: "同步失败",
                        )
                    }
                }
        }
    }

    fun refreshPatientMvp() {
        activePatientUserId.value = currentUserIdProvider()?.takeIf(String::isNotBlank)
        patientRefreshJob?.cancel()
        patientRefreshJob = viewModelScope.launch {
            refreshPatientMvp(silent = false)
        }
    }

    fun generateIntervention() {
        if (mutableUiState.value.isInterventionGenerating) return
        val client = cloudRepository
        if (client == null) {
            mutableUiState.update {
                it.copy(interventionGenerationError = "当前环境未配置个性化干预服务。")
            }
            return
        }
        viewModelScope.launch {
            mutableUiState.update {
                it.copy(
                    isInterventionGenerating = true,
                    interventionGenerationError = null,
                    message = "正在基于最新健康档案生成个性化干预计划",
                )
            }
            client.generateIntervention()
                .onSuccess { plans ->
                    mutableUiState.update { state ->
                        state.copy(
                            patientMvp = state.patientMvp?.copy(
                                interventionPlan = plans,
                                updatedAt = System.currentTimeMillis(),
                            ) ?: PatientMvpPayload(
                                profile = null,
                                risk = null,
                                interventionPlan = plans,
                                recentCheckins = emptyList(),
                                updatedAt = System.currentTimeMillis(),
                            ),
                            isInterventionGenerating = false,
                            interventionGenerationError = null,
                            message = "个性化干预计划已生成",
                        )
                    }
                }
                .onFailure { error ->
                    if (error is CancellationException) return@onFailure
                    mutableUiState.update {
                        it.copy(
                            isInterventionGenerating = false,
                            interventionGenerationError = error.message
                                ?: "个性化干预计划生成失败，请稍后重试。",
                            message = "个性化干预计划生成失败",
                        )
                    }
                }
        }
    }

    fun clearPatientSession() {
        patientRefreshJob?.cancel()
        patientRefreshJob = null
        activePatientUserId.value = null
        pushProfileToRepository(repository, null)
        mutableUiState.update(RingUiState::clearedForPatientSession)
    }

    fun measure(type: RingMetricType) {
        viewModelScope.launch {
            Log.i(TAG, "measure clicked type=$type")
            val action = "测量"
            mutableUiState.update {
                it.copy(
                    isSyncing = true,
                    measuringMetric = type,
                    syncProgress = 15,
                    message = "请保持设备佩戴稳定，正在${action}${type.displayName()}",
                )
            }
            val progressJob = launch {
                listOf(32, 55, 76, 92).forEach { progress ->
                    delay(1_500)
                    mutableUiState.update { it.copy(syncProgress = progress) }
                }
            }
            runCatching { repository.measure(type) }
                .onSuccess { result ->
                    progressJob.cancel()
                    mutableUiState.update {
                        it.copy(
                            isSyncing = false,
                            measuringMetric = null,
                            syncProgress = 100,
                            lastSyncAt = result.completedAt,
                            message = if (result.recordsWritten > 0) {
                                "${type.displayName()}${action}完成，结果已保存"
                            } else {
                                "没有读取到${type.displayName()}结果，请保持设备连接后重试"
                            },
                        )
                    }
                }
                .onFailure { error ->
                    progressJob.cancel()
                    mutableUiState.update {
                        it.copy(
                            isSyncing = false,
                            measuringMetric = null,
                            syncProgress = 0,
                            message = error.message ?: "${type.displayName()}${action}失败",
                        )
                    }
                }
        }
    }

    /**
     * Aggregate measurements/activities/sleep from the local DB for the last [windowDays] days.
     * windowDays == 0 means "since start of today".
     */
    suspend fun loadPeriodAggregate(windowDays: Int): PeriodAggregate {
        val since = periodStartMillis(windowDays)
        val cloudMode = repository.acquisitionMode == RingAcquisitionMode.CLOUD
        val ownerUserId = activePatientUserId.value ?: return PeriodAggregate(windowDays)
        val measurements = dao.getMeasurementsSinceForOwner(since, ownerUserId)
        val activities = if (cloudMode) emptyList() else dao.getActivitiesSinceForOwner(since, ownerUserId)
        val sleep = if (cloudMode) emptyList() else dao.getSleepSessionsSinceForOwner(since, ownerUserId)

        fun average(metricType: String, value: (RingMeasurementEntity) -> Double?): Double? {
            val matching = measurements.filter { it.metricType == metricType }
            val values = if (cloudMode) {
                matching
                    .groupBy { localDateAt(it.measuredAt) }
                    .values
                    .mapNotNull { rows ->
                        rows.mapNotNull(value).takeIf { it.isNotEmpty() }?.average()
                    }
            } else {
                matching.mapNotNull(value)
            }
            return values.takeIf { it.isNotEmpty() }?.average()
        }
        val spo2Values = measurements
            .filter { it.metricType == RingMetricType.BLOOD_OXYGEN.name }
            .map { it.primaryValue }
        val dailySteps = dailyActivityStepTotals(activities)
        val totalSteps = dailySteps.values.sum()
        val daysWithSteps = dailySteps.size
        val avgSleep = averageDailySleepMinutes(sleep)
        val daysWithMeasurements = measurements.map { localDateAt(it.measuredAt) }.distinct().size
        val riskSummary = riskHistoryRepository?.periodSummary(windowDays)

        return PeriodAggregate(
            windowDays = windowDays,
            avgHeartRate = average(RingMetricType.HEART_RATE.name) { it.primaryValue },
            avgSpo2 = average(RingMetricType.BLOOD_OXYGEN.name) { it.primaryValue },
            minSpo2 = spo2Values.minOrNull(),
            avgSbp = average(RingMetricType.BLOOD_PRESSURE.name) { it.primaryValue },
            avgDbp = average(RingMetricType.BLOOD_PRESSURE.name) { it.secondaryValue },
            avgTemp = average(RingMetricType.TEMPERATURE.name) { it.primaryValue },
            totalSteps = totalSteps,
            avgDailySteps = if (daysWithSteps > 0) totalSteps.toDouble() / daysWithSteps else null,
            avgSleepMinutes = avgSleep,
            daysWithData = daysWithMeasurements,
            avgRiskScore = riskSummary?.averageRiskScore,
            healthIndex = riskSummary?.averageHealthIndex,
            daysWithRiskScore = riskSummary?.daysWithScore ?: 0,
        )
    }

    fun setBloodGlucoseCalibration(enabled: Boolean, referenceValue: Double) {
        configureFeature(
            feature = RingFeatureType.BLOOD_GLUCOSE_CALIBRATION,
            workingMessage = "正在写入血糖校准设置",
            successMessage = if (enabled) "血糖校准已启用" else "血糖校准已关闭",
        ) { repository ->
            repository.setBloodGlucoseCalibration(BloodGlucoseCalibration(enabled, referenceValue))
        }
    }

    fun setMenstrualCycle(periodLengthDays: Int, cycleLengthDays: Int, lastPeriodStartAt: Long) {
        configureFeature(
            feature = RingFeatureType.WOMENS_HEALTH,
            workingMessage = "正在写入女性健康设置",
            successMessage = "女性健康周期已保存到设备",
        ) { repository ->
            repository.setMenstrualCycle(
                MenstrualCycleConfig(periodLengthDays, cycleLengthDays, lastPeriodStartAt),
            )
        }
    }

    private fun configureFeature(
        feature: RingFeatureType,
        workingMessage: String,
        successMessage: String,
        operation: suspend (RingFeatureRepository) -> Boolean,
    ) {
        val featureRepository = repository as? RingFeatureRepository
        if (featureRepository == null || feature !in featureRepository.supportedFeatures) {
            mutableUiState.update { it.copy(message = "当前设备不支持此功能") }
            return
        }
        viewModelScope.launch {
            mutableUiState.update {
                it.copy(
                    isSyncing = true,
                    configuringFeature = feature,
                    syncProgress = 30,
                    message = workingMessage,
                )
            }
            runCatching { operation(featureRepository) }
                .onSuccess { success ->
                    mutableUiState.update {
                        it.copy(
                            isSyncing = false,
                            configuringFeature = null,
                            syncProgress = if (success) 100 else 0,
                            message = if (success) successMessage else "设备未接受设置，请检查佩戴与连接状态",
                        )
                    }
                }
                .onFailure { error ->
                    mutableUiState.update {
                        it.copy(
                            isSyncing = false,
                            configuringFeature = null,
                            syncProgress = 0,
                            message = error.message ?: "设备设置失败",
                        )
                    }
                }
        }
    }

    suspend fun loadHealthHistory(limitPerType: Int = 50): RingHealthHistory =
        activePatientUserId.value?.let { dao.loadRingHealthHistory(it, limitPerType) }
            ?: RingHealthHistory()

    private data class RingDatabaseSnapshot(
        val measurements: List<RingMeasurementEntity>,
        val sleep: RingSleepSessionEntity?,
        val activities: List<RingActivityEntity>,
        val signals: List<RingSignalChunkEntity>,
    )

    private suspend fun uploadLatestSnapshot(collectedAt: Long, trigger: String): CloudUploadUiStatus {
        val client = cloudRepository ?: return CloudUploadUiStatus("云端未配置")
        val device = mutableUiState.value.connectedDevice
            ?: return CloudUploadUiStatus("未绑定设备，云端未上传")
        return client.enqueueLatestTelemetry(device, collectedAt, trigger).fold(
            onSuccess = { batchId -> CloudUploadUiStatus("已进入安全上传队列", batchId) },
            onFailure = { error ->
                Log.w(TAG, "cloud queue enqueue failed type=${error::class.java.simpleName}")
                CloudUploadUiStatus("云端稍后重试")
            },
        )
    }

    private suspend fun refreshPatientMvp(silent: Boolean) {
        val client = cloudRepository ?: return
        if (!silent) {
            mutableUiState.update { it.copy(isPatientMvpLoading = true, message = "正在读取个人资料与健康档案") }
        }
        client.restoreRecentTelemetryOncePerSession()
            .onSuccess { restoredCount ->
                if (restoredCount > 0) {
                    mutableUiState.update { it.copy(lastSyncAt = System.currentTimeMillis()) }
                }
            }
            .onFailure { error ->
                if (error is CancellationException) throw error
                Log.w(TAG, "login telemetry restore failed type=${error::class.java.simpleName}")
            }
        client.fetchPatientMvp()
            .onSuccess { mvp ->
                val risk = mvp.risk
                pushProfileToRepository(repository, mvp.profile)
                mutableUiState.update {
                    it.copy(
                        patientMvp = mvp,
                        isPatientMvpLoading = false,
                        cloudRiskLevel = risk?.riskLevel ?: it.cloudRiskLevel,
                        cloudRiskScore = risk?.riskScore ?: it.cloudRiskScore,
                        cloudRiskMode = risk?.mode ?: it.cloudRiskMode,
                        cloudRiskSummary = risk?.summary ?: it.cloudRiskSummary,
                        message = if (silent) it.message else "个人资料与健康档案已更新",
                    )
                }
            }
            .onFailure { error ->
                if (error is CancellationException) return@onFailure
                Log.w(TAG, "patient mvp refresh failed", error)
                mutableUiState.update {
                    it.copy(
                        isPatientMvpLoading = false,
                        message = if (silent) it.message else "资料读取失败，请检查网络后重试",
                    )
                }
            }
    }

    class Factory(
        private val repository: RingRepository,
        private val dao: RingDataDao,
        private val cloudRepository: RingCloudRepository? = null,
        private val wearableManager: ActiveWearableManager? = null,
        private val allowWearableProductSwitch: Boolean = false,
        private val riskHistoryRepository: RiskHistoryRepository? = null,
        private val currentUserIdProvider: () -> String? = { null },
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            RingViewModel(
                repository,
                dao,
                cloudRepository,
                wearableManager,
                allowWearableProductSwitch,
                riskHistoryRepository,
                currentUserIdProvider,
            ) as T
    }
}

private data class CloudUploadUiStatus(
    val message: String,
    val batchId: String? = null,
)

private const val TAG = "RingViewModel"
private const val SYNC_PROGRESS_TICK_MILLIS = 180L
private const val ECG_HISTORY_LIMIT = 10
private val DAILY_SYNC_METRICS = setOf(
    RingMetricType.SLEEP,
    RingMetricType.STEPS,
    RingMetricType.ACTIVITY,
)
private val CLOUD_SYNC_METRICS = setOf(
    RingMetricType.HEART_RATE,
    RingMetricType.BLOOD_OXYGEN,
    RingMetricType.BLOOD_PRESSURE,
)
internal fun canonicalSleepMinutes(session: RingSleepSessionEntity): Int? {
    session.totalSleepMinutes?.takeIf { it > 0 }?.let { return it }
    // Awake time is part of the session span, not actual sleep duration.
    val stagedMinutes = session.deepMinutes + session.lightMinutes + session.remMinutes
    if (stagedMinutes > 0) return stagedMinutes
    return ((session.endedAt - session.startedAt) / 60_000L)
        .toInt()
        .takeIf { it > 0 }
}

/** Selects the same final nightly record used by both the Data and Profile surfaces. */
internal fun preferredSleepSession(
    sessions: List<RingSleepSessionEntity>,
): RingSleepSessionEntity? = sessions.maxWithOrNull(
    compareBy<RingSleepSessionEntity> { it.endedAt }
        .thenBy { if ((it.totalSleepMinutes ?: 0) > 0) 1 else 0 }
        .thenBy { canonicalSleepMinutes(it) ?: 0 }
        .thenBy { it.startedAt }
        .thenBy { it.id },
)

/**
 * Vendor SDKs may emit several cumulative snapshots while assembling one night's sleep.
 * Select the same preferred final result for each local wake-up day before averaging days,
 * otherwise one night is incorrectly counted several times.
 */
internal fun averageDailySleepMinutes(sessions: List<RingSleepSessionEntity>): Double? {
    val dailyMinutes = sessions
        .groupBy { localDateAt(it.endedAt) }
        .values
        .mapNotNull { dailySessions ->
            preferredSleepSession(dailySessions)?.let(::canonicalSleepMinutes)
        }
    return dailyMinutes.takeIf(List<Int>::isNotEmpty)?.average()
}

private fun localDateAt(timestamp: Long) =
    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()

private fun periodStartMillis(windowDays: Int): Long {
    return Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (windowDays > 1) add(Calendar.DAY_OF_YEAR, -(windowDays - 1))
    }.timeInMillis
}

/**
 * Feeds the real patient profile (when available) into the mock ring repository so its
 * simulated vitals are computed from actual profile inputs rather than a neutral baseline.
 */
private fun pushProfileToRepository(repository: RingRepository, profile: PatientProfilePayload?) {
    val baseline: BaselineHealthProfile? = HealthMemorySnapshot.fromPatientProfile(profile).profile
    (repository as? SimulatedRingProfileSink)?.profile = baseline
    (repository as? WearableUserProfileSink)?.wearableUserProfile = baseline
}

data class WearableProductOption(val productCode: String, val displayName: String)

/**
 * The current pilot exposes only the two supported connection workflows:
 * HBand over Bluetooth and Viomi watches through the cloud IMEI flow.
 * Legacy MRD/RWFit providers remain available only for Debug engineering QA;
 * Release migrates those stored selections to HBand.
 */
internal fun userSelectableWearableProductOptions(
    products: List<WearableProductOption>,
    activeProductCode: String?,
): List<WearableProductOption> = collapseViomiProductOptions(
    products = products.filter { option ->
        option.productCode == HBAND_PRODUCT_CODE || option.productCode.startsWith(VIOMI_PRODUCT_CODE_PREFIX)
    },
    activeProductCode = activeProductCode,
).map { option ->
    when {
        option.productCode == HBAND_PRODUCT_CODE -> option.copy(displayName = "HBand")
        option.productCode.startsWith(VIOMI_PRODUCT_CODE_PREFIX) -> option.copy(displayName = "云米（IMEI 云端）")
        else -> option
    }
}

/**
 * All supported Viomi watches use the same cloud/IMEI workflow. Keep the concrete
 * productCode for backend compatibility, but expose one vendor-level choice to users.
 */
internal fun collapseViomiProductOptions(
    products: List<WearableProductOption>,
    activeProductCode: String?,
): List<WearableProductOption> {
    val viomiProducts = products.filter { it.productCode.startsWith(VIOMI_PRODUCT_CODE_PREFIX) }
    if (viomiProducts.isEmpty()) return products
    val selectedViomi = viomiProducts.firstOrNull { it.productCode == activeProductCode }
        ?: viomiProducts.first()
    val viomiOption = WearableProductOption(selectedViomi.productCode, "云米")
    val firstViomiIndex = products.indexOfFirst { it.productCode.startsWith(VIOMI_PRODUCT_CODE_PREFIX) }
    val visible = products.filterNot { it.productCode.startsWith(VIOMI_PRODUCT_CODE_PREFIX) }.toMutableList()
    visible.add(firstViomiIndex.coerceAtMost(visible.size), viomiOption)
    return visible
}

private const val VIOMI_PRODUCT_CODE_PREFIX = "RH-VM-"

private fun RingMetricType.displayName(): String = when (this) {
    RingMetricType.HEART_RATE -> "心率"
    RingMetricType.BLOOD_OXYGEN -> "血氧"
    RingMetricType.BLOOD_PRESSURE -> "血压"
    RingMetricType.BLOOD_GLUCOSE -> "血糖"
    RingMetricType.TEMPERATURE -> "体温"
    RingMetricType.HRV -> "HRV"
    RingMetricType.SLEEP -> "睡眠"
    RingMetricType.STEPS -> "步数"
    RingMetricType.ACTIVITY -> "运动"
    RingMetricType.STRESS -> "压力"
    RingMetricType.MET -> "MET"
    RingMetricType.RRI -> "RRI"
    RingMetricType.PPG -> "PPG"
    RingMetricType.ECG -> "ECG"
    RingMetricType.BLOOD_COMPONENT -> "血液成分"
    RingMetricType.URIC_ACID -> "尿酸"
    RingMetricType.TOTAL_CHOLESTEROL -> "总胆固醇"
    RingMetricType.TRIGLYCERIDES -> "甘油三酯"
    RingMetricType.HDL_CHOLESTEROL -> "HDL"
    RingMetricType.LDL_CHOLESTEROL -> "LDL"
    RingMetricType.BODY_COMPOSITION -> "身体成分"
    RingMetricType.BMI -> "BMI"
    RingMetricType.BODY_FAT_PERCENT -> "体脂率"
    RingMetricType.FAT_MASS -> "脂肪量"
    RingMetricType.FAT_FREE_MASS -> "去脂体重"
    RingMetricType.MUSCLE_PERCENT -> "肌肉率"
    RingMetricType.MUSCLE_MASS -> "肌肉量"
    RingMetricType.SUBCUTANEOUS_FAT_PERCENT -> "皮下脂肪率"
    RingMetricType.BODY_WATER_PERCENT -> "体水分率"
    RingMetricType.WATER_MASS -> "水分量"
    RingMetricType.SKELETAL_MUSCLE_PERCENT -> "骨骼肌率"
    RingMetricType.BONE_MASS -> "骨量"
    RingMetricType.PROTEIN_PERCENT -> "蛋白质率"
    RingMetricType.PROTEIN_MASS -> "蛋白质量"
    RingMetricType.BASAL_METABOLIC_RATE -> "基础代谢"
}
