package org.jeecg.modules.rehealth.service.impl;

import org.jeecg.modules.rehealth.config.ReHealthIngestProperties;
import org.jeecg.modules.rehealth.ingest.HardwareIngestionPort;
import org.jeecg.modules.rehealth.ingest.query.HardwareTelemetryQuery;
import org.jeecg.modules.rehealth.mobile.dto.AttributionEventsRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.AttributionResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.FeedbackRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.FeedbackResponseDto;
import org.jeecg.modules.rehealth.model.ModelServiceClient;
import org.jeecg.modules.rehealth.model.ModelCallAudit;
import org.jeecg.modules.rehealth.repository.ReHealthBusinessRepository;
import org.jeecg.modules.rehealth.service.intervention.PersonalizedInterventionService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReHealthMobileServiceAttributionTest {
    @Test
    void returnsTypedFeedbackAcknowledgementOnlyAfterSoftwareDbWrite() {
        ReHealthBusinessRepository repository = mock(ReHealthBusinessRepository.class);
        ReHealthMobileServiceImpl service = service(repository, true);
        FeedbackRequestDto request = new FeedbackRequestDto();
        request.status = "completed";

        FeedbackResponseDto response = service.submitFeedback("user-a", "plan-9", request);

        verify(repository).saveFeedback("user-a", "plan-9", request);
        assertEquals("plan-9", response.interventionId);
        assertEquals("completed", response.status);
        assertEquals(true, response.persisted);
        assertEquals("SOFTWARE_DB_COMMITTED", response.persistenceStage);
    }

    @Test
    void rejectsFeedbackBeforeRepositoryWhenSoftwareDbIsDisabled() {
        ReHealthBusinessRepository repository = mock(ReHealthBusinessRepository.class);
        ReHealthMobileServiceImpl service = service(repository, false);
        FeedbackRequestDto request = new FeedbackRequestDto();
        request.status = "completed";

        assertThrows(
                IllegalStateException.class,
                () -> service.submitFeedback("user-a", "plan-9", request)
        );
    }

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
        ReHealthMobileServiceImpl service = service(repository, true, modelClient);
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

    @Test
    void allowsValidatedClientHistoryOnlyWhenSyntheticQaReplayIsExplicitlyEnabled() {
        ModelServiceClient modelClient = mock(ModelServiceClient.class);
        ReHealthBusinessRepository repository = mock(ReHealthBusinessRepository.class);
        AttributionResponseDto downstream = new AttributionResponseDto();
        downstream.status = "ready";
        downstream.attributionMode = "pias";
        downstream.modelVersion = "pias-individual-v2";
        when(modelClient.evaluateAttribution(any())).thenReturn(downstream);
        ReHealthMobileServiceImpl service = service(repository, true, modelClient, true);
        AttributionEventsRequestDto request = new AttributionEventsRequestDto();
        request.riskHistory.add(historyPoint("2026-07-01", 0.31, 0));
        request.riskHistory.add(historyPoint("2026-07-02", 0.29, 1));
        request.riskHistory.add(historyPoint("invalid", 3.0, 4));

        service.recordAttributionEvents("user-a", request);

        ArgumentCaptor<AttributionEventsRequestDto> authorized =
                ArgumentCaptor.forClass(AttributionEventsRequestDto.class);
        verify(modelClient).evaluateAttribution(authorized.capture());
        assertEquals(2, authorized.getValue().riskHistory.size());
        assertEquals("2026-07-01", authorized.getValue().riskHistory.get(0).date);
        assertEquals("2026-07-02", authorized.getValue().riskHistory.get(1).date);
        assertFalse(authorized.getValue().requestId.isBlank());
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

    private ReHealthMobileServiceImpl service(
            ReHealthBusinessRepository repository,
            boolean softwareDbEnabled
    ) {
        return service(repository, softwareDbEnabled, mock(ModelServiceClient.class));
    }

    private ReHealthMobileServiceImpl service(
            ReHealthBusinessRepository repository,
            boolean softwareDbEnabled,
            ModelServiceClient modelClient
    ) {
        return service(repository, softwareDbEnabled, modelClient, false);
    }

    private ReHealthMobileServiceImpl service(
            ReHealthBusinessRepository repository,
            boolean softwareDbEnabled,
            ModelServiceClient modelClient,
            boolean syntheticQaAttributionHistoryEnabled
    ) {
        return new ReHealthMobileServiceImpl(
                modelClient,
                mock(HardwareIngestionPort.class),
                mock(HardwareTelemetryQuery.class),
                repository,
                mock(ReHealthIngestProperties.class),
                mock(PersonalizedInterventionService.class),
                softwareDbEnabled,
                syntheticQaAttributionHistoryEnabled,
                "Asia/Shanghai"
        );
    }
}
