package com.rehealth.device.application;

import org.springframework.http.HttpStatus;

public final class DeviceRequestException extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;

    public DeviceRequestException(HttpStatus status, String errorCode) {
        super(errorCode);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus status() {
        return status;
    }

    public String errorCode() {
        return errorCode;
    }
}
