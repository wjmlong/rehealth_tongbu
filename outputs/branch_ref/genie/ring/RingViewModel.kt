package com.rehealth.genie.ring

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rehealth.genie.ring.SupportedHardwareHealthMetrics
import com.rehealth.genie.ring.data.RingActivityEntity
import com.rehealth.genie.ring.data.RingDataDao
import com.rehealth.genie.ring.data.RingMeasurementEntity
import com.rehealth.genie.ring.data.RingSignalChunkEntity
import com.rehealth.genie.ring.data.RingSleepSessionEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RingUiState(
    val connectionState: RingConnectionState = RingConnectionState.DISCONNECTED,
    val devices: List<RingDevice> = emptyList(),
    val connectedDevice: RingDevice? = null,
    val isScanning: Boolean = false,
    val isSyncing: Boolean = false,
    val measuringMetric: RingMetricType? = null,
    val syncProgress: Int = 0,
    val lastSyncAt: Long? = null,
    val message: String? = null,
    val measurements: Map<RingMetricType, RingMeasurementEntity> = emptyMap(),
    val sleep: RingSleepSessionEntity? = null,
    val activity: RingActivityEntity? = null,
    val signals: Map<RingMetricType, RingSignalChunkEntity> = emptyMap(),
) {
    val collectedMetricCount: Int
        get() = measurements.keys.count { it in SupportedHardwareHealthMetrics && it != RingMetricType.SLEEP } +
            if (sleep != null) 1 else 0
}

class RingViewModel(
    private val repository: RingRepository,
    dao: RingDataDao,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(RingUiState())
    val uiState: StateFlow<RingUiState> = mutableUiState.asStateFlow()
    private var autoCollectionJob: Job? = null

    init {
        viewModelScope.launch {
            repository.connectionState.collect { connectionState ->
                mutableUiState.update { it.copy(connectionState = connectionState) }
            }
        }
        viewModelScope.launch {
            repository.connectedDevice.collect { device ->
                mutableUiState.update { it.copy(connectedDevice = device) }
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
                mutableUiState.update {
                    it.copy(
                        isSyncing = false,
                        measuringMetric = null,
                        syncProgress = 100,
                        lastSyncAt = now,
                        message = if (records > 0) "自动采集完成，已保存 $records 条数据" else "自动采集完成，暂无新数据",
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
                    mutableUiState.update { it.copy(message = "设备已连接") }
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
                    mutableUiState.update {
                        it.copy(
                            isSyncing = false,
                            syncProgress = 100,
                            lastSyncAt = result.completedAt,
                            message = if (result.recordsWritten > 0) {
                                "${result.recordsWritten} 条戒指数据已保存到本机"
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

    fun measure(type: RingMetricType) {
        viewModelScope.launch {
            Log.i(TAG, "measure clicked type=$type")
            mutableUiState.update {
                it.copy(
                    isSyncing = true,
                    measuringMetric = type,
                    syncProgress = 15,
                    message = "请保持戒指佩戴稳定，正在测量${type.displayName()}",
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
                                "${type.displayName()}测量完成，结果已保存"
                            } else {
                                "没有读取到${type.displayName()}结果，请重新测量"
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
                            message = error.message ?: "${type.displayName()}测量失败",
                        )
                    }
                }
        }
    }

    private data class RingDatabaseSnapshot(
        val measurements: List<RingMeasurementEntity>,
        val sleep: RingSleepSessionEntity?,
        val activity: RingActivityEntity?,
        val signals: List<RingSignalChunkEntity>,
    )

    class Factory(
        private val repository: RingRepository,
        private val dao: RingDataDao,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            RingViewModel(repository, dao) as T
    }
}

private const val TAG = "RingViewModel"
private const val AUTO_COLLECTION_INTERVAL_MS = 15 * 60 * 1000L

private fun RingMetricType.displayName(): String = when (this) {
    RingMetricType.HEART_RATE -> "心率"
    RingMetricType.BLOOD_OXYGEN -> "血氧"
    RingMetricType.BLOOD_PRESSURE -> "血压"
    RingMetricType.TEMPERATURE -> "体温"
    RingMetricType.HRV -> "HRV"
    RingMetricType.SLEEP -> "睡眠"
    RingMetricType.STEPS -> "步数"
    RingMetricType.ACTIVITY -> "运动"
    RingMetricType.STRESS -> "压力"
    RingMetricType.RRI -> "RRI"
    RingMetricType.PPG -> "PPG"
}
