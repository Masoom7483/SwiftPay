package com.swiftpay.ledger.service;

import com.swiftpay.common.KafkaTopics;
import com.swiftpay.common.PaymentStatus;
import com.swiftpay.common.event.PaymentCompletedEvent;
import com.swiftpay.common.event.PaymentFailedEvent;
import com.swiftpay.ledger.entity.LedgerEntry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;


@Component
public class LedgerEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public LedgerEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishOutcome(LedgerEntry entry) {
        if (entry.getStatus() == PaymentStatus.COMPLETED) {
            PaymentCompletedEvent event = new PaymentCompletedEvent(
                    entry.getTransactionId(), entry.getSenderId(), entry.getReceiverId(),
                    entry.getAmount(), entry.getCurrency(), Instant.now());
            kafkaTemplate.send(KafkaTopics.PAYMENT_COMPLETED, entry.getTransactionId(), event);
        } else {
            PaymentFailedEvent event = new PaymentFailedEvent(
                    entry.getTransactionId(), entry.getSenderId(), entry.getReceiverId(),
                    entry.getFailureReason(), Instant.now());
            kafkaTemplate.send(KafkaTopics.PAYMENT_FAILED, entry.getTransactionId(), event);
        }
    }
}
