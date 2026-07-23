package org.jeecg.modules.rehealth.kafka;

public final class TelemetryEventContractException extends RuntimeException {
    public TelemetryEventContractException(String message) {
        super(message);
    }

    public TelemetryEventContractException(String message, Throwable cause) {
        super(message, cause);
    }
}
