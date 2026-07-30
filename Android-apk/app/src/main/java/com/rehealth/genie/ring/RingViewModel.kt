package com.rehealth.genie.ring

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rehealth.genie.data.sync.RingCloudRepository
import com.rehealth.genie.network.PatientInterventionPayload
import com.rehealth.genie.network.PatientMvpPayload
import com.rehealth.genie.network.PatientProfilePayload
import com.rehealth.genie.network.PatientRiskPayload
import com.rehealth.genie.features.BaselineHealthProfile
import com.rehealth.genie.features.HealthMemorySnapshot
import com.rehealth.genie.phm.CvdFeatureVector
import com.rehealth.genie.phm.CvdRiskHeuristic
import com.rehealth.genie.ring.SupportedHardwareHealthMetrics
import com.rehealth.genie.ring.data.RingActivityEntity
import com.rehealth.genie.ring.data.RingDataDao
import com.rehealth.genie.ring.data.RingMeasurementEntity
import com.rehealth.genie.ring.data.RingSignalChunkEntity
import com.rehealth.genie.ring.data.RingSleepSessionEntity
import com.rehealth.genie.ring.provider.ActiveWearableManager
import com.rehealth.genie.service.RingForegroundService
import java.util.Calendar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val initialFallbackMvp = buildFallbackPatientMvp()

data class RingUiState(
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
    val cloudRiskLevel: String? = initialFallbackMvp.risk?.riskLevel,
    val cloudRiskScore: Double? = initialFallbackMvp.risk?.riskScore,
    val cloudRiskMode: String? = initialFallbackMvp.risk?.mode,
    val cloudRiskSummary: String? = initialFallbackMvp.risk?.summary,
    val patientMvp: PatientMvpPayload? = initialFallbackMvp,
    val isPatientMvpLoading: Boolean = false,
    val measurements: Map<RingMetricType, RingMeasurementEntity> = emptyMap(),
    val sleep: RingSleepSessionEntity? = null,
    val activity: RingActivityEntity? = null,
    val signals: Map<RingMetricType, RingSignalChunkEntity> = emptyMap(),
    val ecgHistory: List<RingSignalChunkEntity> = emptyList(),
    val liveEcg: RingEcgLiveState = RingEcgLiveState(),
    val supportedMetrics: Set<RingMetricType> = emptySet(),
    val supportedFeatures: Set<RingFeatureType> = emptySet(),
    val wearableProducts: List<WearableProductOption> = emptyList(),
    val activeProductCode: String? = null,
) {
    val collectedMetricCount: Int
        get() = measurements.keys.count { it in SupportedHardwareHealthMetrics && it != RingMetricType.SLEEP } +
            if (sleep != null) 1 else 0
}

/**
 * Real aggregated health stats for a rolling window, computed from local Room history.
 * Used by the Data screen period selector so switching 今日/7天/30天/90天 changes the data.
 */
data class PeriodAggregate(
    val windowDays: Int,
    val avgHeartRate: Double? = null,
    val avgSpo2: Double? = null,
    val avgSbp: Double? = null,
    val avgDbp: Double? = null,
    val avgTemp: Double? = null,
    val totalSteps: Long = 0,
    val avgDailySteps: Double? = null,
    val avgSleepMinutes: Double? = null,
    val daysWithData: Int = 0,
)

class RingViewModel(
    private val repository: RingRepository,
    private val dao: RingDataDao,
    private val cloudRepository: RingCloudRepository? = null,
    private val wearableManager: ActiveWearableManager? = null,
    private val allowWearableProductSwitch: Boolean = false,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(RingUiState(supportedMetrics = repository.supportedMetrics))
    val uiState: StateFlow<RingUiState> = mutableUiState.asStateFlow()
    private var autoCollectionJob: Job? = null
    private var patientRefreshJob: Job? = null
    private var lastRingVector: CvdFeatureVector = CvdFeatureVector()

    init {
        wearableManager?.let { manager ->
            mutableUiState.update {
                it.copy(
                    wearableProducts = if (allowWearableProductSwitch) {
                        manager.products.map { product ->
                            WearableProductOption(product.productCode, product.displayName)
                        }
                    } else {
                        emptyList()
                    },
                    activeProductCode = manager.activeBinding.value.productCode,
                )
            }
            viewModelScope.launch {
                manager.activeBinding.collect { binding ->
                    mutableUiState.update { it.copy(activeProductCode = binding.productCode) }
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
            combine(
                dao.observeLatestMeasurements(),
                dao.observeLatestSleepSession(),
                dao.observeLatestActivity(),
                dao.observeLatestSignalChunks(),
            ) { measurements, sleep, activity, signals ->
                RingDatabaseSnapshot(measurements, sleep, activity, signals)
            }.collect { snapshot ->
                lastRingVector = vectorFromMeasurements(snapshot.measurements)
                mutableUiState.update { state ->
                    state.copy(
                        measurements = snapshot.measurements.mapNotNull { record ->
                            runCatching { RingMetricType.valueOf(record.metricType) }
                                .getOrNull()
                                ?.let { it to record }
                        }.toMap(),
                        sleep = snapshot.sleep,
                        activity = snapshot.activity,
                        signals = snapshot.signals.mapNotNull { record ->
                            runCatching { RingMetricType.valueOf(record.signalType) }
                                .getOrNull()
                                ?.let { it to record }
                        }.toMap(),
                    )
                }
            }
        }
        viewModelScope.launch {
            dao.observeSignalChunks(RingMetricType.ECG.name, ECG_HISTORY_LIMIT).collect { records ->
                mutableUiState.update { it.copy(ecgHistory = records) }
            }
        }
        (repository as? RingEcgRepository)?.let { ecgRepository ->
            viewModelScope.launch {
                ecgRepository.liveEcg.collect { live ->
                    mutableUiState.update { it.copy(liveEcg = live) }
                }
            }
        }
    }

    fun startAutoCollection() {
        if (autoCollectionJob?.isActive == true) return
        autoCollectionJob = viewModelScope.launch {
            delay(3_000)
            while (true) {
                runAutoCollectionCycle()
                delay(AUTO_COLLECTION_INTERVAL_MS)
            }
        }
    }

    fun stopAutoCollection() {
        autoCollectionJob?.cancel()
        autoCollectionJob = null
    }

    fun startBackgroundCollection(context: Context) {
        RingForegroundService.start(context.applicationContext)
    }

    fun stopBackgroundCollection(context: Context) {
        RingForegroundService.stop(context.applicationContext)
    }

    fun switchWearableProduct(context: Context, productCode: String) {
        val manager = wearableManager ?: return
        if (!allowWearableProductSwitch || productCode == mutableUiState.value.activeProductCode) return
        if (mutableUiState.value.isSyncing || mutableUiState.value.isScanning) return
        val appContext = context.applicationContext
        val resumeBackground = RingBackgroundCollectionSettings.isActive(appContext)
        val resumeAuto = autoCollectionJob?.isActive == true
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
            if (resumeBackground) RingForegroundService.start(appContext)
            if (resumeAuto) startAutoCollection()
        }
    }

    private suspend fun runAutoCollectionCycle() {
        if (mutableUiState.value.isSyncing) return
        Log.i(TAG, "auto collection cycle start")
        mutableUiState.update {
            it.copy(isSyncing = true, measuringMetric = null, syncProgress = 12, message = "正在自动采集戒指数据")
        }
        val totalRecords = runCatching {
            var records = repository.syncAll().recordsWritten
            listOf(
                RingMetricType.HEART_RATE,
                RingMetricType.BLOOD_OXYGEN,
                RingMetricType.BLOOD_PRESSURE,
                RingMetricType.TEMPERATURE,
            ).forEachIndexed { index, type ->
                mutableUiState.update { state ->
                    state.copy(
                        measuringMetric = type,
                        syncProgress = 30 + index * 15,
                        message = "正在自动采集${type.displayName()}",
                    )
                }
                records += repository.measure(type).recordsWritten
                delay(800)
            }
            records
        }
        totalRecords
            .onSuccess { records ->
                val now = System.currentTimeMillis()
                val upload = if (records > 0) {
                    uploadLatestSnapshot(now, "auto_collection")
                } else {
                    CloudUploadUiStatus("暂无新增数据，不创建上传批次")
                }
                mutableUiState.update {
                    it.copy(
                        isSyncing = false,
                        measuringMetric = null,
                        syncProgress = 100,
                        lastSyncAt = now,
                        cloudSnapshotId = upload.batchId ?: it.cloudSnapshotId,
                        message = if (records > 0) {
                            "自动采集完成，已保存 $records 条数据，${upload.message}"
                        } else {
                            "自动采集完成，暂无新数据"
                        },
                    )
                }
                Log.i(TAG, "auto collection cycle done records=$records")
            }
            .onFailure { error ->
                mutableUiState.update {
                    it.copy(
                        isSyncing = false,
                        measuringMetric = null,
                        syncProgress = 0,
                        message = error.message ?: "自动采集失败",
                    )
                }
                Log.w(TAG, "auto collection cycle failed", error)
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
                    val binding = cloudRepository?.bindDevice(device)
                    mutableUiState.update {
                        it.copy(
                            message = if (binding == null || binding.isSuccess) {
                                "设备已连接"
                            } else {
                                "设备已连接，云端绑定失败，可稍后重新连接重试"
                            },
                        )
                    }
                }
                .onFailure { error ->
                    mutableUiState.update { it.copy(message = error.message ?: "连接失败") }
                }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            repository.disconnect()
            mutableUiState.update { it.copy(message = "设备已断开") }
        }
    }

    fun syncAll() {
        viewModelScope.launch {
            mutableUiState.update {
                it.copy(isSyncing = true, syncProgress = 8, message = "正在读取戒指数据")
            }
            val progressJob = launch {
                listOf(24, 42, 61, 78, 92).forEach { progress ->
                    delay(220)
                    mutableUiState.update { it.copy(syncProgress = progress) }
                }
            }
            runCatching { repository.syncAll() }
                .onSuccess { result ->
                    progressJob.cancel()
                    val uploadMessage = if (result.recordsWritten > 0) {
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
                                "${result.recordsWritten} 条戒指数据已保存到本机，${uploadMessage?.message ?: "云端未上传"}"
                            } else {
                                "未读取到戒指数据，请确认戒指仍保持连接"
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
        patientRefreshJob?.cancel()
        patientRefreshJob = viewModelScope.launch {
            refreshPatientMvp(silent = false)
        }
    }

    fun clearPatientSession() {
        patientRefreshJob?.cancel()
        patientRefreshJob = null
        pushProfileToRepository(repository, null)
        mutableUiState.update {
            it.copy(
                patientMvp = null,
                isPatientMvpLoading = false,
                cloudRiskLevel = null,
                cloudRiskScore = null,
                cloudRiskMode = null,
                cloudRiskSummary = null,
                message = null,
            )
        }
    }

    fun measure(type: RingMetricType) {
        viewModelScope.launch {
            Log.i(TAG, "measure clicked type=$type")
            val action = if (type == RingMetricType.MET) "获取" else "测量"
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
        val measurements = dao.getMeasurementsSince(since)
        val activities = dao.getActivitiesSince(since)
        val sleep = dao.getSleepSessionsSince(since)

        fun avg(metricType: String): Double? {
            val values = measurements.filter { it.metricType == metricType }.map { it.primaryValue }
            return if (values.isEmpty()) null else values.average()
        }
        fun avgSecondary(metricType: String): Double? {
            val values = measurements.filter { it.metricType == metricType }.mapNotNull { it.secondaryValue }
            return if (values.isEmpty()) null else values.average()
        }
        val totalSteps = activities.sumOf { it.steps.toLong() }
        val daysWithSteps = activities.map { it.startedAt / DAY_MS }.distinct().size
        val avgSleep = if (sleep.isEmpty()) null else sleep.map { (it.endedAt - it.startedAt) / 60_000.0 }.average()
        val daysWithMeasurements = measurements.map { it.measuredAt / DAY_MS }.distinct().size

        return PeriodAggregate(
            windowDays = windowDays,
            avgHeartRate = avg(RingMetricType.HEART_RATE.name),
            avgSpo2 = avg(RingMetricType.BLOOD_OXYGEN.name),
            avgSbp = avg(RingMetricType.BLOOD_PRESSURE.name),
            avgDbp = avgSecondary(RingMetricType.BLOOD_PRESSURE.name),
            avgTemp = avg(RingMetricType.TEMPERATURE.name),
            totalSteps = totalSteps,
            avgDailySteps = if (daysWithSteps > 0) totalSteps.toDouble() / daysWithSteps else null,
            avgSleepMinutes = avgSleep,
            daysWithData = daysWithMeasurements,
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
        dao.loadRingHealthHistory(limitPerType)

    private data class RingDatabaseSnapshot(
        val measurements: List<RingMeasurementEntity>,
        val sleep: RingSleepSessionEntity?,
        val activity: RingActivityEntity?,
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
                        patientMvp = it.patientMvp ?: buildFallbackPatientMvp(lastRingVector),
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
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            RingViewModel(
                repository,
                dao,
                cloudRepository,
                wearableManager,
                allowWearableProductSwitch,
            ) as T
    }
}

private data class CloudUploadUiStatus(
    val message: String,
    val batchId: String? = null,
)

private const val TAG = "RingViewModel"
private const val AUTO_COLLECTION_INTERVAL_MS = 15 * 60 * 1000L
private const val ECG_HISTORY_LIMIT = 10
private const val DAY_MS = 24L * 60 * 60 * 1000

private fun periodStartMillis(windowDays: Int): Long {
    val now = System.currentTimeMillis()
    if (windowDays <= 0) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
    return now - windowDays * DAY_MS
}

/**
 * Computed offline fallback for the patient MVP. Used only when the backend is
 * unavailable and no prior real [PatientMvpPayload] exists. Every field is derived from
 * the available local feature vector (or a neutral baseline) via [CvdRiskHeuristic] —
 * no canned persona (no hardcoded name/age). This satisfies Requirement C: the fallback
 * is simulated data computed from real local inputs, never arbitrary placeholders.
 */
private fun buildFallbackPatientMvp(vector: CvdFeatureVector = CvdFeatureVector()): PatientMvpPayload {
    val score = CvdRiskHeuristic.score(vector)
    val level = CvdRiskHeuristic.level(score)
    val profile = PatientProfilePayload(
        patientId = "local-computed",
        name = null,
        gender = if (vector.gender == 1) "male" else if (vector.gender == 0) "female" else null,
        age = vector.age,
        heightCm = null,
        weightKg = null,
        bmi = vector.bmi,
        diagnoses = emptyList(),
        medications = emptyList(),
        allergies = emptyList(),
        familyHistory = vector.familyHistory == 1,
        smoking = vector.smoking == 1,
        drinking = vector.drinking == 1,
        diabetesHistory = vector.diabetesHistory == 1,
        hypertensionHistory = vector.hypertensionHistory == 1,
        updatedAt = System.currentTimeMillis(),
    )
    val risk = PatientRiskPayload(
        mode = "local_heuristic",
        modelVersion = "rehealth-local-heuristic-0.1",
        riskScore = score,
        riskLevel = level,
        summary = CvdRiskHeuristic.summary(score, vector),
        generatedAt = null,
    )
    return PatientMvpPayload(
        profile = profile,
        risk = risk,
        interventionPlan = buildFallbackInterventions(vector, level),
        recentCheckins = emptyList(),
        updatedAt = System.currentTimeMillis(),
    )
}

private fun buildFallbackInterventions(
    vector: CvdFeatureVector,
    level: String,
): List<PatientInterventionPayload> {
    val list = mutableListOf<PatientInterventionPayload>()
    if (vector.sbp != null || vector.dbp != null) {
        list += PatientInterventionPayload(
            id = "bp_monitor",
            title = "规律监测血压",
            goal = "观察血压趋势",
            action = "早晚各测 1 次血压",
            duration = "3 天",
            reason = "当前风险分提示需要关注血压波动",
            status = "active",
        )
    }
    if (vector.exerciseDays == null || vector.exerciseDays < 4) {
        list += PatientInterventionPayload(
            id = "walking_zone2",
            title = "餐后轻运动",
            goal = "降低餐后代谢压力",
            action = "晚餐后步行 15-20 分钟",
            duration = "2 周",
            reason = "低强度活动有助于血糖、血脂和压力管理",
            status = "active",
        )
    }
    list += PatientInterventionPayload(
        id = "sleep_baseline",
        title = "睡眠节律打卡",
        goal = "连续记录睡眠恢复情况",
        action = "起床后记录精神状态",
        duration = "7 天",
        reason = "稳定睡眠有助于血压和心率恢复",
        status = "active",
    )
    return list
}

private fun vectorFromMeasurements(measurements: List<RingMeasurementEntity>): CvdFeatureVector {
    val bp = measurements.firstOrNull { runCatching { RingMetricType.valueOf(it.metricType) }.getOrNull() == RingMetricType.BLOOD_PRESSURE }
    return CvdFeatureVector(
        sbp = bp?.primaryValue,
        dbp = bp?.secondaryValue,
        fastingGlucose = null,
        totalCholesterol = null,
        ldl = null,
        hdl = null,
        triglycerides = null,
        exerciseDays = null,
        smoking = null,
        drinking = null,
        diabetesHistory = null,
        hypertensionHistory = null,
        familyHistory = null,
        age = null,
        gender = null,
        bmi = null,
    )
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
