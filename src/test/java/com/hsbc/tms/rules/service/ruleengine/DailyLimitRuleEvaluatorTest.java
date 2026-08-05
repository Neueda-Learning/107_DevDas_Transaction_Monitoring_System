package com.hsbc.tms.rules.service.ruleengine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.hsbc.tms.rules.entity.MonitoringRule;
import com.hsbc.tms.rules.model.RuleType;
import com.hsbc.tms.rules.repository.RuleTransactionMetricsRepository;
import com.hsbc.tms.transaction.model.Transaction;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DailyLimitRuleEvaluatorTest {

    @Mock
    private RuleTransactionMetricsRepository metricsRepository;

    private DailyLimitRuleEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new DailyLimitRuleEvaluator(metricsRepository);
    }

    @Test
    void supportedType_returnsDailyLimit() {
        assertThat(evaluator.supportedType()).isEqualTo(RuleType.DAILY_LIMIT);
    }

    @Test
    void evaluate_returnsNotTriggeredWhenThresholdMissing() {
        MonitoringRule rule = new MonitoringRule();
        Transaction tx = buildTransaction();

        RuleEvaluator.RuleEvaluationResult result = evaluator.evaluate(tx, rule);

        assertThat(result.triggered()).isFalse();
        assertThat(result.reason()).isEqualTo("amountThreshold is not configured");
    }

    @Test
    void evaluate_returnsTriggeredWhenDailyTotalExceedsLimit() {
        MonitoringRule rule = new MonitoringRule();
        rule.setAmountThreshold(new BigDecimal("1000.00"));
        Transaction tx = buildTransaction();

        when(metricsRepository.sumAmountByAccountAndTransactionTimeRange(any(), any(), any()))
                .thenReturn(new BigDecimal("1200.00"));

        RuleEvaluator.RuleEvaluationResult result = evaluator.evaluate(tx, rule);

        assertThat(result.triggered()).isTrue();
        assertThat(result.reason()).contains("Daily limit exceeded");
    }

    @Test
    void evaluate_returnsNotTriggeredWhenDailyTotalDoesNotExceedLimit() {
        MonitoringRule rule = new MonitoringRule();
        rule.setAmountThreshold(new BigDecimal("1000.00"));
        Transaction tx = buildTransaction();

        when(metricsRepository.sumAmountByAccountAndTransactionTimeRange(any(), any(), any()))
                .thenReturn(new BigDecimal("1000.00"));

        RuleEvaluator.RuleEvaluationResult result = evaluator.evaluate(tx, rule);

        assertThat(result.triggered()).isFalse();
        assertThat(result.reason()).isEqualTo("Rule conditions not met for transaction");
    }

    private Transaction buildTransaction() {
        Transaction tx = new Transaction();
        tx.setId(UUID.randomUUID());
        tx.setAccountId("A-33");
        tx.setPayeeId("P-33");
        tx.setAmount(new BigDecimal("200.00"));
        tx.setTransactionTime(Instant.parse("2026-08-05T12:00:00Z"));
        return tx;
    }
}

