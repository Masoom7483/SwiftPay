package com.swiftpay.gateway.dto;

import com.swiftpay.common.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;


@Schema(name = "PaymentResponse", description = "Acknowledgement of an accepted payment")
public record PaymentResponse(

        @Schema(description = "Idempotency / transaction id echoed back",
                example = "a3f1c2d4-5e6f-7a8b-9c0d-1e2f3a4b5c6d")
        String transactionId,

        @Schema(description = "Current status of the payment", example = "PENDING")
        PaymentStatus status,

        @Schema(description = "Payer account id", example = "acc_1001")
        String senderId,

        @Schema(description = "Payee account id", example = "acc_2002")
        String receiverId,

        @Schema(description = "Transfer amount", example = "150.75")
        BigDecimal amount,

        @Schema(description = "ISO-4217 currency code", example = "USD")
        String currency,

        @Schema(description = "Server timestamp the request was accepted (UTC)",
                example = "2026-07-01T10:15:30Z")
        Instant createdAt
) {
}
