package com.hsbc.tms.transaction.repository;

import com.hsbc.tms.transaction.dto.TransactionFilterRequest;
import com.hsbc.tms.transaction.model.Transaction;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TransactionRepository {

	Transaction save(Transaction transaction);

	Transaction update(Transaction transaction);

	Optional<Transaction> findById(UUID id);

	Page<Transaction> findByFilter(TransactionFilterRequest filter, Pageable pageable);
}


