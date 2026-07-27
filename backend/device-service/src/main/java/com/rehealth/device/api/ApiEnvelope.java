package com.rehealth.device.api;

public record ApiEnvelope<T>(
        boolean success,
        String message,
        int code,
        T result,
        long timestamp
) {
    public static <T> ApiEnvelope<T> ok(T result) {
        return new ApiEnvelope<>(true, "", 200, result, System.currentTimeMillis());
    }

    public static <T> ApiEnvelope<T> error(int code, String message) {
        return new ApiEnvelope<>(false, message, code, null, System.currentTimeMillis());
    }
}
