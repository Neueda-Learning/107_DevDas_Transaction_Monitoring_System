package com.hsbc.tms.simulation.controller;

import com.hsbc.tms.simulation.dto.SimulationRequest;
import com.hsbc.tms.simulation.dto.SimulationResponse;
import com.hsbc.tms.simulation.service.SimulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
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
    public SimulationResponse generate(@Valid @RequestBody SimulationRequest request) {
        List<UUID> ids = simulationService.generate(request.count());
        return new SimulationResponse(request.count(), ids.size(), ids);
    }
}
