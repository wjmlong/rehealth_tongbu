package org.jeecg.modules.rehealth.kafka;

import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReHealthKafkaSecretPropertiesTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void readsKafkaCredentialsOnlyFromMountedFiles() throws Exception {
        Path jaas = temporaryDirectory.resolve("jaas");
        Path password = temporaryDirectory.resolve("truststore-password");
        Files.writeString(jaas, "org.example.LoginModule required token=\"opaque\";\n");
        Files.writeString(password, "mounted-secret\n");
        ReHealthKafkaSecretProperties secrets = new ReHealthKafkaSecretProperties();
        secrets.setSaslJaasConfigFile(jaas);
        secrets.setSslTruststorePasswordFile(password);
        Map<String, Object> properties = new HashMap<>();

        secrets.applyTo(properties);

        assertEquals("org.example.LoginModule required token=\"opaque\";",
                properties.get(SaslConfigs.SASL_JAAS_CONFIG));
        assertEquals("mounted-secret",
                properties.get(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG));
    }
}
