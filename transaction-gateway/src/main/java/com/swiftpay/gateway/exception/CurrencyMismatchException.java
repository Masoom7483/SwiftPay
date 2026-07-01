package com.swiftpay.gateway.exception;

/**
 * Thrown when the requested payment currency does not match an account currency.
 */
public class CurrencyMismatchException extends RuntimeException {

    public CurrencyMismatchException(String accountId, String accountCurrency, String requestCurrency) {
        super("Account '" + accountId + "' uses currency " + accountCurrency
                + " but request currency is " + requestCurrency);
    }
}
