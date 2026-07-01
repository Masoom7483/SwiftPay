package com.swiftpay.common.event;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Event published to {@code payment.initiated} when the gateway persists a
 * PENDING payment. The ledger service consumes this to apply the transfer.
 *
 * <p>Events are immutable records — the JSON contract is the source of truth
 * shared between services, so field names must stay stable.
 */
public record PaymentInitiatedEvent(
        @JsonProperty("transaction_id") String transactionId,
        @JsonProperty("sender_id") String senderId,
        @JsonProperty("receiver_id") String receiverId,
        @JsonProperty("amount") BigDecimal amount,
        @JsonProperty("currency") String currency,
        @JsonProperty("occurred_at") Instant occurredAt
) {
}
