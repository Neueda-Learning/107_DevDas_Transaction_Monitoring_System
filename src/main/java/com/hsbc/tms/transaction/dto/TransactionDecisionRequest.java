package com.hsbc.tms.transaction.dto;

import jakarta.validation.constraints.NotBlank;

public record TransactionDecisionRequest(
        @NotBlank String operatorId,
        String note) {
}
