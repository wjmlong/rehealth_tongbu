package org.jeecg.modules.rehealth.service.agent;

import com.sun.net.httpserver.HttpServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.jeecg.modules.rehealth.mobile.dto.HealthAgentHistoryMessageDto;
import org.jeecg.modules.rehealth.mobile.dto.HealthAgentModelRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.PatientProfileDto;
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
                    128,
                    profileTool(new StubReHealthBusinessRepository())
            );
            HealthAgentModelRequestDto request = new HealthAgentModelRequestDto();
            request.requestId = "request-http-client";
            request.message = "你好";

            var response = engine.respond(new HealthAgentEngineRequest(
                    "user-http",
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
        LangChain4jHealthAgentEngine engine = new LangChain4jHealthAgentEngine(
                model,
                "test-model",
                profileTool(new StubReHealthBusinessRepository())
        );
        HealthAgentModelRequestDto legacy = new HealthAgentModelRequestDto();
        legacy.requestId = "request-1";
        legacy.message = "我今天应该怎么运动";
        HealthAgentHistoryMessageDto previous = new HealthAgentHistoryMessageDto();
        previous.role = "USER";
        previous.content = "我昨天睡了七小时";

        var response = engine.respond(new HealthAgentEngineRequest(
                "user-a",
                new HealthAgentPromptContext(legacy, "{\"age\":54}"),
                List.of(previous)
        ));

        assertEquals("ok", response.status);
        assertEquals("逐步增加活动量", response.answer);
        assertEquals(3, model.messages.size());
        assertTrue(((SystemMessage) model.messages.get(0)).text().contains("\"age\":54"));
        assertFalse(((SystemMessage) model.messages.get(0)).text().contains("request-1"));
    }

    @Test
    void executesCurrentProfileToolForAuthenticatedUserWithoutAcceptingAUserIdArgument() {
        StubReHealthBusinessRepository repository = new StubReHealthBusinessRepository();
        repository.profile = new PatientProfileDto();
        repository.profile.name = "小禾";
        ToolCallingChatModel model = new ToolCallingChatModel();
        LangChain4jHealthAgentEngine engine = new LangChain4jHealthAgentEngine(
                model,
                "test-model",
                profileTool(repository)
        );
        HealthAgentModelRequestDto legacy = new HealthAgentModelRequestDto();
        legacy.requestId = "identity-request";
        legacy.message = "我是谁";

        var response = engine.respond(new HealthAgentEngineRequest(
                "authenticated-user",
                new HealthAgentPromptContext(legacy, "{}"),
                List.of()
        ));

        assertEquals("ok", response.status);
        assertEquals("你是小禾", response.answer);
        assertEquals(List.of("authenticated-user"), repository.queriedUsers);
        assertTrue(model.toolResult.contains("\"name\":\"小禾\""));
        assertFalse(model.toolSpecification.parameters().properties().containsKey("userId"));
    }

    private static CurrentUserProfileTool profileTool(StubReHealthBusinessRepository repository) {
        return new CurrentUserProfileTool(repository, new ObjectMapper());
    }

    private static class RecordingChatModel implements ChatModel {
        List<ChatMessage> messages = new ArrayList<>();

        @Override
        public ChatResponse chat(ChatRequest request) {
            this.messages = List.copyOf(request.messages());
            return ChatResponse.builder()
                    .aiMessage(AiMessage.from("逐步增加活动量"))
                    .modelName("test-model")
                    .build();
        }
    }

    private static class ToolCallingChatModel implements ChatModel {
        String toolResult = "";
        dev.langchain4j.agent.tool.ToolSpecification toolSpecification;

        @Override
        public ChatResponse chat(ChatRequest request) {
            toolSpecification = request.toolSpecifications().get(0);
            ToolExecutionResultMessage result = request.messages().stream()
                    .filter(ToolExecutionResultMessage.class::isInstance)
                    .map(ToolExecutionResultMessage.class::cast)
                    .findFirst()
                    .orElse(null);
            if (result == null) {
                return ChatResponse.builder()
                        .aiMessage(AiMessage.from(ToolExecutionRequest.builder()
                                .id("tool-1")
                                .name(CurrentUserProfileTool.NAME)
                                .arguments("{\"userId\":\"another-user\"}")
                                .build()))
                        .modelName("test-model")
                        .build();
            }
            toolResult = result.text();
            return ChatResponse.builder()
                    .aiMessage(AiMessage.from("你是小禾"))
                    .modelName("test-model")
                    .build();
        }
    }
}
