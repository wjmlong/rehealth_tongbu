package com.rehealth.genie.ring.device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rehealth.genie.ring.RingRepository
import com.rehealth.genie.ring.mrd.MrdProtocolAdapter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 设备设置ViewModel
 * 管理所有设备设置相关的状态和命令
 */
class DeviceSettingsViewModel(
    private val repository: RingRepository,
    private val protocolAdapter: MrdProtocolAdapter
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceSettingsUiState())
    val uiState: StateFlow<DeviceSettingsUiState> = _uiState.asStateFlow()

    // ==================== 提醒设置 ====================

    fun setAlarmClock(alarms: List<AlarmClock>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = "正在设置闹钟...") }
            runCatching {
                val mrdClocks = alarms.map { it.toMrdEventClock() }
                val commands = protocolAdapter.setAlarmClock(mrdClocks)
                commands.forEach { command ->
                    repository.sendCommand(command)
                }
            }.onSuccess {
                _uiState.update { it.copy(isLoading = false, message = "闹钟设置成功") }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, message = "闹钟设置失败: ${error.message}") }
            }
        }
    }

    fun setSedentaryReminder(reminder: SedentaryReminder) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = "正在设置久坐提醒...") }
            runCatching {
                val command = protocolAdapter.setSedentaryReminder(reminder.toMrdSedentary())
                repository.sendCommand(command)
            }.onSuccess {
                _uiState.update { it.copy(isLoading = false, message = "久坐提醒设置成功") }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, message = "设置失败: ${error.message}") }
            }
        }
    }

    fun setDrinkReminder(reminder: DrinkReminder) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = "正在设置喝水提醒...") }
            runCatching {
                val command = protocolAdapter.setDrinkReminder(reminder.toDrinkReminderBean())
                repository.sendCommand(command)
            }.onSuccess {
                _uiState.update { it.copy(isLoading = false, message = "喝水提醒设置成功") }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, message = "设置失败: ${error.message}") }
            }
        }
    }

    fun setDoNotDisturb(dnd: DoNotDisturb) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = "正在设置勿扰模式...") }
            runCatching {
                val command = protocolAdapter.setDoNotDisturb(dnd.toMrdNotDisturb())
                repository.sendCommand(command)
            }.onSuccess {
                _uiState.update { it.copy(isLoading = false, message = "勿扰模式设置成功") }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, message = "设置失败: ${error.message}") }
            }
        }
    }

    // ==================== 测量设置 ====================

    fun setTimingHrTest(enabled: Boolean, intervalMinutes: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = "正在设置心率监测...") }
            runCatching {
                val command = protocolAdapter.setTimingHrTest(enabled, intervalMinutes)
                repository.sendCommand(command)
            }.onSuccess {
                _uiState.update { it.copy(isLoading = false, message = "心率监测设置成功") }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, message = "设置失败: ${error.message}") }
            }
        }
    }

    fun setHealthAlert(alert: HealthAlert) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = "正在设置健康预警...") }
            runCatching {
                val command = protocolAdapter.setHealthAlert(alert.toMrdHeartBloodAlert())
                repository.sendCommand(command)
            }.onSuccess {
                _uiState.update { it.copy(isLoading = false, message = "健康预警设置成功") }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, message = "设置失败: ${error.message}") }
            }
        }
    }

    // ==================== 用户信息 ====================

    fun setUserInfo(userInfo: UserInfo) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = "正在设置用户信息...") }
            runCatching {
                val command = protocolAdapter.setUserInfo(userInfo.toMrdUserInfo())
                repository.sendCommand(command)
            }.onSuccess {
                _uiState.update { it.copy(isLoading = false, message = "用户信息设置成功") }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, message = "设置失败: ${error.message}") }
            }
        }
    }

    fun setSportTarget(target: SportTarget) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = "正在设置运动目标...") }
            runCatching {
                val command = protocolAdapter.setSportTarget(target.stepTarget, target.calorieTarget)
                repository.sendCommand(command)
            }.onSuccess {
                _uiState.update { it.copy(isLoading = false, message = "运动目标设置成功") }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, message = "设置失败: ${error.message}") }
            }
        }
    }

    fun setUnitSettings(settings: UnitSettings) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = "正在设置单位...") }
            runCatching {
                val unitCommand = protocolAdapter.setUnit(settings.lengthUnit, settings.weightUnit)
                repository.sendCommand(unitCommand)
                val hourCommand = protocolAdapter.setHourFormat(settings.is24Hour)
                repository.sendCommand(hourCommand)
            }.onSuccess {
                _uiState.update { it.copy(isLoading = false, message = "单位设置成功") }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, message = "设置失败: ${error.message}") }
            }
        }
    }

    // ==================== 设备功能 ====================

    fun findDevice(durationSeconds: Int = 3) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = "正在呼叫戒指...") }
            runCatching {
                val command = protocolAdapter.findDevice(durationSeconds)
                repository.sendCommand(command)
            }.onSuccess {
                _uiState.update { it.copy(isLoading = false, message = "戒指已收到呼叫") }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, message = "呼叫失败: ${error.message}") }
            }
        }
    }

    fun setWristOn(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = "正在设置抬腕亮屏...") }
            runCatching {
                val command = protocolAdapter.setWristOn(enabled)
                repository.sendCommand(command)
            }.onSuccess {
                _uiState.update { it.copy(isLoading = false, message = "抬腕亮屏设置成功") }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, message = "设置失败: ${error.message}") }
            }
        }
    }

    fun setScreenLightTime(seconds: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = "正在设置亮屏时长...") }
            runCatching {
                val command = protocolAdapter.setScreenLightTime(seconds)
                repository.sendCommand(command)
            }.onSuccess {
                _uiState.update { it.copy(isLoading = false, message = "亮屏时长设置成功") }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, message = "设置失败: ${error.message}") }
            }
        }
    }

    fun setLostAlert(enabled: Boolean, distance: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = "正在设置防丢提醒...") }
            runCatching {
                val command = protocolAdapter.setLostAlert(enabled, distance)
                repository.sendCommand(command)
            }.onSuccess {
                _uiState.update { it.copy(isLoading = false, message = "防丢提醒设置成功") }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, message = "设置失败: ${error.message}") }
            }
        }
    }

    // ==================== 系统信息 ====================

    fun getBatteryLevel() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = "正在获取电池信息...") }
            runCatching {
                val command = protocolAdapter.getBatteryLevel()
                repository.sendCommand(command)
            }.onSuccess {
                _uiState.update { it.copy(isLoading = false, message = "电池信息请求已发送") }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, message = "获取失败: ${error.message}") }
            }
        }
    }

    fun getFirmwareVersion() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = "正在获取固件版本...") }
            runCatching {
                val command = protocolAdapter.getFirmwareVersion()
                repository.sendCommand(command)
            }.onSuccess {
                _uiState.update { it.copy(isLoading = false, message = "固件版本请求已发送") }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, message = "获取失败: ${error.message}") }
            }
        }
    }

    fun syncTime() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = "正在同步时间...") }
            runCatching {
                val command = protocolAdapter.syncTime()
                repository.sendCommand(command)
            }.onSuccess {
                _uiState.update { it.copy(isLoading = false, message = "时间同步成功") }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, message = "同步失败: ${error.message}") }
            }
        }
    }

    fun factoryReset() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = "正在恢复出厂设置...") }
            runCatching {
                val command = protocolAdapter.factoryReset()
                repository.sendCommand(command)
            }.onSuccess {
                _uiState.update { it.copy(isLoading = false, message = "恢复出厂设置命令已发送") }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, message = "操作失败: ${error.message}") }
            }
        }
    }

    class Factory(
        private val repository: RingRepository,
        private val protocolAdapter: MrdProtocolAdapter
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DeviceSettingsViewModel(repository, protocolAdapter) as T
    }
}

data class DeviceSettingsUiState(
    val isLoading: Boolean = false,
    val message: String? = null
)
