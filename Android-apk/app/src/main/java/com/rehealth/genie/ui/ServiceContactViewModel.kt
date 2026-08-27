package com.rehealth.genie.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rehealth.genie.ReHealthApplication
import com.rehealth.genie.network.ApiResult
import com.rehealth.genie.network.dto.InsuranceServiceContactDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 服务专员卡片状态：服务关系由保险侧管理，App 只读展示。 */
data class ServiceContactUiState(
    val contact: InsuranceServiceContactDto? = null,
    val loading: Boolean = false,
)

/** 读取当前登录用户的保险服务专员（服务关系），按账号隔离并避免重复加载。 */
class ServiceContactViewModel(context: Context) : ViewModel() {
    private val app = context.applicationContext as ReHealthApplication
    private val _uiState = MutableStateFlow(ServiceContactUiState())
    val uiState: StateFlow<ServiceContactUiState> = _uiState.asStateFlow()
    private var loadedUserId: String? = null

    fun loadForCurrentUser(force: Boolean = false) {
        val userId = app.sessionStore.userId ?: return
        if (!force && userId == loadedUserId) return
        loadedUserId = userId
        viewModelScope.launch {
            _uiState.value = ServiceContactUiState(loading = true)
            _uiState.value = when (val result = app.authenticatedApiClient.getServiceContact()) {
                is ApiResult.Success -> ServiceContactUiState(contact = result.data)
                else -> ServiceContactUiState()
            }
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ServiceContactViewModel(context) as T
    }
}
