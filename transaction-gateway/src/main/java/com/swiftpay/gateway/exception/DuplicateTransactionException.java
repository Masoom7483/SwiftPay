package com.swiftpay.gateway.exception;


public class DuplicateTransactionException extends RuntimeException {

    public DuplicateTransactionException(String transactionId) {
        super("Transaction '" + transactionId + "' has already been processed");
    }
}
