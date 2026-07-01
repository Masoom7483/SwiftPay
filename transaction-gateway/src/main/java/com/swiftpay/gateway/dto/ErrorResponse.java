package com.swiftpay.gateway.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;


@Schema(name = "ErrorResponse", description = "Standard error payload")
public record ErrorResponse(

        @Schema(description = "HTTP status code", example = "400")
        int status,

        @Schema(description = "Short machine-readable error code", example = "VALIDATION_ERROR")
        String error,

        @Schema(description = "Human-readable message", example = "Request validation failed")
        String message,

        @Schema(description = "Request path that produced the error", example = "/v1/payments")
        String path,

        @Schema(description = "Field-level validation details, when applicable")
        List<FieldError> fieldErrors,

        @Schema(description = "Server timestamp (UTC)", example = "2026-07-01T10:15:30Z")
        Instant timestamp
) {
    /** A single field validation failure. */
    @Schema(name = "FieldError", description = "Validation failure for one field")
    public record FieldError(
            @Schema(example = "amount") String field,
            @Schema(example = "amount must be greater than 0") String message
    ) {
    }
}
