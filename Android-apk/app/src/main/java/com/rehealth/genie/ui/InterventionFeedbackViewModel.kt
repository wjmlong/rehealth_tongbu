package com.rehealth.genie.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rehealth.genie.ReHealthApplication
import com.rehealth.genie.network.ApiResult
import com.rehealth.genie.network.dto.InsurancePlanBindingDto
import com.rehealth.genie.network.dto.InstitutionCarePlanDto
import com.rehealth.genie.network.dto.InstitutionCarePlanItemDto
import com.rehealth.genie.work.MeasurementSyncWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FeedbackUiState(
    val isSubmitting: Boolean = false,
    val isLoadingBindings: Boolean = false,
    val activeBindings: List<InsurancePlanBindingDto> = emptyList(),
    val institutionCarePlans: List<InstitutionCarePlanDto> = emptyList(),
    val isLoadingCarePlans: Boolean = false,
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

    init {
        refreshActiveBindings()
        refreshInstitutionCarePlans()
    }

    fun refreshInstitutionCarePlans() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingCarePlans = true)
            _uiState.value = when (val result = app.authenticatedApiClient.getCurrentInstitutionCarePlans()) {
                is ApiResult.Success -> _uiState.value.copy(
                    isLoadingCarePlans = false,
                    institutionCarePlans = result.data,
                )
                is ApiResult.Unauthorized -> _uiState.value.copy(
                    isLoadingCarePlans = false,
                    institutionCarePlans = emptyList(),
                    message = "登录已失效，机构计划暂不可用",
                )
                else -> _uiState.value.copy(
                    isLoadingCarePlans = false,
                    message = "机构计划读取失败，可稍后重试",
                )
            }
        }
    }

    fun refreshActiveBindings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingBindings = true)
            _uiState.value = when (val result = app.authenticatedApiClient.getActiveInsurancePlans()) {
                is ApiResult.Success -> _uiState.value.copy(
                    isLoadingBindings = false,
                    activeBindings = result.data,
                )
                is ApiResult.Unauthorized -> _uiState.value.copy(
                    isLoadingBindings = false,
                    message = "登录已失效，机构计划暂不可用",
                )
                else -> _uiState.value.copy(
                    isLoadingBindings = false,
                    message = "机构计划读取失败，可稍后重试",
                )
            }
        }
    }

    fun submitFeedback(interventionId: String, status: String, note: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, message = null)
            try {
                val feedbackId = feedbackRepo.submitFeedback(
                    interventionId = interventionId,
                    status = status,
                    note = note,
                )
                // D3: trigger immediate upload of queued feedback
                MeasurementSyncWorker.triggerImmediate(context)
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    message = getSuccessMessage(status),
                    lastSubmittedId = feedbackId,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    message = "反馈保存失败: ${e.message}",
                )
            }
        }
    }

    fun submitInstitutionFeedback(
        binding: InsurancePlanBindingDto,
        planItemId: String,
        status: String,
        note: String? = null,
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, message = null)
            try {
                val completedCount = when (status) {
                    "completed" -> 1.0
                    "partially_completed" -> 0.5
                    "skipped" -> 0.0
                    else -> null
                }
                val feedbackId = feedbackRepo.submitFeedback(
                    interventionId = binding.planId,
                    status = status,
                    note = note,
                    bindingId = binding.bindingId,
                    tenantId = binding.tenantId,
                    planItemId = planItemId,
                    expectedCount = if (status == "not_applicable") null else 1.0,
                    completedCount = completedCount,
                )
                MeasurementSyncWorker.triggerImmediate(context)
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    message = "机构 ${binding.tenantId}：${getSuccessMessage(status)}",
                    lastSubmittedId = feedbackId,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    message = "反馈保存失败: ${e.message}",
                )
            }
        }
    }

    fun submitInstitutionCarePlanFeedback(
        plan: InstitutionCarePlanDto,
        item: InstitutionCarePlanItemDto,
        status: String,
        note: String? = null,
    ) {
        val occurrence = item.todayOccurrence ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, message = null)
            try {
                val completedCount = when (status) {
                    "completed" -> 1.0
                    "partially_completed" -> 0.5
                    "skipped" -> 0.0
                    else -> null
                }
                val feedbackId = feedbackRepo.submitFeedback(
                    interventionId = plan.planId,
                    status = status,
                    note = note,
                    tenantId = plan.tenantId,
                    planItemId = item.itemId,
                    occurrenceId = occurrence.occurrenceId,
                    expectedCount = if (status == "not_applicable") null else 1.0,
                    completedCount = completedCount,
                )
                MeasurementSyncWorker.triggerImmediate(context)
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    institutionCarePlans = _uiState.value.institutionCarePlans.map { existingPlan ->
                        if (existingPlan.planId != plan.planId) existingPlan else existingPlan.copy(
                            items = existingPlan.items.map { existingItem ->
                                if (existingItem.itemId != item.itemId) existingItem else existingItem.copy(
                                    todayOccurrence = existingItem.todayOccurrence?.copy(feedbackType = status),
                                )
                            },
                        )
                    },
                    message = "${plan.organizationName ?: "机构"}：${getSuccessMessage(status)}，正在同步",
                    lastSubmittedId = feedbackId,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    message = "反馈保存失败: ${e.message}",
                )
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
