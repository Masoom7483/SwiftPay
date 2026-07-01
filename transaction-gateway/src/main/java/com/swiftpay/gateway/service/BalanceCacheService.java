package com.swiftpay.gateway.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftpay.gateway.entity.Account;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

@Service
public class BalanceCacheService {

    private static final Logger log = LoggerFactory.getLogger(BalanceCacheService.class);
    private static final String KEY_PREFIX = "balance:account:";
    private static final Duration TTL = Duration.ofSeconds(30);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public BalanceCacheService(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public Optional<CachedAccount> find(String accountId) {
        try {
            String payload = redis.opsForValue().get(KEY_PREFIX + accountId);
            if (payload == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(payload, CachedAccount.class));
        } catch (RuntimeException | JsonProcessingException ex) {
            log.warn("Unable to read balance cache for account {}", accountId, ex);
            return Optional.empty();
        }
    }

    public void cache(Account account) {
        try {
            CachedAccount cached = CachedAccount.from(account);
            redis.opsForValue().set(
                    KEY_PREFIX + account.getAccountId(),
                    objectMapper.writeValueAsString(cached),
                    TTL);
        } catch (RuntimeException | JsonProcessingException ex) {
            log.warn("Unable to write balance cache for account {}", account.getAccountId(), ex);
        }
    }

    public record CachedAccount(String accountId, BigDecimal balance, String currency) {

        public static CachedAccount from(Account account) {
            return new CachedAccount(account.getAccountId(), account.getBalance(), account.getCurrency());
        }

        public boolean canDebit(BigDecimal amount) {
            return balance.compareTo(amount) >= 0;
        }
    }
}
