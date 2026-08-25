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

    public static InsuranceApiException conflict(String message) {
        return new InsuranceApiException(HttpStatus.CONFLICT, message);
    }

    //update-begin---author:ai-agent ---date:2026-08-25  for：【保险侧用户服务关系一期】预留接口返回501-----------
    public static InsuranceApiException notImplemented(String message) {
        return new InsuranceApiException(HttpStatus.NOT_IMPLEMENTED, message);
    }
    //update-end---author:ai-agent ---date:2026-08-25  for：【保险侧用户服务关系一期】预留接口返回501-----------

    public static InsuranceApiException serviceUnavailable(String message) {
        return new InsuranceApiException(HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}
