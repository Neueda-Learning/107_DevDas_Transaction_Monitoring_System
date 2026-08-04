package com.hsbc.tms.alerts.dto;

import com.hsbc.tms.rules.model.AlertSeverity;
import com.hsbc.tms.rules.model.RuleType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record CreateAlertRequest(
        @Schema(description = "Rule name that generated or is associated with this alert", example = "High Value Transaction Rule")
        @NotBlank String ruleName,
        @Schema(description = "Rule type", example = "AMOUNT_THRESHOLD")
        @NotNull RuleType ruleType,
        @Schema(description = "Alert severity", example = "HIGH")
        @NotNull AlertSeverity severity,
        @Schema(description = "Alert message", example = "Transaction amount exceeded threshold")
        @NotBlank @Size(max = 1000) String message,
        @Schema(description = "Operator creating the alert", example = "analyst-01")
        @NotBlank @Size(max = 100) String operatorId,
        @Schema(description = "Optional note for initial history record", example = "Created manually from investigation queue")
        @Size(max = 1000) String note,
        @Schema(description = "Optional list of triggering transaction IDs")
        List<UUID> triggeringTransactionIds) {
}


