package com.swiftpay.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Service A — Transaction Gateway.
 *
 * <p>Exposes the public payment REST API, enforces idempotency via Redis,
 * validates the request, persists a PENDING row to PostgreSQL and emits a
 * {@code PaymentInitiated} event to Kafka.
 */
@SpringBootApplication
public class TransactionGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransactionGatewayApplication.class, args);
    }
}
