package org.jeecg.modules.rehealth.viomi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class ViomiOpenApiClient implements ViomiOpenApiGateway {
    private static final DateTimeFormatter API_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneOffset.UTC);
    private final ViomiAdapterProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private volatile Token token;

    public ViomiOpenApiClient(ViomiAdapterProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getConnectTimeoutSeconds()))
                .build();
    }

    @Override
    public boolean deviceExists(String imei) {
        Token current = accessToken();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("UserId", current.userId() == null || current.userId().isBlank()
                ? properties.getUserId() : current.userId());
        body.put("GroupId", "");
        body.put("MapType", "Baidu");
        JsonNode response = authorizedPost("/api/devicelist/get_devicelist", body, Set.of(0, 1, 1001));
        if (response.path("Code").asInt(-1) == 1001) return false;
        JsonNode result = response.path("Result");
        if (!result.isArray()) return false;
        for (JsonNode item : result) {
            if (imei.equals(text(item, "Imei", "IMEI", "imei"))) return true;
        }
        return false;
    }

    @Override
    public List<JsonNode> history(String metric, String imei, Instant begin, Instant end) {
        String path = switch (metric) {
            case "HEART_RATE" -> "/api/heartrate/get_heartrate_bytime";
            case "BLOOD_PRESSURE" -> "/api/bloodpressure/get_bloodpressure_bytime";
            case "BLOOD_OXYGEN" -> "/api/bloodoxygen/get_bloodoxygen_bytime";
            default -> throw new IllegalArgumentException("unsupported Viomi metric: " + metric);
        };
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("Imei", imei);
        body.put("BeginTime", API_TIME.format(begin));
        body.put("EndTime", API_TIME.format(end));
        if ("BLOOD_PRESSURE".equals(metric)) body.put("MapType", "Baidu");
        JsonNode result = authorizedPost(path, body).path("Result");
        List<JsonNode> rows = new ArrayList<>();
        if (result.isArray()) result.forEach(rows::add);
        return rows;
    }

    @Override
    public void sendMeasurementCommand(String metric, String imei) {
        int commandCode = properties.commandFor(metric);
        if (commandCode <= 0) {
            throw new IllegalStateException("Viomi measurement command is not validated for metric " + metric);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("Imei", imei);
        body.put("Time", API_TIME.format(Instant.now()));
        body.put("CommandCode", Integer.toString(commandCode));
        body.put("CommandValue", "");
        body.put("ReqId", UUID.randomUUID().toString());
        authorizedPost("/api/command/sendcommand", body, Set.of(0, 1, 1803));
    }

    private JsonNode authorizedPost(String path, Map<String, Object> body) {
        return authorizedPost(path, body, Set.of(0, 1));
    }

    private JsonNode authorizedPost(String path, Map<String, Object> body, Set<Integer> acceptedCodes) {
        Token current = accessToken();
        body.put("AccessToken", current.value());
        try {
            return post(path, body, current.value(), acceptedCodes);
        } catch (UnauthorizedTokenException ignored) {
            token = null;
            Token refreshed = accessToken();
            body.put("AccessToken", refreshed.value());
            return post(path, body, refreshed.value(), acceptedCodes);
        }
    }

    private synchronized Token accessToken() {
        if (!properties.isEnabled()) throw new IllegalStateException("Viomi integration is disabled");
        if (token != null && token.expiresAt().isAfter(Instant.now().plusSeconds(60))) return token;
        if (properties.getAppId().isBlank() || properties.getAppKey().isBlank()) {
            throw new IllegalStateException("Viomi AppId/AppKey are not configured");
        }
        long timestamp = Instant.now().getEpochSecond();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("AppId", properties.getAppId());
        body.put("Timestamp", timestamp);
        body.put("Password", md5(properties.getAppKey() + properties.getAppId() + timestamp));
        JsonNode result = post("/api/token/get_token", body, null, Set.of(0, 1)).path("Result");
        String value = text(result, "AccessToken", "accessToken");
        if (value == null || value.isBlank()) throw new IllegalStateException("Viomi token response is missing AccessToken");
        String userId = text(result, "UserId", "userId");
        long expires = result.path("ExpiresIn").asLong(7200);
        token = new Token(value, userId, Instant.now().plusSeconds(Math.max(120, expires)));
        return token;
    }

    private JsonNode post(String path, Map<String, Object> body, String accessToken, Set<Integer> acceptedCodes) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getBaseUrl() + path))
                    .timeout(Duration.ofSeconds(properties.getRequestTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
            if (accessToken != null) builder.header("Authorization", accessToken);
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 401) throw new UnauthorizedTokenException();
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Viomi OpenAPI HTTP " + response.statusCode());
            }
            JsonNode json = objectMapper.readTree(response.body());
            int code = json.path("Code").asInt(json.path("code").asInt(-1));
            if (!acceptedCodes.contains(code)) throw new IllegalStateException("Viomi OpenAPI rejected request");
            return json;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Viomi OpenAPI request interrupted", e);
        } catch (Exception e) {
            if (e instanceof IllegalStateException state) throw state;
            throw new IllegalStateException("Viomi OpenAPI request failed", e);
        }
    }

    private static String text(JsonNode node, String... names) {
        for (String name : names) if (node.path(name).isValueNode()) return node.path(name).asText();
        return null;
    }

    private static String md5(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("MD5").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            for (byte b : bytes) out.append(String.format("%02x", b));
            return out.toString();
        } catch (Exception e) {
            throw new IllegalStateException("MD5 unavailable", e);
        }
    }

    private record Token(String value, String userId, Instant expiresAt) {}

    private static final class UnauthorizedTokenException extends IllegalStateException {}
}
