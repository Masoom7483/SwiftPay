package com.swiftpay.common;

/**
 * Central registry of Kafka topic names shared between producers (gateway)
 * and consumers (ledger, analytics). Keeping them here prevents typos and
 * keeps topic contracts in one place.
 */
public final class KafkaTopics {

    private KafkaTopics() {
    }

    /** Emitted by the Transaction Gateway when a payment is accepted (PENDING). */
    public static final String PAYMENT_INITIATED = "payment.initiated";

    /** Emitted by the Ledger Service after a successful debit/credit. */
    public static final String PAYMENT_COMPLETED = "payment.completed";

    /** Emitted by the Ledger Service when a payment cannot be applied. */
    public static final String PAYMENT_FAILED = "payment.failed";
}
