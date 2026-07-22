package org.jeecg.modules.rehealth.repository.impl;

import org.jeecg.modules.rehealth.mobile.dto.AttributionEventsRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.AttributionResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.DeviceBindRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.DeviceBindResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.FeedbackRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.InterventionGenerateResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.RiskEvaluateResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.RiskEvaluateRequestDto;
import org.jeecg.modules.rehealth.repository.ReHealthBusinessRepository;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.Optional;

@Service
@ConditionalOnProperty(name = "rehealth.software-db.enabled", havingValue = "false", matchIfMissing = true)
public class E1PendingSoftwareDbReHealthBusinessRepository implements ReHealthBusinessRepository {
    @Override
    public DeviceBindResponseDto recordDeviceBinding(String userId, DeviceBindRequestDto request) {
        DeviceBindResponseDto response = new DeviceBindResponseDto();
        response.deviceId = request == null ? null : request.deviceId;
        response.status = "SOFTWARE_DB_INTERFACE_READY_E1_PERSISTENCE_PENDING";
        response.persisted = false;
        response.persistenceStage = "E1 defines repository port; software_db tables/mappers are pending";
        return response;
    }

    @Override
    public void saveRiskResult(String userId, String requestId, RiskEvaluateRequestDto request, RiskEvaluateResponseDto response) {
        // E1 keeps the software_db boundary explicit; table/mappers are intentionally pending.
    }

    @Override
    public Optional<RiskEvaluateResponseDto> findLatestRiskResult(String userId) {
        return Optional.empty();
    }

    @Override
    public void saveInterventionPlan(String userId, InterventionGenerateResponseDto response) {
        // E1 keeps the software_db boundary explicit; table/mappers are intentionally pending.
    }

    @Override
    public Optional<InterventionGenerateResponseDto> findLatestInterventionPlan(String userId) {
        return Optional.empty();
    }

    @Override
    public void saveFeedback(String userId, String interventionId, FeedbackRequestDto request) {
        // E1 keeps the software_db boundary explicit; table/mappers are intentionally pending.
    }

    @Override
    public void recordAttributionResult(String userId, AttributionEventsRequestDto request, AttributionResponseDto response) {
        // E1 keeps the software_db boundary explicit; table/mappers are intentionally pending.
    }
}
