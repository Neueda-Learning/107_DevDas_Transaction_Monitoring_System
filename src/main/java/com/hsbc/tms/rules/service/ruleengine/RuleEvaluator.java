package com.hsbc.tms.rules.service.ruleengine;

import com.hsbc.tms.rules.entity.MonitoringRule;
import com.hsbc.tms.rules.model.RuleType;
import com.hsbc.tms.transaction.model.Transaction;
import java.util.List;

public interface RuleEvaluator {

    RuleType supportedType();

    RuleEvaluationResult evaluate(Transaction transaction, MonitoringRule rule);

    record RuleEvaluationResult(boolean triggered, String reason, List<Transaction> triggeringTransactions) {
    }
}

