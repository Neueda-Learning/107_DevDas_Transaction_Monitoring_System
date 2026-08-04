package com.hsbc.tms.rules.service;

import com.hsbc.tms.common.exception.BadRequestException;
import com.hsbc.tms.common.exception.ConflictException;
import com.hsbc.tms.common.exception.ResourceNotFoundException;
import com.hsbc.tms.rules.dto.RuleAuditHistoryResponse;
import com.hsbc.tms.rules.dto.RuleCreateRequest;
import com.hsbc.tms.rules.dto.RuleResponse;
import com.hsbc.tms.rules.dto.RuleStatsResponse;
import com.hsbc.tms.rules.dto.RuleStatusUpdateRequest;
import com.hsbc.tms.rules.dto.RuleUpdateRequest;
import com.hsbc.tms.rules.entity.MonitoringRule;
import com.hsbc.tms.rules.entity.RuleAuditHistory;
import com.hsbc.tms.rules.model.AlertSeverity;
import com.hsbc.tms.rules.model.RuleAuditAction;
import com.hsbc.tms.rules.model.RuleType;
import com.hsbc.tms.rules.repository.MonitoringRuleRepository;
import com.hsbc.tms.rules.repository.RuleAuditHistoryRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RuleService {

    private final MonitoringRuleRepository ruleRepository;
    private final RuleAuditHistoryRepository ruleAuditHistoryRepository;
    private final RuleValidationService ruleValidationService;

    public RuleService(
            MonitoringRuleRepository ruleRepository,
            RuleAuditHistoryRepository ruleAuditHistoryRepository,
            RuleValidationService ruleValidationService) {
        this.ruleRepository = ruleRepository;
        this.ruleAuditHistoryRepository = ruleAuditHistoryRepository;
        this.ruleValidationService = ruleValidationService;
    }

    @Transactional(readOnly = true)
    public List<RuleResponse> getRules(Boolean active, RuleType type, AlertSeverity severity) {
        return ruleRepository.search(active, type, severity).stream()
                .map(RuleApiMapper::toRuleResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RuleStatsResponse getRuleStats() {
        long totalRules = ruleRepository.count();
        long activeRules = ruleRepository.countByActiveTrue();
        long inactiveRules = ruleRepository.countByActiveFalse();
        Map<RuleType, Long> rulesByType = ruleRepository.countGroupedByType();
        Map<AlertSeverity, Long> rulesBySeverity = ruleRepository.countGroupedBySeverity();

        return new RuleStatsResponse(totalRules, activeRules, inactiveRules, rulesByType, rulesBySeverity);
    }

    @Transactional(readOnly = true)
    public List<RuleAuditHistoryResponse> getRuleHistory(Long id) {
        if (!ruleRepository.existsById(id)) {
            throw new ResourceNotFoundException("Rule with id " + id + " not found");
        }

        return ruleAuditHistoryRepository.findByRuleIdOrderByChangedAtAsc(id).stream()
                .map(RuleApiMapper::toRuleAuditHistoryResponse)
                .toList();
    }

    @Transactional
    public RuleResponse createRule(RuleCreateRequest request) {
        ruleValidationService.validateForCreate(request);

        String normalizedName = request.name().trim();
        if (ruleRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new ConflictException("Rule name already exists: " + normalizedName);
        }

        MonitoringRule rule = new MonitoringRule();
        rule.setName(normalizedName);
        rule.setType(request.type());
        rule.setSeverity(request.severity());
        rule.setActive(request.active() == null || request.active());
        applyTypeSpecificFields(
                rule,
                request.type(),
                request.amountThreshold(),
                request.transactionCountThreshold(),
                request.timeWindowMinutes());

        MonitoringRule saved = ruleRepository.save(rule);
        recordAudit(saved.getId(), RuleAuditAction.CREATED, null, snapshot(saved));
        return RuleApiMapper.toRuleResponse(saved);
    }

    @Transactional
    public RuleResponse updateRule(Long id, RuleUpdateRequest request) {
        MonitoringRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rule with id " + id + " not found"));
        String previousValues = snapshot(rule);

        ruleValidationService.validateForUpdate(request);

        String normalizedName = request.name().trim();
        if (ruleRepository.existsByNameIgnoreCaseAndIdNot(normalizedName, id)) {
            throw new ConflictException("Rule name already exists: " + normalizedName);
        }

        rule.setName(normalizedName);
        rule.setType(request.type());
        rule.setSeverity(request.severity());
        rule.setActive(Boolean.TRUE.equals(request.active()));
        applyTypeSpecificFields(
                rule,
                request.type(),
                request.amountThreshold(),
                request.transactionCountThreshold(),
                request.timeWindowMinutes());

        MonitoringRule saved = ruleRepository.save(rule);
        recordAudit(saved.getId(), RuleAuditAction.UPDATED, previousValues, snapshot(saved));
        return RuleApiMapper.toRuleResponse(saved);
    }

    @Transactional
    public RuleResponse updateRuleStatus(Long id, RuleStatusUpdateRequest request) {
        MonitoringRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rule with id " + id + " not found"));
        String previousValues = snapshot(rule);

        if (rule.isActive() == request.active()) {
            String status = request.active() ? "active" : "inactive";
            throw new BadRequestException("Rule with id " + id + " is already " + status);
        }

        rule.setActive(request.active());
        MonitoringRule saved = ruleRepository.save(rule);
        RuleAuditAction action = request.active() ? RuleAuditAction.ACTIVATED : RuleAuditAction.DEACTIVATED;
        recordAudit(saved.getId(), action, previousValues, snapshot(saved));
        return RuleApiMapper.toRuleResponse(saved);
    }

    @Transactional
    public RuleResponse softDeleteRule(Long id) {
        MonitoringRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rule with id " + id + " not found"));
        String previousValues = snapshot(rule);

        rule.setActive(false);
        MonitoringRule saved = ruleRepository.save(rule);
        recordAudit(saved.getId(), RuleAuditAction.DELETED, previousValues, snapshot(saved));
        return RuleApiMapper.toRuleResponse(saved);
    }

    private void applyTypeSpecificFields(
            MonitoringRule rule,
            RuleType type,
            java.math.BigDecimal amountThreshold,
            Integer transactionCountThreshold,
            Integer timeWindowMinutes) {
        switch (type) {
            case AMOUNT_THRESHOLD, DAILY_LIMIT -> {
                rule.setAmountThreshold(amountThreshold);
                rule.setTransactionCountThreshold(null);
                rule.setTimeWindowMinutes(null);
            }
            case VELOCITY -> {
                rule.setAmountThreshold(null);
                rule.setTransactionCountThreshold(transactionCountThreshold);
                rule.setTimeWindowMinutes(timeWindowMinutes);
            }
            case NEW_PAYEE -> {
                rule.setAmountThreshold(null);
                rule.setTransactionCountThreshold(null);
                rule.setTimeWindowMinutes(null);
            }
            default -> throw new IllegalStateException("Unexpected rule type: " + type);
        }
    }

    private void recordAudit(Long ruleId, RuleAuditAction action, String previousValues, String newValues) {
        RuleAuditHistory history = new RuleAuditHistory();
        history.setRuleId(ruleId);
        history.setAction(action);
        history.setPreviousValues(previousValues);
        history.setNewValues(newValues);
        history.setChangedAt(Instant.now());
        history.setChangedBy("SYSTEM");
        ruleAuditHistoryRepository.save(history);
    }

    private String snapshot(MonitoringRule rule) {
        return "{" +
                "id=" + rule.getId() +
                ",name='" + rule.getName() + "'" +
                ",type=" + rule.getType() +
                ",severity=" + rule.getSeverity() +
                ",active=" + rule.isActive() +
                ",amountThreshold=" + rule.getAmountThreshold() +
                ",transactionCountThreshold=" + rule.getTransactionCountThreshold() +
                ",timeWindowMinutes=" + rule.getTimeWindowMinutes() +
                ",createdAt=" + rule.getCreatedAt() +
                "}";
    }
}

