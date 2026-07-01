package com.swiftpay.gateway.service;

import com.swiftpay.common.KafkaTopics;
import com.swiftpay.common.event.PaymentInitiatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes {@link PaymentInitiatedEvent}s to Kafka. The {@code transactionId}
 * is used as the message key so all events for a payment land on the same
 * partition, preserving per-payment ordering.
 */
@Component
public class PaymentEventProducer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PaymentEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishInitiated(PaymentInitiatedEvent event) {
        kafkaTemplate.send(KafkaTopics.PAYMENT_INITIATED, event.transactionId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish PaymentInitiated for txn {}",
                                event.transactionId(), ex);
                    } else {
                        log.debug("Published PaymentInitiated for txn {} to partition {}",
                                event.transactionId(),
                                result.getRecordMetadata().partition());
                    }
                });
    }
}
