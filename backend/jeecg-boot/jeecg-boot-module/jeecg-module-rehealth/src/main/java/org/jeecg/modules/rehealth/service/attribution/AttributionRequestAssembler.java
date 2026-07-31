package org.jeecg.modules.rehealth.service.attribution;

import com.alibaba.fastjson.JSON;
import org.jeecg.modules.rehealth.mobile.dto.AttributionEventsRequestDto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * Local-QA-only replay path. Production must keep the corresponding property disabled;
     * normal attribution continues to ignore client-provided outcome history.
     */
    public static AttributionEventsRequestDto fromSyntheticQaHistory(
            String userId,
            AttributionEventsRequestDto clientRequest
    ) {
        Map<String, AttributionEventsRequestDto.AttributionHistoryPointDto> validByDate =
                new LinkedHashMap<>();
        if (clientRequest != null && clientRequest.riskHistory != null) {
            clientRequest.riskHistory.stream()
                    .filter(AttributionRequestAssembler::isValidSyntheticPoint)
                    .sorted(Comparator.comparing(point -> LocalDate.parse(point.date)))
                    .forEach(point -> validByDate.put(point.date, point));
        }
        List<AttributionEventsRequestDto.AttributionHistoryPointDto> ordered =
                validByDate.values().stream().toList();
        int fromIndex = Math.max(0, ordered.size() - 90);
        AttributionEventsRequestDto authorized = new AttributionEventsRequestDto();
        authorized.forecastDays = normalizedForecastDays(clientRequest);
        authorized.language = normalizedLanguage(clientRequest);
        authorized.riskHistory = List.copyOf(ordered.subList(fromIndex, ordered.size()));
        authorized.requestId = requestId(userId + "|synthetic_qa", authorized);
        return authorized;
    }

    private static boolean isValidSyntheticPoint(
            AttributionEventsRequestDto.AttributionHistoryPointDto point
    ) {
        if (point == null || point.date == null || point.riskScore == null || point.intervention == null) {
            return false;
        }
        try {
            LocalDate.parse(point.date);
        } catch (RuntimeException ignored) {
            return false;
        }
        return Double.isFinite(point.riskScore)
                && point.riskScore >= 0.0
                && point.riskScore <= 1.0
                && (point.intervention == 0 || point.intervention == 1);
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
