package com.rehealth.genie.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rehealth.genie.ReHealthApplication
import com.rehealth.genie.network.ApiResult
import com.rehealth.genie.network.dto.PatientProfileDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileEditUiState(
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saved: Boolean = false,
)

/**
 * Profile edit ViewModel. Saves user-editable profile fields (name/age/height/weight)
 * to the backend via `PUT /rehealth/mobile/profile` ([AuthenticatedApiClient.updateProfile]).
 *
 * To avoid wiping server-side fields not shown in the edit dialog (diagnoses,
 * medications, history flags...), it first fetches the current remote profile and
 * merges the edited fields into it before submitting.
 */
class ProfileEditViewModel(private val context: Context) : ViewModel() {
    private val app = context.applicationContext as ReHealthApplication
    private val _uiState = MutableStateFlow(ProfileEditUiState())
    val uiState: StateFlow<ProfileEditUiState> = _uiState.asStateFlow()

    fun save(name: String, age: String, heightCm: String, weightKg: String) {
        if (_uiState.value.isSaving) return
        val trimmedName = name.trim().take(32)
        if (trimmedName.isBlank()) {
            _uiState.value = ProfileEditUiState(errorMessage = "请输入姓名/昵称")
            return
        }
        viewModelScope.launch {
            _uiState.value = ProfileEditUiState(isSaving = true)
            // Merge into current remote profile so unrelated fields survive the PUT.
            val remote = when (val current = app.authenticatedApiClient.getProfile()) {
                is ApiResult.Success -> current.data
                is ApiResult.Unauthorized -> {
                    _uiState.value = ProfileEditUiState(errorMessage = "登录已过期，请重新登录后再修改")
                    return@launch
                }
                else -> null
            }
            val request = (remote ?: PatientProfileDto()).copy(
                name = trimmedName,
                age = age.trim().toIntOrNull() ?: remote?.age,
                heightCm = heightCm.trim().toDoubleOrNull() ?: remote?.heightCm,
                weightKg = weightKg.trim().toDoubleOrNull() ?: remote?.weightKg,
            )
            when (val result = app.authenticatedApiClient.updateProfile(request)) {
                is ApiResult.Success -> {
                    _uiState.value = ProfileEditUiState(saved = true)
                }
                is ApiResult.Unauthorized -> {
                    _uiState.value = ProfileEditUiState(errorMessage = "登录已过期，请重新登录后再修改")
                }
                is ApiResult.InvalidRequest -> {
                    _uiState.value = ProfileEditUiState(errorMessage = "保存失败: ${result.message}")
                }
                is ApiResult.NetworkError -> {
                    _uiState.value = ProfileEditUiState(errorMessage = "网络错误，请稍后重试")
                }
                else -> {
                    _uiState.value = ProfileEditUiState(errorMessage = "保存失败，请重试")
                }
            }
        }
    }

    /** Reset transient state after the dialog consumes a save/error result. */
    fun reset() {
        _uiState.value = ProfileEditUiState()
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProfileEditViewModel(context) as T
        }
    }
}
