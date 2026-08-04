package com.hsbc.tms.rules.dto;

import com.hsbc.tms.rules.model.AlertSeverity;
import com.hsbc.tms.rules.model.RuleType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

public record RuleStatsResponse(
        @Schema(description = "Total number of monitoring rules")
        long totalRules,
        @Schema(description = "Number of active monitoring rules")
        long activeRules,
        @Schema(description = "Number of inactive monitoring rules")
        long inactiveRules,
        @Schema(description = "Count of rules grouped by rule type")
        Map<RuleType, Long> rulesByType,
        @Schema(description = "Count of rules grouped by severity")
        Map<AlertSeverity, Long> rulesBySeverity) {
}

