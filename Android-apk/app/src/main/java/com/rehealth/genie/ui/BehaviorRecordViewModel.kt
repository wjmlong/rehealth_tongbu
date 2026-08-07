package com.rehealth.genie.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rehealth.genie.ReHealthApplication
import com.rehealth.genie.network.ApiResult
import com.rehealth.genie.network.dto.BehaviorRecordDto
import java.io.File
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
    private val app = application as ReHealthApplication
    private val repository = app.behaviorRecordRepository
    private val dietRepository = app.dietRecordRepository
    private val _state = MutableStateFlow(BehaviorRecordUiState())
    val state: StateFlow<BehaviorRecordUiState> = _state.asStateFlow()

    fun analyzePhoto(photoFile: File) {
        if (_state.value.isUploading) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isUploading = true, message = null, error = null)
            when (val result = repository.analyzeCameraPhoto(photoFile)) {
                is ApiResult.Success -> {
                    val records = listOf(result.data) + _state.value.records.filterNot { it.id == result.data.id }
                    if (result.data.category.equals("FOOD", ignoreCase = true)) {
                        runCatching { dietRepository.saveAnalyzedFood(result.data) }
                            .onSuccess { meal ->
                                _state.value = _state.value.copy(
                                    records = records,
                                    isUploading = false,
                                    message = if (meal != null) {
                                        "已识别并加入今日行为和餐食记录"
                                    } else {
                                        "已写入今日行为记录；营养信息不完整，请手动补录餐食"
                                    },
                                )
                            }
                            .onFailure {
                                _state.value = _state.value.copy(
                                    records = records,
                                    isUploading = false,
                                    error = "识别成功，但加入今日餐食记录失败，请手动补录",
                                )
                            }
                    } else {
                        _state.value = _state.value.copy(
                            records = records,
                            isUploading = false,
                            message = "已识别并写入今日行为记录",
                        )
                    }
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
                is ApiResult.Success -> {
                    val mealRestoreError = runCatching {
                        dietRepository.restoreAnalyzedFoods(result.data)
                    }.exceptionOrNull()
                    _state.value = _state.value.copy(
                        records = result.data,
                        isLoading = false,
                        error = mealRestoreError?.let { "今日行为已加载，但餐食记录恢复失败，请稍后重试" },
                    )
                }
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
    is ApiResult.NetworkError -> if (isTimeout) {
        "图片识别超时，请稍后重新拍摄"
    } else {
        "网络连接失败，请检查网络后重试"
    }
    is ApiResult.Success -> fallback
}
