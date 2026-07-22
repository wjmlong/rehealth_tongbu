package com.rehealth.genie.network.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HealthInterviewAnswerDto(
    val questionId: String,
    val topic: String,
    val content: String,
)

@JsonClass(generateAdapter = true)
data class HealthInterviewBaselineItemDto(
    val label: String,
    val value: String,
)

@JsonClass(generateAdapter = true)
data class HealthInterviewSubmitRequestDto(
    val answers: List<HealthInterviewAnswerDto>,
    val baselineItems: List<HealthInterviewBaselineItemDto> = emptyList(),
    val focusAreas: List<String> = emptyList(),
    val generatedAt: Long,
)
