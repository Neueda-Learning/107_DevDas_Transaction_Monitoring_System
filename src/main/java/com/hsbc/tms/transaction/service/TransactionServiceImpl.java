package com.hsbc.tms.transaction.service;

import com.hsbc.tms.alerts.service.AlertService;
import com.hsbc.tms.common.dto.PagedResponse;
import com.hsbc.tms.common.exception.BadRequestException;
import com.hsbc.tms.common.exception.ResourceNotFoundException;
import com.hsbc.tms.rules.service.RuleEngineService;
import com.hsbc.tms.transaction.dto.CreateTransactionRequest;
import com.hsbc.tms.transaction.dto.TransactionDecisionRequest;
import com.hsbc.tms.transaction.dto.TransactionFilterRequest;
import com.hsbc.tms.transaction.dto.TransactionResponse;
import com.hsbc.tms.transaction.dto.TransactionRollbackDecisionRequest;
import com.hsbc.tms.transaction.dto.TransactionRollbackRequest;
import com.hsbc.tms.transaction.model.Transaction;
import com.hsbc.tms.transaction.model.TransactionStatus;
import com.hsbc.tms.transaction.repository.TransactionRepository;
import java.util.List;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionServiceImpl implements TransactionService {

    private static final List<String> SORTABLE_FIELDS = List.of("amount", "transactionTime", "createdAt", "updatedAt", "accountId", "status");

    private final TransactionRepository transactionRepository;
    private final RuleEngineService ruleEngineService;
    private final AlertService alertService;

    public TransactionServiceImpl(TransactionRepository transactionRepository, RuleEngineService ruleEngineService, AlertService alertService) {
        this.transactionRepository = transactionRepository;
        this.ruleEngineService = ruleEngineService;
        this.alertService = alertService;
    }

    @Override
    @Transactional
    public TransactionResponse createTransaction(CreateTransactionRequest request) {
        Transaction transaction = new Transaction();
        transaction.setAccountId(request.getAccountId());
        transaction.setPayeeId(request.getPayeeId());
        transaction.setAmount(request.getAmount());
        transaction.setCurrency(request.getCurrency());
        transaction.setType(request.getType());
        transaction.setStatus(request.getStatus());
        transaction.setTransactionTime(request.getTransactionTime());
        transaction.setDescription(request.getDescription());
        transaction.setId(UUID.randomUUID());
        Instant now = Instant.now();
        transaction.setCreatedAt(now);
        transaction.setUpdatedAt(now);

        Transaction saved = transactionRepository.save(transaction);

        boolean violated = ruleEngineService.evaluate(saved);
        if (violated && saved.getStatus() == TransactionStatus.COMPLETED) {
            saved.setStatus(TransactionStatus.PENDING_APPROVAL);
            saved.setReviewNote("Rule violation detected. Operator approval required.");
            saved.setUpdatedAt(Instant.now());
            saved = transactionRepository.update(saved);
            // Note: alertService.createAlertForRuleTrigger is already called by ruleEngineService.evaluate()
            // but confirming transaction is persisted with PENDING_APPROVAL status before alert reference is resolved
        }

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(UUID id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found for id: " + id));
        return toResponse(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<TransactionResponse> findTransactions(TransactionFilterRequest filter, int page, int size, String sortBy, String sortDir) {
        validateRange(filter);
        validateSorting(sortBy, sortDir);

        Sort.Direction direction = Sort.Direction.fromString(sortDir);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<Transaction> result = transactionRepository.findByFilter(filter, pageable);

        PagedResponse<TransactionResponse> response = new PagedResponse<>();
        response.setContent(result.stream().map(this::toResponse).toList());
        response.setPage(result.getNumber());
        response.setSize(result.getSize());
        response.setTotalElements(result.getTotalElements());
        response.setTotalPages(result.getTotalPages());
        response.setFirst(result.isFirst());
        response.setLast(result.isLast());
        return response;
    }

    @Override
    @Transactional
    public TransactionResponse approve(UUID id, TransactionDecisionRequest request) {
        Transaction transaction = getOrThrow(id);
        if (transaction.getStatus() != TransactionStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Only pending transactions can be approved");
        }

        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setReviewedBy(request.operatorId());
        transaction.setReviewedAt(Instant.now());
        transaction.setReviewNote(normalizeNote(request.note(), "Approved by operator"));
        transaction.setUpdatedAt(Instant.now());

        Transaction updated = transactionRepository.update(transaction);
        alertService.resolveAlertsForTransactionDecision(id, request.operatorId(), true, request.note());
        return toResponse(updated);
    }

    @Override
    @Transactional
    public TransactionResponse reject(UUID id, TransactionDecisionRequest request) {
        Transaction transaction = getOrThrow(id);
        if (transaction.getStatus() != TransactionStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Only pending transactions can be rejected");
        }

        transaction.setStatus(TransactionStatus.REJECTED);
        transaction.setReviewedBy(request.operatorId());
        transaction.setReviewedAt(Instant.now());
        transaction.setReviewNote(normalizeNote(request.note(), "Rejected by operator"));
        transaction.setUpdatedAt(Instant.now());

        Transaction updated = transactionRepository.update(transaction);
        alertService.resolveAlertsForTransactionDecision(id, request.operatorId(), false, request.note());
        return toResponse(updated);
    }

    @Override
    @Transactional
    public TransactionResponse requestRollback(UUID id, TransactionRollbackRequest request) {
        Transaction transaction = getOrThrow(id);
        if (transaction.getStatus() != TransactionStatus.COMPLETED) {
            throw new BadRequestException("Rollback can only be requested for completed transactions");
        }

        transaction.setStatus(TransactionStatus.ROLLBACK_REQUESTED);
        transaction.setRollbackReasonCode(request.reasonCode().trim().toUpperCase(Locale.ROOT));
        transaction.setRollbackReasonDetail(request.reasonDetail().trim());
        transaction.setRollbackRequestedBy(request.requestedBy().trim());
        transaction.setRollbackSupportingReference(normalizeOptional(request.supportingReference()));
        transaction.setRollbackRequestedAt(Instant.now());
        transaction.setUpdatedAt(Instant.now());

        return toResponse(transactionRepository.update(transaction));
    }

    @Override
    @Transactional
    public TransactionResponse approveRollback(UUID id, TransactionRollbackDecisionRequest request) {
        Transaction transaction = getOrThrow(id);
        if (transaction.getStatus() != TransactionStatus.ROLLBACK_REQUESTED) {
            throw new BadRequestException("Only rollback-requested transactions can be approved for refund");
        }

        Instant now = Instant.now();
        transaction.setRollbackReviewedBy(request.operatorId().trim());
        transaction.setRollbackReviewedAt(now);
        transaction.setRollbackReviewNote(normalizeNote(request.note(), "Rollback approved and refunded"));
        transaction.setStatus(TransactionStatus.REFUNDED);
        transaction.setRefundedAt(now);
        transaction.setUpdatedAt(now);

        Transaction refund = buildRefundTransaction(transaction, now);
        Transaction savedRefund = transactionRepository.save(refund);
        transaction.setRefundTransactionId(savedRefund.getId());

        return toResponse(transactionRepository.update(transaction));
    }

    @Override
    @Transactional
    public TransactionResponse rejectRollback(UUID id, TransactionRollbackDecisionRequest request) {
        Transaction transaction = getOrThrow(id);
        if (transaction.getStatus() != TransactionStatus.ROLLBACK_REQUESTED) {
            throw new BadRequestException("Only rollback-requested transactions can be rejected");
        }

        transaction.setStatus(TransactionStatus.ROLLBACK_REJECTED);
        transaction.setRollbackReviewedBy(request.operatorId().trim());
        transaction.setRollbackReviewedAt(Instant.now());
        transaction.setRollbackReviewNote(normalizeNote(request.note(), "Rollback rejected"));
        transaction.setUpdatedAt(Instant.now());

        return toResponse(transactionRepository.update(transaction));
    }

    private Transaction getOrThrow(UUID id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found for id: " + id));
    }

    private Transaction buildRefundTransaction(Transaction original, Instant now) {
        Transaction refund = new Transaction();
        refund.setId(UUID.randomUUID());
        refund.setAccountId(original.getAccountId());
        refund.setPayeeId(original.getPayeeId());
        refund.setAmount(original.getAmount().negate());
        refund.setCurrency(original.getCurrency());
        refund.setType(original.getType());
        refund.setStatus(TransactionStatus.COMPLETED);
        refund.setTransactionTime(now);
        refund.setDescription("Refund for " + original.getId());
        refund.setCreatedAt(now);
        refund.setUpdatedAt(now);
        refund.setReviewNote("System-generated refund");
        refund.setRefundedForTransactionId(original.getId());
        return refund;
    }

    private String normalizeNote(String note, String defaultText) {
        if (note == null || note.isBlank()) {
            return defaultText;
        }
        return note;
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private void validateRange(TransactionFilterRequest filter) {
        if (filter.getMinAmount() != null && filter.getMaxAmount() != null
                && filter.getMinAmount().compareTo(filter.getMaxAmount()) > 0) {
            throw new BadRequestException("minAmount cannot be greater than maxAmount");
        }

        if (filter.getFromTime() != null && filter.getToTime() != null
                && filter.getFromTime().isAfter(filter.getToTime())) {
            throw new BadRequestException("fromTime cannot be after toTime");
        }
    }

    private void validateSorting(String sortBy, String sortDir) {
        if (!SORTABLE_FIELDS.contains(sortBy)) {
            throw new BadRequestException("Unsupported sortBy field: " + sortBy);
        }

        if (!"asc".equalsIgnoreCase(sortDir) && !"desc".equalsIgnoreCase(sortDir)) {
            throw new BadRequestException("sortDir must be either 'asc' or 'desc'");
        }
    }

    private TransactionResponse toResponse(Transaction transaction) {
        TransactionResponse response = new TransactionResponse();
        response.setId(transaction.getId());
        response.setAccountId(transaction.getAccountId());
        response.setPayeeId(transaction.getPayeeId());
        response.setAmount(transaction.getAmount());
        response.setCurrency(transaction.getCurrency());
        response.setType(transaction.getType());
        response.setStatus(transaction.getStatus());
        response.setTransactionTime(transaction.getTransactionTime());
        response.setDescription(transaction.getDescription());
        response.setCreatedAt(transaction.getCreatedAt());
        response.setUpdatedAt(transaction.getUpdatedAt());
        response.setReviewedBy(transaction.getReviewedBy());
        response.setReviewedAt(transaction.getReviewedAt());
        response.setReviewNote(transaction.getReviewNote());
        response.setRollbackReasonCode(transaction.getRollbackReasonCode());
        response.setRollbackReasonDetail(transaction.getRollbackReasonDetail());
        response.setRollbackRequestedBy(transaction.getRollbackRequestedBy());
        response.setRollbackRequestedAt(transaction.getRollbackRequestedAt());
        response.setRollbackSupportingReference(transaction.getRollbackSupportingReference());
        response.setRollbackReviewedBy(transaction.getRollbackReviewedBy());
        response.setRollbackReviewedAt(transaction.getRollbackReviewedAt());
        response.setRollbackReviewNote(transaction.getRollbackReviewNote());
        response.setRefundedAt(transaction.getRefundedAt());
        response.setRefundTransactionId(transaction.getRefundTransactionId());
        response.setRefundedForTransactionId(transaction.getRefundedForTransactionId());
        return response;
    }
}
