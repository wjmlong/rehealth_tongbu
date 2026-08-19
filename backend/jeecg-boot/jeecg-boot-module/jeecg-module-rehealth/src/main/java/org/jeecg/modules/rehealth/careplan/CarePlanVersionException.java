package org.jeecg.modules.rehealth.careplan;

import org.springframework.http.HttpStatus;

public final class CarePlanVersionException extends RuntimeException {
    private final HttpStatus status;

    private CarePlanVersionException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }

    public static CarePlanVersionException badRequest(String message) {
        return new CarePlanVersionException(HttpStatus.BAD_REQUEST, message);
    }

    public static CarePlanVersionException notFound(String message) {
        return new CarePlanVersionException(HttpStatus.NOT_FOUND, message);
    }

    public static CarePlanVersionException conflict(String message) {
        return new CarePlanVersionException(HttpStatus.CONFLICT, message);
    }
}
