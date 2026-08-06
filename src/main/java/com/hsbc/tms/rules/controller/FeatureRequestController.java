package com.hsbc.tms.rules.controller;

import com.hsbc.tms.rules.dto.FeatureRequestCreateRequest;
import com.hsbc.tms.rules.dto.FeatureRequestResponse;
import com.hsbc.tms.rules.dto.FeatureRequestStatusUpdateRequest;
import com.hsbc.tms.rules.service.FeatureRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rule-requests")
@Tag(name = "Rule Feature Requests", description = "APIs for operators to request new monitoring rule features")
public class FeatureRequestController {

    private final FeatureRequestService service;

    public FeatureRequestController(FeatureRequestService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List all rule feature requests")
    public List<FeatureRequestResponse> list() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a rule feature request by ID")
    public FeatureRequestResponse get(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Submit a new rule feature request")
    public FeatureRequestResponse create(@Valid @RequestBody FeatureRequestCreateRequest request) {
        return service.create(request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update the status of a rule feature request (admin/developer)")
    public FeatureRequestResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody FeatureRequestStatusUpdateRequest request) {
        return service.updateStatus(id, request);
    }

    @PatchMapping("/{id}/withdraw")
    @Operation(summary = "Withdraw a rule feature request (operator)")
    public FeatureRequestResponse withdraw(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String requestedBy = body.getOrDefault("requestedBy", "operator");
        return service.withdraw(id, requestedBy);
    }
}

