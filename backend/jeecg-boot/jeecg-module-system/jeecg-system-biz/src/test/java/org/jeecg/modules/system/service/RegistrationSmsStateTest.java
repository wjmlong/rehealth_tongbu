package org.jeecg.modules.system.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegistrationSmsStateTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> values;
    private RegistrationSmsState state;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        values = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(values);
        state = new RegistrationSmsState(redisTemplate);
    }

    @Test
    void storesOnlyOpaqueSessionUnderHashedPhoneKey() {
        state.markSent("13800138000", "dypns", "out-1", 300, 60);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(values, times(2)).set(keyCaptor.capture(), valueCaptor.capture(), ttlCaptor.capture());

        String key = keyCaptor.getAllValues().get(0);
        String value = valueCaptor.getAllValues().get(0);
        assertFalse(key.contains("13800138000"));
        assertFalse(value.contains("13800138000"));
        assertFalse(value.contains("123456"));
        assertTrue(value.contains("dypns"));
        assertTrue(value.contains("out-1"));
        assertEquals(Duration.ofSeconds(300), ttlCaptor.getAllValues().get(0));
    }

    @Test
    void rejectsConcurrentSendWhenCooldownWasAlreadyAcquired() {
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        assertFalse(state.allowSend("13800138000", "127.0.0.1"));

        verify(redisTemplate, never()).execute(any(), any(), anyString());
    }

    @Test
    void restoresOpaqueProviderSession() {
        when(values.get(anyString())).thenReturn("{\"provider\":\"dypns\",\"outId\":\"out-1\"}");

        RegistrationSmsState.Session session = state.getSession("13800138000");

        assertNotNull(session);
        assertEquals("dypns", session.provider());
        assertEquals("out-1", session.outId());
    }
}
