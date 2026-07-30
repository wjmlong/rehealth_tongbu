package com.rehealth.genie.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rehealth.genie.ReHealthApplication
import com.rehealth.genie.data.HealthChatMessageEntity
import com.rehealth.genie.network.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi

data class HealthChatUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HealthChatViewModel(
    private val application: ReHealthApplication,
) : ViewModel() {
    private val userId = MutableStateFlow(application.sessionStore.userId)
    private val mutableUiState = MutableStateFlow(HealthChatUiState())
    val uiState: StateFlow<HealthChatUiState> = mutableUiState.asStateFlow()
    val messages: StateFlow<List<HealthChatMessageEntity>> = userId
        .flatMapLatest { currentUser ->
            if (currentUser.isNullOrBlank()) flowOf(emptyList())
            else application.healthChatRepository.observeLatestConversation(currentUser)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        refresh()
    }

    fun refresh() {
        userId.value = application.sessionStore.userId
        if (userId.value.isNullOrBlank()) {
            mutableUiState.value = HealthChatUiState(errorMessage = "请重新登录后查看健康问答")
            return
        }
        viewModelScope.launch {
            when (val result = application.healthChatRepository.refreshLatest()) {
                is ApiResult.Success -> Unit
                is ApiResult.Unauthorized -> mutableUiState.value = HealthChatUiState(errorMessage = result.message)
                else -> Unit // Local Room history remains available while offline.
            }
        }
    }

    fun send(text: String) {
        if (text.isBlank() || mutableUiState.value.isLoading) return
        viewModelScope.launch {
            mutableUiState.value = HealthChatUiState(isLoading = true)
            val result = application.healthChatRepository.send(text)
            mutableUiState.value = when (result) {
                is ApiResult.Success -> HealthChatUiState()
                is ApiResult.Unauthorized -> HealthChatUiState(errorMessage = result.message)
                is ApiResult.Forbidden -> HealthChatUiState(errorMessage = result.message)
                is ApiResult.InvalidRequest -> HealthChatUiState(errorMessage = result.message)
                is ApiResult.InvalidResponse -> HealthChatUiState(errorMessage = "健康问答响应异常，请稍后重试")
                is ApiResult.ServiceUnavailable -> HealthChatUiState(errorMessage = "健康问答暂时不可用，请稍后重试")
                is ApiResult.NetworkError -> HealthChatUiState(errorMessage = "网络连接失败，问题已保存在本机")
            }
        }
    }

    fun clearError() {
        mutableUiState.value = mutableUiState.value.copy(errorMessage = null)
    }

    class Factory(
        private val application: ReHealthApplication,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HealthChatViewModel(application) as T
    }
}
