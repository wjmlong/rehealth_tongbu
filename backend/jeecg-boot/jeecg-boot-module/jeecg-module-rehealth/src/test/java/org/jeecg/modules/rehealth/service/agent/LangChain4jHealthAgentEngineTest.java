package org.jeecg.modules.rehealth.service.agent;

import com.sun.net.httpserver.HttpServer;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.jeecg.modules.rehealth.mobile.dto.HealthAgentHistoryMessageDto;
import org.jeecg.modules.rehealth.mobile.dto.HealthAgentModelRequestDto;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LangChain4jHealthAgentEngineTest {
    @Test
    void usesExplicitJdkHttpClientWhenExecutableContainsMultipleClientProviders() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> {
            byte[] response = """
                    {"id":"chat-1","object":"chat.completion","created":1785391200,
                    "model":"test-model","choices":[{"index":0,"message":{"role":"assistant",
                    "content":"服务可用"},"finish_reason":"stop"}],
                    "usage":{"prompt_tokens":10,"completion_tokens":4,"total_tokens":14}}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getRequestBody().readAllBytes();
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            LangChain4jHealthAgentEngine engine = new LangChain4jHealthAgentEngine(
                    "http://127.0.0.1:" + server.getAddress().getPort(),
                    "test-key",
                    "",
                    "test-model",
                    2,
                    128
            );
            HealthAgentModelRequestDto request = new HealthAgentModelRequestDto();
            request.requestId = "request-http-client";
            request.message = "你好";

            var response = engine.respond(new HealthAgentEngineRequest(
                    new HealthAgentPromptContext(request, "{}"),
                    List.of()
            ));

            assertEquals("ok", response.status);
            assertEquals("服务可用", response.answer);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void injectsAuthorizedPortraitAndBoundedHistoryIntoLangChain4jMessages() {
        RecordingChatModel model = new RecordingChatModel();
        LangChain4jHealthAgentEngine engine = new LangChain4jHealthAgentEngine(model, "test-model");
        HealthAgentModelRequestDto legacy = new HealthAgentModelRequestDto();
        legacy.requestId = "request-1";
        legacy.message = "我今天应该怎么运动";
        HealthAgentHistoryMessageDto previous = new HealthAgentHistoryMessageDto();
        previous.role = "USER";
        previous.content = "我昨天睡了七小时";

        var response = engine.respond(new HealthAgentEngineRequest(
                new HealthAgentPromptContext(legacy, "{\"age\":54}"),
                List.of(previous)
        ));

        assertEquals("ok", response.status);
        assertEquals("逐步增加活动量", response.answer);
        assertEquals(3, model.messages.size());
        assertTrue(((SystemMessage) model.messages.get(0)).text().contains("\"age\":54"));
        assertFalse(((SystemMessage) model.messages.get(0)).text().contains("request-1"));
    }

    private static class RecordingChatModel implements ChatModel {
        List<ChatMessage> messages = new ArrayList<>();

        @Override
        public ChatResponse chat(List<ChatMessage> messages) {
            this.messages = List.copyOf(messages);
            return ChatResponse.builder()
                    .aiMessage(AiMessage.from("逐步增加活动量"))
                    .modelName("test-model")
                    .build();
        }
    }
}
