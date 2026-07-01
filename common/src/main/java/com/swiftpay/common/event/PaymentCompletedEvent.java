package com.swiftpay.common.event;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Event published to {@code payment.completed} after the ledger successfully
 * applies the debit/credit. Consumed by the gateway (status update) and the
 * analytics worker (OLAP volume monitoring).
 */
public record PaymentCompletedEvent(
        @JsonProperty("transaction_id") String transactionId,
        @JsonProperty("sender_id") String senderId,
        @JsonProperty("receiver_id") String receiverId,
        @JsonProperty("amount") BigDecimal amount,
        @JsonProperty("currency") String currency,
        @JsonProperty("completed_at") Instant completedAt
) {
}
