package com.hsbc.tms.rules.repository;

import com.hsbc.tms.rules.entity.RuleExecutionHistory;
import java.util.List;
import java.util.UUID;

public interface RuleExecutionHistoryRepository {

    void save(RuleExecutionHistory history);

    List<RuleExecutionHistory> findByTransactionId(UUID transactionId);
}

