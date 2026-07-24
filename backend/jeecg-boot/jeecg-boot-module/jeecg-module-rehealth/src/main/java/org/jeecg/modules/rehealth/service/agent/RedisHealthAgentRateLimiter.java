package org.jeecg.modules.rehealth.service.agent;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@Component
public class RedisHealthAgentRateLimiter implements HealthAgentRateLimiter {
    private static final DefaultRedisScript<Long> WINDOW_SCRIPT = new DefaultRedisScript<>("""
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end
            return current
            """, Long.class);

    private final StringRedisTemplate redis;
    private final int limit;
    private final int windowSeconds;

    public RedisHealthAgentRateLimiter(
            StringRedisTemplate redis,
            @Value("${rehealth.health-agent.rate-limit.requests:20}") int limit,
            @Value("${rehealth.health-agent.rate-limit.window-seconds:60}") int windowSeconds
    ) {
        this.redis = redis;
        this.limit = Math.max(1, limit);
        this.windowSeconds = Math.max(1, windowSeconds);
    }

    @Override
    public HealthAgentRateLimitDecision acquire(String tenantId, String userId) {
        try {
            Long count = redis.execute(
                    WINDOW_SCRIPT,
                    List.of("rehealth:agent:rate:" + subjectHash(tenantId, userId)),
                    Integer.toString(windowSeconds)
            );
            if (count == null) {
                return HealthAgentRateLimitDecision.unavailable();
            }
            return count <= limit
                    ? HealthAgentRateLimitDecision.allowedDecision()
                    : HealthAgentRateLimitDecision.exceeded();
        } catch (RedisConnectionFailureException unavailable) {
            return HealthAgentRateLimitDecision.unavailable();
        } catch (RuntimeException unavailable) {
            return HealthAgentRateLimitDecision.unavailable();
        }
    }

    private String subjectHash(String tenantId, String userId) {
        try {
            byte[] value = (tenantId + "|" + userId).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException unavailable) {
            throw new IllegalStateException("SHA-256 is unavailable", unavailable);
        }
    }
}
