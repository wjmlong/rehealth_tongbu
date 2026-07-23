package org.jeecg.modules.rehealth.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DlqEnvelopeFactoryTest {
    @Test
    void emitsOnlyRedactedContractMetadataForConsumerPoison() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String poison = """
                {"event_type":"rehealth.telemetry.persisted.v1","event_id":"event_12345678",
                "batch_id":"batch_12345678","tenant_ref":"opaque_tenant123",
                "user_ref":"opaque_user12345","device_ref":"opaque_device123",
                "record_count":1,"token":"forbidden","heart_rate":72}
                """;

        JsonNode result = mapper.readTree(new DlqEnvelopeFactory(mapper).redacted(poison));

        assertEquals(Set.of(
                        "event_type", "event_id", "batch_id", "schema_id", "tenant_ref",
                        "user_ref", "device_ref", "record_count", "source_event_type",
                        "failure_code", "attempt_count", "persistence_status"),
                mapper.convertValue(result, java.util.Map.class).keySet());
        assertFalse(result.toString().contains("forbidden"));
        assertFalse(result.toString().contains("heart_rate"));
    }
}
