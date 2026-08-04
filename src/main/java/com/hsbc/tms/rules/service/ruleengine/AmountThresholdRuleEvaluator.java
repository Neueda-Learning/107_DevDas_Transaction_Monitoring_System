package com.hsbc.tms.rules.service.ruleengine;

import com.hsbc.tms.rules.entity.MonitoringRule;
import com.hsbc.tms.rules.model.RuleType;
import com.hsbc.tms.transaction.model.Transaction;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AmountThresholdRuleEvaluator implements RuleEvaluator {

    @Override
    public RuleType supportedType() {
        return RuleType.AMOUNT_THRESHOLD;
    }

    @Override
    public RuleEvaluationResult evaluate(Transaction transaction, MonitoringRule rule) {
        if (rule.getAmountThreshold() == null) {
            return new RuleEvaluationResult(false, "amountThreshold is not configured", List.of(transaction));
        }

        if (transaction.getAmount().compareTo(rule.getAmountThreshold()) > 0) {
            String reason = "Transaction amount " + transaction.getAmount() + " exceeded threshold " + rule.getAmountThreshold();
            return new RuleEvaluationResult(true, reason, List.of(transaction));
        }

        return new RuleEvaluationResult(false, "Rule conditions not met for transaction", List.of(transaction));
    }
}

