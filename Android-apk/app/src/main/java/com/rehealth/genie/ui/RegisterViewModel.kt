package com.rehealth.genie.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rehealth.genie.ReHealthApplication
import com.rehealth.genie.network.ApiResult
import com.rehealth.genie.work.MeasurementSyncWorker
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RegisterUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isRegistered: Boolean = false,
)

/**
 * D3 registration ViewModel.
 *
 * Flow:
 *  1. [sendSmsCode] -> calls `/sys/sms` (register mode) to deliver a 6-digit code.
 *     Starts a 60s resend countdown on success.
 *  2. [register] -> calls `/sys/user/register` with phone + smscode + password, then
 *     auto-logs-in via `/sys/mLogin` (username = phone, password) so the new user
 *     lands in onboarding without manually logging in again.
 */
class RegisterViewModel(private val context: Context) : ViewModel() {
    private val app = context.applicationContext as ReHealthApplication
    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _codeCountdown = MutableStateFlow(0)
    val codeCountdown: StateFlow<Int> = _codeCountdown.asStateFlow()

    private var countdownJob: Job? = null

    fun isPhoneValid(phone: String): Boolean =
        phone.length == 11 && phone.all { it.isDigit() }

    fun sendSmsCode(phone: String) {
        if (!isPhoneValid(phone) || _codeCountdown.value > 0 || _uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = app.authenticatedApiClient.sendSms(phone)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    startCountdown()
                }
                is ApiResult.InvalidRequest -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "获取验证码失败，请重试")
                }
            }
        }
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            _codeCountdown.value = 60
            while (_codeCountdown.value > 0) {
                delay(1000)
                _codeCountdown.value -= 1
            }
        }
    }

    fun register(phone: String, smscode: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val reg = app.authenticatedApiClient.register(phone, smscode, phone, password)) {
                is ApiResult.Success -> {
                    autoLoginAfterRegister(phone, password)
                }
                is ApiResult.InvalidRequest -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "注册失败: ${reg.message}")
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "注册失败，请重试")
                }
            }
        }
    }

    private fun autoLoginAfterRegister(phone: String, password: String) {
        viewModelScope.launch {
            when (val login = app.authenticatedApiClient.mobileLogin(phone, password)) {
                is ApiResult.Success -> {
                    val resp = login.data
                    val token = resp.token
                    if (token.isNullOrBlank()) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "注册成功，但登录未返回 token，请手动登录",
                        )
                        return@launch
                    }
                    app.sessionStore.token = token
                    app.sessionStore.userId = resp.userInfo?.id
                    app.sessionStore.username = resp.userInfo?.username
                    app.authenticatedApiClient.onLoginSuccess(token)
                    app.syncRepository.resumeQueue()
                    MeasurementSyncWorker.schedule(context)
                    MeasurementSyncWorker.triggerImmediate(context)
                    _uiState.value = RegisterUiState(isRegistered = true)
                }
                is ApiResult.InvalidRequest -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "注册成功，但自动登录失败: ${login.message}，请手动登录",
                    )
                }
                else -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "注册成功，但自动登录失败，请手动登录",
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RegisterViewModel(context) as T
        }
    }
}
