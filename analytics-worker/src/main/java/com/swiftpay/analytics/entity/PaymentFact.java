package com.swiftpay.analytics.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Denormalized fact row for volume analytics — the "mock analytics table" from
 * the spec. In a real OLAP setup this would be a ClickHouse MergeTree table.
 */
@Entity
@Table(name = "payment_facts")
public class PaymentFact {

    @Id
    @Column(name = "transaction_id", length = 64)
    private String transactionId;

    @Column(name = "amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    protected PaymentFact() {
    }

    public PaymentFact(String transactionId, BigDecimal amount, String currency, Instant completedAt) {
        this.transactionId = transactionId;
        this.amount = amount;
        this.currency = currency;
        this.completedAt = completedAt;
    }

    public String getTransactionId() {
        return transactionId;
    }
}
