package com.swiftpay.common;


public final class KafkaTopics {

    private KafkaTopics() {
    }


    public static final String PAYMENT_INITIATED = "payment.initiated";


    public static final String PAYMENT_COMPLETED = "payment.completed";


    public static final String PAYMENT_FAILED = "payment.failed";
}
