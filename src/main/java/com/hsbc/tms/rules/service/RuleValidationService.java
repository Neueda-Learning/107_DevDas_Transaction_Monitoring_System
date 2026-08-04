package com.hsbc.tms.rules.service;

import com.hsbc.tms.common.exception.BadRequestException;
import com.hsbc.tms.rules.dto.RuleCreateRequest;
import com.hsbc.tms.rules.dto.RuleUpdateRequest;
import com.hsbc.tms.rules.model.AlertSeverity;
import com.hsbc.tms.rules.model.RuleType;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class RuleValidationService {

    public void validateForCreate(RuleCreateRequest request) {
        validateCommonFields(request.name(), request.type(), request.severity());
        validateRuleConfiguration(
                request.type(),
                request.amountThreshold(),
                request.transactionCountThreshold(),
                request.timeWindowMinutes());
    }

    public void validateForUpdate(RuleUpdateRequest request) {
        validateCommonFields(request.name(), request.type(), request.severity());
        validateRuleConfiguration(
                request.type(),
                request.amountThreshold(),
                request.transactionCountThreshold(),
                request.timeWindowMinutes());
    }

    private void validateCommonFields(String name, RuleType type, AlertSeverity severity) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("name is required");
        }
        if (type == null) {
            throw new BadRequestException("type is required");
        }
        if (severity == null) {
            throw new BadRequestException("severity is required");
        }
    }

    private void validateRuleConfiguration(
            RuleType type,
            BigDecimal amountThreshold,
            Integer transactionCountThreshold,
            Integer timeWindowMinutes) {
        switch (type) {
            case AMOUNT_THRESHOLD, DAILY_LIMIT -> requirePositiveAmount(amountThreshold, "amountThreshold");
            case VELOCITY -> {
                requirePositiveInteger(transactionCountThreshold, "transactionCountThreshold");
                requirePositiveInteger(timeWindowMinutes, "timeWindowMinutes");
            }
            case NEW_PAYEE -> {
                // NEW_PAYEE has no extra required thresholds.
            }
            default -> throw new BadRequestException("Unsupported rule type: " + type);
        }
    }

    private void requirePositiveAmount(BigDecimal value, String field) {
        if (value == null || value.signum() <= 0) {
            throw new BadRequestException(field + " must be a positive value");
        }
    }

    private void requirePositiveInteger(Integer value, String field) {
        if (value == null || value <= 0) {
            throw new BadRequestException(field + " must be a positive value");
        }
    }
}

