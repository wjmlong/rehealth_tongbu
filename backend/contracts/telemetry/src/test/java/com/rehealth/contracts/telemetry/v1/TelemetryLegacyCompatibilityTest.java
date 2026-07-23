package com.rehealth.contracts.telemetry.v1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TelemetryLegacyCompatibilityTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final TelemetryContractValidator validator =
            new TelemetryContractValidator(TelemetryValidationPolicy.productionDefault());

    @Test
    void preservesLegacyAndroidShapeWhenFixtureRoundTrips() throws IOException {
        // Given
        Path fixtureDirectory = Path.of(System.getProperty("fixtures", "src/test/resources/legacy-valid"));
        List<Path> fixtures;
        try (var paths = Files.list(fixtureDirectory)) {
            fixtures = paths.filter(path -> path.getFileName().toString().endsWith(".json")).toList();
        }

        // When / Then
        assertFalse(fixtures.isEmpty());
        for (Path fixture : fixtures) {
            JsonNode original = mapper.readTree(fixture.toFile());
            TelemetryBatchRequest request = mapper.treeToValue(original, TelemetryBatchRequest.class);
            TelemetryValidationResult validation = validator.validateClientRequest(request);
            JsonNode roundTrip = mapper.valueToTree(request);

            assertTrue(validation.valid(), () -> validation.errors().toString());
            assertEquals(original.path("batchId"), roundTrip.path("batchId"));
            assertEquals(original.path("deviceId"), roundTrip.path("deviceId"));
            assertEquals(original.path("measurements").size(), roundTrip.path("measurements").size());
            assertEquals(original.path("sleepSessions").size(), roundTrip.path("sleepSessions").size());
            assertEquals(original.path("activitySessions").size(), roundTrip.path("activitySessions").size());
        }
    }

    @Test
    void preservesDurableReceiptSemanticsWhenResponseRoundTrips() throws Exception {
        // Given
        String json = """
                {"batchId":"b-1","receiptId":"r-1","status":"ACCEPTED_PERSISTED",
                 "accepted":true,"persisted":true,"queued":false,"durableQueue":false,
                 "recordCount":3,"measurementCount":1,"sleepSessionCount":1,
                 "activitySessionCount":1,"signalChunkCount":0,"rejectedCount":0,"warnings":[]}
                """;

        // When
        TelemetryBatchResponse response = mapper.readValue(json, TelemetryBatchResponse.class);

        // Then
        assertTrue(response.accepted);
        assertTrue(response.persisted);
        assertEquals("ACCEPTED_PERSISTED", response.status);
        assertEquals(3, response.recordCount);
    }
}
