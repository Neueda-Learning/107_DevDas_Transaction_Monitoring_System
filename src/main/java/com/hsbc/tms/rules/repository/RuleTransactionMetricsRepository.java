package com.hsbc.tms.rules.repository;

import com.hsbc.tms.transaction.model.Transaction;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface RuleTransactionMetricsRepository {

    long countByAccountIdAndTransactionTimeBetween(String accountId, Instant fromTime, Instant toTime);

    List<Transaction> findByAccountIdAndTransactionTimeBetween(String accountId, Instant fromTime, Instant toTime);

    long countByAccountIdAndPayeeIdAndTransactionTimeBefore(String accountId, String payeeId, Instant beforeTime);

    BigDecimal sumAmountByAccountAndTransactionTimeRange(String accountId, Instant fromTime, Instant toTime);
}

