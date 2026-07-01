package com.swiftpay.gateway.exception;

/**
 * Thrown when a payment with the same {@code transactionId} has already been
 * accepted within the idempotency window. Maps to HTTP 409 Conflict.
 */
public class DuplicateTransactionException extends RuntimeException {

    public DuplicateTransactionException(String transactionId) {
        super("Transaction '" + transactionId + "' has already been processed");
    }
}
