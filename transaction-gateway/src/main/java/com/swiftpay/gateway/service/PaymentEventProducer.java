package com.swiftpay.gateway.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftpay.common.KafkaTopics;
import com.swiftpay.common.event.PaymentInitiatedEvent;
import com.swiftpay.gateway.outbox.OutboxEvent;
import com.swiftpay.gateway.outbox.OutboxEventRepository;
import com.swiftpay.gateway.outbox.PaymentOutboxPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


@Component
public class PaymentEventProducer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventProducer.class);

    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public PaymentEventProducer(OutboxEventRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    public void publishInitiated(PaymentInitiatedEvent event) {
        try {
            outboxRepository.save(OutboxEvent.create(
                    PaymentOutboxPublisher.SERVICE_NAME,
                    event.transactionId(),
                    KafkaTopics.PAYMENT_INITIATED,
                    PaymentInitiatedEvent.class.getName(),
                    objectMapper.writeValueAsString(event)));
            log.debug("Queued PaymentInitiated outbox event for txn {}", event.transactionId());
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize PaymentInitiated event", ex);
        }
    }
}
