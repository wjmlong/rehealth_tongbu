package org.jeecg.modules.rehealth.model.impl;

import org.jeecg.modules.rehealth.config.AttributionMode;
import org.jeecg.modules.rehealth.mobile.dto.AttributionEventsRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.AttributionResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.InterventionGenerateRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.InterventionGenerateResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.RiskEvaluateRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.RiskEvaluateResponseDto;
import org.jeecg.modules.rehealth.model.ModelHealthResponseDto;
import org.jeecg.modules.rehealth.model.ModelServiceClient;
import org.jeecg.modules.rehealth.model.ModelServiceErrorCode;
import org.jeecg.modules.rehealth.model.ModelServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class HttpModelServiceClient implements ModelServiceClient {
    private final ModelHttpTransport transport;
    private final String baseUrl;
    private final String attributionBaseUrl;
    private final AttributionMode attributionMode;
    private final String attributionInternalToken;
    private static final Pattern CORRELATION_ID_PATTERN =
            Pattern.compile("^[A-Za-z0-9._:-]{1,128}$");

    public HttpModelServiceClient(
            @Value("${rehealth.model-service.base-url:}") String baseUrl,
            @Value("${rehealth.attribution-service.base-url:${rehealth.model-service.base-url:}}") String attributionBaseUrl,
            @Value("${rehealth.model-service.timeout-seconds:10}") long timeoutSeconds,
            @Value("${rehealth.attribution.mode:pias}") String attributionMode,
            @Value("${rehealth.attribution-service.internal-token:}") String attributionInternalToken,
            @Value("${rehealth.attribution-service.internal-token-file:}") String attributionInternalTokenFile
    ) {
        this(
                baseUrl,
                attributionBaseUrl,
                timeoutSeconds,
                attributionMode,
                attributionInternalToken,
                attributionInternalTokenFile,
                3,
                30
        );
    }

    private HttpModelServiceClient(
            String baseUrl,
            String attributionBaseUrl,
            long timeoutSeconds,
            String attributionMode,
            String attributionInternalToken,
            String attributionInternalTokenFile,
            int circuitFailureThreshold,
            long circuitResetSeconds
    ) {
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.attributionBaseUrl = trimTrailingSlash(attributionBaseUrl);
        this.attributionMode = AttributionMode.parse(attributionMode);
        this.attributionInternalToken = resolveInternalToken(
                attributionInternalToken,
                attributionInternalTokenFile
        );
        this.transport = new ModelHttpTransport(
                timeoutSeconds,
                circuitFailureThreshold,
                circuitResetSeconds
        );
    }

    HttpModelServiceClient(String baseUrl, String attributionBaseUrl, long timeoutSeconds) {
        this(baseUrl, attributionBaseUrl, timeoutSeconds, "pias", "", "");
    }

    HttpModelServiceClient(
            String baseUrl,
            String attributionBaseUrl,
            long timeoutSeconds,
            int circuitFailureThreshold,
            long circuitResetSeconds
    ) {
        this(
                baseUrl,
                attributionBaseUrl,
                timeoutSeconds,
                "pias",
                "",
                "",
                circuitFailureThreshold,
                circuitResetSeconds
        );
    }

    HttpModelServiceClient(
            String baseUrl,
            String attributionBaseUrl,
            long timeoutSeconds,
            String attributionMode,
            String attributionInternalToken
    ) {
        this(baseUrl, attributionBaseUrl, timeoutSeconds, attributionMode, attributionInternalToken, "");
    }

    @Override
    public boolean isConfigured() {
        return baseUrl != null && !baseUrl.isBlank();
    }

    @Override
    public ModelHealthResponseDto health() {
        ensureConfigured();
        return get("/ready", ModelHealthResponseDto.class);
    }

    @Override
    public RiskEvaluateResponseDto evaluateRisk(RiskEvaluateRequestDto request) {
        ensureConfigured();
        String requestId = correlationId(request == null ? null : request.requestId);
        if (request != null) {
            request.requestId = requestId;
        }
        return post("/v1/cvd/risk/evaluate", request, RiskEvaluateResponseDto.class, requestId);
    }

    @Override
    public InterventionGenerateResponseDto generateIntervention(InterventionGenerateRequestDto request) {
        ensureConfigured();
        String requestId = correlationId(request == null ? null : request.requestId);
        if (request != null) {
            request.requestId = requestId;
        }
        return post(
                "/v1/cvd/intervention/generate",
                request,
                InterventionGenerateResponseDto.class,
                requestId
        );
    }

    @Override
    public AttributionResponseDto evaluateAttribution(AttributionEventsRequestDto request) {
        try {
            AttributionResponseDto response;
            switch (attributionMode) {
                case PIAS -> response = evaluatePiasAttribution(request);
                case DEMO_MOCK -> {
                    ensureConfigured(baseUrl, "rehealth.model-service.base-url");
                    String requestId = correlationId(request == null ? null : request.requestId);
                    if (request != null) {
                        request.requestId = requestId;
                    }
                    response = post(
                            baseUrl,
                            "/v1/cvd/attribution/individual",
                            request,
                            AttributionResponseDto.class,
                            requestId
                    );
                }
                default -> throw new IllegalStateException("unsupported attribution mode");
            }
            AttributionResponseAdapter.enrich(response, request, attributionMode);
            return response;
        } catch (RuntimeException failure) {
            return AttributionResponseAdapter.error(request, attributionMode);
        }
    }

    private AttributionResponseDto evaluatePiasAttribution(AttributionEventsRequestDto request) {
        ensureConfigured(attributionBaseUrl, "rehealth.attribution-service.base-url");
        if (attributionInternalToken.isBlank()) {
            throw new IllegalStateException("rehealth.attribution-service.internal-token is not configured");
        }
        String requestId = request == null ? null : request.requestId;
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalStateException("attribution request id is required");
        }
        PiasAttributionEnvelope envelope = postPias(request, requestId);
        if (!Boolean.TRUE.equals(envelope.success) || envelope.result == null) {
            throw new IllegalStateException("PIAS attribution returned an unsuccessful response");
        }
        if (envelope.result.modelVersion == null || envelope.result.modelVersion.isBlank()) {
            throw new IllegalStateException("PIAS attribution returned no engine version");
        }
        return envelope.result;
    }

    private PiasAttributionEnvelope postPias(AttributionEventsRequestDto body, String requestId) {
        return transport.post(
                uri(attributionBaseUrl, "/api/pias/v2/attribute/individual"),
                body,
                PiasAttributionEnvelope.class,
                requestId,
                Map.of(
                        "Authorization", "Bearer " + attributionInternalToken,
                        "Idempotency-Key", requestId
                )
        );
    }

    private <T> T get(String path, Class<T> responseType) {
        String requestId = correlationId(null);
        return transport.get(uri(path), responseType, requestId);
    }

    private <T> T post(String path, Object body, Class<T> responseType, String requestId) {
        return post(baseUrl, path, body, responseType, requestId);
    }

    private <T> T post(
            String targetBaseUrl,
            String path,
            Object body,
            Class<T> responseType,
            String requestId
    ) {
        return transport.post(
                uri(targetBaseUrl, path),
                body,
                responseType,
                requestId,
                Map.of()
        );
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
            throw new ModelServiceException(
                    ModelServiceErrorCode.CONFIGURATION,
                    "configured service base URL is invalid",
                    0,
                    null,
                    e
            );
        }
    }

    private void ensureConfigured(String targetBaseUrl, String propertyName) {
        if (targetBaseUrl == null || targetBaseUrl.isBlank()) {
            throw new ModelServiceException(
                    ModelServiceErrorCode.CONFIGURATION,
                    propertyName + " is not configured",
                    0,
                    null,
                    null
            );
        }
    }

    private String correlationId(String candidate) {
        if (candidate != null && CORRELATION_ID_PATTERN.matcher(candidate).matches()) {
            return candidate;
        }
        return UUID.randomUUID().toString();
    }

    private String trimTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("/+$", "");
    }

    private String resolveInternalToken(String token, String tokenFile) {
        String configuredToken = token == null ? "" : token.trim();
        if (!configuredToken.isBlank()) {
            return configuredToken;
        }
        if (tokenFile == null || tokenFile.isBlank()) {
            return "";
        }
        try {
            return Files.readString(Path.of(tokenFile.trim())).trim();
        } catch (IOException error) {
            throw new IllegalStateException("PIAS internal credential file is unreadable", error);
        }
    }

    private static class PiasAttributionEnvelope {
        public Boolean success;
        public String message;
        public AttributionResponseDto result;
    }
}
