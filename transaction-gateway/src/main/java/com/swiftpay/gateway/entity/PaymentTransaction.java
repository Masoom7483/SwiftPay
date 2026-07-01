package com.swiftpay.gateway.entity;

import com.swiftpay.common.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Persistent record of a payment as seen by the gateway. The {@code transactionId}
 * is the primary key, which doubles as a DB-level idempotency guard (a duplicate
 * insert violates the PK constraint).
 */
@Entity
@Table(name = "payment_transactions", indexes = {
        @Index(name = "idx_payment_sender", columnList = "sender_id"),
        @Index(name = "idx_payment_receiver", columnList = "receiver_id")
})
public class PaymentTransaction {

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

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Optimistic lock — protects status transitions applied by the ledger. */
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected PaymentTransaction() {
        // required by JPA
    }

    public PaymentTransaction(String transactionId, String senderId, String receiverId,
                              BigDecimal amount, String currency, PaymentStatus status,
                              Instant createdAt) {
        this.transactionId = transactionId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
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

    public void setStatus(PaymentStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }
}
