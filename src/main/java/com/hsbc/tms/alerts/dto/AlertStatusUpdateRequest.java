package com.hsbc.tms.alerts.dto;

import com.hsbc.tms.alerts.model.AlertStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AlertStatusUpdateRequest(
        @NotNull AlertStatus status,
        @NotBlank String operatorId,
        String note) {
}


