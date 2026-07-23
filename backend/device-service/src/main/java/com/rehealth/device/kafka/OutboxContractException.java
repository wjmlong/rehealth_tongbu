package com.rehealth.device.kafka;

public final class OutboxContractException extends RuntimeException {
    public OutboxContractException(String message) {
        super(message);
    }

    public OutboxContractException(String message, Throwable cause) {
        super(message, cause);
    }
}
