package org.jeecg.modules.rehealth.viomi;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ViomiPlanSecretCodecTest {
    @Test
    void encryptsAndDecryptsImeiWithoutPersistingPlaintext() {
        ViomiAdapterProperties properties = mock(ViomiAdapterProperties.class);
        when(properties.getPlanEncryptionSecret()).thenReturn("test-only-plan-secret");
        ViomiPlanSecretCodec codec = new ViomiPlanSecretCodec(properties);

        byte[] encrypted = codec.encrypt("123456789012345");

        assertFalse(new String(encrypted, StandardCharsets.UTF_8).contains("123456789012345"));
        assertEquals("123456789012345", codec.decrypt(encrypted));
    }

    @Test
    void rejectsPlanStorageWhenEncryptionSecretIsMissing() {
        ViomiAdapterProperties properties = mock(ViomiAdapterProperties.class);
        when(properties.getPlanEncryptionSecret()).thenReturn("");
        ViomiPlanSecretCodec codec = new ViomiPlanSecretCodec(properties);

        assertThrows(IllegalStateException.class, () -> codec.encrypt("123456789012345"));
    }
}
