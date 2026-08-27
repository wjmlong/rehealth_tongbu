package com.rehealth.genie.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rehealth.genie.ReHealthApplication
import com.rehealth.genie.network.ApiResult
import com.rehealth.genie.network.dto.InsuranceScanConfirmRequestDto
import com.rehealth.genie.network.dto.InsuranceScanPreviewDto
import com.rehealth.genie.network.dto.InsuranceScanRequestDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 扫码关联流程状态：输入员工码 → 预览员工 → 确认建立/更换服务关系。 */
data class ScanLinkUiState(
    val phase: Phase = Phase.INPUT,
    val codeInput: String = "",
    val preview: InsuranceScanPreviewDto? = null,
    val busy: Boolean = false,
    val message: String? = null,
    val done: Boolean = false,
) {
    enum class Phase { INPUT, PREVIEW, DONE }
}

/** 扫码关联（手动输码 + 确认），相机扫码留待后续版本。 */
class ScanLinkViewModel(context: Context) : ViewModel() {
    private val app = context.applicationContext as ReHealthApplication
    private val _uiState = MutableStateFlow(ScanLinkUiState())
    val uiState: StateFlow<ScanLinkUiState> = _uiState.asStateFlow()

    fun updateCode(value: String) {
        val normalized = value.trim().uppercase().take(16)
        _uiState.value = _uiState.value.copy(codeInput = normalized)
    }

    fun scan() {
        val code = _uiState.value.codeInput
        if (code.length < 4) {
            _uiState.value = _uiState.value.copy(message = "请输入 8 位员工码")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busy = true, message = null)
            when (val result = app.authenticatedApiClient.scanInsuranceAssignment(
                InsuranceScanRequestDto(employeeCode = code),
            )) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(
                    busy = false,
                    phase = ScanLinkUiState.Phase.PREVIEW,
                    preview = result.data,
                )
                else -> _uiState.value = _uiState.value.copy(
                    busy = false,
                    message = failMessage(result, "扫码失败，请检查员工码是否有效"),
                )
            }
        }
    }

    fun confirm() {
        val preview = _uiState.value.preview ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busy = true, message = null)
            when (val result = app.authenticatedApiClient.confirmInsuranceScan(
                preview.sessionId,
                InsuranceScanConfirmRequestDto(replaceExisting = true),
            )) {
                is ApiResult.Success -> _uiState.value = ScanLinkUiState(
                    phase = ScanLinkUiState.Phase.DONE,
                    done = true,
                    message = if (result.data.created) "已建立服务关系：${result.data.employeeName ?: "服务专员"}"
                    else if (result.data.alreadyServed) "该服务专员已在为您服务"
                    else "服务关系已更新",
                )
                else -> _uiState.value = _uiState.value.copy(
                    busy = false,
                    message = failMessage(result, "确认失败，请重试"),
                )
            }
        }
    }

    fun backToInput() {
        _uiState.value = ScanLinkUiState()
    }

    private fun messageOf(result: ApiResult<*>): String? = when (result) {
        is ApiResult.Unauthorized -> result.message
        is ApiResult.Forbidden -> result.message
        is ApiResult.InvalidRequest -> result.message
        is ApiResult.InvalidResponse -> result.message
        is ApiResult.ServiceUnavailable -> result.message
        is ApiResult.NetworkError -> result.message
        else -> null
    }

    private fun failMessage(result: ApiResult<*>, fallback: String): String =
        messageOf(result) ?: fallback

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ScanLinkViewModel(context) as T
    }
}
