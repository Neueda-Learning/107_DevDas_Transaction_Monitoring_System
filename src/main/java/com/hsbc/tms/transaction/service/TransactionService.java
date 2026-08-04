package com.hsbc.tms.transaction.service;

import com.hsbc.tms.common.dto.PagedResponse;
import com.hsbc.tms.transaction.dto.CreateTransactionRequest;
import com.hsbc.tms.transaction.dto.TransactionDecisionRequest;
import com.hsbc.tms.transaction.dto.TransactionFilterRequest;
import com.hsbc.tms.transaction.dto.TransactionResponse;
import com.hsbc.tms.transaction.dto.TransactionRollbackDecisionRequest;
import com.hsbc.tms.transaction.dto.TransactionRollbackRequest;
import java.util.UUID;

public interface TransactionService {

    TransactionResponse createTransaction(CreateTransactionRequest request);

    TransactionResponse getTransactionById(UUID id);

    PagedResponse<TransactionResponse> findTransactions(TransactionFilterRequest filter, int page, int size, String sortBy, String sortDir);

    TransactionResponse approve(UUID id, TransactionDecisionRequest request);

    TransactionResponse reject(UUID id, TransactionDecisionRequest request);

    TransactionResponse requestRollback(UUID id, TransactionRollbackRequest request);

    TransactionResponse approveRollback(UUID id, TransactionRollbackDecisionRequest request);

    TransactionResponse rejectRollback(UUID id, TransactionRollbackDecisionRequest request);
}
