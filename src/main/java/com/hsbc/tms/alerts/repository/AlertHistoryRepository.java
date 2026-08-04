package com.hsbc.tms.alerts.repository;

import com.hsbc.tms.alerts.entity.AlertHistory;
import java.util.List;

public interface AlertHistoryRepository {

    void save(AlertHistory history);

    List<AlertHistory> findByAlertIdOrderByCreatedAtAsc(Long alertId);
}


