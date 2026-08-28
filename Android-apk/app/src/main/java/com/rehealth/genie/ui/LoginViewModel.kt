package com.rehealth.genie.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rehealth.genie.ReHealthApplication
import com.rehealth.genie.network.ApiResult
import com.rehealth.genie.network.dto.LoginUserInfo
import com.rehealth.genie.wechat.WechatAuthService
import com.rehealth.genie.wechat.WechatAuthState
import com.rehealth.genie.work.MeasurementSyncWorker
import com.rehealth.genie.work.TelemetryUploadWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false,
    /** 微信登录成功但账号未绑定手机号：导航到强制绑定手机页，不进入主页。 */
    val requiresPhoneBinding: Boolean = false,
    /** 微信授权状态机（拉起授权/取消/失败提示）。 */
    val wechatAuthState: WechatAuthState = WechatAuthState.Idle,
)

/**
 * D3 login ViewModel. Performs a real JeecgBoot login via [com.rehealth.genie.network.AuthenticatedApiClient]
 * (password via `mobileLogin`, WeChat via `wechatLogin`) and, on success, drives the D3 auth lifecycle:
 *  - persists the token + user info to [com.rehealth.genie.network.SessionStore]
 *  - notifies [com.rehealth.genie.network.AuthenticatedApiClient] so its auth interceptor
 *    picks up the new token
 *  - resumes the upload queue and schedules/triggers [MeasurementSyncWorker]
 *
 * 微信登录成功后若 `userInfo.phone` 为空，置 [LoginUiState.requiresPhoneBinding]，由登录页
 * 导航到强制绑定手机页；账号密码登录保持原有行为不变。
 */
class LoginViewModel(private val context: Context) : ViewModel() {
    private val app = context.applicationContext as ReHealthApplication
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        // 微信授权回调驱动：Authorized(code) 直接发起 code 登录；取消静默、失败提示。
        viewModelScope.launch {
            WechatAuthService.authState.collect { state ->
                when (state) {
                    is WechatAuthState.Authorized -> wechatLogin(state.code)
                    is WechatAuthState.NotInstalled ->
                        _uiState.value = _uiState.value.copy(
                            wechatAuthState = state,
                            errorMessage = "未检测到微信客户端",
                        )
                    is WechatAuthState.Failed ->
                        _uiState.value = _uiState.value.copy(
                            wechatAuthState = state,
                            errorMessage = "微信授权失败，请重试",
                        )
                    else -> _uiState.value = _uiState.value.copy(wechatAuthState = state)
                }
            }
        }
    }

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
                    completeLogin(token, response.userInfo, usernameFallback = username)
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

    /** 拉起微信授权（需微信客户端已安装、BuildConfig.WECHAT_APP_ID 已配置）。 */
    fun startWechatAuth() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
        WechatAuthService.startAuth(context)
    }

    /** 用微信 SDK 返回的一次性 code 登录。成功后与账号密码登录共用同一登录后生命周期。 */
    fun wechatLogin(code: String) {
        if (code.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = app.authenticatedApiClient.wechatLogin(code)) {
                is ApiResult.Success -> {
                    val response = result.data
                    val token = response.token
                    if (token.isNullOrBlank()) {
                        _uiState.value = LoginUiState(errorMessage = "登录成功但未返回 token，请重试")
                        return@launch
                    }
                    completeLogin(token, response.userInfo, usernameFallback = "")
                    if (response.userInfo?.phone.isNullOrBlank()) {
                        // 微信新建账号：强制绑定手机号后才进入主页
                        _uiState.value = LoginUiState(isLoggedIn = true, requiresPhoneBinding = true)
                    } else {
                        _uiState.value = LoginUiState(isLoggedIn = true)
                    }
                }
                is ApiResult.NetworkError -> {
                    _uiState.value = LoginUiState(errorMessage = "网络错误: ${result.message}")
                }
                is ApiResult.InvalidRequest -> {
                    _uiState.value = LoginUiState(errorMessage = "微信登录失败: ${result.message}")
                }
                is ApiResult.InvalidResponse -> {
                    _uiState.value = LoginUiState(errorMessage = "响应格式错误，请重试")
                }
                else -> {
                    _uiState.value = LoginUiState(errorMessage = "微信登录失败，请重试")
                }
            }
        }
    }

    /**
     * 登录成功后的公共生命周期：SessionStore 持久化、认证客户端重建、新建本地会话、
     * 上传队列恢复、同步 Worker 调度。账号密码登录与微信登录共用。
     */
    private suspend fun completeLogin(token: String, userInfo: LoginUserInfo?, usernameFallback: String) {
        app.sessionStore.token = token
        app.sessionStore.userId = userInfo?.id
        app.sessionStore.username = userInfo?.username ?: usernameFallback
        app.sessionStore.realname = userInfo?.realname
        app.sessionStore.phone = userInfo?.phone
        app.authenticatedApiClient.onLoginSuccess(token)
        // A fresh local conversation is created exactly once per successful login.
        // Remote history remains available and can be selected from the history UI.
        app.healthChatRepository.createConversation()
        app.syncRepository.resumeQueue()
        MeasurementSyncWorker.schedule(context)
        TelemetryUploadWorker.schedule(context, userInfo?.id)
        MeasurementSyncWorker.triggerImmediate(context)
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LoginViewModel(context) as T
        }
    }
}
