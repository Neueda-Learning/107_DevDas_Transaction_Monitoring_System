package com.hsbc.tms.simulation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record SimulationRequest(
        @Min(1) @Max(200) int count) {
}
