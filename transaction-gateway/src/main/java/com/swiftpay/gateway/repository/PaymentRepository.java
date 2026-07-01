package com.swiftpay.gateway.repository;

import com.swiftpay.gateway.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for {@link PaymentTransaction}. The id (transactionId)
 * is a String, so lookups by id are the natural idempotency check at the DB layer.
 */
@Repository
public interface PaymentRepository extends JpaRepository<PaymentTransaction, String> {
}
