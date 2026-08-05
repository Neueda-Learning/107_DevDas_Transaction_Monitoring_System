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
class NewPayeeRuleEvaluatorTest {

    @Mock
    private RuleTransactionMetricsRepository metricsRepository;

    private NewPayeeRuleEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new NewPayeeRuleEvaluator(metricsRepository);
    }

    @Test
    void supportedType_returnsNewPayee() {
        assertThat(evaluator.supportedType()).isEqualTo(RuleType.NEW_PAYEE);
    }

    @Test
    void evaluate_returnsTriggeredForFirstPayeeTransaction() {
        Transaction tx = buildTransaction();
        when(metricsRepository.countByAccountIdAndPayeeIdAndTransactionTimeBefore(any(), any(), any())).thenReturn(0L);

        RuleEvaluator.RuleEvaluationResult result = evaluator.evaluate(tx, new MonitoringRule());

        assertThat(result.triggered()).isTrue();
        assertThat(result.reason()).contains("First transaction to new payee");
        assertThat(result.triggeringTransactions()).containsExactly(tx);
    }

    @Test
    void evaluate_returnsNotTriggeredWhenPayeeExists() {
        Transaction tx = buildTransaction();
        when(metricsRepository.countByAccountIdAndPayeeIdAndTransactionTimeBefore(any(), any(), any())).thenReturn(3L);

        RuleEvaluator.RuleEvaluationResult result = evaluator.evaluate(tx, new MonitoringRule());

        assertThat(result.triggered()).isFalse();
        assertThat(result.reason()).isEqualTo("Rule conditions not met for transaction");
    }

    private Transaction buildTransaction() {
        Transaction tx = new Transaction();
        tx.setId(UUID.randomUUID());
        tx.setAccountId("A-20");
        tx.setPayeeId("P-NEW");
        tx.setAmount(new BigDecimal("90.00"));
        tx.setTransactionTime(Instant.parse("2026-08-05T12:00:00Z"));
        return tx;
    }
}

