package com.rehealth.genie.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rehealth.genie.ReHealthApplication
import com.rehealth.genie.network.ApiResult
import com.rehealth.genie.network.dto.InsuranceMobileBindablePolicyDto
import com.rehealth.genie.network.dto.InsurancePlanBindRequestDto
import com.rehealth.genie.network.dto.InsurancePlanBindingDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/** 保险计划绑定卡片状态：零输入一键绑定。 */
data class InsurancePlanBindingUiState(
    val bindings: List<InsurancePlanBindingDto> = emptyList(),
    val candidates: List<InsuranceMobileBindablePolicyDto> = emptyList(),
    val loading: Boolean = false,
    val agreed: Boolean = false,
    val binding: Boolean = false,
    val message: String? = null,
)

/** 读取当前用户的保险计划绑定状态与可绑定保单，并执行一键授权绑定。 */
class InsurancePlanBindingViewModel(context: Context) : ViewModel() {
    private val app = context.applicationContext as ReHealthApplication
    private val _uiState = MutableStateFlow(InsurancePlanBindingUiState())
    val uiState: StateFlow<InsurancePlanBindingUiState> = _uiState.asStateFlow()
    private var loadedUserId: String? = null

    fun loadForCurrentUser() {
        val userId = app.sessionStore.userId ?: return
        if (userId == loadedUserId) return
        loadedUserId = userId
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, message = null)
            val plans = when (val result = app.authenticatedApiClient.getActiveInsurancePlans()) {
                is ApiResult.Success -> result.data
                else -> emptyList()
            }
            val candidates = when (val result = app.authenticatedApiClient.getBindableInsurancePolicies()) {
                is ApiResult.Success -> result.data
                else -> emptyList()
            }
            _uiState.value = _uiState.value.copy(
                loading = false,
                bindings = plans,
                candidates = candidates,
                message = if (plans.isEmpty() && candidates.isEmpty()) "未发现可绑定的保单，请联系您的保险机构" else null,
            )
        }
    }

    fun setAgreed(agreed: Boolean) {
        _uiState.value = _uiState.value.copy(agreed = agreed)
    }

    /** 一键绑定：授权版本固定 v1.0，勾选同意后点选候选保单即完成。 */
    fun bindSelected(candidate: InsuranceMobileBindablePolicyDto) {
        if (_uiState.value.binding) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(binding = true, message = null)
            val request = InsurancePlanBindRequestDto(
                tenantId = candidate.tenantId.toString(),
                policyNo = candidate.policyNo,
                planId = null,
                consentVersion = "v1.0",
                evidenceRef = "app_consent_checkbox",
                sourceRecordId = "app-" + UUID.randomUUID(),
            )
            when (val result = app.authenticatedApiClient.bindInsurancePlan(request)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(binding = false, message = "已同意并加入健康管理计划")
                    refresh()
                }
                else -> _uiState.value = _uiState.value.copy(binding = false, message = "绑定失败，请稍后重试")
            }
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            InsurancePlanBindingViewModel(context) as T
    }
}
