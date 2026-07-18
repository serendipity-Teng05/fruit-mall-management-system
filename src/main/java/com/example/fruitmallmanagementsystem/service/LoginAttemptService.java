package com.example.fruitmallmanagementsystem.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {
    private final Map<String, Attempt> localAttempts = new ConcurrentHashMap<>();
    private final StringRedisTemplate redisTemplate;
    private final boolean distributedStateEnabled;
    private final int maxFailures;
    private final Duration lockDuration;

    /** 便于纯单元测试使用；Spring 运行时使用下方的注入构造器。 */
    public LoginAttemptService() {
        this.redisTemplate = null;
        this.distributedStateEnabled = false;
        this.maxFailures = 5;
        this.lockDuration = Duration.ofMinutes(15);
    }

    @Autowired
    public LoginAttemptService(ObjectProvider<StringRedisTemplate> redisProvider,
                               @Value("${app.security.distributed-state-enabled:false}") boolean distributedStateEnabled,
                               @Value("${app.security.login.max-failures:5}") int maxFailures,
                               @Value("${app.security.login.lock-minutes:15}") long lockMinutes) {
        this.redisTemplate = redisProvider.getIfAvailable();
        this.distributedStateEnabled = distributedStateEnabled;
        this.maxFailures = Math.max(3, maxFailures);
        this.lockDuration = Duration.ofMinutes(Math.max(1, lockMinutes));
    }

    public void assertAllowed(String account, String ip) {
        if (distributedStateEnabled) {
            assertAllowedDistributed(account, ip);
            return;
        }
        String key = key(account, ip);
        Attempt attempt = localAttempts.get(key);
        if (attempt == null) return;
        Instant now = Instant.now();
        if (now.isAfter(attempt.firstFailureAt().plus(lockDuration))) {
            localAttempts.remove(key);
            return;
        }
        if (attempt.failures() >= maxFailures) {
            long minutes = Math.max(1, Duration.between(now, attempt.firstFailureAt().plus(lockDuration)).toMinutes() + 1);
            throw locked(minutes);
        }
    }

    public void recordFailure(String account, String ip) {
        if (distributedStateEnabled) {
            executeRedis(() -> {
                String key = redisKey(account, ip);
                Long failures = redisTemplate.opsForValue().increment(key);
                if (failures != null && failures == 1L) redisTemplate.expire(key, lockDuration);
                return null;
            });
            return;
        }
        String key = key(account, ip);
        Instant now = Instant.now();
        localAttempts.compute(key, (ignored, current) -> {
            if (current == null || now.isAfter(current.firstFailureAt().plus(lockDuration))) {
                return new Attempt(1, now);
            }
            return new Attempt(current.failures() + 1, current.firstFailureAt());
        });
    }

    public void recordSuccess(String account, String ip) {
        if (distributedStateEnabled) {
            executeRedis(() -> redisTemplate.delete(redisKey(account, ip)));
            return;
        }
        localAttempts.remove(key(account, ip));
    }

    private void assertAllowedDistributed(String account, String ip) {
        executeRedis(() -> {
            String key = redisKey(account, ip);
            String value = redisTemplate.opsForValue().get(key);
            if (value != null && Integer.parseInt(value) >= maxFailures) {
                Long seconds = redisTemplate.getExpire(key);
                long minutes = seconds == null || seconds < 0 ? lockDuration.toMinutes()
                        : Math.max(1, (seconds + 59) / 60);
                throw locked(minutes);
            }
            return null;
        });
    }

    private <T> T executeRedis(RedisOperation<T> operation) {
        if (redisTemplate == null) {
            throw new IllegalStateException("登录安全服务未配置，请联系管理员");
        }
        try {
            return operation.execute();
        } catch (DataAccessException exception) {
            throw new IllegalStateException("登录安全服务暂不可用，请稍后再试");
        }
    }

    private IllegalStateException locked(long minutes) {
        return new IllegalStateException("登录失败次数过多，请 " + minutes + " 分钟后再试");
    }

    private String redisKey(String account, String ip) {
        return "fruit-mall:auth:fail:" + key(account, ip);
    }

    private String key(String account, String ip) {
        String value = (account == null ? "" : account.trim().toLowerCase())
                + "|" + (ip == null ? "unknown" : ip);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", impossible);
        }
    }

    private record Attempt(int failures, Instant firstFailureAt) {}

    @FunctionalInterface
    private interface RedisOperation<T> {
        T execute();
    }
}

