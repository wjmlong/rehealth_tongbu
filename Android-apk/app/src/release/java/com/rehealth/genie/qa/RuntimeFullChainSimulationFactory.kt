package com.rehealth.genie.qa

import com.rehealth.genie.ReHealthApplication

/** Release builds do not package synthetic health-data generation. */
internal fun createRuntimeFullChainSimulationRunner(
    application: ReHealthApplication,
): FullChainSimulationRunner = object : FullChainSimulationRunner {
    override val available: Boolean = false

    override suspend fun run(): FullChainSimulationReport {
        error("Full-chain simulation is unavailable in release builds.")
    }
}
