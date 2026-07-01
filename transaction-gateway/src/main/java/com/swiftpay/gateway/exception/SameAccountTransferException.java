package com.swiftpay.gateway.exception;

/**
 * Thrown when a payment attempts to debit and credit the same account.
 */
public class SameAccountTransferException extends RuntimeException {

    public SameAccountTransferException() {
        super("senderId and receiverId must refer to different accounts");
    }
}
