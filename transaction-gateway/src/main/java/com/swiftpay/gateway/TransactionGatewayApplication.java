package com.swiftpay.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Service A — Transaction Gateway.
 *
 * <p>Exposes the public payment REST API, enforces idempotency via Redis,
 * validates the request, persists a PENDING row to PostgreSQL and queues a
 * {@code PaymentInitiated} event through the transactional outbox.
 */
@SpringBootApplication
@EnableScheduling
public class TransactionGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransactionGatewayApplication.class, args);
    }
}
