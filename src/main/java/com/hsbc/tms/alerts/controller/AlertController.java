package com.hsbc.tms.alerts.controller;

import com.hsbc.tms.alerts.dto.AlertHistoryResponse;
import com.hsbc.tms.alerts.dto.AlertResponse;
import com.hsbc.tms.alerts.dto.AlertStatusUpdateRequest;
import com.hsbc.tms.alerts.dto.CreateAlertRequest;
import com.hsbc.tms.alerts.model.AlertStatus;
import com.hsbc.tms.alerts.service.AlertService;
import com.hsbc.tms.rules.model.AlertSeverity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/alerts")
@Tag(name = "Alerts Management", description = "APIs for viewing alerts and managing alert workflow states")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create alert")
    public AlertResponse create(@Valid @RequestBody CreateAlertRequest request) {
        return alertService.createAlert(request);
    }

    @GetMapping
    @Operation(summary = "View alerts")
    public List<AlertResponse> list(
            @RequestParam(required = false) AlertStatus status,
            @RequestParam(required = false) AlertSeverity severity,
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        return alertService.getAlerts(status, severity, activeOnly);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get alert by id")
    public AlertResponse get(@PathVariable Long id) {
        return alertService.getAlert(id);
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "View alert status history")
    public List<AlertHistoryResponse> history(@PathVariable Long id) {
        return alertService.getHistory(id);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update alert status")
    public AlertResponse updateStatus(@PathVariable Long id, @Valid @RequestBody AlertStatusUpdateRequest request) {
        return alertService.updateStatus(id, request);
    }
}



