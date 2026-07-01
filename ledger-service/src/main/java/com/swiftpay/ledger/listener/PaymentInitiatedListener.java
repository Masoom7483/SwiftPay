package com.swiftpay.ledger.listener;

import com.swiftpay.common.KafkaTopics;
import com.swiftpay.common.event.PaymentInitiatedEvent;
import com.swiftpay.ledger.entity.LedgerEntry;
import com.swiftpay.ledger.service.LedgerEventProducer;
import com.swiftpay.ledger.service.LedgerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentInitiatedListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentInitiatedListener.class);

    private final LedgerService ledgerService;
    private final LedgerEventProducer eventProducer;

    public PaymentInitiatedListener(LedgerService ledgerService, LedgerEventProducer eventProducer) {
        this.ledgerService = ledgerService;
        this.eventProducer = eventProducer;
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT_INITIATED, groupId = "ledger-service")
    public void onPaymentInitiated(PaymentInitiatedEvent event) {
        log.debug("Received PaymentInitiated txn={}", event.transactionId());
        LedgerEntry outcome = ledgerService.applyTransfer(event);
        eventProducer.publishOutcome(outcome);
    }
}
