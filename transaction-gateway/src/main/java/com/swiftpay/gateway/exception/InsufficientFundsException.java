package com.swiftpay.gateway.exception;

import java.math.BigDecimal;


public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(String senderId, BigDecimal available, BigDecimal required, String currency) {
        super("Account '" + senderId + "' has insufficient balance. Available: "
                + available.toPlainString() + " " + currency
                + ", required: " + required.toPlainString() + " " + currency);
    }
}
