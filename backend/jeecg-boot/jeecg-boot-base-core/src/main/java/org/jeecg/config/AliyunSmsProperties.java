package org.jeecg.config;

import lombok.Data;
import org.jeecg.common.constant.enums.DySmsEnum;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Dedicated server-side Aliyun SMS configuration.
 *
 * <p>Secret files take precedence over inline environment values so production can mount
 * Docker/Kubernetes secrets without exposing credentials in tracked YAML or process arguments.</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "jeecg.sms.aliyun")
public class AliyunSmsProperties {

    private boolean enabled;
    private String accessKeyId;
    private String accessKeySecret;
    private String accessKeyIdFile;
    private String accessKeySecretFile;
    private String signName;
    private String registerTemplateCode;
    private String loginTemplateCode;
    private String changePasswordTemplateCode;

    public Resolved resolve(DySmsEnum template) {
        if (!enabled) {
            throw new IllegalStateException("Aliyun SMS is disabled");
        }
        return new Resolved(
                resolveSecret(accessKeyId, accessKeyIdFile, "access key id"),
                resolveSecret(accessKeySecret, accessKeySecretFile, "access key secret"),
                requireText(signName, "sign name"),
                requireText(templateCodeFor(template), templateLabel(template))
        );
    }

    String templateCodeFor(DySmsEnum template) {
        return switch (template) {
            case REGISTER_TEMPLATE_CODE -> registerTemplateCode;
            case LOGIN_TEMPLATE_CODE, FORGET_PASSWORD_TEMPLATE_CODE -> loginTemplateCode;
            case CHANGE_PASSWORD_TEMPLATE_CODE -> changePasswordTemplateCode;
        };
    }

    private String templateLabel(DySmsEnum template) {
        return switch (template) {
            case REGISTER_TEMPLATE_CODE -> "register template code";
            case LOGIN_TEMPLATE_CODE, FORGET_PASSWORD_TEMPLATE_CODE -> "login template code";
            case CHANGE_PASSWORD_TEMPLATE_CODE -> "change-password template code";
        };
    }

    private String resolveSecret(String inlineValue, String fileName, String label) {
        String pathValue = trimToNull(fileName);
        if (pathValue != null) {
            try {
                return requireText(
                        Files.readString(Path.of(pathValue), StandardCharsets.UTF_8),
                        label + " file"
                );
            } catch (IOException | RuntimeException exception) {
                throw new IllegalStateException("Unable to read Aliyun SMS " + label + " file", exception);
            }
        }
        return requireText(inlineValue, label);
    }

    private String requireText(String value, String label) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new IllegalStateException("Missing Aliyun SMS " + label);
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    public record Resolved(
            String accessKeyId,
            String accessKeySecret,
            String signName,
            String templateCode
    ) {
    }
}
