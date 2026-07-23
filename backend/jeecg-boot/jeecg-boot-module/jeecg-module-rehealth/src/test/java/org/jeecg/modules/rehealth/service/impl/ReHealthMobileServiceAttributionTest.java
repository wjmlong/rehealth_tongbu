package org.jeecg.modules.rehealth.service.impl;

import org.jeecg.modules.rehealth.config.ReHealthIngestProperties;
import org.jeecg.modules.rehealth.ingest.HardwareIngestionPort;
import org.jeecg.modules.rehealth.ingest.query.HardwareTelemetryQuery;
import org.jeecg.modules.rehealth.mobile.dto.AttributionEventsRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.AttributionResponseDto;
import org.jeecg.modules.rehealth.model.ModelServiceClient;
import org.jeecg.modules.rehealth.model.ModelCallAudit;
import org.jeecg.modules.rehealth.repository.ReHealthBusinessRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReHealthMobileServiceAttributionTest {
    @Test
    void usesAuthenticatedPersistedHistoryInsteadOfClientLabelsAndAuditsProvenance() {
        ModelServiceClient modelClient = mock(ModelServiceClient.class);
        ReHealthBusinessRepository repository = mock(ReHealthBusinessRepository.class);
        AttributionEventsRequestDto.AttributionHistoryPointDto persisted = historyPoint(
                "2026-07-23", 0.27, 1
        );
        when(repository.findAttributionHistory("user-a")).thenReturn(List.of(persisted));
        AttributionResponseDto downstream = new AttributionResponseDto();
        downstream.status = "ready";
        downstream.attributionMode = "pias";
        downstream.isMock = false;
        downstream.provider = "pias";
        downstream.modelVersion = "pias-individual-v2";
        when(modelClient.evaluateAttribution(any())).thenReturn(downstream);
        ReHealthMobileServiceImpl service = new ReHealthMobileServiceImpl(
                modelClient,
                mock(HardwareIngestionPort.class),
                mock(HardwareTelemetryQuery.class),
                repository,
                mock(ReHealthIngestProperties.class),
                true
        );
        AttributionEventsRequestDto clientRequest = new AttributionEventsRequestDto();
        clientRequest.riskHistory.add(historyPoint("2099-01-01", 0.99, 0));

        AttributionResponseDto response = service.recordAttributionEvents("user-a", clientRequest);

        ArgumentCaptor<AttributionEventsRequestDto> authorized =
                ArgumentCaptor.forClass(AttributionEventsRequestDto.class);
        verify(modelClient).evaluateAttribution(authorized.capture());
        assertEquals(1, authorized.getValue().riskHistory.size());
        assertEquals("2026-07-23", authorized.getValue().riskHistory.get(0).date);
        assertEquals(0.27, authorized.getValue().riskHistory.get(0).riskScore);
        assertFalse(authorized.getValue().requestId.isBlank());
        verify(repository).recordAttributionResult(eq("user-a"), eq(authorized.getValue()), eq(response));
        ArgumentCaptor<ModelCallAudit> audit = ArgumentCaptor.forClass(ModelCallAudit.class);
        verify(repository).recordModelRequest(eq("user-a"), audit.capture());
        assertEquals(authorized.getValue().requestId, audit.getValue().correlationId());
        assertEquals("ATTRIBUTION_EVALUATE_PIAS", audit.getValue().operation());
        assertEquals("pias-individual-v2", audit.getValue().modelVersion());
        assertEquals("SUCCESS", audit.getValue().outcome());
    }

    private AttributionEventsRequestDto.AttributionHistoryPointDto historyPoint(
            String date,
            double riskScore,
            int intervention
    ) {
        AttributionEventsRequestDto.AttributionHistoryPointDto point =
                new AttributionEventsRequestDto.AttributionHistoryPointDto();
        point.date = date;
        point.riskScore = riskScore;
        point.intervention = intervention;
        return point;
    }
}
