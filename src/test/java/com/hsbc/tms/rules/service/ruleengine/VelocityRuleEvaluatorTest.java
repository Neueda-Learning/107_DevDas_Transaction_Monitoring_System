package com.hsbc.tms.rules.service.ruleengine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hsbc.tms.rules.entity.MonitoringRule;
import com.hsbc.tms.rules.model.RuleType;
import com.hsbc.tms.rules.repository.RuleTransactionMetricsRepository;
import com.hsbc.tms.transaction.model.Transaction;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VelocityRuleEvaluatorTest {

    @Mock
    private RuleTransactionMetricsRepository metricsRepository;

    private VelocityRuleEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new VelocityRuleEvaluator(metricsRepository);
    }

    @Test
    void supportedType_returnsVelocity() {
        assertThat(evaluator.supportedType()).isEqualTo(RuleType.VELOCITY);
    }

    @Test
    void evaluate_returnsNotTriggeredWhenThresholdsMissing() {
        MonitoringRule rule = new MonitoringRule();
        Transaction tx = buildTransaction();

        RuleEvaluator.RuleEvaluationResult result = evaluator.evaluate(tx, rule);

        assertThat(result.triggered()).isFalse();
        assertThat(result.reason()).isEqualTo("Velocity thresholds are not configured");
        verify(metricsRepository, never()).countByAccountIdAndTransactionTimeBetween(any(), any(), any());
    }

    @Test
    void evaluate_returnsTriggeredWhenCountExceedsThreshold() {
        MonitoringRule rule = new MonitoringRule();
        rule.setTransactionCountThreshold(5);
        rule.setTimeWindowMinutes(10);

        Transaction tx = buildTransaction();
        Transaction t1 = buildTransaction();
        Transaction t2 = buildTransaction();

        when(metricsRepository.countByAccountIdAndTransactionTimeBetween(any(), any(), any())).thenReturn(6L);
        when(metricsRepository.findByAccountIdAndTransactionTimeBetween(any(), any(), any())).thenReturn(List.of(t1, t2));

        RuleEvaluator.RuleEvaluationResult result = evaluator.evaluate(tx, rule);

        assertThat(result.triggered()).isTrue();
        assertThat(result.reason()).contains("Velocity threshold exceeded");
        assertThat(result.triggeringTransactions()).containsExactly(t1, t2);
    }

    @Test
    void evaluate_returnsNotTriggeredWhenCountDoesNotExceedThreshold() {
        MonitoringRule rule = new MonitoringRule();
        rule.setTransactionCountThreshold(5);
        rule.setTimeWindowMinutes(10);

        Transaction tx = buildTransaction();
        when(metricsRepository.countByAccountIdAndTransactionTimeBetween(any(), any(), any())).thenReturn(5L);

        RuleEvaluator.RuleEvaluationResult result = evaluator.evaluate(tx, rule);

        assertThat(result.triggered()).isFalse();
        assertThat(result.reason()).isEqualTo("Rule conditions not met for transaction");
        verify(metricsRepository, never()).findByAccountIdAndTransactionTimeBetween(any(), any(), any());
    }

    private Transaction buildTransaction() {
        Transaction tx = new Transaction();
        tx.setId(UUID.randomUUID());
        tx.setAccountId("A-11");
        tx.setPayeeId("P-11");
        tx.setAmount(new BigDecimal("250.00"));
        tx.setTransactionTime(Instant.parse("2026-08-05T12:00:00Z"));
        return tx;
    }
}

