package com.hsbc.tms.rules.repository;

import com.hsbc.tms.rules.entity.RuleAuditHistory;
import java.util.List;

public interface RuleAuditHistoryRepository {

    void save(RuleAuditHistory history);

    List<RuleAuditHistory> findByRuleIdOrderByChangedAtAsc(Long ruleId);
}

