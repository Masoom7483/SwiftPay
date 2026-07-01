package com.swiftpay.gateway.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;


@Service
public class IdempotencyService {

    private static final Duration WINDOW = Duration.ofHours(24);
    private static final String KEY_PREFIX = "idempotency:payment:";

    private final StringRedisTemplate redis;

    public IdempotencyService(StringRedisTemplate redis) {
        this.redis = redis;
    }


    public boolean tryReserve(String transactionId) {
        Boolean firstSeen = redis.opsForValue()
                .setIfAbsent(KEY_PREFIX + transactionId, "1", WINDOW);
        return Boolean.TRUE.equals(firstSeen);
    }

    public void release(String transactionId) {
        redis.delete(KEY_PREFIX + transactionId);
    }
}
