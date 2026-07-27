package org.jeecg.modules.rehealth.service.attribution;

import com.alibaba.fastjson.JSON;
import org.jeecg.modules.rehealth.mobile.dto.AttributionEventsRequestDto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

public final class AttributionRequestAssembler {
    private AttributionRequestAssembler() {
    }

    public static AttributionEventsRequestDto fromPersistedHistory(
            String userId,
            AttributionEventsRequestDto clientRequest,
            List<AttributionEventsRequestDto.AttributionHistoryPointDto> persistedHistory
    ) {
        AttributionEventsRequestDto authorized = new AttributionEventsRequestDto();
        authorized.forecastDays = normalizedForecastDays(clientRequest);
        authorized.language = normalizedLanguage(clientRequest);
        authorized.riskHistory = persistedHistory == null ? List.of() : List.copyOf(persistedHistory);
        authorized.requestId = requestId(userId, authorized);
        return authorized;
    }

    private static int normalizedForecastDays(AttributionEventsRequestDto request) {
        if (request == null || request.forecastDays == null) {
            return 30;
        }
        return Math.max(1, Math.min(90, request.forecastDays));
    }

    private static String normalizedLanguage(AttributionEventsRequestDto request) {
        return request != null && "en".equalsIgnoreCase(request.language) ? "en" : "zh";
    }

    private static String requestId(String userId, AttributionEventsRequestDto request) {
        String material = userId + "|" + JSON.toJSONString(request);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(material.getBytes(StandardCharsets.UTF_8));
            return "attr-" + HexFormat.of().formatHex(digest).substring(0, 32);
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is unavailable", error);
        }
    }
}
