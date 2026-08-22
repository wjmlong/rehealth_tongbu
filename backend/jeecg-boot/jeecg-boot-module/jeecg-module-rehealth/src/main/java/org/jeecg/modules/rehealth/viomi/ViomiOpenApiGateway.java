package org.jeecg.modules.rehealth.viomi;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.List;

public interface ViomiOpenApiGateway {
    boolean deviceExists(String imei);
    List<JsonNode> history(String metric, String imei, Instant begin, Instant end);
    void sendMeasurementCommand(String metric, String imei);
}
