package com.swiftpay.ledger.service;

import com.swiftpay.common.PaymentStatus;
import com.swiftpay.common.event.PaymentInitiatedEvent;
import com.swiftpay.ledger.entity.Account;
import com.swiftpay.ledger.entity.LedgerEntry;
import com.swiftpay.ledger.entity.PaymentTransaction;
import com.swiftpay.ledger.repository.AccountRepository;
import com.swiftpay.ledger.repository.LedgerEntryRepository;
import com.swiftpay.ledger.repository.PaymentTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private LedgerEventProducer eventProducer;

    @InjectMocks
    private LedgerService service;

    @Test
    void applyTransferDebitsSenderCreditsReceiverAndMarksCompleted() {
        PaymentInitiatedEvent event = event("txn-1", "30.00");
        Account sender = new Account("acc_1001", new BigDecimal("100.00"), "USD");
        Account receiver = new Account("acc_2002", new BigDecimal("25.00"), "USD");
        PaymentTransaction transaction = new PaymentTransaction(
                "txn-1", PaymentStatus.PENDING, Instant.parse("2026-07-01T10:15:30Z"));

        when(ledgerEntryRepository.findById("txn-1")).thenReturn(Optional.empty());
        when(accountRepository.findByIdForUpdate("acc_1001")).thenReturn(Optional.of(sender));
        when(accountRepository.findByIdForUpdate("acc_2002")).thenReturn(Optional.of(receiver));
        when(paymentTransactionRepository.findById("txn-1")).thenReturn(Optional.of(transaction));

        LedgerEntry outcome = service.applyTransfer(event);

        assertThat(outcome.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(sender.getBalance()).isEqualByComparingTo("70.00");
        assertThat(receiver.getBalance()).isEqualByComparingTo("55.00");
        assertThat(transaction.getStatus()).isEqualTo(PaymentStatus.COMPLETED);

        ArgumentCaptor<LedgerEntry> entryCaptor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryRepository).save(entryCaptor.capture());
        assertThat(entryCaptor.getValue().getTransactionId()).isEqualTo("txn-1");
        assertThat(entryCaptor.getValue().getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(entryCaptor.getValue().getFailureReason()).isNull();
        verify(eventProducer).publishOutcome(outcome);
    }

    @Test
    void applyTransferRecordsFailureWhenSenderHasInsufficientFunds() {
        PaymentInitiatedEvent event = event("txn-low-balance", "130.00");
        Account sender = new Account("acc_1001", new BigDecimal("100.00"), "USD");
        Account receiver = new Account("acc_2002", new BigDecimal("25.00"), "USD");
        PaymentTransaction transaction = new PaymentTransaction(
                "txn-low-balance", PaymentStatus.PENDING, Instant.parse("2026-07-01T10:15:30Z"));

        when(ledgerEntryRepository.findById("txn-low-balance")).thenReturn(Optional.empty());
        when(accountRepository.findByIdForUpdate("acc_1001")).thenReturn(Optional.of(sender));
        when(accountRepository.findByIdForUpdate("acc_2002")).thenReturn(Optional.of(receiver));
        when(paymentTransactionRepository.findById("txn-low-balance")).thenReturn(Optional.of(transaction));

        LedgerEntry outcome = service.applyTransfer(event);

        assertThat(outcome.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(outcome.getFailureReason()).isEqualTo("INSUFFICIENT_FUNDS");
        assertThat(sender.getBalance()).isEqualByComparingTo("100.00");
        assertThat(receiver.getBalance()).isEqualByComparingTo("25.00");
        assertThat(transaction.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(eventProducer).publishOutcome(outcome);
    }

    @Test
    void applyTransferSkipsAlreadyAppliedTransaction() {
        PaymentInitiatedEvent event = event("txn-replay", "30.00");
        LedgerEntry existing = new LedgerEntry(
                "txn-replay",
                "acc_1001",
                "acc_2002",
                new BigDecimal("30.00"),
                "USD",
                PaymentStatus.COMPLETED,
                null,
                Instant.parse("2026-07-01T10:15:31Z"));
        when(ledgerEntryRepository.findById("txn-replay")).thenReturn(Optional.of(existing));

        LedgerEntry outcome = service.applyTransfer(event);

        assertThat(outcome).isSameAs(existing);
        verify(ledgerEntryRepository, never()).save(any());
        verifyNoInteractions(accountRepository, paymentTransactionRepository, eventProducer);
    }

    private PaymentInitiatedEvent event(String transactionId, String amount) {
        return new PaymentInitiatedEvent(
                transactionId,
                "acc_1001",
                "acc_2002",
                new BigDecimal(amount),
                "USD",
                Instant.parse("2026-07-01T10:15:30Z"));
    }
}
