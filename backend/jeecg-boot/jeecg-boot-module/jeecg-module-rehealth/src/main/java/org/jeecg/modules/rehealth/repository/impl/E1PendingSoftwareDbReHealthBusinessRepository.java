package org.jeecg.modules.rehealth.repository.impl;

import org.jeecg.modules.rehealth.mobile.dto.AttributionEventsRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.DeviceBindRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.DeviceBindResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.FeedbackRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.InterventionGenerateResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.RiskEvaluateResponseDto;
import org.jeecg.modules.rehealth.repository.ReHealthBusinessRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class E1PendingSoftwareDbReHealthBusinessRepository implements ReHealthBusinessRepository {
    @Override
    public DeviceBindResponseDto recordDeviceBinding(DeviceBindRequestDto request) {
        DeviceBindResponseDto response = new DeviceBindResponseDto();
        response.deviceId = request == null ? null : request.deviceId;
        response.status = "SOFTWARE_DB_INTERFACE_READY_E1_PERSISTENCE_PENDING";
        response.persisted = false;
        response.persistenceStage = "E1 defines repository port; software_db tables/mappers are pending";
        return response;
    }

    @Override
    public void saveRiskResult(String requestId, RiskEvaluateResponseDto response) {
        // E1 keeps the software_db boundary explicit; table/mappers are intentionally pending.
    }

    @Override
    public Optional<RiskEvaluateResponseDto> findLatestRiskResult() {
        return Optional.empty();
    }

    @Override
    public void saveInterventionPlan(InterventionGenerateResponseDto response) {
        // E1 keeps the software_db boundary explicit; table/mappers are intentionally pending.
    }

    @Override
    public Optional<InterventionGenerateResponseDto> findLatestInterventionPlan() {
        return Optional.empty();
    }

    @Override
    public void saveFeedback(String interventionId, FeedbackRequestDto request) {
        // E1 keeps the software_db boundary explicit; table/mappers are intentionally pending.
    }

    @Override
    public void recordAttributionEvents(AttributionEventsRequestDto request) {
        // E1 keeps the software_db boundary explicit; table/mappers are intentionally pending.
    }
}
