package com.hsbc.tms.rules.dto;

import com.hsbc.tms.rules.model.AlertSeverity;
import com.hsbc.tms.rules.model.RuleType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record RuleCreateRequest(
        @Schema(description = "Unique monitoring rule name", example = "High Value Transaction Rule")
        @NotBlank String name,
        @Schema(description = "Rule type", example = "AMOUNT_THRESHOLD")
        @NotNull RuleType type,
        @Schema(description = "Alert severity used when rule triggers", example = "HIGH")
        @NotNull AlertSeverity severity,
        @Schema(description = "Whether rule is active. Defaults to true when omitted.", example = "true")
        Boolean active,
        @Schema(description = "Threshold amount for AMOUNT_THRESHOLD and DAILY_LIMIT rules", example = "10000.00")
        @DecimalMin(value = "0.01") BigDecimal amountThreshold,
        @Schema(description = "Max transaction count for VELOCITY rules", example = "5")
        @Positive Integer transactionCountThreshold,
        @Schema(description = "Time window in minutes for VELOCITY rules", example = "10")
        @Positive Integer timeWindowMinutes) {
}

