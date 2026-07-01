package com.swiftpay.common;

/**
 * Lifecycle status of a payment as it moves through the SwiftPay pipeline.
 *
 * <pre>
 *   PENDING  -> gateway accepted the request and emitted PaymentInitiated
 *   COMPLETED-> ledger successfully debited/credited within a DB transaction
 *   FAILED   -> ledger rejected (e.g. insufficient funds, constraint violation)
 * </pre>
 */
public enum PaymentStatus {
    PENDING,
    COMPLETED,
    FAILED
}
