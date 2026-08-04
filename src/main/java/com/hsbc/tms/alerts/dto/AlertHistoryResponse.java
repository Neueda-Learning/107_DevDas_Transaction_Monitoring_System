package com.hsbc.tms.alerts.dto;

import com.hsbc.tms.alerts.model.AlertStatus;
import java.time.Instant;

public record AlertHistoryResponse(
        Long id,
        AlertStatus fromStatus,
        AlertStatus toStatus,
        String note,
        String changedBy,
        Instant createdAt) {
}


