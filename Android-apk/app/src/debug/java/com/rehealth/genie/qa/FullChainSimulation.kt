package com.rehealth.genie.qa

enum class SimulationStageStatus {
    SUCCESS,
    WARNING,
    FAILED,
}

data class SimulationStageResult(
    val code: String,
    val label: String,
    val status: SimulationStageStatus,
    val detail: String,
)

data class FullChainSimulationReport(
    val startedAt: Long,
    val completedAt: Long,
    val stages: List<SimulationStageResult>,
) {
    val successful: Boolean
        get() = stages.none { it.status == SimulationStageStatus.FAILED }
}

interface FullChainSimulationRunner {
    val available: Boolean

    suspend fun run(): FullChainSimulationReport
}
