package com.hsbc.tms.rules.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FeatureRequestStatusUpdateRequest(
        @Schema(description = "New status", example = "BANK_APPROVAL")
        @NotBlank String status,

        @Schema(description = "Optional note from admin/developer")
        @Size(max = 1000) String adminNote
) {}

