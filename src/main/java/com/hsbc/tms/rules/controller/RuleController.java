package com.hsbc.tms.rules.controller;

import com.hsbc.tms.rules.dto.RuleAuditHistoryResponse;
import com.hsbc.tms.rules.dto.RuleCreateRequest;
import com.hsbc.tms.rules.dto.RuleResponse;
import com.hsbc.tms.rules.dto.RuleStatsResponse;
import com.hsbc.tms.rules.dto.RuleStatusUpdateRequest;
import com.hsbc.tms.rules.dto.RuleUpdateRequest;
import com.hsbc.tms.rules.model.AlertSeverity;
import com.hsbc.tms.rules.model.RuleType;
import com.hsbc.tms.rules.service.RuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rules")
@Tag(name = "Rules Management", description = "APIs for creating, viewing, updating, enabling or disabling, deleting, and reporting monitoring rules")
public class RuleController {

    private final RuleService ruleService;

    public RuleController(RuleService ruleService) {
        this.ruleService = ruleService;
    }

    @GetMapping
    @Operation(summary = "View monitoring rules")
    public List<RuleResponse> list(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) RuleType type,
            @RequestParam(required = false) AlertSeverity severity) {
        return ruleService.getRules(active, type, severity);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create monitoring rule")
    public RuleResponse create(@Valid @RequestBody RuleCreateRequest request) {
        return ruleService.createRule(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update monitoring rule")
    public RuleResponse update(@PathVariable Long id, @Valid @RequestBody RuleUpdateRequest request) {
        return ruleService.updateRule(id, request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Enable or disable monitoring rule")
    public RuleResponse updateStatus(@PathVariable Long id, @Valid @RequestBody RuleStatusUpdateRequest request) {
        return ruleService.updateRuleStatus(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete monitoring rule")
    public RuleResponse delete(@PathVariable Long id) {
        return ruleService.softDeleteRule(id);
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "View rule audit history")
    public List<RuleAuditHistoryResponse> history(@PathVariable Long id) {
        return ruleService.getRuleHistory(id);
    }

    @GetMapping("/stats")
    @Operation(summary = "View rule statistics")
    public RuleStatsResponse stats() {
        return ruleService.getRuleStats();
    }
}

