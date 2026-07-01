package com.swiftpay.gateway.exception;

public class CurrencyMismatchException extends RuntimeException {

    public CurrencyMismatchException(String accountId, String accountCurrency, String requestCurrency) {
        super("Account '" + accountId + "' uses currency " + accountCurrency
                + " but request currency is " + requestCurrency);
    }
}
