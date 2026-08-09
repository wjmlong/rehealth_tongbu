package org.jeecg.modules.rehealth.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RehealthDeviceHealthClientTest {
    @Test
    void missingBaseUrlOrCredentialFailsClosedAsServiceUnavailable() {
        RehealthDeviceHealthClient client = new RehealthDeviceHealthClient(
                "", "", Duration.ofSeconds(1), null);

        ResponseStatusException failure = assertThrows(
                ResponseStatusException.class,
                () -> client.fetch("1000", "patient-a")
        );

        assertEquals(503, failure.getStatusCode().value());
        assertEquals("DEVICE_SERVICE_NOT_CONFIGURED", failure.getReason());
    }
}
