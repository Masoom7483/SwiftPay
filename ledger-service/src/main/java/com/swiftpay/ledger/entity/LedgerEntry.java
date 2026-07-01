package com.swiftpay.ledger.entity;

import com.swiftpay.common.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Immutable audit record of an applied (or attempted) transfer. One row per
 * transaction id; this is the source for the per-user reporting endpoint and
 * doubles as the ledger-side idempotency guard.
 */
@Entity
@Table(name = "ledger_entries", indexes = {
        @Index(name = "idx_ledger_sender", columnList = "sender_id"),
        @Index(name = "idx_ledger_receiver", columnList = "receiver_id")
})
public class LedgerEntry {

    @Id
    @Column(name = "transaction_id", nullable = false, updatable = false, length = 64)
    private String transactionId;

    @Column(name = "sender_id", nullable = false, length = 64)
    private String senderId;

    @Column(name = "receiver_id", nullable = false, length = 64)
    private String receiverId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private PaymentStatus status;

    @Column(name = "failure_reason", length = 128)
    private String failureReason;

    @Column(name = "applied_at", nullable = false, updatable = false)
    private Instant appliedAt;

    protected LedgerEntry() {
    }

    public LedgerEntry(String transactionId, String senderId, String receiverId, BigDecimal amount,
                       String currency, PaymentStatus status, String failureReason, Instant appliedAt) {
        this.transactionId = transactionId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.failureReason = failureReason;
        this.appliedAt = appliedAt;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getAppliedAt() {
        return appliedAt;
    }
}
