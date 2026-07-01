package com.swiftpay.ledger.repository;

import com.swiftpay.ledger.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, String> {
}
