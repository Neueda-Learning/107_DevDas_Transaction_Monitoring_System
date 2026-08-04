package com.hsbc.tms.rules.service.ruleengine;

import com.hsbc.tms.rules.entity.MonitoringRule;
import com.hsbc.tms.rules.model.RuleType;
import com.hsbc.tms.rules.repository.RuleTransactionMetricsRepository;
import com.hsbc.tms.transaction.model.Transaction;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class NewPayeeRuleEvaluator implements RuleEvaluator {

    private final RuleTransactionMetricsRepository metricsRepository;

    public NewPayeeRuleEvaluator(RuleTransactionMetricsRepository metricsRepository) {
        this.metricsRepository = metricsRepository;
    }

    @Override
    public RuleType supportedType() {
        return RuleType.NEW_PAYEE;
    }

    @Override
    public RuleEvaluationResult evaluate(Transaction transaction, MonitoringRule rule) {
        long previousCount = metricsRepository.countByAccountIdAndPayeeIdAndTransactionTimeBefore(
                transaction.getAccountId(),
                transaction.getPayeeId(),
                transaction.getTransactionTime());

        if (previousCount == 0) {
            String reason = "First transaction to new payee " + transaction.getPayeeId()
                    + " for account " + transaction.getAccountId();
            return new RuleEvaluationResult(true, reason, List.of(transaction));
        }

        return new RuleEvaluationResult(false, "Rule conditions not met for transaction", List.of(transaction));
    }
}

