package org.jeecg.modules.rehealth.viomi;

import org.jeecg.modules.rehealth.ingest.HardwareIngestionPort;
import org.jeecg.modules.rehealth.mobile.dto.TelemetryBatchRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.TelemetryBatchResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Orchestrates the Viomi active-report flow: authenticate, map to the canonical telemetry batch,
 * and persist through the same {@link HardwareIngestionPort} used by the mobile app.
 */
@Service
public class ViomiReportService {

    private static final Logger log = LoggerFactory.getLogger(ViomiReportService.class);

    private final ViomiJwtVerifier jwtVerifier;
    private final ViomiTelemetryMapper mapper;
    private final HardwareIngestionPort ingestionPort;
    private final ViomiAdapterProperties properties;

    public ViomiReportService(ViomiJwtVerifier jwtVerifier, ViomiTelemetryMapper mapper,
                              HardwareIngestionPort ingestionPort, ViomiAdapterProperties properties) {
        this.jwtVerifier = jwtVerifier;
        this.mapper = mapper;
        this.ingestionPort = ingestionPort;
        this.properties = properties;
    }

    public ViomiAck handle(ViomiReportEnvelope envelope, String authorizationHeader) {
        if (!properties.isEnabled()) {
            log.warn("Viomi adapter is disabled; dropping report for dataType={}", envelope.DataType);
            return ViomiAck.fail("adapter disabled");
        }
        String token = resolveToken(envelope, authorizationHeader);
        ViomiAuthResult auth = jwtVerifier.verify(token);
        if (properties.isRequireAuth() && !auth.valid) {
            log.warn("Viomi report auth failed for dataType={}", envelope.DataType);
            return ViomiAck.fail("auth failed");
        }

        String imei = firstNonBlank(auth.imei, envelope.Imei);
        TelemetryBatchRequestDto batch = mapper.toBatch(envelope, imei);

        if (batch.measurements.isEmpty() && batch.sleepSessions.isEmpty() && batch.activitySessions.isEmpty()) {
            log.debug("Viomi report for dataType={} produced no persistable records; ack received", envelope.DataType);
            return ViomiAck.ok();
        }

        try {
            TelemetryBatchResponseDto result = ingestionPort.acceptBatch(batch);
            if (result != null && result.status != null && result.status.startsWith("REJECTED")) {
                log.warn("Viomi batch rejected for deviceId={}: {}", batch.deviceId, result.warnings);
                return ViomiAck.fail("rejected");
            }
            return ViomiAck.ok();
        } catch (RuntimeException e) {
            log.error("Viomi batch persistence failed for deviceId={}", batch.deviceId, e);
            return ViomiAck.fail("persistence error");
        }
    }

    private String resolveToken(ViomiReportEnvelope envelope, String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.toLowerCase().startsWith("bearer ")) {
            return authorizationHeader.substring(7).trim();
        }
        if (envelope.AccessToken != null && !envelope.AccessToken.isBlank()) {
            return envelope.AccessToken;
        }
        return null;
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }
}
