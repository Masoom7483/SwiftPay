package com.swiftpay.ledger.repository;

import com.swiftpay.ledger.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, String> {

    /**
     * Transaction history for a user, whether they were the sender or the
     * receiver, most recent first. Backs the reporting endpoint.
     */
    List<LedgerEntry> findBySenderIdOrReceiverIdOrderByAppliedAtDesc(String senderId, String receiverId);
}
