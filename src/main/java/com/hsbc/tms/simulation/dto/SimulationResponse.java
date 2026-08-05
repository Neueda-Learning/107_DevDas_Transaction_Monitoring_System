package com.hsbc.tms.simulation.dto;

import java.util.List;
import java.util.UUID;

public record SimulationResponse(
        int count,
        int createdCount,
        List<UUID> createdTransactionIds) {
}

