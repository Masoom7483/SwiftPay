package com.swiftpay.common.event;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Event published to {@code payment.failed} when the ledger cannot apply a
 * transfer (insufficient funds, constraint violation, unknown account, ...).
 * The {@code reason} is a machine-readable code for downstream handling.
 */
public record PaymentFailedEvent(
        @JsonProperty("transaction_id") String transactionId,
        @JsonProperty("sender_id") String senderId,
        @JsonProperty("receiver_id") String receiverId,
        @JsonProperty("reason") String reason,
        @JsonProperty("failed_at") Instant failedAt
) {
}
