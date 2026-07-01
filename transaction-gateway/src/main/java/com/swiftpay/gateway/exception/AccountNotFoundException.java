package com.swiftpay.gateway.exception;


public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(String accountRole, String accountId) {
        super(accountRole + " account '" + accountId + "' does not exist");
    }
}
