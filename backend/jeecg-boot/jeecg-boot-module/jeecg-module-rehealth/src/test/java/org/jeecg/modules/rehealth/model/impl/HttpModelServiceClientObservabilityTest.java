package org.jeecg.modules.rehealth.model.impl;

import com.sun.net.httpserver.HttpServer;
import org.jeecg.modules.rehealth.mobile.dto.RiskEvaluateRequestDto;
import org.jeecg.modules.rehealth.model.ModelServiceErrorCode;
import org.jeecg.modules.rehealth.model.ModelServiceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HttpModelServiceClientObservabilityTest {
    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void forwardsRiskCorrelationIdWithoutAuthorizationOrFeatureAuditHeaders() throws IOException {
        AtomicReference<String> correlationId = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        server = server(exchange -> {
            correlationId.set(exchange.getRequestHeaders().getFirst("X-Request-ID"));
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            exchange.getRequestBody().readAllBytes();
            byte[] body = """
                    {"risk_score":0.21,"risk_level":"low","feature_contributions":{},
                     "model_version":"real-v1","is_mock":false,"missing_fields":[],
                     "quality_warnings":[],"summary":"test","request_id":"trace-java-1"}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        HttpModelServiceClient client = new HttpModelServiceClient(baseUrl(), baseUrl(), 2);
        RiskEvaluateRequestDto request = new RiskEvaluateRequestDto();
        request.requestId = "trace-java-1";

        client.evaluateRisk(request);

        assertEquals("trace-java-1", correlationId.get());
        assertEquals(null, authorization.get());
    }

    @Test
    void classifiesModelUnavailableResponseWithStableTaxonomy() throws IOException {
        server = server(exchange -> {
            byte[] body = """
                    {"detail":{"code":"model_unavailable","message":"reviewed artifact unavailable"}}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(503, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        HttpModelServiceClient client = new HttpModelServiceClient(baseUrl(), baseUrl(), 2);

        ModelServiceException failure = assertThrows(
                ModelServiceException.class,
                () -> client.evaluateRisk(new RiskEvaluateRequestDto())
        );

        assertEquals(ModelServiceErrorCode.UNAVAILABLE, failure.code());
        assertEquals(503, failure.remoteStatus());
    }

    @Test
    void classifiesRequestTimeoutAndOpensCircuit() throws IOException {
        CountDownLatch release = new CountDownLatch(1);
        server = server(exchange -> {
            try {
                release.await(3, TimeUnit.SECONDS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
            exchange.close();
        });
        HttpModelServiceClient client = new HttpModelServiceClient(baseUrl(), baseUrl(), 1, 1, 30);

        ModelServiceException timeout = assertThrows(
                ModelServiceException.class,
                () -> client.evaluateRisk(new RiskEvaluateRequestDto())
        );
        ModelServiceException circuit = assertThrows(
                ModelServiceException.class,
                () -> client.evaluateRisk(new RiskEvaluateRequestDto())
        );
        release.countDown();

        assertEquals(ModelServiceErrorCode.TIMEOUT, timeout.code());
        assertEquals(ModelServiceErrorCode.CIRCUIT_OPEN, circuit.code());
    }

    private HttpServer server(com.sun.net.httpserver.HttpHandler handler) throws IOException {
        HttpServer created = HttpServer.create(new InetSocketAddress(0), 0);
        created.createContext("/v1/cvd/risk/evaluate", handler);
        created.start();
        return created;
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }
}
