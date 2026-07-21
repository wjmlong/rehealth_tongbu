package com.rehealth.genie.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rehealth.genie.ReHealthApplication
import com.rehealth.genie.network.ApiResult
import com.rehealth.genie.work.MeasurementSyncWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false,
)

/**
 * D3 login ViewModel. Performs a real JeecgBoot login via [AuthenticatedApiClient.mobileLogin]
 * and, on success, drives the D3 auth lifecycle:
 *  - persists the token + user info to [com.rehealth.genie.network.SessionStore]
 *  - notifies [com.rehealth.genie.network.AuthenticatedApiClient] so its auth interceptor
 *    picks up the new token
 *  - resumes the upload queue and schedules/triggers [MeasurementSyncWorker]
 */
class LoginViewModel(private val context: Context) : ViewModel() {
    private val app = context.applicationContext as ReHealthApplication
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState(isLoading = true)
            when (val result = app.authenticatedApiClient.mobileLogin(username, password)) {
                is ApiResult.Success -> {
                    val response = result.data
                    val token = response.token
                    if (token.isNullOrBlank()) {
                        _uiState.value = LoginUiState(errorMessage = "登录成功但未返回 token，请重试")
                        return@launch
                    }
                    // D3: persist token + user info
                    app.sessionStore.token = token
                    app.sessionStore.userId = response.userInfo?.id
                    app.sessionStore.username = response.userInfo?.username
                    // D3: notify auth client (rebuilds authorized API client)
                    app.authenticatedApiClient.onLoginSuccess(token)
                    // D3: resume queue + schedule/trigger worker
                    app.syncRepository.resumeQueue()
                    MeasurementSyncWorker.schedule(context)
                    MeasurementSyncWorker.triggerImmediate(context)
                    _uiState.value = LoginUiState(isLoggedIn = true)
                }
                is ApiResult.NetworkError -> {
                    _uiState.value = LoginUiState(errorMessage = "网络错误: ${result.message}")
                }
                is ApiResult.InvalidRequest -> {
                    _uiState.value = LoginUiState(errorMessage = "登录失败: ${result.message}")
                }
                is ApiResult.InvalidResponse -> {
                    _uiState.value = LoginUiState(errorMessage = "响应格式错误，请重试")
                }
                else -> {
                    _uiState.value = LoginUiState(errorMessage = "登录失败，请重试")
                }
            }
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LoginViewModel(context) as T
        }
    }
}
