package com.swiftpay.common.event;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;


public record PaymentCompletedEvent(
        @JsonProperty("transaction_id") String transactionId,
        @JsonProperty("sender_id") String senderId,
        @JsonProperty("receiver_id") String receiverId,
        @JsonProperty("amount") BigDecimal amount,
        @JsonProperty("currency") String currency,
        @JsonProperty("completed_at") Instant completedAt
) {
}
