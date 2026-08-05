package com.hsbc.tms.rules.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hsbc.tms.alerts.service.AlertService;
import com.hsbc.tms.rules.entity.MonitoringRule;
import com.hsbc.tms.rules.entity.RuleExecutionHistory;
import com.hsbc.tms.rules.model.AlertSeverity;
import com.hsbc.tms.rules.model.RuleExecutionOutcome;
import com.hsbc.tms.rules.model.RuleType;
import com.hsbc.tms.rules.repository.MonitoringRuleRepository;
import com.hsbc.tms.rules.repository.RuleExecutionHistoryRepository;
import com.hsbc.tms.rules.service.ruleengine.RuleEvaluator;
import com.hsbc.tms.transaction.model.Transaction;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RuleEngineServiceTest {

    @Mock
    private MonitoringRuleRepository ruleRepository;

    @Mock
    private RuleExecutionHistoryRepository ruleExecutionHistoryRepository;

    @Mock
    private AlertService alertService;

    @Mock
    private RuleEvaluator amountEvaluator;

    @Mock
    private RuleEvaluator velocityEvaluator;

    private RuleEngineService service;

    @BeforeEach
    void setUp() {
        when(amountEvaluator.supportedType()).thenReturn(RuleType.AMOUNT_THRESHOLD);
        when(velocityEvaluator.supportedType()).thenReturn(RuleType.VELOCITY);
        service = new RuleEngineService(
                ruleRepository,
                ruleExecutionHistoryRepository,
                alertService,
                List.of(amountEvaluator, velocityEvaluator));
    }

    @Test
    void evaluate_returnsFalseWhenNoActiveRules() {
        when(ruleRepository.findByActiveTrue()).thenReturn(List.of());

        boolean violated = service.evaluate(buildTransaction("A-1", "P-1", "100.00"));

        assertThat(violated).isFalse();
        verify(ruleExecutionHistoryRepository, never()).save(any(RuleExecutionHistory.class));
        verify(alertService, never()).createAlertForRuleTrigger(any(), any(), any(), any());
    }

    @Test
    void evaluate_savesNotTriggeredWhenEvaluatorMissing() {
        MonitoringRule rule = buildRule(5L, RuleType.DAILY_LIMIT);
        when(ruleRepository.findByActiveTrue()).thenReturn(List.of(rule));

        boolean violated = service.evaluate(buildTransaction("A-1", "P-1", "100.00"));

        assertThat(violated).isFalse();
        ArgumentCaptor<RuleExecutionHistory> captor = ArgumentCaptor.forClass(RuleExecutionHistory.class);
        verify(ruleExecutionHistoryRepository).save(captor.capture());
        RuleExecutionHistory history = captor.getValue();
        assertThat(history.getRuleId()).isEqualTo(5L);
        assertThat(history.getOutcome()).isEqualTo(RuleExecutionOutcome.NOT_TRIGGERED);
        assertThat(history.getMessage()).contains("No evaluator registered");
        verify(alertService, never()).createAlertForRuleTrigger(any(), any(), any(), any());
    }

    @Test
    void evaluate_savesExecutionHistoryAndCreatesAlertsForTriggeredRules() {
        MonitoringRule amountRule = buildRule(11L, RuleType.AMOUNT_THRESHOLD);
        MonitoringRule velocityRule = buildRule(12L, RuleType.VELOCITY);
        Transaction transaction = buildTransaction("A-9", "P-9", "15000.00");

        when(ruleRepository.findByActiveTrue()).thenReturn(List.of(amountRule, velocityRule));

        RuleEvaluator.RuleEvaluationResult triggered = new RuleEvaluator.RuleEvaluationResult(
                true,
                "Amount exceeded",
                List.of(transaction));
        RuleEvaluator.RuleEvaluationResult notTriggered = new RuleEvaluator.RuleEvaluationResult(
                false,
                "Rule conditions not met for transaction",
                List.of(transaction));

        when(amountEvaluator.evaluate(transaction, amountRule)).thenReturn(triggered);
        when(velocityEvaluator.evaluate(transaction, velocityRule)).thenReturn(notTriggered);

        boolean violated = service.evaluate(transaction);

        assertThat(violated).isTrue();

        ArgumentCaptor<RuleExecutionHistory> historyCaptor = ArgumentCaptor.forClass(RuleExecutionHistory.class);
        verify(ruleExecutionHistoryRepository, org.mockito.Mockito.times(2)).save(historyCaptor.capture());
        List<RuleExecutionHistory> historyRows = historyCaptor.getAllValues();

        assertThat(historyRows.get(0).getOutcome()).isEqualTo(RuleExecutionOutcome.TRIGGERED);
        assertThat(historyRows.get(1).getOutcome()).isEqualTo(RuleExecutionOutcome.NOT_TRIGGERED);
        assertThat(historyRows.get(0).getExecutionId()).isEqualTo(historyRows.get(1).getExecutionId());

        verify(alertService).createAlertForRuleTrigger(amountRule, transaction, "Amount exceeded", List.of(transaction));
    }

    private MonitoringRule buildRule(Long id, RuleType type) {
        MonitoringRule rule = new MonitoringRule();
        rule.setId(id);
        rule.setName("rule-" + id);
        rule.setType(type);
        rule.setSeverity(AlertSeverity.MEDIUM);
        rule.setActive(true);
        return rule;
    }

    private Transaction buildTransaction(String accountId, String payeeId, String amount) {
        Transaction transaction = new Transaction();
        transaction.setId(UUID.randomUUID());
        transaction.setAccountId(accountId);
        transaction.setPayeeId(payeeId);
        transaction.setAmount(new BigDecimal(amount));
        transaction.setTransactionTime(Instant.parse("2026-08-05T12:00:00Z"));
        return transaction;
    }
}

