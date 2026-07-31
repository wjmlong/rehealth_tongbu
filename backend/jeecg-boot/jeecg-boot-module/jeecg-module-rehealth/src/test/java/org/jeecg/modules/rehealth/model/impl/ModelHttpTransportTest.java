package org.jeecg.modules.rehealth.model.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModelHttpTransportTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void parsesRhiResponseAsJacksonJsonNode() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v2/rhi/evaluate", exchange -> {
            byte[] body = """
                    {"dynamic_health_index":{"score":72.4},"model_version":"rhi-rule-2.0.0"}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        ModelHttpTransport transport = new ModelHttpTransport(2, 3, 30);
        JsonNode response = transport.get(
                URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/v2/rhi/evaluate"),
                JsonNode.class,
                "rhi-json-node-test"
        );

        assertEquals(72.4, response.path("dynamic_health_index").path("score").asDouble());
        assertEquals("rhi-rule-2.0.0", response.path("model_version").asText());
    }
}
