package com.rehealth.genie.interview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.rehealth.genie.ReHealthApplication
import com.rehealth.genie.data.sync.UploadQueueEntity
import com.rehealth.genie.network.dto.HealthInterviewAnswerDto
import com.rehealth.genie.network.dto.HealthInterviewBaselineItemDto
import com.rehealth.genie.network.dto.HealthInterviewSubmitRequestDto
import com.rehealth.genie.work.MeasurementSyncWorker
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HealthInterviewSyncUiState(
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

class HealthInterviewSyncViewModel(
    private val application: ReHealthApplication,
    private val gson: Gson = Gson(),
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(HealthInterviewSyncUiState())
    val uiState: StateFlow<HealthInterviewSyncUiState> = mutableUiState.asStateFlow()

    fun enqueue(
        answers: List<InterviewAnswer>,
        baseline: HealthBaseline,
        onStored: () -> Unit,
    ) {
        if (mutableUiState.value.isSaving) return
        val request = healthInterviewSyncPayload(answers, baseline)
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            mutableUiState.value = HealthInterviewSyncUiState(isSaving = true)
            runCatching {
                application.syncRepository.enqueue(
                    UploadQueueEntity(
                        id = UUID.randomUUID().toString(),
                        kind = "health_interview",
                        payloadJson = gson.toJson(request),
                        status = "pending",
                        createdAt = now,
                        nextRetryAt = now,
                    ),
                )
            }.onSuccess {
                mutableUiState.value = HealthInterviewSyncUiState()
                runCatching { MeasurementSyncWorker.triggerImmediate(application) }
                onStored()
            }.onFailure {
                mutableUiState.value = HealthInterviewSyncUiState(
                    errorMessage = "健康档案保存失败，请重试",
                )
            }
        }
    }

    class Factory(
        private val application: ReHealthApplication,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HealthInterviewSyncViewModel(application) as T
    }
}

internal fun healthInterviewSyncPayload(
    answers: List<InterviewAnswer>,
    baseline: HealthBaseline,
): HealthInterviewSubmitRequestDto = HealthInterviewSubmitRequestDto(
    answers = answers.map { answer ->
        HealthInterviewAnswerDto(
            questionId = answer.question.id,
            topic = answer.question.topic.name,
            content = answer.content,
        )
    },
    baselineItems = baseline.items.map { item ->
        HealthInterviewBaselineItemDto(label = item.label, value = item.value)
    },
    focusAreas = baseline.focusAreas,
    generatedAt = baseline.generatedAt,
)
