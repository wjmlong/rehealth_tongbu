package org.jeecg.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Server-only Alibaba Cloud Phone Number Verification Service configuration.
 *
 * <p>Dypnsapi signatures/templates are isolated from standard Dysmsapi resources. Secret files
 * take precedence over inline values so production can mount credentials without placing them in
 * tracked YAML, process arguments, or the Android package.</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "jeecg.sms.dypns")
public class AliyunSmsVerificationProperties {

    private boolean enabled;
    private String accessKeyId;
    private String accessKeySecret;
    private String accessKeyIdFile;
    private String accessKeySecretFile;
    private String endpoint = "dypnsapi.aliyuncs.com";
    private String countryCode = "86";
    private String signName;
    private String registerTemplateCode = "100001";
    private String schemeName = "rehealth-register";
    private int codeLength = 6;
    private int validMinutes = 5;
    private int intervalSeconds = 60;

    public Resolved resolve() {
        if (!enabled) {
            throw new IllegalStateException("Aliyun SMS verification is disabled");
        }
        if (codeLength < 4 || codeLength > 8) {
            throw new IllegalStateException("Aliyun SMS verification code length must be between 4 and 8");
        }
        if (validMinutes <= 0) {
            throw new IllegalStateException("Aliyun SMS verification validity must be positive");
        }
        if (intervalSeconds <= 0) {
            throw new IllegalStateException("Aliyun SMS verification interval must be positive");
        }

        String resolvedSchemeName = requireText(schemeName, "scheme name");
        if (resolvedSchemeName.length() > 20) {
            throw new IllegalStateException("Aliyun SMS verification scheme name must not exceed 20 characters");
        }

        return new Resolved(
                resolveSecret(accessKeyId, accessKeyIdFile, "access key id"),
                resolveSecret(accessKeySecret, accessKeySecretFile, "access key secret"),
                requireText(endpoint, "endpoint"),
                requireText(countryCode, "country code"),
                requireText(signName, "sign name"),
                requireText(registerTemplateCode, "register template code"),
                resolvedSchemeName,
                codeLength,
                validMinutes,
                intervalSeconds
        );
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
                throw new IllegalStateException(
                        "Unable to read Aliyun SMS verification " + label + " file",
                        exception
                );
            }
        }
        return requireText(inlineValue, label);
    }

    private String requireText(String value, String label) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new IllegalStateException("Missing Aliyun SMS verification " + label);
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
            String endpoint,
            String countryCode,
            String signName,
            String registerTemplateCode,
            String schemeName,
            int codeLength,
            int validMinutes,
            int intervalSeconds
    ) {
        public long validTimeSeconds() {
            return validMinutes * 60L;
        }
    }
}
