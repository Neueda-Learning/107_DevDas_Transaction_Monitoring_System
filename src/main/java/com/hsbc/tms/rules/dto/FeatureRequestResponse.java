package com.hsbc.tms.rules.dto;

import java.time.Instant;

public record FeatureRequestResponse(
        Long id,
        String title,
        String description,
        String requestedBy,
        String status,
        String adminNote,
        Instant createdAt,
        Instant updatedAt
) {}

