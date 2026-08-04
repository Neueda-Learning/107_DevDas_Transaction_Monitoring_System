package com.hsbc.tms.rules.repository;

import com.hsbc.tms.rules.entity.MonitoringRule;
import com.hsbc.tms.rules.model.AlertSeverity;
import com.hsbc.tms.rules.model.RuleType;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface MonitoringRuleRepository {

    List<MonitoringRule> findByActiveTrue();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    long count();

    long countByActiveTrue();

    long countByActiveFalse();

    Map<RuleType, Long> countGroupedByType();

    Map<AlertSeverity, Long> countGroupedBySeverity();

    List<MonitoringRule> search(Boolean active, RuleType type, AlertSeverity severity);

    Optional<MonitoringRule> findById(Long id);

    boolean existsById(Long id);

    MonitoringRule save(MonitoringRule rule);
}

