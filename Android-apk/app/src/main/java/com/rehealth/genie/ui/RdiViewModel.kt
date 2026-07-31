package com.rehealth.genie.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rehealth.genie.ReHealthApplication
import com.rehealth.genie.rdi.RdiDisplayData
import com.rehealth.genie.rdi.RdiPeriodSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RdiViewModel(
    private val application: ReHealthApplication,
) : ViewModel() {
    val display: StateFlow<RdiDisplayData?> = application.rdiRepository.observeLatestDisplay()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    private val _refreshError = MutableStateFlow<String?>(null)
    val refreshError: StateFlow<String?> = _refreshError.asStateFlow()
    private val _periodSummary = MutableStateFlow<RdiPeriodSummary?>(null)
    val periodSummary: StateFlow<RdiPeriodSummary?> = _periodSummary.asStateFlow()
    private var refreshJob: Job? = null

    fun refresh(periodDays: Int = 7) {
        refreshJob?.cancel()
        _refreshError.value = null
        refreshJob = viewModelScope.launch {
            try {
                _periodSummary.value = application.rdiRepository.refreshPeriod(periodDays)
                _refreshError.value = null
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _refreshError.value = "健康改善得分暂时无法更新，请稍后重试"
            }
        }
    }

    class Factory(context: Context) : ViewModelProvider.Factory {
        private val application = context.applicationContext as ReHealthApplication

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            RdiViewModel(application) as T
    }
}
