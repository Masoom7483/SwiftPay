package com.swiftpay.ledger.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftpay.common.KafkaTopics;
import com.swiftpay.common.PaymentStatus;
import com.swiftpay.common.event.PaymentCompletedEvent;
import com.swiftpay.common.event.PaymentFailedEvent;
import com.swiftpay.ledger.entity.LedgerEntry;
import com.swiftpay.ledger.outbox.LedgerOutboxPublisher;
import com.swiftpay.ledger.outbox.OutboxEvent;
import com.swiftpay.ledger.outbox.OutboxEventRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;


@Component
public class LedgerEventProducer {

    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public LedgerEventProducer(OutboxEventRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    public void publishOutcome(LedgerEntry entry) {
        try {
            if (entry.getStatus() == PaymentStatus.COMPLETED) {
                PaymentCompletedEvent event = new PaymentCompletedEvent(
                        entry.getTransactionId(), entry.getSenderId(), entry.getReceiverId(),
                        entry.getAmount(), entry.getCurrency(), Instant.now());
                queue(entry.getTransactionId(), KafkaTopics.PAYMENT_COMPLETED,
                        PaymentCompletedEvent.class.getName(), objectMapper.writeValueAsString(event));
            } else {
                PaymentFailedEvent event = new PaymentFailedEvent(
                        entry.getTransactionId(), entry.getSenderId(), entry.getReceiverId(),
                        entry.getFailureReason(), Instant.now());
                queue(entry.getTransactionId(), KafkaTopics.PAYMENT_FAILED,
                        PaymentFailedEvent.class.getName(), objectMapper.writeValueAsString(event));
            }
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize ledger outcome event", ex);
        }
    }

    private void queue(String transactionId, String topic, String eventType, String payload) {
        outboxRepository.save(OutboxEvent.create(
                LedgerOutboxPublisher.SERVICE_NAME,
                transactionId,
                topic,
                eventType,
                payload));
    }
}
