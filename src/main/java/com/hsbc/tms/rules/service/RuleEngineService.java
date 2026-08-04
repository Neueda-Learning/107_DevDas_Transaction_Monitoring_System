package com.hsbc.tms.rules.service;

import com.hsbc.tms.rules.entity.MonitoringRule;
import com.hsbc.tms.rules.entity.RuleExecutionHistory;
import com.hsbc.tms.rules.model.RuleExecutionOutcome;
import com.hsbc.tms.rules.model.RuleType;
import com.hsbc.tms.rules.repository.MonitoringRuleRepository;
import com.hsbc.tms.rules.repository.RuleExecutionHistoryRepository;
import com.hsbc.tms.rules.service.ruleengine.RuleEvaluator;
import com.hsbc.tms.transaction.model.Transaction;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RuleEngineService {

    private final MonitoringRuleRepository ruleRepository;
    private final RuleExecutionHistoryRepository ruleExecutionHistoryRepository;
    private final Map<RuleType, RuleEvaluator> evaluatorsByType;

    public RuleEngineService(
            MonitoringRuleRepository ruleRepository,
            RuleExecutionHistoryRepository ruleExecutionHistoryRepository,
            List<RuleEvaluator> evaluators) {
        this.ruleRepository = ruleRepository;
        this.ruleExecutionHistoryRepository = ruleExecutionHistoryRepository;
        this.evaluatorsByType = buildEvaluatorMap(evaluators);
    }

    public boolean evaluate(Transaction transaction) {
        List<MonitoringRule> activeRules = ruleRepository.findByActiveTrue();
        boolean violated = false;
        UUID executionId = UUID.randomUUID();

        for (MonitoringRule rule : activeRules) {
            RuleEvaluator evaluator = evaluatorsByType.get(rule.getType());
            if (evaluator == null) {
                saveExecutionHistory(
                        executionId,
                        rule,
                        transaction,
                        RuleExecutionOutcome.NOT_TRIGGERED,
                        "No evaluator registered for rule type " + rule.getType());
                continue;
            }

            RuleEvaluator.RuleEvaluationResult result = evaluator.evaluate(transaction, rule);
            if (result.triggered()) {
                saveExecutionHistory(
                        executionId,
                        rule,
                        transaction,
                        RuleExecutionOutcome.TRIGGERED,
                        result.reason());
                violated = true;
            } else {
                saveExecutionHistory(
                        executionId,
                        rule,
                        transaction,
                        RuleExecutionOutcome.NOT_TRIGGERED,
                        result.reason());
            }
        }

        return violated;
    }

    private Map<RuleType, RuleEvaluator> buildEvaluatorMap(List<RuleEvaluator> evaluators) {
        Map<RuleType, RuleEvaluator> map = new EnumMap<>(RuleType.class);
        for (RuleEvaluator evaluator : evaluators) {
            map.put(evaluator.supportedType(), evaluator);
        }
        return map;
    }

    private void saveExecutionHistory(
            UUID executionId,
            MonitoringRule rule,
            Transaction transaction,
            RuleExecutionOutcome outcome,
            String message) {
        RuleExecutionHistory history = new RuleExecutionHistory();
        history.setExecutionId(executionId);
        history.setRuleId(rule.getId());
        history.setTransactionId(transaction.getId());
        history.setOutcome(outcome);
        history.setMessage(message);
        history.setCreatedAt(Instant.now());
        ruleExecutionHistoryRepository.save(history);
    }
}

