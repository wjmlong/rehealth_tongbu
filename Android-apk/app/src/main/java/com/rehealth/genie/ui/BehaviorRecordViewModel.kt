package com.rehealth.genie.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rehealth.genie.ReHealthApplication
import com.rehealth.genie.network.ApiResult
import com.rehealth.genie.network.dto.BehaviorRecordDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BehaviorRecordUiState(
    val records: List<BehaviorRecordDto> = emptyList(),
    val isLoading: Boolean = false,
    val isUploading: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

class BehaviorRecordViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as ReHealthApplication).behaviorRecordRepository
    private val _state = MutableStateFlow(BehaviorRecordUiState())
    val state: StateFlow<BehaviorRecordUiState> = _state.asStateFlow()

    init {
        refreshToday()
    }

    fun analyzePhoto(uri: Uri) {
        if (_state.value.isUploading) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isUploading = true, message = null, error = null)
            when (val result = repository.analyzeCameraPhoto(uri)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        records = listOf(result.data) + _state.value.records.filterNot { it.id == result.data.id },
                        isUploading = false,
                        message = "已识别并写入今日行为记录",
                    )
                }
                else -> _state.value = _state.value.copy(
                    isUploading = false,
                    error = result.userMessage("照片分析失败，请稍后重试"),
                )
            }
        }
    }

    fun refreshToday() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            when (val result = repository.today()) {
                is ApiResult.Success -> _state.value = _state.value.copy(records = result.data, isLoading = false)
                else -> _state.value = _state.value.copy(
                    isLoading = false,
                    error = result.userMessage("今日行为记录加载失败"),
                )
            }
        }
    }

    fun clearNotice() {
        _state.value = _state.value.copy(message = null, error = null)
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            BehaviorRecordViewModel(application) as T
    }
}

private fun ApiResult<*>.userMessage(fallback: String): String = when (this) {
    is ApiResult.Unauthorized -> "登录已失效，请重新登录"
    is ApiResult.Forbidden -> "当前账号无权保存这条记录"
    is ApiResult.InvalidRequest -> message.ifBlank { fallback }
    is ApiResult.InvalidResponse -> fallback
    is ApiResult.ServiceUnavailable -> "图片分析服务暂时不可用，请稍后重试"
    is ApiResult.NetworkError -> "网络连接失败，请检查网络后重试"
    is ApiResult.Success -> fallback
}
