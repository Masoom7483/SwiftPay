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

import java.time.Instant;
import java.util.Objects;


@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository repository;
    private final AccountRepository accountRepository;
    private final IdempotencyService idempotencyService;
    private final PaymentEventProducer eventProducer;
    private final BalanceCacheService balanceCacheService;

    public PaymentService(PaymentRepository repository,
                          AccountRepository accountRepository,
                          IdempotencyService idempotencyService,
                          PaymentEventProducer eventProducer,
                          BalanceCacheService balanceCacheService) {
        this.repository = repository;
        this.accountRepository = accountRepository;
        this.idempotencyService = idempotencyService;
        this.eventProducer = eventProducer;
        this.balanceCacheService = balanceCacheService;
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
            try {
                repository.saveAndFlush(txn);
            } catch (DataIntegrityViolationException e) {
                // DB says this id already exists — a genuine duplicate slipped past Redis.
                throw new DuplicateTransactionException(request.transactionId());
            }

            PaymentInitiatedEvent event = new PaymentInitiatedEvent(
                    txn.getTransactionId(), txn.getSenderId(), txn.getReceiverId(),
                    txn.getAmount(), txn.getCurrency(), now);
            eventProducer.publishInitiated(event);

            log.info("Accepted payment txn={} amount={} {}",
                    txn.getTransactionId(), txn.getAmount(), txn.getCurrency());

            return new PaymentResponse(
                    txn.getTransactionId(), txn.getStatus(), txn.getSenderId(),
                    txn.getReceiverId(), txn.getAmount(), txn.getCurrency(), txn.getCreatedAt());

        } catch (DuplicateTransactionException e) {
            throw e;
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

        BalanceCacheService.CachedAccount sender = accountForValidation("Sender", request.senderId());
        BalanceCacheService.CachedAccount receiver = accountForValidation("Receiver", request.receiverId());

        if (!sender.currency().equals(request.currency())) {
            throw new CurrencyMismatchException(sender.accountId(), sender.currency(), request.currency());
        }
        if (!receiver.currency().equals(request.currency())) {
            throw new CurrencyMismatchException(receiver.accountId(), receiver.currency(), request.currency());
        }
        if (!sender.canDebit(request.amount())) {
            throw new InsufficientFundsException(
                    sender.accountId(), sender.balance(), request.amount(), request.currency());
        }
    }

    private BalanceCacheService.CachedAccount accountForValidation(String accountRole, String accountId) {
        return balanceCacheService.find(accountId)
                .orElseGet(() -> {
                    Account account = accountRepository.findById(accountId)
                            .orElseThrow(() -> new AccountNotFoundException(accountRole, accountId));
                    balanceCacheService.cache(account);
                    return BalanceCacheService.CachedAccount.from(account);
                });
    }
}
