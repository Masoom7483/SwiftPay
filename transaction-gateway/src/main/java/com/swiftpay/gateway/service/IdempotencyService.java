package com.swiftpay.gateway.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis-backed idempotency guard. Uses an atomic {@code SET key value NX EX 86400}
 * so the very first request for a {@code transactionId} claims the key; any repeat
 * within 24h is rejected. This is the fast first line of defence — the PostgreSQL
 * primary key on {@code transaction_id} is the durable backstop.
 */
@Service
public class IdempotencyService {

    /** 24-hour window mandated by the spec. */
    private static final Duration WINDOW = Duration.ofHours(24);
    private static final String KEY_PREFIX = "idempotency:payment:";

    private final StringRedisTemplate redis;

    public IdempotencyService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * Attempts to reserve the given transaction id.
     *
     * @return {@code true} if this is the first time we've seen the id (caller may
     *         proceed), {@code false} if it was already reserved (duplicate).
     */
    public boolean tryReserve(String transactionId) {
        Boolean firstSeen = redis.opsForValue()
                .setIfAbsent(KEY_PREFIX + transactionId, "1", WINDOW);
        return Boolean.TRUE.equals(firstSeen);
    }

    /**
     * Releases a previously reserved id. Called when downstream persistence fails,
     * so a genuinely retried request isn't wrongly treated as a duplicate.
     */
    public void release(String transactionId) {
        redis.delete(KEY_PREFIX + transactionId);
    }
}
