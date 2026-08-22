package org.jeecg.modules.rehealth.viomi;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
final class ViomiPlanSecretCodec {
    private static final int IV_LENGTH = 12;
    private final byte[] key;
    private final SecureRandom random = new SecureRandom();

    ViomiPlanSecretCodec(ViomiAdapterProperties properties) {
        String secret = properties.getPlanEncryptionSecret();
        if (secret == null || secret.isBlank()) {
            this.key = null;
        } else {
            try {
                this.key = MessageDigest.getInstance("SHA-256")
                        .digest(secret.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                throw new IllegalStateException("unable to initialize Viomi plan encryption", e);
            }
        }
    }

    byte[] encrypt(String plaintext) {
        requireConfigured();
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.allocate(iv.length + ciphertext.length).put(iv).put(ciphertext).array();
        } catch (Exception e) {
            throw new IllegalStateException("unable to encrypt Viomi plan identity", e);
        }
    }

    String decrypt(byte[] value) {
        requireConfigured();
        try {
            byte[] iv = Arrays.copyOfRange(value, 0, IV_LENGTH);
            byte[] ciphertext = Arrays.copyOfRange(value, IV_LENGTH, value.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("unable to decrypt Viomi plan identity", e);
        }
    }

    private void requireConfigured() {
        if (key == null) {
            throw new IllegalStateException("REHEALTH_VIOMI_PLAN_ENCRYPTION_SECRET is not configured");
        }
    }
}
