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
import com.rehealth.genie.network.dto.PatientProfileDto
import com.rehealth.genie.work.MeasurementSyncWorker
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
                // Deterministic queue id: an identical resubmission (e.g. after a
                // crash between enqueue and upload) replaces the same row instead
                // of duplicating the interview upload.
                val payloadJson = gson.toJson(request)
                val userId = application.sessionStore.userId?.takeIf(String::isNotBlank)
                val identity = "$userId|$payloadJson".toByteArray(Charsets.UTF_8)
                val digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(identity)
                    .joinToString("") { "%02x".format(it) }
                    .take(24)
                application.syncRepository.enqueue(
                    UploadQueueEntity(
                        id = "interview-$digest",
                        kind = "health_interview",
                        payloadJson = payloadJson,
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
    profile = extractInterviewProfile(answers),
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

internal fun extractInterviewProfile(answers: List<InterviewAnswer>): PatientProfileDto? {
    val content = answers.firstOrNull { it.question.topic == InterviewTopic.PROFILE }
        ?.content
        ?.trim()
        ?: return null
    val age = Regex("(\\d{1,3})\\s*岁").find(content)?.groupValues?.get(1)?.toIntOrNull()
        ?.takeIf { it in 1..120 }
    val height = Regex("(\\d{2,3}(?:\\.\\d+)?)\\s*(?:cm|厘米|公分)", RegexOption.IGNORE_CASE)
        .find(content)?.groupValues?.get(1)?.toDoubleOrNull()?.takeIf { it in 50.0..250.0 }
    val weight = Regex("(\\d{1,3}(?:\\.\\d+)?)\\s*(?:kg|公斤|千克)", RegexOption.IGNORE_CASE)
        .find(content)?.groupValues?.get(1)?.toDoubleOrNull()?.takeIf { it in 2.0..500.0 }
    if (age == null && height == null && weight == null) return null
    return PatientProfileDto(age = age, heightCm = height, weightKg = weight)
}
