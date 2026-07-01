package com.swiftpay.analytics.repository;

import com.swiftpay.analytics.entity.PaymentFact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentFactRepository extends JpaRepository<PaymentFact, String> {
}
