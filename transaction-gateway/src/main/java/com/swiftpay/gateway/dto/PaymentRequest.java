package com.swiftpay.gateway.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/**
 * Inbound payment request body for {@code POST /v1/payments}.
 *
 * <p>Every field carries a Swagger {@link Schema} annotation so the generated
 * OpenAPI doc shows an example payload and validation constraints.
 */
@Schema(name = "PaymentRequest", description = "A peer-to-peer payment instruction")
public record PaymentRequest(

        @Schema(description = "Client-generated unique id used for idempotency (UUID recommended)",
                example = "a3f1c2d4-5e6f-7a8b-9c0d-1e2f3a4b5c6d", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "transaction_id is required")
        String transactionId,

        @Schema(description = "Account id of the payer", example = "acc_1001",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "sender_id is required")
        String senderId,

        @Schema(description = "Account id of the payee", example = "acc_2002",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "receiver_id is required")
        String receiverId,

        @Schema(description = "Transfer amount, strictly positive, up to 2 decimal places",
                example = "150.75", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be greater than 0")
        @Digits(integer = 17, fraction = 2, message = "amount must have at most 2 decimal places")
        BigDecimal amount,

        @Schema(description = "ISO-4217 3-letter currency code", example = "USD",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "currency is required")
        @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be a 3-letter ISO-4217 code")
        String currency
) {
}
