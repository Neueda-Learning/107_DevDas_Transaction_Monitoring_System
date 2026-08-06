package com.hsbc.tms.alerts.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.hsbc.tms.alerts.entity.AlertHistory;
import com.hsbc.tms.alerts.model.AlertStatus;
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
@Import(JdbcAlertHistoryRepository.class)
@ActiveProfiles("test")
class JdbcAlertHistoryRepositoryTest {

    @Autowired
    private JdbcAlertHistoryRepository repository;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void clean() {
        jdbcClient.sql("DELETE FROM alert_history").update();
    }

    @Test
    void save_andFindByAlertIdOrderByCreatedAtAsc_workAsExpected() {
        AlertHistory second = new AlertHistory();
        second.setAlertId(10L);
        second.setFromStatus(AlertStatus.ACKNOWLEDGED);
        second.setToStatus(AlertStatus.CLOSED);
        second.setNote("closed");
        second.setChangedBy("analyst-2");
        second.setCreatedAt(Instant.parse("2026-08-06T10:10:00Z"));

        AlertHistory first = new AlertHistory();
        first.setAlertId(10L);
        first.setFromStatus(null);
        first.setToStatus(AlertStatus.OPEN);
        first.setNote("created");
        first.setChangedBy("system");
        first.setCreatedAt(Instant.parse("2026-08-06T10:00:00Z"));

        repository.save(second);
        repository.save(first);

        List<AlertHistory> rows = repository.findByAlertIdOrderByCreatedAtAsc(10L);

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).getFromStatus()).isNull();
        assertThat(rows.get(0).getToStatus()).isEqualTo(AlertStatus.OPEN);
        assertThat(rows.get(0).getNote()).isEqualTo("created");
        assertThat(rows.get(1).getFromStatus()).isEqualTo(AlertStatus.ACKNOWLEDGED);
        assertThat(rows.get(1).getToStatus()).isEqualTo(AlertStatus.CLOSED);
        assertThat(rows.get(1).getChangedBy()).isEqualTo("analyst-2");
    }
}

