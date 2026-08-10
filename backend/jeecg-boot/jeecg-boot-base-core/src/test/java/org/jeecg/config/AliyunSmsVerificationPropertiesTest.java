package org.jeecg.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AliyunSmsVerificationPropertiesTest {

    @TempDir
    Path tempDir;

    @Test
    void resolvesConfirmedRegistrationTemplateDefaults() {
        AliyunSmsVerificationProperties properties = configuredProperties();

        AliyunSmsVerificationProperties.Resolved resolved = properties.resolve();

        assertEquals("100001", resolved.registerTemplateCode());
        assertEquals("rehealth-register", resolved.schemeName());
        assertEquals(6, resolved.codeLength());
        assertEquals(5, resolved.validMinutes());
        assertEquals(300L, resolved.validTimeSeconds());
        assertEquals(60, resolved.intervalSeconds());
    }

    @Test
    void secretFilesTakePrecedenceOverInlineValues() throws IOException {
        AliyunSmsVerificationProperties properties = configuredProperties();
        Path idFile = tempDir.resolve("access-key-id");
        Path secretFile = tempDir.resolve("access-key-secret");
        Files.writeString(idFile, " file-dypns-id \n");
        Files.writeString(secretFile, " file-dypns-secret \n");
        properties.setAccessKeyIdFile(idFile.toString());
        properties.setAccessKeySecretFile(secretFile.toString());

        AliyunSmsVerificationProperties.Resolved resolved = properties.resolve();

        assertEquals("file-dypns-id", resolved.accessKeyId());
        assertEquals("file-dypns-secret", resolved.accessKeySecret());
    }

    @Test
    void failsClosedWhenDisabledOrRequiredSignIsMissing() {
        AliyunSmsVerificationProperties disabled = configuredProperties();
        disabled.setEnabled(false);
        assertThrows(IllegalStateException.class, disabled::resolve);

        AliyunSmsVerificationProperties missingSign = configuredProperties();
        missingSign.setSignName(" ");
        assertThrows(IllegalStateException.class, missingSign::resolve);
    }

    @Test
    void rejectsUnsupportedCodeLengthAndOverlongScheme() {
        AliyunSmsVerificationProperties invalidLength = configuredProperties();
        invalidLength.setCodeLength(9);
        assertThrows(IllegalStateException.class, invalidLength::resolve);

        AliyunSmsVerificationProperties invalidScheme = configuredProperties();
        invalidScheme.setSchemeName("registration-scheme-over-20");
        assertThrows(IllegalStateException.class, invalidScheme::resolve);
    }

    private AliyunSmsVerificationProperties configuredProperties() {
        AliyunSmsVerificationProperties properties = new AliyunSmsVerificationProperties();
        properties.setEnabled(true);
        properties.setAccessKeyId("dypns-ak-id");
        properties.setAccessKeySecret("dypns-ak-secret");
        properties.setSignName("系统赠送签名");
        return properties;
    }
}
