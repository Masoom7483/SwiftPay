package com.swiftpay.common.event;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;


public record PaymentFailedEvent(
        @JsonProperty("transaction_id") String transactionId,
        @JsonProperty("sender_id") String senderId,
        @JsonProperty("receiver_id") String receiverId,
        @JsonProperty("reason") String reason,
        @JsonProperty("failed_at") Instant failedAt
) {
}
