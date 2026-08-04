package com.hsbc.tms.rules.dto;

import com.hsbc.tms.rules.model.AlertSeverity;
import com.hsbc.tms.rules.model.RuleType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record RuleUpdateRequest(
        @Schema(description = "Updated rule name", example = "Updated High Value Rule")
        @NotBlank String name,
        @Schema(description = "Updated rule type", example = "VELOCITY")
        @NotNull RuleType type,
        @Schema(description = "Updated alert severity", example = "MEDIUM")
        @NotNull AlertSeverity severity,
        @Schema(description = "Updated active flag", example = "true")
        @NotNull Boolean active,
        @Schema(description = "Threshold amount for AMOUNT_THRESHOLD and DAILY_LIMIT rules", example = "15000.00")
        BigDecimal amountThreshold,
        @Schema(description = "Transaction count threshold for VELOCITY rules", example = "6")
        Integer transactionCountThreshold,
        @Schema(description = "Time window in minutes for VELOCITY rules", example = "10")
        Integer timeWindowMinutes) {
}

