package com.rehealth.genie.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rehealth.genie.ReHealthApplication
import com.rehealth.genie.rhi.RhiPeriodSummary
import com.rehealth.genie.rhi.RhiCalculationSource
import com.rehealth.genie.network.PatientProfilePayload
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RhiViewModel(
    private val application: ReHealthApplication,
) : ViewModel() {
    private val _periodSummary = MutableStateFlow<RhiPeriodSummary?>(null)
    val periodSummary: StateFlow<RhiPeriodSummary?> = _periodSummary.asStateFlow()
    private val _refreshError = MutableStateFlow<String?>(null)
    val refreshError: StateFlow<String?> = _refreshError.asStateFlow()
    private val preferences = application.getSharedPreferences("rhi_calculation", Context.MODE_PRIVATE)
    private val _calculationSource = MutableStateFlow(
        preferences.getString("source", null)
            ?.let { runCatching { RhiCalculationSource.valueOf(it) }.getOrNull() }
            ?: RhiCalculationSource.LOCAL,
    )
    val calculationSource: StateFlow<RhiCalculationSource> = _calculationSource.asStateFlow()
    private var refreshJob: Job? = null

    fun setCalculationSource(source: RhiCalculationSource) {
        if (_calculationSource.value == source) return
        _calculationSource.value = source
        preferences.edit().putString("source", source.name).apply()
    }

    fun refresh(periodDays: Int = 7, profile: PatientProfilePayload? = null) {
        refreshJob?.cancel()
        _refreshError.value = null
        refreshJob = viewModelScope.launch {
            try {
                _periodSummary.value = application.rhiRepository.refreshPeriod(
                    periodDays,
                    profile = profile,
                    calculationSource = _calculationSource.value,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _refreshError.value = if (_calculationSource.value == RhiCalculationSource.REMOTE) {
                    "远程 RHI 复算失败：${error.message ?: "服务暂不可用"}"
                } else {
                    "健康改善得分暂时无法更新，请稍后重试"
                }
            }
        }
    }

    class Factory(context: Context) : ViewModelProvider.Factory {
        private val application = context.applicationContext as ReHealthApplication

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            RhiViewModel(application) as T
    }
}
