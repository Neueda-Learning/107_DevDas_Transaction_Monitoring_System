package com.hsbc.tms.rules.repository;

import com.hsbc.tms.rules.entity.RuleFeatureRequest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcRuleFeatureRequestRepository implements RuleFeatureRequestRepository {

    private static final RowMapper<RuleFeatureRequest> ROW_MAPPER = new FeatureRequestRowMapper();

    private final JdbcClient jdbcClient;

    public JdbcRuleFeatureRequestRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<RuleFeatureRequest> findAll() {
        String sql = """
                SELECT id, title, description, requested_by, status, admin_note, created_at, updated_at
                FROM rule_feature_requests
                ORDER BY created_at DESC
                """;
        return jdbcClient.sql(sql).query(ROW_MAPPER).list();
    }

    @Override
    public Optional<RuleFeatureRequest> findById(Long id) {
        String sql = """
                SELECT id, title, description, requested_by, status, admin_note, created_at, updated_at
                FROM rule_feature_requests
                WHERE id = :id
                """;
        try {
            RuleFeatureRequest req = jdbcClient.sql(sql).param("id", id).query(ROW_MAPPER).single();
            return Optional.of(req);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    @Override
    public RuleFeatureRequest save(RuleFeatureRequest request) {
        Instant now = Instant.now();
        request.setCreatedAt(now);
        request.setUpdatedAt(now);

        String insertSql = """
                INSERT INTO rule_feature_requests (title, description, requested_by, status, admin_note, created_at, updated_at)
                VALUES (:title, :description, :requestedBy, :status, :adminNote, :createdAt, :updatedAt)
                """;
        Map<String, Object> params = new HashMap<>();
        params.put("title", request.getTitle());
        params.put("description", request.getDescription());
        params.put("requestedBy", request.getRequestedBy());
        params.put("status", request.getStatus());
        params.put("adminNote", request.getAdminNote());
        params.put("createdAt", Timestamp.from(request.getCreatedAt()));
        params.put("updatedAt", Timestamp.from(request.getUpdatedAt()));

        jdbcClient.sql(insertSql).params(params).update();

        Long id = jdbcClient.sql("SELECT LAST_INSERT_ID()").query(Long.class).single();
        request.setId(id);
        return request;
    }

    @Override
    public void updateStatus(Long id, String status, String adminNote) {
        String sql = """
                UPDATE rule_feature_requests
                SET status = :status, admin_note = :adminNote, updated_at = :updatedAt
                WHERE id = :id
                """;
        jdbcClient.sql(sql)
                .param("status", status)
                .param("adminNote", adminNote)
                .param("updatedAt", Timestamp.from(Instant.now()))
                .param("id", id)
                .update();
    }

    @Override
    public void delete(Long id) {
        jdbcClient.sql("DELETE FROM rule_feature_requests WHERE id = :id").param("id", id).update();
    }

    private static final class FeatureRequestRowMapper implements RowMapper<RuleFeatureRequest> {
        @Override
        public RuleFeatureRequest mapRow(ResultSet rs, int rowNum) throws SQLException {
            RuleFeatureRequest req = new RuleFeatureRequest();
            req.setId(rs.getLong("id"));
            req.setTitle(rs.getString("title"));
            req.setDescription(rs.getString("description"));
            req.setRequestedBy(rs.getString("requested_by"));
            req.setStatus(rs.getString("status"));
            req.setAdminNote(rs.getString("admin_note"));
            Timestamp createdAt = rs.getTimestamp("created_at");
            req.setCreatedAt(createdAt == null ? null : createdAt.toInstant());
            Timestamp updatedAt = rs.getTimestamp("updated_at");
            req.setUpdatedAt(updatedAt == null ? null : updatedAt.toInstant());
            return req;
        }
    }
}

