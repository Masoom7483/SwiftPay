package com.swiftpay.ledger.repository;

import com.swiftpay.ledger.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Lets the ledger update the gateway-created {@code payment_transactions} row
 * (PENDING → COMPLETED / FAILED) as part of the atomic transfer.
 */
@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, String> {
}
