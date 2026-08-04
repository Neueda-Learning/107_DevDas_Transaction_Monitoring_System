package com.hsbc.tms.transaction.dto;

import jakarta.validation.constraints.NotBlank;

public record TransactionRollbackRequest(
        @NotBlank String reasonCode,
        @NotBlank String reasonDetail,
        @NotBlank String requestedBy,
        String supportingReference) {
}
