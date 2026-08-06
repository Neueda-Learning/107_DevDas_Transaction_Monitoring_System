package com.hsbc.tms.rules.service.ruleengine;

import static org.assertj.core.api.Assertions.assertThat;

import com.hsbc.tms.rules.entity.MonitoringRule;
import com.hsbc.tms.rules.model.RuleType;
import com.hsbc.tms.transaction.model.Transaction;
import java.util.List;
import org.junit.jupiter.api.Test;

class RuleEvaluatorContractTest {

    @Test
    void ruleEvaluationResult_recordStoresValues() {
        Transaction tx = new Transaction();
        RuleEvaluator.RuleEvaluationResult result =
                new RuleEvaluator.RuleEvaluationResult(true, "reason", List.of(tx));

        assertThat(result.triggered()).isTrue();
        assertThat(result.reason()).isEqualTo("reason");
        assertThat(result.triggeringTransactions()).containsExactly(tx);
    }

    @Test
    void customEvaluator_implementsContractMethods() {
        RuleEvaluator evaluator = new RuleEvaluator() {
            @Override
            public RuleType supportedType() {
                return RuleType.NEW_PAYEE;
            }

            @Override
            public RuleEvaluationResult evaluate(Transaction transaction, MonitoringRule rule) {
                return new RuleEvaluationResult(false, "not-triggered", List.of(transaction));
            }
        };

        Transaction tx = new Transaction();
        RuleEvaluator.RuleEvaluationResult result = evaluator.evaluate(tx, new MonitoringRule());

        assertThat(evaluator.supportedType()).isEqualTo(RuleType.NEW_PAYEE);
        assertThat(result.triggered()).isFalse();
        assertThat(result.reason()).isEqualTo("not-triggered");
        assertThat(result.triggeringTransactions()).containsExactly(tx);
    }
}

