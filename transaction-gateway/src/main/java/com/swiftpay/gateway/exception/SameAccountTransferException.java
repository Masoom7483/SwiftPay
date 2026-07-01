package com.swiftpay.gateway.exception;


public class SameAccountTransferException extends RuntimeException {

    public SameAccountTransferException() {
        super("senderId and receiverId must refer to different accounts");
    }
}
