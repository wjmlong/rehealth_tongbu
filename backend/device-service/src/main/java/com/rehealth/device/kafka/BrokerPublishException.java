package com.rehealth.device.kafka;

public final class BrokerPublishException extends RuntimeException {
    public BrokerPublishException(String message) {
        super(message);
    }

    public BrokerPublishException(String message, Throwable cause) {
        super(message, cause);
    }
}
