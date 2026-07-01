package com.swiftpay.gateway.service;

import com.swiftpay.common.PaymentStatus;
import com.swiftpay.common.event.PaymentInitiatedEvent;
import com.swiftpay.gateway.dto.PaymentRequest;
import com.swiftpay.gateway.dto.PaymentResponse;
import com.swiftpay.gateway.entity.Account;
import com.swiftpay.gateway.entity.PaymentTransaction;
import com.swiftpay.gateway.exception.DuplicateTransactionException;
import com.swiftpay.gateway.exception.InsufficientFundsException;
import com.swiftpay.gateway.repository.AccountRepository;
import com.swiftpay.gateway.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository repository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private PaymentEventProducer eventProducer;

    @Mock
    private BalanceCacheService balanceCacheService;

    @InjectMocks
    private PaymentService service;

    @Test
    void initiatePaymentPersistsPendingAndQueuesOutboxEvent() {
        PaymentRequest request = request("txn-1", "12.50");
        stubUsdAccounts("100.00", "25.00");
        when(idempotencyService.tryReserve("txn-1")).thenReturn(true);
        when(repository.saveAndFlush(any(PaymentTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = service.initiatePayment(request);

        assertThat(response.transactionId()).isEqualTo("txn-1");
        assertThat(response.status()).isEqualTo(PaymentStatus.PENDING);

        ArgumentCaptor<PaymentInitiatedEvent> eventCaptor =
                ArgumentCaptor.forClass(PaymentInitiatedEvent.class);
        verify(eventProducer).publishInitiated(eventCaptor.capture());
        PaymentInitiatedEvent event = eventCaptor.getValue();
        assertThat(event.transactionId()).isEqualTo("txn-1");
        assertThat(event.senderId()).isEqualTo("acc_1001");
        assertThat(event.receiverId()).isEqualTo("acc_2002");
        assertThat(event.amount()).isEqualByComparingTo("12.50");
    }

    @Test
    void duplicateTransactionIsRejectedBeforePersistence() {
        PaymentRequest request = request("txn-duplicate", "12.50");
        stubUsdAccounts("100.00", "25.00");
        when(idempotencyService.tryReserve("txn-duplicate")).thenReturn(false);

        assertThatThrownBy(() -> service.initiatePayment(request))
                .isInstanceOf(DuplicateTransactionException.class);

        verify(repository, never()).saveAndFlush(any());
        verifyNoInteractions(eventProducer);
    }

    @Test
    void insufficientFundsIsRejectedBeforeIdempotencyReservation() {
        PaymentRequest request = request("txn-low-balance", "200.00");
        stubUsdAccounts("100.00", "25.00");

        assertThatThrownBy(() -> service.initiatePayment(request))
                .isInstanceOf(InsufficientFundsException.class);

        verifyNoInteractions(idempotencyService, repository, eventProducer);
    }

    @Test
    void releasesIdempotencyReservationWhenPersistenceFails() {
        PaymentRequest request = request("txn-db-down", "12.50");
        stubUsdAccounts("100.00", "25.00");
        when(idempotencyService.tryReserve("txn-db-down")).thenReturn(true);
        when(repository.saveAndFlush(any(PaymentTransaction.class)))
                .thenThrow(new IllegalStateException("database unavailable"));

        assertThatThrownBy(() -> service.initiatePayment(request))
                .isInstanceOf(IllegalStateException.class);

        verify(idempotencyService).release("txn-db-down");
        verifyNoInteractions(eventProducer);
    }

    @Test
    void durableDuplicateDoesNotReleaseIdempotencyReservation() {
        PaymentRequest request = request("txn-db-duplicate", "12.50");
        stubUsdAccounts("100.00", "25.00");
        when(idempotencyService.tryReserve("txn-db-duplicate")).thenReturn(true);
        when(repository.saveAndFlush(any(PaymentTransaction.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate transaction id"));

        assertThatThrownBy(() -> service.initiatePayment(request))
                .isInstanceOf(DuplicateTransactionException.class);

        verify(idempotencyService, never()).release("txn-db-duplicate");
        verifyNoInteractions(eventProducer);
    }

    private PaymentRequest request(String transactionId, String amount) {
        return new PaymentRequest(
                transactionId,
                "acc_1001",
                "acc_2002",
                new BigDecimal(amount),
                "USD");
    }

    private void stubUsdAccounts(String senderBalance, String receiverBalance) {
        when(balanceCacheService.find("acc_1001")).thenReturn(Optional.empty());
        when(balanceCacheService.find("acc_2002")).thenReturn(Optional.empty());
        when(accountRepository.findById("acc_1001"))
                .thenReturn(Optional.of(new Account("acc_1001", new BigDecimal(senderBalance), "USD")));
        when(accountRepository.findById("acc_2002"))
                .thenReturn(Optional.of(new Account("acc_2002", new BigDecimal(receiverBalance), "USD")));
    }
}
