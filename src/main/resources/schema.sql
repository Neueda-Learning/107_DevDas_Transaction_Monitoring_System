CREATE TABLE IF NOT EXISTS transactions (
    id CHAR(36) PRIMARY KEY,
    account_id VARCHAR(50) NOT NULL,
    payee_id VARCHAR(50) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    transaction_time TIMESTAMP(6) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    reviewed_by VARCHAR(100),
    reviewed_at TIMESTAMP(6),
    review_note VARCHAR(1000),
    rollback_reason_code VARCHAR(50),
    rollback_reason_detail VARCHAR(1000),
    rollback_requested_by VARCHAR(100),
    rollback_requested_at TIMESTAMP(6),
    rollback_supporting_reference VARCHAR(100),
    rollback_reviewed_by VARCHAR(100),
    rollback_reviewed_at TIMESTAMP(6),
    rollback_review_note VARCHAR(1000),
    refunded_at TIMESTAMP(6),
    refund_transaction_id CHAR(36),
    refunded_for_transaction_id CHAR(36),
    INDEX idx_transactions_account_id (account_id),
    INDEX idx_transactions_payee_id (payee_id),
    INDEX idx_transactions_status (status),
    INDEX idx_transactions_transaction_time (transaction_time)
);

CREATE TABLE IF NOT EXISTS monitoring_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL UNIQUE,
    type VARCHAR(40) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL,
    amount_threshold DECIMAL(19, 2),
    transaction_count_threshold INT,
    time_window_minutes INT,
    created_at TIMESTAMP(6) NOT NULL,
    INDEX idx_monitoring_rules_active (active),
    INDEX idx_monitoring_rules_type (type),
    INDEX idx_monitoring_rules_severity (severity)
);

CREATE TABLE IF NOT EXISTS rule_audit_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_id BIGINT NOT NULL,
    action VARCHAR(20) NOT NULL,
    previous_values VARCHAR(4000),
    new_values VARCHAR(4000),
    changed_at TIMESTAMP(6) NOT NULL,
    changed_by VARCHAR(100) NOT NULL,
    INDEX idx_rule_audit_history_rule_id (rule_id),
    INDEX idx_rule_audit_history_changed_at (changed_at)
);

CREATE TABLE IF NOT EXISTS rule_execution_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    execution_id CHAR(36) NOT NULL,
    rule_id BIGINT NOT NULL,
    transaction_id CHAR(36) NOT NULL,
    outcome VARCHAR(20) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    INDEX idx_rule_execution_history_rule_id (rule_id),
    INDEX idx_rule_execution_history_transaction_id (transaction_id),
    INDEX idx_rule_execution_history_created_at (created_at)
);

CREATE TABLE IF NOT EXISTS alerts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_name VARCHAR(150) NOT NULL,
    rule_type VARCHAR(40) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    INDEX idx_alerts_status (status),
    INDEX idx_alerts_severity (severity),
    INDEX idx_alerts_created_at (created_at)
);

CREATE TABLE IF NOT EXISTS alert_transactions (
    alert_id BIGINT NOT NULL,
    transaction_id CHAR(36) NOT NULL,
    PRIMARY KEY (alert_id, transaction_id),
    INDEX idx_alert_transactions_transaction_id (transaction_id)
);

CREATE TABLE IF NOT EXISTS alert_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    alert_id BIGINT NOT NULL,
    from_status VARCHAR(20),
    to_status VARCHAR(20) NOT NULL,
    note VARCHAR(1000) NOT NULL,
    changed_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    INDEX idx_alert_history_alert_id (alert_id),
    INDEX idx_alert_history_created_at (created_at)
);

CREATE TABLE IF NOT EXISTS rule_feature_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(4000) NOT NULL,
    requested_by VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'REQUESTED',
    admin_note VARCHAR(1000),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    INDEX idx_rule_feature_requests_status (status),
    INDEX idx_rule_feature_requests_requested_by (requested_by)
);

