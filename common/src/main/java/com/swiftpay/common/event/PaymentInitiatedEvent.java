package com.swiftpay.common.event;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;


public record PaymentInitiatedEvent(
        @JsonProperty("transaction_id") String transactionId,
        @JsonProperty("sender_id") String senderId,
        @JsonProperty("receiver_id") String receiverId,
        @JsonProperty("amount") BigDecimal amount,
        @JsonProperty("currency") String currency,
        @JsonProperty("occurred_at") Instant occurredAt
) {
}
