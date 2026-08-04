package com.hsbc.tms.transaction.service;

import com.hsbc.tms.common.dto.PagedResponse;
import com.hsbc.tms.common.exception.BadRequestException;
import com.hsbc.tms.common.exception.ResourceNotFoundException;
import com.hsbc.tms.transaction.dto.CreateTransactionRequest;
import com.hsbc.tms.transaction.dto.TransactionFilterRequest;
import com.hsbc.tms.transaction.dto.TransactionResponse;
import com.hsbc.tms.transaction.model.Transaction;
import com.hsbc.tms.transaction.repository.TransactionRepository;
import java.util.List;
import java.time.Instant;
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

    public TransactionServiceImpl(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
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
        return response;
    }
}


