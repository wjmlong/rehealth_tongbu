package org.jeecg.modules.rehealth.service.behavior;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.jeecg.modules.rehealth.mobile.dto.BehaviorRecordDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class BehaviorPhotoAnalysisService {
    private static final Logger log = LoggerFactory.getLogger(BehaviorPhotoAnalysisService.class);
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private final boolean enabled;
    private final String baseUrl;
    private final String apiKey;
    private final String modelName;
    private final String proxyHost;
    private final int proxyPort;
    private final Duration timeout;
    private final int maxTokens;
    private final int maxImageBytes;
    private final ObjectMapper objectMapper;
    private volatile ChatModel chatModel;

    @Autowired
    public BehaviorPhotoAnalysisService(
            @Value("${rehealth.vision.enabled:false}") boolean enabled,
            @Value("${rehealth.vision.base-url:}") String baseUrl,
            @Value("${rehealth.vision.api-key:}") String apiKey,
            @Value("${rehealth.vision.api-key-file:}") String apiKeyFile,
            @Value("${rehealth.vision.model:gpt-5.6-luna}") String modelName,
            @Value("${rehealth.vision.proxy-host:}") String proxyHost,
            @Value("${rehealth.vision.proxy-port:0}") int proxyPort,
            @Value("${rehealth.vision.timeout-seconds:75}") long timeoutSeconds,
            @Value("${rehealth.vision.max-tokens:1200}") int maxTokens,
            @Value("${rehealth.vision.max-image-bytes:4194304}") int maxImageBytes,
            ObjectMapper objectMapper
    ) {
        this.enabled = enabled;
        this.baseUrl = normalize(baseUrl);
        this.apiKey = resolveSecret(apiKey, apiKeyFile);
        this.modelName = modelName == null ? "" : modelName.trim();
        this.proxyHost = proxyHost == null ? "" : proxyHost.trim();
        this.proxyPort = proxyPort;
        this.timeout = Duration.ofSeconds(Math.max(5, Math.min(timeoutSeconds, 120)));
        this.maxTokens = Math.max(256, Math.min(maxTokens, 3000));
        this.maxImageBytes = Math.max(64 * 1024, Math.min(maxImageBytes, 8 * 1024 * 1024));
        this.objectMapper = objectMapper;
    }

    BehaviorPhotoAnalysisService(ChatModel chatModel, String modelName, ObjectMapper objectMapper) {
        this.enabled = true;
        this.baseUrl = "test";
        this.apiKey = "test";
        this.modelName = modelName;
        this.proxyHost = "";
        this.proxyPort = 0;
        this.timeout = Duration.ofSeconds(5);
        this.maxTokens = 1200;
        this.maxImageBytes = 4 * 1024 * 1024;
        this.objectMapper = objectMapper;
        this.chatModel = chatModel;
    }

    public BehaviorRecordDto analyze(byte[] image, String contentType) {
        validateImage(image, contentType);
        try {
            UserMessage userMessage = UserMessage.from(List.of(
                    TextContent.from(USER_PROMPT),
                    ImageContent.from(
                            Base64.getEncoder().encodeToString(image),
                            contentType,
                            ImageContent.DetailLevel.HIGH
                    )
            ));
            ChatResponse response = model().chat(ChatRequest.builder()
                    .messages(List.of(SystemMessage.from(SYSTEM_PROMPT), userMessage))
                    .build());
            String answer = response == null || response.aiMessage() == null
                    ? null
                    : response.aiMessage().text();
            if (answer == null || answer.isBlank()) {
                throw new BehaviorPhotoAnalysisException("vision provider returned an empty result");
            }
            BehaviorRecordDto record = parse(answer);
            record.modelVersion = response.modelName() == null || response.modelName().isBlank()
                    ? modelName
                    : response.modelName();
            return record;
        } catch (BehaviorPhotoAnalysisException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            log.warn("Behavior photo analysis failed: failureType={}", failure.getClass().getSimpleName());
            throw new BehaviorPhotoAnalysisException("vision provider is unavailable", failure);
        }
    }

    private BehaviorRecordDto parse(String answer) {
        String json = extractJson(answer);
        try {
            VisionPayload payload = objectMapper.readValue(json, VisionPayload.class);
            BehaviorRecordDto record = new BehaviorRecordDto();
            record.category = normalizeCategory(payload.category);
            record.title = bounded(nonBlank(payload.title, defaultTitle(record.category)), 255);
            record.summary = bounded(nonBlank(payload.summary, "已从照片生成行为记录。"), 2000);
            record.items = payload.items == null ? List.of() : payload.items.stream()
                    .filter(value -> value != null && !value.isBlank())
                    .map(value -> bounded(value.strip(), 255))
                    .limit(20)
                    .toList();
            record.caloriesKcal = nonNegative(payload.caloriesKcal, 100_000);
            record.proteinGrams = nonNegative(payload.proteinGrams, 10_000);
            record.carbohydrateGrams = nonNegative(payload.carbohydrateGrams, 10_000);
            record.fatGrams = nonNegative(payload.fatGrams, 10_000);
            record.ocrText = payload.ocrText == null ? null : bounded(payload.ocrText.strip(), 10_000);
            record.confidence = payload.confidence == null
                    ? null
                    : Math.max(0.0, Math.min(1.0, payload.confidence));
            return record;
        } catch (JsonProcessingException failure) {
            throw new BehaviorPhotoAnalysisException("vision provider returned invalid structured data", failure);
        }
    }

    private void validateImage(byte[] image, String contentType) {
        if (!enabled) throw new BehaviorPhotoAnalysisException("photo analysis is not configured");
        if (image == null || image.length == 0) throw new IllegalArgumentException("image is required");
        if (image.length > maxImageBytes) throw new IllegalArgumentException("image exceeds upload limit");
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("unsupported image type");
        }
    }

    private ChatModel model() {
        ChatModel current = chatModel;
        if (current != null) return current;
        if (baseUrl.isBlank() || apiKey.isBlank() || modelName.isBlank()) {
            throw new BehaviorPhotoAnalysisException("photo analysis is not configured");
        }
        synchronized (this) {
            if (chatModel == null) {
                chatModel = OpenAiChatModel.builder()
                        .httpClientBuilder(new JdkHttpClientBuilder().httpClientBuilder(
                                jdkClientBuilder(proxyHost, proxyPort)
                        ))
                        .baseUrl(baseUrl)
                        .apiKey(apiKey)
                        .modelName(modelName)
                        .maxTokens(maxTokens)
                        .timeout(timeout)
                        .maxRetries(0)
                        .logRequests(false)
                        .logResponses(false)
                        .build();
            }
        }
        return chatModel;
    }

    static HttpClient.Builder jdkClientBuilder(String proxyHost, int proxyPort) {
        HttpClient.Builder builder = HttpClient.newBuilder();
        if (proxyHost != null && !proxyHost.isBlank() && proxyPort > 0 && proxyPort <= 65_535) {
            builder.proxy(ProxySelector.of(new InetSocketAddress(proxyHost.trim(), proxyPort)));
        }
        return builder;
    }

    private String normalizeCategory(String category) {
        if (category == null) return "OTHER";
        return switch (category.trim().toUpperCase(Locale.ROOT)) {
            case "FOOD" -> "FOOD";
            case "OCR", "DOCUMENT", "REPORT" -> "OCR";
            default -> "OTHER";
        };
    }

    private String defaultTitle(String category) {
        return switch (category) {
            case "FOOD" -> "饮食记录";
            case "OCR" -> "文字识别记录";
            default -> "照片记录";
        };
    }

    private Double nonNegative(Double value, double maximum) {
        if (value == null || !Double.isFinite(value)) return null;
        return Math.max(0, Math.min(maximum, value));
    }

    private String extractJson(String answer) {
        String value = answer.strip();
        if (value.startsWith("```")) {
            int firstLine = value.indexOf('\n');
            int closing = value.lastIndexOf("```");
            if (firstLine >= 0 && closing > firstLine) value = value.substring(firstLine + 1, closing).strip();
        }
        int start = value.indexOf('{');
        int end = value.lastIndexOf('}');
        if (start < 0 || end <= start) throw new BehaviorPhotoAnalysisException("vision provider did not return JSON");
        return value.substring(start, end + 1);
    }

    private String resolveSecret(String configured, String secretFile) {
        if (configured != null && !configured.isBlank()) return configured.trim();
        if (secretFile == null || secretFile.isBlank()) return "";
        try {
            return Files.readString(Path.of(secretFile.trim())).trim();
        } catch (IOException failure) {
            throw new IllegalStateException("vision provider credential file is unreadable", failure);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("/+$", "");
    }

    private String nonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.strip();
    }

    private String bounded(String value, int limit) {
        return value.length() <= limit ? value : value.substring(0, limit);
    }

    private static final String SYSTEM_PROMPT = """
            你是睿禾健康的图像记录解析器。只分析图中可见内容，不诊断疾病，不把不确定内容当事实。
            必须只返回一个 JSON 对象，禁止 Markdown。category 只能是 FOOD、OCR 或 OTHER。
            可见餐食、饮料或主要用于记录饮食的食品包装优先归为 FOOD；否则，只要图片的主要可用
            信息是可辨文字（包括截图、票据、标签、文档或报告）就归为 OCR，不要归为 OTHER。
            食物照片估算热量和三大营养素时必须保守，并在 summary 明确这是图像估算；无法可靠估算时填 null。
            文档或报告照片只做 OCR 和客观摘要，不解释为医疗诊断。
            """;

    private static final String USER_PROMPT = """
            将照片识别为食物、以可辨文字为主的截图/票据/标签/文档/报告或其他内容，并返回：
            {"category":"FOOD|OCR|OTHER","title":"简短标题","summary":"客观摘要",
             "items":["识别到的主要项目"],"caloriesKcal":null,"proteinGrams":null,
             "carbohydrateGrams":null,"fatGrams":null,"ocrText":null,"confidence":0.0}
            FOOD 时填写可见食物和合理的区间中值估算；OCR 时填写完整可辨文字，无法辨认的部分不要编造。
            """;

    public static class VisionPayload {
        public String category;
        public String title;
        public String summary;
        public List<String> items;
        public Double caloriesKcal;
        public Double proteinGrams;
        public Double carbohydrateGrams;
        public Double fatGrams;
        public String ocrText;
        public Double confidence;
    }
}
