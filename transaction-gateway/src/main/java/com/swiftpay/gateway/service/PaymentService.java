package com.swiftpay.gateway.service;

import com.swiftpay.common.PaymentStatus;
import com.swiftpay.common.event.PaymentInitiatedEvent;
import com.swiftpay.gateway.dto.PaymentRequest;
import com.swiftpay.gateway.dto.PaymentResponse;
import com.swiftpay.gateway.entity.Account;
import com.swiftpay.gateway.entity.PaymentTransaction;
import com.swiftpay.gateway.exception.AccountNotFoundException;
import com.swiftpay.gateway.exception.CurrencyMismatchException;
import com.swiftpay.gateway.exception.DuplicateTransactionException;
import com.swiftpay.gateway.exception.InsufficientFundsException;
import com.swiftpay.gateway.exception.SameAccountTransferException;
import com.swiftpay.gateway.repository.AccountRepository;
import com.swiftpay.gateway.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.Objects;

/**
 * Orchestrates the "accept a payment" use case:
 * <ol>
 *   <li>Run gateway-side business validation.</li>
 *   <li>Reserve the transaction id in Redis (fast idempotency check).</li>
 *   <li>Persist a PENDING row to PostgreSQL (durable idempotency via PK).</li>
 *   <li>Emit a {@code PaymentInitiated} event to Kafka.</li>
 * </ol>
 *
 * <p>The DB write and the event publish are intentionally kept simple here;
 * production-grade delivery would use the transactional outbox pattern (see
 * ARCHITECTURE.md). We reserve/release the Redis key so failures don't
 * permanently block a legitimate retry.
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository repository;
    private final AccountRepository accountRepository;
    private final IdempotencyService idempotencyService;
    private final PaymentEventProducer eventProducer;

    public PaymentService(PaymentRepository repository,
                          AccountRepository accountRepository,
                          IdempotencyService idempotencyService,
                          PaymentEventProducer eventProducer) {
        this.repository = repository;
        this.accountRepository = accountRepository;
        this.idempotencyService = idempotencyService;
        this.eventProducer = eventProducer;
    }

    @Transactional
    public PaymentResponse initiatePayment(PaymentRequest request) {
        validatePreflight(request);

        if (!idempotencyService.tryReserve(request.transactionId())) {
            throw new DuplicateTransactionException(request.transactionId());
        }

        try {
            Instant now = Instant.now();
            PaymentTransaction txn = new PaymentTransaction(
                    request.transactionId(), request.senderId(), request.receiverId(),
                    request.amount(), request.currency(), PaymentStatus.PENDING, now);
            repository.saveAndFlush(txn);

            PaymentInitiatedEvent event = new PaymentInitiatedEvent(
                    txn.getTransactionId(), txn.getSenderId(), txn.getReceiverId(),
                    txn.getAmount(), txn.getCurrency(), now);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    eventProducer.publishInitiated(event);
                }
            });

            log.info("Accepted payment txn={} amount={} {}",
                    txn.getTransactionId(), txn.getAmount(), txn.getCurrency());

            return new PaymentResponse(
                    txn.getTransactionId(), txn.getStatus(), txn.getSenderId(),
                    txn.getReceiverId(), txn.getAmount(), txn.getCurrency(), txn.getCreatedAt());

        } catch (DataIntegrityViolationException e) {
            // DB says this id already exists — a genuine duplicate slipped past Redis.
            throw new DuplicateTransactionException(request.transactionId());
        } catch (RuntimeException e) {
            // Downstream failure: free the Redis reservation so the client can retry.
            idempotencyService.release(request.transactionId());
            throw e;
        }
    }

    private void validatePreflight(PaymentRequest request) {
        if (Objects.equals(request.senderId(), request.receiverId())) {
            throw new SameAccountTransferException();
        }

        Account sender = accountRepository.findById(request.senderId())
                .orElseThrow(() -> new AccountNotFoundException("Sender", request.senderId()));
        Account receiver = accountRepository.findById(request.receiverId())
                .orElseThrow(() -> new AccountNotFoundException("Receiver", request.receiverId()));

        if (!sender.getCurrency().equals(request.currency())) {
            throw new CurrencyMismatchException(sender.getAccountId(), sender.getCurrency(), request.currency());
        }
        if (!receiver.getCurrency().equals(request.currency())) {
            throw new CurrencyMismatchException(receiver.getAccountId(), receiver.getCurrency(), request.currency());
        }
        if (!sender.canDebit(request.amount())) {
            throw new InsufficientFundsException(
                    sender.getAccountId(), sender.getBalance(), request.amount(), request.currency());
        }
    }
}
