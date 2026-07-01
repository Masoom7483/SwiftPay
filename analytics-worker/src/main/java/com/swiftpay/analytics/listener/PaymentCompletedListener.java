package com.swiftpay.analytics.listener;

import com.swiftpay.analytics.entity.PaymentFact;
import com.swiftpay.analytics.repository.PaymentFactRepository;
import com.swiftpay.common.KafkaTopics;
import com.swiftpay.common.event.PaymentCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Appends completed payments to the analytics fact table. Idempotent on the
 * transaction id (save is an upsert on the PK).
 */
@Component
public class PaymentCompletedListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentCompletedListener.class);

    private final PaymentFactRepository repository;

    public PaymentCompletedListener(PaymentFactRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT_COMPLETED, groupId = "analytics-worker")
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        repository.save(new PaymentFact(
                event.transactionId(), event.amount(), event.currency(), event.completedAt()));
        log.debug("Recorded analytics fact for txn {}", event.transactionId());
    }
}
