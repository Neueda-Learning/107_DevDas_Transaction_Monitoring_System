package com.hsbc.tms.rules.service.ruleengine;

import com.hsbc.tms.rules.entity.MonitoringRule;
import com.hsbc.tms.rules.model.RuleType;
import com.hsbc.tms.rules.repository.RuleTransactionMetricsRepository;
import com.hsbc.tms.transaction.model.Transaction;
import com.hsbc.tms.transaction.model.TransactionStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class VelocityRuleEvaluator implements RuleEvaluator {

    private final RuleTransactionMetricsRepository metricsRepository;

    public VelocityRuleEvaluator(RuleTransactionMetricsRepository metricsRepository) {
        this.metricsRepository = metricsRepository;
    }

    @Override
    public RuleType supportedType() {
        return RuleType.VELOCITY;
    }

    @Override
    public RuleEvaluationResult evaluate(Transaction transaction, MonitoringRule rule) {
        if (rule.getTransactionCountThreshold() == null || rule.getTimeWindowMinutes() == null) {
            return new RuleEvaluationResult(false, "Velocity thresholds are not configured", List.of(transaction));
        }

        Instant toTime = transaction.getTransactionTime();
        Instant fromTime = toTime.minusSeconds(rule.getTimeWindowMinutes() * 60L);

        // Only count COMPLETED transactions for velocity rule evaluation
        long count = metricsRepository.countByAccountIdAndTransactionTimeBetween(transaction.getAccountId(), fromTime, toTime);
        if (count > rule.getTransactionCountThreshold()) {
            List<Transaction> related = metricsRepository.findByAccountIdAndTransactionTimeBetweenAndStatus(transaction.getAccountId(), fromTime, toTime, TransactionStatus.COMPLETED);
            String reason = "Velocity threshold exceeded for account " + transaction.getAccountId()
                    + ": count=" + count
                    + ", threshold=" + rule.getTransactionCountThreshold()
                    + ", windowMinutes=" + rule.getTimeWindowMinutes();
            return new RuleEvaluationResult(true, reason, related);
        }

        return new RuleEvaluationResult(false, "Rule conditions not met for transaction", List.of(transaction));
    }
}

