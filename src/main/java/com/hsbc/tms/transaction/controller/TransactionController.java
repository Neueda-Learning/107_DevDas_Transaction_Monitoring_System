package com.hsbc.tms.transaction.controller;

import com.hsbc.tms.common.dto.PagedResponse;
import com.hsbc.tms.transaction.dto.CreateTransactionRequest;
import com.hsbc.tms.transaction.dto.TransactionDecisionRequest;
import com.hsbc.tms.transaction.dto.TransactionFilterRequest;
import com.hsbc.tms.transaction.dto.TransactionResponse;
import com.hsbc.tms.transaction.dto.TransactionRollbackDecisionRequest;
import com.hsbc.tms.transaction.dto.TransactionRollbackRequest;
import com.hsbc.tms.transaction.model.TransactionStatus;
import com.hsbc.tms.transaction.model.TransactionType;
import com.hsbc.tms.transaction.service.TransactionEventService;
import com.hsbc.tms.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/transactions")
@Validated
@Tag(name = "Transactions", description = "Transaction management APIs")
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionEventService transactionEventService;

    public TransactionController(TransactionService transactionService,
                                 TransactionEventService transactionEventService) {
        this.transactionService = transactionService;
        this.transactionEventService = transactionEventService;
    }

    /**
     * SSE endpoint – frontend pages subscribe here to receive real-time
     * "transaction-updated" events whenever a transaction is created or its status changes.
     * This keeps every page (Dashboard, Transactions, Pending Approvals, Report, etc.)
     * automatically in sync without manual refreshes.
     */
    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Subscribe to real-time transaction update events (SSE)")
    public SseEmitter subscribeToEvents() {
        return transactionEventService.subscribe();
    }

    @PostMapping
    @Operation(summary = "Create a transaction")
    public ResponseEntity<TransactionResponse> createTransaction(@Valid @RequestBody CreateTransactionRequest request) {
        TransactionResponse response = transactionService.createTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get transaction by id")
    public ResponseEntity<TransactionResponse> getTransactionById(@PathVariable UUID id) {
        return ResponseEntity.ok(transactionService.getTransactionById(id));
    }

    @GetMapping
    @Operation(summary = "Search transactions with pagination")
    public ResponseEntity<PagedResponse<TransactionResponse>> findTransactions(
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) String payeeId,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) Instant fromTime,
            @RequestParam(required = false) Instant toTime,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "transactionTime") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        TransactionFilterRequest filter = new TransactionFilterRequest();
        filter.setAccountId(accountId);
        filter.setPayeeId(payeeId);
        filter.setStatus(status);
        filter.setType(type);
        filter.setMinAmount(minAmount);
        filter.setMaxAmount(maxAmount);
        filter.setFromTime(fromTime);
        filter.setToTime(toTime);

        return ResponseEntity.ok(transactionService.findTransactions(filter, page, size, sortBy, sortDir));
    }

    @PatchMapping("/{id}/approve")
    @Operation(summary = "Approve a pending transaction")
    public ResponseEntity<TransactionResponse> approve(@PathVariable UUID id, @Valid @RequestBody TransactionDecisionRequest request) {
        return ResponseEntity.ok(transactionService.approve(id, request));
    }

    @PatchMapping("/{id}/reject")
    @Operation(summary = "Reject a pending transaction")
    public ResponseEntity<TransactionResponse> reject(@PathVariable UUID id, @Valid @RequestBody TransactionDecisionRequest request) {
        return ResponseEntity.ok(transactionService.reject(id, request));
    }

    @PatchMapping("/{id}/rollback/request")
    @Operation(summary = "Request a rollback for a completed transaction")
    public ResponseEntity<TransactionResponse> requestRollback(@PathVariable UUID id, @Valid @RequestBody TransactionRollbackRequest request) {
        return ResponseEntity.ok(transactionService.requestRollback(id, request));
    }

    @PatchMapping("/{id}/rollback/approve")
    @Operation(summary = "Approve a rollback request and issue a refund")
    public ResponseEntity<TransactionResponse> approveRollback(@PathVariable UUID id, @Valid @RequestBody TransactionRollbackDecisionRequest request) {
        return ResponseEntity.ok(transactionService.approveRollback(id, request));
    }

    @PatchMapping("/{id}/rollback/reject")
    @Operation(summary = "Reject a rollback request")
    public ResponseEntity<TransactionResponse> rejectRollback(@PathVariable UUID id, @Valid @RequestBody TransactionRollbackDecisionRequest request) {
        return ResponseEntity.ok(transactionService.rejectRollback(id, request));
    }
}

