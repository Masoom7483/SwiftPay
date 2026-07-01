package com.swiftpay.gateway.exception;

/**
 * Thrown when a payment references an account that does not exist.
 */
public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(String accountRole, String accountId) {
        super(accountRole + " account '" + accountId + "' does not exist");
    }
}
