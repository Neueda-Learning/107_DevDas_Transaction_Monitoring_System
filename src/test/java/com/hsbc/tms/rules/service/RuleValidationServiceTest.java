package com.hsbc.tms.rules.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hsbc.tms.common.exception.BadRequestException;
import com.hsbc.tms.rules.dto.RuleCreateRequest;
import com.hsbc.tms.rules.dto.RuleUpdateRequest;
import com.hsbc.tms.rules.model.AlertSeverity;
import com.hsbc.tms.rules.model.RuleType;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RuleValidationServiceTest {

    private RuleValidationService service;

    @BeforeEach
    void setUp() {
        service = new RuleValidationService();
    }

    @Test
    void validateForCreate_acceptsValidAmountThresholdRule() {
        RuleCreateRequest request = new RuleCreateRequest(
                "High Amount",
                RuleType.AMOUNT_THRESHOLD,
                AlertSeverity.HIGH,
                true,
                new BigDecimal("10000.00"),
                null,
                null);

        assertThatCode(() -> service.validateForCreate(request)).doesNotThrowAnyException();
    }

    @Test
    void validateForCreate_rejectsBlankName() {
        RuleCreateRequest request = new RuleCreateRequest(
                " ",
                RuleType.AMOUNT_THRESHOLD,
                AlertSeverity.HIGH,
                true,
                new BigDecimal("100.00"),
                null,
                null);

        assertThatThrownBy(() -> service.validateForCreate(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("name is required");
    }

    @Test
    void validateForCreate_rejectsMissingType() {
        RuleCreateRequest request = new RuleCreateRequest(
                "Rule",
                null,
                AlertSeverity.HIGH,
                true,
                new BigDecimal("100.00"),
                null,
                null);

        assertThatThrownBy(() -> service.validateForCreate(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("type is required");
    }

    @Test
    void validateForCreate_rejectsMissingSeverity() {
        RuleCreateRequest request = new RuleCreateRequest(
                "Rule",
                RuleType.AMOUNT_THRESHOLD,
                null,
                true,
                new BigDecimal("100.00"),
                null,
                null);

        assertThatThrownBy(() -> service.validateForCreate(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("severity is required");
    }

    @Test
    void validateForCreate_rejectsMissingAmountForDailyLimit() {
        RuleCreateRequest request = new RuleCreateRequest(
                "Daily Limit",
                RuleType.DAILY_LIMIT,
                AlertSeverity.HIGH,
                true,
                null,
                null,
                null);

        assertThatThrownBy(() -> service.validateForCreate(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("amountThreshold must be a positive value");
    }

    @Test
    void validateForUpdate_rejectsMissingVelocityCount() {
        RuleUpdateRequest request = new RuleUpdateRequest(
                "Velocity",
                RuleType.VELOCITY,
                AlertSeverity.MEDIUM,
                true,
                null,
                null,
                10);

        assertThatThrownBy(() -> service.validateForUpdate(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("transactionCountThreshold must be a positive value");
    }

    @Test
    void validateForCreate_rejectsMissingVelocityTimeWindow() {
        RuleCreateRequest request = new RuleCreateRequest(
                "Velocity",
                RuleType.VELOCITY,
                AlertSeverity.MEDIUM,
                true,
                null,
                5,
                null);

        assertThatThrownBy(() -> service.validateForCreate(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("timeWindowMinutes must be a positive value");
    }

    @Test
    void validateForCreate_acceptsNewPayeeWithoutThresholds() {
        RuleCreateRequest request = new RuleCreateRequest(
                "New Payee",
                RuleType.NEW_PAYEE,
                AlertSeverity.MEDIUM,
                true,
                null,
                null,
                null);

        assertThatCode(() -> service.validateForCreate(request)).doesNotThrowAnyException();
    }

    @Test
    void validateForUpdate_rejectsNonPositiveAmount() {
        RuleUpdateRequest request = new RuleUpdateRequest(
                "Amount Rule",
                RuleType.AMOUNT_THRESHOLD,
                AlertSeverity.HIGH,
                true,
                BigDecimal.ZERO,
                null,
                null);

        assertThatThrownBy(() -> service.validateForUpdate(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("amountThreshold must be a positive value");
    }
}

