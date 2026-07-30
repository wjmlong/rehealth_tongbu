package com.rehealth.genie.ui

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rehealth.genie.data.ProfileAvatarStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ProfileAvatarUiState(
    val bitmap: Bitmap? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

class ProfileAvatarViewModel(
    private val store: ProfileAvatarStore,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ProfileAvatarUiState())
    val uiState: StateFlow<ProfileAvatarUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.IO) { store.load() }
            mutableUiState.value = ProfileAvatarUiState(bitmap = bitmap)
        }
    }

    fun save(uri: Uri) {
        if (mutableUiState.value.isSaving) return
        viewModelScope.launch {
            mutableUiState.value = mutableUiState.value.copy(isSaving = true, errorMessage = null)
            mutableUiState.value = runCatching {
                withContext(Dispatchers.IO) { store.save(uri) }
            }.fold(
                onSuccess = { ProfileAvatarUiState(bitmap = it) },
                onFailure = {
                    ProfileAvatarUiState(
                        bitmap = mutableUiState.value.bitmap,
                        errorMessage = it.message ?: "头像保存失败，请重新选择",
                    )
                },
            )
        }
    }

    fun clearError() {
        mutableUiState.value = mutableUiState.value.copy(errorMessage = null)
    }

    class Factory(
        private val context: Context,
        private val identity: String,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ProfileAvatarViewModel(ProfileAvatarStore(context, identity)) as T
    }
}
