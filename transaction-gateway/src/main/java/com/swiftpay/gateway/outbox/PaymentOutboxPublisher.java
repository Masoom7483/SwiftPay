package com.swiftpay.gateway.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftpay.common.event.PaymentInitiatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Component
public class PaymentOutboxPublisher {

    public static final String SERVICE_NAME = "transaction-gateway";

    private static final Logger log = LoggerFactory.getLogger(PaymentOutboxPublisher.class);
    private static final int BATCH_SIZE = 50;

    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public PaymentOutboxPublisher(OutboxEventRepository repository,
                                  KafkaTemplate<String, Object> kafkaTemplate,
                                  ObjectMapper objectMapper) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${swiftpay.outbox.poll-interval-ms:1000}")
    public void publishReadyEvents() {
        var readyEvents = repository.findByServiceNameAndStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                SERVICE_NAME, OutboxStatus.PENDING, Instant.now(), PageRequest.of(0, BATCH_SIZE));

        readyEvents.forEach(this::publish);
    }

    private void publish(OutboxEvent outboxEvent) {
        try {
            Object event = toKafkaEvent(outboxEvent);
            kafkaTemplate.send(outboxEvent.getTopic(), outboxEvent.getAggregateId(), event)
                    .get(10, TimeUnit.SECONDS);
            outboxEvent.markSent(Instant.now());
            log.debug("Published outbox event type={} aggregate={}",
                    outboxEvent.getEventType(), outboxEvent.getAggregateId());
        } catch (Exception ex) {
            outboxEvent.recordFailure(rootMessage(ex), Instant.now());
            log.warn("Outbox publish failed type={} aggregate={} next retry scheduled",
                    outboxEvent.getEventType(), outboxEvent.getAggregateId(), ex);
        }
        repository.save(outboxEvent);
    }

    private Object toKafkaEvent(OutboxEvent outboxEvent) throws Exception {
        if (PaymentInitiatedEvent.class.getName().equals(outboxEvent.getEventType())) {
            return objectMapper.readValue(outboxEvent.getPayload(), PaymentInitiatedEvent.class);
        }
        throw new IllegalStateException("Unsupported outbox event type: " + outboxEvent.getEventType());
    }

    private String rootMessage(Exception ex) {
        Throwable current = ex;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }
}
