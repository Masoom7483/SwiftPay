package com.swiftpay.gateway.exception;

import java.math.BigDecimal;

/**
 * Thrown at the gateway when a pre-flight balance check (against the cached
 * balance / ledger) shows the sender cannot cover the amount.
 * Maps to HTTP 422 Unprocessable Entity.
 */
public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(String senderId, BigDecimal available, BigDecimal required, String currency) {
        super("Account '" + senderId + "' has insufficient balance. Available: "
                + available.toPlainString() + " " + currency
                + ", required: " + required.toPlainString() + " " + currency);
    }
}
