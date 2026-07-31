package com.rehealth.device;

import com.rehealth.device.application.DeviceRequestException;
import com.rehealth.device.application.InternalOperationsService;
import com.rehealth.device.application.InterventionTelemetryContext;
import com.rehealth.device.config.ServiceCredentialProvider;
import com.rehealth.device.port.TelemetryReadPort;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InternalOperationsServiceTest {
    @Test
    void authorizesAndScopesInterventionContextByTenantUserAndTimeZone() {
        ServiceCredentialProvider credentials = mock(ServiceCredentialProvider.class);
        TelemetryReadPort reader = mock(TelemetryReadPort.class);
        when(credentials.credential()).thenReturn("service-secret");
        InterventionTelemetryContext expected = new InterventionTelemetryContext(
                1L,
                "2026-07-31",
                "Asia/Shanghai",
                null,
                new InterventionTelemetryContext.TodayBehavior(
                        0, 0, 0.0, null, null, null, List.of(), List.of()
                ),
                List.of()
        );
        when(reader.interventionContext(
                "tenant-a",
                "user-a",
                ZoneId.of("Asia/Shanghai")
        )).thenReturn(expected);
        InternalOperationsService service = new InternalOperationsService(credentials, reader);

        var result = service.interventionContext(
                "service-secret",
                "tenant-a",
                "user-a",
                "Asia/Shanghai"
        );

        assertEquals(expected, result);
        verify(reader).interventionContext(
                "tenant-a",
                "user-a",
                ZoneId.of("Asia/Shanghai")
        );
    }

    @Test
    void rejectsMissingTenantBeforeTimescaleQuery() {
        ServiceCredentialProvider credentials = mock(ServiceCredentialProvider.class);
        when(credentials.credential()).thenReturn("service-secret");
        InternalOperationsService service =
                new InternalOperationsService(credentials, mock(TelemetryReadPort.class));

        DeviceRequestException failure = assertThrows(
                DeviceRequestException.class,
                () -> service.interventionContext(
                        "service-secret",
                        "",
                        "user-a",
                        "Asia/Shanghai"
                )
        );

        assertEquals("TENANT_ID_REQUIRED", failure.errorCode());
    }
}
