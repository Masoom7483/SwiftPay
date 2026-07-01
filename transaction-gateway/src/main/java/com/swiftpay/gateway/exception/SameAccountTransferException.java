package com.swiftpay.gateway.exception;


public class SameAccountTransferException extends RuntimeException {

    public SameAccountTransferException() {
        super("sender_id and receiver_id must refer to different accounts");
    }
}
