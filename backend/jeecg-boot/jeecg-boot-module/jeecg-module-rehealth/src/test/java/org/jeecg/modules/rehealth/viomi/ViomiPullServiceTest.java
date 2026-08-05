package org.jeecg.modules.rehealth.viomi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.modules.rehealth.ingest.HardwareIngestionPort;
import org.jeecg.modules.rehealth.mobile.dto.TelemetryBatchRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.TelemetryBatchResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.ViomiSyncRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.ViomiSyncResponseDto;
import org.jeecg.modules.rehealth.repository.ReHealthBusinessRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ViomiPullServiceTest {
    @Test
    void syncNormalizesUtcHistoryAndPersistsBeforeReturning() throws Exception {
        ViomiOpenApiGateway gateway = mock(ViomiOpenApiGateway.class);
        ReHealthBusinessRepository repository = mock(ReHealthBusinessRepository.class);
        HardwareIngestionPort ingestion = mock(HardwareIngestionPort.class);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode row = mapper.readTree("{\"Systolic\":128,\"Diastolic\":79,\"BpTime\":\"2026-08-05 04:30:00\"}");
        when(repository.hasActiveDeviceBinding(anyString(), anyString())).thenReturn(true);
        when(gateway.history(anyString(), anyString(), any(), any())).thenReturn(List.of(row));
        TelemetryBatchResponseDto receipt = new TelemetryBatchResponseDto();
        receipt.accepted = true;
        receipt.persisted = true;
        receipt.status = "ACCEPTED_PERSISTED";
        when(ingestion.acceptBatch(any())).thenReturn(receipt);

        ViomiSyncRequestDto request = new ViomiSyncRequestDto();
        request.imei = "123456789012345";
        request.beginAt = 1_754_368_200_000L;
        request.endAt = request.beginAt + 86_400_000L;
        request.metrics = Set.of("BLOOD_PRESSURE");
        ViomiSyncResponseDto response = new ViomiPullService(gateway, repository, ingestion).sync("user-1", request);

        assertTrue(response.persisted);
        assertEquals(1, response.recordCount);
        assertEquals(128.0, response.measurements.get(0).primaryValue);
        assertEquals(79.0, response.measurements.get(0).secondaryValue);
        ArgumentCaptor<TelemetryBatchRequestDto> batch = ArgumentCaptor.forClass(TelemetryBatchRequestDto.class);
        verify(ingestion).acceptBatch(batch.capture());
        assertEquals("viomi_cloud", batch.getValue().source);
        assertEquals("BLOOD_PRESSURE", batch.getValue().measurements.get(0).get("metricType"));
    }
}
