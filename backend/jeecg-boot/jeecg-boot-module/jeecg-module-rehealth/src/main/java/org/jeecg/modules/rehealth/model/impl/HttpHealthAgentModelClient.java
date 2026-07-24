package org.jeecg.modules.rehealth.model.impl;

import org.jeecg.modules.rehealth.mobile.dto.HealthAgentModelRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.HealthAgentResponseDto;
import org.jeecg.modules.rehealth.model.HealthAgentModelClient;
import org.jeecg.modules.rehealth.model.ModelServiceErrorCode;
import org.jeecg.modules.rehealth.model.ModelServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Service
public class HttpHealthAgentModelClient implements HealthAgentModelClient {
    private final ModelHttpTransport transport;
    private final String baseUrl;
    private final String internalToken;

    public HttpHealthAgentModelClient(
            @Value("${rehealth.model-service.base-url:}") String baseUrl,
            @Value("${rehealth.model-service.timeout-seconds:10}") long timeoutSeconds,
            @Value("${rehealth.health-agent.internal-token:}") String internalToken,
            @Value("${rehealth.health-agent.internal-token-file:}") String internalTokenFile
    ) {
        this.baseUrl = baseUrl == null ? "" : baseUrl.replaceAll("/+$", "");
        this.internalToken = resolveToken(internalToken, internalTokenFile);
        this.transport = new ModelHttpTransport(timeoutSeconds, 3, 30);
    }

    HttpHealthAgentModelClient(String baseUrl, long timeoutSeconds, String internalToken) {
        this(baseUrl, timeoutSeconds, internalToken, "");
    }

    @Override
    public HealthAgentResponseDto respond(HealthAgentModelRequestDto request) {
        if (baseUrl.isBlank() || internalToken.isBlank()) {
            throw new ModelServiceException(
                    ModelServiceErrorCode.CONFIGURATION,
                    "health-agent model-service boundary is not configured",
                    0,
                    request == null ? null : request.requestId,
                    null
            );
        }
        String requestId = request == null ? null : request.requestId;
        return transport.post(
                URI.create(baseUrl + "/v1/health-agent/respond"),
                request,
                HealthAgentResponseDto.class,
                requestId,
                Map.of("Authorization", "Bearer " + internalToken)
        );
    }

    private String resolveToken(String token, String tokenFile) {
        if (token != null && !token.isBlank()) {
            return token.trim();
        }
        if (tokenFile == null || tokenFile.isBlank()) {
            return "";
        }
        try {
            return Files.readString(Path.of(tokenFile.trim())).trim();
        } catch (IOException failure) {
            throw new IllegalStateException("health-agent internal credential file is unreadable", failure);
        }
    }
}
