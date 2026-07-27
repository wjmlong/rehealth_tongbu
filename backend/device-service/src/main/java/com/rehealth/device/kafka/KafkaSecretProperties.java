package com.rehealth.device.kafka;

import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@ConfigurationProperties(prefix = "rehealth.kafka.security")
public final class KafkaSecretProperties {
    private Path saslJaasConfigFile;
    private Path sslTruststorePasswordFile;

    public Path getSaslJaasConfigFile() {
        return saslJaasConfigFile;
    }

    public void setSaslJaasConfigFile(Path saslJaasConfigFile) {
        this.saslJaasConfigFile = saslJaasConfigFile;
    }

    public Path getSslTruststorePasswordFile() {
        return sslTruststorePasswordFile;
    }

    public void setSslTruststorePasswordFile(Path sslTruststorePasswordFile) {
        this.sslTruststorePasswordFile = sslTruststorePasswordFile;
    }

    void applyTo(Map<String, Object> properties) {
        putSecret(properties, SaslConfigs.SASL_JAAS_CONFIG, saslJaasConfigFile);
        putSecret(properties, SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, sslTruststorePasswordFile);
    }

    private void putSecret(Map<String, Object> properties, String key, Path path) {
        if (path == null) {
            return;
        }
        try {
            String value = Files.readString(path, StandardCharsets.UTF_8).strip();
            if (value.isEmpty()) {
                throw new IllegalStateException("Kafka secret file is empty: " + path);
            }
            properties.put(key, value);
        } catch (IOException exception) {
            throw new IllegalStateException("Kafka secret file is unreadable: " + path, exception);
        }
    }
}
