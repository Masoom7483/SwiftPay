package com.swiftpay.ledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Service B — Ledger Service.
 *
 * <p>Consumes {@code PaymentInitiated} events, applies the debit/credit inside a
 * single database transaction, emits {@code PaymentCompleted}/{@code PaymentFailed},
 * and exposes a per-user transaction history endpoint for reporting.
 */
@SpringBootApplication
public class LedgerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LedgerServiceApplication.class, args);
    }
}
