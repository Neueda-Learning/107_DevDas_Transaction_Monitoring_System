package com.hsbc.tms.rules.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.hsbc.tms.rules.entity.RuleAuditHistory;
import com.hsbc.tms.rules.model.RuleAuditAction;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JdbcRuleAuditHistoryRepository.class)
@ActiveProfiles("test")
class JdbcRuleAuditHistoryRepositoryTest {

    @Autowired
    private JdbcRuleAuditHistoryRepository repository;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void clean() {
        jdbcClient.sql("DELETE FROM rule_audit_history").update();
    }

    @Test
    void save_andFindByRuleIdOrderByChangedAtAsc_workAsExpected() {
        RuleAuditHistory first = new RuleAuditHistory();
        first.setRuleId(10L);
        first.setAction(RuleAuditAction.CREATED);
        first.setPreviousValues(null);
        first.setNewValues("new1");
        first.setChangedAt(Instant.parse("2026-08-05T10:00:00Z"));
        first.setChangedBy("SYSTEM");

        RuleAuditHistory second = new RuleAuditHistory();
        second.setRuleId(10L);
        second.setAction(RuleAuditAction.UPDATED);
        second.setPreviousValues("old");
        second.setNewValues("new2");
        second.setChangedAt(Instant.parse("2026-08-05T10:05:00Z"));
        second.setChangedBy("SYSTEM");

        repository.save(second);
        repository.save(first);

        List<RuleAuditHistory> rows = repository.findByRuleIdOrderByChangedAtAsc(10L);

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).getAction()).isEqualTo(RuleAuditAction.CREATED);
        assertThat(rows.get(0).getNewValues()).isEqualTo("new1");
        assertThat(rows.get(1).getAction()).isEqualTo(RuleAuditAction.UPDATED);
        assertThat(rows.get(1).getPreviousValues()).isEqualTo("old");
    }
}

