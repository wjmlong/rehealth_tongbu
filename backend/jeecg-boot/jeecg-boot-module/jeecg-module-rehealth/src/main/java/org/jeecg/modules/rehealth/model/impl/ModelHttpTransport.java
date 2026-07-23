package org.jeecg.modules.rehealth.model.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import org.jeecg.modules.rehealth.model.ModelServiceErrorCode;
import org.jeecg.modules.rehealth.model.ModelServiceException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Map;

final class ModelHttpTransport {
    private final HttpClient httpClient;
    private final Duration timeout;
    private final ModelServiceCircuitBreaker circuitBreaker;

    ModelHttpTransport(long timeoutSeconds, int failureThreshold, long resetSeconds) {
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.circuitBreaker = new ModelServiceCircuitBreaker(failureThreshold, resetSeconds);
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(timeout)
                .build();
    }

    <T> T get(URI uri, Class<T> responseType, String requestId) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(timeout)
                .header("X-Request-ID", requestId)
                .GET()
                .build();
        return send(request, responseType, requestId);
    }

    <T> T post(
            URI uri,
            Object body,
            Class<T> responseType,
            String requestId,
            Map<String, String> headers
    ) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .header("X-Request-ID", requestId);
        headers.forEach(builder::header);
        HttpRequest request = builder
                .POST(HttpRequest.BodyPublishers.ofString(JSON.toJSONString(body)))
                .build();
        return send(request, responseType, requestId);
    }

    private <T> T send(HttpRequest request, Class<T> responseType, String requestId) {
        circuitBreaker.beforeCall(requestId);
        try {
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                ModelServiceException failure = remoteFailure(
                        response.statusCode(),
                        response.body(),
                        requestId
                );
                if (failure.code() == ModelServiceErrorCode.UNAVAILABLE
                        || failure.code() == ModelServiceErrorCode.TIMEOUT) {
                    circuitBreaker.failure();
                }
                throw failure;
            }
            return parseResponse(response, responseType, requestId);
        } catch (HttpTimeoutException timeoutFailure) {
            circuitBreaker.failure();
            throw new ModelServiceException(
                    ModelServiceErrorCode.TIMEOUT,
                    "model-service request timed out",
                    0,
                    requestId,
                    timeoutFailure
            );
        } catch (IOException transportFailure) {
            circuitBreaker.failure();
            throw new ModelServiceException(
                    ModelServiceErrorCode.TRANSPORT,
                    "model-service transport failed",
                    0,
                    requestId,
                    transportFailure
            );
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new ModelServiceException(
                    ModelServiceErrorCode.INTERRUPTED,
                    "model-service request interrupted",
                    0,
                    requestId,
                    interrupted
            );
        }
    }

    private <T> T parseResponse(
            HttpResponse<String> response,
            Class<T> responseType,
            String requestId
    ) {
        try {
            T parsed = JSON.parseObject(response.body(), responseType);
            if (parsed == null) {
                throw new JSONException("empty JSON response");
            }
            circuitBreaker.success();
            return parsed;
        } catch (JSONException invalidJson) {
            throw new ModelServiceException(
                    ModelServiceErrorCode.INVALID_RESPONSE,
                    "model-service returned an invalid response",
                    response.statusCode(),
                    requestId,
                    invalidJson
            );
        }
    }

    private ModelServiceException remoteFailure(int status, String body, String requestId) {
        String remoteCode = null;
        try {
            JSONObject payload = JSON.parseObject(body);
            if (payload != null) {
                JSONObject detail = payload.getJSONObject("detail");
                remoteCode = detail == null ? null : detail.getString("code");
            }
        } catch (JSONException ignored) {
            remoteCode = null;
        }
        ModelServiceErrorCode code;
        if (status == 503 && "model_timeout".equals(remoteCode)) {
            code = ModelServiceErrorCode.TIMEOUT;
        } else if (status == 503 && "model_circuit_open".equals(remoteCode)) {
            code = ModelServiceErrorCode.CIRCUIT_OPEN;
        } else if (status >= 500) {
            code = ModelServiceErrorCode.UNAVAILABLE;
        } else {
            code = ModelServiceErrorCode.REMOTE_REJECTED;
        }
        return new ModelServiceException(
                code,
                "model-service request was not successful",
                status,
                requestId,
                null
        );
    }
}
