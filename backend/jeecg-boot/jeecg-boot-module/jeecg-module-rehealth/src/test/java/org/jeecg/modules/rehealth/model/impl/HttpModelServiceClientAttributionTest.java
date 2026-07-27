package org.jeecg.modules.rehealth.model.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sun.net.httpserver.HttpServer;
import org.jeecg.modules.rehealth.mobile.dto.AttributionEventsRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.AttributionResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.RiskEvaluateRequestDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class HttpModelServiceClientAttributionTest {
    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void forwardsPiasHistoryAndUnwrapsRichReadyResponse() throws IOException {
        AtomicReference<String> path = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> requestId = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/pias/v2/attribute/individual", exchange -> {
            path.set(exchange.getRequestURI().getPath());
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            requestId.set(exchange.getRequestHeaders().getFirst("X-Request-ID"));
            byte[] response = READY_ENVELOPE.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        HttpModelServiceClient client = new HttpModelServiceClient(
                baseUrl(), baseUrl(), 2, "pias", "synthetic-internal-token"
        );
        AttributionEventsRequestDto request = requestWithHistory();

        AttributionResponseDto response = client.evaluateAttribution(request);

        assertEquals("/api/pias/v2/attribute/individual", path.get());
        assertEquals("Bearer synthetic-internal-token", authorization.get());
        assertEquals("req-attribution", requestId.get());
        JSONObject forwarded = JSON.parseObject(body.get());
        assertEquals(30, forwarded.getIntValue("forecast_days"));
        assertEquals("zh", forwarded.getString("language"));
        assertEquals(0.31, forwarded.getJSONArray("risk_history").getJSONObject(0).getDouble("Y"));
        assertEquals(1, forwarded.getJSONArray("risk_history").getJSONObject(0).getIntValue("Z"));
        assertEquals("ready", response.status);
        assertEquals("pias", response.attributionMode);
        assertEquals(false, response.isMock);
        assertEquals("pias", response.provider);
        assertEquals(2, response.historyDays);
        assertEquals(14, response.minHistoryDays);
        assertEquals(0.29, response.currentState.riskScore);
        assertEquals(0.21, response.forecast.summary.d30WithPlan);
        assertEquals(-0.04, response.interventionEffect.individualAtt);
        assertEquals("继续保持", response.reports.user.advice);
    }

    @Test
    void preservesAccumulatingStateWithoutInventingForecastOrAtt() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/pias/v2/attribute/individual", exchange -> {
            byte[] response = ACCUMULATING_ENVELOPE.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        HttpModelServiceClient client = new HttpModelServiceClient(
                baseUrl(), baseUrl(), 2, "pias", "synthetic-internal-token"
        );

        AttributionResponseDto response = client.evaluateAttribution(requestWithHistory());

        assertEquals("accumulating", response.status);
        assertEquals(2, response.historyDays);
        assertEquals(14, response.minHistoryDays);
        assertNull(response.interventionEffect.individualAtt);
        assertEquals(0, response.forecast.raw.noAction.size());
    }

    @Test
    void routesExplicitDemoMockToModelServiceWithVisibleProvenance() throws IOException {
        AtomicReference<String> path = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/cvd/attribution/individual", exchange -> {
            path.set(exchange.getRequestURI().getPath());
            byte[] response = """
                    {"status":"ready","model_version":"cvd-mock-rules-v1",
                     "forecast":{"raw":{"dates":[],"no_action":[],"with_plan":[],"ci_upper":[],"ci_lower":[]}},
                     "intervention_effect":{"individual_att":null}}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        HttpModelServiceClient client = new HttpModelServiceClient(
                baseUrl(), "http://127.0.0.1:1", 2, "demo_mock", ""
        );

        AttributionResponseDto response = client.evaluateAttribution(requestWithHistory());

        assertEquals("/v1/cvd/attribution/individual", path.get());
        assertEquals("ready", response.status);
        assertEquals("demo_mock", response.attributionMode);
        assertEquals(true, response.isMock);
        assertEquals("model-service", response.provider);
    }

    @Test
    void piasOutageReturnsRetryableErrorAndNeverMock() {
        HttpModelServiceClient client = new HttpModelServiceClient(
                "http://127.0.0.1:1", "http://127.0.0.1:1", 1, "pias", "synthetic-internal-token"
        );

        AttributionResponseDto response = client.evaluateAttribution(requestWithHistory());

        assertEquals("error", response.status);
        assertEquals("PIAS_UNAVAILABLE", response.errorCode);
        assertEquals(true, response.retryable);
        assertEquals(false, response.isMock);
        assertEquals(0, response.forecast.raw.noAction.size());
    }

    @Test
    void usesHttp11ForUvicornCompatiblePosts() throws IOException {
        AtomicReference<String> upgrade = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/cvd/risk/evaluate", exchange -> {
            upgrade.set(exchange.getRequestHeaders().getFirst("Upgrade"));
            exchange.getRequestBody().readAllBytes();
            byte[] response = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        HttpModelServiceClient client = new HttpModelServiceClient(baseUrl(), baseUrl(), 2);

        client.evaluateRisk(new RiskEvaluateRequestDto());

        assertNull(upgrade.get());
    }

    private AttributionEventsRequestDto requestWithHistory() {
        AttributionEventsRequestDto request = new AttributionEventsRequestDto();
        request.requestId = "req-attribution";
        request.forecastDays = 30;
        request.language = "zh";
        AttributionEventsRequestDto.AttributionHistoryPointDto first =
                new AttributionEventsRequestDto.AttributionHistoryPointDto();
        first.date = "2026-07-20";
        first.riskScore = 0.31;
        first.intervention = 1;
        AttributionEventsRequestDto.AttributionHistoryPointDto second =
                new AttributionEventsRequestDto.AttributionHistoryPointDto();
        second.date = "2026-07-21";
        second.riskScore = 0.29;
        second.intervention = 0;
        request.riskHistory.add(first);
        request.riskHistory.add(second);
        return request;
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private static final String READY_ENVELOPE = """
            {"success":true,"code":200,"message":"ok","result":{
              "status":"ready","model_version":"pias-individual-v2",
              "current_state":{"risk_score":0.29,"risk_level":"low","trend":"improving"},
              "forecast":{"raw":{"dates":["Day 1"],"no_action":[0.28],"with_plan":[0.27],"ci_upper":[0.30],"ci_lower":[0.26]},"summary":{"30d_no_action":0.25,"30d_with_plan":0.21,"risk_reduction":0.04}},
              "intervention_effect":{"individual_att":-0.04,"att_ci_lower":-0.06,"att_ci_upper":-0.02,"att_p_value":0.03,"att_significant":true},
              "reports":{"user":{"headline":"计划有效","body":"合成测试报告","advice":"继续保持"}}
            }}
            """;

    private static final String ACCUMULATING_ENVELOPE = """
            {"success":true,"code":200,"message":"ok","result":{
              "status":"accumulating","model_version":"pias-individual-v2",
              "current_state":{"risk_score":0.29,"risk_level":"unknown","trend":"unknown"},
              "forecast":{"raw":{"dates":[],"no_action":[],"with_plan":[],"ci_upper":[],"ci_lower":[]},"summary":{"30d_no_action":0.0,"30d_with_plan":0.0,"risk_reduction":0.0}},
              "intervention_effect":{"individual_att":null,"att_ci_lower":null,"att_ci_upper":null,"att_p_value":null,"att_significant":null},
              "reports":{"user":{"headline":"","body":"","advice":""}}
            }}
            """;
}
