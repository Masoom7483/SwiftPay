package com.swiftpay.ledger.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.swiftpay.common.PaymentStatus;
import com.swiftpay.ledger.entity.LedgerEntry;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;


@Schema(name = "TransactionHistoryItem", description = "One entry in a user's transaction history")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TransactionHistoryItem(
        @Schema(example = "a3f1c2d4-5e6f-7a8b-9c0d-1e2f3a4b5c6d") String transactionId,
        @Schema(example = "DEBIT") String direction,
        @Schema(example = "acc_1001") String senderId,
        @Schema(example = "acc_2002") String receiverId,
        @Schema(example = "150.75") BigDecimal amount,
        @Schema(example = "USD") String currency,
        @Schema(example = "COMPLETED") PaymentStatus status,
        @Schema(description = "Why the transfer failed; null when COMPLETED",
                example = "CURRENCY_MISMATCH", nullable = true) String failureReason,
        @Schema(example = "2026-07-01T10:15:31Z") Instant appliedAt
) {
    public static TransactionHistoryItem from(LedgerEntry entry, String userId) {
        String direction = entry.getSenderId().equals(userId) ? "DEBIT" : "CREDIT";
        return new TransactionHistoryItem(
                entry.getTransactionId(), direction, entry.getSenderId(), entry.getReceiverId(),
                entry.getAmount(), entry.getCurrency(), entry.getStatus(),
                entry.getFailureReason(), entry.getAppliedAt());
    }
}
