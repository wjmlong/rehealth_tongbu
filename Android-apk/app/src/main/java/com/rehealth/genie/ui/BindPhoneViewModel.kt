package com.rehealth.genie.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rehealth.genie.ReHealthApplication
import com.rehealth.genie.network.ApiResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BindPhoneUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    /** 验证码 60s 冷却倒计时（0 表示可发送）。 */
    val countdown: Int = 0,
    val bound: Boolean = false,
    val loggedOut: Boolean = false,
)

/**
 * 微信新建账号的强制绑定手机页 ViewModel。
 *
 * 复用注册短信链路：发送走 /rehealth/mobile/account/bind-phone/sms（需登录），
 * 校验走 /rehealth/mobile/account/bind-phone，绑定成功后写 SessionStore.phone 并放行。
 */
class BindPhoneViewModel(private val context: Context) : ViewModel() {
    private val app = context.applicationContext as ReHealthApplication
    private val _uiState = MutableStateFlow(BindPhoneUiState())
    val uiState: StateFlow<BindPhoneUiState> = _uiState.asStateFlow()
    private var countdownJob: Job? = null

    fun isPhoneValid(phone: String): Boolean =
        phone.length == 11 && phone.all { it.isDigit() }

    fun sendCode(phone: String) {
        if (!isPhoneValid(phone) || _uiState.value.countdown > 0 || _uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.value = BindPhoneUiState(isLoading = true)
            when (val result = app.authenticatedApiClient.bindPhoneSms(phone)) {
                is ApiResult.Success -> {
                    startCountdown()
                    _uiState.value = BindPhoneUiState(countdown = 60)
                }
                is ApiResult.InvalidRequest -> {
                    _uiState.value = BindPhoneUiState(errorMessage = result.message)
                }
                is ApiResult.Unauthorized -> {
                    _uiState.value = BindPhoneUiState(errorMessage = "登录已失效，请重新登录")
                }
                else -> {
                    _uiState.value = BindPhoneUiState(errorMessage = "网络错误，请稍后重试")
                }
            }
        }
    }

    fun bind(phone: String, smsCode: String) {
        if (!isPhoneValid(phone) || smsCode.length != 6) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = app.authenticatedApiClient.bindPhone(phone, smsCode)) {
                is ApiResult.Success -> {
                    app.sessionStore.phone = phone
                    countdownJob?.cancel()
                    _uiState.value = BindPhoneUiState(bound = true)
                }
                is ApiResult.InvalidRequest -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                }
                is ApiResult.Unauthorized -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "登录已失效，请重新登录")
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "网络错误，请稍后重试")
                }
            }
        }
    }

    /** 退出登录：清理登录态，由页面回调导航回登录页。 */
    fun logout() {
        app.authenticatedApiClient.onLogout()
        _uiState.value = BindPhoneUiState(loggedOut = true)
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (remaining in 60 downTo 0) {
                _uiState.value = _uiState.value.copy(countdown = remaining)
                if (remaining > 0) delay(1000)
            }
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BindPhoneViewModel(context) as T
        }
    }
}
