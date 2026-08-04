package com.hsbc.tms.alerts.repository;

import com.hsbc.tms.alerts.entity.AlertHistory;
import com.hsbc.tms.alerts.model.AlertStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAlertHistoryRepository implements AlertHistoryRepository {

    private static final RowMapper<AlertHistory> ROW_MAPPER = new AlertHistoryRowMapper();

    private final JdbcClient jdbcClient;

    public JdbcAlertHistoryRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void save(AlertHistory history) {
        String sql = """
                INSERT INTO alert_history (
                    alert_id, from_status, to_status, note, changed_by, created_at
                ) VALUES (
                    :alertId, :fromStatus, :toStatus, :note, :changedBy, :createdAt
                )
                """;

        jdbcClient.sql(sql)
                .param("alertId", history.getAlertId())
                .param("fromStatus", history.getFromStatus() == null ? null : history.getFromStatus().name())
                .param("toStatus", history.getToStatus().name())
                .param("note", history.getNote())
                .param("changedBy", history.getChangedBy())
                .param("createdAt", Timestamp.from(history.getCreatedAt()))
                .update();
    }

    @Override
    public List<AlertHistory> findByAlertIdOrderByCreatedAtAsc(Long alertId) {
        String sql = """
                SELECT id, alert_id, from_status, to_status, note, changed_by, created_at
                FROM alert_history
                WHERE alert_id = :alertId
                ORDER BY created_at ASC, id ASC
                """;

        return jdbcClient.sql(sql)
                .param("alertId", alertId)
                .query(ROW_MAPPER)
                .list();
    }

    private static final class AlertHistoryRowMapper implements RowMapper<AlertHistory> {

        @Override
        public AlertHistory mapRow(ResultSet rs, int rowNum) throws SQLException {
            AlertHistory history = new AlertHistory();
            history.setId(rs.getLong("id"));
            history.setAlertId(rs.getLong("alert_id"));

            String fromStatus = rs.getString("from_status");
            history.setFromStatus(fromStatus == null ? null : AlertStatus.valueOf(fromStatus));

            history.setToStatus(AlertStatus.valueOf(rs.getString("to_status")));
            history.setNote(rs.getString("note"));
            history.setChangedBy(rs.getString("changed_by"));
            Timestamp createdAt = rs.getTimestamp("created_at");
            history.setCreatedAt(createdAt == null ? null : createdAt.toInstant());
            return history;
        }
    }
}


