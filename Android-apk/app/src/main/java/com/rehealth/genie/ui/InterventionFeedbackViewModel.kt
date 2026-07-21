package com.rehealth.genie.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rehealth.genie.ReHealthApplication
import com.rehealth.genie.work.MeasurementSyncWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FeedbackUiState(
    val isSubmitting: Boolean = false,
    val message: String? = null,
    val lastSubmittedId: String? = null,
)

/**
 * D3 intervention feedback ViewModel. Replaces the legacy `RingViewModel.submitCheckIn`
 * with typed feedback (completed / not_applicable / skipped / partially_completed):
 *  - persists feedback locally first via [com.rehealth.genie.data.sync.InterventionFeedbackRepository]
 *  - never fails locally (the repository queues it)
 *  - triggers an immediate [MeasurementSyncWorker] run to upload pending feedback
 */
class InterventionFeedbackViewModel(private val context: Context) : ViewModel() {
    private val app = context.applicationContext as ReHealthApplication
    private val feedbackRepo = app.interventionFeedbackRepository
    private val _uiState = MutableStateFlow(FeedbackUiState())
    val uiState: StateFlow<FeedbackUiState> = _uiState.asStateFlow()

    fun submitFeedback(interventionId: String, status: String, note: String? = null) {
        viewModelScope.launch {
            _uiState.value = FeedbackUiState(isSubmitting = true)
            try {
                val feedbackId = feedbackRepo.submitFeedback(
                    interventionId = interventionId,
                    status = status,
                    note = note,
                )
                // D3: trigger immediate upload of queued feedback
                MeasurementSyncWorker.triggerImmediate(context)
                _uiState.value = FeedbackUiState(
                    message = getSuccessMessage(status),
                    lastSubmittedId = feedbackId,
                )
            } catch (e: Exception) {
                _uiState.value = FeedbackUiState(message = "反馈保存失败: ${e.message}")
            }
        }
    }

    private fun getSuccessMessage(status: String): String {
        return when (status) {
            "completed" -> "已完成反馈，感谢您的坚持！"
            "partially_completed" -> "部分完成反馈已记录"
            "skipped" -> "已标记为稍后完成"
            "not_applicable" -> "已标记为不适用"
            else -> "反馈已记录"
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return InterventionFeedbackViewModel(context) as T
        }
    }
}
