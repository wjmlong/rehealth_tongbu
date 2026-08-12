package org.jeecg.modules.rehealth.insurance;

import org.springframework.http.HttpStatus;

public final class InsuranceApiException extends RuntimeException {
    private final HttpStatus status;

    private InsuranceApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }

    public static InsuranceApiException badRequest(String message) {
        return new InsuranceApiException(HttpStatus.BAD_REQUEST, message);
    }

    public static InsuranceApiException forbidden(String message) {
        return new InsuranceApiException(HttpStatus.FORBIDDEN, message);
    }

    public static InsuranceApiException notFound(String message) {
        return new InsuranceApiException(HttpStatus.NOT_FOUND, message);
    }

    public static InsuranceApiException serviceUnavailable(String message) {
        return new InsuranceApiException(HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}
