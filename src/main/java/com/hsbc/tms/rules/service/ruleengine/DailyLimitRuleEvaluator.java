package com.hsbc.tms.rules.service.ruleengine;

import com.hsbc.tms.rules.entity.MonitoringRule;
import com.hsbc.tms.rules.model.RuleType;
import com.hsbc.tms.rules.repository.RuleTransactionMetricsRepository;
import com.hsbc.tms.transaction.model.Transaction;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DailyLimitRuleEvaluator implements RuleEvaluator {

    private final RuleTransactionMetricsRepository metricsRepository;

    public DailyLimitRuleEvaluator(RuleTransactionMetricsRepository metricsRepository) {
        this.metricsRepository = metricsRepository;
    }

    @Override
    public RuleType supportedType() {
        return RuleType.DAILY_LIMIT;
    }

    @Override
    public RuleEvaluationResult evaluate(Transaction transaction, MonitoringRule rule) {
        if (rule.getAmountThreshold() == null) {
            return new RuleEvaluationResult(false, "amountThreshold is not configured", List.of(transaction));
        }

        LocalDate day = LocalDate.ofInstant(transaction.getTransactionTime(), ZoneOffset.UTC);
        Instant startOfDay = day.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endOfDay = day.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).minusNanos(1);

        BigDecimal total = metricsRepository.sumAmountByAccountAndTransactionTimeRange(
                transaction.getAccountId(), startOfDay, endOfDay);

        if (total.compareTo(rule.getAmountThreshold()) > 0) {
            String reason = "Daily limit exceeded for account " + transaction.getAccountId()
                    + ": total=" + total
                    + ", limit=" + rule.getAmountThreshold();
            return new RuleEvaluationResult(true, reason, List.of(transaction));
        }

        return new RuleEvaluationResult(false, "Rule conditions not met for transaction", List.of(transaction));
    }
}

