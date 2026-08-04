package org.jeecg.modules.rehealth.service.behavior;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BehaviorPhotoAnalysisServiceTest {
    @Test
    void sendsImageContentAndParsesBoundedFoodRecord() {
        RecordingModel model = new RecordingModel("""
                ```json
                {"category":"food","title":"午餐","summary":"图像估算",
                 "items":["米饭","青菜"],"caloriesKcal":520,"proteinGrams":22,
                 "carbohydrateGrams":70,"fatGrams":15,"ocrText":null,"confidence":1.4}
                ```
                """);
        BehaviorPhotoAnalysisService service = new BehaviorPhotoAnalysisService(
                model, "gpt-5.6-luna", new ObjectMapper()
        );

        var record = service.analyze(new byte[]{1, 2, 3}, "image/jpeg");

        assertEquals("FOOD", record.category);
        assertEquals("午餐", record.title);
        assertEquals(520.0, record.caloriesKcal);
        assertEquals(1.0, record.confidence);
        assertNull(record.ocrText);
        UserMessage message = assertInstanceOf(UserMessage.class, model.request.messages().get(1));
        assertEquals(2, message.contents().size());
        assertInstanceOf(ImageContent.class, message.contents().get(1));
    }

    @Test
    void rejectsUnsupportedImageAndInvalidProviderJson() {
        BehaviorPhotoAnalysisService invalidJson = new BehaviorPhotoAnalysisService(
                new RecordingModel("not-json"), "gpt-5.6-luna", new ObjectMapper()
        );
        assertThrows(IllegalArgumentException.class, () -> invalidJson.analyze(new byte[]{1}, "image/gif"));
        assertThrows(
                BehaviorPhotoAnalysisException.class,
                () -> invalidJson.analyze(new byte[]{1}, "image/png")
        );
    }

    @Test
    void configuresAnExplicitProxyOnlyWhenHostAndPortAreValid() {
        var proxiedClient = BehaviorPhotoAnalysisService
                .jdkClientBuilder("127.0.0.1", 7897)
                .build();
        var selected = proxiedClient.proxy().orElseThrow()
                .select(URI.create("https://vision.example.com/v1"));

        assertEquals(1, selected.size());
        assertInstanceOf(InetSocketAddress.class, selected.get(0).address());
        assertTrue(BehaviorPhotoAnalysisService
                .jdkClientBuilder("", 0)
                .build()
                .proxy()
                .isEmpty());
    }

    private static class RecordingModel implements ChatModel {
        private final String answer;
        private ChatRequest request;

        private RecordingModel(String answer) {
            this.answer = answer;
        }

        @Override
        public ChatResponse chat(ChatRequest request) {
            this.request = request;
            return ChatResponse.builder().aiMessage(AiMessage.from(answer)).modelName("gpt-5.6-luna").build();
        }
    }
}
