package org.jeecg.modules.rehealth.model.impl;

import com.sun.net.httpserver.HttpServer;
import org.jeecg.modules.rehealth.mobile.dto.HealthAgentModelRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.HealthAgentResponseDto;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class HttpHealthAgentModelClientTest {
    @Test
    void springCreatesClientWhenMultipleConstructorsExist() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                    "healthAgentClientTest",
                    Map.of(
                            "rehealth.model-service.base-url", "http://127.0.0.1:8000",
                            "rehealth.model-service.timeout-seconds", "2",
                            "rehealth.health-agent.internal-token", "test-only-token",
                            "rehealth.health-agent.internal-token-file", ""
                    )
            ));
            context.register(HttpHealthAgentModelClient.class);

            context.refresh();

            assertNotNull(context.getBean(HttpHealthAgentModelClient.class));
        }
    }

    @Test
    void sendsInternalCredentialAndNoUserOrTenantIdentifier() throws Exception {
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/health-agent/respond", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = """
                    {"request_id":"agent-1","status":"ok","answer":"safe","medical_disclaimer":"notice",
                    "provider":"configured","model_version":"agent-v1","is_demo":false,"retryable":false}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
            HttpHealthAgentModelClient client =
                    new HttpHealthAgentModelClient(baseUrl, 2, "agent-secret");
            HealthAgentModelRequestDto request = new HealthAgentModelRequestDto();
            request.requestId = "agent-1";
            request.message = "question";
            request.context.riskLevel = "moderate";

            HealthAgentResponseDto response = client.respond(request);

            assertEquals("ok", response.status);
            assertEquals("Bearer agent-secret", authorization.get());
            assertFalse(body.get().contains("userId"));
            assertFalse(body.get().contains("tenantId"));
        } finally {
            server.stop(0);
        }
    }
}
