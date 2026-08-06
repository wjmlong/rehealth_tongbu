package org.jeecg.config;

import org.jeecg.common.constant.enums.DySmsEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AliyunSmsPropertiesTest {

    @TempDir
    Path tempDir;

    @Test
    void resolvesDedicatedRegistrationConfiguration() {
        AliyunSmsProperties properties = configuredProperties();

        AliyunSmsProperties.Resolved resolved = properties.resolve(DySmsEnum.REGISTER_TEMPLATE_CODE);

        assertEquals("sms-ak-id", resolved.accessKeyId());
        assertEquals("sms-ak-secret", resolved.accessKeySecret());
        assertEquals("ReHealth", resolved.signName());
        assertEquals("SMS_REGISTER", resolved.templateCode());
    }

    @Test
    void secretFilesTakePrecedenceOverInlineValues() throws IOException {
        AliyunSmsProperties properties = configuredProperties();
        Path idFile = tempDir.resolve("access-key-id");
        Path secretFile = tempDir.resolve("access-key-secret");
        Files.writeString(idFile, " file-ak-id \n");
        Files.writeString(secretFile, " file-ak-secret \n");
        properties.setAccessKeyIdFile(idFile.toString());
        properties.setAccessKeySecretFile(secretFile.toString());

        AliyunSmsProperties.Resolved resolved = properties.resolve(DySmsEnum.REGISTER_TEMPLATE_CODE);

        assertEquals("file-ak-id", resolved.accessKeyId());
        assertEquals("file-ak-secret", resolved.accessKeySecret());
    }

    @Test
    void failsClosedWhenProviderOrRequiredTemplateIsMissing() {
        AliyunSmsProperties disabled = configuredProperties();
        disabled.setEnabled(false);
        assertThrows(
                IllegalStateException.class,
                () -> disabled.resolve(DySmsEnum.REGISTER_TEMPLATE_CODE)
        );

        AliyunSmsProperties missingTemplate = configuredProperties();
        missingTemplate.setRegisterTemplateCode(" ");
        assertThrows(
                IllegalStateException.class,
                () -> missingTemplate.resolve(DySmsEnum.REGISTER_TEMPLATE_CODE)
        );
    }

    @Test
    void mapsLoginAndPasswordTemplatesIndependently() {
        AliyunSmsProperties properties = configuredProperties();

        assertEquals(
                "SMS_LOGIN",
                properties.resolve(DySmsEnum.FORGET_PASSWORD_TEMPLATE_CODE).templateCode()
        );
        assertEquals(
                "SMS_CHANGE_PASSWORD",
                properties.resolve(DySmsEnum.CHANGE_PASSWORD_TEMPLATE_CODE).templateCode()
        );
    }

    private AliyunSmsProperties configuredProperties() {
        AliyunSmsProperties properties = new AliyunSmsProperties();
        properties.setEnabled(true);
        properties.setAccessKeyId("sms-ak-id");
        properties.setAccessKeySecret("sms-ak-secret");
        properties.setSignName("ReHealth");
        properties.setRegisterTemplateCode("SMS_REGISTER");
        properties.setLoginTemplateCode("SMS_LOGIN");
        properties.setChangePasswordTemplateCode("SMS_CHANGE_PASSWORD");
        return properties;
    }
}
