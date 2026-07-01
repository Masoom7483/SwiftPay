package com.swiftpay.ledger.entity;

import com.swiftpay.common.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

/**
 * Ledger-side view of the {@code payment_transactions} row that the gateway
 * created with status PENDING. The ledger updates its {@code status} to
 * COMPLETED / FAILED inside the same atomic transfer transaction.
 *
 * <p>Only the columns the ledger needs to update are mapped here — the gateway
 * owns the full entity; on UPDATE Hibernate only touches the mapped columns and
 * leaves the rest untouched.
 */
@Entity
@Table(name = "payment_transactions")
public class PaymentTransaction {

    @Id
    @Column(name = "transaction_id", length = 64)
    private String transactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private PaymentStatus status;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Same optimistic-lock column the gateway writes — kept in sync. */
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected PaymentTransaction() {
    }

    public String getTransactionId() {
        return transactionId;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    /** Transition the payment status and stamp the update time. */
    public void markStatus(PaymentStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = Instant.now();
    }
}
