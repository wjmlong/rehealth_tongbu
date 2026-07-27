package org.jeecg.modules.rehealth.model;

public final class ModelServiceException extends RuntimeException {
    private final ModelServiceErrorCode code;
    private final int remoteStatus;
    private final String correlationId;

    public ModelServiceException(
            ModelServiceErrorCode code,
            String message,
            int remoteStatus,
            String correlationId,
            Throwable cause
    ) {
        super(message, cause);
        this.code = code;
        this.remoteStatus = remoteStatus;
        this.correlationId = correlationId;
    }

    public ModelServiceErrorCode code() {
        return code;
    }

    public int remoteStatus() {
        return remoteStatus;
    }

    public String correlationId() {
        return correlationId;
    }
}
