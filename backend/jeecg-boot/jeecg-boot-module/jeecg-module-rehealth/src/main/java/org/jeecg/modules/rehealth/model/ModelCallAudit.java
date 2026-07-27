package org.jeecg.modules.rehealth.model;

public record ModelCallAudit(
        String correlationId,
        String operation,
        String modelVersion,
        String outcome,
        String errorCode,
        long latencyMillis
) {
}
