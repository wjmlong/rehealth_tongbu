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
    private final String attributionBaseUrl;
    private final long timeoutSeconds;

    public HttpModelServiceClient(
            @Value("${rehealth.model-service.base-url:}") String baseUrl,
            @Value("${rehealth.attribution-service.base-url:${rehealth.model-service.base-url:}}") String attributionBaseUrl,
            @Value("${rehealth.model-service.timeout-seconds:10}") long timeoutSeconds
    ) {
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.attributionBaseUrl = trimTrailingSlash(attributionBaseUrl);
        this.timeoutSeconds = timeoutSeconds;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
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
        ensureConfigured(attributionBaseUrl, "rehealth.attribution-service.base-url");
        PiasAttributionEnvelope envelope = post(
                attributionBaseUrl,
                "/api/pias/v2/attribute/individual",
                request,
                PiasAttributionEnvelope.class
        );
        if (!Boolean.TRUE.equals(envelope.success)) {
            throw new IllegalStateException(
                    envelope.message == null || envelope.message.isBlank()
                            ? "PIAS attribution returned an unsuccessful response"
                            : "PIAS attribution failed: " + envelope.message
            );
        }
        if (envelope.result == null) {
            throw new IllegalStateException("PIAS attribution returned no result payload");
        }
        enrichAttributionMetadata(envelope.result, request);
        return envelope.result;
    }

    private <T> T get(String path, Class<T> responseType) {
        HttpRequest request = HttpRequest.newBuilder(uri(path))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .GET()
                .build();
        return send(request, responseType);
    }

    private <T> T post(String path, Object body, Class<T> responseType) {
        return post(baseUrl, path, body, responseType);
    }

    private <T> T post(String targetBaseUrl, String path, Object body, Class<T> responseType) {
        HttpRequest request = HttpRequest.newBuilder(uri(targetBaseUrl, path))
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
        ensureConfigured(baseUrl, "rehealth.model-service.base-url");
    }

    private URI uri(String path) {
        return uri(baseUrl, path);
    }

    private URI uri(String targetBaseUrl, String path) {
        try {
            return URI.create(targetBaseUrl + path);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("configured service base URL is invalid", e);
        }
    }

    private void ensureConfigured(String targetBaseUrl, String propertyName) {
        if (targetBaseUrl == null || targetBaseUrl.isBlank()) {
            throw new IllegalStateException(propertyName + " is not configured");
        }
    }

    private void enrichAttributionMetadata(
            AttributionResponseDto response,
            AttributionEventsRequestDto request
    ) {
        int historyDays = request == null || request.riskHistory == null ? 0 : request.riskHistory.size();
        long interventionDays = request == null || request.riskHistory == null
                ? 0
                : request.riskHistory.stream().filter(point -> Integer.valueOf(1).equals(point.intervention)).count();
        if (response.historyDays == null) {
            response.historyDays = historyDays;
        }
        if (response.minHistoryDays == null) {
            response.minHistoryDays = 14;
        }
        if (response.interventionDays == null) {
            response.interventionDays = Math.toIntExact(interventionDays);
        }
        if (response.interventionDataSufficient == null) {
            response.interventionDataSufficient = interventionDays >= 7;
        }
        if (response.interventionEffect == null) {
            response.interventionEffect = new AttributionResponseDto.InterventionEffectDto();
        }
        AttributionResponseDto.InterventionEffectDto effect = response.interventionEffect;
        if (effect.interventionDays == null) {
            effect.interventionDays = response.interventionDays;
        }
        if (effect.interventionDataSufficient == null) {
            effect.interventionDataSufficient = response.interventionDataSufficient;
        }
        if (effect.attAvailable == null) {
            effect.attAvailable = effect.individualAtt != null;
        }
        if (!Boolean.TRUE.equals(effect.attAvailable) && effect.attUnavailableReason == null) {
            effect.attUnavailableReason = interventionDays < 7
                    ? "intervention_days_lt_7"
                    : "pias_did_not_return_att";
        }
    }

    private String trimTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("/+$", "");
    }

    private static class PiasAttributionEnvelope {
        public Boolean success;
        public String message;
        public AttributionResponseDto result;
    }
}
