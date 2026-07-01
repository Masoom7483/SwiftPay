package com.swiftpay.ledger.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findByServiceNameAndStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            String serviceName, OutboxStatus status, Instant now, Pageable pageable);
}
