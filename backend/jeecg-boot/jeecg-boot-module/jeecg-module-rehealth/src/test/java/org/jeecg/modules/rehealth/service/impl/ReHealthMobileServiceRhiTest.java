package org.jeecg.modules.rehealth.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jeecg.modules.rehealth.config.ReHealthIngestProperties;
import org.jeecg.modules.rehealth.ingest.HardwareIngestionPort;
import org.jeecg.modules.rehealth.ingest.query.HardwareTelemetryQuery;
import org.jeecg.modules.rehealth.model.ModelServiceClient;
import org.jeecg.modules.rehealth.repository.ReHealthBusinessRepository;
import org.jeecg.modules.rehealth.service.intervention.PersonalizedInterventionService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReHealthMobileServiceRhiTest {
    @Test
    void evaluatesSeriesInOrderAndBuildsServerControlledHistory() {
        ModelServiceClient modelClient = mock(ModelServiceClient.class);
        when(modelClient.evaluateRhi(any()))
                .thenReturn(rhiResponse(60.0), rhiResponse(62.0), rhiResponse(64.0));
        ReHealthMobileServiceImpl service = service(modelClient);
        ObjectNode request = JsonNodeFactory.instance.objectNode();
        ArrayNode evaluations = request.putArray("evaluations");
        evaluations.add(evaluation("rhi-day-1"));
        evaluations.add(evaluation("rhi-day-2"));
        evaluations.add(evaluation("rhi-day-3"));

        JsonNode response = service.evaluateRhiSeries("user-a", request);

        assertEquals("model-service", response.path("provider").asText());
        assertEquals("/v2/rhi/evaluate", response.path("route").asText());
        assertEquals(3, response.path("evaluations").size());
        ArgumentCaptor<JsonNode> requests = ArgumentCaptor.forClass(JsonNode.class);
        verify(modelClient, times(3)).evaluateRhi(requests.capture());
        List<JsonNode> values = requests.getAllValues();
        assertEquals(1, values.get(0).path("history").path("available_days").asInt());
        assertEquals(2, values.get(1).path("history").path("available_days").asInt());
        assertEquals(60.0, values.get(1).path("history").path("previous_display_score").asDouble());
        assertEquals(62.0, values.get(2).path("history").path("previous_display_score").asDouble());
    }

    @Test
    void rejectsEmptyOrOversizedSeriesBeforeCallingModelService() {
        ModelServiceClient modelClient = mock(ModelServiceClient.class);
        ReHealthMobileServiceImpl service = service(modelClient);
        ObjectNode empty = JsonNodeFactory.instance.objectNode();
        empty.putArray("evaluations");
        ObjectNode oversized = JsonNodeFactory.instance.objectNode();
        ArrayNode evaluations = oversized.putArray("evaluations");
        for (int index = 0; index < 121; index++) {
            evaluations.add(evaluation("rhi-" + index));
        }

        assertThrows(IllegalArgumentException.class, () -> service.evaluateRhiSeries("user-a", empty));
        assertThrows(
                IllegalArgumentException.class,
                () -> service.evaluateRhiSeries("user-a", oversized)
        );
        verify(modelClient, times(0)).evaluateRhi(any());
    }

    private ObjectNode evaluation(String requestId) {
        ObjectNode evaluation = JsonNodeFactory.instance.objectNode();
        evaluation.put("requestId", requestId);
        evaluation.putObject("history").put("available_days", 99);
        return evaluation;
    }

    private JsonNode rhiResponse(double score) {
        ObjectNode response = JsonNodeFactory.instance.objectNode();
        response.putObject("dynamic_health_index").put("score", score);
        return response;
    }

    private ReHealthMobileServiceImpl service(ModelServiceClient modelClient) {
        return new ReHealthMobileServiceImpl(
                modelClient,
                mock(HardwareIngestionPort.class),
                mock(HardwareTelemetryQuery.class),
                mock(ReHealthBusinessRepository.class),
                mock(ReHealthIngestProperties.class),
                mock(PersonalizedInterventionService.class),
                true,
                false,
                "Asia/Shanghai"
        );
    }
}
