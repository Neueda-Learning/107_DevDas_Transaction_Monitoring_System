package com.hsbc.tms.rules.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.hsbc.tms.rules.entity.RuleExecutionHistory;
import com.hsbc.tms.rules.model.RuleExecutionOutcome;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
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
@Import(JdbcRuleExecutionHistoryRepository.class)
@ActiveProfiles("test")
class JdbcRuleExecutionHistoryRepositoryTest {

    @Autowired
    private JdbcRuleExecutionHistoryRepository repository;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void clean() {
        jdbcClient.sql("DELETE FROM rule_execution_history").update();
    }

    @Test
    void save_andFindByTransactionId_workAsExpected() {
        UUID txId = UUID.randomUUID();

        RuleExecutionHistory first = new RuleExecutionHistory();
        first.setExecutionId(UUID.randomUUID());
        first.setRuleId(7L);
        first.setTransactionId(txId);
        first.setOutcome(RuleExecutionOutcome.NOT_TRIGGERED);
        first.setMessage("not triggered");
        first.setCreatedAt(Instant.parse("2026-08-05T12:00:00Z"));

        RuleExecutionHistory second = new RuleExecutionHistory();
        second.setExecutionId(UUID.randomUUID());
        second.setRuleId(8L);
        second.setTransactionId(txId);
        second.setOutcome(RuleExecutionOutcome.TRIGGERED);
        second.setMessage("triggered");
        second.setCreatedAt(Instant.parse("2026-08-05T12:05:00Z"));

        repository.save(second);
        repository.save(first);

        List<RuleExecutionHistory> rows = repository.findByTransactionId(txId);

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).getOutcome()).isEqualTo(RuleExecutionOutcome.NOT_TRIGGERED);
        assertThat(rows.get(1).getOutcome()).isEqualTo(RuleExecutionOutcome.TRIGGERED);
        assertThat(rows.get(1).getMessage()).isEqualTo("triggered");
    }
}

