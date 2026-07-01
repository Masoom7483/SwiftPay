package com.swiftpay.ledger.controller;

import com.swiftpay.ledger.dto.TransactionHistoryItem;
import com.swiftpay.ledger.repository.LedgerEntryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Reporting endpoint (Service B): fetch the transaction history for a user.
 */
@RestController
@RequestMapping(value = "/v1/users", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Reporting", description = "Per-user transaction history")
public class TransactionHistoryController {

    private final LedgerEntryRepository ledgerEntryRepository;

    public TransactionHistoryController(LedgerEntryRepository ledgerEntryRepository) {
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Operation(summary = "Get a user's transaction history",
            description = "Returns all ledger entries where the user is the sender or receiver, newest first.")
    @GetMapping("/{userId}/transactions")
    public List<TransactionHistoryItem> getUserTransactions(
            @Parameter(description = "Account id", example = "acc_1001")
            @PathVariable String userId) {
        return ledgerEntryRepository
                .findBySenderIdOrReceiverIdOrderByAppliedAtDesc(userId, userId)
                .stream()
                .map(entry -> TransactionHistoryItem.from(entry, userId))
                .toList();
    }
}
