package org.jeecg.modules.rehealth.service.intervention;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.jeecg.modules.rehealth.mobile.dto.InterventionGenerateResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class LangChain4jInterventionEngine {
    private static final String MEDICAL_DISCLAIMER =
            "本计划仅用于健康管理参考，不能替代医生诊断、处方或治疗；如有明显不适或指标持续异常，请及时就医。";
    private static final Set<String> CATEGORIES = Set.of(
            "diet",
            "exercise",
            "sleep",
            "blood_pressure",
            "metabolic",
            "follow_up"
    );
    private static final List<String> EVIDENCE_PREFIXES = List.of(
            "telemetry.todayBehavior.",
            "telemetry.recentChanges",
            "profile.",
            "latestInterview.",
            "latestRisk."
    );
    private static final List<String> UNSAFE_OUTPUT_TERMS = List.of(
            "立即停药",
            "停止服用",
            "停用药",
            "调整药量",
            "调整剂量",
            "增加剂量",
            "减少剂量",
            "你患有",
            "已经确诊",
            "确诊为",
            "stop taking",
            "increase the dose",
            "decrease the dose",
            "you have been diagnosed"
    );

    private final String baseUrl;
    private final String apiKey;
    private final String modelName;
    private final Duration timeout;
    private final int maxTokens;
    private final ObjectMapper objectMapper;
    private volatile ChatModel chatModel;

    @Autowired
    public LangChain4jInterventionEngine(
            @Value("${rehealth.health-agent.langchain4j.base-url:}") String baseUrl,
            @Value("${rehealth.health-agent.langchain4j.api-key:}") String apiKey,
            @Value("${rehealth.health-agent.langchain4j.api-key-file:}") String apiKeyFile,
            @Value("${rehealth.health-agent.langchain4j.model:deepseek-v4-flash}") String modelName,
            @Value("${rehealth.intervention.langchain4j.timeout-seconds:${rehealth.health-agent.langchain4j.timeout-seconds:20}}")
            long timeoutSeconds,
            @Value("${rehealth.intervention.langchain4j.max-tokens:1400}") int maxTokens,
            ObjectMapper objectMapper
    ) {
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.apiKey = resolveSecret(apiKey, apiKeyFile);
        this.modelName = modelName == null ? "" : modelName.strip();
        this.timeout = Duration.ofSeconds(Math.max(1, Math.min(timeoutSeconds, 60)));
        this.maxTokens = Math.max(600, Math.min(maxTokens, 3000));
        this.objectMapper = objectMapper;
    }

    LangChain4jInterventionEngine(
            ChatModel chatModel,
            String modelName,
            ObjectMapper objectMapper
    ) {
        this.baseUrl = "test";
        this.apiKey = "test";
        this.modelName = modelName;
        this.timeout = Duration.ofSeconds(5);
        this.maxTokens = 1400;
        this.objectMapper = objectMapper;
        this.chatModel = chatModel;
    }

    public InterventionGenerateResponseDto generate(
            String requestId,
            PersonalizedInterventionContext context
    ) {
        String authorizedContext = authorizedContextJson(context);
        List<ChatMessage> messages = List.of(
                SystemMessage.from(systemPrompt()),
                UserMessage.from("请基于以下服务端授权上下文生成今日干预计划：\n" + authorizedContext)
        );
        ChatResponse modelResponse = model().chat(messages);
        String text = modelResponse == null || modelResponse.aiMessage() == null
                ? null
                : modelResponse.aiMessage().text();
        if (text == null || text.isBlank()) {
            throw new IllegalStateException("LangChain4j intervention provider returned no content");
        }
        InterventionGenerateResponseDto response = parse(text);
        normalize(response, requestId, context, modelResponse.modelName());
        return response;
    }

    private String authorizedContextJson(PersonalizedInterventionContext context) {
        Map<String, Object> authorized = new LinkedHashMap<>();
        authorized.put("contextVersion", context.contextVersion());
        if (context.profile() != null) {
            authorized.put("profile", context.profile());
        }
        if (context.latestInterview() != null) {
            authorized.put("latestInterview", context.latestInterview());
        }
        if (context.latestRisk() != null) {
            authorized.put("latestRisk", context.latestRisk());
        }
        authorized.put("telemetry", context.telemetry());
        try {
            return objectMapper.writeValueAsString(authorized);
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("personalized intervention context cannot be serialized", failure);
        }
    }

    private InterventionGenerateResponseDto parse(String raw) {
        String json = jsonObject(raw);
        try {
            return objectMapper.readValue(json, InterventionGenerateResponseDto.class);
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("LangChain4j intervention response is not valid JSON", failure);
        }
    }

    private void normalize(
            InterventionGenerateResponseDto response,
            String requestId,
            PersonalizedInterventionContext context,
            String providerModelName
    ) {
        if (response == null) {
            throw new IllegalStateException("LangChain4j intervention response is empty");
        }
        List<InterventionGenerateResponseDto.InterventionActionDto> normalizedItems =
                new ArrayList<>();
        if (response.items != null) {
            for (InterventionGenerateResponseDto.InterventionActionDto candidate : response.items) {
                if (candidate == null || blank(candidate.title) || blank(candidate.action)) {
                    continue;
                }
                InterventionGenerateResponseDto.InterventionActionDto item =
                        new InterventionGenerateResponseDto.InterventionActionDto();
                item.id = "action-" + String.format(Locale.ROOT, "%02d", normalizedItems.size() + 1);
                item.category = normalizeCategory(candidate.category);
                item.title = bounded(candidate.title, 40);
                item.action = bounded(candidate.action, 240);
                item.rationale = bounded(candidate.rationale, 240);
                item.target = bounded(candidate.target, 120);
                item.timing = bounded(candidate.timing, 80);
                item.priority = normalizedItems.size() + 1;
                item.evidenceRefs = safeEvidenceRefs(candidate.evidenceRefs);
                if (item.evidenceRefs.isEmpty()) {
                    continue;
                }
                normalizedItems.add(item);
                if (normalizedItems.size() == 5) {
                    break;
                }
            }
        }
        if (normalizedItems.isEmpty()) {
            throw new IllegalStateException("LangChain4j intervention response has no actionable item");
        }
        response.items = List.copyOf(normalizedItems);
        response.planId = UUID.nameUUIDFromBytes(
                ("personalized-intervention|" + requestId).getBytes(StandardCharsets.UTF_8)
        ).toString();
        response.generatedAt = Instant.now().toString();
        response.modelVersion = blank(providerModelName) ? modelName : providerModelName.strip();
        response.isMock = false;
        response.medicalDisclaimer = MEDICAL_DISCLAIMER;
        response.contextVersion = context.contextVersion();
        response.contextGeneratedAt = context.telemetry().generatedAt;
        response.latestDataAt = context.telemetry().latestDataAt;
        response.focusDate = context.telemetry().localDate;
        response.summary = blank(response.summary)
                ? "结合今日行为与近期变化安排的个性化行动"
                : bounded(response.summary, 160);
        response.contraindications = boundedList(response.contraindications, 8, 160);
        response.confidence = response.confidence == null
                ? null
                : Math.max(0.0, Math.min(response.confidence, 1.0));

        InterventionGenerateResponseDto.InterventionActionDto first = normalizedItems.get(0);
        response.priorityIntervention = first.title;
        response.rationale = first.action;
        response.expectedImpact = blank(first.target) ? first.rationale : first.target;
        assertSafeOutput(response);
    }

    private ChatModel model() {
        ChatModel current = chatModel;
        if (current != null) {
            return current;
        }
        if (baseUrl.isBlank() || apiKey.isBlank() || modelName.isBlank()) {
            throw new IllegalStateException("LangChain4j intervention provider is not configured");
        }
        synchronized (this) {
            if (chatModel == null) {
                chatModel = OpenAiChatModel.builder()
                        .httpClientBuilder(new JdkHttpClientBuilder())
                        .baseUrl(baseUrl)
                        .apiKey(apiKey)
                        .modelName(modelName)
                        .temperature(0.1)
                        .maxTokens(maxTokens)
                        .timeout(timeout)
                        .maxRetries(1)
                        .logRequests(false)
                        .logResponses(false)
                        .build();
            }
            return chatModel;
        }
    }

    private String systemPrompt() {
        return """
                你是睿禾健康个性化干预计划生成器。只输出一个 JSON 对象，不要 Markdown 或解释。
                你必须优先使用 telemetry.todayBehavior，再参考 telemetry.recentChanges、profile、
                latestInterview 和 latestRisk。recentChanges 只是描述性趋势，不得宣称因果或治疗效果。
                信息缺失时明确降低建议强度，不得编造饮食、检查、设备或疾病数据。
                只提供保守、可执行的生活方式和复查建议；不得诊断、开药、停药、调整药量或替代医生。
                精确热量、营养素、血压或实验室目标只有在授权上下文支持时才可给出。
                最多 5 项，按今天可执行性排序；category 只能是
                diet、exercise、sleep、blood_pressure、metabolic、follow_up。
                evidenceRefs 只能引用这些路径前缀：
                telemetry.todayBehavior、telemetry.recentChanges、profile、latestInterview、latestRisk。
                若上下文提示紧急危险，只建议及时线下就医，不提供居家替代处理。

                JSON 结构：
                {
                  "summary": "不超过80字",
                  "confidence": 0.0,
                  "contraindications": ["注意事项"],
                  "items": [
                    {
                      "category": "exercise",
                      "title": "运动",
                      "action": "今天具体做什么",
                      "rationale": "为什么与当前上下文相关",
                      "target": "可观察目标",
                      "timing": "执行时间",
                      "evidenceRefs": ["telemetry.todayBehavior.steps"]
                    }
                  ]
                }
                """;
    }

    private String normalizeCategory(String value) {
        if (value == null) {
            return "follow_up";
        }
        String normalized = value.strip().toLowerCase(Locale.ROOT);
        return CATEGORIES.contains(normalized) ? normalized : "follow_up";
    }

    private String jsonObject(String raw) {
        String stripped = raw.strip();
        int first = stripped.indexOf('{');
        int last = stripped.lastIndexOf('}');
        if (first < 0 || last <= first) {
            throw new IllegalStateException("LangChain4j intervention response contains no JSON object");
        }
        return stripped.substring(first, last + 1);
    }

    private List<String> boundedList(List<String> values, int maxItems, int maxLength) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .limit(maxItems)
                .map(value -> bounded(value, maxLength))
                .toList();
    }

    private List<String> safeEvidenceRefs(List<String> values) {
        return boundedList(values, 6, 80).stream()
                .filter(reference -> EVIDENCE_PREFIXES.stream().anyMatch(reference::startsWith))
                .toList();
    }

    private void assertSafeOutput(InterventionGenerateResponseDto response) {
        List<String> text = new ArrayList<>();
        text.add(response.summary);
        text.addAll(response.contraindications == null ? List.of() : response.contraindications);
        for (InterventionGenerateResponseDto.InterventionActionDto item : response.items) {
            text.add(item.title);
            text.add(item.action);
            text.add(item.rationale);
            text.add(item.target);
            text.add(item.timing);
        }
        String normalized = text.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .reduce("", (left, right) -> left + "\n" + right);
        if (UNSAFE_OUTPUT_TERMS.stream().anyMatch(normalized::contains)) {
            throw new IllegalStateException("LangChain4j intervention response failed medical safety policy");
        }
    }

    private String bounded(String value, int limit) {
        if (value == null) {
            return null;
        }
        String stripped = value.strip();
        return stripped.length() <= limit ? stripped : stripped.substring(0, limit);
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String resolveSecret(String configured, String secretFile) {
        if (configured != null && !configured.isBlank()) {
            return configured.strip();
        }
        if (secretFile == null || secretFile.isBlank()) {
            return "";
        }
        try {
            return Files.readString(Path.of(secretFile.strip())).strip();
        } catch (IOException failure) {
            throw new IllegalStateException("LangChain4j intervention credential file is unreadable", failure);
        }
    }

    private String normalizeBaseUrl(String value) {
        return value == null ? "" : value.strip().replaceAll("/+$", "");
    }
}
