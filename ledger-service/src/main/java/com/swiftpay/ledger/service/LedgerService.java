package com.swiftpay.ledger.service;

import com.swiftpay.common.PaymentStatus;
import com.swiftpay.common.event.PaymentInitiatedEvent;
import com.swiftpay.ledger.entity.Account;
import com.swiftpay.ledger.entity.LedgerEntry;
import com.swiftpay.ledger.repository.AccountRepository;
import com.swiftpay.ledger.repository.LedgerEntryRepository;
import com.swiftpay.ledger.repository.PaymentTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Applies a peer-to-peer transfer atomically.
 *
 * <p>The whole method runs in one DB transaction: both accounts are locked
 * FOR UPDATE, the balances are moved, and a {@link LedgerEntry} audit row is
 * written. If anything fails the transaction rolls back and nothing is applied.
 *
 * <p>Idempotency: if a ledger entry already exists for the transaction id we
 * short-circuit — the event was already processed (e.g. a Kafka redelivery).
 */
@Service
public class LedgerService {

    private static final Logger log = LoggerFactory.getLogger(LedgerService.class);

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;

    public LedgerService(AccountRepository accountRepository,
                         LedgerEntryRepository ledgerEntryRepository,
                         PaymentTransactionRepository paymentTransactionRepository) {
        this.accountRepository = accountRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
    }

    /**
     * @return the resulting {@link LedgerEntry} (COMPLETED or FAILED). The caller
     *         inspects its status to decide which Kafka event to emit.
     */
    @Transactional
    public LedgerEntry applyTransfer(PaymentInitiatedEvent event) {
        // Idempotent replay guard: already processed this transaction id.
        var existing = ledgerEntryRepository.findById(event.transactionId());
        if (existing.isPresent()) {
            log.info("Skipping already-applied txn {}", event.transactionId());
            return existing.get();
        }

        // Lock lower id first to establish a consistent ordering and avoid deadlocks.
        String first = event.senderId().compareTo(event.receiverId()) <= 0
                ? event.senderId() : event.receiverId();
        String second = first.equals(event.senderId()) ? event.receiverId() : event.senderId();
        accountRepository.findByIdForUpdate(first);
        accountRepository.findByIdForUpdate(second);

        Account sender = accountRepository.findByIdForUpdate(event.senderId()).orElse(null);
        Account receiver = accountRepository.findByIdForUpdate(event.receiverId()).orElse(null);

        if (sender == null || receiver == null) {
            return recordFailure(event, "ACCOUNT_NOT_FOUND");
        }
        if (!sender.getCurrency().equals(event.currency()) || !receiver.getCurrency().equals(event.currency())) {
            return recordFailure(event, "CURRENCY_MISMATCH");
        }
        if (!sender.canDebit(event.amount())) {
            return recordFailure(event, "INSUFFICIENT_FUNDS");
        }

        // Move the money: debit sender, credit receiver (dirty-checked, flushed on commit).
        sender.debit(event.amount());
        receiver.credit(event.amount());

        // Write the immutable audit row.
        LedgerEntry entry = new LedgerEntry(
                event.transactionId(), event.senderId(), event.receiverId(),
                event.amount(), event.currency(), PaymentStatus.COMPLETED, null, Instant.now());
        ledgerEntryRepository.save(entry);

        // Update the gateway's transaction status PENDING -> COMPLETED, in this
        // same transaction, so the status flip and the balance change commit together.
        updateGatewayStatus(event.transactionId(), PaymentStatus.COMPLETED);

        log.info("Applied transfer txn={} {} {} -> {}", event.transactionId(),
                event.amount(), event.senderId(), event.receiverId());
        return entry;
    }

    private LedgerEntry recordFailure(PaymentInitiatedEvent event, String reason) {
        LedgerEntry entry = new LedgerEntry(
                event.transactionId(), event.senderId(), event.receiverId(),
                event.amount(), event.currency(), PaymentStatus.FAILED, reason, Instant.now());
        ledgerEntryRepository.save(entry);

        // A business rejection is a definitive outcome: mark PENDING -> FAILED and commit.
        updateGatewayStatus(event.transactionId(), PaymentStatus.FAILED);

        log.warn("Rejected transfer txn={} reason={}", event.transactionId(), reason);
        return entry;
    }

    /**
     * Flips the gateway-owned {@code payment_transactions} row to the given status.
     * Best-effort: if the row isn't visible yet we skip it — the {@link LedgerEntry}
     * is the ledger's own authoritative record either way.
     */
    private void updateGatewayStatus(String transactionId, PaymentStatus status) {
        paymentTransactionRepository.findById(transactionId)
                .ifPresent(txn -> txn.markStatus(status));
    }
}
