package com.hsbc.tms.rules.service.ruleengine;

import static org.assertj.core.api.Assertions.assertThat;

import com.hsbc.tms.rules.entity.MonitoringRule;
import com.hsbc.tms.rules.model.RuleType;
import com.hsbc.tms.transaction.model.Transaction;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AmountThresholdRuleEvaluatorTest {

    private AmountThresholdRuleEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new AmountThresholdRuleEvaluator();
    }

    @Test
    void supportedType_returnsAmountThreshold() {
        assertThat(evaluator.supportedType()).isEqualTo(RuleType.AMOUNT_THRESHOLD);
    }

    @Test
    void evaluate_returnsNotTriggeredWhenThresholdMissing() {
        MonitoringRule rule = new MonitoringRule();
        Transaction tx = buildTransaction("500.00");

        RuleEvaluator.RuleEvaluationResult result = evaluator.evaluate(tx, rule);

        assertThat(result.triggered()).isFalse();
        assertThat(result.reason()).isEqualTo("amountThreshold is not configured");
        assertThat(result.triggeringTransactions()).containsExactly(tx);
    }

    @Test
    void evaluate_returnsTriggeredWhenAmountExceedsThreshold() {
        MonitoringRule rule = new MonitoringRule();
        rule.setAmountThreshold(new BigDecimal("1000.00"));
        Transaction tx = buildTransaction("1200.00");

        RuleEvaluator.RuleEvaluationResult result = evaluator.evaluate(tx, rule);

        assertThat(result.triggered()).isTrue();
        assertThat(result.reason()).contains("exceeded threshold");
    }

    @Test
    void evaluate_returnsNotTriggeredWhenAmountDoesNotExceedThreshold() {
        MonitoringRule rule = new MonitoringRule();
        rule.setAmountThreshold(new BigDecimal("1000.00"));
        Transaction tx = buildTransaction("999.99");

        RuleEvaluator.RuleEvaluationResult result = evaluator.evaluate(tx, rule);

        assertThat(result.triggered()).isFalse();
        assertThat(result.reason()).isEqualTo("Rule conditions not met for transaction");
    }

    private Transaction buildTransaction(String amount) {
        Transaction tx = new Transaction();
        tx.setId(UUID.randomUUID());
        tx.setAmount(new BigDecimal(amount));
        tx.setTransactionTime(Instant.parse("2026-08-05T12:00:00Z"));
        return tx;
    }
}

