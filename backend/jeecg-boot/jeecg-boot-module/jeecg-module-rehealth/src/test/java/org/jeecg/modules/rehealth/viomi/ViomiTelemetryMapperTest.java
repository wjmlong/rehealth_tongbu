package org.jeecg.modules.rehealth.viomi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.modules.rehealth.mobile.dto.TelemetryBatchRequestDto;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ViomiTelemetryMapperTest {

    private ViomiTelemetryMapper mapper() {
        ViomiAdapterProperties properties = new ViomiAdapterProperties() {
            @Override
            public String getUserId() {
                return "test-user";
            }

            @Override
            public String getSource() {
                return "viomi";
            }

            @Override
            public String getAppId() {
                return "app-1";
            }
        };
        return new ViomiTelemetryMapper(properties, new ObjectMapper());
    }

    private ViomiReportEnvelope envelope(String dataType, String resultData) {
        ViomiReportEnvelope envelope = new ViomiReportEnvelope();
        envelope.DataType = dataType;
        envelope.ResultData = resultData;
        envelope.Imei = "7809101598";
        envelope.ReqId = "req-123";
        envelope.Time = "1670209774";
        return envelope;
    }

    @Test
    void mapsHealthPayload() {
        String resultData = "{\"bloodOxygen\":\"95\",\"bloodOxygenTime\":\"2021-10-12 08:09:10\","
                + "\"bloodPressureMax\":\"108\",\"bloodPressureMin\":\"67\",\"bpTime\":\"2021-10-12 08:09:10\","
                + "\"calorie\":\"23\",\"distance\":\"2021\",\"heartRate\":67,\"hrTime\":\"2021-10-12 08:09:09\","
                + "\"steps\":\"1234\",\"deepSleep\":\"3600\",\"lighSleep\":\"1800\",\"sleepTime\":\"2021-10-12 23:00:00\","
                + "\"totalSleep\":\"9000\"}";
        TelemetryBatchRequestDto batch = mapper().toBatch(envelope("Health", resultData), null);

        assertEquals("test-user", batch.userId);
        assertEquals("7809101598", batch.deviceId);
        assertEquals("viomi-7809101598-Health-req-123", batch.batchId);

        Map<String, Map<String, Object>> byType = index(batch.measurements);
        assertTrue(byType.containsKey("HEART_RATE"));
        assertEquals(67.0, byType.get("HEART_RATE").get("primaryValue"));
        assertEquals("bpm", byType.get("HEART_RATE").get("unit"));

        assertTrue(byType.containsKey("SPO2"));
        assertEquals(95.0, byType.get("SPO2").get("primaryValue"));

        assertTrue(byType.containsKey("BLOOD_PRESSURE"));
        assertEquals(108.0, byType.get("BLOOD_PRESSURE").get("primaryValue"));
        assertEquals(67.0, byType.get("BLOOD_PRESSURE").get("secondaryValue"));
        assertEquals("mmHg", byType.get("BLOOD_PRESSURE").get("unit"));

        assertTrue(byType.containsKey("STEPS"));
        assertTrue(byType.containsKey("DISTANCE"));
        assertTrue(byType.containsKey("CALORIE"));

        assertEquals(1, batch.sleepSessions.size());
        Map<String, Object> sleep = batch.sleepSessions.get(0);
        assertEquals(60, sleep.get("deepMinutes"));
        assertEquals(30, sleep.get("lightMinutes"));
    }

    @Test
    void skipsEmptyHealthFields() {
        // Only heart rate is populated; the rest are blank like the Viomi sample.
        String resultData = "{\"bloodOxygen\":\"95\",\"bloodOxygenTime\":\"2021-10-12 08:09:10\","
                + "\"bloodPressureMax\":\"108\",\"bloodPressureMin\":\"67\",\"bpTime\":\"2021-10-12 08:09:10\","
                + "\"calorie\":\"\",\"distance\":\"\",\"heartRate\":67,\"hrTime\":\"2021-10-12 08:09:09\","
                + "\"imei\":\"7809101598\",\"reqid\":\"\",\"steps\":\"\",\"totalSleep\":\"\"}";
        TelemetryBatchRequestDto batch = mapper().toBatch(envelope("Health", resultData), null);
        Map<String, Map<String, Object>> byType = index(batch.measurements);
        assertEquals(3, byType.size());
        assertTrue(byType.containsKey("HEART_RATE"));
        assertTrue(byType.containsKey("SPO2"));
        assertTrue(byType.containsKey("BLOOD_PRESSURE"));
        assertTrue(batch.sleepSessions.isEmpty());
    }

    @Test
    void mapsStepRollAndTemperature() {
        TelemetryBatchRequestDto step = mapper().toBatch(
                envelope("StepRoll", "{\"step\":\"1234\",\"roll\":\"5\",\"dataTime\":\"2021-10-12 08:09:10\","
                        + "\"imei\":\"7809101598\"}"), null);
        Map<String, Map<String, Object>> stepTypes = index(step.measurements);
        assertTrue(stepTypes.containsKey("STEPS"));
        assertTrue(stepTypes.containsKey("ROLL"));

        TelemetryBatchRequestDto temp = mapper().toBatch(
                envelope("Temperature", "{\"temperature\":36.1,\"temperatureTime\":\"2021-10-12 14:00:09\","
                        + "\"imei\":\"7400138847\"}"), null);
        Map<String, Map<String, Object>> tempTypes = index(temp.measurements);
        assertTrue(tempTypes.containsKey("BODY_TEMPERATURE"));
        assertEquals(36.1, tempTypes.get("BODY_TEMPERATURE").get("primaryValue"));
        assertEquals("°C", tempTypes.get("BODY_TEMPERATURE").get("unit"));
    }

    @Test
    void unknownDataTypeAcksWithoutRecords() {
        TelemetryBatchRequestDto batch = mapper().toBatch(envelope("Alarm", "{}"), null);
        assertTrue(batch.measurements.isEmpty());
        assertTrue(batch.sleepSessions.isEmpty());
    }

    private Map<String, Map<String, Object>> index(List<Map<String, Object>> measurements) {
        Map<String, Map<String, Object>> byType = new java.util.LinkedHashMap<>();
        for (Map<String, Object> m : measurements) {
            byType.put(String.valueOf(m.get("metricType")), m);
        }
        assertFalse(byType.isEmpty());
        return byType;
    }
}
