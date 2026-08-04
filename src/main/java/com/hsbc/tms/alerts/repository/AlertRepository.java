package com.hsbc.tms.alerts.repository;

import com.hsbc.tms.alerts.entity.Alert;
import com.hsbc.tms.alerts.model.AlertStatus;
import com.hsbc.tms.rules.model.AlertSeverity;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface AlertRepository {

    List<Alert> search(AlertStatus status, AlertSeverity severity, boolean activeOnly, Set<AlertStatus> activeStatuses);

    Optional<Alert> findById(Long id);

    boolean existsById(Long id);

    Alert save(Alert alert);

    void replaceTriggeringTransactions(Long alertId, List<UUID> triggeringTransactionIds);

    List<UUID> findTriggeringTransactionIdsByAlertId(Long alertId);

    List<Alert> findActiveByTriggeringTransactionId(UUID transactionId, Set<AlertStatus> activeStatuses);
}


