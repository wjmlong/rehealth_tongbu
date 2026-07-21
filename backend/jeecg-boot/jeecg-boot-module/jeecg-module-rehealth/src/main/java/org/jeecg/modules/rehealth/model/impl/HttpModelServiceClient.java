package org.jeecg.modules.rehealth.model.impl;

import com.alibaba.fastjson.JSON;
import org.jeecg.modules.rehealth.mobile.dto.AttributionEventsRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.AttributionResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.InterventionGenerateRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.InterventionGenerateResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.RiskEvaluateRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.RiskEvaluateResponseDto;
import org.jeecg.modules.rehealth.model.ModelHealthResponseDto;
import org.jeecg.modules.rehealth.model.ModelServiceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class HttpModelServiceClient implements ModelServiceClient {
    private final HttpClient httpClient;
    private final String baseUrl;
    private final long timeoutSeconds;

    public HttpModelServiceClient(
            @Value("${rehealth.model-service.base-url:}") String baseUrl,
            @Value("${rehealth.model-service.timeout-seconds:10}") long timeoutSeconds
    ) {
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.timeoutSeconds = timeoutSeconds;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    @Override
    public boolean isConfigured() {
        return baseUrl != null && !baseUrl.isBlank();
    }

    @Override
    public ModelHealthResponseDto health() {
        ensureConfigured();
        return get("/health", ModelHealthResponseDto.class);
    }

    @Override
    public RiskEvaluateResponseDto evaluateRisk(RiskEvaluateRequestDto request) {
        ensureConfigured();
        return post("/v1/cvd/risk/evaluate", request, RiskEvaluateResponseDto.class);
    }

    @Override
    public InterventionGenerateResponseDto generateIntervention(InterventionGenerateRequestDto request) {
        ensureConfigured();
        return post("/v1/cvd/intervention/generate", request, InterventionGenerateResponseDto.class);
    }

    @Override
    public AttributionResponseDto evaluateAttribution(AttributionEventsRequestDto request) {
        ensureConfigured();
        return post("/v1/cvd/attribution/individual", request, AttributionResponseDto.class);
    }

    private <T> T get(String path, Class<T> responseType) {
        HttpRequest request = HttpRequest.newBuilder(uri(path))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .GET()
                .build();
        return send(request, responseType);
    }

    private <T> T post(String path, Object body, Class<T> responseType) {
        HttpRequest request = HttpRequest.newBuilder(uri(path))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JSON.toJSONString(body)))
                .build();
        return send(request, responseType);
    }

    private <T> T send(HttpRequest request, Class<T> responseType) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("model-service returned HTTP " + response.statusCode());
            }
            return JSON.parseObject(response.body(), responseType);
        } catch (IOException e) {
            throw new IllegalStateException("model-service request failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("model-service request interrupted", e);
        }
    }

    private void ensureConfigured() {
        if (!isConfigured()) {
            throw new IllegalStateException("rehealth.model-service.base-url is not configured");
        }
    }

    private URI uri(String path) {
        try {
            return URI.create(baseUrl + path);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("rehealth.model-service.base-url is invalid", e);
        }
    }

    private String trimTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("/+$", "");
    }
}
