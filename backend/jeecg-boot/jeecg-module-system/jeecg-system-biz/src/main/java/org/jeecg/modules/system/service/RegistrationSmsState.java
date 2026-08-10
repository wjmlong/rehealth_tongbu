package org.jeecg.modules.system.service;

import com.alibaba.fastjson.JSONObject;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/** Cluster-safe registration SMS cooldown, quota, session, and registration lock state. */
@Component
public class RegistrationSmsState {

    private static final String PREFIX = "rehealth:sms:register:";
    private static final int PHONE_DAILY_LIMIT = 10;
    private static final int IP_MINUTE_LIMIT = 5;
    private static final int IP_DAILY_LIMIT = 100;
    private static final Duration REGISTRATION_LOCK_TTL = Duration.ofSeconds(30);

    private static final DefaultRedisScript<Long> INCREMENT_WITH_EXPIRY = new DefaultRedisScript<>(
            "local count = redis.call('INCR', KEYS[1]); "
                    + "if count == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]); end; "
                    + "return count;",
            Long.class
    );
    private static final DefaultRedisScript<Long> RELEASE_LOCK = new DefaultRedisScript<>(
            "if redis.call('GET', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('DEL', KEYS[1]); else return 0; end;",
            Long.class
    );

    private final StringRedisTemplate redisTemplate;

    public RegistrationSmsState(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean allowSend(String phoneNumber, String clientIp) {
        Boolean cooldownAcquired = redisTemplate.opsForValue().setIfAbsent(
                cooldownKey(phoneNumber),
                "pending",
                Duration.ofSeconds(60)
        );
        if (!Boolean.TRUE.equals(cooldownAcquired)) {
            return false;
        }
        long phoneDaily = increment(rateKey("phone:day", phoneNumber), Duration.ofDays(1));
        long ipMinute = increment(rateKey("ip:minute", clientIp), Duration.ofMinutes(1));
        long ipDaily = increment(rateKey("ip:day", clientIp), Duration.ofDays(1));
        return phoneDaily <= PHONE_DAILY_LIMIT
                && ipMinute <= IP_MINUTE_LIMIT
                && ipDaily <= IP_DAILY_LIMIT;
    }

    public void markSent(
            String phoneNumber,
            String provider,
            String outId,
            int validSeconds,
            int intervalSeconds
    ) {
        JSONObject session = new JSONObject();
        session.put("provider", provider);
        if (outId != null && !outId.isBlank()) {
            session.put("outId", outId);
        }
        redisTemplate.opsForValue().set(
                sessionKey(phoneNumber),
                session.toJSONString(),
                Duration.ofSeconds(validSeconds)
        );
        redisTemplate.opsForValue().set(
                cooldownKey(phoneNumber),
                "1",
                Duration.ofSeconds(intervalSeconds)
        );
    }

    public Session getSession(String phoneNumber) {
        String raw = redisTemplate.opsForValue().get(sessionKey(phoneNumber));
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            JSONObject json = JSONObject.parseObject(raw);
            return new Session(json.getString("provider"), json.getString("outId"));
        } catch (RuntimeException exception) {
            return null;
        }
    }

    public void clearSession(String phoneNumber) {
        redisTemplate.delete(sessionKey(phoneNumber));
    }

    public String tryAcquireRegistrationLock(String phoneNumber) {
        String token = UUID.randomUUID().toString();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                lockKey(phoneNumber),
                token,
                REGISTRATION_LOCK_TTL
        );
        return Boolean.TRUE.equals(acquired) ? token : null;
    }

    public void releaseRegistrationLock(String phoneNumber, String token) {
        if (token != null) {
            redisTemplate.execute(RELEASE_LOCK, List.of(lockKey(phoneNumber)), token);
        }
    }

    private long increment(String key, Duration ttl) {
        Long count = redisTemplate.execute(
                INCREMENT_WITH_EXPIRY,
                List.of(key),
                Long.toString(ttl.toSeconds())
        );
        return count == null ? Long.MAX_VALUE : count;
    }

    private static String sessionKey(String phoneNumber) {
        return PREFIX + "session:" + hash(phoneNumber);
    }

    private static String cooldownKey(String phoneNumber) {
        return PREFIX + "cooldown:" + hash(phoneNumber);
    }

    private static String lockKey(String phoneNumber) {
        return PREFIX + "lock:" + hash(phoneNumber);
    }

    private static String rateKey(String window, String value) {
        return PREFIX + "rate:" + window + ":" + hash(value);
    }

    private static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public record Session(String provider, String outId) {
    }
}
