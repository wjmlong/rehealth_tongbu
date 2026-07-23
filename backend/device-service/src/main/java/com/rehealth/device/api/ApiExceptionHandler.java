package com.rehealth.device.api;

import com.rehealth.device.application.DeviceRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(DeviceRequestException.class)
    ResponseEntity<ApiEnvelope<Void>> deviceRequest(DeviceRequestException exception) {
        return ResponseEntity.status(exception.status())
                .body(ApiEnvelope.error(exception.status().value(), exception.errorCode()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiEnvelope<Void>> malformedJson() {
        return ResponseEntity.badRequest().body(ApiEnvelope.error(400, "MALFORMED_REQUEST"));
    }
}
