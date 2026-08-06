package com.hsbc.tms.rules.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FeatureRequestCreateRequest(
        @Schema(description = "Short title of the requested rule", example = "Block high-risk country transfers")
        @NotBlank @Size(max = 200) String title,

        @Schema(description = "Detailed description of the rule the operator wants implemented")
        @NotBlank @Size(max = 4000) String description,

        @Schema(description = "Operator submitting the request", example = "operator-01")
        @NotBlank @Size(max = 100) String requestedBy
) {}

