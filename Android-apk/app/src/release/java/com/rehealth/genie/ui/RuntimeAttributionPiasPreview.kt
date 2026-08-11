package com.rehealth.genie.ui

import com.rehealth.genie.phm.IndividualAttributionResult
import com.rehealth.genie.phm.PiasAttributionCacheRepository

internal suspend fun runtimeAttributionPiasResult(
    repository: PiasAttributionCacheRepository,
    historyDays: Int,
): IndividualAttributionResult? = repository.load(allowMock = false)
