package com.hsbc.tms.rules.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record RuleStatusUpdateRequest(
        @Schema(description = "Set to true to activate rule, false to deactivate", example = "false")
        @NotNull Boolean active) {
}

