package org.jeecg.modules.rehealth.service.intervention;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.jeecg.modules.rehealth.mobile.dto.PatientProfileDto;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LangChain4jInterventionEngineTest {
    @Test
    void returnsBoundedStructuredPlanFromAuthorizedTodayContext() {
        RecordingChatModel model = new RecordingChatModel("""
                ```json
                {
                  "summary":"优先把今天尚未完成的活动补齐",
                  "confidence":0.78,
                  "items":[
                    {
                      "category":"exercise",
                      "title":"运动",
                      "action":"晚餐后轻快步行15分钟，以能正常交谈为宜。",
                      "rationale":"今天活动时长较少。",
                      "target":"今天累计增加15分钟活动",
                      "timing":"晚餐后",
                      "evidenceRefs":["telemetry.todayBehavior.activeMinutes"]
                    },
                    {
                      "category":"sleep",
                      "title":"睡眠",
                      "action":"今晚按固定时间准备入睡。",
                      "rationale":"最近睡眠时长较前一周下降。",
                      "target":"保持规律作息",
                      "timing":"睡前1小时",
                      "evidenceRefs":["telemetry.recentChanges.sleep_minutes"]
                    }
                  ]
                }
                ```
                """);
        LangChain4jInterventionEngine engine =
                new LangChain4jInterventionEngine(model, "test-model", new ObjectMapper());
        PatientProfileDto profile = new PatientProfileDto();
        profile.age = 52;
        DeviceInterventionContext telemetry = telemetry();
        PersonalizedInterventionContext context = new PersonalizedInterventionContext(
                PersonalizedInterventionService.CONTEXT_VERSION,
                "tenant-secret",
                "user-secret",
                profile,
                null,
                null,
                telemetry
        );

        var response = engine.generate("request-1", context);

        assertNotNull(response.planId);
        assertEquals("test-model", response.modelVersion);
        assertEquals(false, response.isMock);
        assertEquals(2, response.items.size());
        assertEquals("运动", response.priorityIntervention);
        assertEquals("2026-07-31", response.focusDate);
        assertEquals(1785427200000L, response.latestDataAt);
        assertTrue(response.medicalDisclaimer.contains("不能替代"));
        String userPrompt = ((UserMessage) model.messages.get(1)).singleText();
        assertTrue(userPrompt.contains("\"steps\":4200"));
        assertTrue(userPrompt.contains("\"age\":52"));
        assertFalse(userPrompt.contains("tenant-secret"));
        assertFalse(userPrompt.contains("user-secret"));
    }

    @Test
    void rejectsMedicationDirectionsFromProviderOutput() {
        RecordingChatModel model = new RecordingChatModel("""
                {
                  "items":[{
                    "category":"follow_up",
                    "title":"用药",
                    "action":"立即停药并观察一天。",
                    "rationale":"不安全的模型输出。",
                    "target":"无",
                    "timing":"现在",
                    "evidenceRefs":["latestRisk.risk_level"]
                  }]
                }
                """);
        LangChain4jInterventionEngine engine =
                new LangChain4jInterventionEngine(model, "test-model", new ObjectMapper());
        PersonalizedInterventionContext context = new PersonalizedInterventionContext(
                PersonalizedInterventionService.CONTEXT_VERSION,
                "tenant-secret",
                "user-secret",
                new PatientProfileDto(),
                null,
                null,
                telemetry()
        );

        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> engine.generate("unsafe-request", context)
        );

        assertTrue(failure.getMessage().contains("medical safety policy"));
    }

    private static DeviceInterventionContext telemetry() {
        DeviceInterventionContext telemetry = new DeviceInterventionContext();
        telemetry.generatedAt = 1785456000000L;
        telemetry.localDate = "2026-07-31";
        telemetry.timeZone = "Asia/Shanghai";
        telemetry.latestDataAt = 1785427200000L;
        telemetry.todayBehavior = new DeviceInterventionContext.TodayBehavior();
        telemetry.todayBehavior.steps = 4200;
        telemetry.todayBehavior.activeMinutes = 18;
        telemetry.todayBehavior.activityCaloriesKcal = 126.0;
        telemetry.recentChanges = new ArrayList<>();
        return telemetry;
    }

    private static final class RecordingChatModel implements ChatModel {
        private final String response;
        private List<ChatMessage> messages = List.of();

        private RecordingChatModel(String response) {
            this.response = response;
        }

        @Override
        public ChatResponse chat(List<ChatMessage> messages) {
            this.messages = List.copyOf(messages);
            return ChatResponse.builder()
                    .aiMessage(AiMessage.from(response))
                    .modelName("test-model")
                    .build();
        }
    }
}
