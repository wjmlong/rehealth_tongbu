package com.rehealth.contracts.telemetry.v1;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TelemetryContractRejectionTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final TelemetryContractValidator validator =
            new TelemetryContractValidator(TelemetryValidationPolicy.productionDefault());

    @Test
    void rejectsClientOwnerWhenBodyAttemptsToSetUserId() throws Exception {
        // Given
        TelemetryBatchRequest request = mapper.readValue(validJson("\"userId\":\"another-user\","), TelemetryBatchRequest.class);

        // When
        TelemetryValidationResult result = validator.validateClientRequest(request);

        // Then
        assertFalse(result.valid());
        assertEquals("owner.client_supplied", result.errors().get(0).code());
    }

    @Test
    void rejectsUnsupportedSchemaWhenExplicitVersionIsUnknown() throws Exception {
        // Given
        TelemetryBatchRequest request = mapper.readValue(validJson("\"schemaVersion\":\"telemetry-v99\","), TelemetryBatchRequest.class);

        // When
        TelemetryValidationResult result = validator.validateClientRequest(request);

        // Then
        assertFalse(result.valid());
        assertEquals("schema.unsupported", result.errors().get(0).code());
    }

    @Test
    void rejectsMalformedTimestampTypeAtDeserializationBoundary() {
        // Given
        String json = validJson("").replace("1720000010000", "\"not-a-timestamp\"");

        // When / Then
        assertThrows(InvalidFormatException.class, () -> mapper.readValue(json, TelemetryBatchRequest.class));
    }

    @Test
    void rejectsEmptyAndOversizedBatches() throws Exception {
        // Given
        TelemetryBatchRequest empty = mapper.readValue(validJson(""), TelemetryBatchRequest.class);
        empty.measurements.clear();
        TelemetryBatchRequest oversized = mapper.readValue(validJson(""), TelemetryBatchRequest.class);
        oversized.measurements = new ArrayList<>(List.of(oversized.measurements.get(0), oversized.measurements.get(0)));
        TelemetryContractValidator oneRecordValidator =
                new TelemetryContractValidator(new TelemetryValidationPolicy(1, false));

        // When
        TelemetryValidationResult emptyResult = validator.validateClientRequest(empty);
        TelemetryValidationResult oversizedResult = oneRecordValidator.validateClientRequest(oversized);

        // Then
        assertTrue(emptyResult.errors().stream().anyMatch(error -> error.code().equals("batch.empty")));
        assertTrue(oversizedResult.errors().stream().anyMatch(error -> error.code().equals("batch.oversized")));
    }

    @Test
    void rejectsInvalidMeasurementAndRawPayloadAliases() throws Exception {
        // Given
        String json = validJson("")
                .replace("\"measuredAt\":1720000010000", "\"measuredAt\":0")
                .replace("\"unit\":\"bpm\"", "\"unit\":\"bpm\",\"rawPpg\":[1,2,3]");
        TelemetryBatchRequest request = mapper.readValue(json, TelemetryBatchRequest.class);

        // When
        TelemetryValidationResult result = validator.validateClientRequest(request);

        // Then
        assertTrue(result.errors().stream().anyMatch(error -> error.code().equals("timestamp.invalid")));
        assertTrue(result.errors().stream().anyMatch(error -> error.code().equals("raw_signal.disabled")));
    }

    private String validJson(String prefix) {
        return "{" + prefix + "\"batchId\":\"batch-1\",\"deviceId\":\"ring-1\"," +
                "\"collectedFrom\":1720000000000,\"collectedTo\":1720000020000," +
                "\"source\":\"android-mrd\",\"measurements\":[{" +
                "\"metricType\":\"HEART_RATE\",\"measuredAt\":1720000010000," +
                "\"primaryValue\":72.0,\"unit\":\"bpm\"}]," +
                "\"quality\":{\"schemaVersion\":\"d2-v1\"}}";
    }
}
