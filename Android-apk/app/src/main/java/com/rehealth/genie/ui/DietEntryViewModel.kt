package com.rehealth.genie.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rehealth.genie.ReHealthApplication
import com.rehealth.genie.diet.DietRecordDraft
import com.rehealth.genie.diet.DietRecordRepository
import com.rehealth.genie.diet.DietRecordWithUploadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DietEntryUiState(
    val records: List<DietRecordWithUploadState> = emptyList(),
    val saving: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
)

class DietEntryViewModel(
    private val repository: DietRecordRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(DietEntryUiState())
    val state: StateFlow<DietEntryUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { repository.preparePendingUploads() }
            repository.observeToday().collectLatest { records ->
                mutableState.update { it.copy(records = records) }
            }
        }
    }

    fun save(draft: DietRecordDraft) {
        if (mutableState.value.saving) return
        viewModelScope.launch {
            mutableState.update { it.copy(saving = true, message = null, isError = false) }
            runCatching { repository.save(draft) }
                .onSuccess { result ->
                    mutableState.update {
                        it.copy(
                            saving = false,
                            message = if (result.queued) {
                                "餐食已保存，正在等待同步。"
                            } else {
                                "餐食已保存，绑定设备后将自动同步。"
                            },
                        )
                    }
                }
                .onFailure { failure ->
                    mutableState.update {
                        it.copy(
                            saving = false,
                            message = failure.message ?: "餐食保存失败，请重试。",
                            isError = true,
                        )
                    }
                }
        }
    }

    fun preparePendingUploads() {
        viewModelScope.launch {
            runCatching { repository.preparePendingUploads() }
        }
    }

    fun clearMessage() {
        mutableState.update { it.copy(message = null, isError = false) }
    }

    class Factory(context: Context) : ViewModelProvider.Factory {
        private val application = context.applicationContext as ReHealthApplication

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DietEntryViewModel(application.dietRecordRepository) as T
        }
    }
}
