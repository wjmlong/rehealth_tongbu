package org.jeecg.modules.rehealth.model.impl;

import org.jeecg.modules.rehealth.config.AttributionMode;
import org.jeecg.modules.rehealth.mobile.dto.AttributionEventsRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.AttributionResponseDto;

import java.util.ArrayList;

final class AttributionResponseAdapter {
    private AttributionResponseAdapter() {
    }

    static void enrich(
            AttributionResponseDto response,
            AttributionEventsRequestDto request,
            AttributionMode mode
    ) {
        int historyDays = request == null || request.riskHistory == null
                ? 0
                : request.riskHistory.size();
        long interventionDays = request == null || request.riskHistory == null
                ? 0
                : request.riskHistory.stream()
                        .filter(point -> Integer.valueOf(1).equals(point.intervention))
                        .count();
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
        response.attributionMode = mode.wireValue();
        response.isMock = mode == AttributionMode.DEMO_MOCK;
        response.provider = mode == AttributionMode.PIAS ? "pias" : "model-service";
        response.requestId = request == null ? null : request.requestId;
    }

    static AttributionResponseDto error(
            AttributionEventsRequestDto request,
            AttributionMode mode
    ) {
        AttributionResponseDto response = new AttributionResponseDto();
        response.status = "error";
        response.attributionMode = mode.wireValue();
        response.isMock = mode == AttributionMode.DEMO_MOCK;
        response.provider = mode == AttributionMode.PIAS ? "pias" : "model-service";
        response.modelVersion = mode == AttributionMode.PIAS
                ? "pias-unavailable"
                : "demo-mock-unavailable";
        response.requestId = request == null ? null : request.requestId;
        response.errorCode = mode == AttributionMode.PIAS
                ? "PIAS_UNAVAILABLE"
                : "DEMO_ATTRIBUTION_UNAVAILABLE";
        response.errorMessage = "Attribution is temporarily unavailable; retry later.";
        response.retryable = true;
        response.forecast = new AttributionResponseDto.ForecastDto();
        response.forecast.raw = new AttributionResponseDto.ForecastRawDto();
        response.forecast.raw.dates = new ArrayList<>();
        response.forecast.raw.noAction = new ArrayList<>();
        response.forecast.raw.withPlan = new ArrayList<>();
        response.forecast.raw.ciUpper = new ArrayList<>();
        response.forecast.raw.ciLower = new ArrayList<>();
        response.interventionEffect = new AttributionResponseDto.InterventionEffectDto();
        response.interventionEffect.attAvailable = false;
        response.interventionEffect.attUnavailableReason = response.errorCode;
        return response;
    }
}
