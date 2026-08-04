package com.hsbc.tms.dashboard.controller;

import com.hsbc.tms.alerts.model.AlertStatus;
import com.hsbc.tms.alerts.repository.AlertRepository;
import com.hsbc.tms.transaction.dto.TransactionFilterRequest;
import com.hsbc.tms.transaction.repository.TransactionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard", description = "Aggregate counters for the monitoring dashboard")
public class DashboardController {

    private static final Set<AlertStatus> ACTIVE_STATUSES = Set.of(
            AlertStatus.OPEN,
            AlertStatus.ACKNOWLEDGED,
            AlertStatus.INVESTIGATING);

    private final TransactionRepository transactionRepository;
    private final AlertRepository alertRepository;

    public DashboardController(TransactionRepository transactionRepository, AlertRepository alertRepository) {
        this.transactionRepository = transactionRepository;
        this.alertRepository = alertRepository;
    }

    @GetMapping("/summary")
    @Operation(summary = "Get dashboard summary counters")
    public Map<String, Object> summary() {
        long totalTransactions = transactionRepository
                .findByFilter(new TransactionFilterRequest(), PageRequest.of(0, 1))
                .getTotalElements();
        long totalAlerts = alertRepository.search(null, null, false, Set.of()).size();
        long activeAlerts = alertRepository.search(null, null, true, ACTIVE_STATUSES).size();
        long openAlerts = alertRepository.search(AlertStatus.OPEN, null, false, ACTIVE_STATUSES).size();

        return Map.of(
                "totalTransactions", totalTransactions,
                "totalAlerts", totalAlerts,
                "activeAlerts", activeAlerts,
                "openAlerts", openAlerts);
    }
}
