package com.hsbc.tms.simulation.controller;

import com.hsbc.tms.simulation.dto.SimulationRequest;
import com.hsbc.tms.simulation.service.SimulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/simulator")
@Tag(name = "Simulator", description = "Generates sample transactions for testing and demos")
public class SimulationController {

    private final SimulationService simulationService;

    public SimulationController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @PostMapping("/generate")
    @Operation(summary = "Generate random sample transactions")
    public Map<String, Object> generate(@Valid @RequestBody SimulationRequest request) {
        return Map.of(
                "createdTransactionIds", simulationService.generate(request.count()),
                "count", request.count());
    }
}
